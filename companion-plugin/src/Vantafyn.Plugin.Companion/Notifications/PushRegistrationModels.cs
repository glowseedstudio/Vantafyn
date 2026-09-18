using System;
using System.Collections.Generic;

namespace Vantafyn.Plugin.Companion.Notifications;

public sealed record DevicePushRegistration(
    string DeviceId,
    string Endpoint,
    string? Distributor,
    string? ClientName,
    DateTimeOffset RegisteredAtUtc,
    DateTimeOffset UpdatedAtUtc
);

public sealed record UserPushDevicesEnvelope(
    int SchemaVersion,
    DateTimeOffset UpdatedAtUtc,
    Dictionary<string, DevicePushRegistration> Devices
);

public sealed record PushRegistrationRequest(
    string DeviceId,
    string Endpoint,
    string? Distributor = null,
    string? ClientName = null
);

public sealed record PushRegistrationResponse(
    string DeviceId,
    bool Registered,
    DateTimeOffset UpdatedAtUtc
);

public sealed record PushDeviceSummary(
    string DeviceId,
    string? Distributor,
    string? ClientName,
    bool HasEndpoint,
    DateTimeOffset RegisteredAtUtc,
    DateTimeOffset UpdatedAtUtc
);

public sealed record PushTestRequest(
    string? DeviceId = null
);

public sealed record PushTestResponse(
    bool Success,
    int DevicesContacted,
    int DevicesSucceeded,
    int StaleEndpointsRemoved,
    string? Message
);

public sealed record PushNotifyChatRequest(
    Guid RecipientUserId,
    string? ConversationId,
    string? SenderName,
    string MessageText
);

public sealed record PushNotifyAchievementRequest(
    string AchievementId,
    string Title,
    string? Description = null
);

public sealed record PushDispatchResult(
    int DevicesContacted,
    int DevicesSucceeded,
    int StaleEndpointsRemoved
);
