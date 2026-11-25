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
