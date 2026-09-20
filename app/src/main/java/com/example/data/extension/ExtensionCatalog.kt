package com.example.data.extension

import com.example.data.model.ExtensionItem
import com.example.data.model.ExtensionSourceItem
import com.example.data.model.SourceChapterItem
import com.example.data.model.SourceMangaItem

object ExtensionCatalog {

    val defaultRepositories = listOf(
        "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json" to "Keiyoushi Official Extensions Repo",
        "https://raw.githubusercontent.com/tachiyomiorg/tachiyomi-extensions/repo/index.min.json" to "Tachiyomi Community Archive",
        "https://kamick-extensions.github.io/index.json" to "Kamick Curated Comic & Webtoon Repo"
    )

    val defaultExtensions: List<ExtensionItem> = listOf(
        ExtensionItem(
            pkgName = "eu.kanade.tachiyomi.extension.all.mangadex",
            name = "MangaDex",
            versionName = "1.4.22",
            versionCode = 1422,
            lang = "all",
            repoUrl = "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json",
            isInstalled = true,
            hasUpdate = false,
            isPinned = true,
            description = "Community manga aggregator with official translations, scanlations, and high quality releases.",
            sources = listOf(
                ExtensionSourceItem("mangadex-en", "MangaDex", "en", "https://mangadex.org", isPinned = true),
                ExtensionSourceItem("mangadex-ja", "MangaDex (Japanese)", "ja", "https://mangadex.org")
            )
        ),
        ExtensionItem(
            pkgName = "eu.kanade.tachiyomi.extension.en.mangakakalot",
            name = "MangaKakalot",
            versionName = "1.3.18",
            versionCode = 1318,
            lang = "en",
            repoUrl = "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json",
            isInstalled = true,
            hasUpdate = false,
            isPinned = true,
            description = "Massive catalog of popular shonen, action, adventure, and fantasy manga.",
            sources = listOf(
                ExtensionSourceItem("mangakakalot", "MangaKakalot", "en", "https://mangakakalot.com", isPinned = true)
            )
        ),
        ExtensionItem(
            pkgName = "eu.kanade.tachiyomi.extension.en.asurascans",
            name = "Asura Scans",
            versionName = "1.4.5",
            versionCode = 1405,
            lang = "en",
            repoUrl = "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json",
            isInstalled = true,
            hasUpdate = false,
            isPinned = true,
            description = "Premier translation team for high-octane Webtoons, dungeons, leveling, and hunter manhwa.",
            sources = listOf(
                ExtensionSourceItem("asurascans", "Asura Scans", "en", "https://asuracomic.net", isPinned = true)
            )
        ),
        ExtensionItem(
            pkgName = "eu.kanade.tachiyomi.extension.en.webtoons",
            name = "Webtoons Official",
            versionName = "1.2.9",
            versionCode = 1209,
            lang = "en",
            repoUrl = "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json",
            isInstalled = true,
            hasUpdate = false,
            isPinned = false,
            description = "Worldwide official portal for color webcomics, drama, comedy, and superhero originals.",
            sources = listOf(
                ExtensionSourceItem("webtoons", "Webtoons", "en", "https://www.webtoons.com")
            )
        ),
        ExtensionItem(
            pkgName = "eu.kanade.tachiyomi.extension.en.comicextra",
            name = "ComicExtra",
            versionName = "1.1.9",
            versionCode = 1109,
            lang = "en",
            repoUrl = "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json",
            isInstalled = false,
            hasUpdate = false,
            isPinned = false,
            description = "Online catalog for western comics including superhero action and graphic novels.",
            sources = listOf(
                ExtensionSourceItem("comicextra", "ComicExtra", "en", "https://comicextra.org")
            )
        ),
        ExtensionItem(
            pkgName = "eu.kanade.tachiyomi.extension.en.flamecomics",
            name = "Flame Comics",
            versionName = "1.3.7",
            versionCode = 1307,
            lang = "en",
            repoUrl = "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json",
            isInstalled = false,
            hasUpdate = false,
            isPinned = false,
            description = "Top hub for martial arts, fantasy, and adventure series.",
            sources = listOf(
                ExtensionSourceItem("flamecomics", "Flame Comics", "en", "https://flamecomics.xyz")
            )
        ),
        ExtensionItem(
            pkgName = "eu.kanade.tachiyomi.extension.en.mangasee",
            name = "MangaSee",
            versionName = "1.4.2",
            versionCode = 1402,
            lang = "en",
            repoUrl = "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json",
            isInstalled = false,
            hasUpdate = false,
            isPinned = false,
            description = "Official volume scan archive with clear image quality.",
            sources = listOf(
                ExtensionSourceItem("mangasee", "MangaSee", "en", "https://mangasee123.com")
            )
        ),
        ExtensionItem(
            pkgName = "eu.kanade.tachiyomi.extension.en.readm",
            name = "ReadM",
            versionName = "1.2.3",
            versionCode = 1203,
            lang = "en",
            repoUrl = "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json",
            isInstalled = false,
            hasUpdate = false,
            isPinned = false,
            description = "Fast, clean comic and manga reader with regular chapter updates.",
            sources = listOf(
                ExtensionSourceItem("readm", "ReadM", "en", "https://readm.today")
            )
        )
    )

