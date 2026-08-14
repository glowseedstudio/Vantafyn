namespace Vantafyn.Plugin.Companion.Core;

public interface ICompanionDiagnostics
{
    object Snapshot(PluginConfiguration configuration);
}

public sealed class CompanionDiagnostics : ICompanionDiagnostics
{
    public object Snapshot(PluginConfiguration configuration)
    {
        var requestsState = !configuration.RequestsEnabled
            ? ModuleState.Disabled
            : string.IsNullOrWhiteSpace(configuration.Ombi.BaseUrl) || string.IsNullOrWhiteSpace(configuration.Ombi.ApiKey)
                ? ModuleState.Unconfigured
                : ModuleState.Ready;

        return new
        {
            pluginVersion = "0.1.0",
            apiVersion = 1,
            jellyfinTarget = "10.11.11",
            userSettings = new { state = configuration.UserSettingsEnabled ? ModuleState.Ready.ToString().ToLowerInvariant() : "disabled" },
            requests = new
            {
                state = requestsState.ToString().ToLowerInvariant(),
                provider = "ombi",
                serverConfigured = requestsState == ModuleState.Ready,
                keyStored = !string.IsNullOrWhiteSpace(configuration.Ombi.ApiKey),
                apiKey = "redacted"
            },
            watchParties = new { state = configuration.WatchPartiesEnabled ? "ready" : "disabled" },
            personalPlaylists = new { state = configuration.PersonalPlaylistsEnabled ? "ready" : "disabled" },
            notifications = new { liveSession = configuration.NotificationsEnabled, backgroundPush = false }
        };
    }
}
