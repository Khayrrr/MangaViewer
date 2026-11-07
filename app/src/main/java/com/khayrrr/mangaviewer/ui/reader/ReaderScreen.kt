package com.khayrrr.mangaviewer.ui.reader

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate as animateValue
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlinx.coroutines.Job
import kotlin.math.abs
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.khayrrr.mangaviewer.data.model.ImageItem
import com.khayrrr.mangaviewer.data.preferences.AppSettings
import com.khayrrr.mangaviewer.data.preferences.PageIndicatorFormat
import com.khayrrr.mangaviewer.data.preferences.ReaderBackground
import com.khayrrr.mangaviewer.data.preferences.ReadingDirection
import com.khayrrr.mangaviewer.ui.theme.PillShape
import com.khayrrr.mangaviewer.util.PdfPages
import com.khayrrr.mangaviewer.util.ShareHelper
import com.khayrrr.mangaviewer.util.staticImageRequest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

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

/** Rebuilds the reading surface when the direction changes, keeping the current page. */
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
    var lastPage by remember { mutableIntStateOf(startPage) }

    key(settings.readingDirection) {
        val initialPage = remember { lastPage.coerceIn(0, images.lastIndex) }
        ReaderBody(
            images = images,
            initialPage = initialPage,
            aspects = aspects,
            favoriteUris = favoriteUris,
            settings = settings,
            uiVisible = uiVisible,
            onToggleUi = onToggleUi,
            onNavigateBack = onNavigateBack,
            onPageChanged = { lastPage = it },
            viewModel = viewModel
        )
    }
}