    val sourceCatalog: List<SourceMangaItem> = listOf(
        SourceMangaItem(
            id = "src_solo_leveling",
            sourceId = "asurascans",
            sourceName = "Asura Scans",
            title = "Solo Leveling: Ragnarok",
            coverUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop&q=80",
            author = "Chugong, Daul",
            artist = "REDICE Studio",
            description = "Sung Suho, the son of the Shadow Monarch, awakens his bloodline as dimensional gates reopen to challenge earth.",
            status = "Ongoing",
            genres = listOf("Action", "Fantasy", "Supernatural", "System", "Hunter"),
            chapters = listOf(
                SourceChapterItem("sl_ch_32", "src_solo_leveling", 32f, "Chapter 32: The Monarch's Heir", "Asura Scans", "3 hours ago"),
                SourceChapterItem("sl_ch_31", "src_solo_leveling", 31f, "Chapter 31: Shadows of Busan", "Asura Scans", "1 day ago"),
                SourceChapterItem("sl_ch_30", "src_solo_leveling", 30f, "Chapter 30: Dungeon Break Encounter", "Asura Scans", "4 days ago"),
                SourceChapterItem("sl_ch_29", "src_solo_leveling", 29f, "Chapter 29: Awakening of the Fangs", "Asura Scans", "1 week ago")
            )
        ),
        SourceMangaItem(
            id = "src_one_piece",
            sourceId = "mangadex-en",
            sourceName = "MangaDex",
            title = "One Piece",
            coverUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=600&auto=format&fit=crop&q=80",
            author = "Eiichiro Oda",
            artist = "Eiichiro Oda",
            description = "Monkey D. Luffy embarks on an epic voyage across the Grand Line with his crew in search of the legendary pirate treasure.",
            status = "Ongoing",
            genres = listOf("Action", "Adventure", "Comedy", "Shonen", "Pirates"),
            chapters = listOf(
                SourceChapterItem("op_ch_1120", "src_one_piece", 1120f, "Chapter 1120: Atlas", "MangaDex Team", "Yesterday"),
                SourceChapterItem("op_ch_1119", "src_one_piece", 1119f, "Chapter 1119: Emeth", "MangaDex Team", "1 week ago"),
                SourceChapterItem("op_ch_1118", "src_one_piece", 1118f, "Chapter 1118: Be Free", "MangaDex Team", "2 weeks ago")
            )
        ),
        SourceMangaItem(
            id = "src_tower_of_god",
            sourceId = "webtoons",
            sourceName = "Webtoons",
            title = "Tower of God",
            coverUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80",
            author = "SIU",
            artist = "SIU",
            description = "What do you desire? Honor and pride? Authority and power? Whatever you desire is at the top of the mysterious Tower.",
            status = "Ongoing",
            genres = listOf("Fantasy", "Action", "Drama", "Mystery", "Webtoon"),
            chapters = listOf(
                SourceChapterItem("tog_ch_624", "src_tower_of_god", 624f, "Season 3 Ep. 207", "LINE Webtoon", "Today"),
                SourceChapterItem("tog_ch_623", "src_tower_of_god", 623f, "Season 3 Ep. 206", "LINE Webtoon", "5 days ago")
            )
        ),
        SourceMangaItem(
            id = "src_spider_man",
            sourceId = "comicextra",
            sourceName = "ComicExtra",
            title = "Ultimate Spider-Man",
            coverUrl = "https://images.unsplash.com/photo-1635805737707-575885ab0820?w=600&auto=format&fit=crop&q=80",
            author = "Jonathan Hickman",
            artist = "Marco Checchetto",
            description = "Peter Parker discovers a secret destiny in a freshly reimagined comic universe.",
            status = "Ongoing",
            genres = listOf("Comic", "Superhero", "Action", "Sci-Fi"),
            chapters = listOf(
                SourceChapterItem("usm_ch_8", "src_spider_man", 8f, "Issue #8: Sinister Six Rising", "ComicExtra", "1 day ago"),
                SourceChapterItem("usm_ch_7", "src_spider_man", 7f, "Issue #7: Family First", "ComicExtra", "1 week ago")
            )
        ),
        SourceMangaItem(
            id = "src_frieren",
            sourceId = "mangadex-en",
            sourceName = "MangaDex",
            title = "Frieren: Beyond Journey's End",
            coverUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
            author = "Kanehito Yamada",
            artist = "Tsukasa Abe",
            description = "Elf mage Frieren begins a journey across the realm to understand humanity after defeating the Demon King.",
            status = "Ongoing",
            genres = listOf("Adventure", "Drama", "Fantasy", "Shounen"),
            chapters = listOf(
                SourceChapterItem("fr_ch_130", "src_frieren", 130f, "Chapter 130: The Ancient Empire", "Kirei Cake", "3 days ago"),
                SourceChapterItem("fr_ch_129", "src_frieren", 129f, "Chapter 129: Shadows in the North", "Kirei Cake", "10 days ago")
            )
        )
    )
}
