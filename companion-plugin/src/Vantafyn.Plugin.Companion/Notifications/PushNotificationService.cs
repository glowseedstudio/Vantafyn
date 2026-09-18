using System;
using System.Collections.Generic;
using System.Net;
using System.Net.Http;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Logging;

namespace Vantafyn.Plugin.Companion.Notifications;

public sealed class PushNotificationService(
    IPushRegistrationStore store,
    IHttpClientFactory httpClientFactory,
    ILogger<PushNotificationService> logger) : IPushNotificationService
{
    public async Task<PushDispatchResult> SendToUserAsync(Guid userId, object payload, CancellationToken cancellationToken)
    {
        var devices = await store.GetDevicesForUserAsync(userId, cancellationToken).ConfigureAwait(false);
        if (devices.Count == 0)
        {
            return new PushDispatchResult(0, 0, 0);
        }

        return await DispatchToDevicesAsync(userId, devices, payload, cancellationToken).ConfigureAwait(false);
    }

    public async Task<PushDispatchResult> SendToDeviceAsync(Guid userId, string deviceId, object payload, CancellationToken cancellationToken)
    {
        var device = await store.GetDeviceAsync(userId, deviceId, cancellationToken).ConfigureAwait(false);
        if (device == null)
        {
            return new PushDispatchResult(0, 0, 0);
        }

        return await DispatchToDevicesAsync(userId, [device], payload, cancellationToken).ConfigureAwait(false);
    }

    private async Task<PushDispatchResult> DispatchToDevicesAsync(
        Guid userId,
        IReadOnlyList<DevicePushRegistration> targets,
        object payload,
        CancellationToken cancellationToken)
    {
        var payloadJson = JsonSerializer.Serialize(payload, new JsonSerializerOptions(JsonSerializerDefaults.Web));
        var client = httpClientFactory.CreateClient();
        client.Timeout = TimeSpan.FromSeconds(10);

        var devicesSucceeded = 0;
        var staleEndpointsRemoved = 0;

        foreach (var target in targets)
        {
            if (string.IsNullOrWhiteSpace(target.Endpoint)) continue;

            try
            {
                using var msg = new HttpRequestMessage(HttpMethod.Post, target.Endpoint)
                {
                    Content = new StringContent(payloadJson, Encoding.UTF8, "application/json")
                };
                msg.Headers.TryAddWithoutValidation("Urgency", "high");
                msg.Headers.TryAddWithoutValidation("TTL", "60");

                using var response = await client.SendAsync(msg, cancellationToken).ConfigureAwait(false);

                if (response.IsSuccessStatusCode)
                {
                    devicesSucceeded++;
                    logger.LogInformation(
                        "UnifiedPush delivered successfully to user {UserId}, device {DeviceId} ({MaskedEndpoint})",
                        userId,
                        target.DeviceId,
                        MaskEndpoint(target.Endpoint));
                }
                else if (response.StatusCode == HttpStatusCode.NotFound || response.StatusCode == HttpStatusCode.Gone)
                {
                    staleEndpointsRemoved++;
                    await store.RemoveDeviceAsync(userId, target.DeviceId, cancellationToken).ConfigureAwait(false);

                    logger.LogWarning(
                        "Stale UnifiedPush endpoint detected (HTTP {StatusCode}) for user {UserId}, device {DeviceId} ({MaskedEndpoint}); removed device registration",
                        (int)response.StatusCode,
                        userId,
                        target.DeviceId,
                        MaskEndpoint(target.Endpoint));
                }
                else
                {
                    logger.LogWarning(
                        "UnifiedPush delivery to device {DeviceId} returned HTTP {StatusCode} ({MaskedEndpoint})",
                        target.DeviceId,
                        (int)response.StatusCode,
                        MaskEndpoint(target.Endpoint));
                }
            }
            catch (Exception ex)
            {
                logger.LogError(
                    ex,
                    "Failed to dispatch UnifiedPush to device {DeviceId} ({MaskedEndpoint})",
                    target.DeviceId,
                    MaskEndpoint(target.Endpoint));
            }
        }

        return new PushDispatchResult(targets.Count, devicesSucceeded, staleEndpointsRemoved);
    }

    private static string MaskEndpoint(string endpoint)
    {
        if (string.IsNullOrWhiteSpace(endpoint)) return "[empty]";
        if (!Uri.TryCreate(endpoint, UriKind.Absolute, out var uri)) return "[invalid-uri]";
        using var sha = SHA256.Create();
        var hash = Convert.ToHexString(sha.ComputeHash(Encoding.UTF8.GetBytes(endpoint)))[..8];
        return $"{uri.Scheme}://{uri.Authority}/...#{hash}";
    }
}
