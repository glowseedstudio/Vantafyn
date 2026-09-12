package dev.vantafyn.core.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dev.vantafyn.core.jellyfin.JellyfinSession
import java.io.File

@OptIn(UnstableApi::class)
object VantafynMediaCache {
    private const val CACHE_DIR_NAME = "vantafyn_music_media_cache"
    const val DEFAULT_MAX_CACHE_BYTES = 512L * 1024L * 1024L // 512 MB LRU disk cache

    @Volatile
    private var simpleCache: SimpleCache? = null

    @Volatile
    private var databaseProvider: StandaloneDatabaseProvider? = null

    @Volatile
    var authHeaderProvider: (() -> Map<String, String>)? = null

    @Volatile
    private var fallbackToken: String? = null

    @Volatile
    private var fallbackDeviceId: String? = null

    @Volatile
    var currentRemoteServerUrl: String? = null
        private set

    @Volatile
    var currentLocalServerUrl: String? = null
        private set

    @Volatile
    var currentAccessToken: String? = null
        private set

    @Volatile
    var currentUserId: java.util.UUID? = null
        private set

    fun setFallbackCredentials(token: String?, deviceId: String? = null) {
        if (!token.isNullOrBlank()) {
            fallbackToken = token
            currentAccessToken = token
        }
        if (!deviceId.isNullOrBlank()) {
            fallbackDeviceId = deviceId
        }
    }

    fun updateJellyfinSession(session: JellyfinSession?) {
        if (session == null) {
            authHeaderProvider = null
            fallbackToken = null
            fallbackDeviceId = null
            currentRemoteServerUrl = null
            currentLocalServerUrl = null
            currentAccessToken = null
            currentUserId = null
        } else {
            val token = session.accessToken
            val devId = session.server.localId.ifBlank { "vantafyn-android" }
            fallbackToken = token
            fallbackDeviceId = devId
            currentRemoteServerUrl = session.server.remoteUrl
            currentLocalServerUrl = session.server.localUrl ?: session.server.url
            currentAccessToken = token
            currentUserId = session.user.id
            authHeaderProvider = {
                val headers = mutableMapOf<String, String>()
                if (token.isNotBlank()) {
                    headers["X-Emby-Token"] = token
                    val clientAuth = "MediaBrowser Client=\"Vantafyn\", Device=\"Android\", DeviceId=\"$devId\", Version=\"1.0.0\", Token=\"$token\""
                    headers["Authorization"] = clientAuth
                    headers["X-Emby-Authorization"] = clientAuth
                }
                headers
            }
        }
    }

    @Synchronized
    fun getSimpleCache(context: Context, maxCacheBytes: Long = DEFAULT_MAX_CACHE_BYTES): SimpleCache {
        val appContext = context.applicationContext
        return simpleCache ?: run {
            val cacheDir = File(appContext.cacheDir, CACHE_DIR_NAME).apply { mkdirs() }
            val evictor = LeastRecentlyUsedCacheEvictor(maxCacheBytes)
            val dbProvider = databaseProvider ?: StandaloneDatabaseProvider(appContext).also {
                databaseProvider = it
            }
            SimpleCache(cacheDir, evictor, dbProvider).also { simpleCache = it }
        }
    }

    fun getHttpDataSourceFactory(): HttpDataSource.Factory {
        val baseFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("Vantafyn-Android/${android.os.Build.VERSION.RELEASE}")

        return object : HttpDataSource.Factory {
            override fun createDataSource(): HttpDataSource {
                val dynamicHeaders = authHeaderProvider?.invoke().orEmpty().toMutableMap()
                if (dynamicHeaders.isEmpty() && !fallbackToken.isNullOrBlank()) {
                    val token = fallbackToken.orEmpty()
                    val devId = fallbackDeviceId ?: "vantafyn-android"
                    dynamicHeaders["X-Emby-Token"] = token
                    val clientAuth = "MediaBrowser Client=\"Vantafyn\", Device=\"Android\", DeviceId=\"$devId\", Version=\"1.0.0\", Token=\"$token\""
                    dynamicHeaders["Authorization"] = clientAuth
                    dynamicHeaders["X-Emby-Authorization"] = clientAuth
                }
                if (dynamicHeaders.isNotEmpty()) {
                    baseFactory.setDefaultRequestProperties(dynamicHeaders)
                }
                return baseFactory.createDataSource()
            }

            override fun setDefaultRequestProperties(defaultRequestProperties: Map<String, String>): HttpDataSource.Factory {
                baseFactory.setDefaultRequestProperties(defaultRequestProperties)
                return this
            }
        }
    }

    fun getCacheDataSourceFactory(context: Context): CacheDataSource.Factory {
        val appContext = context.applicationContext
        val cache = getSimpleCache(appContext)
        val httpDataSourceFactory = getHttpDataSourceFactory()
        val defaultUpstreamFactory = DefaultDataSource.Factory(appContext, httpDataSourceFactory)

        val cacheWriteSinkFactory = CacheDataSink.Factory()
            .setCache(cache)
            .setFragmentSize(CacheDataSink.DEFAULT_FRAGMENT_SIZE)

        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(defaultUpstreamFactory)
            .setCacheWriteDataSinkFactory(cacheWriteSinkFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    @Synchronized
    fun release() {
        simpleCache?.release()
        simpleCache = null
        databaseProvider = null
    }
}

