namespace Vantafyn.Plugin.Companion.UserSettings;

public sealed record UserSettingsEnvelope(
    int SchemaVersion,
    int Revision,
    DateTimeOffset UpdatedAtUtc,
    string? AppVersion,
    string Shared,
    string Mobile,
    string Tv);

public sealed record UserSettingsPutRequest(
    int SchemaVersion,
    int Revision,
    string? AppVersion,
    string? Shared,
    string? Mobile,
    string? Tv);

public sealed record UserSettingsResponse(
    int SchemaVersion,
    int Revision,
    DateTimeOffset UpdatedAtUtc,
    string? AppVersion,
    string Shared,
    string Mobile,
    string Tv);
