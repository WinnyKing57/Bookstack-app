package com.winnyking.bookstackcompanion.data.offline

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import com.winnyking.bookstackcompanion.data.security.SecureStorageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineImageCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureStorageManager: SecureStorageManager
) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun imagesDir(serverId: String, bookId: Long): File =
        File(context.filesDir, "offline_images/$serverId/$bookId")

    suspend fun cacheImagesForPage(serverId: String, bookId: Long, baseUrl: String, html: String) {
        withContext(Dispatchers.IO) {
            val imageUrls = extractImageUrls(html, baseUrl)
            if (imageUrls.isEmpty()) return@withContext
            val dir = imagesDir(serverId, bookId)
            if (!dir.exists()) dir.mkdirs()

            val tokenId = secureStorageManager.getTokenId(serverId)
            val tokenSecret = secureStorageManager.getTokenSecret(serverId)

            for (url in imageUrls) {
                val target = fileFor(dir, url)
                if (target.exists() && target.length() > 0) continue
                try {
                    val requestBuilder = Request.Builder().url(url)
                    if (tokenId.isNotBlank() && tokenSecret.isNotBlank()) {
                        requestBuilder.header("Authorization", "Token $tokenId:$tokenSecret")
                    }
                    val response = httpClient.newCall(requestBuilder.build()).execute()
                    response.use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body ?: return@use
                            val tmp = File(dir, target.name + ".tmp")
                            tmp.outputStream().use { out -> body.byteStream().copyTo(out) }
                            if (tmp.renameTo(target)) {
                                urlIndexFile(sha256(url)).writeText(url)
                            } else {
                                tmp.delete()
                            }
                        }
                    }
                } catch (_: Exception) {
                    continue
                }
            }
        }
    }

    fun rewriteHtmlWithLocalImages(serverId: String, bookId: Long, html: String): String {
        val dir = imagesDir(serverId, bookId)
        if (!dir.exists()) return html
        var result = html
        for (file in dir.listFiles().orEmpty()) {
            val remoteUrl = indexFileNameToUrl(file.name) ?: continue
            result = result.replace("src=\"$remoteUrl\"", "src=\"$LOCAL_SCHEME://${file.name}\"")
            result = result.replace("src='$remoteUrl'", "src='$LOCAL_SCHEME://${file.name}'")
        }
        return result
    }

    fun interceptLocalImage(url: Uri): WebResourceResponse? {
        if (url.scheme != LOCAL_SCHEME) return null
        val fileName = url.host ?: return null
        if (fileName.contains("..") || fileName.contains("/")) return null
        val file = findCachedFile(fileName) ?: return null
        return WebResourceResponse(mimeTypeFor(file.name), null, file.inputStream())
    }

    fun deleteImagesForBook(serverId: String, bookId: Long) {
        imagesDir(serverId, bookId).deleteRecursively()
    }

    fun deleteAllImages(serverId: String) {
        File(context.filesDir, "offline_images/$serverId").deleteRecursively()
    }

    private fun extractImageUrls(html: String, baseUrl: String): List<String> {
        return try {
            val doc = Jsoup.parse(html, baseUrl)
            doc.select("img[src]").mapNotNull { el ->
                val abs = el.absUrl("src")
                abs.takeIf { it.startsWith("http") && !it.contains("$LOCAL_SCHEME://") }
            }.distinct()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun fileFor(dir: File, remoteUrl: String): File {
        val hash = sha256(remoteUrl)
        val ext = remoteUrl.substringBefore('?').substringAfterLast('.', missingDelimiterValue = "").lowercase()
        val safeExt = when (ext) {
            "png", "jpg", "jpeg", "gif", "webp", "svg", "bmp" -> ext
            else -> ""
        }
        val name = if (safeExt.isNotEmpty()) "$hash.$safeExt" else hash
        return File(dir, name)
    }

    private fun findCachedFile(fileName: String): File? {
        val root = File(context.filesDir, "offline_images")
        if (!root.exists()) return null
        root.listFiles()?.forEach { serverDir ->
            serverDir.listFiles()?.forEach { bookDir ->
                val candidate = File(bookDir, fileName)
                if (candidate.exists()) return candidate
            }
        }
        return null
    }

    private fun urlIndexFile(hash: String): File {
        val root = File(context.filesDir, "offline_images/_index")
        if (!root.exists()) root.mkdirs()
        return File(root, "$hash.url")
    }

    private fun indexFileNameToUrl(fileName: String): String? {
        val hash = fileName.substringBefore('.')
        val indexFile = File(File(context.filesDir, "offline_images/_index"), "$hash.url")
        return if (indexFile.exists()) indexFile.readText().trim() else null
    }

    companion object {
        const val LOCAL_SCHEME = "bscimg"

        fun mimeTypeFor(name: String): String = when {
            name.endsWith(".png") -> "image/png"
            name.endsWith(".jpg") || name.endsWith(".jpeg") -> "image/jpeg"
            name.endsWith(".gif") -> "image/gif"
            name.endsWith(".webp") -> "image/webp"
            name.endsWith(".svg") -> "image/svg+xml"
            name.endsWith(".bmp") -> "image/bmp"
            else -> "application/octet-stream"
        }

        fun sha256(value: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
