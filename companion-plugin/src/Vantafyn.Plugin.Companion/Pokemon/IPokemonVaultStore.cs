namespace Vantafyn.Plugin.Companion.Pokemon;

/// <summary>
/// Persistence contract for per-user Pokémon Vault storage.
/// Strictly enforces multi-user isolation using Jellyfin user GUIDs.
/// </summary>
public interface IPokemonVaultStore
{
    /// <summary>
    /// Gets the user's personal vault, creating an initialized empty 30-box vault if one does not exist yet.
    /// </summary>
    Task<PokemonVault> GetOrCreateVaultAsync(Guid userId, CancellationToken cancellationToken);

    /// <summary>
    /// Retrieves a high-level summary of the user's personal vault (total count, shiny count, box capacity).
    /// </summary>
    Task<PokemonVaultSummaryDto> GetVaultSummaryAsync(Guid userId, CancellationToken cancellationToken);

    /// <summary>
    /// Retrieves summary metadata for all boxes in the user's vault.
    /// </summary>
    Task<IReadOnlyList<PokemonVaultBoxSummaryDto>> GetBoxesSummaryAsync(Guid userId, CancellationToken cancellationToken);

    /// <summary>
    /// Retrieves a specific box (1-indexed) including its Pokémon entries.
    /// </summary>
    Task<PokemonVaultBox?> GetBoxAsync(Guid userId, int boxIndex, CancellationToken cancellationToken);

    /// <summary>
    /// Sorts and compacts Pokémon in a specified box using the chosen criterion and direction.
    /// Re-compacts slot indices 1..N.
    /// </summary>
    Task<PokemonVaultBox?> SortBoxAsync(Guid userId, int boxIndex, string criterion, bool ascending, CancellationToken cancellationToken);

    /// <summary>
    /// Retrieves a specific Pokémon entry by its ID from the user's vault.
    /// </summary>
    Task<PokemonVaultEntry?> GetEntryAsync(Guid userId, string entryId, CancellationToken cancellationToken);

    /// <summary>
    /// Saves the complete vault document for the user atomically.
    /// </summary>
    Task<PokemonVault> SaveVaultAsync(Guid userId, PokemonVault vault, CancellationToken cancellationToken);

    /// <summary>
    /// Adds or updates an individual Pokémon entry within the user's vault.
    /// </summary>
    Task<PokemonVaultEntry> AddOrUpdateEntryAsync(Guid userId, PokemonVaultEntry entry, CancellationToken cancellationToken);

    /// <summary>
    /// Removes a Pokémon entry by its ID from the user's vault.
    /// </summary>
    Task<bool> RemoveEntryAsync(Guid userId, string entryId, CancellationToken cancellationToken);

    /// <summary>
    /// Updates the active moves for a Pokémon entry in the user's vault.
    /// </summary>
    Task<PokemonVaultEntry?> UpdateEntryMovesAsync(Guid userId, string entryId, IReadOnlyList<string> moves, CancellationToken cancellationToken);

    /// <summary>
    /// Evolves a Pokémon entry in the user's vault to a new species.
    /// </summary>
    Task<PokemonVaultEntry?> EvolveEntryAsync(Guid userId, string entryId, int targetSpeciesId, CancellationToken cancellationToken);

    /// <summary>
    /// Gets the user's Pokémon profile/preferences.
    /// </summary>
    Task<PokemonUserProfile> GetUserProfileAsync(Guid userId, CancellationToken cancellationToken);

    /// <summary>
    /// Saves the user's Pokémon profile/preferences.
    /// </summary>
    Task<PokemonUserProfile> SaveUserProfileAsync(Guid userId, PokemonUserProfile profile, CancellationToken cancellationToken);
}
