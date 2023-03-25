package com.paniclabs.rch

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.remoteconfig.ktx.get
import com.google.firebase.remoteconfig.ktx.remoteConfig
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

val rch: Rch get() = Rch(Firebase.remoteConfig)

class Rch(private val frc: FirebaseRemoteConfig) {
    init {
        // config
        val fs = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(getMinimumFetchIntervalInSeconds()) // default is 12 hours... for testing/debug set to 0L
            .setFetchTimeoutInSeconds(getFetchTimeoutInSeconds()).build()

        frc.setConfigSettingsAsync(fs)

        // set defaults
        frc.setDefaultsAsync(R.xml.rcd)
    }

    fun getMinimumFetchIntervalInSeconds(): Long = TimeUnit.HOURS.toSeconds(12)

    fun getFetchTimeoutInSeconds(): Long = 60L

    fun fetch(now: Boolean = false): Task<Void> {
        return if (now) {
            // Starts fetching configs, adhering to the specified (0L) minimum fetch interval (fetch NOW)
            // LIMIT: 5 calls per hour
            frc.fetch(0L)
        } else {
            // Starts fetching configs, adhering to the default minimum fetch interval.
            frc.fetch()
        }
    }

    fun activate(): Task<Boolean> {
        return frc.activate()
    }

    fun fetchAndActivateAsync(
        now: Boolean = false, onFailureBlock: () -> Unit = {}, onSuccessBlock: () -> Unit = {}
    ) {
        val fetchTask = if (now || BuildConfig.DEBUG) {
            // Starts fetching configs, adhering to the specified (0L) minimum fetch interval (fetch NOW)
            frc.fetch(0L)
        } else {
            // Starts fetching configs, adhering to the default minimum fetch interval.
            frc.fetch()
        }

        fetchTask.addOnCompleteListener { task ->
            when {
                task.isSuccessful -> {
                    frc.activate()
                    onSuccessBlock()
                }
                else -> {
                    onFailureBlock()
                }
            }
        }
    }

    fun fetchAndActivateNow(timeoutSeconds: Long = DEFAULT_TIMEOUT_FETCH_SECONDS_SHORT): Boolean {
        // Starts fetching configs, adhering to the specified (0L) minimum fetch interval (fetch NOW)
        val fetchTask = frc.fetch(0L)

        // Await fetch, then activate right away if fetch was successful
        try {
            Tasks.await(fetchTask, timeoutSeconds, TimeUnit.SECONDS)
            if (fetchTask.isSuccessful) {
                frc.activate()
                return true
            }
        } catch (e: TimeoutException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    fun getLastFetchStatus(): String {
        return when (frc.info.lastFetchStatus) {
            FirebaseRemoteConfig.LAST_FETCH_STATUS_SUCCESS -> "Success"
            FirebaseRemoteConfig.LAST_FETCH_STATUS_FAILURE -> "Failure"
            FirebaseRemoteConfig.LAST_FETCH_STATUS_NO_FETCH_YET -> "No Fetch Yet"
            FirebaseRemoteConfig.LAST_FETCH_STATUS_THROTTLED -> "Throttled"
            else -> "Unknown"
        }
    }

    fun getLong(key: String) = frc[key].asLong()
    private fun getBoolean(key: String) = frc[key].asBoolean()
    fun getString(key: String) = frc[key].asString()
    fun getDouble(key: String) = frc[key].asDouble()

    fun isEnabled(): Boolean {
        return getBoolean(CAN)
    }

    companion object {
        const val CAN = "c_a_n"
        const val TAG = "RCH_LOG"
        const val DEFAULT_TIMEOUT_FETCH_SECONDS_SHORT: Long = 10
        const val DEFAULT_TIMEOUT_FETCH_SECONDS_LONG: Long = 60
    }
}