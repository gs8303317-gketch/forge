package com.gketch.forge.cast

import android.content.Context
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

/**
 * Optional Cast bootstrap. Never throws into the app process.
 * Missing / broken Play Services must never crash playback.
 */
object ForgeCast {
    private const val TAG = "ForgeCast"

    @Volatile
    var available: Boolean = false
        private set

    fun init(context: Context) {
        available = false
        val app = context.applicationContext
        try {
            val status = GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(app)
            if (status != ConnectionResult.SUCCESS) {
                Log.i(TAG, "Cast unavailable: Play Services status=$status")
                return
            }
            val clazz = Class.forName("com.google.android.gms.cast.framework.CastContext")
            val sync = clazz.getMethod("getSharedInstance", Context::class.java)
            sync.invoke(null, app)
            available = true
            Log.i(TAG, "Cast ready")
        } catch (t: Throwable) {
            available = false
            val root = (t as? java.lang.reflect.InvocationTargetException)?.targetException ?: t
            Log.i(TAG, "Cast unavailable: ${root.javaClass.simpleName}: ${root.message}")
        }
    }

    /** Hide Cast UI for the rest of this process after a runtime failure. */
    fun markUnavailable(reason: String) {
        if (available) {
            Log.w(TAG, "Cast disabled at runtime: $reason")
        }
        available = false
    }
}
