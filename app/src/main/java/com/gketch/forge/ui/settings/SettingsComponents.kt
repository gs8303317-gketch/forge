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
import com.gketch.forge.player.BufferPreset
import com.gketch.forge.player.DecoderPreference
import com.gketch.forge.player.EnginePrefs
import com.gketch.forge.player.ForgeCrossfade
import com.gketch.forge.player.ForgeEngine
import com.gketch.forge.player.ForgeLoudness
import com.gketch.forge.player.ForgePlayerPrefsStore
import com.gketch.forge.ui.theme.ForgeAccent
import com.gketch.forge.ui.theme.ForgeBlack
import com.gketch.forge.ui.theme.ForgeGraphite
import com.gketch.forge.ui.theme.ForgeMuted
import kotlinx.coroutines.launch

// Shared settings row components

@Composable
internal fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(0.dp))
            .background(ForgeGraphite)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = ForgeAccent,
        )
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
internal fun EngineSwitchRow(
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
internal fun engineChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = ForgeAccent,
    selectedLabelColor = Color.Black,
    containerColor = ForgeBlack,
    labelColor = Color.White,
)
