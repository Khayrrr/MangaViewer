package com.khayrrr.mangaviewer.ui.reader

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil3.compose.AsyncImage
import com.khayrrr.mangaviewer.data.model.ImageItem
import com.khayrrr.mangaviewer.data.preferences.AppSettings
import com.khayrrr.mangaviewer.data.preferences.PageIndicatorFormat
import com.khayrrr.mangaviewer.data.preferences.ReaderBackground
import com.khayrrr.mangaviewer.ui.theme.PillShape
import com.khayrrr.mangaviewer.util.ShareHelper
import org.koin.androidx.compose.koinViewModel

private const val MAX_ZOOM = 4f
private const val DEFAULT_ASPECT = 3f / 4f

private enum class ReaderAction { WALLPAPER, RENAME, DELETE }

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (true) {
        if (current is Activity) return current
        if (current is ContextWrapper) current = current.baseContext else return null
    }
}

private fun readerBackgroundColor(bg: ReaderBackground): Color = when (bg) {
    ReaderBackground.BLACK -> Color(0xFF000000)
    ReaderBackground.DARK -> Color(0xFF18181B)
    ReaderBackground.SEPIA -> Color(0xFFF5EBD7)
    ReaderBackground.WHITE -> Color(0xFFFFFFFF)
}

@Composable
fun ReaderScreen(
    albumUri: String,
    initialPage: Int,
    onNavigateBack: () -> Unit,
    viewModel: ReaderViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val aspects by viewModel.aspects.collectAsState()
    val favoriteUris by viewModel.favoriteImageUris.collectAsState()

    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(view) { view.context.findActivity() }
    var uiVisible by remember { mutableStateOf(false) }
    val barsHidden = settings.hideSystemBars && !uiVisible

    LaunchedEffect(albumUri, initialPage) {
        viewModel.load(albumUri, initialPage)
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(barsHidden) {
        val window = activity?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (barsHidden) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.window?.let {
                WindowCompat.getInsetsController(it, view).show(WindowInsetsCompat.Type.systemBars())
            }
            viewModel.saveNow()
        }
    }

    DisposableEffect(settings.keepScreenOn) {
        view.keepScreenOn = settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    val background = readerBackgroundColor(settings.readerBackground)
    val textColor = if (background.luminance() > 0.5f) Color.Black else Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        when {
            state.loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            state.images.isEmpty() -> {
                Text(
                    text = "No images in this album",
                    color = textColor,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            else -> {
                ReaderContent(
                    images = state.images,
                    startPage = state.startPage,
                    aspects = aspects,
                    favoriteUris = favoriteUris,
                    settings = settings,
                    uiVisible = uiVisible,
                    onToggleUi = { uiVisible = !uiVisible },
                    onNavigateBack = onNavigateBack,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun ReaderContent(
    images: List<ImageItem>,
    startPage: Int,
    aspects: Map<String, Float>,
    favoriteUris: Set<String>,
    settings: AppSettings,
    uiVisible: Boolean,
    onToggleUi: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ReaderViewModel
) {
    val context = LocalContext.current
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startPage)
    val currentPage by remember(images) {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.totalItemsCount > 0 && !listState.canScrollForward) {
                images.lastIndex
            } else {
                listState.firstVisibleItemIndex.coerceIn(0, images.lastIndex)
            }
        }
    }

    LaunchedEffect(currentPage) {
        viewModel.onPageChanged(currentPage)
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var boxWidth by remember { mutableFloatStateOf(0f) }
    val toggleUi by rememberUpdatedState(onToggleUi)

    var pendingAction by remember { mutableStateOf<ReaderAction?>(null) }
    var actionImage by remember { mutableStateOf<ImageItem?>(null) }

    val systemInsets = WindowInsets.systemBars.asPaddingValues()
    val contentPadding = if (settings.hideSystemBars) {
        PaddingValues(0.dp)
    } else {
        PaddingValues(
            top = systemInsets.calculateTopPadding(),
            bottom = systemInsets.calculateBottomPadding()
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Reading column with tap, double-tap and pinch zoom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { boxWidth = it.width.toFloat() }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { toggleUi() },
                        onDoubleTap = {
                            if (scale > 1f) {
                                scale = 1f
                                offsetX = 0f
                            } else {
                                scale = 2f
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val pressedCount = event.changes.count { it.pressed }
                            if (pressedCount > 1 || scale > 1f) {
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val centroid = event.calculateCentroid(useCurrent = true)
                                val oldScale = scale
                                val newScale = (oldScale * zoom).coerceIn(1f, MAX_ZOOM)
                                var newOffset = offsetX
                                if (centroid.isSpecified && oldScale > 0f) {
                                    val q = centroid.x - boxWidth / 2f
                                    newOffset = q - (newScale / oldScale) * (q - newOffset)
                                }
                                newOffset += pan.x
                                val maxOffset = (newScale - 1f) * boxWidth / 2f
                                offsetX = newOffset.coerceIn(-maxOffset, maxOffset)
                                scale = newScale
                                if (newScale > 1f && pan.y != 0f) {
                                    listState.dispatchRawDelta(-pan.y / newScale)
                                }
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            LazyColumn(
                state = listState,
                contentPadding = contentPadding,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                    }
            ) {
                items(images, key = { it.uri }) { image ->
                    ReaderPage(
                        image = image,
                        aspect = aspects[image.uri],
                        onAspectKnown = { viewModel.reportAspect(image.uri, it) }
                    )
                }
            }
        }

        // Page indicator (always visible)
        val label = when (settings.pageIndicatorFormat) {
            PageIndicatorFormat.COUNT -> "${currentPage + 1}/${images.size}"
            PageIndicatorFormat.PERCENT -> "${((currentPage + 1) * 100) / images.size}%"
        }
        Surface(
            shape = PillShape,
            color = Color.Black.copy(alpha = 0.55f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 12.dp, bottom = 16.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        if (uiVisible) {
            // Back button (top left)
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // Floating pill toolbar (top right)
            Surface(
                shape = PillShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentImage = images[currentPage]
                    val isFavorite = favoriteUris.contains(currentImage.uri)

                    IconButton(
                        onClick = { viewModel.toggleFavorite(currentImage) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = LocalContentColor.current,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Set as wallpaper") },
                                leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    actionImage = currentImage
                                    pendingAction = ReaderAction.WALLPAPER
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share image") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    ShareHelper.shareSingleImage(
                                        context,
                                        Uri.parse(currentImage.uri),
                                        currentImage.mimeType
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Rename image") },
                                leadingIcon = {
                                    Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    actionImage = currentImage
                                    pendingAction = ReaderAction.RENAME
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete image") },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    actionImage = currentImage
                                    pendingAction = ReaderAction.DELETE
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            DropdownMenuItem(
                                text = { Text("Keep screen on") },
                                trailingIcon = {
                                    Switch(checked = settings.keepScreenOn, onCheckedChange = null)
                                },
                                onClick = { viewModel.setKeepScreenOn(!settings.keepScreenOn) }
                            )
                            DropdownMenuItem(
                                text = { Text("Hide system bars") },
                                trailingIcon = {
                                    Switch(checked = settings.hideSystemBars, onCheckedChange = null)
                                },
                                onClick = { viewModel.setHideSystemBars(!settings.hideSystemBars) }
                            )
                        }
                    }
                }
            }

            // Thin progress bar (only with the toolbar)
            LinearProgressIndicator(
                progress = { (currentPage + 1).toFloat() / images.size },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(3.dp)
            )
        }
    }

    // Dialogs for the page actions
    val target = actionImage
    if (target != null) {
        when (pendingAction) {
            ReaderAction.WALLPAPER -> {
                AlertDialog(
                    onDismissRequest = { pendingAction = null },
                    title = { Text("Set as wallpaper") },
                    text = {
                        Column {
                            listOf(
                                WallpaperTarget.HOME to "Home screen",
                                WallpaperTarget.LOCK to "Lock screen",
                                WallpaperTarget.BOTH to "Home and lock screens"
                            ).forEach { (wallpaperTarget, label) ->
                                TextButton(
                                    onClick = {
                                        pendingAction = null
                                        viewModel.setWallpaper(target, wallpaperTarget)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { pendingAction = null }) { Text("Cancel") }
                    }
                )
            }

            ReaderAction.RENAME -> {
                var renameInput by remember(target.uri) {
                    mutableStateOf(target.name.substringBeforeLast('.'))
                }
                val ext = target.name.substringAfterLast('.', "")
                val valid = renameInput.isNotBlank() && !renameInput.contains('/')
                AlertDialog(
                    onDismissRequest = { pendingAction = null },
                    title = { Text("Rename image") },
                    text = {
                        OutlinedTextField(
                            value = renameInput,
                            onValueChange = { renameInput = it },
                            label = { Text("File name") },
                            suffix = { if (ext.isNotEmpty()) Text(".$ext") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            enabled = valid,
                            onClick = {
                                pendingAction = null
                                viewModel.renameImage(target, renameInput.trim())
                            }
                        ) {
                            Text("Rename")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingAction = null }) { Text("Cancel") }
                    }
                )
            }

            ReaderAction.DELETE -> {
                AlertDialog(
                    onDismissRequest = { pendingAction = null },
                    title = { Text("Delete image?") },
                    text = {
                        Text("\"${target.name}\" will be permanently deleted from your device. This cannot be undone.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                pendingAction = null
                                viewModel.deleteImage(target)
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingAction = null }) { Text("Cancel") }
                    }
                )
            }

            null -> Unit
        }
    }
}

@Composable
private fun ReaderPage(
    image: ImageItem,
    aspect: Float?,
    onAspectKnown: (Float) -> Unit
) {
    AsyncImage(
        model = image.uri,
        contentDescription = image.name,
        contentScale = ContentScale.FillWidth,
        onSuccess = { success ->
            if (aspect == null) {
                val size = success.painter.intrinsicSize
                if (size.isSpecified && size.height > 0f) {
                    onAspectKnown(size.width / size.height)
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspect ?: DEFAULT_ASPECT)
    )
}