package tech.kshot.midnightprover

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ProofJobInputTest {
    @Test fun emptyInputIsRejected() {
        assertNotNull(ProofJobInput.validationError(""))
        assertNotNull(ProofJobInput.validationError("   "))
    }

    @Test fun oddLengthHexIsRejected() {
        assertNotNull(ProofJobInput.validationError("abc"))
    }

    @Test fun nonHexIsRejected() {
        assertNotNull(ProofJobInput.validationError("zz00"))
    }

    @Test fun validHexPasses() {
        assertNull(ProofJobInput.validationError("00ffAA10"))
        assertNull(ProofJobInput.validationError(" 00 ff "))
    }

    @Test fun cleanHexNormalises() {
        assertEquals("00ffaa", ProofJobInput.cleanHex(" 00 FF\naA "))
    }

    @Test fun baseUrlNormalisation() {
        assertEquals("http://127.0.0.1:6300", ProofJobInput.normaliseBaseUrl("127.0.0.1:6300/"))
        assertEquals("http://127.0.0.1:6300", ProofJobInput.normaliseBaseUrl("http://127.0.0.1:6300"))
        assertEquals("https://example.org:6300", ProofJobInput.normaliseBaseUrl("https://example.org:6300/"))
    }
}

class JobHistoryFormatTest {
    @Test fun summariseShortTx() {
        assertEquals("0x00ff · 2 bytes", JobHistoryFormat.summariseTx("00ff"))
    }

    @Test fun summariseLongTx() {
        assertEquals("0x00112233…8899 · 10 bytes", JobHistoryFormat.summariseTx("00112233445566778899"))
    }

    @Test fun durationFormatting() {
        assertEquals("3.3 s", JobHistoryFormat.formatDuration(3.277))
        assertEquals("2 min 05 s", JobHistoryFormat.formatDuration(125.0))
    }
}

class HexCodecTest {
    @Test fun roundTrip() {
        val bytes = byteArrayOf(0x00, 0x6d, 0x69, 0x64, 0xff.toByte())
        assertEquals("006d6964ff", HexCodec.bytesToHex(bytes))
        assertTrue(HexCodec.hexToBytes("006d6964ff").contentEquals(bytes))
    }

    @Test fun oddLengthRejected() {
        assertThrows(IllegalArgumentException::class.java) { HexCodec.hexToBytes("abc") }
    }

    @Test fun nonHexRejected() {
        assertThrows(IllegalArgumentException::class.java) { HexCodec.hexToBytes("zz") }
    }
}

class ProofFormatTest {
    @Test fun recognisesProofHeader() {
        val proof = "midnight:proof-versioned:".toByteArray(Charsets.US_ASCII) + byteArrayOf(1, 2, 3)
        assertTrue(ProofFormat.isProof(proof))
        assertEquals("Midnight proof · ${proof.size} bytes", ProofFormat.summarise(proof))
    }

    @Test fun rejectsOtherBytes() {
        assertTrue(!ProofFormat.isProof("garbage".toByteArray(Charsets.US_ASCII)))
    }
}

class ReadyFieldsTest {
    @Test fun parsesLiveShape() {
        val body = """{"status":"ok","jobsProcessing":2,"jobsPending":1,"jobCapacity":10,"timestamp":"2026-11-25 21:43:55 +00:00:00"}"""
        val r = ReadyFields.parse(body)
        assertEquals("ok", r.status)
        assertEquals(2, r.jobsProcessing)
        assertEquals(1, r.jobsPending)
        assertEquals(10, r.jobCapacity)
    }

    @Test fun parsesBusyShape() {
        val r = ReadyFields.parse("""{"status":"busy"}""")
        assertEquals("busy", r.status)
        assertEquals(null, r.jobCapacity)
    }
}
