package com.khayrrr.mangaviewer.ui.home

import android.content.Context
import android.net.Uri
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.khayrrr.mangaviewer.data.model.Album
import com.khayrrr.mangaviewer.data.model.ImageItem
import com.khayrrr.mangaviewer.data.preferences.AlbumSortBy
import com.khayrrr.mangaviewer.data.preferences.AlbumViewStyle
import com.khayrrr.mangaviewer.data.preferences.SortOrder
import com.khayrrr.mangaviewer.ui.home.components.AlbumGridCard
import com.khayrrr.mangaviewer.ui.home.components.AlbumListRow
import com.khayrrr.mangaviewer.ui.home.components.AlbumMiniGridCard
import com.khayrrr.mangaviewer.util.ShareHelper
import kotlinx.coroutines.launch
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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { viewModel.addRootFolder(it) }
    }

    // Top menu states
    var menuExpanded by remember { mutableStateOf(false) }
    var contextMenuExpanded by remember { mutableStateOf(false) }
    var showViewStyleDialog by remember { mutableStateOf(false) }
    var showSortByDialog by remember { mutableStateOf(false) }
    var showOrderDialog by remember { mutableStateOf(false) }

    // Action dialog states
    var showRemoveDialog by remember { mutableStateOf(false) }
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }
    var showShareDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showEditTagsDialog by remember { mutableStateOf(false) }
    var showChangeCoverDialog by remember { mutableStateOf(false) }

    val selectedAlbums = remember(uiState.selectedAlbumUris, uiState.albums) {
        viewModel.getSelectedAlbums()
    }
    val singleSelectedAlbum = selectedAlbums.firstOrNull()

    Scaffold(
        topBar = {
            when {
                // Contextual Action Bar Overlay in Selection Mode
                uiState.isSelectionMode -> {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shadowElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close selection"
                                )
                            }

                            Text(
                                text = "${uiState.selectedAlbumUris.size} selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )

                            // Actions for ONE album selected
                            if (uiState.selectedAlbumUris.size == 1 && singleSelectedAlbum != null) {
                                // Remove
                                IconButton(onClick = { showRemoveDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove album"
                                    )
                                }

                                // Pin / Unpin
                                IconButton(onClick = { viewModel.togglePinSelectedAlbum() }) {
                                    Icon(
                                        imageVector = if (singleSelectedAlbum.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                        contentDescription = "Pin / Unpin",
                                        tint = if (singleSelectedAlbum.isPinned) Color(0xFFFFD54F) else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Rename
                                IconButton(onClick = {
                                    renameInput = singleSelectedAlbum.name
                                    showRenameDialog = true
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.DriveFileRenameOutline,
                                        contentDescription = "Rename album"
                                    )
                                }

                                // Share
                                IconButton(onClick = { showShareDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share album"
                                    )
                                }

                                // More overflow menu (Change cover, Edit tags, Hide, Details)
                                Box {
                                    IconButton(onClick = { contextMenuExpanded = true }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "More actions"
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = contextMenuExpanded,
                                        onDismissRequest = { contextMenuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Change cover image") },
                                            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                                            onClick = {
                                                contextMenuExpanded = false
                                                showChangeCoverDialog = true
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = { Text("Edit tags") },
                                            leadingIcon = { Icon(Icons.Default.Label, contentDescription = null) },
                                            onClick = {
                                                contextMenuExpanded = false
                                                showEditTagsDialog = true
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = { Text("Hide album") },
                                            leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) },
                                            onClick = {
                                                contextMenuExpanded = false
                                                viewModel.hideSelectedAlbum()
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = { Text("Details") },
                                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                            onClick = {
                                                contextMenuExpanded = false
                                                showDetailsDialog = true
                                            }
                                        )
                                    }
                                }
                            } else {
                                // Actions for MULTIPLE albums selected: only Remove and Share
                                IconButton(onClick = { showRemoveDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove albums"
                                    )
                                }

                                IconButton(onClick = { showShareDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share albums"
                                    )
                                }
                            }
                        }
                    }
                }

                // Expanded Search Bar
                uiState.isSearchActive -> {
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
                                placeholder = { Text("Search titles and tags…") },
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
                }

                // Standard Top App Bar
                else -> {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Manga Reader",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        },
                        actions = {
                            IconButton(onClick = { viewModel.setSearchActive(true) }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search"
                                )
                            }

                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options"
                                )
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("View style") },
                                    leadingIcon = { Icon(Icons.Default.GridView, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        showViewStyleDialog = true
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Sort by") },
                                    leadingIcon = { Icon(Icons.Default.Sort, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        showSortByDialog = true
                                    }
                                )

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
                                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onNavigateToFavorites()
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Hidden albums") },
                                    leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onNavigateToHidden()
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Refresh") },
                                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.refresh()
                                    }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                DropdownMenuItem(
                                    text = { Text("Settings") },
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
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
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->

        PullToRefreshBox(
            isRefreshing = uiState.isScanning,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                !uiState.hasRootFolders -> {
                    EmptyFolderPrompt(
                        onSelectFolder = { folderPickerLauncher.launch(null) }
                    )
                }

                uiState.albums.isEmpty() && !uiState.isScanning -> {
                    EmptyAlbumsPrompt(
                        searchQuery = uiState.searchQuery,
                        onAddFolder = { folderPickerLauncher.launch(null) }
                    )
                }

                else -> {
                    when (uiState.viewStyle) {
                        AlbumViewStyle.GRID_2 -> {
                            AlbumGridView(
                                pinnedAlbums = uiState.pinnedAlbums,
                                otherAlbums = uiState.otherAlbums,
                                selectedAlbumUris = uiState.selectedAlbumUris,
                                isSelectionMode = uiState.isSelectionMode,
                                onAlbumClick = { album ->
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleAlbumSelection(album.uri)
                                    } else {
                                        onOpenReader(album.uri)
                                    }
                                },
                                onAlbumLongClick = { album ->
                                    if (!uiState.isSelectionMode) {
                                        viewModel.enterSelectionMode(album.uri)
                                    } else {
                                        viewModel.toggleAlbumSelection(album.uri)
                                    }
                                }
                            )
                        }

                        AlbumViewStyle.LIST -> {
                            AlbumListView(
                                pinnedAlbums = uiState.pinnedAlbums,
                                otherAlbums = uiState.otherAlbums,
                                selectedAlbumUris = uiState.selectedAlbumUris,
                                isSelectionMode = uiState.isSelectionMode,
                                onAlbumClick = { album ->
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleAlbumSelection(album.uri)
                                    } else {
                                        onOpenReader(album.uri)
                                    }
                                },
                                onAlbumLongClick = { album ->
                                    if (!uiState.isSelectionMode) {
                                        viewModel.enterSelectionMode(album.uri)
                                    } else {
                                        viewModel.toggleAlbumSelection(album.uri)
                                    }
                                }
                            )
                        }

                        AlbumViewStyle.MINI_GRID_3 -> {
                            AlbumMiniGridView(
                                pinnedAlbums = uiState.pinnedAlbums,
                                otherAlbums = uiState.otherAlbums,
                                selectedAlbumUris = uiState.selectedAlbumUris,
                                isSelectionMode = uiState.isSelectionMode,
                                onAlbumClick = { album ->
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleAlbumSelection(album.uri)
                                    } else {
                                        onOpenReader(album.uri)
                                    }
                                },
                                onAlbumLongClick = { album ->
                                    if (!uiState.isSelectionMode) {
                                        viewModel.enterSelectionMode(album.uri)
                                    } else {
                                        viewModel.toggleAlbumSelection(album.uri)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // 1. Remove Dialog (2 options: Remove from Library vs Delete from Device)
    if (showRemoveDialog) {
        val count = selectedAlbums.size
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text(if (count == 1) "Remove Album" else "Remove $count Albums") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Choose how you would like to remove ${if (count == 1) "this album" else "these albums"}:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedButton(
                        onClick = {
                            showRemoveDialog = false
                            viewModel.removeSelectedAlbums(fromDevice = false)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                            Text("Remove from library", fontWeight = FontWeight.Bold)
                            Text(
                                "Hides it from the app. Files remain untouched on device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = {
                            showRemoveDialog = false
                            showConfirmDeleteDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                            Text("Delete from device", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError)
                            Text(
                                "Permanently deletes the album folder and all its files.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onError.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Secondary Confirmation for Delete from Device
    if (showConfirmDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteDialog = false },
            title = { Text("Permanently Delete?") },
            text = {
                Text("Are you sure you want to delete this folder and all images inside it from your storage? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDeleteDialog = false
                        viewModel.removeSelectedAlbums(fromDevice = true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 2. Rename Dialog
    if (showRenameDialog && singleSelectedAlbum != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Album") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            showRenameDialog = false
                            viewModel.renameSelectedAlbum(renameInput.trim())
                        }
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 3. Share Dialog (Share as Images vs Share as ZIP)
    if (showShareDialog && selectedAlbums.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("Share Album") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showShareDialog = false
                            coroutineScope.launch {
                                val allImages = mutableListOf<Uri>()
                                selectedAlbums.forEach { album ->
                                    val images = viewModel.getImagesForAlbum(album.uri)
                                    allImages.addAll(images.map { Uri.parse(it.uri) })
                                }
                                ShareHelper.shareImages(context, allImages, "Share Images")
                                viewModel.clearSelection()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Share as Images")
                    }

                    OutlinedButton(
                        onClick = {
                            showShareDialog = false
                            coroutineScope.launch {
                                val map = mutableMapOf<Album, List<ImageItem>>()
                                selectedAlbums.forEach { album ->
                                    map[album] = viewModel.getImagesForAlbum(album.uri)
                                }
                                val baseName = if (selectedAlbums.size == 1) selectedAlbums.first().name else "manga_export"
                                ShareHelper.shareAlbumsAsZip(context, map, baseName)
                                viewModel.clearSelection()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Share as ZIP")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showShareDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 4. Details Dialog
    if (showDetailsDialog && singleSelectedAlbum != null) {
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = { Text("Album Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow("Name", singleSelectedAlbum.name)
                    DetailRow("Full Path", singleSelectedAlbum.path)
                    DetailRow("Total Images", "${singleSelectedAlbum.totalImages} pages")
                    DetailRow("Size", Formatter.formatFileSize(context, singleSelectedAlbum.totalSize))
                    DetailRow(
                        "Date Modified",
                        DateUtils.formatDateTime(
                            context,
                            singleSelectedAlbum.lastModified,
                            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_YEAR
                        )
                    )
                    DetailRow(
                        "Tags",
                        if (singleSelectedAlbum.tags.isEmpty()) "None" else singleSelectedAlbum.tags.joinToString(", ")
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // 5. Edit Tags Dialog
    if (showEditTagsDialog && singleSelectedAlbum != null) {
        val currentTags = remember { mutableStateListOf<String>().apply { addAll(singleSelectedAlbum.tags) } }
        var newTagInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showEditTagsDialog = false },
            title = { Text("Edit Tags") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newTagInput,
                            onValueChange = { newTagInput = it },
                            placeholder = { Text("New tag") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val tag = newTagInput.trim()
                                if (tag.isNotEmpty() && !currentTags.contains(tag)) {
                                    currentTags.add(tag)
                                    newTagInput = ""
                                }
                            }
                        ) {
                            Text("Add")
                        }
                    }

                    if (currentTags.isEmpty()) {
                        Text(
                            text = "No tags added yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            currentTags.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = tag,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove tag",
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { currentTags.remove(tag) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setAlbumTags(singleSelectedAlbum.uri, currentTags.toList())
                        showEditTagsDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTagsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 6. Change Cover Image Dialog
    if (showChangeCoverDialog && singleSelectedAlbum != null) {
        var albumImages by remember { mutableStateOf<List<ImageItem>?>(null) }

        LaunchedEffect(singleSelectedAlbum.uri) {
            albumImages = viewModel.getImagesForAlbum(singleSelectedAlbum.uri)
        }

        AlertDialog(
            onDismissRequest = { showChangeCoverDialog = false },
            title = { Text("Choose Cover Image") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    val images = albumImages
                    if (images == null) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else if (images.isEmpty()) {
                        Text(
                            text = "No images found in album.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(images, key = { it.uri }) { image ->
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(3f / 4f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            viewModel.setCustomCover(singleSelectedAlbum.uri, image.uri)
                                            showChangeCoverDialog = false
                                        }
                                ) {
                                    AsyncImage(
                                        model = image.uri,
                                        contentDescription = image.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showChangeCoverDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // View Style Dialog
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

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f, fill = false)
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
    selectedAlbumUris: Set<String>,
    isSelectionMode: Boolean,
    onAlbumClick: (Album) -> Unit,
    onAlbumLongClick: (Album) -> Unit
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
                    isSelected = selectedAlbumUris.contains(album.uri),
                    onClick = { onAlbumClick(album) },
                    onLongClick = { onAlbumLongClick(album) }
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
                    isSelected = selectedAlbumUris.contains(album.uri),
                    onClick = { onAlbumClick(album) },
                    onLongClick = { onAlbumLongClick(album) }
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
    selectedAlbumUris: Set<String>,
    isSelectionMode: Boolean,
    onAlbumClick: (Album) -> Unit,
    onAlbumLongClick: (Album) -> Unit
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
                    isSelected = selectedAlbumUris.contains(album.uri),
                    onClick = { onAlbumClick(album) },
                    onLongClick = { onAlbumLongClick(album) }
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
                    isSelected = selectedAlbumUris.contains(album.uri),
                    onClick = { onAlbumClick(album) },
                    onLongClick = { onAlbumLongClick(album) }
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
    selectedAlbumUris: Set<String>,
    isSelectionMode: Boolean,
    onAlbumClick: (Album) -> Unit,
    onAlbumLongClick: (Album) -> Unit
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
                    isSelected = selectedAlbumUris.contains(album.uri),
                    onClick = { onAlbumClick(album) },
                    onLongClick = { onAlbumLongClick(album) }
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
                    isSelected = selectedAlbumUris.contains(album.uri),
                    onClick = { onAlbumClick(album) },
                    onLongClick = { onAlbumLongClick(album) }
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
