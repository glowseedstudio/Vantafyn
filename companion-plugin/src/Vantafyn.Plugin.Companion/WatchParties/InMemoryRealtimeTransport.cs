using System.Collections.Concurrent;
using System.Threading.Channels;
using System.Text.Json;
using Vantafyn.Plugin.Companion.Core;

namespace Vantafyn.Plugin.Companion.WatchParties;

public interface IRealtimeTransport
{
    IAsyncEnumerable<string> SubscribeAsync(Guid userId, CancellationToken cancellationToken);
    Task PublishAsync(Guid userId, object payload, CancellationToken cancellationToken);
}

public sealed class InMemoryRealtimeTransport : IRealtimeTransport
{
    private readonly ConcurrentDictionary<Guid, Channel<string>> _channels = new();

    public async IAsyncEnumerable<string> SubscribeAsync(Guid userId, [System.Runtime.CompilerServices.EnumeratorCancellation] CancellationToken cancellationToken)
    {
        var channel = _channels.GetOrAdd(userId, _ => Channel.CreateUnbounded<string>());
        while (await channel.Reader.WaitToReadAsync(cancellationToken).ConfigureAwait(false))
        {
            while (channel.Reader.TryRead(out var message))
            {
                yield return message;
            }
        }
    }

    public Task PublishAsync(Guid userId, object payload, CancellationToken cancellationToken)
    {
        var channel = _channels.GetOrAdd(userId, _ => Channel.CreateUnbounded<string>());
        var message = JsonSerializer.Serialize(payload, new JsonSerializerOptions(JsonSerializerDefaults.Web));
        channel.Writer.TryWrite(message);
        return Task.CompletedTask;
    }
}
