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
            SupportedGenerations = new[] { "1", "2", "3", "4", "5", "6", "7" },
            SupportedPlatforms = new[] { "gb", "gbc", "gba", "nds", "3ds" }
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
            int expectedGen = generation;
            if (expectedGen <= 0)
            {
                expectedGen = InferGeneration(pokemonGameId, platform);
            }

            PokemonSaveParseResult result;
            switch (expectedGen)
            {
                case 1:
                    result = Gen1SaveParser.Parse(saveBytes, pokemonGameId, _catalog);
                    break;
                case 2:
                    result = Gen2SaveParser.Parse(saveBytes, pokemonGameId, _catalog);
                    break;
                case 3:
                    result = Gen3SaveParser.Parse(saveBytes, pokemonGameId, _catalog);
                    break;
                case 4:
                    result = Gen4SaveParser.Parse(saveBytes, pokemonGameId, _catalog);
                    break;
                case 5:
                    result = Gen5SaveParser.Parse(saveBytes, pokemonGameId, _catalog);
                    break;
                case 6:
                    result = Gen6SaveParser.Parse(saveBytes, pokemonGameId, _catalog);
                    break;
                case 7:
                    result = Gen7SaveParser.Parse(saveBytes, pokemonGameId, _catalog);
                    break;
                default:
                    result = Probe(saveBytes, pokemonGameId, platform);
                    break;
            }

            if (result != null && expectedGen > 0 && result.DetectedGeneration.HasValue && result.DetectedGeneration.Value != expectedGen)
            {
                return Task.FromResult(new PokemonSaveParseResult
                {
                    IsSuccess = false,
                    ErrorMessage = $"Save data generation ({result.DetectedGeneration.Value}) does not match expected Generation {expectedGen}."
                });
            }

            return Task.FromResult(result ?? new PokemonSaveParseResult
            {
                IsSuccess = false,
                ErrorMessage = "Could not parse Pokémon save data for this title."
            });
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

    private PokemonSaveParseResult Probe(byte[] saveBytes, string pokemonGameId, string platform)
    {
        var plat = (platform ?? string.Empty).ToLowerInvariant();
        if (plat is "3ds" or "n3ds" or "nintendo3ds")
        {
            if (Gen6SaveParser.IsGen6Save(saveBytes, pokemonGameId))
                return Gen6SaveParser.Parse(saveBytes, pokemonGameId, _catalog);
            if (Gen7SaveParser.IsGen7Save(saveBytes, pokemonGameId))
                return Gen7SaveParser.Parse(saveBytes, pokemonGameId, _catalog);
            return new PokemonSaveParseResult
            {
                IsSuccess = false,
                ErrorMessage = "3DS save data does not match known Gen 6 or Gen 7 formats."
            };
        }
        if (plat == "nds")
        {
            if (Gen4SaveParser.IsGen4Save(saveBytes, pokemonGameId))
                return Gen4SaveParser.Parse(saveBytes, pokemonGameId, _catalog);
            if (Gen5SaveParser.IsGen5Save(saveBytes, pokemonGameId))
                return Gen5SaveParser.Parse(saveBytes, pokemonGameId, _catalog);
            return new PokemonSaveParseResult
            {
                IsSuccess = false,
                ErrorMessage = "NDS save data does not match known Gen 4 or Gen 5 formats."
            };
        }
        if (plat == "gba")
        {
            if (Gen3SaveParser.IsGen3Save(saveBytes))
                return Gen3SaveParser.Parse(saveBytes, pokemonGameId, _catalog);
            return new PokemonSaveParseResult
            {
                IsSuccess = false,
                ErrorMessage = "GBA save data does not match known Gen 3 format."
            };
        }
        if (plat is "gbc" or "gb")
        {
            if (Gen2SaveParser.IsGen2Save(saveBytes))
                return Gen2SaveParser.Parse(saveBytes, pokemonGameId, _catalog);
            if (Gen1SaveParser.IsGen1Save(saveBytes))
                return Gen1SaveParser.Parse(saveBytes, pokemonGameId, _catalog);
            return new PokemonSaveParseResult
            {
                IsSuccess = false,
                ErrorMessage = "GB/GBC save data does not match known Gen 1 or Gen 2 format."
            };
        }

        if (Gen1SaveParser.IsGen1Save(saveBytes))
            return Gen1SaveParser.Parse(saveBytes, pokemonGameId, _catalog);
        if (Gen2SaveParser.IsGen2Save(saveBytes))
            return Gen2SaveParser.Parse(saveBytes, pokemonGameId, _catalog);
        if (Gen4SaveParser.IsGen4Save(saveBytes, pokemonGameId))
            return Gen4SaveParser.Parse(saveBytes, pokemonGameId, _catalog);
        if (Gen5SaveParser.IsGen5Save(saveBytes, pokemonGameId))
            return Gen5SaveParser.Parse(saveBytes, pokemonGameId, _catalog);
        if (Gen3SaveParser.IsGen3Save(saveBytes))
            return Gen3SaveParser.Parse(saveBytes, pokemonGameId, _catalog);
        if (Gen6SaveParser.IsGen6Save(saveBytes, pokemonGameId))
            return Gen6SaveParser.Parse(saveBytes, pokemonGameId, _catalog);
        if (Gen7SaveParser.IsGen7Save(saveBytes, pokemonGameId))
            return Gen7SaveParser.Parse(saveBytes, pokemonGameId, _catalog);

        return new PokemonSaveParseResult
        {
            IsSuccess = false,
            ErrorMessage = "Unrecognized Pokémon save file."
        };
    }

    private static int InferGeneration(string pokemonGameId, string platform)
    {
        var lower = (pokemonGameId ?? string.Empty).ToLowerInvariant();
        var tokens = lower.Split([' ', '_', '-', ':'], StringSplitOptions.RemoveEmptyEntries);

        if (lower.Contains("ultra sun", StringComparison.Ordinal) || lower.Contains("ultra moon", StringComparison.Ordinal) ||
            lower.Contains("usum", StringComparison.Ordinal) || lower.Contains("pokemon sun", StringComparison.Ordinal) ||
            lower.Contains("pokemon moon", StringComparison.Ordinal) ||
            Array.Exists(tokens, t => t is "sun" or "moon"))
        {
            return 7;
        }

        if (lower.Contains("omega ruby", StringComparison.Ordinal) || lower.Contains("alpha sapphire", StringComparison.Ordinal) ||
            lower.Contains("oras", StringComparison.Ordinal) || lower.Contains("pokemon x", StringComparison.Ordinal) ||
            lower.Contains("pokemon y", StringComparison.Ordinal) ||
            Array.Exists(tokens, t => t is "x" or "y"))
        {
            return 6;
        }

        if (lower.Contains("heartgold") || lower.Contains("soulsilver") || lower.Contains("heart gold") ||
            lower.Contains("soul silver") || lower.Contains("hgss") || lower.Contains("diamond") ||
            lower.Contains("pearl") || lower.Contains("platinum"))
        {
            return 4;
        }
        if (lower.Contains("black") || lower.Contains("white") || lower.Contains("b2w2") ||
            lower.Contains("black 2") || lower.Contains("white 2"))
        {
            return 5;
        }
        if (lower.Contains("firered") || lower.Contains("leafgreen") || lower.Contains("fire red") ||
            lower.Contains("leaf green") || lower.Contains("emerald") || lower.Contains("ruby") ||
            lower.Contains("sapphire"))
        {
            return 3;
        }
        if (lower.Contains("crystal") || lower.Contains("gold") || lower.Contains("silver"))
        {
            return 2;
        }
        if (lower.Contains("red") || lower.Contains("blue") || lower.Contains("yellow") || lower.Contains("pikachu"))
        {
            return 1;
        }

        var plat = (platform ?? string.Empty).ToLowerInvariant();
        if (plat is "3ds" or "n3ds" or "nintendo3ds") return 6;
        if (plat == "gba") return 3;
        return 0;
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
        int effectiveGen = generation > 0 ? generation : InferGeneration(pokemonGameId, platform);
        if (effectiveGen != 3)
        {
            return Task.FromResult(new PokemonExtractResult
            {
                IsSuccess = false,
                ErrorMessage = $"Native Pokémon extraction is currently only supported for Generation 3 (got Generation {effectiveGen})."
            });
        }

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
        int effectiveGen = generation > 0 ? generation : InferGeneration(pokemonGameId, platform);
        if (effectiveGen != 3)
        {
            return Task.FromResult(new PokemonInjectResult
            {
                IsSuccess = false,
                ErrorMessage = $"Native Pokémon injection is currently only supported for Generation 3 (got Generation {effectiveGen})."
            });
        }

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
