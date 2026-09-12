package com.gketch.forge.cast

import android.content.Context
import android.util.Log

/**
 * Optional Cast bootstrap. Full Media3 CastPlayer handoff is gated so free CI
 * remains reliable if Play Services Cast is unavailable at runtime.
 */
object ForgeCast {
    private const val TAG = "ForgeCast"
    @Volatile
    var available: Boolean = false
        private set

    fun init(context: Context) {
        available = try {
            val clazz = Class.forName("com.google.android.gms.cast.framework.CastContext")
            val method = clazz.getMethod("getSharedInstance", Context::class.java)
            method.invoke(null, context.applicationContext)
            true
        } catch (t: Throwable) {
            Log.i(TAG, "Cast unavailable: ${t.javaClass.simpleName}")
            false
        }
    }
}
