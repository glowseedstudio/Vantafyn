package dev.vantafyn.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.LruCache
import dev.vantafyn.core.jellyfin.JellyfinRepositoryProvider
import dev.vantafyn.core.jellyfin.withAccessToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Universal artwork caching, pre-fetching, and endpoint-fallback loader for Vantafyn media playback.
 *
 * Handles:
 *  - High-performance LruCache in-memory caching.
 *  - Persistent disk caching (`vantafyn_artwork_cache`) for seamless offline & cellular playback.
 *  - Automatic endpoint fallback: rewrites private LAN IP URLs (e.g. 192.168.x.x) to `remoteServerUrl`
 *    when on cellular data or when the local address is unreachable.
 *  - Jellyfin authentication token injection (`X-Emby-Token`, `Authorization`, and `withAccessToken`)
 *    for remote servers and reverse proxies.
 */
object VantafynArtworkLoader {
    private const val TAG = "VantafynArtworkLoader"
    private const val DISK_CACHE_DIR = "vantafyn_artwork_cache"
    private const val MAX_DISK_CACHE_BYTES = 100L * 1024L * 1024L // 100 MB
    private const val DEFAULT_MAX_DIMENSION = 1024

    private val memoryCache = object : LruCache<String, Bitmap>(32) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    fun getCachedBitmap(context: Context, artworkUrl: String?, maxDimension: Int = DEFAULT_MAX_DIMENSION): Bitmap? {
        if (artworkUrl.isNullOrBlank()) return null
        val cacheKey = getCacheKey(artworkUrl)

        synchronized(memoryCache) {
            memoryCache.get(cacheKey)?.let { return it }
        }

        val cacheFile = getDiskCacheFile(context, cacheKey)
        if (cacheFile.exists() && cacheFile.length() > 0) {
            runCatching {
                BitmapFactory.decodeFile(cacheFile.absolutePath)?.let { raw ->
                    val scaled = scaleBitmap(raw, maxDimension)
                    synchronized(memoryCache) {
                        memoryCache.put(cacheKey, scaled)
                    }
                    return scaled
                }
            }.onFailure { e ->
                Log.w(TAG, "Failed to decode cached artwork $cacheKey: ${e.message}")
            }
        }

        return null
    }

    suspend fun preCacheArtwork(context: Context, artworkUrl: String?, maxDimension: Int = DEFAULT_MAX_DIMENSION): Boolean {
        if (artworkUrl.isNullOrBlank()) return false
        return withContext(Dispatchers.IO) {
            loadArtworkBitmap(context, artworkUrl, maxDimension) != null
        }
    }

    suspend fun loadArtworkBitmap(context: Context, artworkUrl: String, maxDimension: Int = DEFAULT_MAX_DIMENSION): Bitmap? {
        getCachedBitmap(context, artworkUrl, maxDimension)?.let { return it }

        return withContext(Dispatchers.IO) {
            val cacheKey = getCacheKey(artworkUrl)
            val cacheFile = getDiskCacheFile(context, cacheKey)

            if (!cacheFile.exists() || cacheFile.length() == 0L) {
                val success = fetchAndSaveToDisk(context, artworkUrl, cacheFile)
                if (!success) {
                    return@withContext null
                }
            }

            runCatching {
                BitmapFactory.decodeFile(cacheFile.absolutePath)?.let { raw ->
                    val scaled = scaleBitmap(raw, maxDimension)
                    synchronized(memoryCache) {
                        memoryCache.put(cacheKey, scaled)
                    }
                    scaled
                }
            }.getOrNull()
        }
    }

    fun putInMemoryCache(artworkUrl: String, bitmap: Bitmap) {
        val cacheKey = getCacheKey(artworkUrl)
        synchronized(memoryCache) {
            memoryCache.put(cacheKey, bitmap)
        }
    }

    private suspend fun fetchAndSaveToDisk(context: Context, artworkUrl: String, targetFile: File): Boolean {
        val candidates = buildCandidateUrls(context, artworkUrl)
        val authHeaders = getAuthHeaders(context)

        for (candidate in candidates) {
            if (downloadToFile(candidate, targetFile, authHeaders)) {
                pruneDiskCacheIfNeeded(context)
                return true
            }
        }
        return false
    }

