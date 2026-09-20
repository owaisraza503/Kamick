package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray

class MangaRepository(
    private val context: Context,
    private val dao: KamickDao,
    private val contentManager: LocalContentManager
) {
    val allMangas: Flow<List<MangaEntity>> = dao.getAllMangas()
    val allCategories: Flow<List<CategoryEntity>> = dao.getAllCategories()
    val allTrackers: Flow<List<TrackerEntity>> = dao.getAllTrackers()
    val appSettings: Flow<AppSettingsEntity?> = dao.getSettingsFlow()

    suspend fun initializeDefaults() = withContext(Dispatchers.IO) {
        val currentMangas = allMangas.firstOrNull() ?: emptyList()
        if (currentMangas.isEmpty()) {
            SampleDataProvider.populateInitialDataIfEmpty(dao)
        }
    }

    fun getManga(id: Long): Flow<MangaEntity?> = dao.getMangaByIdFlow(id)

    fun getChapters(mangaId: Long): Flow<List<ChapterEntity>> = dao.getChaptersForManga(mangaId)

    suspend fun getChapter(chapterId: Long): ChapterEntity? = withContext(Dispatchers.IO) {
        dao.getChapterById(chapterId)
    }

    suspend fun updateReadProgress(mangaId: Long, chapterId: Long, page: Int, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        dao.updateChapterReadProgress(chapterId, isCompleted, page)

        val manga = dao.getMangaById(mangaId) ?: return@withContext
        val chapters = dao.getChaptersForMangaOnce(mangaId)
        val unread = chapters.count { !it.isRead }
        val currentChapter = chapters.find { it.id == chapterId }

        val updatedManga = manga.copy(
            unreadCount = unread,
            lastReadChapterId = chapterId,
            lastReadChapterNumber = currentChapter?.chapterNumber ?: manga.lastReadChapterNumber,
            lastReadPage = page,
            lastReadTimestamp = System.currentTimeMillis(),
            readingStatus = if (unread == 0) "COMPLETED" else "READING"
        )
        dao.updateManga(updatedManga)

        // Auto-sync tracker if enabled
        val settings = dao.getSettingsOnce()
        if (settings?.autoSyncTrackers == true && isCompleted) {
            syncTrackerProgress(mangaId, currentChapter?.chapterNumber?.toInt() ?: 1)
        }
    }

    suspend fun toggleFavorite(mangaId: Long) = withContext(Dispatchers.IO) {
        val manga = dao.getMangaById(mangaId) ?: return@withContext
        dao.updateManga(manga.copy(favorite = !manga.favorite))
    }

    suspend fun updateMangaCategory(mangaId: Long, newCategoryId: String) = withContext(Dispatchers.IO) {
        val manga = dao.getMangaById(mangaId) ?: return@withContext
        dao.updateManga(manga.copy(categoryId = newCategoryId))
    }

    suspend fun updateMangaSettings(mangaId: Long, direction: String, displayMode: String) = withContext(Dispatchers.IO) {
        val manga = dao.getMangaById(mangaId) ?: return@withContext
        dao.updateManga(manga.copy(readingDirection = direction, displayMode = displayMode))
    }

    suspend fun updateTrackerBinding(
        mangaId: Long,
        malId: String?,
        anilistId: String?,
        kitsuId: String?,
        mangaUpdatesId: String?,
        score: Int,
        status: String
    ) = withContext(Dispatchers.IO) {
        val manga = dao.getMangaById(mangaId) ?: return@withContext
        dao.updateManga(
            manga.copy(
                malId = malId,
                anilistId = anilistId,
                kitsuId = kitsuId,
                mangaUpdatesId = mangaUpdatesId,
                trackerScore = score,
                trackerStatus = status
            )
        )
    }

    suspend fun syncTrackerProgress(mangaId: Long, chapterNumber: Int): Boolean = withContext(Dispatchers.IO) {
        val manga = dao.getMangaById(mangaId) ?: return@withContext false
        // Update trackers' last sync time
        val trackers = listOf("MyAnimeList", "AniList", "Kitsu", "MangaUpdates")
        trackers.forEach { service ->
            val tracker = dao.getTracker(service)
            if (tracker != null && tracker.isConnected) {
                dao.insertTracker(tracker.copy(lastSyncTimestamp = System.currentTimeMillis()))
            }
        }
        true
    }

    suspend fun connectTracker(service: String, username: String, token: String) = withContext(Dispatchers.IO) {
        dao.insertTracker(
            TrackerEntity(
                service = service,
                username = username,
                isConnected = true,
                token = token,
                lastSyncTimestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun disconnectTracker(service: String) = withContext(Dispatchers.IO) {
        dao.insertTracker(
            TrackerEntity(
                service = service,
                username = "",
                isConnected = false,
                token = null,
                lastSyncTimestamp = 0
            )
        )
    }

    suspend fun addCategory(name: String) = withContext(Dispatchers.IO) {
        val id = name.lowercase().replace(" ", "_")
        dao.insertCategory(CategoryEntity(id = id, name = name, orderIndex = 99))
    }

    suspend fun deleteCategory(categoryId: String) = withContext(Dispatchers.IO) {
        dao.deleteCategory(categoryId)
    }

    suspend fun deleteManga(mangaId: Long) = withContext(Dispatchers.IO) {
        dao.deleteChaptersByMangaId(mangaId)
        dao.deleteMangaById(mangaId)
    }

    suspend fun importArchive(uri: Uri, fileName: String): Boolean = withContext(Dispatchers.IO) {
        val result = contentManager.importArchive(uri, fileName) ?: return@withContext false
        val mangaId = dao.insertManga(result.first)
        dao.insertChapter(result.second.copy(mangaId = mangaId))
        true
    }

    suspend fun importImages(uris: List<Uri>, title: String): Boolean = withContext(Dispatchers.IO) {
        val result = contentManager.importImages(uris, title) ?: return@withContext false
        val mangaId = dao.insertManga(result.first)
        dao.insertChapter(result.second.copy(mangaId = mangaId))
        true
    }

    suspend fun updateSettings(settings: AppSettingsEntity) = withContext(Dispatchers.IO) {
        dao.insertSettings(settings)
    }

    /**
     * Library Update Checker: Checks for new chapters.
     * Simulates network/local checking on schedule or manually,
     * adding newly released chapters for ongoing titles.
     */
    suspend fun checkForLibraryUpdates(): Int = withContext(Dispatchers.IO) {
        val mangas = dao.getAllMangas().firstOrNull() ?: emptyList()
        var newChaptersCount = 0

        for (manga in mangas) {
            if (manga.status.equals("Ongoing", ignoreCase = true)) {
                val chapters = dao.getChaptersForMangaOnce(manga.id)
                val highestNum = chapters.maxOfOrNull { it.chapterNumber } ?: 1f

                // If highest chapter is less than 4, add next chapter simulation
                if (highestNum < 4f) {
                    val nextNum = highestNum + 1f
                    val samplePages = JSONArray().apply {
                        put("res:${com.example.R.drawable.manga_page_sample_1789918025455}")
                        put("res:${com.example.R.drawable.webtoon_page_sample_1789918042114}")
                        put("res:${com.example.R.drawable.manga_page_sample_1789918025455}")
                    }.toString()

                    val newChapter = ChapterEntity(
                        mangaId = manga.id,
                        chapterNumber = nextNum,
                        title = "Chapter ${nextNum.toInt()}: New Release",
                        pageCount = 3,
                        pageUrisJson = samplePages,
                        isRead = false,
                        lastPageRead = 0,
                        releaseDate = System.currentTimeMillis()
                    )
                    dao.insertChapter(newChapter)
                    dao.updateManga(
                        manga.copy(
                            totalChapters = manga.totalChapters + 1,
                            unreadCount = manga.unreadCount + 1
                        )
                    )
                    newChaptersCount++
                }
            }
        }

        val settings = dao.getSettingsOnce() ?: SampleDataProvider.getDefaultSettings()
        dao.insertSettings(settings.copy(lastUpdateCheckTime = System.currentTimeMillis()))
        newChaptersCount
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val mangas = dao.getAllMangas().firstOrNull() ?: emptyList()
        val allChapters = mutableListOf<ChapterEntity>()
        for (m in mangas) {
            allChapters.addAll(dao.getChaptersForMangaOnce(m.id))
        }
        val categories = dao.getAllCategories().firstOrNull() ?: emptyList()
        val trackers = dao.getAllTrackers().firstOrNull() ?: emptyList()
        val settings = dao.getSettingsOnce()

        contentManager.createBackupJson(mangas, allChapters, categories, trackers, settings)
    }

    suspend fun restoreBackupJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        val payload = contentManager.parseBackupJson(jsonString) ?: return@withContext false
        for (cat in payload.categories) {
            dao.insertCategory(cat)
        }
        for (t in payload.trackers) {
            dao.insertTracker(t)
        }
        for (m in payload.mangas) {
            val newId = dao.insertManga(m)
            val relatedChapters = payload.chapters.filter { it.mangaId == m.id }
            for (c in relatedChapters) {
                dao.insertChapter(c.copy(mangaId = newId))
            }
        }
        true
    }

    suspend fun insertManga(manga: MangaEntity): Long = withContext(Dispatchers.IO) {
        dao.insertManga(manga)
    }

    suspend fun insertChapters(chapters: List<ChapterEntity>) = withContext(Dispatchers.IO) {
        dao.insertChapters(chapters)
    }

    suspend fun insertChapter(chapter: ChapterEntity): Long = withContext(Dispatchers.IO) {
        dao.insertChapter(chapter)
    }
}
