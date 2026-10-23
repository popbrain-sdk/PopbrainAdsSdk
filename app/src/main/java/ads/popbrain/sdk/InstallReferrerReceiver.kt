package ads.popbrain.sdk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class InstallReferrerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.android.vending.INSTALL_REFERRER") return

        try {
            PopbrainStorage.init(context)

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
