using System.Collections.Concurrent;
using System.Security.Cryptography;
using Vantafyn.Plugin.Companion.Core;

namespace Vantafyn.Plugin.Companion.WatchParties;

public interface IWatchPartyService
{
    Task<WatchPartyState> CreateAsync(Guid hostUserId, CreateWatchPartyRequest request, CancellationToken cancellationToken);
    Task<WatchPartyState?> GetAsync(string partyId, Guid userId, CancellationToken cancellationToken);
    Task<WatchPartyState> JoinAsync(string partyId, Guid userId, string? inviteId, CancellationToken cancellationToken);
    Task LeaveAsync(string partyId, Guid userId, CancellationToken cancellationToken);
    Task EndAsync(string partyId, Guid userId, CancellationToken cancellationToken);
    Task<WatchPartyState> CommandAsync(string partyId, Guid userId, WatchPartyCommandRequest request, CancellationToken cancellationToken);
    Task<WatchPartyInvite> InviteAsync(string partyId, Guid senderUserId, InviteRequest request, CancellationToken cancellationToken);
}

public sealed class InMemoryWatchPartyService(IClock clock, IRealtimeTransport realtime) : IWatchPartyService
{
    private readonly ConcurrentDictionary<string, MutableParty> _parties = new();
    private readonly ConcurrentDictionary<string, WatchPartyInvite> _invites = new();

    public async Task<WatchPartyState> CreateAsync(Guid hostUserId, CreateWatchPartyRequest request, CancellationToken cancellationToken)
    {
        var now = clock.UtcNow;
        var party = new MutableParty(
            Id: NewOpaqueId("wp"),
            HostUserId: hostUserId,
            MediaItemId: request.MediaItemId,
            CreatedAtUtc: now,
            LastActivityUtc: now,
            ExpiresAtUtc: now.AddHours(6),
            Sequence: 1,
            Snapshot: new WatchPartyPlaybackSnapshot(request.MediaItemId, request.PositionTicks, request.IsPaused, now, 1));
        party.Participants[hostUserId] = new WatchPartyParticipant(hostUserId, now, now);
        _parties[party.Id] = party;
        await realtime.PublishAsync(hostUserId, new { type = "PartyStateChanged", partyId = party.Id }, cancellationToken).ConfigureAwait(false);
        return party.ToState();
    }

    public Task<WatchPartyState?> GetAsync(string partyId, Guid userId, CancellationToken cancellationToken)
    {
        if (!_parties.TryGetValue(partyId, out var party) || !party.Participants.ContainsKey(userId)) return Task.FromResult<WatchPartyState?>(null);
        party.Touch(userId, clock.UtcNow);
        return Task.FromResult<WatchPartyState?>(party.ToState());
    }

    public async Task<WatchPartyState> JoinAsync(string partyId, Guid userId, string? inviteId, CancellationToken cancellationToken)
    {
        if (!_parties.TryGetValue(partyId, out var party)) throw new InvalidOperationException("Party was not found.");
        if (party.HostUserId != userId)
        {
            if (string.IsNullOrWhiteSpace(inviteId) || !_invites.TryGetValue(inviteId, out var invite) || invite.PartyId != partyId || invite.RecipientUserId != userId || invite.ExpiresAtUtc <= clock.UtcNow)
            {
                throw new UnauthorizedAccessException("A valid invite is required.");
            }
            _invites[inviteId] = invite with { Status = "accepted" };
        }
        var now = clock.UtcNow;
        party.Participants[userId] = new WatchPartyParticipant(userId, now, now);
        party.LastActivityUtc = now;
        await BroadcastAsync(party, "ParticipantJoined", cancellationToken).ConfigureAwait(false);
        return party.ToState();
    }

    public async Task LeaveAsync(string partyId, Guid userId, CancellationToken cancellationToken)
    {
        if (!_parties.TryGetValue(partyId, out var party)) return;
        if (party.HostUserId == userId)
        {
            await EndAsync(partyId, userId, cancellationToken).ConfigureAwait(false);
            return;
        }
        party.Participants.TryRemove(userId, out _);
        await BroadcastAsync(party, "ParticipantLeft", cancellationToken).ConfigureAwait(false);
    }

    public async Task EndAsync(string partyId, Guid userId, CancellationToken cancellationToken)
    {
        if (!_parties.TryGetValue(partyId, out var party)) return;
        if (party.HostUserId != userId) throw new UnauthorizedAccessException("Only the host can end the party.");
        _parties.TryRemove(partyId, out _);
        await BroadcastAsync(party, "PartyEnded", cancellationToken).ConfigureAwait(false);
    }

