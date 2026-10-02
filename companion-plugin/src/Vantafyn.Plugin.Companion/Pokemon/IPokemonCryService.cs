using System.IO;
using System.Threading;
using System.Threading.Tasks;

namespace Vantafyn.Plugin.Companion.Pokemon;

/// <summary>
/// Service responsible for resolving and caching Pokémon cry audio (.ogg).
/// </summary>
public interface IPokemonCryService
{
    /// <summary>
    /// Gets a stream to the Pokémon's cry audio (.ogg).
    /// If not cached locally, downloads it from the configured upstream repository.
    /// </summary>
    /// <param name="speciesId">The National Pokédex species ID (1-1025+).</param>
    /// <param name="style">Optional style: "latest" (default) or "legacy".</param>
    /// <param name="cancellationToken">Cancellation token.</param>
    /// <returns>A tuple containing the read stream, content type ("audio/ogg"), and whether the cry was found.</returns>
    Task<(Stream? Stream, string ContentType, bool Found)> GetCryStreamAsync(
        int speciesId,
        string? style = null,
        CancellationToken cancellationToken = default);
}
