package com.khayrrr.mangaviewer.ui.settings

import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.filled.ChevronRight
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khayrrr.mangaviewer.BuildConfig
import com.khayrrr.mangaviewer.data.preferences.AppSettings
import com.khayrrr.mangaviewer.data.preferences.PageIndicatorFormat
import com.khayrrr.mangaviewer.data.preferences.ReaderBackground
import com.khayrrr.mangaviewer.data.preferences.ReadingDirection
import com.khayrrr.mangaviewer.data.preferences.ThemeMode
import com.khayrrr.mangaviewer.ui.theme.SeedAmber
import com.khayrrr.mangaviewer.ui.theme.SeedForest
import com.khayrrr.mangaviewer.ui.theme.SeedRose
import com.khayrrr.mangaviewer.ui.theme.SeedSapphire
import com.khayrrr.mangaviewer.ui.theme.SeedTeal
import com.khayrrr.mangaviewer.ui.theme.SeedViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    var showFolderDialog by remember { mutableStateOf(false) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(it, flags)
            } catch (_: Exception) {}
            viewModel.addRootFolder(it.toString())
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Appearance Section
            SettingsSectionHeader(
                icon = Icons.Default.Palette,
                title = "Appearance"
            )

            SettingsCard {
                // Theme Mode
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                SegmentedButtons(
                    options = listOf("System", "Light", "Dark"),
                    selectedIndex = when (settings.themeMode) {
                        ThemeMode.SYSTEM -> 0
                        ThemeMode.LIGHT -> 1
                        ThemeMode.DARK -> 2
                    },
                    onSelect = { index ->
                        val mode = when (index) {
                            0 -> ThemeMode.SYSTEM
                            1 -> ThemeMode.LIGHT
                            else -> ThemeMode.DARK
                        }
                        viewModel.setThemeMode(mode)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Dynamic Seed Color Toggle
                SettingsSwitchRow(
                    title = "Dynamic Seed Color",
                    subtitle = "Follow system Monet palette (Android 12+)",
                    checked = settings.useDynamicColor,
                    onCheckedChange = { viewModel.setUseDynamicColor(it) }
                )

                // Custom Seed Color Picker (shown if dynamic color is disabled)
                AnimatedVisibility(visible = !settings.useDynamicColor) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Text(
                            text = "Custom Palette Seed Color",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            val seedColors = listOf(
                                SeedTeal to 0xFF006B5FL,
                                SeedSapphire to 0xFF005AC1L,
                                SeedViolet to 0xFF6750A4L,
                                SeedRose to 0xFFB3261EL,
                                SeedAmber to 0xFF8A5100L,
                                SeedForest to 0xFF2E6C38L
                            )
                            seedColors.forEach { (color, value) ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (settings.customSeedColor == value) 3.dp else 1.dp,
                                            color = if (settings.customSeedColor == value) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.setCustomSeedColor(value) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (settings.customSeedColor == value) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reader Background Canvas
                Text(
                    text = "Reader Background Canvas",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val backgrounds = listOf(
                        ReaderBackground.BLACK to ("OLED Black" to Color(0xFF000000)),
                        ReaderBackground.DARK to ("Dark" to Color(0xFF1E2321)),
                        ReaderBackground.SEPIA to ("Sepia" to Color(0xFFF5EBD7)),
                        ReaderBackground.WHITE to ("White" to Color(0xFFFFFFFF))
                    )
                    backgrounds.forEach { (bg, labelAndColor) ->
                        val isSelected = settings.readerBackground == bg
                        val textColor = if (bg == ReaderBackground.WHITE || bg == ReaderBackground.SEPIA) Color.Black else Color.White
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = labelAndColor.second,
                            border = borderStrokeFor(isSelected),
                            modifier = Modifier.clickable { viewModel.setReaderBackground(bg) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray)
                                )
                                Text(
                                    text = labelAndColor.first,
                                    color = textColor,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Reader & Controls Section
            SettingsSectionHeader(
                icon = Icons.Default.MenuBook,
                title = "Reader & Controls"
            )

            SettingsCard {
                // Reading Direction
                Text(
                    text = "Reading Direction",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                SegmentedButtons(
                    options = listOf("Vertical", "LTR", "RTL"),
                    selectedIndex = when (settings.readingDirection) {
                        ReadingDirection.VERTICAL -> 0
                        ReadingDirection.LTR -> 1
                        ReadingDirection.RTL -> 2
                    },
                    onSelect = { index ->
                        val dir = when (index) {
                            0 -> ReadingDirection.VERTICAL
                            1 -> ReadingDirection.LTR
                            else -> ReadingDirection.RTL
                        }
                        viewModel.setReadingDirection(dir)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Page Indicator Format
                Text(
                    text = "Page Indicator Format",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                SegmentedButtons(
                    options = listOf("Count (12/48)", "Percentage (%)"),
                    selectedIndex = when (settings.pageIndicatorFormat) {
                        PageIndicatorFormat.COUNT -> 0
                        PageIndicatorFormat.PERCENT -> 1
                    },
                    onSelect = { index ->
                        val fmt = if (index == 0) PageIndicatorFormat.COUNT else PageIndicatorFormat.PERCENT
                        viewModel.setPageIndicatorFormat(fmt)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Hide System Bars
                SettingsSwitchRow(
                    title = "Hide System Bars",
                    subtitle = "Full screen immersive mode while reading",
                    checked = settings.hideSystemBars,
                    onCheckedChange = { viewModel.setHideSystemBars(it) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Landscape Double-Page
                SettingsSwitchRow(
                    title = "Landscape Double-Page",
                    subtitle = "Side-by-side spread in landscape orientation",
                    checked = false,
                    onCheckedChange = {},
                    enabled = false,
                    note = "Not implemented"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Keep Screen On
                SettingsSwitchRow(
                    title = "Keep Screen On",
                    subtitle = "Prevent device sleep while reading",
                    checked = settings.keepScreenOn,
                    onCheckedChange = { viewModel.setKeepScreenOn(it) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Animate GIF/AVIF
                SettingsSwitchRow(
                    title = "Animate GIF & AVIF",
                    subtitle = "Play animated images when device decoders support it",
                    checked = settings.animateGifAvif,
                    onCheckedChange = { viewModel.setAnimateGifAvif(it) }
                )
            }

            // Library & Security Section
            SettingsSectionHeader(
                icon = Icons.Default.FolderOpen,
                title = "Library & Security"
            )

            SettingsCard {
                // Biometric Lock
                SettingsSwitchRow(
                    title = "Biometric Lock",
                    subtitle = "Require biometric authentication to open hidden albums",
                    checked = settings.biometricLock,
                    onCheckedChange = { viewModel.setBiometricLock(it) },
                    icon = Icons.Default.Fingerprint
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Manage Root Folders
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showFolderDialog = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Manage Local Folders",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${settings.rootFolderUris.size} root folders configured",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Storage & About Section
            SettingsSectionHeader(
                icon = Icons.Default.Info,
                title = "Storage & About"
            )

            SettingsCard {
                // Clear Thumbnail Cache
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            // Clear image cache
                            runCatching {
                                context.cacheDir.deleteRecursively()
                            }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Clear Cached Thumbnails",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Frees up temporary cache memory",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // About
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onNavigateToAbout() }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "About",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "MangaViewer ${BuildConfig.VERSION_NAME} • Fully Offline & Open Source",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text("Configured Root Folders") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (settings.rootFolderUris.isEmpty()) {
                        Text(
                            text = "No root folders configured yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        settings.rootFolderUris.forEach { folderUri ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Uri.decode(folderUri).substringAfterLast(':').ifEmpty { folderUri },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.removeRootFolder(folderUri) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            folderPickerLauncher.launch(null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add New Folder")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFolderDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun SettingsCard(
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    note: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (note != null) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun SegmentedButtons(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(index) }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun borderStrokeFor(isSelected: Boolean): androidx.compose.foundation.BorderStroke? {
    return if (isSelected) {
        androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}
