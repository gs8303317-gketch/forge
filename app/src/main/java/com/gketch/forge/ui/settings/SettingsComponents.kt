package com.gketch.forge.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gketch.forge.ui.theme.ForgeAccent
import com.gketch.forge.ui.theme.ForgeBlack
import com.gketch.forge.ui.theme.ForgeGraphite
import com.gketch.forge.ui.theme.ForgeMuted

internal data class SettingsBucket(val id: String, val label: String)

internal val SETTINGS_BUCKETS = listOf(
    SettingsBucket("all", "All"),
    SettingsBucket("look", "Look"),
    SettingsBucket("player", "Player"),
    SettingsBucket("audio", "Audio"),
    SettingsBucket("library", "Library"),
    SettingsBucket("network", "Network"),
    SettingsBucket("privacy", "Privacy"),
    SettingsBucket("about", "About"),
)

@Composable
internal fun SettingsBucketRow(
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SETTINGS_BUCKETS.forEach { bucket ->
            FilterChip(
                selected = selected == bucket.id,
                onClick = { onSelect(bucket.id) },
                label = { Text(bucket.label) },
                colors = engineChipColors(),
            )
        }
    }
}

@Composable
internal fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ForgeGraphite)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = ForgeAccent,
        )
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = ForgeMuted.copy(alpha = 0.22f), thickness = 0.5.dp)
        Spacer(Modifier.height(10.dp))
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
