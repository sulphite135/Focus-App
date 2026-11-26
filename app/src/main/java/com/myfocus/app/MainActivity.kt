package com.myfocus.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.widget.*

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var minutes: EditText
    private lateinit var video: VideoView
    private lateinit var list: LinearLayout
    private val h = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() { refresh(); h.postDelayed(this, 1000) }
    }

    private fun prefs() = getSharedPreferences("videos", MODE_PRIVATE)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun btn(t: String, onClick: () -> Unit) = Button(this).apply {
        text = t; setOnClickListener { onClick() }
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(32), dp(16), dp(16))
        }
        status = TextView(this).apply { textSize = 20f }
        minutes = EditText(this).apply {
            hint = "Focus minutes (jaise 60)"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        video = VideoView(this)
        video.setMediaController(MediaController(this).also { it.setAnchorView(video) })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        root.addView(status)
        root.addView(minutes)
        root.addView(btn("1. Accessibility ON karo (ek baar)") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        root.addView(btn("2. Videos chuno") {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "video/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or
                         Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }, 1)
        })
        root.addView(btn("3. FOCUS START") { startFocus() })
        root.addView(btn("Emergency exit") { emergency() })
        root.addView(video, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(220)))
        root.addView(list)

        setContentView(ScrollView(this).apply { addView(root) })
        renderList()
    }

    private fun startFocus() {
        val m = minutes.text.toString().toIntOrNull()
        if (m == null || m <= 0) { toast("Minutes daalo"); return }
        val gray = Focus.start(this, m)
        if (!gray) toast("Black&white ke liye WRITE_SECURE_SETTINGS permission do (adb se)")
        refresh()
    }

    private fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_LONG).show()

    private fun emergency() {
        AlertDialog.Builder(this)
            .setMessage("Pakka focus mode band karna hai?")
            .setPositiveButton("Haan") { _, _ -> Focus.stop(this); refresh() }
            .setNegativeButton("Nahi", null).show()
    }

    private fun refresh() {
        if (Focus.isActive(this)) {
            val left = (Focus.endTime(this) - System.currentTimeMillis()) / 1000
            status.text = "FOCUS ON: %02d:%02d:%02d baaki".format(left / 3600, left % 3600 / 60, left % 60)
        } else status.text = "Focus band hai"
    }

    private fun renderList() {
        list.removeAllViews()
        prefs().getStringSet("v", emptySet())!!.forEachIndexed { i, u ->
            list.addView(btn("Video ${i + 1}") {
                video.setVideoURI(Uri.parse(u)); video.start()
            })
        }
    }

    override fun onActivityResult(req: Int, res: Int, d: Intent?) {
        if (req != 1 || res != RESULT_OK || d == null) return
        val set = prefs().getStringSet("v", emptySet())!!.toMutableSet()
        val uris = mutableListOf<Uri>()
        d.data?.let { uris.add(it) }
        d.clipData?.let { for (i in 0 until it.itemCount) uris.add(it.getItemAt(i).uri) }
        uris.forEach {
            try { contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            catch (e: Exception) {}
            set.add(it.toString())
        }
        prefs().edit().putStringSet("v", set).apply()
        renderList()
    }

    override fun onResume() { super.onResume(); h.post(tick) }
    override fun onPause() { super.onPause(); h.removeCallbacks(tick) }
}
