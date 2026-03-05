package com.aria.assistente.system

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import com.aria.assistente.R
import kotlin.math.max
import kotlin.math.min

class OverlayBubble(private val ctx: Context) {

    private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: View? = null
    private var bubble: TextView? = null

    private fun params(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 120
        }
    }

    fun showListening() {
        ensure()
        bubble?.text = "ARIA"
        bubble?.alpha = 0.90f
        animateUp()
    }

    fun showActivated() {
        ensure()
        bubble?.text = "OK"
        bubble?.alpha = 1.0f
        animateUp()
    }

    fun showIdle() {
        ensure()
        bubble?.alpha = 0.65f
    }

    fun pulse(rms: Float) {
        val v = bubble ?: return
        val scale = 1.0f + min(0.35f, max(0f, (rms - 2f) / 25f))
        v.scaleX = scale
        v.scaleY = scale
        v.alpha = 0.75f + min(0.25f, max(0f, rms / 30f))
    }

    fun hide() {
        val v = view ?: return
        try { wm.removeView(v) } catch (_: Exception) {}
        view = null
        bubble = null
    }

    private fun ensure() {
        if (view != null) return
        val v = LayoutInflater.from(ctx).inflate(R.layout.overlay_bubble, null, false)
        bubble = v.findViewById(R.id.bubble)
        bubble?.alpha = 0.85f
        view = v
        wm.addView(v, params())
        v.translationY = 250f
        animateUp()
    }

    private fun animateUp() {
        val v = view ?: return
        v.animate()
            .translationY(0f)
            .setDuration(350)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }
}
