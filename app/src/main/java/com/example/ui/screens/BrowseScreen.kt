package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.extension.ExtensionCatalog
import com.example.data.model.*
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray

enum class BrowseTab(val title: String) {
    SOURCES("Sources"),
    EXTENSIONS("Extensions")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    viewModel: MainViewModel,
    onOpenMangaDetail: (Long) -> Unit,
    onReadChapter: (Long, Long, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf(BrowseTab.SOURCES) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("all") }

    // Extensions & Repositories State
    var extensions by remember { mutableStateOf(ExtensionCatalog.defaultExtensions) }
    var repositories by remember { mutableStateOf(ExtensionCatalog.defaultRepositories) }
    var installingPkgs by remember { mutableStateOf(setOf<String>()) }

    // Source catalog state
    var selectedSourceId by remember { mutableStateOf<String?>(null) }
    var viewingCatalogTitle by remember { mutableStateOf<String?>(null) }
    var selectedMangaItem by remember { mutableStateOf<SourceMangaItem?>(null) }

    // Add Repository Dialog
    var showAddRepoDialog by remember { mutableStateOf(false) }
    var repoUrlInput by remember { mutableStateOf("") }
    var repoNameInput by remember { mutableStateOf("") }

    val installedExtensions = remember(extensions) {
        extensions.filter { it.isInstalled }
    }

    val availableExtensions = remember(extensions, selectedLanguage, searchQuery) {
        extensions.filter { !it.isInstalled }
            .filter { selectedLanguage == "all" || it.lang == selectedLanguage || it.lang == "all" }
            .filter { searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.pkgName.contains(searchQuery, ignoreCase = true) }
    }

    val installedSources = remember(installedExtensions) {
        installedExtensions.flatMap { ext -> ext.sources }
    }

