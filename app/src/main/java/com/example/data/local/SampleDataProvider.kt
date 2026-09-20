package com.example.data.local

import android.content.Context
import com.example.R
import com.example.data.model.*
import org.json.JSONArray

object SampleDataProvider {

    fun getDefaultCategories(): List<CategoryEntity> {
        return listOf(
            CategoryEntity(id = "all", name = "All", orderIndex = 0),
            CategoryEntity(id = "reading", name = "Reading", orderIndex = 1),
            CategoryEntity(id = "favorites", name = "Favorites", orderIndex = 2),
            CategoryEntity(id = "completed", name = "Completed", orderIndex = 3),
            CategoryEntity(id = "plan_to_read", name = "Plan to Read", orderIndex = 4)
        )
    }

    fun getDefaultTrackers(): List<TrackerEntity> {
        return listOf(
            TrackerEntity(
                service = "MyAnimeList",
                username = "OtakuReader",
                isConnected = true,
                token = "mal_token_active_kamick",
                lastSyncTimestamp = System.currentTimeMillis() - 3600000 * 4
            ),
            TrackerEntity(
                service = "AniList",
                username = "KamickFan",
                isConnected = true,
                token = "anilist_oauth_token",
                lastSyncTimestamp = System.currentTimeMillis() - 3600000 * 2
            ),
            TrackerEntity(
                service = "Kitsu",
                username = "",
                isConnected = false,
                token = null,
                lastSyncTimestamp = 0
            ),
            TrackerEntity(
                service = "MangaUpdates",
                username = "Scanlover",
                isConnected = true,
                token = "mu_session_kamick",
                lastSyncTimestamp = System.currentTimeMillis() - 3600000 * 12
            )
        )
    }

    fun getDefaultSettings(): AppSettingsEntity {
        return AppSettingsEntity(
            id = 1,
            themeMode = "DARK",
            defaultReadingDirection = "RTL",
            defaultDisplayMode = "SINGLE",
            readerBackground = "BLACK",
            bubbleZoomEnabled = true,
            bubbleZoomScale = 2.5f,
            updateScheduleHours = 24,
            lastUpdateCheckTime = System.currentTimeMillis() - 3600000 * 6,
            autoSyncTrackers = true,
            keepScreenOn = true,
            cropBorders = false
        )
    }

