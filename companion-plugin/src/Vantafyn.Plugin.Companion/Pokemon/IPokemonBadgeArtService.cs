namespace Vantafyn.Plugin.Companion.Pokemon;

public interface IPokemonBadgeArtService
{
    PokemonBadgeArtCatalogDto GetCatalog(PokemonConfiguration config, string imageUrlBase);
    PokemonBadgeArtFile? ResolveImage(PokemonConfiguration config, string regionId, string badgeId);
}
