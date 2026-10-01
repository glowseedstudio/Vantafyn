using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;
using Vantafyn.Plugin.Companion.Pokemon.PkVault;

namespace Vantafyn.Plugin.Companion.Pokemon.Native;

/// <summary>
/// Native Pokémon provider running entirely in-process on the Jellyfin server.
/// Eliminates any external dependencies, Docker containers, or network calls for Pokémon save parsing.
/// </summary>
public sealed class NativePokemonProvider : IPokemonProvider
{
    private readonly PkVaultStaticCatalog _catalog = new();
    private readonly ILogger<NativePokemonProvider> _logger;

    public string ProviderName => "native";

    public NativePokemonProvider(ILogger<NativePokemonProvider>? logger = null)
    {
        _logger = logger ?? NullLogger<NativePokemonProvider>.Instance;
    }

    public Task<PokemonConnectionTestResult> TestConnectionAsync(CancellationToken cancellationToken)
    {
        return Task.FromResult(new PokemonConnectionTestResult
        {
            IsSuccess = true,
            Message = "Vantafyn Native Pokémon Save Engine (Embedded)",
            LatencyMs = 0
        });
    }

    public Task<PokemonProviderCapabilities> GetCapabilitiesAsync(CancellationToken cancellationToken)
    {
        return Task.FromResult(new PokemonProviderCapabilities
        {
            CanReadSaves = true,
            CanWriteSaves = true,
            CanTransferSameGeneration = true,
            CanTransferCrossGeneration = true,
            CanValidateLegality = true,
            SupportedGenerations = new[] { "1", "2", "3", "4", "5" },
            SupportedPlatforms = new[] { "gb", "gbc", "gba", "nds" }
        });
    }

    public Task<PokemonSaveParseResult> ParseSaveAsync(
        byte[] saveBytes,
        string pokemonGameId,
        string platform,
        int generation,
        CancellationToken cancellationToken)
    {
        if (saveBytes == null || saveBytes.Length == 0)
        {
            return Task.FromResult(new PokemonSaveParseResult
            {
                IsSuccess = false,
                ErrorMessage = "Save data is empty or missing."
            });
        }

        try
        {
            if (Gen1SaveParser.IsGen1Save(saveBytes))
            {
                var result = Gen1SaveParser.Parse(saveBytes, pokemonGameId, _catalog);
                return Task.FromResult(result);
            }
            else if (Gen2SaveParser.IsGen2Save(saveBytes))
            {
                var result = Gen2SaveParser.Parse(saveBytes, pokemonGameId, _catalog);
                return Task.FromResult(result);
            }
            else if (Gen4SaveParser.IsGen4Save(saveBytes, pokemonGameId))
            {
                var result = Gen4SaveParser.Parse(saveBytes, pokemonGameId, _catalog);
                return Task.FromResult(result);
            }
            else if (Gen5SaveParser.IsGen5Save(saveBytes, pokemonGameId))
            {
                var result = Gen5SaveParser.Parse(saveBytes, pokemonGameId, _catalog);
                return Task.FromResult(result);
            }
            else
            {
                var result = Gen3SaveParser.Parse(saveBytes, pokemonGameId, _catalog);
                return Task.FromResult(result);
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error parsing Pokémon save file with native parser for {GameId}", pokemonGameId);
            return Task.FromResult(new PokemonSaveParseResult
            {
                IsSuccess = false,
                ErrorMessage = $"Native save parsing error: {ex.Message}"
            });
        }
    }

    public Task<PokemonExtractResult> ExtractPokemonFromSaveAsync(
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
            var parseResult = Gen3SaveParser.Parse(saveBytes, pokemonGameId, _catalog);
            var targetPkm = parseResult.Details.Values.FirstOrDefault(d =>
                d.Summary.Id == pokemonId ||
                (d.Summary.IsInParty == isInParty && d.Summary.BoxIndex == boxIndex && d.Summary.SlotIndex == slotIndex));

            var updatedBytes = Gen3SaveParser.ExtractPokemon(saveBytes, isInParty, boxIndex, slotIndex);

            return Task.FromResult(new PokemonExtractResult
            {
                IsSuccess = true,
                UpdatedSaveBytes = updatedBytes,
                ExtractedPokemon = targetPkm
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error extracting Pokémon from save with native engine");
            return Task.FromResult(new PokemonExtractResult
            {
                IsSuccess = false,
                ErrorMessage = $"Native extract failed: {ex.Message}"
            });
        }
    }

    public Task<PokemonInjectResult> InjectPokemonIntoSaveAsync(
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
            int box = targetBoxIndex ?? 1;
            int slot = targetSlotIndex ?? 1;
            var updatedBytes = Gen3SaveParser.InjectPokemon(saveBytes, entry, box, slot);

            return Task.FromResult(new PokemonInjectResult
            {
                IsSuccess = true,
                UpdatedSaveBytes = updatedBytes,
                AssignedLocation = $"Box {box} Slot {slot}"
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error injecting Pokémon into save with native engine");
            return Task.FromResult(new PokemonInjectResult
            {
                IsSuccess = false,
                ErrorMessage = $"Native injection failed: {ex.Message}"
            });
        }
    }
}
