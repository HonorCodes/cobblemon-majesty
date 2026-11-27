package codes.honor.cobblemonmajesty.config

import net.neoforged.neoforge.common.ModConfigSpec

object MajestyConfig {
    val SPEC: ModConfigSpec
    val spawning: SpawningSection
    val combat: CombatSection

    init {
        val builder = ModConfigSpec.Builder()
        spawning = SpawningSection(builder)
        combat = CombatSection(builder)
        SPEC = builder.build()
    }

    class SpawningSection(builder: ModConfigSpec.Builder) {
        val enabled: ModConfigSpec.BooleanValue
        val oneIn: ModConfigSpec.IntValue
        val trainerOneIn: ModConfigSpec.IntValue
        val additionalPerfectIvs: ModConfigSpec.IntValue
        val wildSpawns: ModConfigSpec.BooleanValue
        val fishingSpawns: ModConfigSpec.BooleanValue
        val snackSpawns: ModConfigSpec.BooleanValue
        val fossils: ModConfigSpec.BooleanValue
        val starters: ModConfigSpec.BooleanValue
        val trainerPokemon: ModConfigSpec.BooleanValue

        init {
            builder.comment("Natural and NPC trainer Majestic spawn odds and IV bonuses.").push("spawning")
            enabled = builder.comment("Enables natural and trainer Majestic rolls.").define("enabled", true)
            oneIn = builder.comment("Natural spawn roll odds as 1 in N. Set to 0 to disable natural rolls.")
                .defineInRange("oneIn", 6000, 0, 1_000_000)
            trainerOneIn = builder.comment("NPC and trainer Pokemon roll odds as 1 in N. Set to 0 to disable random trainer rolls.")
                .defineInRange("trainerOneIn", 6000, 0, 1_000_000)
            additionalPerfectIvs = builder.comment("Additional stats guaranteed to roll 31 IVs on top of existing 31 IVs.")
                .defineInRange("additionalPerfectIvs", 2, 0, 6)
            wildSpawns = builder.comment("Allows wild world spawner Pokemon to roll Majestic.").define("wildSpawns", true)
            fishingSpawns = builder.comment("Allows Pokerod fishing spawns to roll Majestic.").define("fishingSpawns", true)
            snackSpawns = builder.comment("Allows PokeSnack spawns to roll Majestic.").define("snackSpawns", true)
            fossils = builder.comment("Allows revived fossil Pokemon to roll Majestic.").define("fossils", true)
            starters = builder.comment("Allows chosen starter Pokemon to roll Majestic.").define("starters", true)
            trainerPokemon = builder.comment("Allows NPC and trainer mod Pokemon (such as RCT) to roll Majestic at battle start.")
                .define("trainerPokemon", true)
            builder.pop()
        }
    }

    class CombatSection(builder: ModConfigSpec.Builder) {
        val damageBonusEnabled: ModConfigSpec.BooleanValue
        val damageMultiplier: ModConfigSpec.DoubleValue

        init {
            builder.comment("Server-side Showdown combat bonus for Majestic Pokemon.").push("combat")
            damageBonusEnabled = builder.comment(
                "When true, Majestic Pokemon gain a final damage multiplier on moves matching their active natural types.",
                "When false, Majestic is purely aesthetic in battle."
            ).define("damageBonusEnabled", true)
            damageMultiplier = builder.comment("Final damage multiplier applied after STAB for moves matching active natural types.")
                .defineInRange("damageMultiplier", 1.1, 1.0, 10.0)
            builder.pop()
        }
    }
}
