package tech.kshot.midnightprover

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders every screen and state to docs/screenshots/ (record with
 * `gradle testDebugUnitTest -Proborazzi.test.record=true`). States are
 * built explicitly; the "online" server card uses the field values the real
 * 9.0.2 server returned in GROK-17's live run, and the proof result uses the
 * real proof size it returned (2,940 bytes). Rendered by Robolectric on the
 * JVM — not an emulator or a phone.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val rule = createComposeRule()
    private val app get() = ApplicationProvider.getApplicationContext<ProverApplication>()
    private val now = System.currentTimeMillis()

    private val online = ServerHealth.from(
        ServerStatus(reachable = true, version = "9.0.2", proofVersions = "[\"V2\", \"V3\"]", ready = true,
            readyStatus = "ok", jobCapacity = 10, latencyMs = 6)
    )
    private val offline = ServerHealth.from(ServerStatus(reachable = false, failure = ProveException.Unreachable("refused")))
    private val busy = ServerHealth.from(
        ServerStatus(reachable = true, version = "9.0.2", proofVersions = "[\"V2\", \"V3\"]", ready = false,
            readyStatus = "busy", jobsProcessing = 10, jobCapacity = 10)
    )
    private val settings = ProverSettings(onboardingDone = true)
    private val proofHex = HexCodec.bytesToHex(ProofFormat.PROOF_HEADER_TEXT.toByteArray() + ByteArray(2940 - 25) { (it * 31).toByte() })

    private fun shoot(name: String, state: ProverUiState, tab: Tab = Tab.PROVE, onboardingPage: Int? = null, scrollToTag: String? = null) {
        val vm = ProverViewModel(app, SavedStateHandle())
        rule.setContent {
            MidnightProverTheme(reduceMotion = true) {
                if (onboardingPage != null) {
                    androidx.compose.runtime.CompositionLocalProvider(LocalWidthClass provides WidthClass.COMPACT) {
                        StarfieldBackground(); OnboardingScreen(state, vm, onboardingPage)
                    }
                } else ProverApp(vm, state, tab)
            }
        }
        if (scrollToTag != null) rule.onNodeWithTag(scrollToTag).performScrollTo()
        rule.onRoot().captureRoboImage("../docs/screenshots/$name.png")
    }

    private fun history() = listOf(
        JobRecord(now - 60_000, "http://127.0.0.1:6300", "0x6d69646e…6f00 · 1.5 KB", true, 0.6, "Midnight proof · 2.9 KB", "midnight/dust/spend"),
        JobRecord(now - 3_600_000, "http://127.0.0.1:6300", "0x6d69646e…1f2a · 58.9 KB", true, 0.4, "Midnight proof · 3.8 KB", "contract:006db7d9…/storeMessage"),
        JobRecord(now - 7_200_000, "http://127.0.0.1:6300", "0x00ff · 2 bytes", false, 0.0, "The server rejected this payload"),
        JobRecord(now - 90_000_000, "http://127.0.0.1:6300", "0x6d69646e…6f00 · 1.5 KB", false, 4.2, "Cancelled", "midnight/dust/spend", cancelled = true)
    )

    @Test fun s01_onboarding_welcome() = shoot("01-onboarding-welcome", ProverUiState(settings = ProverSettings()), onboardingPage = 0)
    @Test fun s02_onboarding_testnet() = shoot("02-onboarding-testnet", ProverUiState(settings = ProverSettings()), onboardingPage = 2)
    @Test fun s03_onboarding_setup() = shoot("03-onboarding-setup", ProverUiState(settings = ProverSettings()), onboardingPage = 3)
    @Test fun s04_onboarding_connect() = shoot("04-onboarding-connect", ProverUiState(settings = ProverSettings(), health = online), onboardingPage = 4)
    @Test fun s05_prove_empty() = shoot("05-prove-empty", ProverUiState(settings = settings, health = online))
    @Test fun s06_prove_inspect() = shoot("06-prove-payload-inspected", ProverUiState(settings = settings, health = online, draft = Fixtures.dustSpendHex))
    @Test fun s07_proving() = shoot("07-proving", ProverUiState(settings = settings, health = online,
        session = ProveSession(JobState.PROVING, Fixtures.dustSpendHex, "http://127.0.0.1:6300", now - 12_000)))
    @Test fun s08_done() = shoot("08-proof-ready", ProverUiState(settings = settings, health = online,
        session = ProveSession(JobState.DONE, Fixtures.dustSpendHex, "http://127.0.0.1:6300", now - 722, now, proofHex)))
    @Test fun s09_failed_unreachable() = shoot("09-failed-unreachable", ProverUiState(settings = settings, health = offline, draft = Fixtures.dustSpendHex,
        session = ProveSession(JobState.FAILED, Fixtures.dustSpendHex, "http://127.0.0.1:6300", now - 50, now,
            failure = FailureInfo("Unreachable", "Can't reach the proof server", "refused"))))
    @Test fun s10_interrupted() = shoot("10-interrupted", ProverUiState(settings = settings, health = online, draft = Fixtures.dustSpendHex,
        session = ProveSession(JobState.INTERRUPTED, Fixtures.dustSpendHex, "http://127.0.0.1:6300", now - 50_000, now,
            failure = FailureInfo("Interrupted", "Proof interrupted", Copy.ERR_INTERRUPTED))))
    @Test fun s11_server_online() = shoot("11-server-online", ProverUiState(settings = settings, health = online), Tab.SERVER)
    @Test fun s12_server_busy() = shoot("12-server-busy", ProverUiState(settings = settings, health = busy), Tab.SERVER)
    @Test fun s13_server_offline_bad_url() = shoot("13-server-offline-invalid-url", ProverUiState(settings = settings, health = offline, urlDraft = "http://host:6300/prove"), Tab.SERVER)
    @Test fun s14_activity() = shoot("14-activity", ProverUiState(settings = settings, health = online, history = history()), Tab.ACTIVITY)
    @Test fun s15_activity_empty() = shoot("15-activity-empty", ProverUiState(settings = settings, health = online), Tab.ACTIVITY)
    @Test fun s16_settings_about_tip() = shoot("16-settings-about-tip", ProverUiState(settings = settings, health = online), Tab.SETTINGS)

    @Test fun s16b_tip() = shoot("16b-about-tip", ProverUiState(settings = settings, health = online), Tab.SETTINGS, scrollToTag = "tip")
    @Test fun s16c_off_phone_enabled() = shoot("16c-server-off-phone-opt-in", ProverUiState(
        settings = settings.copy(allowOffPhone = true, baseUrl = "https://prover.example.org"), urlDraft = "https://prover.example.org"), Tab.SERVER)

    @Config(qualifiers = "w884dp-h841dp-xhdpi")
    @Test fun s17_unfolded_prove() = shoot("17-unfolded-prove-inspected", ProverUiState(settings = settings, health = online, draft = Fixtures.dustSpendHex))

    @Config(qualifiers = "w884dp-h841dp-xhdpi")
    @Test fun s18_unfolded_done() = shoot("18-unfolded-proof-ready", ProverUiState(settings = settings, health = online,
        session = ProveSession(JobState.DONE, Fixtures.dustSpendHex, "http://127.0.0.1:6300", now - 722, now, proofHex)))

    @Config(qualifiers = "w884dp-h841dp-xhdpi")
    @Test fun s19_unfolded_settings() = shoot("19-unfolded-settings-tip", ProverUiState(settings = settings, health = online), Tab.SETTINGS)

    @Config(qualifiers = "w411dp-h891dp-xxhdpi", fontScale = 1.6f)
    @Test fun s20_large_font() = shoot("20-large-font-prove", ProverUiState(settings = settings, health = online, draft = Fixtures.dustSpendHex))

    // Joe's 5 configs: 411 (above), 360, 360@font 2.0, 884 unfolded (above), landscape.
    @Config(qualifiers = "w360dp-h760dp-xxhdpi")
    @Test fun s21_small360_prove() = shoot("21-small360-prove-inspected", ProverUiState(settings = settings, health = online, draft = Fixtures.dustSpendHex))
    @Config(qualifiers = "w360dp-h760dp-xxhdpi")
    @Test fun s22_small360_server() = shoot("22-small360-server-online", ProverUiState(settings = settings, health = online), Tab.SERVER)
    @Config(qualifiers = "w360dp-h760dp-xxhdpi", fontScale = 2.0f)
    @Test fun s23_small360_font2_prove() = shoot("23-small360-font2-prove", ProverUiState(settings = settings, health = online, draft = Fixtures.dustSpendHex))
    @Config(qualifiers = "w360dp-h760dp-xxhdpi", fontScale = 2.0f)
    @Test fun s24_small360_font2_settings() = shoot("24-small360-font2-settings", ProverUiState(settings = settings, health = online), Tab.SETTINGS)
    @Config(qualifiers = "w884dp-h841dp-xhdpi")
    @Test fun s25_unfolded884_server() = shoot("25-unfolded884-server-busy", ProverUiState(settings = settings, health = busy), Tab.SERVER)
    @Config(qualifiers = "w640dp-h360dp-xhdpi")
    @Test fun s26_landscape_prove() = shoot("26-landscape-prove-inspected", ProverUiState(settings = settings, health = online, draft = Fixtures.dustSpendHex))
    @Config(qualifiers = "w640dp-h360dp-xhdpi")
    @Test fun s27_landscape_onboarding() = shoot("27-landscape-onboarding-welcome", ProverUiState(settings = ProverSettings()), onboardingPage = 0)
    @Config(qualifiers = "w640dp-h360dp-xhdpi")
    @Test fun s28_landscape_settings() = shoot("28-landscape-settings-tip", ProverUiState(settings = settings, health = online), Tab.SETTINGS)
}
