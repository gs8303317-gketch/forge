package com.gketch.forge.ui.settings

import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gketch.forge.BuildConfig
import com.gketch.forge.data.AccentPreset
import com.gketch.forge.data.AppSettings
import com.gketch.forge.data.AppSettingsStore
import com.gketch.forge.data.BackupStore
import com.gketch.forge.data.HiddenFolder
import com.gketch.forge.data.HiddenFoldersStore
import com.gketch.forge.data.RecentStore
import com.gketch.forge.data.ResumeStore
import com.gketch.forge.data.SafFolder
import com.gketch.forge.data.SafFoldersStore
import com.gketch.forge.data.SleepEndAction
import com.gketch.forge.playback.BufferPreset
import com.gketch.forge.playback.DecoderPreference
import com.gketch.forge.playback.EnginePrefs
import com.gketch.forge.playback.ForgeEngine
import com.gketch.forge.playback.ForgePlayerPrefsStore
import com.gketch.forge.ui.theme.ForgeAccent
import com.gketch.forge.ui.theme.ForgeBlack
import com.gketch.forge.ui.theme.ForgeGraphite
import com.gketch.forge.ui.theme.ForgeMuted
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { ForgePlayerPrefsStore(context) }
    val appStore = remember { AppSettingsStore(context) }
    val recentStore = remember { RecentStore(context) }
    val resumeStore = remember { ResumeStore(context) }
    val hiddenStore = remember { HiddenFoldersStore(context) }
    val safStore = remember { SafFoldersStore(context) }
    val backupStore = remember { BackupStore(context) }
    var prefs by remember { mutableStateOf(EnginePrefs()) }
    var app by remember { mutableStateOf(AppSettings()) }
    var hidden by remember { mutableStateOf<List<HiddenFolder>>(emptyList()) }
    var safFolders by remember { mutableStateOf<List<SafFolder>>(emptyList()) }
    var clearDialog by remember { mutableStateOf(false) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var pendingExportShare by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = backupStore.writeExport(uri)
            backupMessage = result.fold({ "Backup saved" }, { it.message ?: "Export failed" })
            if (pendingExportShare && result.isSuccess) {
                pendingExportShare = false
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                runCatching { context.startActivity(Intent.createChooser(send, "Share Forge backup")) }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            backupMessage = backupStore.importFrom(uri).fold({ it }, { it.message ?: "Import failed" })
        }
    }
    val safLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (_: SecurityException) {
        }
        val name = uri.lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/') ?: "Folder"
        scope.launch { safStore.add(uri, name) }
    }

    LaunchedEffect(Unit) {
        store.prefs.collect { prefs = it }
    }
    LaunchedEffect(Unit) {
        appStore.settings.collect { app = it }
    }
    LaunchedEffect(Unit) {
        hiddenStore.hidden.collect { hidden = it }
    }
    LaunchedEffect(Unit) {
        safStore.folders.collect { safFolders = it }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ForgeBlack)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsCard(title = "Playback") {
                Text("Double-tap seek", color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppSettingsStore.SEEK_OPTIONS.forEach { sec ->
                        FilterChip(
                            selected = app.seekSeconds == sec,
                            onClick = { scope.launch { appStore.setSeekSeconds(sec) } },
                            label = { Text("±${sec}s") },
                            colors = engineChipColors(),
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                EngineSwitchRow(
                    title = "Autoplay next",
                    subtitle = "Continue to the next item in queue or folder",
                    checked = app.autoplayNext,
                    onChecked = { scope.launch { appStore.setAutoplayNext(it) } },
                )
            }

            SettingsCard(title = "Playback engine") {
                Text("Decoder", color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DecoderPreference.entries.forEach { mode ->
                        FilterChip(
                            selected = prefs.decoder == mode,
                            onClick = {
                                scope.launch { store.setDecoder(mode) }
                            },
                            label = { Text(mode.label) },
                            colors = engineChipColors(),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Auto / Hardware / Software via MediaCodecSelector. ExtensionRendererMode stays OFF — FFmpeg decoder extension is not bundled (keeps CI free of NDK .so).",
                    color = ForgeMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(14.dp))
                Text("Network buffers", color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BufferPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = prefs.buffer == preset,
                            onClick = { scope.launch { store.setBuffer(preset) } },
                            label = { Text(preset.label) },
                            colors = engineChipColors(),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Higher buffers reduce rebuffering on HLS/DASH. Decoder & buffer apply the next time playback starts.",
                    color = ForgeMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(14.dp))
                EngineSwitchRow(
                    title = "Precise seeking",
                    subtitle = "Prefer SeekParameters.EXACT where the container supports it",
                    checked = prefs.preciseSeek,
                    onChecked = {
                        scope.launch {
                            store.setPreciseSeek(it)
                            ForgeEngine.setPreciseSeek(it)
                        }
                    },
                )
                EngineSwitchRow(
                    title = "Skip silence",
                    subtitle = "Media3 skipSilenceEnabled (audio gaps)",
                    checked = prefs.skipSilence,
                    onChecked = {
                        scope.launch {
                            store.setSkipSilence(it)
                            ForgeEngine.setSkipSilence(it)
                        }
                    },
                )
            }

            SettingsCard(title = "Appearance") {
                Text("Accent", color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AccentPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = app.accentPreset == preset && !app.dynamicColor,
                            onClick = {
                                scope.launch {
                                    appStore.setDynamicColor(false)
                                    appStore.setAccentPreset(preset)
                                }
                            },
                            label = { Text(preset.label) },
                            colors = engineChipColors(),
                        )
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Spacer(Modifier.height(10.dp))
                    EngineSwitchRow(
                        title = "Material You",
                        subtitle = "Dynamic accent from wallpaper (Android 12+)",
                        checked = app.dynamicColor,
                        onChecked = { scope.launch { appStore.setDynamicColor(it) } },
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text("Dark theme is always on. Accents tint controls and chips.", color = ForgeMuted, style = MaterialTheme.typography.bodySmall)
            }

            SettingsCard(title = "Sleep timer") {
                EngineSwitchRow(
                    title = "Fade out",
                    subtitle = "Lower volume in the last seconds",
                    checked = app.sleepFadeEnabled,
                    onChecked = { scope.launch { appStore.setSleepFadeEnabled(it) } },
                )
                if (app.sleepFadeEnabled) {
                    Spacer(Modifier.height(6.dp))
                    Text("Fade length", color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AppSettingsStore.FADE_OPTIONS.forEach { sec ->
                            FilterChip(
                                selected = app.sleepFadeSeconds == sec,
                                onClick = { scope.launch { appStore.setSleepFadeSeconds(sec) } },
                                label = { Text("${sec}s") },
                                colors = engineChipColors(),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("When timer ends", color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SleepEndAction.entries.forEach { action ->
                        FilterChip(
                            selected = app.sleepEndAction == action,
                            onClick = { scope.launch { appStore.setSleepEndAction(action) } },
                            label = { Text(action.label) },
                            colors = engineChipColors(),
                        )
                    }
                }
            }

            SettingsCard(title = "Hidden folders") {
                Text("Blacklisted folders stay out of the library. Long-press a folder to hide it.", color = ForgeMuted, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                if (hidden.isEmpty()) {
                    Text("None hidden", color = ForgeMuted)
                } else {
                    hidden.forEach { folder ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(folder.name, color = Color.White, modifier = Modifier.weight(1f))
                            TextButton(onClick = { scope.launch { hiddenStore.unhide(folder.bucketId) } }) {
                                Text("Unhide", color = ForgeAccent)
                            }
                        }
                    }
                }
            }

            SettingsCard(title = "Storage folders") {
                Text("Add a folder via the system picker. Forge scans and plays those files even if MediaStore misses them.", color = ForgeMuted, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { safLauncher.launch(null) }) {
                    Text("Add folder…", color = ForgeAccent)
                }
                if (safFolders.isEmpty()) {
                    Text("No extra folders", color = ForgeMuted)
                } else {
                    safFolders.forEach { folder ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(folder.name, color = Color.White)
                                Text(folder.uri, color = ForgeMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                            TextButton(onClick = { scope.launch { safStore.remove(folder.uri) } }) {
                                Text("Remove", color = ForgeAccent)
                            }
                        }
                    }
                }
            }

            SettingsCard(title = "Backup & restore") {
                Text("Export settings, playlists, favorites, streams, and bookmarks as JSON. Share or save the file, then import on another device.", color = ForgeMuted, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        pendingExportShare = false
                        exportLauncher.launch("forge-backup.json")
                    }) { Text("Export", color = ForgeAccent) }
                    TextButton(onClick = {
                        pendingExportShare = true
                        exportLauncher.launch("forge-backup.json")
                    }) { Text("Share", color = ForgeAccent) }
                    TextButton(onClick = {
                        importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    }) { Text("Import", color = Color.White) }
                }
                backupMessage?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = ForgeMuted, style = MaterialTheme.typography.bodySmall)
                }
            }

            SettingsCard(title = "History") {
                Text(
                    "Clear recently played items. Optionally also wipe saved resume positions.",
                    color = ForgeMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = { clearDialog = true }) {
                    Text("Clear history…", color = ForgeAccent)
                }
            }

            SettingsCard(title = "About") {
                Text(
                    text = "Forge",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Premium local + network media player",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ForgeMuted,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Version",
                    style = MaterialTheme.typography.labelSmall,
                    color = ForgeMuted,
                )
                Text(
                    text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Multi-select · hide folders · SAF folders · themes · backup · sleep fade · mini player · chapters · bass / virtualizer",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ForgeMuted,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Kotlin · Jetpack Compose · Media3",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ForgeMuted,
                )
            }
        }
    }

    if (clearDialog) {
        AlertDialog(
            onDismissRequest = { clearDialog = false },
            containerColor = ForgeGraphite,
            title = { Text("Clear history", color = Color.White) },
            text = {
                Text(
                    "Remove recently played entries from the library home section.",
                    color = ForgeMuted,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            recentStore.clear()
                            clearDialog = false
                        }
                    },
                ) { Text("Clear recent", color = ForgeAccent) }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            scope.launch {
                                recentStore.clear()
                                resumeStore.clearAll()
                                clearDialog = false
                            }
                        },
                    ) { Text("Recent + resume", color = Color.White) }
                    TextButton(onClick = { clearDialog = false }) {
                        Text("Cancel", color = ForgeMuted)
                    }
                }
            },
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ForgeGraphite)
            .padding(20.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = ForgeAccent)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun EngineSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, color = ForgeMuted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = ForgeAccent,
                uncheckedThumbColor = ForgeMuted,
                uncheckedTrackColor = ForgeBlack,
            ),
        )
    }
}

@Composable
private fun engineChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = ForgeAccent,
    selectedLabelColor = Color.Black,
    containerColor = ForgeBlack,
    labelColor = Color.White,
)
