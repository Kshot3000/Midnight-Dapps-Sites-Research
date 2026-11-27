package tech.kshot.midnightprover

import android.net.Uri
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.io.RandomAccessFile

/** Robolectric Compose UI tests: real ViewModel, real engine, real composables. */
@RunWith(AndroidJUnit4::class)
class ComposeUiTest {
    @get:Rule val rule = createComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<ProverApplication>()
    private fun vm() = ProverViewModel(app, SavedStateHandle())

    private fun launch(vm: ProverViewModel, initialTab: Tab = Tab.PROVE) {
        rule.setContent {
            val state by vm.state.collectAsState()
            MidnightProverTheme(reduceMotion = true) { ProverApp(vm, state, initialTab) }
        }
    }

    private fun onboarded(): ProverViewModel {
        app.store.saveSettings(ProverSettings(onboardingDone = true, baseUrl = "http://127.0.0.1:1"))
        return vm()
    }

    @Test fun firstRunShowsOnboardingAndFinishes() {
        val vm = vm()
        launch(vm)
        rule.onNodeWithText(Copy.ONBOARD_WELCOME).assertIsDisplayed()
        repeat(4) { rule.onNodeWithTag("onboarding-next").performClick() }
        rule.onNodeWithText(Copy.ONBOARD_CONNECT_TITLE).assertIsDisplayed()
        rule.onNodeWithTag("onboarding-finish").performClick()
        rule.waitForIdle()
        assertTrue(vm.state.value.settings.onboardingDone)
        rule.onNodeWithText(Copy.HEADER_SUBTITLE).assertIsDisplayed()
    }

    @Test fun onboardingSetupPageShowsCommandsAndPhantomWarning() {
        launch(vm())
        repeat(3) { rule.onNodeWithTag("onboarding-next").performClick() }
        rule.onNodeWithText("./setup-proof-server.sh --start").assertExists()
        rule.onNodeWithText(Copy.ONBOARD_KEEP_RUNNING).assertExists()
        rule.onNodeWithContentDescription("Copy command: ./setup-proof-server.sh --start").assertExists()
    }

    @Test fun proveButtonNeedsAValidPayload() {
        launch(onboarded())
        rule.onNodeWithTag("prove").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("payload-field").assertDoesNotExist()
        rule.onNodeWithTag("hex-toggle").performScrollTo().performClick()
        rule.onNodeWithTag("payload-field").performScrollTo().performTextInput("abc")
        rule.onNodeWithText(Copy.INPUT_ODD).performScrollTo()
        rule.onNodeWithText(Copy.INPUT_ODD).assertIsDisplayed()
        rule.onNodeWithTag("prove").assertIsNotEnabled()
        rule.onNodeWithTag("payload-field").performTextReplacement("0x00ff")
        rule.onNodeWithTag("prove").assertIsEnabled()
    }

    @Test fun sampleLoadsAndShowsCircuit() {
        launch(onboarded())
        rule.onNodeWithTag("sample").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        rule.onNodeWithTag("payload-field").assertDoesNotExist()
        rule.onNodeWithText("dust/spend").assertExists()
        rule.onNodeWithTag("circuit-details").performScrollTo().performClick()
        rule.onNodeWithText("midnight/dust/spend").assertExists()
        rule.onNodeWithText("Not in payload — set by the circuit's key").assertExists()
        rule.onNodeWithTag("prove").performScrollTo().assertIsEnabled()
        rule.onNodeWithTag("replace-payload").performScrollTo().performClick()
        rule.onNodeWithTag("import-file").assertExists()
        rule.onNodeWithText("dust/spend").assertExists()
    }

    @Test fun proofServerStatusOpensConfiguration() {
        launch(onboarded())
        rule.onNodeWithTag("open-proof-server").performScrollTo().performClick()
        rule.onNodeWithTag("url-field").assertExists()
    }

    @Test fun proofResultCanHideItsPreviewAndStartACleanJob() {
        MockWebServer().use { server ->
            server.start()
            val proof = ProofFormat.PROOF_HEADER_TEXT.toByteArray() + ByteArray(40)
            server.enqueue(MockResponse().setBody(Buffer().write(proof)))
            val vm = onboarded()
            launch(vm)
            rule.runOnIdle {
                vm.loadSample()
                assertTrue(app.engine.start(Fixtures.dustSpendHex, server.url("/").toString().trimEnd('/'), ClientTimeouts()))
            }
            rule.waitUntil(20_000) {
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                vm.state.value.session.state == JobState.DONE
            }
            rule.onNodeWithTag("copy-proof").performScrollTo().assertIsDisplayed()
            rule.onNodeWithTag("result-cloak").performScrollTo().performClick()
            rule.waitForIdle()
            assertTrue(vm.state.value.settings.privacyCloak)
            rule.onNodeWithText(HexCloak.preview(HexCodec.bytesToHex(proof), true)).assertExists()
            rule.onNodeWithText("Start a new job").performScrollTo().performClick()
            rule.waitForIdle()
            assertEquals("", vm.state.value.draft)
            assertEquals(JobState.IDLE, vm.state.value.session.state)
            assertEquals(1, vm.state.value.history.size)
            rule.onNodeWithTag("result-card").assertDoesNotExist()
            rule.onNodeWithTag("prove").assertIsNotEnabled()
        }
    }

    @Test fun oversizedDocumentLeavesExistingDraftIntact() {
        val vm = onboarded()
        launch(vm)
        val file = File.createTempFile("oversized-payload", ".bin", app.cacheDir)
        try {
            RandomAccessFile(file, "rw").use { it.setLength(MainActivity.MAX_IMPORT_BYTES.toLong() + 1L) }
            rule.runOnIdle {
                vm.onDraftChange("0x00ff")
                vm.importDocument(Uri.fromFile(file), "a file")
            }
            rule.waitUntil(10_000) {
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                vm.state.value.importMessage.contains("larger than 2 MB")
            }
            assertEquals("0x00ff", vm.state.value.draft)
        } finally {
            file.delete()
        }
    }

