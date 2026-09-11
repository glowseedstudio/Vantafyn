package dev.vantafyn.mobile

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.allowHardware
import coil3.request.crossfade
import dev.vantafyn.core.media.VantafynMediaCache
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class VantafynMobileApplication : Application(), SingletonImageLoader.Factory {

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val headers = VantafynMediaCache.authHeaderProvider?.invoke().orEmpty()
                android.util.Log.d("VantafynImage", ">>> REQ: ${original.url} headers=$headers")
                val reqBuilder = original.newBuilder()
                headers.forEach { (k, v) ->
                    if (original.header(k) == null) {
                        reqBuilder.addHeader(k, v)
                    }
                }
                val finalReq = reqBuilder.build()
                try {
                    val resp = chain.proceed(finalReq)
                    android.util.Log.d("VantafynImage", "<<< RESP: ${resp.code} ${resp.message} for ${finalReq.url}")
                    resp
                } catch (e: Exception) {
                    android.util.Log.e("VantafynImage", "<<< ERR: ${e.message} for ${finalReq.url}", e)
                    throw e
                }
            }
            .build()

        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.20)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("vantafyn_image_cache"))
                    .maxSizeBytes(150L * 1024L * 1024L)
                    .build()
            }
            .allowHardware(true)
            .crossfade(150)
            .build()
    }
}
