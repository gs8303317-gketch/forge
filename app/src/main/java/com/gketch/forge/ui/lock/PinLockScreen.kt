package com.gketch.forge.ui.lock

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gketch.forge.R
import com.gketch.forge.data.PinLockStore
import com.gketch.forge.ui.theme.ForgeAccent
import com.gketch.forge.ui.theme.ForgeBlack
import com.gketch.forge.ui.theme.ForgeMuted
import kotlinx.coroutines.launch

private const val BIOMETRIC_AUTHENTICATORS =
    BiometricManager.Authenticators.BIOMETRIC_WEAK or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

private fun biometricAvailable(context: Context): Boolean {
    return try {
        val mgr = BiometricManager.from(context.applicationContext)
        mgr.canAuthenticate(BIOMETRIC_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
    } catch (_: Throwable) {
        false
    }
}

@Composable
fun PinLockGate(
    store: PinLockStore,
    biometricEnabled: Boolean,
    onUnlocked: () -> Unit,
    title: String = stringResource(R.string.pin_unlock_title),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var activityResumed by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { source, _ ->
            activityResumed = source.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        activityResumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val canBiometric = remember(biometricEnabled) {
        biometricEnabled && biometricAvailable(context)
    }

    fun tryBiometric() {
        if (!activityResumed) return
        val activity = context.findFragmentActivity() ?: return
        if (activity.isFinishing || activity.isDestroyed) return
        try {
            val executor = ContextCompat.getMainExecutor(activity)
            val prompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onUnlocked()
                    }
                },
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(context.getString(R.string.pin_unlock_title))
                .setSubtitle(context.getString(R.string.pin_biometric_sub))
                .setAllowedAuthenticators(BIOMETRIC_AUTHENTICATORS)
                .build()
            prompt.authenticate(info)
        } catch (_: Throwable) {
        }
    }

    LaunchedEffect(canBiometric, activityResumed) {
        if (canBiometric && activityResumed) tryBiometric()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ForgeBlack)
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.pin_unlock_sub), color = ForgeMuted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = {
                if (it.length <= 8 && it.all { c -> c.isDigit() }) {
                    pin = it
                    error = null
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.pin_label)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ForgeAccent,
                unfocusedBorderColor = ForgeMuted.copy(alpha = 0.4f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = ForgeAccent,
                focusedLabelColor = ForgeAccent,
                unfocusedLabelColor = ForgeMuted,
            ),
        )
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                scope.launch {
                    if (store.verifyPin(pin)) onUnlocked()
                    else {
                        error = context.getString(R.string.pin_wrong)
                        pin = ""
                    }
                }
            },
            enabled = pin.length >= 4,
            colors = ButtonDefaults.buttonColors(containerColor = ForgeAccent, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.pin_unlock))
        }
        if (canBiometric) {
            TextButton(onClick = { tryBiometric() }) {
                Text(stringResource(R.string.pin_use_biometric), color = ForgeAccent)
            }
        }
    }
}
