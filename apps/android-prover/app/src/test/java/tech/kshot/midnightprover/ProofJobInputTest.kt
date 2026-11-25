package tech.kshot.midnightprover

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
        assertEquals("2 min 5 s", JobHistoryFormat.formatDuration(125.0))
    }
}
