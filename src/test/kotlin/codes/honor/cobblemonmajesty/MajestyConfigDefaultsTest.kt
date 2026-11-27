package codes.honor.cobblemonmajesty

import codes.honor.cobblemonmajesty.config.MajestyConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MajestyConfigDefaultsTest {
    @Test
    fun commonConfigHasExpectedDefaults() {
        assertTrue(MajestyConfig.spawning.enabled.default)
        assertEquals(6000, MajestyConfig.spawning.oneIn.default)
        assertEquals(6000, MajestyConfig.spawning.trainerOneIn.default)
        assertEquals(2, MajestyConfig.spawning.additionalPerfectIvs.default)
        assertTrue(MajestyConfig.spawning.wildSpawns.default)
        assertTrue(MajestyConfig.spawning.fishingSpawns.default)
        assertTrue(MajestyConfig.spawning.snackSpawns.default)
        assertTrue(MajestyConfig.spawning.fossils.default)
        assertTrue(MajestyConfig.spawning.starters.default)
        assertTrue(MajestyConfig.spawning.trainerPokemon.default)

        assertTrue(MajestyConfig.combat.damageBonusEnabled.default)
        assertEquals(1.1, MajestyConfig.combat.damageMultiplier.default, 1e-6)
    }
}
