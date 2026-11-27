package codes.honor.cobblemonmajesty

import codes.honor.cobblemonmajesty.client.IrisShaderBridge
import codes.honor.cobblemonmajesty.feature.MajestyIvs
import codes.honor.cobblemonmajesty.palette.EyeBoneClassifier
import codes.honor.cobblemonmajesty.palette.MajestyPalette
import codes.honor.cobblemonmajesty.palette.TypeColorPair
import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MajestyIvsAndPaletteTest {
    @Test
    fun promotesAdditionalPerfectIvsOnTopOfExistingOnes() {
        val zeroPerfect = linkedMapOf(
            "hp" to 10, "atk" to 12, "def" to 4, "spa" to 20, "spd" to 8, "spe" to 15
        )
        val pickedFromZero = MajestyIvs.selectStatsToMaximize(zeroPerfect, 2, Random(42))
        assertEquals(2, pickedFromZero.size)
        assertEquals(2, pickedFromZero.distinct().size)

        val alphaThreePerfect = linkedMapOf(
            "hp" to 31, "atk" to 31, "def" to 31, "spa" to 14, "spd" to 9, "spe" to 22
        )
        val pickedFromAlpha = MajestyIvs.selectStatsToMaximize(alphaThreePerfect, 2, Random(42))
        assertEquals(2, pickedFromAlpha.size)
        assertTrue(pickedFromAlpha.none { it in setOf("hp", "atk", "def") })

        val fivePerfect = linkedMapOf(
            "hp" to 31, "atk" to 31, "def" to 31, "spa" to 31, "spd" to 31, "spe" to 7
        )
        assertEquals(listOf("spe"), MajestyIvs.selectStatsToMaximize(fivePerfect, 2, Random(42)))

        val allPerfect = linkedMapOf(
            "hp" to 31, "atk" to 31, "def" to 31, "spa" to 31, "spd" to 31, "spe" to 31
        )
        assertTrue(MajestyIvs.selectStatsToMaximize(allPerfect, 2, Random(42)).isEmpty())
        assertTrue(MajestyIvs.selectStatsToMaximize(zeroPerfect, 0, Random(42)).isEmpty())
    }

    @Test
    fun rollSemanticsRespectZeroOneAndProbability() {
        assertFalse(MajestyIvs.shouldRoll(0, Random(1)))
        assertFalse(MajestyIvs.shouldRoll(-5, Random(1)))
        assertTrue(MajestyIvs.shouldRoll(1, Random(1)))

        val rng = Random(123456)
        var hits = 0
        val trials = 600_000
        repeat(trials) {
            if (MajestyIvs.shouldRoll(6000, rng)) hits++
        }
        assertTrue(hits in 60..145, "Expected ~100 hits over 600k rolls at 1/6000, got $hits")
    }

    @Test
    fun dualTypeColorsStayDistinctAndNeverAveraged() {
        val (steel, fighting) = MajestyPalette.resolveDualStrandColors(
            primaryTypeName = "Steel",
            secondaryTypeName = "Fighting",
            fallbackPrimaryHue = 0xABD1F4,
            fallbackSecondaryHue = 0xEF565D
        )
        assertEquals(0xD8E4F0, steel)
        assertEquals(0xE63946, fighting)
        val averaged = (((0xD8 + 0xE6) / 2) shl 16) or (((0xE4 + 0x39) / 2) shl 8) or ((0xF0 + 0x46) / 2)
        assertNotEquals(averaged, steel)
        assertNotEquals(averaged, fighting)
    }

    @Test
    fun monoTypeAndCustomTypeFallbacksResolveTwoTonePair() {
        val (darkPrimary, darkAccent) = MajestyPalette.resolveDualStrandColors(
            primaryTypeName = "Dark",
            secondaryTypeName = null,
            fallbackPrimaryHue = 0x587DA0,
            fallbackSecondaryHue = null
        )
        assertEquals(0x3B1D6E, darkPrimary)
        assertEquals(0x9D4EDD, darkAccent)

        val (customPrimary, customAccent) = MajestyPalette.resolveDualStrandColors(
            primaryTypeName = "Sound",
            secondaryTypeName = null,
            fallbackPrimaryHue = 0x2080A0,
            fallbackSecondaryHue = null
        )
        assertEquals(0x2080A0, customPrimary)
        assertTrue(customAccent != customPrimary)

        assertEquals(0xD8E4F0, TypeColorPair.parseHexRgb("#D8E4F0"))
        assertEquals(0xE63946, TypeColorPair.parseHexRgb("e63946"))
        assertNull(TypeColorPair.parseHexRgb("#ZZZZZZ"))
        assertNull(TypeColorPair.parseHexRgb("#123"))
    }

    @Test
    fun eyeBoneClassifierMatchesEyesAndPupilsWhileExcludingLidsAndSpecies() {
        assertEquals(EyeBoneClassifier.Role.SCLERA_OR_EYE, EyeBoneClassifier.classify("eye_left", "lucario"))
        assertEquals(EyeBoneClassifier.Role.SCLERA_OR_EYE, EyeBoneClassifier.classify("right_eye", "raikou"))
        assertEquals(EyeBoneClassifier.Role.SCLERA_OR_EYE, EyeBoneClassifier.classify("lefteye", "pikachu"))
        assertEquals(EyeBoneClassifier.Role.SCLERA_OR_EYE, EyeBoneClassifier.classify("eyeright", "pikachu"))
        assertEquals(EyeBoneClassifier.Role.SCLERA_OR_EYE, EyeBoneClassifier.classify("eyes_open", "arcanine"))
        assertEquals(EyeBoneClassifier.Role.SCLERA_OR_EYE, EyeBoneClassifier.classify("eye_left2", "dodrio"))

        assertEquals(EyeBoneClassifier.Role.PUPIL_OR_IRIS, EyeBoneClassifier.classify("pupil_left", "articuno"))
        assertEquals(EyeBoneClassifier.Role.PUPIL_OR_IRIS, EyeBoneClassifier.classify("right_pupil", "zapdos"))
        assertEquals(EyeBoneClassifier.Role.PUPIL_OR_IRIS, EyeBoneClassifier.classify("iris_left", "gardevoir"))

        assertEquals(EyeBoneClassifier.Role.NONE, EyeBoneClassifier.classify("eyelid_left", "lucario"))
        assertEquals(EyeBoneClassifier.Role.NONE, EyeBoneClassifier.classify("eye_right_lid", "furret"))
        assertEquals(EyeBoneClassifier.Role.NONE, EyeBoneClassifier.classify("eyes_closed", "arcanine"))
        assertEquals(EyeBoneClassifier.Role.NONE, EyeBoneClassifier.classify("eyebrow_left", "machamp"))
        assertEquals(EyeBoneClassifier.Role.NONE, EyeBoneClassifier.classify("locator_eye_left", "articuno"))
        assertEquals(EyeBoneClassifier.Role.NONE, EyeBoneClassifier.classify("sableye", "sableye"))
        assertEquals(EyeBoneClassifier.Role.NONE, EyeBoneClassifier.classify("decidueye", "decidueye"))
    }

    @Test
    fun irisColoredEntityIdsStayWithinShaderRange() {
        for ((_, pair) in MajestyPalette.DEFAULTS) {
            val idPrimary = IrisShaderBridge.nearestShaderEntityId(pair.primaryRgb)
            val idAccent = IrisShaderBridge.nearestShaderEntityId(pair.accentRgb)
            assertTrue(idPrimary in 50300..50312, "Out of range shader entityId: $idPrimary")
            assertTrue(idAccent in 50300..50312, "Out of range shader entityId: $idAccent")
        }
        assertEquals(50302, IrisShaderBridge.nearestShaderEntityId(0xE63946))
        assertEquals(50300, IrisShaderBridge.nearestShaderEntityId(0xD8E4F0))
        assertEquals(50304, IrisShaderBridge.nearestShaderEntityId(0xFFE600))
        assertEquals(50310, IrisShaderBridge.nearestShaderEntityId(0x7B2CBF))
    }

    @Test
    fun baseTextureInversionPreservesAlphaAndDistinguishesVariants() {
        assertEquals(0, MajestyPalette.invertPixelAbgr(0x00FF8844))

        val normalBlue = (0xFF shl 24) or (0xC6 shl 16) or (0x72 shl 8) or 0x3E
        val shinyGold = (0xFF shl 24) or (0x4B shl 16) or (0xCD shl 8) or 0xE6
        val invNormal = MajestyPalette.invertPixelAbgr(normalBlue)
        val invShiny = MajestyPalette.invertPixelAbgr(shinyGold)

        assertEquals(0xFF, (invNormal ushr 24) and 0xFF)
        assertEquals(0xFF, (invShiny ushr 24) and 0xFF)
        assertNotEquals(invNormal, invShiny)

        val pureWhite = (0xCC shl 24) or 0xFFFFFF
        val invWhite = MajestyPalette.invertPixelAbgr(pureWhite)
        assertEquals(0xCC, (invWhite ushr 24) and 0xFF)
        assertTrue((invWhite and 0xFF) in 12..24)
    }
}
