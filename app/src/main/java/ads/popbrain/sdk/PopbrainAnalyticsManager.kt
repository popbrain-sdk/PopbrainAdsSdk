package ads.popbrain.sdk

import org.json.JSONObject
import java.util.concurrent.TimeUnit

object PopbrainAnalyticsManager {

    private const val MAX_PENDING_EVENTS = 100
    private const val S2S_PATH = "api/v1/pixel/s2s"

    private const val MAX_EVENT_RETRIES = 2
    private val RETRY_DELAYS_SECONDS = longArrayOf(2, 8)

    private val RESERVED_KEYS = setOf("clickId", "advertiserId", "eventName")

    private val pending = ArrayDeque<Pair<String, Map<String, Any>?>>()

    @JvmStatic
    @JvmOverloads
    fun initAnalytics(clickId: String, advertiserId: String? = null) {
        val id = clickId.trim()
        if (!PopbrainServerRules.isValidClickId(id)) {
            PopbrainLogger.e("clickId must be a 24 character Popbrain click id, ignoring initAnalytics call")
            return
        }

        PopbrainStorage.clickId = id
        advertiserId?.trim()?.takeIf(String::isNotEmpty)?.let { PopbrainStorage.advertiserId = it }
        flushPending()
    }

    internal fun onAttributionResolved() {
        flushPending()
    }

    @JvmStatic
    @JvmOverloads
    fun sendEvent(eventName: String, extraParams: Map<String, Any>? = null) {
        val name = eventName.trim()
        if (!PopbrainServerRules.isValidEventName(name)) {
            PopbrainLogger.e(
                "Event name '$name' must be 2-40 letters, numbers or underscores, dropping event"
            )
            return
        }

        val clickId = PopbrainStorage.clickId?.takeIf(PopbrainServerRules::isValidClickId)
        if (clickId == null) {
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
        val clickId = PopbrainStorage.clickId?.takeIf(PopbrainServerRules::isValidClickId) ?: return
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

    internal fun buildBody(
        eventName: String,
        extraParams: Map<String, Any>?,
        clickId: String,
        advertiserId: String?
    ): String {
        val json = JSONObject()
        val sendAdvertiserId = advertiserId?.takeIf(PopbrainServerRules::isValidString)
        val maxExtraParams = PopbrainServerRules.MAX_KEYS - if (sendAdvertiserId != null) 3 else 2

        extraParams?.forEach { (key, value) ->
            when {
                key in RESERVED_KEYS ->
                    PopbrainLogger.e("Ignoring reserved param '$key' in event '$eventName'")
                !PopbrainServerRules.isValidKey(key) ->
                    PopbrainLogger.e("Ignoring param with blank or too long key in event '$eventName'")
                !isAcceptedValue(key, value) ->
                    PopbrainLogger.e("Ignoring param '$key' with unsupported value in event '$eventName'")
                json.length() >= maxExtraParams ->
                    PopbrainLogger.e("Too many params in event '$eventName', ignoring '$key'")
                else -> runCatching { json.put(key, value) }.onFailure {
                    PopbrainLogger.e("Ignoring unserialisable param '$key' in event '$eventName'")
                }
            }
        }

        json.put("clickId", clickId)
        sendAdvertiserId?.let { json.put("advertiserId", it) }
        json.put("eventName", eventName)

        return json.toString()
    }

    private fun isAcceptedValue(key: String, value: Any?): Boolean = when (key) {
        "value" -> eventValue(value)?.let {
            it >= PopbrainServerRules.MIN_EVENT_VALUE && it <= PopbrainServerRules.MAX_EVENT_VALUE
        } ?: false
        "currency" -> value is String && PopbrainServerRules.isValidCurrency(value)
        "userId" -> value is String && value.trim().length in
            PopbrainServerRules.MIN_USER_ID_LENGTH..PopbrainServerRules.MAX_USER_ID_LENGTH
        else -> when (value) {
            is Boolean -> true
            is Double -> value.isFinite()
            is Float -> value.isFinite()
            is Number -> true
            is String -> PopbrainServerRules.isValidString(value)
            else -> false
        }
    }

    private fun eventValue(value: Any?): Double? = when (value) {
        is Number -> value.toDouble().takeIf(Double::isFinite)
        is String -> value.trim().toDoubleOrNull()?.takeIf(Double::isFinite)
        else -> null
    }
}
