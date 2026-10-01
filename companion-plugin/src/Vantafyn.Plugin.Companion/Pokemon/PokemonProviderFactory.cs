using Microsoft.Extensions.Logging;
using Vantafyn.Plugin.Companion.Pokemon.Native;
using Vantafyn.Plugin.Companion.Pokemon.PkVault;

namespace Vantafyn.Plugin.Companion.Pokemon;

public interface IPokemonProviderFactory
{
    IPokemonProvider Create(PokemonConfiguration config);
}

public sealed class PokemonProviderFactory(
    IHttpClientFactory httpClientFactory,
    ILogger<PkVaultPokemonProvider> pkVaultLogger,
    ILogger<NativePokemonProvider>? nativeLogger = null) : IPokemonProviderFactory
{
    private readonly NativePokemonProvider _nativeProvider = new(nativeLogger);

    public IPokemonProvider Create(PokemonConfiguration config)
    {
        // If user configured a reachable external PKVault service URL and requested pkvault, use it.
        // Otherwise, always use the high-performance embedded Native save engine.
        if (!string.IsNullOrWhiteSpace(config.PkVaultBaseUrl) &&
            config.ProviderType.Equals("pkvault", StringComparison.OrdinalIgnoreCase))
        {
            return new PkVaultPokemonProvider(httpClientFactory, config, pkVaultLogger);
        }

        return _nativeProvider;
    }
}
