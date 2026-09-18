namespace Vantafyn.Plugin.Companion.Core;

public interface ICompanionPaths
{
    string DataRoot { get; }
    string UserSettingsRoot { get; }
    string PersonalPlaylistsRoot { get; }
    string OmbiSessionsRoot { get; }
    string SecretsRoot { get; }
    string PushRegistrationsRoot { get; }
}

public sealed class CompanionPaths : ICompanionPaths
{
    public string DataRoot => Plugin.Instance?.DataRootPath
        ?? throw new InvalidOperationException("Vantafyn Companion plugin instance is not ready.");

    public string UserSettingsRoot => Ensure(Path.Combine(DataRoot, "user-settings"));

    public string PersonalPlaylistsRoot => Ensure(Path.Combine(DataRoot, "personal-playlists"));

    public string OmbiSessionsRoot => Ensure(Path.Combine(DataRoot, "ombi-sessions"));

    public string SecretsRoot => Ensure(Path.Combine(DataRoot, "secrets"));

    public string PushRegistrationsRoot => Ensure(Path.Combine(DataRoot, "push-registrations"));

    private static string Ensure(string path)
    {
        Directory.CreateDirectory(path);
        return path;
    }
}