@Composable
private fun ReaderBody(
    images: List<ImageItem>,
    initialPage: Int,
    aspects: Map<String, Float>,
    favoriteUris: Set<String>,
    settings: AppSettings,
    uiVisible: Boolean,
    onToggleUi: () -> Unit,
    onNavigateBack: () -> Unit,
    onPageChanged: (Int) -> Unit,
    viewModel: ReaderViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isVertical = settings.readingDirection == ReadingDirection.VERTICAL
    val rtl = settings.readingDirection == ReadingDirection.RTL

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialPage)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { images.size })

    val currentPage by remember(images, isVertical) {
        derivedStateOf {
            if (isVertical) {
                val info = listState.layoutInfo
                if (info.totalItemsCount > 0 && !listState.canScrollForward) {
                    images.lastIndex
                } else {
                    listState.firstVisibleItemIndex.coerceIn(0, images.lastIndex)
                }
            } else {
                pagerState.currentPage.coerceIn(0, images.lastIndex)
            }
        }
    }

    LaunchedEffect(currentPage) {
        viewModel.onPageChanged(currentPage)
        onPageChanged(currentPage)
    }

    val showBackToTop by remember(images, isVertical) {
        derivedStateOf {
            images.size > 1 && if (isVertical) {
                listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 40
            } else {
                pagerState.currentPage > 0
            }
        }
    }

    // Seekbar (vertical mode): jump to the dragged page
    var seekTarget by remember { mutableIntStateOf(-1) }
    LaunchedEffect(seekTarget) {
        if (seekTarget >= 0) listState.scrollToItem(seekTarget)
    }

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
        if (isVertical) {
            VerticalPages(
                images = images,
                aspects = aspects,
                listState = listState,
                contentPadding = contentPadding,
                animate = settings.animateGifAvif,
                onToggleUi = onToggleUi,
                onAspectKnown = { uri, aspect -> viewModel.reportAspect(uri, aspect) }
            )
        } else {
            HorizontalPages(
                images = images,
                pagerState = pagerState,
                rtl = rtl,
                animate = settings.animateGifAvif,
                onToggleUi = onToggleUi
            )
        }

        // Page indicator: always in horizontal mode; in vertical mode the seekbar replaces it
        if (!isVertical || !uiVisible) {
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
        }

                // End of the album: button to jump back to the first page
        AnimatedVisibility(
            visible = showBackToTop,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 12.dp, bottom = if (isVertical && uiVisible) 96.dp else 56.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f)
            ) {
                IconButton(
                    onClick = {
                        scope.launch {
                            if (isVertical) listState.animateScrollToItem(0) else pagerState.animateScrollToPage(0)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Back to first page",
                        tint = Color.White
                    )
                }
            }
        }

        if (uiVisible) {
            // Back button (top left): same size and adaptive colours as the toolbar
            Surface(
                onClick = onNavigateBack,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(22.dp)
                    )
                }
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
                    modifier = Modifier.height(48.dp).padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentImage = images[currentPage]
                    val isFavorite = favoriteUris.contains(currentImage.uri)
                    val isPdfPage = PdfPages.isPdfPage(currentImage.uri)

                    IconButton(
                        onClick = { viewModel.toggleFavorite(currentImage) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = LocalContentColor.current,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f)
                        ) {
                            if (!isPdfPage) {
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
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            listOf(
                                ReadingDirection.VERTICAL to "Vertical scroll",
                                ReadingDirection.LTR to "Left to right",
                                ReadingDirection.RTL to "Right to left"
                            ).forEach { (direction, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    leadingIcon = {
                                        RadioButton(
                                            selected = settings.readingDirection == direction,
                                            onClick = null
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.setReadingDirection(direction)
                                    }
                                )
                            }

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

            // Seekbar (vertical mode only)
            if (isVertical && images.size > 1) {
                SeekBar(
                    currentPage = currentPage,
                    total = images.size,
                    onSeek = { seekTarget = it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }
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
                            colors = ButtonDefaults.buttonColors(
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
@OptIn(ExperimentalMaterial3Api::class)
private fun SeekBar(
    currentPage: Int,
    total: Int,
    onSeek: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val value = if (dragging) dragValue else currentPage.toFloat()
    val shownPage = value.roundToInt().coerceIn(0, total - 1)

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (dragging) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.75f)
            ) {
                Text(
                    text = "${shownPage + 1} / $total",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Surface(
            shape = PillShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "${shownPage + 1}", style = MaterialTheme.typography.labelSmall)

                val active = MaterialTheme.colorScheme.onSurface
                val inactive = active.copy(alpha = 0.3f)

                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 32.dp) {
                    Slider(
                        value = value,
                        onValueChange = {
                            dragging = true
                            dragValue = it
                            onSeek(it.roundToInt())
                        },
                        onValueChangeFinished = { dragging = false },
                        valueRange = 0f..(total - 1).toFloat(),
                        thumb = {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(22.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(active)
                            )
                        },
                        track = { sliderState ->
                            val range = sliderState.valueRange
                            val span = range.endInclusive - range.start
                            val fraction = if (span > 0f) {
                                ((sliderState.value - range.start) / span).coerceIn(0f, 1f)
                            } else 0f
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(inactive)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction)
                                        .fillMaxHeight()
                                        .background(active)
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    )
                }

                Text(text = "$total", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun PageImage(
    image: ImageItem,
    animate: Boolean,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
    onSuccess: ((AsyncImagePainter.State.Success) -> Unit)? = null
) {
    val context = LocalContext.current
    val model: Any = remember(image.uri, animate) {
        if (animate) image.uri else staticImageRequest(context, image.uri)
    }
    AsyncImage(
        model = model,
        contentDescription = image.name,
        contentScale = contentScale,
        onSuccess = onSuccess,
        modifier = modifier
    )
}

/** Vertical continuous reading column with pinch / double-tap zoom and flick momentum. */
@Composable
private fun VerticalPages(
    images: List<ImageItem>,
    aspects: Map<String, Float>,
    listState: LazyListState,
    contentPadding: PaddingValues,
    animate: Boolean,
    onToggleUi: () -> Unit,
    onAspectKnown: (String, Float) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val toggleUi by rememberUpdatedState(onToggleUi)
    val scope = rememberCoroutineScope()
    val flingJobs = remember { mutableListOf<Job>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { boxSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { toggleUi() },
                    onDoubleTap = { tap ->
                        flingJobs.forEach { it.cancel() }
                        flingJobs.clear()
                        val startScale = scale
                        val startOffset = offsetX
                        val targetScale = if (startScale > 1f) 1f else 2f
                        val qx = tap.x - boxSize.width / 2f
                        val qy = tap.y - boxSize.height / 2f
                        flingJobs += scope.launch {
                            var prevScale = startScale
                            animateValue(
                                0f, 1f,
                                animationSpec = tween(260, easing = FastOutSlowInEasing)
                            ) { t, _ ->
                                val s = startScale + (targetScale - startScale) * t
                                val maxX = (s - 1f) * boxSize.width / 2f
                                offsetX = (qx - (s / startScale) * (qx - startOffset)).coerceIn(-maxX, maxX)
                                // Keep the tapped spot under the finger while scaling
                                listState.dispatchRawDelta(qy / prevScale - qy / s)
                                prevScale = s
                                scale = s
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    flingJobs.forEach { it.cancel() }
                    flingJobs.clear()
                    val tracker = VelocityTracker()
                    var handled = false
                    var pastSlop = false
                    var slopPan = Offset.Zero
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pressedCount = event.changes.count { it.pressed }
                        if (pressedCount > 1 || scale > 1f) {
                            val zoom = event.calculateZoom()
                            var pan = event.calculatePan()
                            if (!pastSlop) {
                                if (pressedCount > 1) {
                                    pastSlop = true
                                } else {
                                    slopPan += pan
                                    if (slopPan.getDistance() > viewConfiguration.touchSlop) {
                                        pastSlop = true
                                        pan = slopPan
                                    } else {
                                        pan = Offset.Zero
                                    }
                                }
                            }
                            if (!pastSlop) continue
                            handled = true
                            val centroid = event.calculateCentroid(useCurrent = true)
                            val oldScale = scale
                            val newScale = (oldScale * zoom).coerceIn(1f, MAX_ZOOM)
                            var newOffset = offsetX
                            if (centroid.isSpecified && oldScale > 0f) {
                                val qx = centroid.x - boxSize.width / 2f
                                newOffset = qx - (newScale / oldScale) * (qx - newOffset)
                                if (newScale != oldScale) {
                                    val qy = centroid.y - boxSize.height / 2f
                                    listState.dispatchRawDelta(qy / oldScale - qy / newScale)
                                }
                            }
                            newOffset += pan.x
                            val maxOffset = (newScale - 1f) * boxSize.width / 2f
                            offsetX = newOffset.coerceIn(-maxOffset, maxOffset)
                            scale = newScale
                            if (newScale > 1f && pan.y != 0f) {
                                listState.dispatchRawDelta(-pan.y / newScale)
                            }
                            if (pressedCount == 1) {
                                event.changes.firstOrNull { it.pressed }?.let {
                                    tracker.addPosition(it.uptimeMillis, it.position)
                                }
                            } else {
                                tracker.resetTracking()
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })

                    if (handled && scale > 1f) {
                        val velocity = tracker.calculateVelocity()
                        if (abs(velocity.x) > 200f) {
                            flingJobs += scope.launch {
                                var last = 0f
                                AnimationState(initialValue = 0f, initialVelocity = velocity.x)
                                    .animateDecay(exponentialDecay()) {
                                        val d = value - last
                                        last = value
                                        val max = (scale - 1f) * boxSize.width / 2f
                                        offsetX = (offsetX + d).coerceIn(-max, max)
                                    }
                            }
                        }
                        if (abs(velocity.y) > 200f) {
                            flingJobs += scope.launch {
                                var last = 0f
                                AnimationState(initialValue = 0f, initialVelocity = velocity.y)
                                    .animateDecay(exponentialDecay()) {
                                        val d = value - last
                                        last = value
                                        listState.dispatchRawDelta(-d / scale)
                                    }
                            }
                        }
                    }
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
                val aspect = aspects[image.uri]
                PageImage(
                    image = image,
                    animate = animate,
                    contentScale = ContentScale.FillWidth,
                    onSuccess = { success ->
                        if (aspect == null) {
                            val size = success.painter.intrinsicSize
                            if (size.isSpecified && size.height > 0f) {
                                onAspectKnown(image.uri, size.width / size.height)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspect ?: DEFAULT_ASPECT)
                )
            }
        }
    }
}

/**
 * One page per swipe (left-to-right or right-to-left). When a page is zoomed in, dragging
 * moves the image first; once an edge is reached, dragging further turns the page.
 */
@Composable
private fun HorizontalPages(
    images: List<ImageItem>,
    pagerState: PagerState,
    rtl: Boolean,
    animate: Boolean,
    onToggleUi: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var zoomPage by remember { mutableIntStateOf(pagerState.currentPage) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val toggleUi by rememberUpdatedState(onToggleUi)
    val scope = rememberCoroutineScope()
    val flingJobs = remember { mutableListOf<Job>() }

    // After a page turn from a zoomed page: settle, then reset zoom for the new page
    val onGestureEnd: () -> Unit = {
        scope.launch {
            if (pagerState.currentPageOffsetFraction != 0f) {
                pagerState.animateScrollToPage(pagerState.currentPage)
            }
            if (pagerState.currentPage != zoomPage) {
                scale = 1f
                offsetX = 0f
                offsetY = 0f
                zoomPage = pagerState.currentPage
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { boxSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { toggleUi() },
                    onDoubleTap = { tap ->
                        flingJobs.forEach { it.cancel() }
                        flingJobs.clear()
                        val startScale = scale
                        val startX = offsetX
                        val startY = offsetY
                        val targetScale = if (startScale > 1f) 1f else 2f
                        if (targetScale > 1f) zoomPage = pagerState.currentPage
                        val qx = tap.x - boxSize.width / 2f
                        val qy = tap.y - boxSize.height / 2f
                        flingJobs += scope.launch {
                            animateValue(
                                0f, 1f,
                                animationSpec = tween(260, easing = FastOutSlowInEasing)
                            ) { t, _ ->
                                val s = startScale + (targetScale - startScale) * t
                                val maxX = (s - 1f) * boxSize.width / 2f
                                val maxY = (s - 1f) * boxSize.height / 2f
                                offsetX = (qx - (s / startScale) * (qx - startX)).coerceIn(-maxX, maxX)
                                offsetY = (qy - (s / startScale) * (qy - startY)).coerceIn(-maxY, maxY)
                                scale = s
                            }
                        }
                    }
                )
            }
            .pointerInput(rtl) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    flingJobs.forEach { it.cancel() }
                    flingJobs.clear()
                    val tracker = VelocityTracker()
                    var handled = false
                    var pastSlop = false
                    var slopPan = Offset.Zero
                    var turned = false
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pressedCount = event.changes.count { it.pressed }
                        if (pressedCount > 1 || scale > 1f) {
                            val zoom = event.calculateZoom()
                            var pan = event.calculatePan()
                            if (!pastSlop) {
                                if (pressedCount > 1) {
                                    pastSlop = true
                                } else {
                                    slopPan += pan
                                    if (slopPan.getDistance() > viewConfiguration.touchSlop) {
                                        pastSlop = true
                                        pan = slopPan
                                    } else {
                                        pan = Offset.Zero
                                    }
                                }
                            }
                            if (!pastSlop) continue
                            handled = true
                            val centroid = event.calculateCentroid(useCurrent = true)
                            val oldScale = scale
                            val newScale = (oldScale * zoom).coerceIn(1f, MAX_ZOOM)
                            if (oldScale <= 1f && newScale > 1f) {
                                zoomPage = pagerState.currentPage
                            }
                            var newX = offsetX
                            var newY = offsetY
                            if (centroid.isSpecified && oldScale > 0f) {
                                val qx = centroid.x - boxSize.width / 2f
                                val qy = centroid.y - boxSize.height / 2f
                                newX = qx - (newScale / oldScale) * (qx - newX)
                                newY = qy - (newScale / oldScale) * (qy - newY)
                            }
                            newX += pan.x
                            newY += pan.y
                            val maxX = (newScale - 1f) * boxSize.width / 2f
                            val maxY = (newScale - 1f) * boxSize.height / 2f
                            val clampedX = newX.coerceIn(-maxX, maxX)
                            val leftover = newX - clampedX
                            offsetX = clampedX
                            offsetY = newY.coerceIn(-maxY, maxY)
                            scale = newScale

                            // One finger pushing past the image edge turns the page
                            if (pressedCount == 1 && newScale > 1f && leftover != 0f) {
                                turned = true
                                pagerState.dispatchRawDelta(if (rtl) leftover else -leftover)
                            }
                            if (pressedCount == 1) {
                                event.changes.firstOrNull { it.pressed }?.let {
                                    tracker.addPosition(it.uptimeMillis, it.position)
                                }
                            } else {
                                tracker.resetTracking()
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })

                    if (handled) {
                        onGestureEnd()
                        if (!turned && scale > 1f) {
                            val velocity = tracker.calculateVelocity()
                            if (abs(velocity.x) > 200f) {
                                flingJobs += scope.launch {
                                    var last = 0f
                                    AnimationState(initialValue = 0f, initialVelocity = velocity.x)
                                        .animateDecay(exponentialDecay()) {
                                            val d = value - last
                                            last = value
                                            val max = (scale - 1f) * boxSize.width / 2f
                                            offsetX = (offsetX + d).coerceIn(-max, max)
                                        }
                                }
                            }
                            if (abs(velocity.y) > 200f) {
                                flingJobs += scope.launch {
                                    var last = 0f
                                    AnimationState(initialValue = 0f, initialVelocity = velocity.y)
                                        .animateDecay(exponentialDecay()) {
                                            val d = value - last
                                            last = value
                                            val max = (scale - 1f) * boxSize.height / 2f
                                            offsetY = (offsetY + d).coerceIn(-max, max)
                                        }
                                }
                            }
                        }
                    }
                }
            }
    ) {
        HorizontalPager(
            state = pagerState,
            reverseLayout = rtl,
            beyondViewportPageCount = 1,
            key = { index -> images.getOrNull(index)?.uri ?: index },
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val image = images.getOrNull(page)
            if (image != null) {
                val zoomed = page == zoomPage
                PageImage(
                    image = image,
                    animate = animate,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (zoomed) {
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetX
                                translationY = offsetY
                            }
                        }
                )
            }
        }
    }
}