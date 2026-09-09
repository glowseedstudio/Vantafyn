package dev.vantafyn.core.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
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

    fun getHttpDataSourceFactory(): DefaultHttpDataSource.Factory {
        val factory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("Vantafyn-Android/${android.os.Build.VERSION.RELEASE}")

        authHeaderProvider?.invoke()?.let { headers ->
            if (headers.isNotEmpty()) {
                factory.setDefaultRequestProperties(headers)
            }
        }
        return factory
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

