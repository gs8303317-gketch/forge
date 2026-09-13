package com.gketch.forge.ui.settings

import android.content.Intent
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import android.os.Build
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gketch.forge.BuildConfig
import com.gketch.forge.R
import com.gketch.forge.data.AccentPreset
import com.gketch.forge.data.AppSettings
import com.gketch.forge.data.AudioFocusBehavior
import com.gketch.forge.data.AppLanguage
import com.gketch.forge.data.GestureSensitivity
import com.gketch.forge.data.MinClipLength
import com.gketch.forge.data.AppSettingsStore
import com.gketch.forge.data.ChromeHideDelay
import com.gketch.forge.data.CrossfadeDuration
import com.gketch.forge.data.LibraryStorageHint
import com.gketch.forge.data.MediaRepository
import com.gketch.forge.data.PinLockState
import com.gketch.forge.data.PinLockStore
import com.gketch.forge.data.BackupStore
import com.gketch.forge.data.HiddenFolder
import com.gketch.forge.data.HiddenFoldersStore
import com.gketch.forge.data.RecentStore
import com.gketch.forge.data.ResumeBehavior
import com.gketch.forge.data.SkipIntroSeconds
import com.gketch.forge.data.HoldToSpeed
import com.gketch.forge.data.ResumeStore
import com.gketch.forge.data.SafFolder
import com.gketch.forge.data.SafFoldersStore
import com.gketch.forge.data.SleepEndAction
import com.gketch.forge.playback.BufferPreset
import com.gketch.forge.playback.DecoderPreference
import com.gketch.forge.playback.EnginePrefs
import com.gketch.forge.playback.ForgeCrossfade
import com.gketch.forge.playback.ForgeEngine
import com.gketch.forge.playback.ForgeLoudness
import com.gketch.forge.playback.ForgePlayerPrefsStore
import com.gketch.forge.ui.theme.ForgeAccent
import com.gketch.forge.ui.theme.ForgeBlack
import com.gketch.forge.ui.theme.ForgeGraphite
import com.gketch.forge.ui.theme.ForgeMuted
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoilApi::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
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
    val pinStore = remember { PinLockStore(context) }
    var pinState by remember { mutableStateOf(PinLockState()) }
    var pinDialog by remember { mutableStateOf(false) }
    var pinDraft by remember { mutableStateOf("") }
    var pinConfirm by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var cacheMessage by remember { mutableStateOf<String?>(null) }
    var settingsQuery by remember { mutableStateOf("") }
    var storageHint by remember { mutableStateOf(LibraryStorageHint(0, 0, 0)) }
    val mediaRepo = remember { MediaRepository(context) }

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
    LaunchedEffect(Unit) {
        pinStore.state.collect { pinState = it }
    }
    LaunchedEffect(Unit) {
        storageHint = runCatching { mediaRepo.storageHint() }.getOrDefault(LibraryStorageHint(0, 0, 0))
    }
    LaunchedEffect(app.crossfade, app.loudnessNormalize, app.gaplessPlayback) {
        ForgeCrossfade.setDurationSec(app.crossfade.seconds)
        ForgeLoudness.setNormalizeEnabled(app.loudnessNormalize)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ForgeBlack)
            .padding(horizontal = 12.dp, vertical = 8.dp),
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
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = settingsQuery,
            onValueChange = { settingsQuery = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.settings_search_hint)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ForgeAccent,
                unfocusedBorderColor = ForgeMuted.copy(alpha = 0.4f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = ForgeAccent,
                focusedContainerColor = ForgeGraphite,
                unfocusedContainerColor = ForgeGraphite,
            ),
        )
        Spacer(Modifier.height(12.dp))
        val q = settingsQuery.trim().lowercase()
        fun matches(vararg keys: String): Boolean {
            if (q.isEmpty()) return true
            return keys.any { it.lowercase().contains(q) }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── Interface ──────────────────────────────────────────────
            if (matches(
                "interface", "language", "hindi", "english", "भाषा", "हिन्दी",
                "appearance", "accent", "theme", "dynamic", "color", "chrome", "hide",
            )) {
            SettingsCard(title = stringResource(R.string.settings_section_interface)) {
                Text(stringResource(R.string.language), color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.language_sub), color = ForgeMuted, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = app.appLanguage == lang,
                            onClick = { scope.launch { appStore.setAppLanguage(lang) } },
                            label = { Text(lang.label) },
                            colors = engineChipColors(),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
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
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.chrome_hide_delay), color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ChromeHideDelay.entries.forEach { opt ->
                        FilterChip(
                            selected = app.chromeHideDelay == opt,
                            onClick = { scope.launch { appStore.setChromeHideDelay(opt) } },
                            label = { Text(opt.label) },
                            colors = engineChipColors(),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.chrome_hide_delay_sub),
                    color = ForgeMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            }

            // ── Video ──────────────────────────────────────────────────
            if (matches("video", "decoder", "hardware", "software", "mediacodec", "engine")) {
            SettingsCard(title = stringResource(R.string.settings_section_video)) {
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
                    "Hardware preferred by default (Software fallback). Auto / Hardware / Software via MediaCodecSelector. ExtensionRendererMode stays OFF — no FFmpeg .so in CI.",
                    color = ForgeMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            }

            // ── Audio ──────────────────────────────────────────────────
            if (matches(
                "audio", "gapless", "crossfade", "loudness", "normalize",
                "silence", "headset", "focus", "duck", "pause",
            )) {
            SettingsCard(title = stringResource(R.string.settings_section_audio)) {
                EngineSwitchRow(
                    title = stringResource(R.string.gapless_playback),
                    subtitle = stringResource(R.string.gapless_playback_sub),
                    checked = app.gaplessPlayback,
                    onChecked = { scope.launch { appStore.setGaplessPlayback(it) } },
                )
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.crossfade), color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CrossfadeDuration.entries.forEach { opt ->
                        FilterChip(
                            selected = app.crossfade == opt,
                            onClick = {
                                scope.launch {
                                    appStore.setCrossfade(opt)
                                    ForgeCrossfade.setDurationSec(opt.seconds)
                                }
                            },
                            label = { Text(opt.label) },
                            colors = engineChipColors(),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.crossfade_sub), color = ForgeMuted, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                EngineSwitchRow(
                    title = stringResource(R.string.loudness_normalize),
                    subtitle = stringResource(R.string.loudness_normalize_sub),
                    checked = app.loudnessNormalize,
                    onChecked = {
                        scope.launch {
                            appStore.setLoudnessNormalize(it)
                            ForgeLoudness.setNormalizeEnabled(it)
                        }
                    },
                )
                Spacer(Modifier.height(8.dp))
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
                Spacer(Modifier.height(8.dp))
                EngineSwitchRow(
                    title = stringResource(R.string.pause_on_headset),
                    subtitle = stringResource(R.string.pause_on_headset_sub),
                    checked = app.pauseOnHeadsetUnplug,
                    onChecked = { scope.launch { appStore.setPauseOnHeadsetUnplug(it) } },
                )
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.audio_focus), color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AudioFocusBehavior.entries.forEach { opt ->
                        FilterChip(
                            selected = app.audioFocusBehavior == opt,
                            onClick = { scope.launch { appStore.setAudioFocusBehavior(opt) } },
                            label = { Text(opt.label) },
                            colors = engineChipColors(),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.audio_focus_sub),
                    color = ForgeMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            }

            // ── Playback ───────────────────────────────────────────────
            if (matches(
                "playback", "seek", "autoplay", "series", "intro", "resume",
                "speed", "precise", "sleep", "fade", "timer",
            )) {
            SettingsCard(title = stringResource(R.string.settings_section_playback)) {
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
                Spacer(Modifier.height(14.dp))
                EngineSwitchRow(
                    title = stringResource(R.string.series_auto_next),
                    subtitle = stringResource(R.string.series_auto_next_sub),
                    checked = app.seriesAutoNext,
                    onChecked = { scope.launch { appStore.setSeriesAutoNext(it) } },
                )
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.skip_intro), color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SkipIntroSeconds.entries.forEach { opt ->
                        FilterChip(
                            selected = app.skipIntroSeconds == opt,
                            onClick = { scope.launch { appStore.setSkipIntroSeconds(opt) } },
                            label = { Text(opt.label) },
                            colors = engineChipColors(),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.skip_intro_sub),
                    color = ForgeMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.resume_playback_title), color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ResumeBehavior.entries.forEach { mode ->
                        FilterChip(
                            selected = app.resumeBehavior == mode,
                            onClick = { scope.launch { appStore.setResumeBehavior(mode) } },
                            label = { Text(mode.label) },
                            colors = engineChipColors(),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "When reopening a video with a saved position (also next / previous / recent)",
                    color = ForgeMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(14.dp))
                Text("Default playback speed", color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppSettingsStore.SPEED_OPTIONS.forEach { spd ->
                        FilterChip(
                            selected = app.defaultPlaybackSpeed == spd,
                            onClick = { scope.launch { appStore.setDefaultPlaybackSpeed(spd) } },
                            label = {
                                Text(
                                    if (spd == spd.toInt().toFloat()) "${spd.toInt()}×" else "${spd}×",
                                )
                            },
                            colors = engineChipColors(),
                        )
                    }
                }
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
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.sleep_timer), color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(8.dp))
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
            }

            // ── Gestures ───────────────────────────────────────────────
            if (matches(
                "gesture", "hold", "brightness", "volume", "swipe", "lock",
                "sensitivity", "invert", "pinch",
            )) {
            SettingsCard(title = stringResource(R.string.settings_section_gestures)) {
                Text("Gesture sensitivity", color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GestureSensitivity.entries.forEach { sens ->
                        FilterChip(
                            selected = app.gestureSensitivity == sens,
                            onClick = { scope.launch { appStore.setGestureSensitivity(sens) } },
                            label = { Text(sens.label) },
                            colors = engineChipColors(),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Brightness, volume, and seek swipe strength",
                    color = ForgeMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.hold_to_speed), color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HoldToSpeed.entries.forEach { opt ->
                        FilterChip(
                            selected = app.holdToSpeed == opt,
                            onClick = { scope.launch { appStore.setHoldToSpeed(opt) } },
                            label = { Text(opt.label) },
                            colors = engineChipColors(),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.hold_to_speed_sub),
                    color = ForgeMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(14.dp))
                EngineSwitchRow(
                    title = stringResource(R.string.player_gestures),
                    subtitle = stringResource(R.string.player_gestures_sub),
                    checked = app.playerGesturesEnabled,
                    onChecked = { scope.launch { appStore.setPlayerGesturesEnabled(it) } },
                )
                Spacer(Modifier.height(14.dp))
                EngineSwitchRow(
                    title = stringResource(R.string.invert_gesture_sides),
                    subtitle = stringResource(R.string.invert_gesture_sides_sub),
                    checked = app.invertGestureSides,
                    onChecked = { scope.launch { appStore.setInvertGestureSides(it) } },
                )
                Spacer(Modifier.height(14.dp))
                EngineSwitchRow(
                    title = stringResource(R.string.swipe_down_close),
                    subtitle = stringResource(R.string.swipe_down_close_sub),
                    checked = app.swipeDownToClose,
                    onChecked = { scope.launch { appStore.setSwipeDownToClose(it) } },
                )
                Spacer(Modifier.height(14.dp))
                EngineSwitchRow(
                    title = stringResource(R.string.double_tap_lock),
                    subtitle = stringResource(R.string.double_tap_lock_sub),
                    checked = app.doubleTapToLock,
                    onChecked = { scope.launch { appStore.setDoubleTapToLock(it) } },
                )
            }
            }

            // ── Library ────────────────────────────────────────────────
            if (matches(
                "library", "clip", "hidden", "folder", "storage", "saf",
                "cache", "thumb", "history", "recent", "clear", "exclude",
            )) {
            SettingsCard(title = stringResource(R.string.settings_section_library)) {
                Text(stringResource(R.string.exclude_short_clips), color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MinClipLength.entries.forEach { opt ->
                        FilterChip(
                            selected = app.minClipLength == opt,
                            onClick = { scope.launch { appStore.setMinClipLength(opt) } },
                            label = { Text(opt.label) },
                            colors = engineChipColors(),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.exclude_short_clips_sub),
                    color = ForgeMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(16.dp))
                Text("Hidden folders", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
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
                Spacer(Modifier.height(16.dp))
                Text("Storage folders", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
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
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.cache_storage), color = Color.White, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                val vids = storageHint.videoCount
                val auds = storageHint.audioCount
                val bytes = storageHint.totalBytes
                val sizeLabel = when {
                    bytes >= 1_000_000_000L -> "%.1f GB".format(bytes / 1_000_000_000.0)
                    bytes >= 1_000_000L -> "%.0f MB".format(bytes / 1_000_000.0)
                    bytes >= 1_000L -> "%.0f KB".format(bytes / 1_000.0)
                    else -> "$bytes B"
                }
                Text(
                    stringResource(R.string.library_counts) + ": $vids videos · $auds audio · ~$sizeLabel",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = {
                    scope.launch {
                        runCatching {
                            context.imageLoader.memoryCache?.clear()
                            context.imageLoader.diskCache?.clear()
                        }
                        cacheMessage = context.getString(R.string.clear_image_cache_done)
                    }
                }) {
                    Text(stringResource(R.string.clear_image_cache), color = ForgeAccent)
                }
                cacheMessage?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = ForgeMuted, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.history), color = Color.White, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
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
            }

            // ── Network ────────────────────────────────────────────────
            if (matches("network", "stream", "user-agent", "timeout", "buffer", "hls", "dash")) {
            SettingsCard(title = stringResource(R.string.settings_section_network)) {
                Text(stringResource(R.string.stream_user_agent), color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(6.dp))
                var uaDraft by remember(app.streamUserAgent) { mutableStateOf(app.streamUserAgent) }
                OutlinedTextField(
                    value = uaDraft,
                    onValueChange = { uaDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.stream_user_agent_hint)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ForgeAccent,
                        unfocusedBorderColor = ForgeMuted.copy(alpha = 0.4f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = ForgeAccent,
                        focusedContainerColor = ForgeBlack,
                        unfocusedContainerColor = ForgeBlack,
                    ),
                )
                TextButton(
                    onClick = { scope.launch { appStore.setStreamUserAgent(uaDraft) } },
                ) { Text(stringResource(R.string.action_save), color = ForgeAccent) }
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.stream_timeout), color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppSettingsStore.TIMEOUT_OPTIONS.forEach { sec ->
                        FilterChip(
                            selected = app.streamTimeoutSec == sec,
                            onClick = { scope.launch { appStore.setStreamTimeoutSec(sec) } },
                            label = { Text("${sec}s") },
                            colors = engineChipColors(),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.stream_timeout_sub),
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
            }
            }

            // ── Privacy & Lock ─────────────────────────────────────────
            if (matches("privacy", "pin", "lock", "biometric", "backup", "restore", "export", "import")) {
            SettingsCard(title = stringResource(R.string.settings_section_privacy)) {
                Text(stringResource(R.string.pin_lock), color = Color.White, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.pin_lock_sub), color = ForgeMuted, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(10.dp))
                if (pinState.enabled) {
                    TextButton(onClick = {
                        pinDraft = ""
                        pinConfirm = ""
                        pinError = null
                        pinDialog = true
                    }) { Text(stringResource(R.string.pin_change), color = ForgeAccent) }
                    TextButton(onClick = { scope.launch { pinStore.disable() } }) {
                        Text(stringResource(R.string.pin_disable), color = Color.White)
                    }
                    EngineSwitchRow(
                        title = stringResource(R.string.pin_biometric),
                        subtitle = stringResource(R.string.pin_biometric_sub),
                        checked = pinState.biometricEnabled,
                        onChecked = { scope.launch { pinStore.setBiometricEnabled(it) } },
                    )
                } else {
                    TextButton(onClick = {
                        pinDraft = ""
                        pinConfirm = ""
                        pinError = null
                        pinDialog = true
                    }) { Text(stringResource(R.string.pin_set), color = ForgeAccent) }
                }
                Spacer(Modifier.height(16.dp))
                Text("Backup & restore", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
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
            }

            // ── About ──────────────────────────────────────────────────
            if (matches("about", "version", "forge")) {
            SettingsCard(title = stringResource(R.string.settings_section_about)) {
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
                    text = stringResource(R.string.version_label),
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
                    text = "1.27.0 · stability / polish · VLC-like settings sections",
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
    }

    if (pinDialog) {
        AlertDialog(
            onDismissRequest = { pinDialog = false },
            containerColor = ForgeGraphite,
            title = { Text(stringResource(R.string.pin_set), color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = pinDraft,
                        onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) pinDraft = it },
                        label = { Text(stringResource(R.string.pin_label)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ForgeAccent,
                            unfocusedBorderColor = ForgeMuted.copy(alpha = 0.4f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = ForgeAccent,
                        ),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinConfirm,
                        onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) pinConfirm = it },
                        label = { Text(stringResource(R.string.pin_confirm)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ForgeAccent,
                            unfocusedBorderColor = ForgeMuted.copy(alpha = 0.4f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = ForgeAccent,
                        ),
                    )
                    pinError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        when {
                            pinDraft.length !in 4..8 -> pinError = "4–8 digits"
                            pinDraft != pinConfirm -> pinError = context.getString(R.string.pin_mismatch)
                            else -> {
                                pinStore.setPin(pinDraft)
                                pinDialog = false
                            }
                        }
                    }
                }) { Text(stringResource(R.string.action_save), color = ForgeAccent) }
            },
            dismissButton = {
                TextButton(onClick = { pinDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = ForgeMuted)
                }
            },
        )
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
            .clip(RoundedCornerShape(8.dp))
            .background(ForgeGraphite)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = ForgeAccent,
        )
        Spacer(Modifier.height(8.dp))
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
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = ForgeMuted.copy(alpha = 0.18f), thickness = 0.5.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
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
}

@Composable
private fun engineChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = ForgeAccent,
    selectedLabelColor = Color.Black,
    containerColor = ForgeBlack,
    labelColor = Color.White,
)
