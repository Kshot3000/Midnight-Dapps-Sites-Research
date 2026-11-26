package tech.kshot.midnightprover

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Loads the real DUST-spend proving payload captured from the official hello-world flow (local devnet). */
object Fixtures {
    val dustSpendHex: String by lazy {
        Fixtures::class.java.getResource("/fixtures/dust-spend-payload.hex")!!.readText().trim()
    }
    val dustSpend: ByteArray by lazy { HexCodec.hexToBytes(dustSpendHex) }
}

class InputCleaningTest {
    @Test fun stripsAllWhitespaceAnd0xPrefix() {
        assertEquals("00ffaa", ProofJobInput.cleanHex(" 0x00\tFF\r\naA "))
        assertEquals("00ff", ProofJobInput.cleanHex("0X00ff"))
    }

    @Test fun messagesComeFromTheQaBrief() {
        assertEquals(Copy.INPUT_EMPTY, ProofJobInput.validationError(" \n"))
        assertEquals(Copy.INPUT_ODD, ProofJobInput.validationError("abc"))
        assertEquals(Copy.INPUT_NOT_HEX, ProofJobInput.validationError("zz00"))
        assertNull(ProofJobInput.validationError("0x00 ff\tAA"))
    }
}

class BaseUrlValidationTest {
    @Test fun acceptsThisPhone() {
        listOf("127.0.0.1:6300", "http://127.0.0.1:6300/", "localhost:6300", "http://localhost:6300")
            .forEach { assertNull(it, ProofJobInput.baseUrlError(it)) }
    }

    /** P0-5: off-phone servers need the opt-in AND https. */
    @Test fun offPhonePolicy() {
        assertEquals(Copy.URL_OFF_PHONE_DISABLED, ProofJobInput.baseUrlError("http://192.168.1.20:6300"))
        assertEquals(Copy.URL_OFF_PHONE_DISABLED, ProofJobInput.baseUrlError("https://prover.example.org"))
        assertEquals(Copy.URL_OFF_PHONE_NEEDS_HTTPS, ProofJobInput.baseUrlError("http://192.168.1.20:6300", allowOffPhone = true))
        assertNull(ProofJobInput.baseUrlError("https://prover.example.org", allowOffPhone = true))
        assertNull(ProofJobInput.baseUrlError("https://192.168.1.20:6300/", allowOffPhone = true))
    }

    @Test fun refusesBadAddresses() {
        assertNotNull(ProofJobInput.baseUrlError(""))
        assertNotNull(ProofJobInput.baseUrlError("http://"))
        assertNotNull(ProofJobInput.baseUrlError("ftp://host:6300"))
        assertNotNull(ProofJobInput.baseUrlError("http://host name:6300"))
        assertNotNull(ProofJobInput.baseUrlError("http://host:99999"))
        assertNotNull(ProofJobInput.baseUrlError("http://host:6300/prove"))
        assertNotNull(ProofJobInput.baseUrlError("http://host:6300?x=1"))
    }

    @Test fun localhostDetection() {
        assertTrue(ProofJobInput.isLocalhost("http://127.0.0.1:6300"))
        assertTrue(ProofJobInput.isLocalhost("localhost:6300"))
        assertFalse(ProofJobInput.isLocalhost("http://192.168.1.20:6300"))
    }
}

class PayloadInspectorTest {
    @Test fun readsTheRealDustSpendPayload() {
        val info = PayloadInspector.inspect(Fixtures.dustSpend)
        assertEquals(1509, info.sizeBytes)
        assertTrue(info.headerOk)
        assertEquals("midnight/dust/spend", info.keyLocation)
        assertEquals("dust/spend", info.circuitName)
        assertEquals("Built-in ledger circuit", info.kindLabel)
        assertFalse(info.carriesProverKey)
    }

    @Test fun readsAContractCircuit() {
        val addr = "006db7d9f54cc92cb4e614983830c30c849abc0feef729b4c11179c556ea3bfd"
        val text = PayloadInspector.PROVE_HEADER + "\u0001\u0002contract:$addr/storeMessage?vk=9d3c\u0000midnight:prover-key[v7](ir-source[v3-generic]):"
        val info = PayloadInspector.inspect(text.toByteArray(Charsets.ISO_8859_1))
        assertTrue(info.headerOk)
        assertEquals("storeMessage", info.circuitName)
        assertEquals(addr, info.contractAddress)
        assertEquals("Contract circuit", info.kindLabel)
        assertTrue(info.carriesProverKey)
    }

    @Test fun unknownPayload() {
        val info = PayloadInspector.inspect(byteArrayOf(1, 2, 3))
        assertFalse(info.headerOk)
        assertNull(info.circuitName)
        assertNull(PayloadInspector.inspectHex("zz"))
    }
}

class ProofFormatHexTest {
    @Test fun hexHeaderCheck() {
        val proof = ProofFormat.PROOF_HEADER_TEXT.toByteArray() + byteArrayOf(9, 9)
        assertTrue(ProofFormat.isProofHex(HexCodec.bytesToHex(proof)))
        assertFalse(ProofFormat.isProofHex("00ff"))
    }
}

