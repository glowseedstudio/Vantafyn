using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using MediaBrowser.Controller.Net;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;
using Vantafyn.Plugin.Companion.Core;

namespace Vantafyn.Plugin.Companion.Notifications;

[ApiController]
[Authorize]
[Route("Vantafyn/Notifications/Push")]
public sealed partial class PushNotificationsController(
    IAuthorizationContext authorizationContext,
    IPushRegistrationStore store,
    IHttpClientFactory httpClientFactory,
    ILogger<PushNotificationsController> logger,
    IClock clock) : ControllerBase
{
    private static readonly Regex ValidDeviceIdRegex = new(@"^[a-zA-Z0-9_\-\.]{1,128}$", RegexOptions.Compiled);

    [HttpPost("Register")]
    public async Task<IActionResult> Register([FromBody] PushRegistrationRequest request, CancellationToken cancellationToken)
    {
        if (Plugin.Instance?.Configuration.NotificationsEnabled != true)
        {
            return StatusCode(StatusCodes.Status503ServiceUnavailable, new { error = "Push notifications are disabled." });
        }

        var userId = await this.CurrentUserIdAsync(authorizationContext).ConfigureAwait(false);

        if (string.IsNullOrWhiteSpace(request.DeviceId) || !ValidDeviceIdRegex.IsMatch(request.DeviceId.Trim()))
        {
            return BadRequest(new { error = "Invalid or missing deviceId. Must be 1-128 alphanumeric, dash, underscore, or dot characters." });
        }

        var trimmedEndpoint = request.Endpoint?.Trim();
        if (string.IsNullOrWhiteSpace(trimmedEndpoint) ||
            !Uri.TryCreate(trimmedEndpoint, UriKind.Absolute, out var uri) ||
            (uri.Scheme != Uri.UriSchemeHttp && uri.Scheme != Uri.UriSchemeHttps) ||
            trimmedEndpoint.Length > 2048)
        {
            return BadRequest(new { error = "Invalid or missing push endpoint. Must be a valid HTTP/HTTPS URL up to 2048 characters." });
        }

        var deviceId = request.DeviceId.Trim();
        var distributor = request.Distributor?.Trim().TakePrefix(128);
        var clientName = request.ClientName?.Trim().TakePrefix(128);

        var now = clock.UtcNow;
        var registration = new DevicePushRegistration(
            DeviceId: deviceId,
            Endpoint: trimmedEndpoint,
            Distributor: distributor,
            ClientName: clientName,
            RegisteredAtUtc: now,
            UpdatedAtUtc: now
        );

        var updated = await store.UpsertDeviceAsync(userId, registration, cancellationToken).ConfigureAwait(false);

        logger.LogInformation(
            "UnifiedPush endpoint registered for user {UserId}, device {DeviceId} via {Distributor} ({MaskedEndpoint})",
            userId,
            deviceId,
            distributor ?? "unknown",
            MaskEndpoint(trimmedEndpoint));

        return Ok(new PushRegistrationResponse(
            DeviceId: updated.DeviceId,
            Registered: true,
            UpdatedAtUtc: updated.UpdatedAtUtc
        ));
    }

    [HttpDelete("Devices/{deviceId}")]
    public async Task<IActionResult> UnregisterDevice(string deviceId, CancellationToken cancellationToken)
    {
        if (Plugin.Instance?.Configuration.NotificationsEnabled != true)
        {
            return StatusCode(StatusCodes.Status503ServiceUnavailable, new { error = "Push notifications are disabled." });
        }

        var userId = await this.CurrentUserIdAsync(authorizationContext).ConfigureAwait(false);
        if (string.IsNullOrWhiteSpace(deviceId))
        {
            return BadRequest(new { error = "deviceId must be specified." });
        }

        var removed = await store.RemoveDeviceAsync(userId, deviceId.Trim(), cancellationToken).ConfigureAwait(false);

        logger.LogInformation(
            "UnifiedPush device {DeviceId} unregistration for user {UserId}: removed={Removed}",
            deviceId,
            userId,
            removed);

        return NoContent();
    }

    [HttpDelete("Register")]
    public Task<IActionResult> Unregister([FromQuery] string deviceId, CancellationToken cancellationToken) =>
        UnregisterDevice(deviceId, cancellationToken);

    [HttpGet("Devices")]
    public async Task<IActionResult> GetDevices(CancellationToken cancellationToken)
    {
        if (Plugin.Instance?.Configuration.NotificationsEnabled != true)
        {
            return StatusCode(StatusCodes.Status503ServiceUnavailable, new { error = "Push notifications are disabled." });
        }

        var userId = await this.CurrentUserIdAsync(authorizationContext).ConfigureAwait(false);
        var devices = await store.GetDevicesForUserAsync(userId, cancellationToken).ConfigureAwait(false);

        var summaries = devices.Select(d => new PushDeviceSummary(
            DeviceId: d.DeviceId,
            Distributor: d.Distributor,
            ClientName: d.ClientName,
            HasEndpoint: !string.IsNullOrWhiteSpace(d.Endpoint),
            RegisteredAtUtc: d.RegisteredAtUtc,
            UpdatedAtUtc: d.UpdatedAtUtc
        )).ToList();

        return Ok(new { devices = summaries });
    }

    [HttpPost("Test")]
    public async Task<IActionResult> SendTestPush([FromBody] PushTestRequest? request, CancellationToken cancellationToken)
    {
        if (Plugin.Instance?.Configuration.NotificationsEnabled != true)
        {
            return StatusCode(StatusCodes.Status503ServiceUnavailable, new { error = "Push notifications are disabled." });
        }

        var userId = await this.CurrentUserIdAsync(authorizationContext).ConfigureAwait(false);

        List<DevicePushRegistration> targets = [];
        if (!string.IsNullOrWhiteSpace(request?.DeviceId))
        {
            var dev = await store.GetDeviceAsync(userId, request.DeviceId.Trim(), cancellationToken).ConfigureAwait(false);
            if (dev == null)
            {
                return NotFound(new { error = $"Device with ID '{request.DeviceId}' not registered for current user." });
            }
            targets.Add(dev);
        }
        else
        {
            var userDevices = await store.GetDevicesForUserAsync(userId, cancellationToken).ConfigureAwait(false);
            targets.AddRange(userDevices);
        }

        if (targets.Count == 0)
        {
            return BadRequest(new { error = "No registered push devices found for this user." });
        }

        var payloadObject = new
        {
            type = "debug_test",
            title = "Vantafyn Push Test",
            message = "UnifiedPush test notification from Vantafyn Companion server",
            timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
        };
        var payloadJson = JsonSerializer.Serialize(payloadObject);

        var client = httpClientFactory.CreateClient();
        client.Timeout = TimeSpan.FromSeconds(10);

        var devicesSucceeded = 0;
        var staleEndpointsRemoved = 0;

        foreach (var target in targets)
        {
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
                        "Test push delivered successfully to user {UserId}, device {DeviceId} ({MaskedEndpoint})",
                        userId,
                        target.DeviceId,
                        MaskEndpoint(target.Endpoint));
                }
                else if (response.StatusCode == HttpStatusCode.NotFound || response.StatusCode == HttpStatusCode.Gone)
                {
                    // Stale endpoint returned by distributor (e.g. ntfy or gotify indicates topic is expired / unregistered)
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
                    "Failed to dispatch test push to device {DeviceId} ({MaskedEndpoint})",
                    target.DeviceId,
                    MaskEndpoint(target.Endpoint));
            }
        }

        var responseData = new PushTestResponse(
            Success: devicesSucceeded > 0,
            DevicesContacted: targets.Count,
            DevicesSucceeded: devicesSucceeded,
            StaleEndpointsRemoved: staleEndpointsRemoved,
            Message: $"Sent to {devicesSucceeded}/{targets.Count} device(s). {staleEndpointsRemoved} stale endpoint(s) cleaned up."
        );

        return Ok(responseData);
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

internal static class StringExtensions
{
    public static string TakePrefix(this string str, int maxLength) =>
        str.Length <= maxLength ? str : str[..maxLength];
}
