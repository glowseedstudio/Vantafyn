using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace Vantafyn.Plugin.Companion.Notifications;

public interface IPushRegistrationStore
{
    Task<IReadOnlyList<DevicePushRegistration>> GetDevicesForUserAsync(Guid userId, CancellationToken cancellationToken);
    Task<DevicePushRegistration?> GetDeviceAsync(Guid userId, string deviceId, CancellationToken cancellationToken);
    Task<DevicePushRegistration> UpsertDeviceAsync(Guid userId, DevicePushRegistration registration, CancellationToken cancellationToken);
    Task<bool> RemoveDeviceAsync(Guid userId, string deviceId, CancellationToken cancellationToken);
    Task<int> RemoveEndpointGloballyAsync(string endpoint, CancellationToken cancellationToken);
}
