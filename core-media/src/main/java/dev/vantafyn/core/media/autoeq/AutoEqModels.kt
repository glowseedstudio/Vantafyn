package dev.vantafyn.core.media.autoeq

/**
 * Headphone EQ profile model from the AutoEQ project.
 */
data class AutoEqPreset(
    val id: String,
    val name: String,
    val brand: String,
    val type: String,
    val source: String,
    val preamp: Float,
    val frequencies: List<Int>,
    val gains: List<Float>,
)

/**
 * Represents a single physical/hardware EQ band on the Android device.
 */
data class EqualizerBandInfo(
    val bandIndex: Short,
    val centerFreqHz: Int,
    val minLevelMb: Short,
    val maxLevelMb: Short,
    val currentLevelMb: Short,
) {
    val currentGainDb: Float
        get() = currentLevelMb / 100f
}

/**
 * Indicates hardware equalizer capability and health.
 */
sealed interface AutoEqHardwareStatus {
    data object Supported : AutoEqHardwareStatus
    data class Unsupported(val reason: String) : AutoEqHardwareStatus
}

/**
 * Reactive state of AutoEQ system.
 */
data class AutoEqState(
    val isEnabled: Boolean = false,
    val selectedPreset: AutoEqPreset? = null,
    val hardwareStatus: AutoEqHardwareStatus = AutoEqHardwareStatus.Supported,
    val hardwareBands: List<EqualizerBandInfo> = emptyList(),
)
