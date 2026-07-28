package ads.popbrain.sdk

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

object PopbrainAnalyticsManager {

    private const val S2S_URL = "https://server.popbrain.ai/api/v1/pixel/s2s"
    private const val MAX_PENDING_EVENTS = 100

    private val JSON = "application/json; charset=utf-8".toMediaType()

    /** Reserved payload keys that caller-supplied params must never overwrite. */
    private val RESERVED_KEYS = setOf("clickId", "advertiserId", "eventName")

    private val pending = ArrayDeque<Pair<String, Map<String, Any>?>>()

    /**
     * Explicit initialisation, kept for integrators who already know their ids. The SDK also
     * resolves both from the install referrer automatically, so calling this is optional.
     */
    @JvmStatic
    @JvmOverloads
    fun initAnalytics(clickId: String, advertiserId: String? = null) {
        // A blank clickId would still satisfy the null check in sendEvent, so every later
        // event would ship with an empty attribution key. Reject it here instead.
        val id = clickId.trim()
        if (id.isEmpty()) {
            PopbrainLogger.e("clickId is required, ignoring initAnalytics call")
            return
        }

        PopbrainStorage.clickId = id
        advertiserId?.trim()?.takeIf(String::isNotEmpty)?.let { PopbrainStorage.advertiserId = it }
        flushPending()
    }

    /** Called once the referrer has been parsed, to release any events queued before that. */
    internal fun onAttributionResolved() {
        flushPending()
    }

    @JvmStatic
    @JvmOverloads
    fun sendEvent(eventName: String, extraParams: Map<String, Any>? = null) {
        // eventName is mandatory. Dropped here rather than queued/sent, so a blank name can
        // never reach the server and create an unattributable event row.
        val name = eventName.trim()
        if (name.isEmpty()) {
            PopbrainLogger.e("Event name is required, dropping event")
            return
        }

        val clickId = PopbrainStorage.clickId?.takeIf(String::isNotEmpty)
        if (clickId == null) {
            // The referrer lookup is asynchronous, so an event fired at app start can arrive
            // before the clickId exists. Queue it rather than dropping it on the floor.
            queue(name, extraParams)
            return
        }
        dispatch(name, extraParams, clickId, PopbrainStorage.advertiserId)
    }

    private fun queue(eventName: String, extraParams: Map<String, Any>?) {
        synchronized(pending) {
            if (pending.size >= MAX_PENDING_EVENTS) {
                pending.removeFirst()
                PopbrainLogger.e("Pending event queue full, dropping oldest event")
            }
            pending.addLast(eventName to extraParams)
        }
        PopbrainLogger.d("Queued event '$eventName' until attribution resolves")
    }

    private fun flushPending() {
        val clickId = PopbrainStorage.clickId?.takeIf(String::isNotEmpty) ?: return
        val advertiserId = PopbrainStorage.advertiserId

        val drained = synchronized(pending) {
            if (pending.isEmpty()) return
            val copy = pending.toList()
            pending.clear()
            copy
        }

        PopbrainLogger.d("Flushing ${drained.size} queued event(s)")
        drained.forEach { (name, params) -> dispatch(name, params, clickId, advertiserId) }
    }

    private fun dispatch(
        eventName: String,
        extraParams: Map<String, Any>?,
        clickId: String,
        advertiserId: String?
    ) {
        val json = JSONObject().apply {
            // Caller params go in first, then the reserved identity fields overwrite them, so a
            // custom "clickId" key can never change which click an event is attributed to.
            extraParams?.forEach { (key, value) ->
                if (key in RESERVED_KEYS) {
                    PopbrainLogger.e("Ignoring reserved param '$key' in event '$eventName'")
                } else {
                    put(key, value)
                }
            }
            put("clickId", clickId)
            advertiserId?.let { put("advertiserId", it) }
            put("eventName", eventName)
        }

        val request = Request.Builder()
            .url(S2S_URL)
            .post(json.toString().toRequestBody(JSON))
            .build()

        PopbrainHttp.client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                PopbrainLogger.e("S2S event '$eventName' failed", e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        PopbrainLogger.d("Event '$eventName' sent. Code: ${it.code}")
                    } else {
                        PopbrainLogger.e("Event '$eventName' rejected with ${it.code}")
                    }
                }
            }
        })
    }
}
