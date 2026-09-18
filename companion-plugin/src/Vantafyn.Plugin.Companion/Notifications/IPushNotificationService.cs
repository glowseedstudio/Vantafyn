using System;
using System.Threading;
using System.Threading.Tasks;

namespace Vantafyn.Plugin.Companion.Notifications;

public interface IPushNotificationService
{
    Task<PushDispatchResult> SendToUserAsync(Guid userId, object payload, CancellationToken cancellationToken);
    Task<PushDispatchResult> SendToDeviceAsync(Guid userId, string deviceId, object payload, CancellationToken cancellationToken);
}
