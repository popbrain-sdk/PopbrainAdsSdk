package ads.popbrain.sdk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Legacy `INSTALL_REFERRER` broadcast path.
 *
 * Google Play stopped sending this in March 2020 — it is retained only for alternative stores
 * that still broadcast it. The manifest restricts it to senders holding INSTALL_PACKAGES so
 * an arbitrary app cannot forge a paid install.
 */
class InstallReferrerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.android.vending.INSTALL_REFERRER") return

        try {
            PopbrainStorage.init(context)

            // The Install Referrer Library path may already have handled this install; without
            // this guard the two paths would report the same conversion twice.
            if (PopbrainStorage.referrerProcessed) {
                PopbrainLogger.d("Referrer already processed, ignoring broadcast")
                return
            }

            ConversionTracker.track(ReferrerParser.parse(intent.getStringExtra("referrer")))
        } catch (e: Exception) {
            PopbrainLogger.e("Failed to handle install referrer broadcast", e)
        }
    }
}
