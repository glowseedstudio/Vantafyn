namespace Vantafyn.Plugin.Companion.WatchParties;

public sealed record WatchPartyPlaybackSnapshot(
    string ItemId,
    long PositionTicks,
    bool IsPaused,
    DateTimeOffset ServerTimestampUtc,
    long Sequence);

public sealed record WatchPartyParticipant(Guid UserId, DateTimeOffset JoinedAtUtc, DateTimeOffset LastSeenUtc);

public sealed record WatchPartyInvite(
    string InviteId,
    string PartyId,
    Guid SenderUserId,
    Guid RecipientUserId,
    DateTimeOffset CreatedAtUtc,
    DateTimeOffset ExpiresAtUtc,
    string Status);

public sealed record WatchPartyState(
    string PartyId,
    Guid HostUserId,
    string MediaItemId,
    DateTimeOffset CreatedAtUtc,
    DateTimeOffset LastActivityUtc,
    DateTimeOffset ExpiresAtUtc,
    long Sequence,
    WatchPartyPlaybackSnapshot Snapshot,
    IReadOnlyList<WatchPartyParticipant> Participants);

public sealed record CreateWatchPartyRequest(string MediaItemId, long PositionTicks, bool IsPaused);
public sealed record WatchPartyCommandRequest(string Type, long? PositionTicks, string? ItemId);
public sealed record InviteRequest(Guid RecipientUserId, int ExpiresInSeconds = 60);
