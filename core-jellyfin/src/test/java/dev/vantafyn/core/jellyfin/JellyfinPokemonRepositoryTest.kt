package dev.vantafyn.core.jellyfin

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JellyfinPokemonRepositoryTest {

    private val testSession = JellyfinSession(
        server = JellyfinServerConfig(url = "https://jellyfin.example.com"),
        user = JellyfinUser(id = UUID.randomUUID(), name = "AshKetchum"),
        profileId = "profile-pk-123",
        accessToken = "vault-token-xyz",
    )

    private val repo = DefaultJellyfinPokemonRepository()

    @Test
    fun pokemonModels_instantiationAndDefaults() {
        val status = PokemonIntegrationStatus(
            enabled = true,
            provider = "pkvault",
            providerHealthy = true,
            supportedGenerations = listOf(1, 2, 3, 4, 5),
            vaultAvailable = true,
            transfersAvailable = true,
            crossGenerationAvailable = true,
        )
        assertTrue(status.enabled)
        assertTrue(status.vaultAvailable)
        assertEquals(5, status.supportedGenerations.size)

        val summary = PokemonVaultSummary(
            totalCapacity = 900,
            totalOccupied = 42,
            boxCount = 30,
            shinyCount = 3,
            speciesCount = 35,
        )
        assertEquals(900, summary.totalCapacity)
        assertEquals(42, summary.totalOccupied)
        assertEquals(3, summary.shinyCount)

        val entry = PokemonVaultEntry(
            id = "entry-001",
            boxIndex = 1,
            slotIndex = 4,
            species = "Charizard",
            speciesId = 6,
            nickname = "Flame",
            level = 50,
            isShiny = true,
            generation = 3,
            originalTrainer = "Red",
            originGame = "FireRed",
        )
        assertEquals("Charizard", entry.species)
        assertTrue(entry.isShiny)
        assertEquals(1, entry.boxIndex)
        assertEquals(4, entry.slotIndex)

        val box = PokemonVaultBox(
            boxIndex = 1,
            name = "Kanto Champions",
            entries = listOf(entry),
        )
        assertEquals(1, box.entries.size)
        assertEquals("Kanto Champions", box.name)

        val lockState = SaveLockStateDto(
            isLocked = true,
            lockReason = "Active gameplay session in progress",
            activeSessionCount = 1,
            activeClients = listOf("Vantafyn Mobile (Pixel 8)"),
        )
        assertTrue(lockState.isLocked)
        assertEquals(1, lockState.activeSessionCount)
    }

    @Test
    fun pokemonOperationResponse_andValidationModels() {
        val opResponse = PokemonOperationResponse(
            isSuccess = true,
            operationType = "Deposit",
            message = "Deposited Charizard to Vault Box 1, Slot 4",
            transactionId = "tx-12345",
            backupCreated = true,
            backupId = "bak-987",
        )
        assertTrue(opResponse.isSuccess)
        assertEquals("Deposit", opResponse.operationType)
        assertTrue(opResponse.backupCreated)

        val compat = PokemonTransferCompatibilityResult(
            isCompatible = true,
            reason = "Pal Park transfer supported",
            isOneWay = true,
            requiresItemRemoval = true,
            allowedGenerations = listOf(3, 4),
        )
        assertTrue(compat.isCompatible)
        assertTrue(compat.isOneWay)
        assertTrue(compat.requiresItemRemoval)
    }

    @Test
    fun pokemonTradeModels_instantiationAndDefaults() {
        val offer = PokemonTradeOffer(
            pokemonId = "char-1",
            species = "Charizard",
            speciesId = 6,
            nickname = "Flame",
            level = 50,
            isShiny = true,
            generation = 3,
            isVault = true,
            boxIndex = 1,
            slotIndex = 1,
        )
        assertEquals("Charizard", offer.species)
        assertTrue(offer.isShiny)

        val session = PokemonTradeSession(
            id = "trade-100",
            type = PokemonTradeType.LinkCode,
            status = PokemonTradeStatus.Pending,
            initiatorUserId = "user-1",
            initiatorUserName = "Red",
            linkCode = "123456",
            initiatorOffer = offer,
            createdAtUtc = "2026-09-30T10:00:00Z",
        )
        assertEquals("trade-100", session.id)
        assertEquals(PokemonTradeType.LinkCode, session.type)
        assertEquals(PokemonTradeStatus.Pending, session.status)
        assertEquals("123456", session.linkCode)

        val response = PokemonTradeOperationResponse(
            isSuccess = true,
            message = "Trade created successfully",
            tradeSession = session,
            transactionId = "tx-trade-1",
        )
        assertTrue(response.isSuccess)
        assertNotNull(response.tradeSession)
        assertEquals("tx-trade-1", response.transactionId)
    }

    @Test
    fun pokemonBackupAndDiagnosticsModels_instantiation() {
        val backup = PokemonBackupDto(
            backupId = "bak-01",
            gameId = "emerald",
            createdAtUtc = "2026-09-30T12:00:00Z",
            reason = "Pre-transfer snapshot",
            sizeBytes = 131072L,
            isRestored = false,
        )
        assertEquals("bak-01", backup.backupId)
        assertEquals("emerald", backup.gameId)
        assertFalse(backup.isRestored)

        val restoreRes = RestoreBackupResponse(
            isSuccess = true,
            message = "Restored save file",
            backupId = "bak-01",
            gameId = "emerald",
        )
        assertTrue(restoreRes.isSuccess)

        val diag = PokemonDiagnosticsDto(
            enabled = true,
            providerType = "pkvault",
            providerHealthy = true,
            supportedGenerations = listOf("1", "2", "3", "4", "5"),
            totalStoredPokemon = 120,
            totalShinyPokemon = 5,
            totalBackups = 14,
            activeSessions = 0,
            activeTrades = 1,
        )
        assertTrue(diag.enabled)
        assertTrue(diag.providerHealthy)
        assertEquals(5, diag.supportedGenerations.size)
        assertEquals(120, diag.totalStoredPokemon)
    }

    @Test
    fun pokedexAndJourneyModels_instantiation() {
        val step = PokemonJourneyStepDto(
            timestamp = "2026-09-30T10:00:00Z",
            action = "Deposit",
            sourceLocation = "FireRed (Box 1 Slot 1)",
            destinationLocation = "Personal Vault (Box 1 Slot 1)",
            details = "Deposited to vault",
            gameTitle = "Pokémon FireRed",
            generation = 3,
        )
        val journey = PokemonJourneyDto(
            pokemonId = "char-123",
            species = "Charizard",
            speciesId = 6,
            nickname = "Flame",
            level = 50,
            isShiny = true,
            originGame = "Pokémon FireRed",
            originalTrainer = "Red",
            steps = listOf(step),
        )
        assertEquals("char-123", journey.pokemonId)
        assertEquals(1, journey.steps.size)
        assertEquals("Deposit", journey.steps[0].action)

        val entry = PokemonPokedexEntryDto(
            speciesId = 6,
            speciesName = "Charizard",
            generation = 1,
            isCaught = true,
            isSeen = true,
            hasShiny = true,
            firstEncounteredGame = "Pokémon FireRed",
            encounterCount = 2,
        )
        val progress = PokemonPokedexGenerationProgressDto(
            generation = 1,
            generationName = "Gen I (Kanto)",
            minDexNumber = 1,
            maxDexNumber = 151,
            totalSpecies = 151,
            caughtCount = 1,
            seenCount = 1,
            shinyCount = 1,
            caughtPercentage = 0.7,
        )
        val dex = PokemonPokedexDto(
            userId = "user-123",
            isEnabled = true,
            totalCaught = 1,
            totalSeen = 1,
            totalShinies = 1,
            generationProgress = listOf(progress),
            entries = listOf(entry),
        )
        assertTrue(dex.isEnabled)
        assertEquals(1, dex.totalCaught)
        assertEquals(1, dex.entries.size)
        assertEquals("Charizard", dex.entries[0].speciesName)
        assertEquals(0.7, dex.generationProgress[0].caughtPercentage, 0.01)
    }

    @Test
    fun achievementsAndSocialModels_instantiation() {
        val achievement = PokemonAchievementDto(
            id = "pk-vault-first-deposit",
            title = "Vault Initiate",
            description = "Deposited your first Pokémon into the personal cloud vault.",
            category = "Pokemon",
            rarity = "Common",
            score = 100,
            iconName = "cloud_done",
            isUnlocked = true,
            unlockedAtUtc = "2026-09-30T10:00:00Z",
            currentProgress = 1,
            maxProgress = 1,
            progressPercentage = 100.0,
        )
        val summary = PokemonAchievementsSummaryDto(
            userId = "user-123",
            totalScore = 100,
            unlockedCount = 1,
            totalCount = 10,
            achievements = listOf(achievement),
        )
        assertEquals("user-123", summary.userId)
        assertEquals(100, summary.totalScore)
        assertEquals(1, summary.unlockedCount)
        assertEquals(10, summary.totalCount)
        assertTrue(summary.achievements[0].isUnlocked)
        assertEquals("Vault Initiate", summary.achievements[0].title)

        val activity = PokemonSocialActivityEvent(
            id = "act-1",
            userId = "user-123",
            userName = "Ash",
            eventType = "PokemonDeposited",
            title = "Pikachu Deposited to Vault",
            description = "Ash safely stored a Pikachu (Lv. 25) into their Personal Cloud Vault.",
            speciesId = 25,
            speciesName = "Pikachu",
            isShiny = false,
            timestampUtc = "2026-09-30T10:05:00Z",
        )
        assertEquals("act-1", activity.id)
        assertEquals("Ash", activity.userName)
        assertEquals("PokemonDeposited", activity.eventType)
        assertEquals("Pikachu", activity.speciesName)
    }
}


