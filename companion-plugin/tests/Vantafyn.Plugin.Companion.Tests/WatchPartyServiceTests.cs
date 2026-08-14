using Vantafyn.Plugin.Companion.Core;
using Vantafyn.Plugin.Companion.WatchParties;
using Xunit;

namespace Vantafyn.Plugin.Companion.Tests;

public sealed class WatchPartyServiceTests
{
    [Fact]
    public async Task HostOnlyCommandsAreEnforced()
    {
        var realtime = new InMemoryRealtimeTransport();
        var service = new InMemoryWatchPartyService(new FixedClock(), realtime);
        var host = Guid.NewGuid();
        var participant = Guid.NewGuid();
        var party = await service.CreateAsync(host, new CreateWatchPartyRequest("item-1", 0, true), CancellationToken.None);
        var invite = await service.InviteAsync(party.PartyId, host, new InviteRequest(participant), CancellationToken.None);
        await service.JoinAsync(party.PartyId, participant, invite.InviteId, CancellationToken.None);

        await Assert.ThrowsAsync<UnauthorizedAccessException>(() =>
            service.CommandAsync(party.PartyId, participant, new WatchPartyCommandRequest("play", 0, null), CancellationToken.None));

        var updated = await service.CommandAsync(party.PartyId, host, new WatchPartyCommandRequest("play", 123, null), CancellationToken.None);
        Assert.Equal(2, updated.Sequence);
    }

    private sealed class FixedClock : IClock
    {
        public DateTimeOffset UtcNow => DateTimeOffset.UtcNow;
    }
}
