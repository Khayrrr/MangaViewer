package com.khayrrr.mangaviewer.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khayrrr.mangaviewer.data.model.Album
import com.khayrrr.mangaviewer.data.preferences.AlbumSortBy
import com.khayrrr.mangaviewer.data.preferences.AlbumViewStyle
import com.khayrrr.mangaviewer.data.preferences.SortOrder
import com.khayrrr.mangaviewer.ui.home.components.AlbumGridCard
import com.khayrrr.mangaviewer.ui.home.components.AlbumListRow
import com.khayrrr.mangaviewer.ui.home.components.AlbumMiniGridCard
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    onNavigateToSettings: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToHidden: () -> Unit,
    onOpenReader: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    // Launcher for choosing root folder via SAF
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { viewModel.addRootFolder(it) }
    }

    // Dropdown & Dialog state
    var menuExpanded by remember { mutableStateOf(false) }
    var showViewStyleDialog by remember { mutableStateOf(false) }
    var showSortByDialog by remember { mutableStateOf(false) }
    var showOrderDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (uiState.isSearchActive) {
                // Expanded Search Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            viewModel.setSearchActive(false)
                            focusManager.clearFocus()
                        }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Close Search"
                            )
                        }

                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            placeholder = { Text("Search manga titles and tags…") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            modifier = Modifier.weight(1f)
                        )

                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear text"
                                )
                            }
                        }
                    }
                }
            } else {
                // Standard Top App Bar
                TopAppBar(
                    title = {
                        Text(
                            text = "Manga Reader",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    },
                    actions = {
                        // Search icon
                        IconButton(onClick = { viewModel.setSearchActive(true) }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }

                        // Three-dots menu icon
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }

                        // Overflow Dropdown Menu
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("View style") },
                                leadingIcon = {
                                    Icon(Icons.Default.GridView, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    showViewStyleDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Sort by") },
                                leadingIcon = {
                                    Icon(Icons.Default.Sort, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    showSortByDialog = true
                                }
                            )

                            // Order is ENABLED ONLY when sorting alphabetically
                            val isAlphabetical = uiState.sortBy == AlbumSortBy.ALPHABETICAL
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Order",
                                        color = if (isAlphabetical) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    )
                                },
                                enabled = isAlphabetical,
                                onClick = {
                                    menuExpanded = false
                                    showOrderDialog = true
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            DropdownMenuItem(
                                text = { Text("Favorites") },
                                leadingIcon = {
                                    Icon(Icons.Default.Star, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onNavigateToFavorites()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Hidden albums") },
                                leadingIcon = {
                                    Icon(Icons.Default.VisibilityOff, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onNavigateToHidden()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Refresh") },
                                leadingIcon = {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.refresh()
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            DropdownMenuItem(
                                text = { Text("Settings") },
                                leadingIcon = {
                                    Icon(Icons.Default.Settings, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onNavigateToSettings()
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->

        // Pull to refresh container
        PullToRefreshBox(
            isRefreshing = uiState.isScanning,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                // Empty state when no root folder is added
                !uiState.hasRootFolders -> {
                    EmptyFolderPrompt(
                        onSelectFolder = { folderPickerLauncher.launch(null) }
                    )
                }

                // Empty state when root folders exist but no albums found
                uiState.albums.isEmpty() && !uiState.isScanning -> {
                    EmptyAlbumsPrompt(
                        searchQuery = uiState.searchQuery,
                        onAddFolder = { folderPickerLauncher.launch(null) }
                    )
                }

                // Real album list/grid views
                else -> {
                    when (uiState.viewStyle) {
                        AlbumViewStyle.GRID_2 -> {
                            AlbumGridView(
                                pinnedAlbums = uiState.pinnedAlbums,
                                otherAlbums = uiState.otherAlbums,
                                onAlbumClick = onOpenReader
                            )
                        }

                        AlbumViewStyle.LIST -> {
                            AlbumListView(
                                pinnedAlbums = uiState.pinnedAlbums,
                                otherAlbums = uiState.otherAlbums,
                                onAlbumClick = onOpenReader
                            )
                        }

                        AlbumViewStyle.MINI_GRID_3 -> {
                            AlbumMiniGridView(
                                pinnedAlbums = uiState.pinnedAlbums,
                                otherAlbums = uiState.otherAlbums,
                                onAlbumClick = onOpenReader
                            )
                        }
                    }
                }
            }
        }
    }

    // View Style Selection Dialog
    if (showViewStyleDialog) {
        AlertDialog(
            onDismissRequest = { showViewStyleDialog = false },
            title = { Text("View Style") },
            text = {
                Column {
                    listOf(
                        AlbumViewStyle.GRID_2 to "Grid (2 columns)",
                        AlbumViewStyle.LIST to "List",
                        AlbumViewStyle.MINI_GRID_3 to "Mini-grid (3 columns)"
                    ).forEach { (style, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = uiState.viewStyle == style,
                                onClick = {
                                    viewModel.setViewStyle(style)
                                    showViewStyleDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showViewStyleDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Sort By Dialog
    if (showSortByDialog) {
        AlertDialog(
            onDismissRequest = { showSortByDialog = false },
            title = { Text("Sort By") },
            text = {
                Column {
                    listOf(
                        AlbumSortBy.ALPHABETICAL to "Alphabetical",
                        AlbumSortBy.LAST_MODIFIED to "Last modified",
                        AlbumSortBy.OLDEST to "Oldest"
                    ).forEach { (sort, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = uiState.sortBy == sort,
                                onClick = {
                                    viewModel.setSortBy(sort)
                                    showSortByDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortByDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Order Dialog (Ascending / Descending)
    if (showOrderDialog) {
        AlertDialog(
            onDismissRequest = { showOrderDialog = false },
            title = { Text("Sort Order") },
            text = {
                Column {
                    listOf(
                        SortOrder.ASCENDING to "Ascending (A to Z)",
                        SortOrder.DESCENDING to "Descending (Z to A)"
                    ).forEach { (order, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = uiState.sortOrder == order,
                                onClick = {
                                    viewModel.setSortOrder(order)
                                    showOrderDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOrderDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * 2-Column Grid Layout (Default)
 */
@Composable
private fun AlbumGridView(
    pinnedAlbums: List<Album>,
    otherAlbums: List<Album>,
    onAlbumClick: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (pinnedAlbums.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                SectionHeader(
                    icon = Icons.Default.PushPin,
                    title = "PINNED ALBUMS",
                    count = pinnedAlbums.size,
                    isPinned = true
                )
            }
            items(pinnedAlbums, key = { it.uri }) { album ->
                AlbumGridCard(
                    album = album,
                    onClick = { onAlbumClick(album.uri) }
                )
            }
        }

        if (otherAlbums.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                SectionHeader(
                    icon = null,
                    title = "ALL ALBUMS",
                    count = otherAlbums.size,
                    isPinned = false
                )
            }
            items(otherAlbums, key = { it.uri }) { album ->
                AlbumGridCard(
                    album = album,
                    onClick = { onAlbumClick(album.uri) }
                )
            }
        }
    }
}

/**
 * List View Layout
 */
@Composable
private fun AlbumListView(
    pinnedAlbums: List<Album>,
    otherAlbums: List<Album>,
    onAlbumClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (pinnedAlbums.isNotEmpty()) {
            item {
                SectionHeader(
                    icon = Icons.Default.PushPin,
                    title = "PINNED ALBUMS",
                    count = pinnedAlbums.size,
                    isPinned = true
                )
            }
            items(pinnedAlbums, key = { it.uri }) { album ->
                AlbumListRow(
                    album = album,
                    onClick = { onAlbumClick(album.uri) }
                )
            }
        }

        if (otherAlbums.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                SectionHeader(
                    icon = null,
                    title = "ALL ALBUMS",
                    count = otherAlbums.size,
                    isPinned = false
                )
            }
            items(otherAlbums, key = { it.uri }) { album ->
                AlbumListRow(
                    album = album,
                    onClick = { onAlbumClick(album.uri) }
                )
            }
        }
    }
}

/**
 * 3-Column Mini-Grid Layout
 */
@Composable
private fun AlbumMiniGridView(
    pinnedAlbums: List<Album>,
    otherAlbums: List<Album>,
    onAlbumClick: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (pinnedAlbums.isNotEmpty()) {
            item(span = { GridItemSpan(3) }) {
                SectionHeader(
                    icon = Icons.Default.PushPin,
                    title = "PINNED ALBUMS",
                    count = pinnedAlbums.size,
                    isPinned = true
                )
            }
            items(pinnedAlbums, key = { it.uri }) { album ->
                AlbumMiniGridCard(
                    album = album,
                    onClick = { onAlbumClick(album.uri) }
                )
            }
        }

        if (otherAlbums.isNotEmpty()) {
            item(span = { GridItemSpan(3) }) {
                SectionHeader(
                    icon = null,
                    title = "ALL ALBUMS",
                    count = otherAlbums.size,
                    isPinned = false
                )
            }
            items(otherAlbums, key = { it.uri }) { album ->
                AlbumMiniGridCard(
                    album = album,
                    onClick = { onAlbumClick(album.uri) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    title: String,
    count: Int,
    isPinned: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                letterSpacing = 0.5.sp
            )
        }
        Text(
            text = "$count ${if (count == 1) "item" else "items"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun EmptyFolderPrompt(
    onSelectFolder: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Select a Manga Folder",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Pick one or more folders from your device storage containing your manga and comic chapters.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onSelectFolder,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Folder")
            }
        }
    }
}

@Composable
private fun EmptyAlbumsPrompt(
    searchQuery: String,
    onAddFolder: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoStories,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (searchQuery.isNotEmpty()) "No albums matching \"$searchQuery\"" else "No manga albums found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (searchQuery.isNotEmpty()) "Try a different search term or tag." else "Selected folders don't contain any supported image formats.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (searchQuery.isEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onAddFolder,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Another Folder")
                }
            }
        }
    }
}
