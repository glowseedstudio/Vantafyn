using System.Security.Cryptography;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;
using Vantafyn.Plugin.Companion.Core;
using Vantafyn.Plugin.Companion.Games;

namespace Vantafyn.Plugin.Companion.Pokemon;

/// <summary>
/// File-based implementation of IPokemonBackupService storing backups under
/// {PokemonRoot}/backups/{userId:N}/{gameId}/ with SHA256 checksum validation.
/// </summary>
public sealed class FilePokemonBackupService : IPokemonBackupService
{
    private readonly ICompanionPaths _paths;
    private readonly IClock _clock;
    private readonly IGameSavesService _savesService;
    private readonly ILogger<FilePokemonBackupService> _logger;

    public FilePokemonBackupService(
        ICompanionPaths paths,
        IClock clock,
        IGameSavesService savesService,
        ILogger<FilePokemonBackupService>? logger = null)
    {
        _paths = paths;
        _clock = clock;
        _savesService = savesService;
        _logger = logger ?? NullLogger<FilePokemonBackupService>.Instance;
    }

    public async Task<PokemonBackupMetadata> CreateBackupAsync(
        Guid userId,
        string gameId,
        byte[] saveBytes,
        string reason,
        string? transactionId,
        CancellationToken cancellationToken)
    {
        var normalizedGameId = NormalizeGameId(gameId);
        var backupId = Guid.NewGuid().ToString("N");
        var checksum = ComputeSha256(saveBytes);
        var now = _clock.UtcNow;

        var metadata = new PokemonBackupMetadata
        {
            BackupId = backupId,
            UserId = userId,
            GameId = normalizedGameId,
            CreatedAtUtc = now,
            Reason = reason,
            ChecksumSha256 = checksum,
            SizeBytes = saveBytes.Length,
            TransactionId = transactionId,
            IsRestored = false
        };

        var dir = GameBackupDirectory(userId, normalizedGameId);
        Directory.CreateDirectory(dir);

        var binaryPath = Path.Combine(dir, $"{backupId}.sram.bak");
        var metaPath = Path.Combine(dir, $"{backupId}.meta.json");

        await File.WriteAllBytesAsync(binaryPath, saveBytes, cancellationToken).ConfigureAwait(false);
        await JsonFile.WriteAtomicAsync(metaPath, metadata, cancellationToken).ConfigureAwait(false);

        _logger.LogInformation("Created save backup {BackupId} for user {UserId} game {GameId} ({Bytes} bytes, checksum {Checksum})",
            backupId, userId, normalizedGameId, saveBytes.Length, checksum);

        return metadata;
    }

    public async Task<byte[]?> GetBackupBytesAsync(Guid userId, string backupId, CancellationToken cancellationToken)
    {
        var meta = await GetBackupMetadataAsync(userId, backupId, cancellationToken).ConfigureAwait(false);
        if (meta == null)
        {
            return null;
        }

        var dir = GameBackupDirectory(userId, meta.GameId);
        var binaryPath = Path.Combine(dir, $"{backupId}.sram.bak");
        if (!File.Exists(binaryPath))
        {
            return null;
        }

        var bytes = await File.ReadAllBytesAsync(binaryPath, cancellationToken).ConfigureAwait(false);
        var actualChecksum = ComputeSha256(bytes);
        if (!string.Equals(actualChecksum, meta.ChecksumSha256, StringComparison.OrdinalIgnoreCase))
        {
            _logger.LogError("Backup {BackupId} checksum mismatch! Expected {Expected}, got {Actual}",
                backupId, meta.ChecksumSha256, actualChecksum);
            throw new InvalidOperationException($"Backup {backupId} failed checksum integrity verification.");
        }

        return bytes;
    }

    public async Task<PokemonBackupMetadata?> GetBackupMetadataAsync(Guid userId, string backupId, CancellationToken cancellationToken)
    {
        var userRoot = UserBackupRoot(userId);
        if (!Directory.Exists(userRoot))
        {
            return null;
        }

        // Search game subdirectories for the metadata file
        var metaFiles = Directory.GetFiles(userRoot, $"{backupId}.meta.json", SearchOption.AllDirectories);
        if (metaFiles.Length == 0)
        {
            return null;
        }

        return await JsonFile.ReadAsync<PokemonBackupMetadata>(metaFiles[0], cancellationToken).ConfigureAwait(false);
    }

