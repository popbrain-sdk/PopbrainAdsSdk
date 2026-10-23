package ads.popbrain.sdk

internal object PopbrainServerRules {

    const val MAX_KEYS = 20
    const val MAX_KEY_LENGTH = 60
    const val MAX_STRING_LENGTH = 400

    const val MIN_EVENT_VALUE = 0.01
    const val MAX_EVENT_VALUE = 99_999_999.0
    const val MIN_USER_ID_LENGTH = 12
    const val MAX_USER_ID_LENGTH = 40

    private val CLICK_ID = Regex("^[0-9a-fA-F]{24}$")
    private val EVENT_NAME = Regex("^\\w{2,40}$")
    private val CLICK_ID_ALIAS_KEY = Regex("^click[_\\s-]?id$", RegexOption.IGNORE_CASE)
    private val CURRENCY = Regex("^[A-Z]{3}$")

    fun isValidClickId(value: String?): Boolean = value != null && CLICK_ID.matches(value)

    fun isValidEventName(value: String): Boolean = EVENT_NAME.matches(value)

    fun isValidCurrency(value: String): Boolean = CURRENCY.matches(value)

    fun isClickIdAliasKey(key: String): Boolean = CLICK_ID_ALIAS_KEY.matches(key)

    fun isValidKey(key: String): Boolean = key.isNotBlank() && key.length <= MAX_KEY_LENGTH

    fun isValidString(value: String): Boolean = value.trim().length <= MAX_STRING_LENGTH
}
