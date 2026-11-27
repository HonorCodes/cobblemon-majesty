package codes.honor.cobblemonmajesty.client

import codes.honor.cobblemonmajesty.palette.MajestyPalette
import codes.honor.cobblemonmajesty.palette.TypeColorPair
import net.neoforged.neoforge.common.ModConfigSpec

object MajestyClientConfig {
    val SPEC: ModConfigSpec
    val aura: AuraSection
    val palette: PaletteSection

    @Volatile
    var paletteOverrides: Map<String, TypeColorPair> = emptyMap()
        private set

    init {
        val builder = ModConfigSpec.Builder()
        aura = AuraSection(builder)
        palette = PaletteSection(builder)
        SPEC = builder.build()
    }

    fun reloadPalette() {
        val loaded = LinkedHashMap<String, TypeColorPair>()
        for ((typeKey, defaults) in MajestyPalette.DEFAULTS) {
            val entry = palette.entries[typeKey] ?: continue
            val p = TypeColorPair.parseHexRgb(entry.primary.get()) ?: defaults.primaryRgb
            val a = TypeColorPair.parseHexRgb(entry.accent.get()) ?: defaults.accentRgb
            loaded[typeKey] = TypeColorPair(p, a)
        }
        paletteOverrides = loaded
    }

    class AuraSection(builder: ModConfigSpec.Builder) {
        val enabled: ModConfigSpec.BooleanValue
        val invertBaseColors: ModConfigSpec.BooleanValue
        val groundParticlesEnabled: ModConfigSpec.BooleanValue
        val starMotesEnabled: ModConfigSpec.BooleanValue
        val glowingEyesEnabled: ModConfigSpec.BooleanValue
        val auraLightEnabled: ModConfigSpec.BooleanValue
        val auraLightIntensity: ModConfigSpec.DoubleValue
        val wildChimeEnabled: ModConfigSpec.BooleanValue
        val sendOutSfxEnabled: ModConfigSpec.BooleanValue
        val nameplateBadgeEnabled: ModConfigSpec.BooleanValue
        val summaryBadgeEnabled: ModConfigSpec.BooleanValue
        val fullAuraDistance: ModConfigSpec.DoubleValue
        val maxFullAurasOnScreen: ModConfigSpec.IntValue

        init {
            builder.comment("Client rendering, base color inversion, shader eye/light toggles, LOD budget, and kill switch.").push("aura")
            enabled = builder.comment("Master client kill switch for all Majestic visual effects.").define("enabled", true)
            invertBaseColors = builder.comment("Inverts the base model texture colors of Majestic Pokemon while preserving Shiny and resource-pack variants.")
                .define("invertBaseColors", true)
            groundParticlesEnabled = builder.comment("Renders low ground-sweeping helical filaments and fine spark motes around the feet of Majestic Pokemon.")
                .define("groundParticlesEnabled", true)
            starMotesEnabled = builder.comment("Renders rising comet-embers and 4-point star sparkles around Majestic Pokemon.").define("starMotesEnabled", true)
            glowingEyesEnabled = builder.comment("Renders shader-compatible emissive eye glow on Majestic Pokemon.").define("glowingEyesEnabled", true)
            auraLightEnabled = builder.comment("Emits subtle aura-colored ground and area light readable by shaders.").define("auraLightEnabled", true)
            auraLightIntensity = builder.comment("Relative brightness scale for the subtle colored ground and area light.")
                .defineInRange("auraLightIntensity", 1.0, 0.1, 2.5)
            wildChimeEnabled = builder.comment("Plays a resonant harmonic chime when spotting an unowned wild Majestic Pokemon.")
                .define("wildChimeEnabled", true)
            sendOutSfxEnabled = builder.comment("Plays the custom Majestic sparkle SFX when a Majestic Pokemon emerges from a Poke Ball.")
                .define("sendOutSfxEnabled", true)
            nameplateBadgeEnabled = builder.comment("Shows the gold star mark on world and battle HUD nameplates.").define("nameplateBadgeEnabled", true)
            summaryBadgeEnabled = builder.comment("Shows the Majestic badge and tooltip in the Cobblemon Summary screen.")
                .define("summaryBadgeEnabled", true)
            fullAuraDistance = builder.comment("Maximum distance in blocks for Majestic auras, eye glow, and ground lighting.")
                .defineInRange("fullAuraDistance", 32.0, 4.0, 128.0)
            maxFullAurasOnScreen = builder.comment("Maximum number of full Majestic auras rendered per frame (nearest first).")
                .defineInRange("maxFullAurasOnScreen", 16, 1, 128)
            builder.pop()
        }
    }

    class TypePaletteEntry(
        val primary: ModConfigSpec.ConfigValue<String>,
        val accent: ModConfigSpec.ConfigValue<String>
    )

    class PaletteSection(builder: ModConfigSpec.Builder) {
        val entries: Map<String, TypePaletteEntry>

        init {
            builder.comment("Per-type #RRGGBB colors for Majestic dual-strand auras.").push("palette")
            val map = LinkedHashMap<String, TypePaletteEntry>()
            for ((typeKey, defaults) in MajestyPalette.DEFAULTS) {
                builder.push(typeKey)
                val p = builder.define("primary", TypeColorPair.formatHexRgb(defaults.primaryRgb)) {
                    it is String && TypeColorPair.parseHexRgb(it) != null
                }
                val a = builder.define("accent", TypeColorPair.formatHexRgb(defaults.accentRgb)) {
                    it is String && TypeColorPair.parseHexRgb(it) != null
                }
                builder.pop()
                map[typeKey] = TypePaletteEntry(p, a)
            }
            builder.pop()
            entries = map
        }
    }
}
