package com.graviton94.notificationcleaner.platform

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.graviton94.notificationcleaner.BuildConfig

/**
 * Live tuning knobs from Firebase Remote Config (balance v1.6 §5-1, docs/balance/remote-tuning.md).
 * One string parameter, [KEY], holds a flat JSON object of knobs; the core clamps and ignores bad values.
 *
 * Values never change mid-session: [launchJson] is what the previous run activated, applied once at
 * launch; [refresh] fetches and activates in the background for the next launch.
 */
object Tuning {
    const val KEY = "tuning_json"
    private var rc: FirebaseRemoteConfig? = null

    fun init(context: Context) {
        if (!BuildConfig.FIREBASE || FirebaseApp.getApps(context).isEmpty()) return
        rc = runCatching {
            FirebaseRemoteConfig.getInstance().apply {
                setConfigSettingsAsync(remoteConfigSettings { minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 60 else 3600 })
                setDefaultsAsync(mapOf(KEY to "{}"))
            }
        }.getOrNull()
    }

    /** The JSON activated by an earlier run ("{}" when none, or without Firebase). */
    fun launchJson(): String = rc?.getString(KEY)?.takeIf { it.isNotBlank() } ?: "{}"

    fun refresh() { rc?.fetchAndActivate() }
}
