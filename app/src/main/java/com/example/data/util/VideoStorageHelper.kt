package com.example.data.util

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class PreparedVideoFile(
    val uriString: String,
    val durationMs: Long
)

object VideoStorageHelper {
    suspend fun copyPickedVideoToInternalStorage(
        context: Context,
        sourceUri: Uri
    ): PreparedVideoFile = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "vibo_videos").apply {
            if (!exists()) mkdirs()
        }
        val fileName = "vibo_${UUID.randomUUID()}.mp4"
        val destFile = File(dir, fileName)

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("تعذر قراءة ملف الفيديو المختار")

        val durationMs = extractDurationMs(destFile.absolutePath)
        PreparedVideoFile(
            uriString = Uri.fromFile(destFile).toString(),
            durationMs = durationMs
        )
    }

    suspend fun saveVideoToPhoneGallery(
        context: Context,
        videoUriString: String,
        title: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val sanitizedTitle = title
                .trim()
                .replace(Regex("[^\\p{L}\\p{N}_-]+"), "_")
                .take(32)
                .ifBlank { "video" }
            val fileName = "VIBO_${sanitizedTitle}_${System.currentTimeMillis()}.mp4"

            openVideoInputStream(context, videoUriString).use { inputStream ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        put(
                            MediaStore.Video.Media.RELATIVE_PATH,
                            "${Environment.DIRECTORY_MOVIES}/VIBO"
                        )
                        put(MediaStore.Video.Media.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val collection =
                        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    val itemUri = resolver.insert(collection, values)
                        ?: throw IllegalStateException("تعذر إنشاء ملف الفيديو في الهاتف")
                    try {
                        resolver.openOutputStream(itemUri)?.use { output ->
                            inputStream.copyTo(output)
                        } ?: throw IllegalStateException("تعذر الكتابة في ذاكرة الهاتف")
                        values.clear()
                        values.put(MediaStore.Video.Media.IS_PENDING, 0)
                        resolver.update(itemUri, values, null, null)
                        "Movies/VIBO"
                    } catch (e: Exception) {
                        resolver.delete(itemUri, null, null)
                        throw e
                    }
                } else {
                    val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                        ?: context.filesDir
                    val viboDir = File(baseDir, "VIBO").apply { if (!exists()) mkdirs() }
                    val destFile = File(viboDir, fileName)
                    destFile.outputStream().use { output ->
                        inputStream.copyTo(output)
                    }
                    viboDir.absolutePath
                }
            }
        }
    }

    private fun openVideoInputStream(context: Context, uriString: String): InputStream {
        val trimmed = uriString.trim()
        if (trimmed.lowercase().contains(".m3u8")) {
            throw IllegalStateException("مقاطع البث المباشر (HLS) غير قابلة للحفظ كملف على الهاتف")
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            val connection = (URL(trimmed).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
            }
            val code = connection.responseCode
            if (code !in 200..299) {
                connection.disconnect()
                throw IllegalStateException("تعذر تحميل الفيديو (رمز الاستجابة $code)")
            }
            val rawStream = connection.inputStream
            return object : java.io.FilterInputStream(rawStream) {
                override fun close() {
                    try {
                        super.close()
                    } finally {
                        connection.disconnect()
                    }
                }
            }
        }
        val uri = Uri.parse(trimmed)
        if (uri.scheme == "file") {
            val path = uri.path ?: throw IllegalStateException("مسار الفيديو غير صالح")
            return File(path).inputStream()
        }
        return context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("تعذر الوصول إلى مصدر الفيديو")
    }

    private fun extractDurationMs(filePath: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }
}
