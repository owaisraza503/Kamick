package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mangas")
data class MangaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String = "Unknown",
    val description: String = "",
    val coverUri: String = "",
    val categoryId: String = "default", // default, reading, completed, favorites, or custom
    val sourceType: String = "LOCAL", // BUNDLED, CBZ, ZIP, FOLDER, IMAGES
    val sourcePath: String = "",
    val status: String = "Ongoing", // Ongoing, Completed
    val readingStatus: String = "UNREAD", // UNREAD, READING, COMPLETED
    val readingDirection: String = "RTL", // RTL (Manga), LTR (Comic), VERTICAL (Webtoon)
    val displayMode: String = "SINGLE", // SINGLE, DOUBLE, WEBTOON
    val totalChapters: Int = 1,
    val unreadCount: Int = 1,
    val lastReadChapterId: Long = 0,
    val lastReadChapterNumber: Float = 1f,
    val lastReadPage: Int = 0,
    val lastReadTimestamp: Long = 0,
    val dateAdded: Long = System.currentTimeMillis(),
    val favorite: Boolean = false,
    val rating: Float = 0f,
    // Tracker IDs & Sync
    val malId: String? = null,
    val anilistId: String? = null,
    val kitsuId: String? = null,
    val mangaUpdatesId: String? = null,
    val trackerScore: Int = 0,
    val trackerStatus: String = "READING" // READING, COMPLETED, ON_HOLD, DROPPED, PLAN_TO_READ
)

@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mangaId: Long,
    val chapterNumber: Float,
    val title: String,
    val pageCount: Int,
    val pageUrisJson: String, // JSON array of page image paths/drawables/URIs
    val isRead: Boolean = false,
    val lastPageRead: Int = 0,
    val releaseDate: Long = System.currentTimeMillis(),
    val isDownloaded: Boolean = true
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String, // e.g. "default", "reading", "completed", "favorites"
    val name: String,
    val orderIndex: Int = 0
)

@Entity(tableName = "trackers")
data class TrackerEntity(
    @PrimaryKey
    val service: String, // MAL, ANILIST, KITSU, MANGAUPDATES
    val username: String = "",
    val isConnected: Boolean = false,
    val token: String? = null,
    val lastSyncTimestamp: Long = 0
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val themeMode: String = "SYSTEM", // LIGHT, DARK, SYSTEM, AMOLED
    val defaultReadingDirection: String = "RTL", // RTL, LTR, VERTICAL
    val defaultDisplayMode: String = "SINGLE", // SINGLE, DOUBLE, WEBTOON
    val readerBackground: String = "BLACK", // BLACK, DARK_GRAY, WHITE, SEPIA
    val bubbleZoomEnabled: Boolean = true,
    val bubbleZoomScale: Float = 2.4f,
    val updateScheduleHours: Int = 24, // 0 = never, 12, 24, 168 (weekly)
    val lastUpdateCheckTime: Long = 0,
    val autoSyncTrackers: Boolean = true,
    val keepScreenOn: Boolean = true,
    val cropBorders: Boolean = false
)

enum class ReadingDirection(val displayName: String) {
    RTL("Right to Left (Manga)"),
    LTR("Left to Right (Comic)"),
    VERTICAL("Vertical (Webtoon)")
}

enum class DisplayMode(val displayName: String) {
    SINGLE("Single Page"),
    DOUBLE("Double Page"),
    WEBTOON("Continuous Webtoon")
}

enum class ReaderBackground(val displayName: String, val hexColor: Long) {
    BLACK("OLED Black", 0xFF000000),
    DARK_GRAY("Dark Slate", 0xFF18181B),
    WHITE("Paper White", 0xFFFFFFFF),
    SEPIA("Warm Sepia", 0xFFFBF0D9)
}
