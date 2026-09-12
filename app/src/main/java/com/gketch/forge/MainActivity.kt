package com.gketch.forge

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.gketch.forge.ui.navigation.ForgeNav
import com.gketch.forge.ui.theme.ForgeTheme

class MainActivity : ComponentActivity() {

    private var externalUri by mutableStateOf<Uri?>(null)
    private var externalMime by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeIntent(intent)
        setContent {
            ForgeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ForgeNav(
                        externalUri = externalUri,
                        externalMime = externalMime,
                        onExternalConsumed = {
                            externalUri = null
                            externalMime = null
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIntent(intent)
    }

    private fun consumeIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            externalUri = intent.data
            externalMime = intent.type
        }
    }
}