    private suspend fun buildCandidateUrls(context: Context, artworkUrl: String): List<String> {
        val uri = runCatching { Uri.parse(artworkUrl) }.getOrNull() ?: return listOf(artworkUrl)
        val host = uri.host
        val isLocal = isPrivateOrLocalHost(host)
        val isCellular = isCellularNetwork(context)

        val remoteUrl = VantafynMediaCache.currentRemoteServerUrl
            ?: runCatching { JellyfinRepositoryProvider(context).sessionStorage.read()?.remoteServerUrl }.getOrNull()
        val token = VantafynMediaCache.currentAccessToken
            ?: runCatching { JellyfinRepositoryProvider(context).sessionStorage.read()?.accessToken }.getOrNull()

        val primaryWithToken = if (!token.isNullOrBlank()) artworkUrl.withAccessToken(token) else artworkUrl

        val remoteCandidate = if (!remoteUrl.isNullOrBlank()) {
            val path = uri.encodedPath ?: ""
            val query = uri.encodedQuery
            val base = remoteUrl.trimEnd('/')
            val fullRemote = if (query.isNullOrBlank()) "$base$path" else "$base$path?$query"
            if (!token.isNullOrBlank()) fullRemote.withAccessToken(token) else fullRemote
        } else null

        val candidates = mutableListOf<String>()

        if (isCellular && isLocal && remoteCandidate != null) {
            // On cellular data with a private LAN IP, local will definitely fail or hang.
            // Prioritize the remote server URL.
            candidates.add(remoteCandidate)
            candidates.add(primaryWithToken)
        } else {
            candidates.add(primaryWithToken)
            if (remoteCandidate != null && remoteCandidate != primaryWithToken) {
                candidates.add(remoteCandidate)
            }
        }

        return candidates
    }

    private fun downloadToFile(urlStr: String, targetFile: File, headers: Map<String, String>): Boolean {
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = (url.openConnection() as? HttpURLConnection)?.apply {
                connectTimeout = 5_000
                readTimeout = 10_000
                instanceFollowRedirects = true
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
                setRequestProperty("User-Agent", "Vantafyn-Android/${Build.VERSION.RELEASE}")
            } ?: return false

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                Log.w(TAG, "Failed downloading artwork from $urlStr: HTTP $responseCode")
                return false
            }

            targetFile.parentFile?.mkdirs()
            conn.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (tempFile.length() > 0) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)
                true
            } else {
                tempFile.delete()
                false
            }
        } catch (e: Throwable) {
            tempFile.delete()
            Log.w(TAG, "Error fetching artwork from $urlStr: ${e.message}")
            false
        } finally {
            conn?.disconnect()
        }
    }

    private suspend fun getAuthHeaders(context: Context): Map<String, String> {
        val dynamic = VantafynMediaCache.authHeaderProvider?.invoke().orEmpty()
        if (dynamic.isNotEmpty()) return dynamic

        val token = VantafynMediaCache.currentAccessToken
            ?: runCatching { JellyfinRepositoryProvider(context).sessionStorage.read()?.accessToken }.getOrNull()
        if (token.isNullOrBlank()) return emptyMap()

        val clientAuth = "MediaBrowser Client=\"Vantafyn\", Device=\"Android\", DeviceId=\"vantafyn-android\", Version=\"1.0.0\", Token=\"$token\""
        return mapOf(
            "X-Emby-Token" to token,
            "Authorization" to clientAuth,
            "X-Emby-Authorization" to clientAuth,
        )
    }

    private fun getDiskCacheFile(context: Context, cacheKey: String): File {
        val dir = File(context.cacheDir, DISK_CACHE_DIR)
        return File(dir, "$cacheKey.img")
    }

    private fun getCacheKey(url: String): String {
        val cleanUrl = url
            .replace(Regex("[?&](api_key|ApiKey|X-Emby-Token)=[^&]*"), "")
            .replace(Regex("[?&]maxWidth=\\d+"), "")
            .replace(Regex("[?&]maxHeight=\\d+"), "")
            .replace(Regex("[?&]width=\\d+"), "")
            .replace(Regex("[?&]height=\\d+"), "")
            .replace(Regex("[?&]quality=\\d+"), "")

        return runCatching {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(cleanUrl.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        }.getOrElse {
            Integer.toHexString(cleanUrl.hashCode())
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val largestSide = maxOf(bitmap.width, bitmap.height)
        if (largestSide <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / largestSide.toFloat()
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }

    private fun isPrivateOrLocalHost(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        if (host.equals("localhost", ignoreCase = true) || host.endsWith(".local", ignoreCase = true)) return true
        if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("127.")) return true
        if (host.startsWith("172.")) {
            val parts = host.split(".")
            if (parts.size >= 2) {
                val second = parts[1].toIntOrNull()
                if (second != null && second in 16..31) return true
            }
        }
        return false
    }

    private fun isCellularNetwork(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val active = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) && !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun pruneDiskCacheIfNeeded(context: Context) {
        val dir = File(context.cacheDir, DISK_CACHE_DIR)
        if (!dir.exists()) return
        val files = dir.listFiles() ?: return
        var totalSize = files.sumOf { it.length() }
        if (totalSize > MAX_DISK_CACHE_BYTES) {
            val sorted = files.sortedBy { it.lastModified() }
            for (file in sorted) {
                if (totalSize <= MAX_DISK_CACHE_BYTES * 0.75) break
                val len = file.length()
                if (file.delete()) {
                    totalSize -= len
                }
            }
        }
    }
}
