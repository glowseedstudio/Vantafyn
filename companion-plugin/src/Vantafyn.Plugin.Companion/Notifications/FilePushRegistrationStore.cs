using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Vantafyn.Plugin.Companion.Core;

namespace Vantafyn.Plugin.Companion.Notifications;

public sealed class FilePushRegistrationStore(ICompanionPaths paths, IClock clock) : IPushRegistrationStore
{
    private static readonly ConcurrentDictionary<Guid, SemaphoreSlim> Locks = new();

    public async Task<IReadOnlyList<DevicePushRegistration>> GetDevicesForUserAsync(Guid userId, CancellationToken cancellationToken)
    {
        await LockFor(userId).WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var envelope = await JsonFile.ReadAsync<UserPushDevicesEnvelope>(PathFor(userId), cancellationToken).ConfigureAwait(false);
            if (envelope?.Devices == null || envelope.Devices.Count == 0)
            {
                return Array.Empty<DevicePushRegistration>();
            }

            return envelope.Devices.Values.ToList();
        }
        finally
        {
            LockFor(userId).Release();
        }
    }

    public async Task<DevicePushRegistration?> GetDeviceAsync(Guid userId, string deviceId, CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(deviceId)) return null;

        await LockFor(userId).WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var envelope = await JsonFile.ReadAsync<UserPushDevicesEnvelope>(PathFor(userId), cancellationToken).ConfigureAwait(false);
            if (envelope?.Devices == null) return null;

            return envelope.Devices.TryGetValue(deviceId, out var dev) ? dev : null;
        }
        finally
        {
            LockFor(userId).Release();
        }
    }

    public async Task<DevicePushRegistration> UpsertDeviceAsync(Guid userId, DevicePushRegistration registration, CancellationToken cancellationToken)
    {
        await LockFor(userId).WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var path = PathFor(userId);
            var envelope = await JsonFile.ReadAsync<UserPushDevicesEnvelope>(path, cancellationToken).ConfigureAwait(false);
            var devices = envelope?.Devices ?? new Dictionary<string, DevicePushRegistration>(StringComparer.OrdinalIgnoreCase);

            var existing = devices.TryGetValue(registration.DeviceId, out var found) ? found : null;
            var now = clock.UtcNow;

            var updated = registration with
            {
                RegisteredAtUtc = existing?.RegisteredAtUtc ?? registration.RegisteredAtUtc,
                UpdatedAtUtc = now
            };

            devices[registration.DeviceId] = updated;

            var newEnvelope = new UserPushDevicesEnvelope(
                SchemaVersion: 1,
                UpdatedAtUtc: now,
                Devices: devices
            );

            await JsonFile.WriteAtomicAsync(path, newEnvelope, cancellationToken).ConfigureAwait(false);
            return updated;
        }
        finally
        {
            LockFor(userId).Release();
        }
    }

    public async Task<bool> RemoveDeviceAsync(Guid userId, string deviceId, CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(deviceId)) return false;

        await LockFor(userId).WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            var path = PathFor(userId);
            var envelope = await JsonFile.ReadAsync<UserPushDevicesEnvelope>(path, cancellationToken).ConfigureAwait(false);
            if (envelope?.Devices == null || !envelope.Devices.ContainsKey(deviceId))
            {
                return false;
            }

            envelope.Devices.Remove(deviceId);

            if (envelope.Devices.Count == 0)
            {
                if (File.Exists(path))
                {
                    File.Delete(path);
                }
            }
            else
            {
                var newEnvelope = envelope with { UpdatedAtUtc = clock.UtcNow };
                await JsonFile.WriteAtomicAsync(path, newEnvelope, cancellationToken).ConfigureAwait(false);
            }

            return true;
        }
        finally
        {
            LockFor(userId).Release();
        }
    }

    public async Task<int> RemoveEndpointGloballyAsync(string endpoint, CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(endpoint)) return 0;

        var count = 0;
        var files = Directory.GetFiles(paths.PushRegistrationsRoot, "*.json");
        foreach (var file in files)
        {
            var fileName = Path.GetFileNameWithoutExtension(file);
            if (!Guid.TryParse(fileName, out var userId)) continue;

            await LockFor(userId).WaitAsync(cancellationToken).ConfigureAwait(false);
            try
            {
                var envelope = await JsonFile.ReadAsync<UserPushDevicesEnvelope>(file, cancellationToken).ConfigureAwait(false);
                if (envelope?.Devices == null) continue;

                var toRemove = envelope.Devices.Values
                    .Where(d => string.Equals(d.Endpoint, endpoint, StringComparison.OrdinalIgnoreCase))
                    .Select(d => d.DeviceId)
                    .ToList();

                if (toRemove.Count > 0)
                {
                    foreach (var devId in toRemove)
                    {
                        envelope.Devices.Remove(devId);
                        count++;
                    }

                    if (envelope.Devices.Count == 0)
                    {
                        if (File.Exists(file)) File.Delete(file);
                    }
                    else
                    {
                        var updated = envelope with { UpdatedAtUtc = clock.UtcNow };
                        await JsonFile.WriteAtomicAsync(file, updated, cancellationToken).ConfigureAwait(false);
                    }
                }
            }
            finally
            {
                LockFor(userId).Release();
            }
        }

        return count;
    }

    private string PathFor(Guid userId) => Path.Combine(paths.PushRegistrationsRoot, userId.ToString("N") + ".json");

    private static SemaphoreSlim LockFor(Guid userId) => Locks.GetOrAdd(userId, _ => new SemaphoreSlim(1, 1));
}
