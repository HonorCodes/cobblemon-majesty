package codes.honor.cobblemonmajesty.feature

import com.cobblemon.mod.common.api.pokemon.feature.FlagSpeciesFeature
import com.cobblemon.mod.common.api.pokemon.feature.FlagSpeciesFeatureProvider
import com.cobblemon.mod.common.api.pokemon.feature.GlobalSpeciesFeatures
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.pokemon.IVs
import com.cobblemon.mod.common.pokemon.Pokemon
import kotlin.random.Random

object MajestyFeature {
    const val ASPECT = "majestic"
    const val ROLLED_TAG = "cobblemon_majesty_rolled"

    fun registerProvider() {
        if (GlobalSpeciesFeatures.getCodeFeature(ASPECT) == null) {
            GlobalSpeciesFeatures.register(ASPECT, FlagSpeciesFeatureProvider(listOf(ASPECT), false))
        }
    }

    fun isMajestic(pokemon: Pokemon): Boolean {
        if (ASPECT in pokemon.aspects || ASPECT in pokemon.forcedAspects) return true
        return pokemon.features.filterIsInstance<FlagSpeciesFeature>().any { it.name == ASPECT && it.enabled }
    }

    fun isMajesticAspects(aspects: Set<String>): Boolean = ASPECT in aspects

    fun hasBeenRolled(pokemon: Pokemon): Boolean = pokemon.persistentData.getBoolean(ROLLED_TAG)

    fun markRolled(pokemon: Pokemon) {
        pokemon.persistentData.putBoolean(ROLLED_TAG, true)
    }

    fun applyMajestic(pokemon: Pokemon, additionalPerfectIvs: Int, random: Random = Random.Default): Int {
        markRolled(pokemon)
        val existing = pokemon.features.filterIsInstance<FlagSpeciesFeature>().firstOrNull { it.name == ASPECT }
        val feature = if (existing != null) {
            existing.enabled = true
            existing
        } else {
            FlagSpeciesFeature(ASPECT, true).also { pokemon.features.add(it) }
        }
        pokemon.markFeatureDirty(feature)
        pokemon.updateAspects()
        pokemon.entity?.let { syncEntityAspects(it, pokemon) }
        return applyAdditionalPerfectIvs(pokemon, additionalPerfectIvs, random)
    }

    fun applyAdditionalPerfectIvs(pokemon: Pokemon, additionalPerfectIvs: Int, random: Random = Random.Default): Int {
        if (additionalPerfectIvs <= 0) return 0
        val wasAtFullHealth = pokemon.currentHealth >= pokemon.maxHealth
        val currentMap = Stats.PERMANENT.associate { stat -> stat.identifier.toString() to (pokemon.ivs[stat] ?: 0) }
        val chosenKeys = MajestyIvs.selectStatsToMaximize(currentMap, additionalPerfectIvs, random).toSet()
        if (chosenKeys.isEmpty()) return 0
        var promoted = 0
        for (stat in Stats.PERMANENT) {
            if (stat.identifier.toString() in chosenKeys) {
                pokemon.setIV(stat, IVs.MAX_VALUE)
                promoted++
            }
        }
        if (wasAtFullHealth && pokemon.currentHealth < pokemon.maxHealth) {
            pokemon.currentHealth = pokemon.maxHealth
        }
        return promoted
    }

    fun syncEntityAspects(entity: PokemonEntity, pokemon: Pokemon) {
        val updated = LinkedHashSet(entity.aspects)
        updated.addAll(pokemon.aspects)
        updated.add(ASPECT)
        entity.entityData.set(PokemonEntity.ASPECTS, updated)
    }
}
