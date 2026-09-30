namespace Vantafyn.Plugin.Companion.Pokemon;

/// <summary>
/// Domain validation logic for direct Pokémon transfers and cross-generation migrations.
/// Enforces mainline compatibility rules, time-capsule constraints, and server configuration limits.
/// </summary>
public static class PokemonMigrationValidator
{
    public static PokemonTransferCompatibilityResult ValidateTransfer(
        GamePokemonMetadata sourceMeta,
        GamePokemonMetadata destMeta,
        PokemonConfiguration config,
        PokemonDetailsDto? details = null)
    {
        var result = new PokemonTransferCompatibilityResult
        {
            SourceGeneration = sourceMeta.Generation,
            DestinationGeneration = destMeta.Generation
        };

        if (!config.AllowTransfers)
        {
            result.IsCompatible = false;
            result.Direction = "disabled";
            result.Reason = "Direct Pokémon transfers are disabled in server configuration.";
            return result;
        }

        if (!sourceMeta.VaultSupported)
        {
            result.IsCompatible = false;
            result.Direction = "unsupported";
            result.Reason = $"Source game '{sourceMeta.CanonicalTitle}' is not supported for save transfers.";
            return result;
        }

        if (!destMeta.VaultSupported)
        {
            result.IsCompatible = false;
            result.Direction = "unsupported";
            result.Reason = $"Destination game '{destMeta.CanonicalTitle}' is not supported for save transfers.";
            return result;
        }

        // Case 1: Same Generation
        if (sourceMeta.Generation == destMeta.Generation)
        {
            result.IsCompatible = true;
            result.IsCrossGeneration = false;
            result.Direction = "same_generation";
            result.Reason = $"Direct same-generation transfer between {sourceMeta.CanonicalTitle} and {destMeta.CanonicalTitle} (Gen {sourceMeta.Generation}) is supported.";
            return result;
        }

        // Case 2: Cross-Generation
        result.IsCrossGeneration = true;

        if (!config.AllowCrossGenerationTransfers)
        {
            result.IsCompatible = false;
            result.Direction = sourceMeta.Generation < destMeta.Generation ? "forward_migration" : "unsupported_backward";
            result.Reason = "Cross-generation transfers are disabled in server configuration.";
            return result;
        }

        // Forward Migration (sourceGen < destGen)
        if (sourceMeta.Generation < destMeta.Generation)
        {
            result.Direction = "forward_migration";
            result.IsCompatible = true;

            if (sourceMeta.Generation == 3 && destMeta.Generation == 4)
            {
                result.Reason = $"Pal Park forward migration from {sourceMeta.CanonicalTitle} (Gen 3) to {destMeta.CanonicalTitle} (Gen 4) is supported. This is a one-way migration.";
                result.Warnings.Add("Pal Park migration is permanent; Pokémon cannot return to Generation 3.");
            }
            else if (sourceMeta.Generation == 4 && destMeta.Generation == 5)
            {
                result.Reason = $"Poké Transfer forward migration from {sourceMeta.CanonicalTitle} (Gen 4) to {destMeta.CanonicalTitle} (Gen 5) is supported. This is a one-way migration.";
                result.Warnings.Add("Poké Transfer migration is permanent; Pokémon cannot return to Generation 4.");
                result.Warnings.Add("Held items and HM moves may be stripped during Poké Transfer.");
            }
            else if (sourceMeta.Generation == 1 && destMeta.Generation == 2)
            {
                result.Reason = $"Time Capsule forward migration from {sourceMeta.CanonicalTitle} (Gen 1) to {destMeta.CanonicalTitle} (Gen 2) is supported.";
            }
            else
            {
                result.Reason = $"Forward migration from {sourceMeta.CanonicalTitle} (Gen {sourceMeta.Generation}) to {destMeta.CanonicalTitle} (Gen {destMeta.Generation}) is supported. This is a one-way migration.";
                result.Warnings.Add("Forward generation migration is permanent.");
            }

            return result;
        }

        // Backward Transfer (sourceGen > destGen)
        // Exception: Gen 2 -> Gen 1 (Time Capsule)
        if (sourceMeta.Generation == 2 && destMeta.Generation == 1)
        {
            result.Direction = "time_capsule_backward";
            if (details != null)
            {
                if (details.Summary.SpeciesId > 151)
                {
                    result.IsCompatible = false;
                    result.Reason = $"Generation 2 species #{details.Summary.SpeciesId} ({details.Summary.Species}) does not exist in Generation 1.";
                    return result;
                }

                if (details.Moves.Count > 0)
                {
                    result.Warnings.Add("Any Generation 2 specific moves or held items must be compatible with Generation 1.");
                }
            }

            result.IsCompatible = true;
            result.Reason = "Time Capsule backward transfer from Generation 2 to Generation 1 is supported for the original 151 Pokémon without Gen 2 exclusive moves.";
            return result;
        }

        // All other backward transfers are strictly prohibited in mainline Pokémon
        result.IsCompatible = false;
        result.Direction = "unsupported_backward";
        result.Reason = $"Backward transfers from Generation {sourceMeta.Generation} ({sourceMeta.CanonicalTitle}) to Generation {destMeta.Generation} ({destMeta.CanonicalTitle}) are not supported.";
        return result;
    }
}
