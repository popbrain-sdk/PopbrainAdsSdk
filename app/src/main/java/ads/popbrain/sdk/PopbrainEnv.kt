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
 * Endpoints reached from this base:
 *  - `api/v1/analytics/install/add?clickId={clickId}` — install conversion
 *  - `api/v1/pixel/s2s`                               — in-app events (JSON POST)
 *
 * The URL is baked into the published AAR, so it must match the target before the release
 * is cut. Keeping it as a single constant means the switch is one line and greppable.
 */
internal object PopbrainEnv {

    const val TEST_BASE_URL = "https://devserver.popbrain.ai/"
    const val PRODUCTION_BASE_URL = "https://server.popbrain.ai/"

    /** Currently: PRODUCTION */
    const val BASE_URL = PRODUCTION_BASE_URL
}
