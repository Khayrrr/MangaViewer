package com.khayrrr.mangaviewer.ui.hidden

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import coil3.compose.AsyncImage
import com.khayrrr.mangaviewer.data.model.Album
import com.khayrrr.mangaviewer.data.model.ImageItem
import com.khayrrr.mangaviewer.data.preferences.AlbumViewStyle
import com.khayrrr.mangaviewer.ui.home.components.AlbumGridCard
import com.khayrrr.mangaviewer.ui.home.components.AlbumListRow
import com.khayrrr.mangaviewer.ui.home.components.AlbumMiniGridCard
import com.khayrrr.mangaviewer.util.BiometricHelper
import com.khayrrr.mangaviewer.util.HiddenSession
import com.khayrrr.mangaviewer.util.pdfThumbOrSelf
import org.koin.androidx.compose.koinViewModel

private enum class LockState { CHECKING, UNLOCKED, UNAVAILABLE }

private fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context = this
    while (true) {
        if (current is FragmentActivity) return current
        if (current is ContextWrapper) current = current.baseContext else return null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenAlbumsScreen(
    onNavigateBack: () -> Unit,
    onOpenReader: (String) -> Unit,
    viewModel: HiddenAlbumsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var lockState by remember { mutableStateOf(LockState.CHECKING) }

    var menuExpanded by remember { mutableStateOf(false) }
    var showViewStyleDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }
    var showChangeCoverDialog by remember { mutableStateOf(false) }

    val isSelectionMode = state.selectedUris.isNotEmpty()
    val selectedAlbums = remember(state.selectedUris, state.albums) {
        state.albums.filter { state.selectedUris.contains(it.uri) }
    }
    val singleSelected = if (selectedAlbums.size == 1) selectedAlbums.first() else null

    LaunchedEffect(Unit) {
        val required = viewModel.isLockRequired()
        if (!required || HiddenSession.unlocked) {
            lockState = LockState.UNLOCKED
            return@LaunchedEffect
        }
        val activity = context.findFragmentActivity()
        if (activity == null || !BiometricHelper.canAuthenticate(activity)) {
            lockState = LockState.UNAVAILABLE
            return@LaunchedEffect
        }
        BiometricHelper.authenticate(
            activity = activity,
            title = "Unlock hidden albums",
            onSuccess = { lockState = LockState.UNLOCKED },
            onFailure = { onNavigateBack() }
        )
    }

    BackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }

    val onAlbumClick: (Album) -> Unit = { album ->
        if (isSelectionMode) viewModel.toggleSelection(album.uri) else onOpenReader(album.uri)
    }
    val onAlbumLongClick: (Album) -> Unit = { album ->
        if (isSelectionMode) viewModel.toggleSelection(album.uri) else viewModel.enterSelection(album.uri)
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .statusBarsPadding()
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close selection")
                        }

                        Text(
                            text = "${state.selectedUris.size}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select all")
                        }

                        IconButton(onClick = { viewModel.unhideSelected() }) {
                            Icon(Icons.Default.Visibility, contentDescription = "Unhide")
                        }

                        IconButton(onClick = { showRemoveDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove")
                        }

                        if (singleSelected != null) {
                            IconButton(onClick = { showChangeCoverDialog = true }) {
                                Icon(Icons.Default.Image, contentDescription = "Change cover image")
                            }
                        }
                    }
                }
            } else {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Hidden Albums",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        if (lockState == LockState.UNLOCKED) {
                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
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
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (lockState) {
                LockState.CHECKING -> Unit

                LockState.UNAVAILABLE -> {
                    Text(
                        text = "Set a screen lock (PIN, pattern, password or fingerprint) in Android settings to use the hidden albums lock.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(32.dp)
                    )
                }

                LockState.UNLOCKED -> {
                    if (state.albums.isEmpty()) {
                        Text(
                            text = "No hidden albums",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        when (state.viewStyle) {
                            AlbumViewStyle.GRID_2 -> {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    contentPadding = PaddingValues(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(state.albums, key = { it.uri }) { album ->
                                        AlbumGridCard(
                                            album = album,
                                            isSelected = state.selectedUris.contains(album.uri),
                                            onClick = { onAlbumClick(album) },
                                            onLongClick = { onAlbumLongClick(album) }
                                        )
                                    }
                                }
                            }

                            AlbumViewStyle.LIST -> {
                                LazyColumn(
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(state.albums, key = { it.uri }) { album ->
                                        AlbumListRow(
                                            album = album,
                                            isSelected = state.selectedUris.contains(album.uri),
                                            onClick = { onAlbumClick(album) },
                                            onLongClick = { onAlbumLongClick(album) }
                                        )
                                    }
                                }
                            }

                            AlbumViewStyle.MINI_GRID_3 -> {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    contentPadding = PaddingValues(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(state.albums, key = { it.uri }) { album ->
                                        AlbumMiniGridCard(
                                            album = album,
                                            isSelected = state.selectedUris.contains(album.uri),
                                            onClick = { onAlbumClick(album) },
                                            onLongClick = { onAlbumLongClick(album) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // View style dialog
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
                                .clickable {
                                    viewModel.setViewStyle(style)
                                    showViewStyleDialog = false
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = state.viewStyle == style, onClick = null)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showViewStyleDialog = false }) { Text("Close") }
            }
        )
    }

    // Remove dialog
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
                            viewModel.removeSelected(fromDevice = false)
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
                TextButton(onClick = { showRemoveDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showConfirmDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteDialog = false },
            title = { Text("Permanently Delete?") },
            text = {
                Text("Are you sure you want to delete the selected folder(s) and all files inside from your storage? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDeleteDialog = false
                        viewModel.removeSelected(fromDevice = true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Change cover (single album)
    if (showChangeCoverDialog && singleSelected != null) {
        var albumImages by remember { mutableStateOf<List<ImageItem>?>(null) }

        LaunchedEffect(singleSelected.uri) {
            albumImages = viewModel.getImagesForAlbum(singleSelected.uri)
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
                                            viewModel.setCustomCover(singleSelected.uri, pdfThumbOrSelf(image.uri))
                                            showChangeCoverDialog = false
                                        }
                                ) {
                                    AsyncImage(
                                        model = pdfThumbOrSelf(image.uri),
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
                TextButton(onClick = { showChangeCoverDialog = false }) { Text("Cancel") }
            }
        )
    }
}