class ServerHealthTest {
    private fun ok(version: String = "9.0.2", ready: Boolean = true, readyStatus: String = "ok") =
        ServerStatus(reachable = true, version = version, proofVersions = "[\"V2\", \"V3\"]", ready = ready, readyStatus = readyStatus, jobCapacity = 10)

    @Test fun mapping() {
        assertEquals(HealthKind.ONLINE, ServerHealth.from(ok()).kind)
        assertEquals(HealthKind.WRONG_VERSION, ServerHealth.from(ok(version = "9.0.0")).kind)
        assertEquals(HealthKind.BUSY, ServerHealth.from(ok(ready = false, readyStatus = "busy")).kind)
        assertEquals(HealthKind.NOT_READY, ServerHealth.from(ok(ready = false, readyStatus = "loading")).kind)
        assertEquals(HealthKind.OFFLINE, ServerHealth.from(ServerStatus(reachable = false, failure = ProveException.Unreachable("x"))).kind)
        assertEquals(HealthKind.ERROR, ServerHealth.from(ServerStatus(reachable = true, failure = ProveException.Http(500, "boom"))).kind)
    }

    /** GROK-14 fix 4: a busy server must not read as unreachable. */
    @Test fun busyIsNotUnreachable() {
        val h = ServerHealth.from(ok(ready = false, readyStatus = "busy"))
        assertEquals(HealthKind.BUSY, h.kind)
        assertEquals("Proof server busy", h.headline)
    }

    @Test fun proofVersionsShownWithoutJson() {
        assertEquals("V2, V3", Copy.formatProofVersions("[\"V2\", \"V3\"]"))
        assertEquals("?", Copy.formatProofVersions(null))
        assertEquals("Proofs V2, V3", Copy.chipProofs(Copy.formatProofVersions("[\"V2\",\"V3\"]")))
    }

    @Test fun queueFull() {
        assertTrue(ServerStatus(reachable = true, jobsPending = 10, jobCapacity = 10).queueFull)
        assertTrue(ServerStatus(reachable = true, readyStatus = "busy").queueFull)
        assertFalse(ServerStatus(reachable = true, jobsProcessing = 2, jobsPending = 4, jobCapacity = 10).queueFull)
    }
}

class ErrorClassificationTest {
    @Test fun ioErrorsKeepTimeoutsSeparate() {
        assertTrue(ProofServerClient.classifyIo(SocketTimeoutException("Read timed out"), "x", 60_000) is ProveException.Timeout)
        assertTrue(ProofServerClient.classifyIo(SocketTimeoutException("connect timed out"), "x", 60_000) is ProveException.Unreachable)
        assertTrue(ProofServerClient.classifyIo(ConnectException("refused"), "x", 1) is ProveException.Unreachable)
        assertTrue(ProofServerClient.classifyIo(UnknownHostException("h"), "x", 1) is ProveException.Unreachable)
        // Android's network security config blocks cleartext off the phone with UnknownServiceException.
        val blocked = ProofServerClient.classifyIo(java.net.UnknownServiceException("CLEARTEXT communication to 10.0.0.2 not permitted"), "x", 1)
        assertEquals(Copy.URL_OFF_PHONE_NEEDS_HTTPS, blocked.message)
    }

    @Test fun httpCodes() {
        assertTrue(ProofServerClient.classifyHttp(400, "bad") is ProveException.Rejected)
        assertTrue(ProofServerClient.classifyHttp(429, "") is ProveException.QueueFull)
        assertTrue(ProofServerClient.classifyHttp(503, "") is ProveException.Busy)
        val other = ProofServerClient.classifyHttp(500, "boom")
        assertTrue(other is ProveException.Http)
        assertEquals("The server answered with an error (HTTP 500). Details: boom", other.message)
    }

    @Test fun eachFailureKindHasItsOwnMessage() {
        val kinds = listOf("Unreachable", "Timeout", "QueueFull", "Busy", "Rejected", "Cancelled")
        val messages = kinds.map { Copy.failureMessage(FailureInfo(it, "t", "server text"), 15) }
        assertEquals(kinds.size, messages.toSet().size)
        assertEquals(Copy.errTimeout(15), Copy.failureMessage(FailureInfo("Timeout", "", ""), 15))
        assertTrue(Copy.failureMessage(FailureInfo("Rejected", "", "expected header tag"), 15).endsWith("Server said: expected header tag"))
    }
}

class HistoryOutcomeTest {
    @Test fun threeOutcomes() {
        val base = JobRecord(0, "u", "p", success = false, durationSeconds = 1.0, detail = "")
        assertEquals("PROVEN", base.copy(success = true).outcome)
        assertEquals("FAILED", base.outcome)
        assertEquals("CANCELLED", base.copy(cancelled = true).outcome)
    }