    public async Task<IReadOnlyList<PokemonBackupMetadata>> ListBackupsAsync(Guid userId, string? gameId, CancellationToken cancellationToken)
    {
        var results = new List<PokemonBackupMetadata>();
        string targetDir;

        if (!string.IsNullOrWhiteSpace(gameId))
        {
            targetDir = GameBackupDirectory(userId, NormalizeGameId(gameId));
            if (!Directory.Exists(targetDir)) return results;
        }
        else
        {
            targetDir = UserBackupRoot(userId);
            if (!Directory.Exists(targetDir)) return results;
        }

        var metaFiles = Directory.GetFiles(targetDir, "*.meta.json", SearchOption.AllDirectories);
        foreach (var file in metaFiles)
        {
            var meta = await JsonFile.ReadAsync<PokemonBackupMetadata>(file, cancellationToken).ConfigureAwait(false);
            if (meta != null && meta.UserId == userId)
            {
                results.Add(meta);
            }
        }

        return results.OrderByDescending(b => b.CreatedAtUtc).ToList();
    }

    public async Task<bool> RestoreBackupAsync(Guid userId, string backupId, CancellationToken cancellationToken)
    {
        var meta = await GetBackupMetadataAsync(userId, backupId, cancellationToken).ConfigureAwait(false);
        if (meta == null)
        {
            _logger.LogWarning("Cannot restore backup {BackupId}: metadata not found for user {UserId}", backupId, userId);
            return false;
        }

        var bytes = await GetBackupBytesAsync(userId, backupId, cancellationToken).ConfigureAwait(false);
        if (bytes == null)
        {
            _logger.LogWarning("Cannot restore backup {BackupId}: binary data missing", backupId);
            return false;
        }

        // Restore onto active battery save
        await _savesService.SaveAsync(userId, meta.GameId, "sram", bytes, cancellationToken).ConfigureAwait(false);

        // Update metadata
        meta.IsRestored = true;
        meta.RestoredAtUtc = _clock.UtcNow;

        var dir = GameBackupDirectory(userId, meta.GameId);
        var metaPath = Path.Combine(dir, $"{backupId}.meta.json");
        await JsonFile.WriteAtomicAsync(metaPath, meta, cancellationToken).ConfigureAwait(false);

        _logger.LogInformation("Successfully restored backup {BackupId} for user {UserId} game {GameId}", backupId, userId, meta.GameId);
        return true;
    }

    public async Task<int> PruneOldBackupsAsync(Guid userId, string gameId, int retainCount, CancellationToken cancellationToken)
    {
        var backups = await ListBackupsAsync(userId, gameId, cancellationToken).ConfigureAwait(false);
        // Requirement 10: Never delete if only 1 backup exists!
        if (backups.Count <= 1 || backups.Count <= retainCount)
        {
            return 0;
        }

        // Keep the newest retainCount, delete the rest
        var toPrune = backups.Skip(retainCount).ToList();
        var deletedCount = 0;

        foreach (var b in toPrune)
        {
            var dir = GameBackupDirectory(userId, b.GameId);
            var binaryPath = Path.Combine(dir, $"{b.BackupId}.sram.bak");
            var metaPath = Path.Combine(dir, $"{b.BackupId}.meta.json");

            if (File.Exists(binaryPath)) File.Delete(binaryPath);
            if (File.Exists(metaPath)) File.Delete(metaPath);
            deletedCount++;
        }

        _logger.LogInformation("Pruned {Count} old backups for user {UserId} game {GameId}", deletedCount, userId, gameId);
        return deletedCount;
    }

    private string UserBackupRoot(Guid userId) => Path.Combine(_paths.PokemonRoot, "backups", userId.ToString("N"));

    private string GameBackupDirectory(Guid userId, string gameId) =>
        Path.Combine(UserBackupRoot(userId), Sanitize(gameId));

    private static string NormalizeGameId(string gameId) => gameId.Trim().ToLowerInvariant();

    private static string Sanitize(string value)
    {
        var invalid = Path.GetInvalidFileNameChars();
        var chars = value.Where(c => Array.IndexOf(invalid, c) < 0 && c != '.' && c != '/' && c != '\\').ToArray();
        return new string(chars);
    }

    private static string ComputeSha256(byte[] data)
    {
        var hash = SHA256.HashData(data);
        return Convert.ToHexString(hash).ToLowerInvariant();
    }
}