    public async Task<WatchPartyState> CommandAsync(string partyId, Guid userId, WatchPartyCommandRequest request, CancellationToken cancellationToken)
    {
        if (!_parties.TryGetValue(partyId, out var party)) throw new InvalidOperationException("Party was not found.");
        if (party.HostUserId != userId) throw new UnauthorizedAccessException("Only the host can control playback in v0.1.");
        var now = clock.UtcNow;
        var sequence = ++party.Sequence;
        var itemId = request.ItemId ?? party.Snapshot.ItemId;
        var position = request.PositionTicks ?? party.Snapshot.PositionTicks;
        var paused = request.Type.Equals("pause", StringComparison.OrdinalIgnoreCase) || (request.Type.Equals("seek", StringComparison.OrdinalIgnoreCase) && party.Snapshot.IsPaused);
        party.Snapshot = new WatchPartyPlaybackSnapshot(itemId, position, paused, now, sequence);
        party.MediaItemId = itemId;
        party.LastActivityUtc = now;
        await BroadcastAsync(party, "PartyStateChanged", cancellationToken).ConfigureAwait(false);
        return party.ToState();
    }

    public async Task<WatchPartyInvite> InviteAsync(string partyId, Guid senderUserId, InviteRequest request, CancellationToken cancellationToken)
    {
        if (!_parties.TryGetValue(partyId, out var party)) throw new InvalidOperationException("Party was not found.");
        if (party.HostUserId != senderUserId) throw new UnauthorizedAccessException("Only the host can invite participants in v0.1.");
        var now = clock.UtcNow;
        var invite = new WatchPartyInvite(NewOpaqueId("wi"), partyId, senderUserId, request.RecipientUserId, now, now.AddSeconds(Math.Clamp(request.ExpiresInSeconds, 30, 300)), "pending");
        _invites[invite.InviteId] = invite;
        await realtime.PublishAsync(request.RecipientUserId, new { type = "WatchPartyInvite", inviteId = invite.InviteId, partyId }, cancellationToken).ConfigureAwait(false);
        return invite;
    }

    private async Task BroadcastAsync(MutableParty party, string type, CancellationToken cancellationToken)
    {
        foreach (var participant in party.Participants.Keys)
        {
            await realtime.PublishAsync(participant, new { type, partyId = party.Id, sequence = party.Sequence }, cancellationToken).ConfigureAwait(false);
        }
    }

    private static string NewOpaqueId(string prefix)
    {
        Span<byte> bytes = stackalloc byte[18];
        RandomNumberGenerator.Fill(bytes);
        return prefix + "_" + Convert.ToBase64String(bytes).Replace('+', '-').Replace('/', '_').TrimEnd('=');
    }

    private sealed class MutableParty
    {
        public MutableParty(string Id, Guid HostUserId, string MediaItemId, DateTimeOffset CreatedAtUtc, DateTimeOffset LastActivityUtc, DateTimeOffset ExpiresAtUtc, long Sequence, WatchPartyPlaybackSnapshot Snapshot)
        {
            this.MediaItemId = MediaItemId;
            this.LastActivityUtc = LastActivityUtc;
            this.ExpiresAtUtc = ExpiresAtUtc;
            this.Sequence = Sequence;
            this.Snapshot = Snapshot;
            this.Id = Id;
            this.HostUserId = HostUserId;
            this.CreatedAtUtc = CreatedAtUtc;
        }

        public string Id { get; }
        public Guid HostUserId { get; }
        public DateTimeOffset CreatedAtUtc { get; }
        public string MediaItemId { get; set; }
        public DateTimeOffset LastActivityUtc { get; set; }
        public DateTimeOffset ExpiresAtUtc { get; set; }
        public long Sequence { get; set; }
        public WatchPartyPlaybackSnapshot Snapshot { get; set; }
        public ConcurrentDictionary<Guid, WatchPartyParticipant> Participants { get; } = new();

        public void Touch(Guid userId, DateTimeOffset now)
        {
            if (Participants.TryGetValue(userId, out var participant))
            {
                Participants[userId] = participant with { LastSeenUtc = now };
                LastActivityUtc = now;
            }
        }

        public WatchPartyState ToState() => new(Id, HostUserId, MediaItemId, CreatedAtUtc, LastActivityUtc, ExpiresAtUtc, Sequence, Snapshot, Participants.Values.OrderBy(x => x.JoinedAtUtc).ToList());
    }
}
