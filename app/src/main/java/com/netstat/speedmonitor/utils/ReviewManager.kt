package com.netstat.speedmonitor.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.testing.FakeReviewManager
import com.netstat.speedmonitor.BuildConfig

/**
 * Handles in-app review prompts using Google Play In-App Review API.
 * 
 * Trigger conditions:
 * - User has launched the app at least [MIN_LAUNCHES] times
 * - At least [MIN_DAYS_BETWEEN_PROMPTS] days since last prompt
 * - Service has been started at least [MIN_SERVICE_STARTS] times
 * 
 * Note: Google throttles review prompts, so even if conditions are met,
 * the dialog may not appear. This is by design to prevent spamming users.
 */
object ReviewManager {
    private const val TAG = "ReviewManager"
    private const val PREFS_NAME = "review_prefs"
    
    private const val KEY_LAUNCH_COUNT = "launch_count"
    private const val KEY_SERVICE_START_COUNT = "service_start_count"
    private const val KEY_LAST_PROMPT_TIME = "last_prompt_time"
    private const val KEY_FIRST_LAUNCH_TIME = "first_launch_time"
    
    // Trigger thresholds
    private const val MIN_LAUNCHES = 5
    private const val MIN_SERVICE_STARTS = 3
    private const val MIN_DAYS_BETWEEN_PROMPTS = 30
    private const val MIN_DAYS_SINCE_INSTALL = 3
    
    private fun getPrefs(context: Context) = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Call this in onCreate of MainActivity
     */
    fun trackLaunch(context: Context) {
        val prefs = getPrefs(context)
        val count = prefs.getInt(KEY_LAUNCH_COUNT, 0) + 1
        prefs.edit().apply {
            putInt(KEY_LAUNCH_COUNT, count)
            if (!prefs.contains(KEY_FIRST_LAUNCH_TIME)) {
                putLong(KEY_FIRST_LAUNCH_TIME, System.currentTimeMillis())
            }
            apply()
        }
        Log.d(TAG, "Launch count: $count")
    }
    
    /**
     * Call this when the monitoring service is started
     */
    fun trackServiceStart(context: Context) {
        val prefs = getPrefs(context)
        val count = prefs.getInt(KEY_SERVICE_START_COUNT, 0) + 1
        prefs.edit().putInt(KEY_SERVICE_START_COUNT, count).apply()
        Log.d(TAG, "Service start count: $count")
    }
    
    /**
     * Check if conditions are met to show a review prompt
     */
    fun shouldPromptForReview(context: Context): Boolean {
        val prefs = getPrefs(context)
        
        val launchCount = prefs.getInt(KEY_LAUNCH_COUNT, 0)
        val serviceStartCount = prefs.getInt(KEY_SERVICE_START_COUNT, 0)
        val lastPromptTime = prefs.getLong(KEY_LAST_PROMPT_TIME, 0)
        val firstLaunchTime = prefs.getLong(KEY_FIRST_LAUNCH_TIME, System.currentTimeMillis())
        
        val daysSinceLastPrompt = (System.currentTimeMillis() - lastPromptTime) / (1000 * 60 * 60 * 24)
        val daysSinceInstall = (System.currentTimeMillis() - firstLaunchTime) / (1000 * 60 * 60 * 24)
        
        val shouldPrompt = launchCount >= MIN_LAUNCHES &&
                serviceStartCount >= MIN_SERVICE_STARTS &&
                daysSinceInstall >= MIN_DAYS_SINCE_INSTALL &&
                (lastPromptTime == 0L || daysSinceLastPrompt >= MIN_DAYS_BETWEEN_PROMPTS)
        
        Log.d(TAG, "Review check: launches=$launchCount/$MIN_LAUNCHES, " +
                "starts=$serviceStartCount/$MIN_SERVICE_STARTS, " +
                "daysSinceInstall=$daysSinceInstall/$MIN_DAYS_SINCE_INSTALL, " +
                "daysSincePrompt=$daysSinceLastPrompt/$MIN_DAYS_BETWEEN_PROMPTS, " +
                "shouldPrompt=$shouldPrompt")
        
        return shouldPrompt
    }
    
    /**
     * Request the in-app review flow.
     * Call this after a positive user action (e.g., after starting monitoring successfully).
     */
    fun requestReview(activity: Activity, onComplete: (() -> Unit)? = null) {
        if (!shouldPromptForReview(activity)) {
            Log.d(TAG, "Conditions not met for review prompt")
            onComplete?.invoke()
            return
        }
        
        // Use FakeReviewManager for debug builds to test the flow
        val manager = if (BuildConfig.DEBUG) {
            Log.d(TAG, "Using FakeReviewManager for debug build")
            FakeReviewManager(activity)
        } else {
            ReviewManagerFactory.create(activity)
        }
        
        Log.d(TAG, "Requesting review flow...")
        
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                Log.d(TAG, "Got ReviewInfo, launching flow...")
                
                manager.launchReviewFlow(activity, reviewInfo).addOnCompleteListener {
                    // Flow finished - user may have reviewed or dismissed
                    // We don't know which, by design (anti-gaming)
                    Log.d(TAG, "Review flow completed")
                    markPromptShown(activity)
                    onComplete?.invoke()
                }
            } else {
                Log.e(TAG, "Failed to get ReviewInfo", task.exception)
                onComplete?.invoke()
            }
        }
    }
    
    private fun markPromptShown(context: Context) {
        getPrefs(context).edit()
            .putLong(KEY_LAST_PROMPT_TIME, System.currentTimeMillis())
            .apply()
    }
    
    /**
     * Reset all tracking data (for testing purposes)
     */
    fun resetTracking(context: Context) {
        getPrefs(context).edit().clear().apply()
        Log.d(TAG, "Review tracking reset")
    }
    
    /**
     * Force conditions to be met (for testing purposes)
     * Only works in debug builds
     */
    fun forceConditionsMet(context: Context) {
        if (!BuildConfig.DEBUG) return
        
        getPrefs(context).edit().apply {
            putInt(KEY_LAUNCH_COUNT, MIN_LAUNCHES + 1)
            putInt(KEY_SERVICE_START_COUNT, MIN_SERVICE_STARTS + 1)
            putLong(KEY_FIRST_LAUNCH_TIME, System.currentTimeMillis() - (MIN_DAYS_SINCE_INSTALL + 1) * 24 * 60 * 60 * 1000L)
            putLong(KEY_LAST_PROMPT_TIME, 0)
            apply()
        }
        Log.d(TAG, "Forced conditions to be met (debug only)")
    }
}
