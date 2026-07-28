package ads.popbrain.sdk

import org.json.JSONObject
import java.util.concurrent.TimeUnit

object PopbrainAnalyticsManager {

    private const val MAX_PENDING_EVENTS = 100
    private const val S2S_PATH = "api/v1/pixel/s2s"

    /** Retries for a transient event failure, with backoff. Permanent failures never retry. */
    private const val MAX_EVENT_RETRIES = 2
    private val RETRY_DELAYS_SECONDS = longArrayOf(2, 8)

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
        dispatch(name, extraParams, clickId, PopbrainStorage.advertiserId, attempt = 0)
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
        drained.forEach { (name, params) -> dispatch(name, params, clickId, advertiserId, 0) }
    }

    private fun dispatch(
        eventName: String,
        extraParams: Map<String, Any>?,
        clickId: String,
        advertiserId: String?,
        attempt: Int
    ) {
        val body = try {
            buildBody(eventName, extraParams, clickId, advertiserId)
        } catch (e: Exception) {
            PopbrainLogger.e("Could not build event '$eventName'", e)
            return
        }

        PopbrainApiClient.postJson(S2S_PATH, body, "Event '$eventName'") { result ->
            if (result is ApiResult.Retryable && attempt < MAX_EVENT_RETRIES) {
                val delay = RETRY_DELAYS_SECONDS[attempt]
                PopbrainLogger.d("Retrying event '$eventName' in ${delay}s")
                PopbrainApiClient.schedule(delay, TimeUnit.SECONDS) {
                    dispatch(eventName, extraParams, clickId, advertiserId, attempt + 1)
                }
            } else if (result is ApiResult.Retryable) {
                PopbrainLogger.e("Event '$eventName' dropped after ${attempt + 1} attempts")
            }
        }
    }

    private fun buildBody(
        eventName: String,
        extraParams: Map<String, Any>?,
        clickId: String,
        advertiserId: String?
    ): String {
        val json = JSONObject()

        // Caller params go in first, then the reserved identity fields overwrite them, so a
        // custom "clickId" key can never change which click an event is attributed to.
        extraParams?.forEach { (key, value) ->
            when {
                key in RESERVED_KEYS ->
                    PopbrainLogger.e("Ignoring reserved param '$key' in event '$eventName'")
                key.isBlank() ->
                    PopbrainLogger.e("Ignoring blank param key in event '$eventName'")
                // org.json rejects NaN/Infinity — skip the one param, keep the event.
                else -> runCatching { json.put(key, value) }.onFailure {
                    PopbrainLogger.e("Ignoring unserialisable param '$key' in event '$eventName'")
                }
            }
        }

        json.put("clickId", clickId)
        advertiserId?.let { json.put("advertiserId", it) }
        json.put("eventName", eventName)

        return json.toString()
    }
}
