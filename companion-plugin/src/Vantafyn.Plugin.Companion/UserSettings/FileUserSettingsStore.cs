using System.Collections.Concurrent;
using Vantafyn.Plugin.Companion.Core;

namespace Vantafyn.Plugin.Companion.UserSettings;

public interface IUserSettingsStore
{
    Task<UserSettingsEnvelope?> GetAsync(Guid userId, CancellationToken cancellationToken);
    Task<UserSettingsEnvelope> SaveAsync(Guid userId, UserSettingsPutRequest request, CancellationToken cancellationToken);
    Task DeleteAsync(Guid userId, CancellationToken cancellationToken);
}

public sealed class FileUserSettingsStore(ICompanionPaths paths, IClock clock) : IUserSettingsStore
{
    private const int MaxDocumentBytes = 256 * 1024;
    private static readonly ConcurrentDictionary<Guid, SemaphoreSlim> Locks = new();

    public async Task<UserSettingsEnvelope?> GetAsync(Guid userId, CancellationToken cancellationToken)
    {
        await LockFor(userId).WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            return await JsonFile.ReadAsync<UserSettingsEnvelope>(PathFor(userId), cancellationToken).ConfigureAwait(false);
        }
        finally
        {
            LockFor(userId).Release();
        }
    }

    public async Task<UserSettingsEnvelope> SaveAsync(Guid userId, UserSettingsPutRequest request, CancellationToken cancellationToken)
    {
        Validate(request.Shared);
        Validate(request.Mobile);
        Validate(request.Tv);

        await LockFor(userId).WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var existing = await JsonFile.ReadAsync<UserSettingsEnvelope>(PathFor(userId), cancellationToken).ConfigureAwait(false);
            if (existing != null && request.Revision != existing.Revision)
            {
                throw new RevisionConflictException(existing.Revision);
            }

            var envelope = new UserSettingsEnvelope(
                SchemaVersion: request.SchemaVersion <= 0 ? 1 : request.SchemaVersion,
                Revision: (existing?.Revision ?? 0) + 1,
                UpdatedAtUtc: clock.UtcNow,
                AppVersion: request.AppVersion,
                Shared: request.Shared ?? "{}",
                Mobile: request.Mobile ?? "{}",
                Tv: request.Tv ?? "{}");

            await JsonFile.WriteAtomicAsync(PathFor(userId), envelope, cancellationToken).ConfigureAwait(false);
            return envelope;
        }
        finally
        {
            LockFor(userId).Release();
        }
    }

    public async Task DeleteAsync(Guid userId, CancellationToken cancellationToken)
    {
        await LockFor(userId).WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var path = PathFor(userId);
            if (File.Exists(path))
            {
                File.Delete(path);
            }
        }
        finally
        {
            LockFor(userId).Release();
        }
    }

    private string PathFor(Guid userId) => Path.Combine(paths.UserSettingsRoot, userId.ToString("N") + ".json");

    private static SemaphoreSlim LockFor(Guid userId) => Locks.GetOrAdd(userId, _ => new SemaphoreSlim(1, 1));

    private static void Validate(string? json) => JsonFile.ValidateJsonPayload(json ?? "{}", MaxDocumentBytes);
}
