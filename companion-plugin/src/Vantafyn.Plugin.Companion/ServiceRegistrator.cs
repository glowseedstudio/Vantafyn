using MediaBrowser.Controller;
using MediaBrowser.Controller.Plugins;
using Microsoft.Extensions.DependencyInjection;
using Vantafyn.Plugin.Companion.Core;
using Vantafyn.Plugin.Companion.Requests;
using Vantafyn.Plugin.Companion.UserSettings;
using Vantafyn.Plugin.Companion.WatchParties;
using Vantafyn.Plugin.Companion.PersonalPlaylists;

namespace Vantafyn.Plugin.Companion;

public sealed class ServiceRegistrator : IPluginServiceRegistrator
{
    public void RegisterServices(IServiceCollection serviceCollection, IServerApplicationHost applicationHost)
    {
        serviceCollection.AddSingleton<IClock, SystemClock>();
        serviceCollection.AddSingleton<ICompanionPaths, CompanionPaths>();
        serviceCollection.AddSingleton<ICompanionDiagnostics, CompanionDiagnostics>();
        serviceCollection.AddSingleton<IUserSettingsStore, FileUserSettingsStore>();
        serviceCollection.AddSingleton<IWatchPartyService, InMemoryWatchPartyService>();
        serviceCollection.AddSingleton<IRealtimeTransport, InMemoryRealtimeTransport>();
        serviceCollection.AddSingleton<IPersonalPlaylistStore, FilePersonalPlaylistStore>();
        serviceCollection.AddSingleton<IOmbiClientFactory, OmbiClientFactory>();
        serviceCollection.AddSingleton<IOmbiUserSessionStore, FileOmbiUserSessionStore>();
        serviceCollection.AddHttpClient();
    }
}
