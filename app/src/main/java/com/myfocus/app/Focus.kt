package com.myfocus.app

import android.content.Context
import android.provider.Settings

object Focus {
    private fun p(c: Context) = c.getSharedPreferences("focus", Context.MODE_PRIVATE)

    fun endTime(c: Context) = p(c).getLong("end", 0L)

    fun isActive(c: Context): Boolean {
        val on = p(c).getBoolean("active", false)
        if (on && System.currentTimeMillis() >= endTime(c)) { stop(c); return false }
        return on
    }

    fun start(c: Context, minutes: Int): Boolean {
        p(c).edit().putBoolean("active", true)
            .putLong("end", System.currentTimeMillis() + minutes * 60_000L).apply()
        return gray(c, true)
    }

    fun stop(c: Context) {
        p(c).edit().putBoolean("active", false).apply()
        gray(c, false)
    }

    fun gray(c: Context, on: Boolean): Boolean = try {
        val cr = c.contentResolver
        Settings.Secure.putInt(cr, "accessibility_display_daltonizer_enabled", if (on) 1 else 0)
        Settings.Secure.putInt(cr, "accessibility_display_daltonizer", 0)
        true
    } catch (e: Exception) { false }
}
