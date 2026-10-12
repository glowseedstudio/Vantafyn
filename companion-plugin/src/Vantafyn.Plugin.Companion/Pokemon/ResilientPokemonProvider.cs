using System;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Logging;
using Vantafyn.Plugin.Companion.Pokemon.Native;
using Vantafyn.Plugin.Companion.Pokemon.PkVault;

namespace Vantafyn.Plugin.Companion.Pokemon;

/// <summary>
/// Composite Pokémon provider that prioritizes an external provider (such as PKVault)
/// and seamlessly falls back to the embedded Native save engine if the external provider
/// is unreachable, encounters an API error, or cannot identify the save file.
/// </summary>
public sealed class ResilientPokemonProvider : IPokemonProvider
{
    private readonly IPokemonProvider _primary;
    private readonly IPokemonProvider _fallback;
    private readonly ILogger _logger;

    public string ProviderName => $"{_primary.ProviderName}+fallback:{_fallback.ProviderName}";

    public ResilientPokemonProvider(
        IPokemonProvider primary,
        IPokemonProvider fallback,
        ILogger logger)
    {
        _primary = primary ?? throw new ArgumentNullException(nameof(primary));
        _fallback = fallback ?? throw new ArgumentNullException(nameof(fallback));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    public async Task<PokemonConnectionTestResult> TestConnectionAsync(CancellationToken cancellationToken)
    {
        try
        {
            var res = await _primary.TestConnectionAsync(cancellationToken).ConfigureAwait(false);
            if (res.IsSuccess) return res;

            return new PokemonConnectionTestResult
            {
                IsSuccess = true,
                Message = $"{res.Message} (Embedded Native Engine is active as fallback)",
                LatencyMs = res.LatencyMs,
                ProviderVersion = res.ProviderVersion
            };
        }
        catch (Exception ex)
        {
            return new PokemonConnectionTestResult
            {
                IsSuccess = true,
                Message = $"Primary provider error: {ex.Message} (Embedded Native Engine is active as fallback)",
                LatencyMs = 0
            };
        }
    }

    public async Task<PokemonProviderCapabilities> GetCapabilitiesAsync(CancellationToken cancellationToken)
    {
        try
        {
            return await _primary.GetCapabilitiesAsync(cancellationToken).ConfigureAwait(false);
        }
        catch
        {
            return await _fallback.GetCapabilitiesAsync(cancellationToken).ConfigureAwait(false);
        }
    }

    public async Task<PokemonSaveParseResult> ParseSaveAsync(
        byte[] saveBytes,
        string pokemonGameId,
        string platform,
        int generation,
        CancellationToken cancellationToken)
    {
        try
        {
            var result = await _primary.ParseSaveAsync(saveBytes, pokemonGameId, platform, generation, cancellationToken).ConfigureAwait(false);
            if (result.IsSuccess)
            {
                return result;
            }

            _logger.LogWarning("Primary Pokémon provider ({Primary}) failed parsing save for {GameId} ({Error}). Falling back to embedded native engine.",
                _primary.ProviderName, pokemonGameId, result.ErrorMessage);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Primary Pokémon provider ({Primary}) threw during ParseSaveAsync for {GameId}. Falling back to embedded native engine.",
                _primary.ProviderName, pokemonGameId);
        }

        return await _fallback.ParseSaveAsync(saveBytes, pokemonGameId, platform, generation, cancellationToken).ConfigureAwait(false);
    }

    public async Task<PokemonExtractResult> ExtractPokemonFromSaveAsync(
        byte[] saveBytes,
        string pokemonGameId,
        string platform,
        int generation,
        string pokemonId,
        bool isInParty,
        int? boxIndex,
        int slotIndex,
        CancellationToken cancellationToken)
    {
        try
        {
            var result = await _primary.ExtractPokemonFromSaveAsync(
                saveBytes, pokemonGameId, platform, generation, pokemonId, isInParty, boxIndex, slotIndex, cancellationToken).ConfigureAwait(false);
            if (result.IsSuccess && result.UpdatedSaveBytes != null && result.ExtractedPokemon != null)
            {
                return result;
            }

            _logger.LogWarning("Primary Pokémon provider ({Primary}) failed extraction for {GameId} ({Error}). Falling back to embedded native engine.",
                _primary.ProviderName, pokemonGameId, result.ErrorMessage);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Primary Pokémon provider ({Primary}) threw during ExtractPokemonFromSaveAsync for {GameId}. Falling back to embedded native engine.",
                _primary.ProviderName, pokemonGameId);
        }

        return await _fallback.ExtractPokemonFromSaveAsync(
            saveBytes, pokemonGameId, platform, generation, pokemonId, isInParty, boxIndex, slotIndex, cancellationToken).ConfigureAwait(false);
    }

    public async Task<PokemonInjectResult> InjectPokemonIntoSaveAsync(
        byte[] saveBytes,
        string pokemonGameId,
        string platform,
        int generation,
        PokemonVaultEntry entry,
        int? targetBoxIndex,
        int? targetSlotIndex,
        bool targetParty,
        CancellationToken cancellationToken)
    {
        try
        {
            var result = await _primary.InjectPokemonIntoSaveAsync(
                saveBytes, pokemonGameId, platform, generation, entry, targetBoxIndex, targetSlotIndex, targetParty, cancellationToken).ConfigureAwait(false);
            if (result.IsSuccess && result.UpdatedSaveBytes != null)
            {
                return result;
            }

            _logger.LogWarning("Primary Pokémon provider ({Primary}) failed injection for {GameId} ({Error}). Falling back to embedded native engine.",
                _primary.ProviderName, pokemonGameId, result.ErrorMessage);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Primary Pokémon provider ({Primary}) threw during InjectPokemonIntoSaveAsync for {GameId}. Falling back to embedded native engine.",
                _primary.ProviderName, pokemonGameId);
        }

        return await _fallback.InjectPokemonIntoSaveAsync(
            saveBytes, pokemonGameId, platform, generation, entry, targetBoxIndex, targetSlotIndex, targetParty, cancellationToken).ConfigureAwait(false);
    }
}
