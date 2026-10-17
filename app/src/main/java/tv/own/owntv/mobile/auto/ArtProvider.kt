package tv.own.owntv.mobile.auto

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileNotFoundException

/**
 * glcn: Android Auto draws artwork only from `content://` addresses, never from the web. This hands it
 * channel logos and programme pictures: the web address travels inside the content address, and the
 * first request downloads it once (through the app's image loader) into a small cache file.
 */
class ArtProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val ctx = context ?: throw FileNotFoundException()
        val url = decode(uri.lastPathSegment) ?: throw FileNotFoundException()
        if (!url.startsWith("http://") && !url.startsWith("https://")) throw FileNotFoundException()
        val dir = File(ctx.cacheDir, "auto_art").apply { mkdirs() }
        val file = File(dir, url.hashCode().toUInt().toString(16) + ".png")
        if (!file.exists() || file.length() == 0L) {
            val bmp = runBlocking(Dispatchers.IO) {
                withTimeoutOrNull(6_000) {
                    runCatching {
                        val request = coil3.request.ImageRequest.Builder(ctx).data(url).size(480, 480).build()
                        val result = coil3.SingletonImageLoader.get(ctx).execute(request)
                        ((result as? coil3.request.SuccessResult)?.image as? coil3.BitmapImage)?.bitmap
                    }.getOrNull()
                }
            } ?: throw FileNotFoundException(url)
            runCatching {
                val soft = if (bmp.config == Bitmap.Config.HARDWARE) bmp.copy(Bitmap.Config.ARGB_8888, false) else bmp
                file.outputStream().use { soft.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }.onFailure { throw FileNotFoundException(url) }
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String = "image/png"
    override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?): Int = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?): Int = 0

    companion object {
        /** The content address Android Auto can draw for the picture at [url]; null for none. */
        fun uriFor(context: Context, url: String?): Uri? {
            if (url.isNullOrBlank()) return null
            val token = Base64.encodeToString(url.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            return Uri.parse("content://${context.packageName}.glcnart/$token")
        }

        private fun decode(token: String?): String? = token?.let {
            runCatching { String(Base64.decode(it, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)) }.getOrNull()
        }
    }
}
