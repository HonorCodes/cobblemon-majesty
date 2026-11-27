package codes.honor.cobblemonmajesty.client

import codes.honor.cobblemonmajesty.palette.MajestyPalette
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.pokemon.Pokemon
import net.minecraft.resources.ResourceLocation

object MajestyColorResolver {
    fun resolveForEntity(entity: PokemonEntity): Pair<Int, Int> {
        val speciesId = entity.pokemon.species.resourceIdentifier
        return resolveForSpeciesAndAspects(speciesId, entity.aspects)
    }

    fun resolveForPokemon(pokemon: Pokemon): Pair<Int, Int> {
        val p1 = pokemon.primaryType
        val p2 = pokemon.secondaryType
        return MajestyPalette.resolveDualStrandColors(
            primaryTypeName = p1.name,
            secondaryTypeName = p2?.name,
            fallbackPrimaryHue = p1.hue,
            fallbackSecondaryHue = p2?.hue,
            overrides = MajestyClientConfig.paletteOverrides
        )
    }

    fun resolveForSpeciesAndAspects(speciesId: ResourceLocation?, aspects: Set<String>): Pair<Int, Int> {
        val species = speciesId?.let { PokemonSpecies.getByIdentifier(it) }
        val form = species?.getForm(aspects)
        val primary = form?.primaryType ?: species?.primaryType ?: ElementalTypes.NORMAL
        val secondary = form?.secondaryType ?: if (form == null) species?.secondaryType else null
        return MajestyPalette.resolveDualStrandColors(
            primaryTypeName = primary.name,
            secondaryTypeName = secondary?.name,
            fallbackPrimaryHue = primary.hue,
            fallbackSecondaryHue = secondary?.hue,
            overrides = MajestyClientConfig.paletteOverrides
        )
    }
}