    /** Joe P2-1 regression. */
    @Test fun durationNeverShowsSixtySeconds() {
        assertEquals("1 min 59 s", JobHistoryFormat.formatDuration(119.7))
        assertEquals("1 min 00 s", JobHistoryFormat.formatDuration(59.97))
        assertEquals("59.9 s", JobHistoryFormat.formatDuration(59.94))
        assertEquals("2 min 00 s", JobHistoryFormat.formatDuration(119.96))
        assertEquals("0.0 s", JobHistoryFormat.formatDuration(-1.0))
    }

    @Test fun formats() {
        assertEquals("1.5 KB", JobHistoryFormat.formatBytes(1509))
        assertEquals("2.9 KB", JobHistoryFormat.formatBytes(2940))
        assertEquals("1 minute 5 seconds", JobHistoryFormat.spokenDuration(65.4))
        assertEquals("12 seconds", JobHistoryFormat.spokenDuration(12.0))
    }
}

/** Attribution in the app must match the repo copy character for character. */
class AttributionTest {
    private val ada = "addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v"
    private val midnight = "mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q"

    @Test fun addressesExact() {
        assertEquals(ada, Attribution.ADA_ADDRESS)
        assertEquals(midnight, Attribution.MIDNIGHT_ADDRESS)
        assertEquals("@kshot9000", Attribution.HANDLE)
        assertEquals("Independent builder — not affiliated with the Midnight Foundation or IOG.", Attribution.INDEPENDENCE)
    }

    @Test fun tipWordingNeverDonate() {
        assertTrue(Copy.TIP_INTRO.contains("tip", ignoreCase = true))
        val src = File("src/main/java/tech/kshot/midnightprover").walk().filter { it.isFile }.joinToString("\n") { it.readText() }
        assertFalse(Regex("donat", RegexOption.IGNORE_CASE).containsMatchIn(src))
    }
}

/** WCAG AA: every text colour on the card surface is at least 4.5:1. */
class PaletteContrastTest {
    @Test fun textColoursMeetAA() {
        val texts = mapOf(
            "Starlight" to MidnightPalette.Starlight, "Moonlight" to MidnightPalette.Moonlight,
            "DimStar" to MidnightPalette.DimStar, "VioletText" to MidnightPalette.VioletText,
            "ProofGlow" to MidnightPalette.ProofGlow, "WarningAmber" to MidnightPalette.WarningAmber,
            "ErrorRose" to MidnightPalette.ErrorRose
        )
        for (bg in listOf(MidnightPalette.Nebula, MidnightPalette.DeepSpace)) {
            texts.forEach { (name, c) -> assertTrue("$name ${Contrast.ratio(c, bg)}", Contrast.ratio(c, bg) >= 4.5) }
        }
    }

    /** Bob P1-14's exact hex values. */
    @Test fun usesBobsValues() {
        assertEquals(Color(0xFF4A67F4), MidnightPalette.ButtonStart)
        assertEquals(Color(0xFF8457EA), MidnightPalette.ButtonEnd)
        assertEquals(Color(0xFF818BBB), MidnightPalette.DimStar)
        assertEquals(Color(0xFF5A6393), MidnightPalette.FieldBorder)
        assertTrue(Contrast.ratio(MidnightPalette.FieldBorder, MidnightPalette.Nebula) >= 3.0)
    }

    @Test fun buttonTextMeetsAA() {
        assertTrue(Contrast.ratio(Color.White, MidnightPalette.ButtonStart) >= 4.5)
        assertTrue(Contrast.ratio(Color.White, MidnightPalette.ButtonEnd) >= 4.5)
    }
}

/** P0-5: the shipped network security config allows cleartext only to this phone. */
class NetworkSecurityConfigTest {
    private val xml = File("src/main/res/xml/network_security_config.xml").readText()
    private val manifest = File("src/main/AndroidManifest.xml").readText()

    @Test fun cleartextOnlyToLoopback() {
        assertTrue(xml.contains("<base-config cleartextTrafficPermitted=\"false\""))
        val domains = Regex("<domain[^>]*>([^<]+)</domain>").findAll(xml).map { it.groupValues[1] }.toSet()
        assertEquals(setOf("127.0.0.1", "localhost"), domains)
        assertTrue(manifest.contains("android:networkSecurityConfig=\"@xml/network_security_config\""))
        assertFalse(manifest.contains("usesCleartextTraffic=\"true\""))
    }

    /** Bob P0-4: a dark no-action-bar theme and an adaptive launcher icon. */
    @Test fun manifestHasThemeAndIcon() {
        assertTrue(manifest.contains("android:theme=\"@style/Theme.MidnightProver\""))
        assertTrue(manifest.contains("android:icon=\"@mipmap/ic_launcher\""))
        val themes = File("src/main/res/values/themes.xml").readText()
        assertTrue(themes.contains("parent=\"android:Theme.Material.NoActionBar\""))
        assertTrue(themes.contains("<item name=\"android:windowBackground\">@color/abyss</item>"))
        assertTrue(File("src/main/res/values/colors.xml").readText().contains("#FF05070F"))
        assertTrue(File("src/main/res/mipmap-anydpi/ic_launcher.xml").readText().contains("<adaptive-icon"))
    }
}