    @Test fun provingAgainstNothingShowsUnreachableStateWithRetry() {
        org.robolectric.Shadows.shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        val vm = onboarded()
        launch(vm)
        rule.onNodeWithTag("sample").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        rule.waitUntil(10_000) { vm.state.value.draft.isNotEmpty() }
        rule.waitForIdle()
        rule.onNodeWithTag("prove").performScrollTo().assertIsEnabled().performSemanticsAction(SemanticsActions.OnClick)
        // The engine finishes on a background thread; pump Robolectric's paused main looper so the
        // ViewModel's combined state (collected on Main) catches up.
        rule.waitUntil(20_000) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            vm.state.value.session.state == JobState.FAILED
        }
        assertEquals("Unreachable", vm.state.value.session.failure?.kind)
        rule.onNodeWithText(Copy.ERR_UNREACHABLE, substring = true).assertExists()
        rule.onNodeWithTag("retry").assertExists()
    }

    @Test fun aboutShowsTipWithBothAddressesExactly() {
        launch(onboarded(), Tab.SETTINGS)
        rule.onNodeWithText("TIP").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(Copy.TIP_INTRO).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(Attribution.ADA_ADDRESS).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(Attribution.MIDNIGHT_ADDRESS).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Independent builder — not affiliated with the Midnight Foundation or IOG. · @kshot9000").performScrollTo().assertIsDisplayed()
        assertEquals(0, rule.onAllNodesWithText("Donate", substring = true, ignoreCase = true).fetchSemanticsNodes().size)
        rule.onNodeWithContentDescription("Copy Cardano (ADA) tip address").assertIsDisplayed()
    }

    @Test fun offPhoneNeedsOptInAndHttps() {
        val vm = onboarded()
        launch(vm, Tab.SETTINGS)
        rule.onNodeWithText("Allow a server off this phone").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        rule.onNodeWithText("Send private inputs off this phone?").assertIsDisplayed()
        rule.onNodeWithTag("confirm-off-phone").performClick()
        rule.waitForIdle()
        assertTrue(vm.state.value.settings.allowOffPhone)
        vm.onUrlDraftChange("http://192.168.1.20:6300")
        assertEquals(Copy.URL_OFF_PHONE_NEEDS_HTTPS, vm.state.value.let { ProofJobInput.baseUrlError(it.urlDraft, true) })
        vm.onUrlDraftChange("https://192.168.1.20:6300")
        assertTrue(vm.saveUrl())
        rule.waitForIdle()
        assertTrue(vm.state.value.offPhone)
        vm.setAllowOffPhone(false) // turning the opt-in off snaps back to this phone
        rule.waitForIdle()
        assertEquals(ProofJobInput.DEFAULT_BASE_URL, vm.state.value.settings.baseUrl)
    }

    @Test fun activityEmptyState() {
        launch(onboarded(), Tab.ACTIVITY)
        rule.onNodeWithText(Copy.ACTIVITY_EMPTY).assertIsDisplayed()
    }

    @Test fun serverUrlValidationKeepsDraftWhileTyping() {
        val vm = onboarded()
        launch(vm, Tab.SERVER)
        rule.onNodeWithTag("url-field").performScrollTo().performTextReplacement("")
        rule.onNodeWithText("Enter the proof server address, e.g. http://127.0.0.1:6300").assertIsDisplayed()
        rule.onNodeWithTag("save-url").assertIsNotEnabled()
        rule.onNodeWithTag("url-field").performTextInput("192.168.1.20:6300")
        assertEquals("192.168.1.20:6300", vm.state.value.urlDraft) // not normalised mid-typing (P0-3)
        rule.onNodeWithText(Copy.URL_OFF_PHONE_DISABLED).assertExists() // P0-5: off-phone refused by default
        rule.onNodeWithTag("save-url").assertIsNotEnabled()
        rule.onNodeWithTag("url-field").performTextReplacement("localhost:6300")
        rule.onNodeWithTag("save-url").performScrollTo().assertIsEnabled().performClick()
        rule.waitForIdle()
        assertEquals("http://localhost:6300", vm.state.value.settings.baseUrl)
    }

    @Test fun touchTargetsAreAtLeast48dp() {
        launch(onboarded())
        rule.onNodeWithTag("prove").performScrollTo().assertHeightIsAtLeast(48.dp)
        rule.onNodeWithTag("sample").assertHeightIsAtLeast(48.dp)
        rule.onNodeWithTag("check-server").assertHeightIsAtLeast(48.dp)
        rule.onNodeWithTag("tab-PROVE").assertHeightIsAtLeast(48.dp)
    }

    @Config(qualifiers = "+w900dp-h800dp")
    @Test fun expandedWidthUsesRailAndTwoPanes() {
        launch(onboarded())
        // Both panes visible at once: input controls and server card without scrolling.
        rule.onNodeWithTag("sample").assertIsDisplayed()
        rule.onNodeWithTag("server-card").assertIsDisplayed()
        rule.onNodeWithTag("tab-SETTINGS").assertIsDisplayed()
    }

    @Config(fontScale = 2.0f)
    @Test fun largeFontStillUsable() {
        launch(onboarded())
        rule.onNodeWithContentDescription("Step 1 of 4: Load").assertExists()
        rule.onNodeWithTag("prove").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("tab-SETTINGS").performClick()
        rule.onNodeWithText(Attribution.MIDNIGHT_ADDRESS).performScrollTo().assertIsDisplayed()
    }
}