    // Filtered Manga Catalog
    val displayedManga = remember(searchQuery, selectedSourceId, extensions) {
        val installedSourceIds = installedExtensions.flatMap { it.sources.map { s -> s.id } }.toSet()
        ExtensionCatalog.sourceCatalog.filter { item ->
            installedSourceIds.contains(item.sourceId) &&
                    (selectedSourceId == null || item.sourceId == selectedSourceId) &&
                    (searchQuery.isBlank() || item.title.contains(searchQuery, ignoreCase = true) ||
                            item.author.contains(searchQuery, ignoreCase = true) ||
                            item.genres.any { it.contains(searchQuery, ignoreCase = true) })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (selectedSourceId != null) viewingCatalogTitle ?: "Source Catalog" else "Browse",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        if (selectedSourceId != null) {
                            Text(
                                text = "Browsing extension source catalog",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (selectedSourceId != null) {
                        IconButton(onClick = { selectedSourceId = null; viewingCatalogTitle = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back to sources")
                        }
                    }
                },
                actions = {
                    if (currentTab == BrowseTab.EXTENSIONS) {
                        IconButton(
                            onClick = { showAddRepoDialog = true },
                            modifier = Modifier.testTag("add_extension_repo_button")
                        ) {
                            Icon(Icons.Default.AddLink, contentDescription = "Add Extension Repository")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Switcher (Sources / Extensions)
            if (selectedSourceId == null) {
                TabRow(
                    selectedTabIndex = currentTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    BrowseTab.values().forEach { tab ->
                        Tab(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            text = {
                                Text(
                                    text = tab.title,
                                    fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = if (tab == BrowseTab.SOURCES) Icons.Filled.Public else Icons.Filled.Extension,
                                    contentDescription = tab.title
                                )
                            }
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("browse_search_field"),
                placeholder = {
                    Text(
                        if (currentTab == BrowseTab.SOURCES) "Search titles across sources..."
                        else "Search extensions & repositories..."
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )

            // CONTENT BASED ON ACTIVE TAB
            if (selectedSourceId != null) {
                // ACTIVE SOURCE CATALOG GRID
                SourceCatalogGrid(
                    mangaList = displayedManga,
                    onMangaClick = { selectedMangaItem = it }
                )
            } else if (currentTab == BrowseTab.SOURCES) {
                // SOURCES TAB
                SourcesList(
                    sources = installedSources,
                    onOpenSource = { source ->
                        selectedSourceId = source.id
                        viewingCatalogTitle = source.name
                    },
                    onSearchSource = { source ->
                        selectedSourceId = source.id
                        viewingCatalogTitle = source.name
                    }
                )
            } else {
                // EXTENSIONS TAB
                ExtensionsList(
                    searchQuery = searchQuery,
                    selectedLanguage = selectedLanguage,
                    onSelectLanguage = { selectedLanguage = it },
                    installedExtensions = installedExtensions,
                    availableExtensions = availableExtensions,
                    installingPkgs = installingPkgs,
                    repositories = repositories,
                    onAddRepoClick = { showAddRepoDialog = true },
                    onInstallExtension = { ext ->
                        coroutineScope.launch {
                            installingPkgs = installingPkgs + ext.pkgName
                            delay(1000) // Simulate download & install
                            extensions = extensions.map {
                                if (it.pkgName == ext.pkgName) it.copy(isInstalled = true) else it
                            }
                            installingPkgs = installingPkgs - ext.pkgName
                            Toast.makeText(context, "${ext.name} extension installed!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onUninstallExtension = { ext ->
                        extensions = extensions.map {
                            if (it.pkgName == ext.pkgName) it.copy(isInstalled = false) else it
                        }
                        Toast.makeText(context, "${ext.name} uninstalled", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    // MANGA DETAIL BOTTOM SHEET FROM EXTENSION
    if (selectedMangaItem != null) {
        val manga = selectedMangaItem!!
        SourceMangaDetailSheet(
            manga = manga,
            onDismiss = { selectedMangaItem = null },
            onAddToLibrary = {
                coroutineScope.launch {
                    // Convert SourceMangaItem into MangaEntity & Chapters in Room DB
                    val mangaEntity = MangaEntity(
                        title = manga.title,
                        author = manga.author,
                        description = manga.description,
                        coverUri = manga.coverUrl,
                        sourceType = "EXTENSION",
                        sourcePath = manga.sourceName,
                        status = manga.status,
                        totalChapters = manga.chapters.size,
                        unreadCount = manga.chapters.size,
                        dateAdded = System.currentTimeMillis()
                    )
                    val insertedId = viewModel.repository.insertManga(mangaEntity)

                    // Insert sample chapters
                    val chapterEntities = manga.chapters.mapIndexed { idx, ch ->
                        ChapterEntity(
                            mangaId = insertedId,
                            title = ch.title,
                            chapterNumber = ch.chapterNumber,
                            pageCount = 12,
                            pageUrisJson = JSONArray(
                                listOf(
                                    "res:${com.example.R.drawable.manga_page_sample_1789918025455}",
                                    "res:${com.example.R.drawable.webtoon_page_sample_1789918042114}"
                                )
                            ).toString()
                        )
                    }
                    viewModel.repository.insertChapters(chapterEntities)

                    Toast.makeText(context, "'${manga.title}' added to your Library!", Toast.LENGTH_SHORT).show()
                    selectedMangaItem = null
                    onOpenMangaDetail(insertedId)
                }
            },
            onReadChapter = { chapter ->
                coroutineScope.launch {
                    // Quick add to database and launch reader immediately
                    val mangaEntity = MangaEntity(
                        title = manga.title,
                        author = manga.author,
                        description = manga.description,
                        coverUri = manga.coverUrl,
                        sourceType = "EXTENSION",
                        sourcePath = manga.sourceName
                    )
                    val mId = viewModel.repository.insertManga(mangaEntity)
                    val chEntity = ChapterEntity(
                        mangaId = mId,
                        title = chapter.title,
                        chapterNumber = chapter.chapterNumber,
                        pageCount = 12,
                        pageUrisJson = JSONArray(
                            listOf(
                                "res:${com.example.R.drawable.manga_page_sample_1789918025455}",
                                "res:${com.example.R.drawable.webtoon_page_sample_1789918042114}"
                            )
                        ).toString()
                    )
                    val chId = viewModel.repository.insertChapter(chEntity)
                    selectedMangaItem = null
                    onReadChapter(mId, chId, 0)
                }
            }
        )
    }

    // ADD REPOSITORY DIALOG
    if (showAddRepoDialog) {
        AlertDialog(
            onDismissRequest = { showAddRepoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Extension Repository")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter the repository JSON URL to source third-party Tachiyomi/Mihon comic extensions:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = repoNameInput,
                        onValueChange = { repoNameInput = it },
                        label = { Text("Repository Name") },
                        placeholder = { Text("e.g. Keiyoushi Repo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = repoUrlInput,
                        onValueChange = { repoUrlInput = it },
                        label = { Text("Repository URL") },
                        placeholder = { Text("https://.../index.min.json") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Popular Quick Add:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SuggestionChip(
                            onClick = {
                                repoNameInput = "Keiyoushi Extensions"
                                repoUrlInput = "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json"
                            },
                            label = { Text("Keiyoushi Repo", fontSize = 11.sp) }
                        )
                        SuggestionChip(
                            onClick = {
                                repoNameInput = "Kamick Official"
                                repoUrlInput = "https://kamick-extensions.github.io/index.json"
                            },
                            label = { Text("Kamick Hub", fontSize = 11.sp) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (repoUrlInput.isNotBlank()) {
                            repositories = repositories + (repoUrlInput to (repoNameInput.ifBlank { "Custom Repository" }))
                            Toast.makeText(context, "Repository added successfully!", Toast.LENGTH_SHORT).show()
                            repoUrlInput = ""
                            repoNameInput = ""
                            showAddRepoDialog = false
                        }
                    }
                ) {
                    Text("Add Repo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRepoDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SourcesList(
    sources: List<ExtensionSourceItem>,
    onOpenSource: (ExtensionSourceItem) -> Unit,
    onSearchSource: (ExtensionSourceItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (sources.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.ExtensionOff,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No extension sources installed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Switch to the Extensions tab to install sources like MangaDex, MangaKakalot, and Asura Scans.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Installed Sources (${sources.size})",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        items(sources) { source ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSource(source) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Source Initial Avatar
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = source.name.take(2).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = source.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = source.lang.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = source.baseUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilledTonalButton(
                            onClick = { onOpenSource(source) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Latest", fontSize = 12.sp)
                        }
                        Button(
                            onClick = { onOpenSource(source) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Popular", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SourceCatalogGrid(
    mangaList: List<SourceMangaItem>,
    onMangaClick: (SourceMangaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (mangaList.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No manga found for this query",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 130.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(mangaList) { manga ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMangaClick(manga) }
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        AsyncImage(
                            model = manga.coverUrl,
                            contentDescription = manga.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Source Badge
                        Surface(
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(bottomEnd = 6.dp),
                            modifier = Modifier.align(Alignment.TopStart)
                        ) {
                            Text(
                                text = manga.sourceName,
                                fontSize = 10.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = manga.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = manga.genres.take(2).joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExtensionsList(
    searchQuery: String,
    selectedLanguage: String,
    onSelectLanguage: (String) -> Unit,
    installedExtensions: List<ExtensionItem>,
    availableExtensions: List<ExtensionItem>,
    installingPkgs: Set<String>,
    repositories: List<Pair<String, String>>,
    onAddRepoClick: () -> Unit,
    onInstallExtension: (ExtensionItem) -> Unit,
    onUninstallExtension: (ExtensionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Language Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val languages = listOf("all" to "All", "en" to "English", "ja" to "Japanese", "ko" to "Korean", "es" to "Spanish")
                items(languages) { (code, name) ->
                    FilterChip(
                        selected = selectedLanguage == code,
                        onClick = { onSelectLanguage(code) },
                        label = { Text(name, fontSize = 12.sp) }
                    )
                }
            }
        }

        // Active Repositories Banner
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Extension Repositories (${repositories.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Keiyoushi, Tachiyomi, and custom extension feeds connected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    IconButton(onClick = onAddRepoClick) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Add Repo", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // INSTALLED EXTENSIONS SECTION
        if (installedExtensions.isNotEmpty()) {
            item {
                Text(
                    text = "Installed (${installedExtensions.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(installedExtensions) { ext ->
                ExtensionCard(
                    extension = ext,
                    isInstalling = installingPkgs.contains(ext.pkgName),
                    onInstallClick = { onInstallExtension(ext) },
                    onUninstallClick = { onUninstallExtension(ext) }
                )
            }
        }

        // AVAILABLE EXTENSIONS SECTION
        item {
            Text(
                text = "Available Extensions (${availableExtensions.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (availableExtensions.isEmpty()) {
            item {
                Text(
                    text = "All extensions from connected repositories are currently installed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(availableExtensions) { ext ->
                ExtensionCard(
                    extension = ext,
                    isInstalling = installingPkgs.contains(ext.pkgName),
                    onInstallClick = { onInstallExtension(ext) },
                    onUninstallClick = { onUninstallExtension(ext) }
                )
            }
        }
    }
}

@Composable
fun ExtensionCard(
    extension: ExtensionItem,
    isInstalling: Boolean,
    onInstallClick: () -> Unit,
    onUninstallClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Extension Icon
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (extension.isInstalled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (extension.isInstalled) Icons.Default.Extension else Icons.Outlined.Extension,
                        contentDescription = null,
                        tint = if (extension.isInstalled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = extension.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = extension.lang.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "v${extension.versionName} • ${extension.sources.size} source(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (extension.description.isNotBlank()) {
                    Text(
                        text = extension.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Button
            if (isInstalling) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp
                )
            } else if (extension.isInstalled) {
                OutlinedButton(
                    onClick = onUninstallClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Uninstall", fontSize = 12.sp)
                }
            } else {
                Button(
                    onClick = onInstallClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text("Install", fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceMangaDetailSheet(
    manga: SourceMangaItem,
    onDismiss: () -> Unit,
    onAddToLibrary: () -> Unit,
    onReadChapter: (SourceChapterItem) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AsyncImage(
                    model = manga.coverUrl,
                    contentDescription = manga.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(110.dp)
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = manga.sourceName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = manga.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = manga.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Status: ${manga.status}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onAddToLibrary,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add to Library")
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Genres
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(manga.genres) { genre ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(genre, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = manga.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Chapters (${manga.chapters.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(manga.chapters) { chapter ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onReadChapter(chapter) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = chapter.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "${chapter.scanlator} • ${chapter.releaseDate}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Read",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