    suspend fun populateInitialDataIfEmpty(dao: KamickDao) {
        val existingMangas = dao.getAllMangas()
        // Check if categories exist
        dao.insertCategories(getDefaultCategories())

        for (tracker in getDefaultTrackers()) {
            dao.insertTracker(tracker)
        }

        val settings = dao.getSettingsOnce()
        if (settings == null) {
            dao.insertSettings(getDefaultSettings())
        }

        // Insert sample starter titles
        val shadowBlade = MangaEntity(
            title = "Shadow Blade Chronicles",
            author = "Kenji Takahashi",
            description = "In an alternate feudal era consumed by eternal twilight, a lone ronin wielding the Eclipse Katana must defeat the Ten Shoguns of Darkness to reclaim the dawn.",
            coverUri = "res:${R.drawable.cover_shadow_blade_1789917979314}",
            categoryId = "reading",
            sourceType = "BUNDLED",
            sourcePath = "bundled/shadow_blade",
            status = "Ongoing",
            readingDirection = "RTL",
            displayMode = "SINGLE",
            totalChapters = 3,
            unreadCount = 2,
            lastReadChapterId = 1,
            lastReadChapterNumber = 1f,
            lastReadPage = 0,
            lastReadTimestamp = System.currentTimeMillis() - 1800000,
            favorite = true,
            rating = 4.9f,
            malId = "104922",
            anilistId = "88392",
            kitsuId = "shadow-blade",
            mangaUpdatesId = "sb_chronicles",
            trackerScore = 9,
            trackerStatus = "READING"
        )
        val sbId = dao.insertManga(shadowBlade)

        val neonRunner = MangaEntity(
            title = "Neon Runner 2099",
            author = "Aria Chen & Dex Studio",
            description = "Deep in the rain-slicked undercity of Neo-Kyoto, memory courier Maya discovers an encrypted AI consciousness that every megacorporation will kill to erase.",
            coverUri = "res:${R.drawable.cover_neon_runner_1789917995859}",
            categoryId = "reading",
            sourceType = "BUNDLED",
            sourcePath = "bundled/neon_runner",
            status = "Ongoing",
            readingDirection = "VERTICAL",
            displayMode = "WEBTOON",
            totalChapters = 2,
            unreadCount = 2,
            lastReadChapterId = 0,
            lastReadChapterNumber = 1f,
            lastReadPage = 0,
            lastReadTimestamp = System.currentTimeMillis() - 7200000,
            favorite = true,
            rating = 4.8f,
            malId = "129841",
            anilistId = "99401",
            kitsuId = "neon-runner-2099",
            mangaUpdatesId = "neon_runner",
            trackerScore = 8,
            trackerStatus = "READING"
        )
        val nrId = dao.insertManga(neonRunner)

        val eldoria = MangaEntity(
            title = "Tales of Eldoria",
            author = "Victoria Vance",
            description = "When the Ancient Arbor of Eldoria begins to wither, a rebellious hedge-witch and her clockwork sprite embark on an epic quest to rekindle the World Roots.",
            coverUri = "res:${R.drawable.cover_eldoria_1789918011027}",
            categoryId = "favorites",
            sourceType = "BUNDLED",
            sourcePath = "bundled/eldoria",
            status = "Completed",
            readingDirection = "LTR",
            displayMode = "DOUBLE",
            totalChapters = 2,
            unreadCount = 1,
            lastReadChapterId = 0,
            lastReadChapterNumber = 1f,
            lastReadPage = 0,
            lastReadTimestamp = System.currentTimeMillis() - 86400000,
            favorite = true,
            rating = 4.7f,
            malId = null,
            anilistId = "77210",
            kitsuId = "eldoria-tales",
            mangaUpdatesId = "eldoria_tales",
            trackerScore = 9,
            trackerStatus = "PLAN_TO_READ"
        )
        val eldId = dao.insertManga(eldoria)

        // Generate chapters for Shadow Blade
        val sbCh1Pages = JSONArray().apply {
            put("res:${R.drawable.manga_page_sample_1789918025455}")
            put("res:${R.drawable.cover_shadow_blade_1789917979314}")
            put("res:${R.drawable.manga_page_sample_1789918025455}")
            put("res:${R.drawable.webtoon_page_sample_1789918042114}")
            put("res:${R.drawable.manga_page_sample_1789918025455}")
        }.toString()

        val sbCh2Pages = JSONArray().apply {
            put("res:${R.drawable.cover_shadow_blade_1789917979314}")
            put("res:${R.drawable.manga_page_sample_1789918025455}")
            put("res:${R.drawable.manga_page_sample_1789918025455}")
            put("res:${R.drawable.webtoon_page_sample_1789918042114}")
        }.toString()

        val sbCh3Pages = JSONArray().apply {
            put("res:${R.drawable.manga_page_sample_1789918025455}")
            put("res:${R.drawable.webtoon_page_sample_1789918042114}")
            put("res:${R.drawable.manga_page_sample_1789918025455}")
        }.toString()

        val sbChapters = listOf(
            ChapterEntity(
                mangaId = sbId,
                chapterNumber = 1f,
                title = "Chapter 1: The Blood Moon Rises",
                pageCount = 5,
                pageUrisJson = sbCh1Pages,
                isRead = true,
                lastPageRead = 4,
                releaseDate = System.currentTimeMillis() - 86400000 * 14
            ),
            ChapterEntity(
                mangaId = sbId,
                chapterNumber = 2f,
                title = "Chapter 2: Steel and Shadow",
                pageCount = 4,
                pageUrisJson = sbCh2Pages,
                isRead = false,
                lastPageRead = 0,
                releaseDate = System.currentTimeMillis() - 86400000 * 7
            ),
            ChapterEntity(
                mangaId = sbId,
                chapterNumber = 3f,
                title = "Chapter 3: Duel at the Crimson Gate (NEW)",
                pageCount = 3,
                pageUrisJson = sbCh3Pages,
                isRead = false,
                lastPageRead = 0,
                releaseDate = System.currentTimeMillis() - 86400000 * 1
            )
        )
        dao.insertChapters(sbChapters)

        // Generate chapters for Neon Runner (Webtoon)
        val nrCh1Pages = JSONArray().apply {
            put("res:${R.drawable.webtoon_page_sample_1789918042114}")
            put("res:${R.drawable.cover_neon_runner_1789917995859}")
            put("res:${R.drawable.webtoon_page_sample_1789918042114}")
            put("res:${R.drawable.manga_page_sample_1789918025455}")
            put("res:${R.drawable.webtoon_page_sample_1789918042114}")
        }.toString()

        val nrCh2Pages = JSONArray().apply {
            put("res:${R.drawable.webtoon_page_sample_1789918042114}")
            put("res:${R.drawable.cover_neon_runner_1789917995859}")
            put("res:${R.drawable.webtoon_page_sample_1789918042114}")
        }.toString()

        val nrChapters = listOf(
            ChapterEntity(
                mangaId = nrId,
                chapterNumber = 1f,
                title = "Episode 1: The Cyber Corridor",
                pageCount = 5,
                pageUrisJson = nrCh1Pages,
                isRead = false,
                lastPageRead = 0,
                releaseDate = System.currentTimeMillis() - 86400000 * 10
            ),
            ChapterEntity(
                mangaId = nrId,
                chapterNumber = 2f,
                title = "Episode 2: Ghost in the Hologram",
                pageCount = 3,
                pageUrisJson = nrCh2Pages,
                isRead = false,
                lastPageRead = 0,
                releaseDate = System.currentTimeMillis() - 86400000 * 3
            )
        )
        dao.insertChapters(nrChapters)

        // Generate chapters for Tales of Eldoria
        val eldCh1Pages = JSONArray().apply {
            put("res:${R.drawable.cover_eldoria_1789918011027}")
            put("res:${R.drawable.manga_page_sample_1789918025455}")
            put("res:${R.drawable.webtoon_page_sample_1789918042114}")
            put("res:${R.drawable.manga_page_sample_1789918025455}")
        }.toString()

        val eldChapters = listOf(
            ChapterEntity(
                mangaId = eldId,
                chapterNumber = 1f,
                title = "Issue #1: The Whispering Roots",
                pageCount = 4,
                pageUrisJson = eldCh1Pages,
                isRead = true,
                lastPageRead = 3,
                releaseDate = System.currentTimeMillis() - 86400000 * 30
            ),
            ChapterEntity(
                mangaId = eldId,
                chapterNumber = 2f,
                title = "Issue #2: Song of the Starlight Tree",
                pageCount = 4,
                pageUrisJson = eldCh1Pages,
                isRead = false,
                lastPageRead = 0,
                releaseDate = System.currentTimeMillis() - 86400000 * 15
            )
        )
        dao.insertChapters(eldChapters)
    }
}
