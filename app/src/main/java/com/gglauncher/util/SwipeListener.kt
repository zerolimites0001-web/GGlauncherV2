package com.gglauncher.ui.home

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View

abstract class SwipeListener(ctx: Context) : View.OnTouchListener {
    private val det = GestureDetector(ctx, object : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            if (e1 == null) return false
            val dy = e2.y - e1.y
            if (Math.abs(dy) > 120 && Math.abs(vy) > 200) {
                if (dy < 0) onSwipeUp() else onSwipeDown()
                return true
            }
            return false
        }
    })
    abstract fun onSwipeUp()
    abstract fun onSwipeDown()
    override fun onTouch(v: View, e: MotionEvent): Boolean { det.onTouchEvent(e); return false }
}
