using System;
using System.Threading;
using System.Threading.Tasks;

namespace Vantafyn.Plugin.Companion.Games;

public sealed class CloudSaveSummaryDto
{
    public string GameId { get; set; } = string.Empty;
    public string Kind { get; set; } = "sram";
    public long LastModifiedMs { get; set; }
    public long SizeBytes { get; set; }
    public string? TitleId { get; set; }
    public string? GameTitle { get; set; }
}

public interface IGameSavesService
{
    Task<byte[]?> GetAsync(Guid userId, string gameId, string kind, CancellationToken cancellationToken);
    Task SaveAsync(Guid userId, string gameId, string kind, byte[] data, CancellationToken cancellationToken);
    Task<bool> DeleteAsync(Guid userId, string gameId, string kind, CancellationToken cancellationToken);
    Task<IReadOnlyList<CloudSaveSummaryDto>> ListAsync(Guid userId, CancellationToken cancellationToken);
}

