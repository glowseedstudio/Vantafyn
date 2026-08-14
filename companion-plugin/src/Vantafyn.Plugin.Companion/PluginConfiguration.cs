using MediaBrowser.Model.Plugins;

namespace Vantafyn.Plugin.Companion;

public sealed class PluginConfiguration : BasePluginConfiguration
{
    public bool UserSettingsEnabled { get; set; } = true;
    public bool RequestsEnabled { get; set; } = true;
    public bool WatchPartiesEnabled { get; set; } = true;
    public bool PersonalPlaylistsEnabled { get; set; } = true;
    public bool NotificationsEnabled { get; set; } = true;
    public OmbiConfiguration Ombi { get; set; } = new();
}

public sealed class OmbiConfiguration
{
    public string? BaseUrl { get; set; }
    public string? ApiKey { get; set; }
    public int TimeoutSeconds { get; set; } = 5;
    public bool RequireUserLogin { get; set; } = true;
}
