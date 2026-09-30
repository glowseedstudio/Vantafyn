using Microsoft.Extensions.Logging;
using Vantafyn.Plugin.Companion.Pokemon.PkVault;

namespace Vantafyn.Plugin.Companion.Pokemon;

public interface IPokemonProviderFactory
{
    IPokemonProvider Create(PokemonConfiguration config);
}

public sealed class PokemonProviderFactory(
    IHttpClientFactory httpClientFactory,
    ILogger<PkVaultPokemonProvider> pkVaultLogger) : IPokemonProviderFactory
{
    public IPokemonProvider Create(PokemonConfiguration config)
    {
        return new PkVaultPokemonProvider(httpClientFactory, config, pkVaultLogger);
    }
}
