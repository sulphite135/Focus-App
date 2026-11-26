package com.myfocus.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class FocusService : AccessibilityService() {

    private val allowed = setOf(
        "com.android.systemui",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.samsung.android.dialer",
        "com.google.android.inputmethod.latin",
        "com.samsung.android.honeyboard"
    )

    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        if (e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (!Focus.isActive(this)) return
        val pkg = e.packageName?.toString() ?: return
        if (pkg == packageName || pkg in allowed) return

        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        })
    }

    override fun onInterrupt() {}
}
