package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "extension_repos")
data class ExtensionRepoEntity(
    @PrimaryKey
    val url: String,
    val name: String,
    val website: String = "",
    val isDefault: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "installed_extensions")
data class InstalledExtensionEntity(
    @PrimaryKey
    val pkgName: String,
    val name: String,
    val versionName: String,
    val versionCode: Int,
    val lang: String, // e.g. "en", "all", "ja"
    val iconUrl: String = "",
    val repoUrl: String = "",
    val isInstalled: Boolean = true,
    val isPinned: Boolean = false
)

data class ExtensionItem(
    val pkgName: String,
    val name: String,
    val versionName: String,
    val versionCode: Int,
    val lang: String,
    val iconUrl: String = "",
    val repoUrl: String = "",
    val isInstalled: Boolean = false,
    val hasUpdate: Boolean = false,
    val isPinned: Boolean = false,
    val description: String = "",
    val sources: List<ExtensionSourceItem> = emptyList()
)

data class ExtensionSourceItem(
    val id: String,
    val name: String,
    val lang: String,
    val baseUrl: String,
    val isPinned: Boolean = false,
    val supportsLatest: Boolean = true
)

data class SourceMangaItem(
    val id: String,
    val sourceId: String,
    val sourceName: String,
    val title: String,
    val coverUrl: String,
    val author: String = "",
    val artist: String = "",
    val description: String = "",
    val status: String = "Ongoing",
    val genres: List<String> = emptyList(),
    val inLibrary: Boolean = false,
    val chapters: List<SourceChapterItem> = emptyList()
)

data class SourceChapterItem(
    val id: String,
    val mangaId: String,
    val chapterNumber: Float,
    val title: String,
    val scanlator: String = "",
    val releaseDate: String = "Today",
    val pageUrls: List<String> = emptyList()
)
