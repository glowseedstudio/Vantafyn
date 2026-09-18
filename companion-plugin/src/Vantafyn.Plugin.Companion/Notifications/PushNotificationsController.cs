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
    IPushNotificationService pushService,
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

        var payload = new
        {
            type = "debug_test",
            title = "Vantafyn Push Test",
            message = "UnifiedPush test notification from Vantafyn Companion server",
            timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
        };

        PushDispatchResult result;
        if (!string.IsNullOrWhiteSpace(request?.DeviceId))
        {
            result = await pushService.SendToDeviceAsync(userId, request.DeviceId.Trim(), payload, cancellationToken).ConfigureAwait(false);
        }
        else
        {
            result = await pushService.SendToUserAsync(userId, payload, cancellationToken).ConfigureAwait(false);
        }

        if (result.DevicesContacted == 0)
        {
            return BadRequest(new { error = "No registered push devices found for this user." });
        }

        var responseData = new PushTestResponse(
            Success: result.DevicesSucceeded > 0,
            DevicesContacted: result.DevicesContacted,
            DevicesSucceeded: result.DevicesSucceeded,
            StaleEndpointsRemoved: result.StaleEndpointsRemoved,
            Message: $"Sent to {result.DevicesSucceeded}/{result.DevicesContacted} device(s). {result.StaleEndpointsRemoved} stale endpoint(s) cleaned up."
        );

        return Ok(responseData);
    }

    [HttpPost("NotifyChat")]
    public async Task<IActionResult> NotifyChat([FromBody] PushNotifyChatRequest request, CancellationToken cancellationToken)
    {
        if (Plugin.Instance?.Configuration.NotificationsEnabled != true)
        {
            return StatusCode(StatusCodes.Status503ServiceUnavailable, new { error = "Push notifications are disabled." });
        }

        var senderUserId = await this.CurrentUserIdAsync(authorizationContext).ConfigureAwait(false);

        if (request == null || string.IsNullOrWhiteSpace(request.MessageText) || request.RecipientUserId == Guid.Empty)
        {
            return BadRequest(new { error = "Invalid chat notification request. RecipientUserId and MessageText are required." });
        }

        var payload = new
        {
            type = "chat_message",
            senderId = senderUserId.ToString(),
            senderName = request.SenderName ?? "Friend",
            conversationId = request.ConversationId ?? senderUserId.ToString(),
            messageText = request.MessageText.Trim().TakePrefix(500),
            timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
        };

        var result = await pushService.SendToUserAsync(request.RecipientUserId, payload, cancellationToken).ConfigureAwait(false);

        logger.LogInformation(
            "Dispatched chat push notification from {SenderId} to {RecipientId}: contacted={Contacted}, succeeded={Succeeded}",
            senderUserId,
            request.RecipientUserId,
            result.DevicesContacted,
            result.DevicesSucceeded);

        return Ok(new
        {
            sent = result.DevicesSucceeded > 0,
            devicesContacted = result.DevicesContacted,
            devicesSucceeded = result.DevicesSucceeded
        });
    }

    [HttpPost("NotifyAchievement")]
    public async Task<IActionResult> NotifyAchievement([FromBody] PushNotifyAchievementRequest request, CancellationToken cancellationToken)
    {
        if (Plugin.Instance?.Configuration.NotificationsEnabled != true)
        {
            return StatusCode(StatusCodes.Status503ServiceUnavailable, new { error = "Push notifications are disabled." });
        }

        var currentUserId = await this.CurrentUserIdAsync(authorizationContext).ConfigureAwait(false);

        if (request == null || string.IsNullOrWhiteSpace(request.AchievementId) || string.IsNullOrWhiteSpace(request.Title))
        {
            return BadRequest(new { error = "Invalid achievement notification request. AchievementId and Title are required." });
        }

        var payload = new
        {
            type = "achievement_unlock",
            userId = currentUserId.ToString(),
            achievementId = request.AchievementId,
            title = request.Title,
            description = request.Description ?? string.Empty,
            timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
        };

        var result = await pushService.SendToUserAsync(currentUserId, payload, cancellationToken).ConfigureAwait(false);

        logger.LogInformation(
            "Dispatched achievement unlock push notification to user {UserId} for badge {BadgeId}: contacted={Contacted}, succeeded={Succeeded}",
            currentUserId,
            request.AchievementId,
            result.DevicesContacted,
            result.DevicesSucceeded);

        return Ok(new
        {
            sent = result.DevicesSucceeded > 0,
            devicesContacted = result.DevicesContacted,
            devicesSucceeded = result.DevicesSucceeded
        });
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
