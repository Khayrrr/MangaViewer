package com.khayrrr.mangaviewer.ui.hidden

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import coil3.compose.AsyncImage
import com.khayrrr.mangaviewer.data.model.Album
import com.khayrrr.mangaviewer.util.BiometricHelper
import com.khayrrr.mangaviewer.util.HiddenSession
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
    val albums by viewModel.albums.collectAsState()
    var lockState by remember { mutableStateOf(LockState.CHECKING) }

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

    Scaffold(
        topBar = {
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
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
                    if (albums.isEmpty()) {
                        Text(
                            text = "No hidden albums",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(albums, key = { it.uri }) { album ->
                                HiddenAlbumRow(
                                    album = album,
                                    onOpen = { onOpenReader(album.uri) },
                                    onUnhide = { viewModel.unhide(album.uri) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HiddenAlbumRow(
    album: Album,
    onOpen: () -> Unit,
    onUnhide: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = album.coverUri,
                contentDescription = album.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${album.totalImages} pages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(onClick = onUnhide) {
                Text("Unhide")
            }
        }
    }
}