package ir.dastyar.ghest.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * مدیریت عکس مدارک (چک، برگه اقساط و...) — دور ۲
 * عکس‌ها در حافظه‌ی داخلی خود اپ ذخیره می‌شوند؛ بدون هیچ دسترسی خاصی و
 * بدون ارسال به هیچ سروری (سازگار با قوانین حریم خصوصی مایکت).
 */
object PhotoStore {
    const val AUTHORITY_SUFFIX = ".fileprovider"

    /** فایل موقت برای دوربین (داخل cache/captures) */
    fun newCaptureFile(context: Context): File {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val name = "cap_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"
        return File(dir, name)
    }

    fun uriForFile(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file)

    /** ذخیره‌ی نسخه‌ی فشرده‌ی عکس در حافظه‌ی داخلی اپ؛ نام فایل ذخیره‌شده برمی‌گردد */
    fun saveCompressed(context: Context, source: Uri, destName: String, maxDim: Int = 1280): String? {
        return try {
            val bmp = loadScaled(context, source, maxDim) ?: return null
            val dir = File(context.filesDir, "attachments").apply { mkdirs() }
            val dest = File(dir, destName)
            dest.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            if (!bmp.isRecycled) bmp.recycle()
            destName
        } catch (e: Exception) {
            null
        }
    }

    /** خواندن عکس ذخیره‌شده برای نمایش (کوچک‌شده) */
    fun loadPreview(context: Context, name: String, maxDim: Int = 1024): Bitmap? {
        val f = File(File(context.filesDir, "attachments"), name)
        if (!f.exists()) return null
        return loadScaled(context, Uri.fromFile(f), maxDim)
    }

    fun delete(context: Context, name: String) {
        try {
            File(File(context.filesDir, "attachments"), name).delete()
        } catch (e: Exception) {
            // نادیده گرفتن خطای حذف
        }
    }

    private fun loadScaled(context: Context, uri: Uri, maxDim: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            } ?: return null
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= maxDim && bounds.outHeight / (sample * 2) >= maxDim) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return null
            val longest = maxOf(bmp.width, bmp.height).toFloat()
            if (longest > maxDim) {
                val scale = maxDim / longest
                val m = Matrix().apply { postScale(scale, scale) }
                val out = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
                if (out != bmp) bmp.recycle()
                out
            } else bmp
        } catch (e: Exception) {
            null
        }
    }
}
