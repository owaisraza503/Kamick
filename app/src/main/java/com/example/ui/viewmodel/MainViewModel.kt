package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.KamickDatabase
import com.example.data.local.LocalContentManager
import com.example.data.model.*
import com.example.data.repository.MangaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortOption(val displayName: String) {
    LAST_READ("Last Read"),
    TITLE("Title (A-Z)"),
    UNREAD("Unread Count"),
    DATE_ADDED("Date Added")
}

data class LibraryUiState(
    val mangas: List<MangaEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategory: String = "all",
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.LAST_READ,
    val trackers: List<TrackerEntity> = emptyList(),
    val settings: AppSettingsEntity = AppSettingsEntity(),
    val isUpdating: Boolean = false,
    val statusNotification: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = KamickDatabase.getDatabase(application)
    private val contentManager = LocalContentManager(application)
    val repository = MangaRepository(application, db.kamickDao(), contentManager)

    private val _selectedCategory = MutableStateFlow("all")
    private val _searchQuery = MutableStateFlow("")
    private val _sortOption = MutableStateFlow(SortOption.LAST_READ)
    private val _isUpdating = MutableStateFlow(false)
    private val _statusNotification = MutableStateFlow<String?>(null)

    private data class FilterState(
        val category: String,
        val query: String,
        val sort: SortOption,
        val isUpdating: Boolean,
        val notification: String?
    )

    private val filterStateFlow = combine(
        _selectedCategory,
        _searchQuery,
        _sortOption,
        _isUpdating,
        _statusNotification
    ) { cat, q, s, updating, notif ->
        FilterState(cat, q, s, updating, notif)
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        repository.allMangas,
        repository.allCategories,
        repository.allTrackers,
        repository.appSettings,
        filterStateFlow
    ) { mangas, categories, trackers, settings, filter ->
        val filtered = mangas
            .filter { manga ->
                // Category filter
                val matchesCategory = when (filter.category) {
                    "all" -> true
                    "favorites" -> manga.favorite
                    "reading" -> manga.readingStatus == "READING"
                    "completed" -> manga.readingStatus == "COMPLETED"
                    else -> manga.categoryId == filter.category
                }
                // Query filter
                val matchesQuery = filter.query.isBlank() ||
                        manga.title.contains(filter.query, ignoreCase = true) ||
                        manga.author.contains(filter.query, ignoreCase = true)
                matchesCategory && matchesQuery
            }
            .let { list ->
                // Sort
                when (filter.sort) {
                    SortOption.LAST_READ -> list.sortedByDescending { it.lastReadTimestamp }
                    SortOption.TITLE -> list.sortedBy { it.title.lowercase() }
                    SortOption.UNREAD -> list.sortedByDescending { it.unreadCount }
                    SortOption.DATE_ADDED -> list.sortedByDescending { it.dateAdded }
                }
            }

        LibraryUiState(
            mangas = filtered,
            categories = categories,
            selectedCategory = filter.category,
            searchQuery = filter.query,
            sortOption = filter.sort,
            trackers = trackers,
            settings = settings ?: AppSettingsEntity(),
            isUpdating = filter.isUpdating,
            statusNotification = filter.notification
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState()
    )

    init {
        viewModelScope.launch {
            repository.initializeDefaults()
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(sort: SortOption) {
        _sortOption.value = sort
    }

    fun dismissNotification() {
        _statusNotification.value = null
    }

    fun toggleFavorite(mangaId: Long) {
        viewModelScope.launch {
            repository.toggleFavorite(mangaId)
        }
    }

    fun updateMangaCategory(mangaId: Long, categoryId: String) {
        viewModelScope.launch {
            repository.updateMangaCategory(mangaId, categoryId)
        }
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            repository.addCategory(name)
        }
    }

    fun deleteManga(mangaId: Long) {
        viewModelScope.launch {
            repository.deleteManga(mangaId)
            _statusNotification.value = "Manga removed from library"
        }
    }

    fun importArchive(uri: Uri, fileName: String) {
        viewModelScope.launch {
            _isUpdating.value = true
            val success = repository.importArchive(uri, fileName)
            _isUpdating.value = false
            _statusNotification.value = if (success) {
                "Imported $fileName successfully!"
            } else {
                "Failed to import archive: no readable images found."
            }
        }
    }

    fun importImages(uris: List<Uri>, title: String) {
        viewModelScope.launch {
            _isUpdating.value = true
            val success = repository.importImages(uris, title)
            _isUpdating.value = false
            _statusNotification.value = if (success) {
                "Imported ${uris.size} pages into $title!"
            } else {
                "Failed to import images."
            }
        }
    }

    fun checkForLibraryUpdates() {
        viewModelScope.launch {
            _isUpdating.value = true
            val newChapters = repository.checkForLibraryUpdates()
            _isUpdating.value = false
            _statusNotification.value = if (newChapters > 0) {
                "Library updated! $newChapters new chapter(s) discovered."
            } else {
                "Library is up to date. No new chapters found."
            }
        }
    }

    fun updateSettings(settings: AppSettingsEntity) {
        viewModelScope.launch {
            repository.updateSettings(settings)
        }
    }

    fun updateTheme(themeMode: String) {
        viewModelScope.launch {
            val current = uiState.value.settings
            repository.updateSettings(current.copy(themeMode = themeMode))
        }
    }

    fun connectTracker(service: String, username: String, token: String) {
        viewModelScope.launch {
            repository.connectTracker(service, username, token)
            _statusNotification.value = "Connected to $service as $username"
        }
    }

    fun disconnectTracker(service: String) {
        viewModelScope.launch {
            repository.disconnectTracker(service)
            _statusNotification.value = "Disconnected from $service"
        }
    }

    fun updateTrackerBinding(
        mangaId: Long,
        malId: String?,
        anilistId: String?,
        kitsuId: String?,
        mangaUpdatesId: String?,
        score: Int,
        status: String
    ) {
        viewModelScope.launch {
            repository.updateTrackerBinding(mangaId, malId, anilistId, kitsuId, mangaUpdatesId, score, status)
            _statusNotification.value = "Trackers updated"
        }
    }

    fun syncTrackersForManga(mangaId: Long, chapterNum: Int) {
        viewModelScope.launch {
            val success = repository.syncTrackerProgress(mangaId, chapterNum)
            _statusNotification.value = if (success) "Synced progress to active trackers" else "Sync failed"
        }
    }

    fun exportBackup(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportBackupJson()
            onReady(json)
        }
    }

    fun restoreBackup(jsonString: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.restoreBackupJson(jsonString)
            _statusNotification.value = if (success) "Library restored successfully!" else "Invalid backup file."
            onResult(success)
        }
    }
}
