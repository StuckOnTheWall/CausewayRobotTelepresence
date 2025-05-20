package com.three.robot.telepresence

import android.app.Activity
import android.os.Handler
import android.os.Looper

object TimeoutManager {
    private var handler: Handler? = null
    private var timeoutRunnable: Runnable? = null

    /** Initialize the timeout: pass in the Activity to finish, and the duration in ms. */
    fun initialize(activity: Activity, timeoutMillis: Long) {
        handler = Handler(Looper.getMainLooper())
        timeoutRunnable = Runnable {
            activity.finish()
        }
        handler?.postDelayed(timeoutRunnable!!, timeoutMillis)
    }

    /** Reset the timeout back to full duration. */
    fun resetTimer(timeoutMillis: Long) {
        handler?.removeCallbacks(timeoutRunnable!!)
        handler?.postDelayed(timeoutRunnable!!, timeoutMillis)
    }

    /** Call this when the screen is left or destroyed to prevent leaks. */
    fun cancel() {
        handler?.removeCallbacks(timeoutRunnable!!)
        handler = null
        timeoutRunnable = null
    }
}
