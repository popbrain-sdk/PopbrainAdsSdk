package ads.popbrain.sdk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class InstallReferrerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if ("com.android.vending.INSTALL_REFERRER" == intent.action) {
            val referrer = intent.getStringExtra("referrer")
            Log.d("PopbrainSDK", "Broadcast Receiver $referrer")
            val result = ReferrerParser.parse(referrer)
            ConversionTracker.track(result)
        }
    }
}