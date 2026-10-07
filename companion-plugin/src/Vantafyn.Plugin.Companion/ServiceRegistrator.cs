using MediaBrowser.Controller;
using MediaBrowser.Controller.Plugins;
using Microsoft.Extensions.DependencyInjection;
using Vantafyn.Plugin.Companion.Core;
using Vantafyn.Plugin.Companion.Requests;
using Vantafyn.Plugin.Companion.UserSettings;
using Vantafyn.Plugin.Companion.WatchParties;
using Vantafyn.Plugin.Companion.PersonalPlaylists;
using Vantafyn.Plugin.Companion.Notifications;
using Vantafyn.Plugin.Companion.Games;

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
        serviceCollection.AddSingleton<IPushRegistrationStore, FilePushRegistrationStore>();
        serviceCollection.AddSingleton<IPushNotificationService, PushNotificationService>();
        serviceCollection.AddSingleton<IOmbiClientFactory, OmbiClientFactory>();
        serviceCollection.AddSingleton<IOmbiUserSessionStore, FileOmbiUserSessionStore>();
        serviceCollection.AddSingleton<IGamesService, GamesService>();
        serviceCollection.AddSingleton<IGameSavesService, GameSavesService>();
        serviceCollection.AddSingleton<IGameLinkService, InMemoryGameLinkService>();
        serviceCollection.AddSingleton<Pokemon.IPokemonProviderFactory, Pokemon.PokemonProviderFactory>();
        serviceCollection.AddSingleton<Pokemon.IPokemonGameDetector, Pokemon.PokemonGameDetector>();
        serviceCollection.AddSingleton<Pokemon.IPokemonVaultStore, Pokemon.FilePokemonVaultStore>();
        serviceCollection.AddSingleton<Pokemon.IGameSessionTracker, Pokemon.InMemoryGameSessionTracker>();
        serviceCollection.AddSingleton<Pokemon.ISaveOperationCoordinator, Pokemon.FileSaveOperationCoordinator>();
        serviceCollection.AddSingleton<Pokemon.IPokemonBackupService, Pokemon.FilePokemonBackupService>();
        serviceCollection.AddSingleton<Pokemon.IPokemonTransactionManager, Pokemon.PokemonTransactionManager>();
        serviceCollection.AddSingleton<Pokemon.IPokemonTradingService, Pokemon.FilePokemonTradingService>();
        serviceCollection.AddSingleton<Pokemon.IPokemonJourneyService, Pokemon.FilePokemonJourneyService>();
        serviceCollection.AddSingleton<Pokemon.IPokemonSocialService, Pokemon.FilePokemonSocialService>();
        serviceCollection.AddSingleton<Pokemon.IPokemonCryService, Pokemon.FilePokemonCryService>();
        serviceCollection.AddSingleton<Pokemon.IPokemonBadgeArtService, Pokemon.FilePokemonBadgeArtService>();
        serviceCollection.AddHttpClient();
    }
}
