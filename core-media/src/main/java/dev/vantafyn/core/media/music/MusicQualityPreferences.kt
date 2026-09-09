package dev.vantafyn.core.media.music

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object MusicQualityPreferences {
    private const val PREFS_NAME = "vantafyn_music_quality_prefs"
    private const val KEY_WIFI_QUALITY = "music_quality_wifi"
    private const val KEY_CELLULAR_QUALITY = "music_quality_cellular"
    private const val KEY_AUTO_SWITCH = "music_quality_auto_switch"

    private val _wifiQualityFlow = MutableStateFlow(MusicStreamingQuality.DefaultWifi)
    val wifiQualityFlow: StateFlow<MusicStreamingQuality> = _wifiQualityFlow.asStateFlow()

    private val _cellularQualityFlow = MutableStateFlow(MusicStreamingQuality.DefaultCellular)
    val cellularQualityFlow: StateFlow<MusicStreamingQuality> = _cellularQualityFlow.asStateFlow()

    private val _autoSwitchFlow = MutableStateFlow(true)
    val autoSwitchFlow: StateFlow<Boolean> = _autoSwitchFlow.asStateFlow()

    fun getWifiQuality(context: Context): MusicStreamingQuality {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_WIFI_QUALITY, MusicStreamingQuality.DefaultWifi.name)
        val quality = MusicStreamingQuality.entries.firstOrNull { it.name == name } ?: MusicStreamingQuality.DefaultWifi
        _wifiQualityFlow.value = quality
        return quality
    }

    fun setWifiQuality(context: Context, quality: MusicStreamingQuality) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_WIFI_QUALITY, quality.name).apply()
        _wifiQualityFlow.value = quality
    }

    fun getCellularQuality(context: Context): MusicStreamingQuality {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_CELLULAR_QUALITY, MusicStreamingQuality.DefaultCellular.name)
        val quality = MusicStreamingQuality.entries.firstOrNull { it.name == name } ?: MusicStreamingQuality.DefaultCellular
        _cellularQualityFlow.value = quality
        return quality
    }

    fun setCellularQuality(context: Context, quality: MusicStreamingQuality) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CELLULAR_QUALITY, quality.name).apply()
        _cellularQualityFlow.value = quality
    }

    fun isAutoSwitchEnabled(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_AUTO_SWITCH, true)
        _autoSwitchFlow.value = enabled
        return enabled
    }

    fun setAutoSwitchEnabled(context: Context, enabled: Boolean) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_SWITCH, enabled).apply()
        _autoSwitchFlow.value = enabled
    }

    fun isMeteredOrCellular(context: Context): Boolean {
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        val isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        val isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) || cm.isActiveNetworkMetered
        return isCellular || isMetered
    }

    fun resolveCurrentQuality(context: Context): MusicStreamingQuality {
        val appContext = context.applicationContext
        val autoSwitch = isAutoSwitchEnabled(appContext)
        val wifiQuality = getWifiQuality(appContext)
        val cellularQuality = getCellularQuality(appContext)

        if (!autoSwitch) {
            return wifiQuality
        }

        return if (isMeteredOrCellular(appContext)) {
            cellularQuality
        } else {
            wifiQuality
        }
    }

    fun setActiveQuality(context: Context, quality: MusicStreamingQuality) {
        val appContext = context.applicationContext
        if (isMeteredOrCellular(appContext)) {
            setCellularQuality(appContext, quality)
        } else {
            setWifiQuality(appContext, quality)
        }
    }

    fun rewriteStreamUrl(originalUrl: String, quality: MusicStreamingQuality): String {
        if (originalUrl.isBlank() || originalUrl.startsWith("file:") || originalUrl.startsWith("content:")) {
            return originalUrl
        }
        val uri = runCatching { android.net.Uri.parse(originalUrl) }.getOrNull() ?: return originalUrl
        val scheme = uri.scheme
        if (scheme != "http" && scheme != "https") {
            return originalUrl
        }

        val maxKbps = quality.maxBitrateKbps
        val maxBps = quality.maxBitrateBps

        val isSubsonic = originalUrl.contains("/rest/stream.view") || originalUrl.contains("stream.view")
        val isJellyfin = originalUrl.contains("/Audio/", ignoreCase = true) || originalUrl.contains("/Videos/", ignoreCase = true)

        if (!isSubsonic && !isJellyfin) {
            return originalUrl
        }

        val builder = uri.buildUpon().clearQuery()
        val paramNames = runCatching { uri.queryParameterNames }.getOrNull() ?: emptySet()

        for (name in paramNames) {
            if (isSubsonic && name.equals("maxBitRate", ignoreCase = true)) {
                continue
            }
            if (isJellyfin && (name.equals("maxStreamingBitrate", ignoreCase = true) || name.equals("audioBitRate", ignoreCase = true))) {
                continue
            }
            for (value in uri.getQueryParameters(name)) {
                builder.appendQueryParameter(name, value)
            }
        }

        if (isSubsonic && maxKbps != null) {
            builder.appendQueryParameter("maxBitRate", maxKbps.toString())
        }
        if (isJellyfin && maxBps != null) {
            builder.appendQueryParameter("maxStreamingBitrate", maxBps.toString())
            builder.appendQueryParameter("audioBitRate", maxBps.toString())
        }

        return builder.build().toString()
    }
}

