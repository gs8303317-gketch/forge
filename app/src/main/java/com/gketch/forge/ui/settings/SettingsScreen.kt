package com.gketch.forge.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
    var prefs by remember { mutableStateOf(EnginePrefs()) }

    LaunchedEffect(Unit) {
        store.prefs.collect { prefs = it }
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
                    text = "Audio delay · decoder prefs · buffers · precise seek · skip silence · M3U · stream quality · media info · bookmarks · EQ · A-B · PiP",
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
