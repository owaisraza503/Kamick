package com.example.data.local

import android.content.Context
import android.net.Uri
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class LocalContentManager(private val context: Context) {

    private val supportedImageExtensions = setOf("jpg", "jpeg", "png", "webp", "bmp", "gif")

    /**
     * Imports a CBZ or ZIP archive file into Kamick library.
     */
    suspend fun importArchive(uri: Uri, fileName: String): Pair<MangaEntity, ChapterEntity>? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream = contentResolver.openInputStream(uri) ?: return@withContext null

            val mangaTitle = fileName
                .replace(Regex("\\.(cbz|zip|cbr)$", RegexOption.IGNORE_CASE), "")
                .replace("_", " ")
                .trim()

            val uniqueFolder = "manga_${System.currentTimeMillis()}"
            val targetDir = File(context.filesDir, uniqueFolder)
            if (!targetDir.exists()) targetDir.mkdirs()

            val chapterDir = File(targetDir, "chapter_1")
            if (!chapterDir.exists()) chapterDir.mkdirs()

            val extractedImageFiles = mutableListOf<File>()
            val zipStream = ZipInputStream(inputStream)
            var entry: ZipEntry? = zipStream.nextEntry

            while (entry != null) {
                val entryName = entry.name
                val ext = entryName.substringAfterLast(".", "").lowercase()
                if (!entry.isDirectory && ext in supportedImageExtensions) {
                    val cleanFileName = File(entryName).name
                    val outFile = File(chapterDir, cleanFileName)
                    FileOutputStream(outFile).use { fos ->
                        zipStream.copyTo(fos)
                    }
                    extractedImageFiles.add(outFile)
                }
                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }
            zipStream.close()

            if (extractedImageFiles.isEmpty()) {
                return@withContext null
            }

            // Natural sort images (page 1, page 2, etc.)
            extractedImageFiles.sortBy { it.name.lowercase() }

            val coverFile = extractedImageFiles.first()
            val pageUrisArray = JSONArray()
            extractedImageFiles.forEach { file ->
                pageUrisArray.put("file://${file.absolutePath}")
            }

            val manga = MangaEntity(
                title = mangaTitle,
                author = "Local Comic",
                description = "Imported from $fileName on device.",
                coverUri = "file://${coverFile.absolutePath}",
                categoryId = "reading",
                sourceType = "CBZ",
                sourcePath = targetDir.absolutePath,
                status = "Completed",
                readingDirection = "RTL",
                displayMode = "SINGLE",
                totalChapters = 1,
                unreadCount = 1,
                lastReadChapterId = 0,
                lastReadChapterNumber = 1f,
                lastReadPage = 0,
                lastReadTimestamp = System.currentTimeMillis(),
                favorite = false,
                rating = 5.0f
            )

            val chapter = ChapterEntity(
                mangaId = 0, // Will be set after manga insert
                chapterNumber = 1f,
                title = "Chapter 1: $mangaTitle",
                pageCount = extractedImageFiles.size,
                pageUrisJson = pageUrisArray.toString(),
                isRead = false,
                lastPageRead = 0,
                releaseDate = System.currentTimeMillis()
            )

            Pair(manga, chapter)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Imports multiple picked image URIs into a new local chapter
     */
    suspend fun importImages(uris: List<Uri>, title: String): Pair<MangaEntity, ChapterEntity>? = withContext(Dispatchers.IO) {
        try {
            if (uris.isEmpty()) return@withContext null

            val uniqueFolder = "manga_${System.currentTimeMillis()}"
            val targetDir = File(context.filesDir, uniqueFolder)
            if (!targetDir.exists()) targetDir.mkdirs()

            val chapterDir = File(targetDir, "chapter_1")
            if (!chapterDir.exists()) chapterDir.mkdirs()

            val savedFiles = mutableListOf<File>()
            val contentResolver = context.contentResolver

            uris.forEachIndexed { index, uri ->
                val fileName = String.format("page_%03d.jpg", index + 1)
                val outFile = File(chapterDir, fileName)
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                    savedFiles.add(outFile)
                }
            }

            if (savedFiles.isEmpty()) return@withContext null

            savedFiles.sortBy { it.name.lowercase() }
            val coverFile = savedFiles.first()
            val pageUrisArray = JSONArray()
            savedFiles.forEach { file ->
                pageUrisArray.put("file://${file.absolutePath}")
            }

            val manga = MangaEntity(
                title = title.ifBlank { "Local Collection" },
                author = "Local Import",
                description = "Custom image collection imported into Kamick.",
                coverUri = "file://${coverFile.absolutePath}",
                categoryId = "reading",
                sourceType = "IMAGES",
                sourcePath = targetDir.absolutePath,
                status = "Ongoing",
                readingDirection = "RTL",
                displayMode = "SINGLE",
                totalChapters = 1,
                unreadCount = 1,
                lastReadChapterId = 0,
                lastReadChapterNumber = 1f,
                lastReadPage = 0,
                lastReadTimestamp = System.currentTimeMillis()
            )

            val chapter = ChapterEntity(
                mangaId = 0,
                chapterNumber = 1f,
                title = "Chapter 1",
                pageCount = savedFiles.size,
                pageUrisJson = pageUrisArray.toString(),
                isRead = false,
                lastPageRead = 0,
                releaseDate = System.currentTimeMillis()
            )

            Pair(manga, chapter)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates a full JSON string backup of the library
     */
    fun createBackupJson(
        mangas: List<MangaEntity>,
        chapters: List<ChapterEntity>,
        categories: List<CategoryEntity>,
        trackers: List<TrackerEntity>,
        settings: AppSettingsEntity?
    ): String {
        val root = JSONObject()
        root.put("app", "Kamick")
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())

        val mangasArr = JSONArray()
        mangas.forEach { m ->
            val mObj = JSONObject().apply {
                put("id", m.id)
                put("title", m.title)
                put("author", m.author)
                put("description", m.description)
                put("coverUri", m.coverUri)
                put("categoryId", m.categoryId)
                put("sourceType", m.sourceType)
                put("status", m.status)
                put("readingDirection", m.readingDirection)
                put("displayMode", m.displayMode)
                put("favorite", m.favorite)
                put("rating", m.rating.toDouble())
                put("malId", m.malId ?: "")
                put("anilistId", m.anilistId ?: "")
                put("kitsuId", m.kitsuId ?: "")
                put("mangaUpdatesId", m.mangaUpdatesId ?: "")
                put("trackerScore", m.trackerScore)
                put("trackerStatus", m.trackerStatus)
            }
            mangasArr.put(mObj)
        }
        root.put("mangas", mangasArr)

        val chaptersArr = JSONArray()
        chapters.forEach { c ->
            val cObj = JSONObject().apply {
                put("id", c.id)
                put("mangaId", c.mangaId)
                put("chapterNumber", c.chapterNumber.toDouble())
                put("title", c.title)
                put("pageCount", c.pageCount)
                put("pageUrisJson", c.pageUrisJson)
                put("isRead", c.isRead)
                put("lastPageRead", c.lastPageRead)
            }
            chaptersArr.put(cObj)
        }
        root.put("chapters", chaptersArr)

        val categoriesArr = JSONArray()
        categories.forEach { cat ->
            val catObj = JSONObject().apply {
                put("id", cat.id)
                put("name", cat.name)
                put("orderIndex", cat.orderIndex)
            }
            categoriesArr.put(catObj)
        }
        root.put("categories", categoriesArr)

        val trackersArr = JSONArray()
        trackers.forEach { t ->
            val tObj = JSONObject().apply {
                put("service", t.service)
                put("username", t.username)
                put("isConnected", t.isConnected)
                put("token", t.token ?: "")
            }
            trackersArr.put(tObj)
        }
        root.put("trackers", trackersArr)

        return root.toString(2)
    }

    /**
     * Parses a JSON backup string and returns parsed entities
     */
    fun parseBackupJson(jsonString: String): BackupPayload? {
        return try {
            val root = JSONObject(jsonString)
            if (root.optString("app") != "Kamick") return null

            val mangas = mutableListOf<MangaEntity>()
            val mangasArr = root.optJSONArray("mangas") ?: JSONArray()
            for (i in 0 until mangasArr.length()) {
                val o = mangasArr.getJSONObject(i)
                mangas.add(
                    MangaEntity(
                        id = o.optLong("id", 0),
                        title = o.optString("title", "Untitled"),
                        author = o.optString("author", "Unknown"),
                        description = o.optString("description", ""),
                        coverUri = o.optString("coverUri", ""),
                        categoryId = o.optString("categoryId", "default"),
                        sourceType = o.optString("sourceType", "LOCAL"),
                        status = o.optString("status", "Ongoing"),
                        readingDirection = o.optString("readingDirection", "RTL"),
                        displayMode = o.optString("displayMode", "SINGLE"),
                        favorite = o.optBoolean("favorite", false),
                        rating = o.optDouble("rating", 0.0).toFloat(),
                        malId = o.optString("malId").ifEmpty { null },
                        anilistId = o.optString("anilistId").ifEmpty { null },
                        kitsuId = o.optString("kitsuId").ifEmpty { null },
                        mangaUpdatesId = o.optString("mangaUpdatesId").ifEmpty { null },
                        trackerScore = o.optInt("trackerScore", 0),
                        trackerStatus = o.optString("trackerStatus", "READING")
                    )
                )
            }

            val chapters = mutableListOf<ChapterEntity>()
            val chaptersArr = root.optJSONArray("chapters") ?: JSONArray()
            for (i in 0 until chaptersArr.length()) {
                val o = chaptersArr.getJSONObject(i)
                chapters.add(
                    ChapterEntity(
                        id = o.optLong("id", 0),
                        mangaId = o.optLong("mangaId", 0),
                        chapterNumber = o.optDouble("chapterNumber", 1.0).toFloat(),
                        title = o.optString("title", "Chapter 1"),
                        pageCount = o.optInt("pageCount", 1),
                        pageUrisJson = o.optString("pageUrisJson", "[]"),
                        isRead = o.optBoolean("isRead", false),
                        lastPageRead = o.optInt("lastPageRead", 0)
                    )
                )
            }

            val categories = mutableListOf<CategoryEntity>()
            val categoriesArr = root.optJSONArray("categories") ?: JSONArray()
            for (i in 0 until categoriesArr.length()) {
                val o = categoriesArr.getJSONObject(i)
                categories.add(
                    CategoryEntity(
                        id = o.optString("id", "cat_$i"),
                        name = o.optString("name", "Category"),
                        orderIndex = o.optInt("orderIndex", i)
                    )
                )
            }

            val trackers = mutableListOf<TrackerEntity>()
            val trackersArr = root.optJSONArray("trackers") ?: JSONArray()
            for (i in 0 until trackersArr.length()) {
                val o = trackersArr.getJSONObject(i)
                trackers.add(
                    TrackerEntity(
                        service = o.optString("service", "MAL"),
                        username = o.optString("username", ""),
                        isConnected = o.optBoolean("isConnected", false),
                        token = o.optString("token").ifEmpty { null }
                    )
                )
            }

            BackupPayload(mangas, chapters, categories, trackers)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

data class BackupPayload(
    val mangas: List<MangaEntity>,
    val chapters: List<ChapterEntity>,
    val categories: List<CategoryEntity>,
    val trackers: List<TrackerEntity>
)
