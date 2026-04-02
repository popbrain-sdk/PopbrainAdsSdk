package ads.popbrain.sdk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class InstallReferrerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if ("com.android.vending.INSTALL_REFERRER" == intent.action) {
            val referrer = intent.getStringExtra("referrer")
            val result = ReferrerParser.parse(referrer)
            ConversionTracker.track(result)
        }
    }
}