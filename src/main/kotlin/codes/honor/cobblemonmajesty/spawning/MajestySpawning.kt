package codes.honor.cobblemonmajesty.spawning

import codes.honor.cobblemonmajesty.CobblemonMajesty
import codes.honor.cobblemonmajesty.Guard
import codes.honor.cobblemonmajesty.config.MajestyConfig
import codes.honor.cobblemonmajesty.feature.MajestyFeature
import codes.honor.cobblemonmajesty.feature.MajestyIvs
import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.api.battles.model.actor.ActorType
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.pokemon.Pokemon
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

object MajestySpawning {
    object Counts {
        val naturalRolls = AtomicLong()
        val trainerRolls = AtomicLong()
        val naturalMajestics = AtomicLong()
        val trainerMajestics = AtomicLong()
    }

    private val eggGuard = Guard("Majesty egg exclusion")
    private val wildGuard = Guard("Majesty wild spawn roll")
    private val fishingGuard = Guard("Majesty fishing spawn roll")
    private val snackGuard = Guard("Majesty snack spawn roll")
    private val fossilGuard = Guard("Majesty fossil roll")
    private val starterGuard = Guard("Majesty starter roll")
    private val trainerGuard = Guard("Majesty trainer battle roll")

    fun register() {
        CobblemonEvents.HATCH_EGG_POST.subscribe(Priority.HIGHEST) { event ->
            eggGuard.run { MajestyFeature.markRolled(event.pokemon) }
        }
        CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(Priority.LOW) { event ->
            wildGuard.run {
                if (MajestyConfig.spawning.enabled.get() && MajestyConfig.spawning.wildSpawns.get()) {
                    tryNaturalRoll(event.entity.pokemon, "wild")
                }
            }
        }
        CobblemonEvents.BOBBER_SPAWN_POKEMON_POST.subscribe(Priority.NORMAL) { event ->
            fishingGuard.run {
                if (MajestyConfig.spawning.enabled.get() && MajestyConfig.spawning.fishingSpawns.get()) {
                    tryNaturalRoll(event.pokemon.pokemon, "fishing")
                }
            }
        }
        CobblemonEvents.POKE_SNACK_SPAWN_POKEMON_POST.subscribe(Priority.NORMAL) { event ->
            snackGuard.run {
                if (MajestyConfig.spawning.enabled.get() && MajestyConfig.spawning.snackSpawns.get()) {
                    tryNaturalRoll(event.pokemonEntity.pokemon, "snack")
                }
            }
        }
        CobblemonEvents.FOSSIL_REVIVED.subscribe(Priority.NORMAL) { event ->
            fossilGuard.run {
                if (MajestyConfig.spawning.enabled.get() && MajestyConfig.spawning.fossils.get()) {
                    tryNaturalRoll(event.pokemon, "fossil")
                }
            }
        }
        CobblemonEvents.STARTER_CHOSEN.subscribe(Priority.NORMAL) { event ->
            starterGuard.run {
                if (MajestyConfig.spawning.enabled.get() && MajestyConfig.spawning.starters.get()) {
                    tryNaturalRoll(event.pokemon, "starter")
                }
            }
        }
        CobblemonEvents.BATTLE_STARTED_PRE.subscribe(Priority.HIGH) { event ->
            trainerGuard.run {
                if (MajestyConfig.spawning.enabled.get() && MajestyConfig.spawning.trainerPokemon.get()) {
                    rollTrainerTeams(event.battle)
                }
            }
        }
    }

    private fun tryNaturalRoll(pokemon: Pokemon, source: String) {
        if (MajestyFeature.hasBeenRolled(pokemon) || MajestyFeature.isMajestic(pokemon)) return
        MajestyFeature.markRolled(pokemon)
        val oneIn = MajestyConfig.spawning.oneIn.get()
        if (oneIn <= 0) return
        Counts.naturalRolls.incrementAndGet()
        if (MajestyIvs.shouldRoll(oneIn, Random.Default)) {
            val promoted = MajestyFeature.applyMajestic(pokemon, MajestyConfig.spawning.additionalPerfectIvs.get())
            Counts.naturalMajestics.incrementAndGet()
            CobblemonMajesty.LOGGER.debug(
                "Rolled Majestic {} from {} (1 in {}, +{} perfect IVs)",
                pokemon.species.resourceIdentifier,
                source,
                oneIn,
                promoted
            )
        }
    }

    private fun rollTrainerTeams(battle: PokemonBattle) {
        val oneIn = MajestyConfig.spawning.trainerOneIn.get()
        if (oneIn <= 0) return
        val bonusIvs = MajestyConfig.spawning.additionalPerfectIvs.get()
        for (actor in battle.actors) {
            if (actor.type != ActorType.NPC) continue
            for (bp in actor.pokemonList) {
                val target = bp.effectedPokemon
                if (MajestyFeature.hasBeenRolled(target) ||
                    MajestyFeature.isMajestic(target) ||
                    MajestyFeature.isMajestic(bp.originalPokemon)
                ) {
                    continue
                }
                MajestyFeature.markRolled(target)
                Counts.trainerRolls.incrementAndGet()
                if (MajestyIvs.shouldRoll(oneIn, Random.Default)) {
                    val promoted = MajestyFeature.applyMajestic(target, bonusIvs)
                    Counts.trainerMajestics.incrementAndGet()
                    CobblemonMajesty.LOGGER.debug(
                        "Trainer {} rolled Majestic {} (1 in {}, +{} perfect IVs)",
                        actor.getName().string,
                        target.species.resourceIdentifier,
                        oneIn,
                        promoted
                    )
                }
            }
        }
    }
}
