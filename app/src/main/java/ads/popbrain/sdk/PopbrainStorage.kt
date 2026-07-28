package ads.popbrain.sdk

import android.content.Context
import android.content.SharedPreferences

/**
 * Durable state for the SDK.
 *
 * Two independent flags drive the install pipeline:
 *  - [referrerProcessed] — the Play referrer has been read and parsed once. Stops the SDK from
 *    re-binding to the Play Store service on every launch.
 *  - [installReported] — the install reached Popbrain successfully. Only set on a 2xx response,
 *    so an install that happens offline is retried on the next launch instead of being lost.
 */
internal object PopbrainStorage {

    private const val PREFS_NAME = "popbrain_sdk_prefs"

    private const val KEY_CLICK_ID = "click_id"
    private const val KEY_ADVERTISER_ID = "advertiser_id"
    private const val KEY_REFERRER = "referrer"
    private const val KEY_REFERRER_PROCESSED = "referrer_processed"
    private const val KEY_INSTALL_REPORTED = "install_reported"

    @Volatile
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            synchronized(this) {
                if (prefs == null) {
                    prefs = context.applicationContext
                        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                }
            }
        }
    }

    var clickId: String?
        get() = prefs?.getString(KEY_CLICK_ID, null)
        set(value) = put(KEY_CLICK_ID, value)

    var advertiserId: String?
        get() = prefs?.getString(KEY_ADVERTISER_ID, null)
        set(value) = put(KEY_ADVERTISER_ID, value)

    var referrer: String?
        get() = prefs?.getString(KEY_REFERRER, null)
        set(value) = put(KEY_REFERRER, value)

    var referrerProcessed: Boolean
        get() = prefs?.getBoolean(KEY_REFERRER_PROCESSED, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(KEY_REFERRER_PROCESSED, value)?.apply()
        }

    var installReported: Boolean
        get() = prefs?.getBoolean(KEY_INSTALL_REPORTED, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(KEY_INSTALL_REPORTED, value)?.apply()
        }

    private fun put(key: String, value: String?) {
        prefs?.edit()?.putString(key, value)?.apply()
    }
}
