package ads.popbrain.sdk

/**
 * The one place the backend environment is selected.
 *
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │  SWITCH THIS BEFORE PUBLISHING                                           │
 * │                                                                          │
 * │  Test build   → https://devserver.popbrain.ai/                           │
 * │  Production   → https://server.popbrain.ai/                              │
 * └──────────────────────────────────────────────────────────────────────────┘
 *
 * The URL is baked into the published AAR, so it must match the target before the release
 * is cut. Keeping it as a single constant means the switch is one line and greppable.
 */
internal object PopbrainEnv {

    const val TEST_BASE_URL = "https://devserver.popbrain.ai/"
    const val PRODUCTION_BASE_URL = "https://server.popbrain.ai/"

    /** Currently: TEST */
    const val BASE_URL = TEST_BASE_URL
}
