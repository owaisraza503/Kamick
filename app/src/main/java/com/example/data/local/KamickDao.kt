package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface KamickDao {

    // --- Manga ---
    @Query("SELECT * FROM mangas ORDER BY lastReadTimestamp DESC, dateAdded DESC")
    fun getAllMangas(): Flow<List<MangaEntity>>

    @Query("SELECT * FROM mangas WHERE id = :id")
    fun getMangaByIdFlow(id: Long): Flow<MangaEntity?>

    @Query("SELECT * FROM mangas WHERE id = :id")
    suspend fun getMangaById(id: Long): MangaEntity?

    @Query("SELECT * FROM mangas WHERE categoryId = :categoryId ORDER BY dateAdded DESC")
    fun getMangasByCategory(categoryId: String): Flow<List<MangaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertManga(manga: MangaEntity): Long

    @Update
    suspend fun updateManga(manga: MangaEntity)

    @Delete
    suspend fun deleteManga(manga: MangaEntity)

    @Query("DELETE FROM mangas WHERE id = :id")
    suspend fun deleteMangaById(id: Long)

    // --- Chapters ---
    @Query("SELECT * FROM chapters WHERE mangaId = :mangaId ORDER BY chapterNumber ASC")
    fun getChaptersForManga(mangaId: Long): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE mangaId = :mangaId ORDER BY chapterNumber ASC")
    suspend fun getChaptersForMangaOnce(mangaId: Long): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE id = :chapterId")
    suspend fun getChapterById(chapterId: Long): ChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity): Long

    @Update
    suspend fun updateChapter(chapter: ChapterEntity)

    @Query("UPDATE chapters SET isRead = :isRead, lastPageRead = :lastPage WHERE id = :chapterId")
    suspend fun updateChapterReadProgress(chapterId: Long, isRead: Boolean, lastPage: Int)

    @Query("DELETE FROM chapters WHERE mangaId = :mangaId")
    suspend fun deleteChaptersByMangaId(mangaId: Long)

    // --- Categories ---
    @Query("SELECT * FROM categories ORDER BY orderIndex ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: String)

    // --- Trackers ---
    @Query("SELECT * FROM trackers")
    fun getAllTrackers(): Flow<List<TrackerEntity>>

    @Query("SELECT * FROM trackers WHERE service = :service")
    suspend fun getTracker(service: String): TrackerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracker(tracker: TrackerEntity)

    // --- Settings ---
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getSettingsFlow(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettingsOnce(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: AppSettingsEntity)
}
