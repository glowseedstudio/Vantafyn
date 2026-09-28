using System;
using System.Threading;
using System.Threading.Tasks;

namespace Vantafyn.Plugin.Companion.Games;

public interface IGameSavesService
{
    Task<byte[]?> GetAsync(Guid userId, string gameId, string kind, CancellationToken cancellationToken);
    Task SaveAsync(Guid userId, string gameId, string kind, byte[] data, CancellationToken cancellationToken);
    Task<bool> DeleteAsync(Guid userId, string gameId, string kind, CancellationToken cancellationToken);
}
