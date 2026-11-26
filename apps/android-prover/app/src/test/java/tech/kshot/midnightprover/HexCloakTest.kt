package tech.kshot.midnightprover

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** CLAUDE-AP-7: the privacy cloak masks display hex and nothing else. */
class HexCloakTest {
    private val hex = "6d69646e696768743a70726f6f66" + "ab".repeat(64)

    @Test fun uncloakedPreviewMatchesTheStandardTruncatedForm() {
        assertEquals("0x" + hex.take(48) + "…" + hex.takeLast(8), HexCloak.preview(hex, cloaked = false))
    }

    @Test fun cloakedPreviewLeaksNoHex() {
        val masked = HexCloak.preview(hex, cloaked = true)
        assertEquals("0x" + HexCloak.MASK, masked)
        assertFalse(masked.drop(2).any { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test fun emptyHexStaysEmptyEitherWay() {
        assertEquals("", HexCloak.preview("", cloaked = true))
        assertEquals("", HexCloak.preview("", cloaked = false))
        assertEquals("", HexCloak.label("", cloaked = true))
    }

    @Test fun labelMasksOnlyWhenCloaked() {
        assertEquals("payload preview", HexCloak.label("payload preview", cloaked = false))
        val masked = HexCloak.label("payload preview", cloaked = true)
        assertEquals(HexCloak.MASK, masked)
        assertTrue(masked.all { it == '•' })
    }
}
