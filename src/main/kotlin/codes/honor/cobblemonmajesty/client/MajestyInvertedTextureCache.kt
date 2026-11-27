package codes.honor.cobblemonmajesty.client

import codes.honor.cobblemonmajesty.CobblemonMajesty
import codes.honor.cobblemonmajesty.Guard
import codes.honor.cobblemonmajesty.feature.MajestyFeature
import codes.honor.cobblemonmajesty.palette.MajestyPalette
import com.cobblemon.mod.common.client.entity.PokemonClientDelegate
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState
import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.ResourceLocation

object MajestyInvertedTextureCache {
    private const val MAX_CACHED = 256

    private val forwardCache = LinkedHashMap<ResourceLocation, ResourceLocation?>()
    private val reverseLookup = HashMap<ResourceLocation, ResourceLocation>()
    private val guard = Guard("MajestyInvertedTextureCache")
    private var counter = 0

    fun clearCache() {
        val texManager = Minecraft.getInstance().textureManager
        for (dynId in forwardCache.values) {
            if (dynId != null) {
                guard.run {
                    texManager.release(dynId)
                }
            }
        }
        forwardCache.clear()
        reverseLookup.clear()
    }

    fun unwrapOriginal(location: ResourceLocation): ResourceLocation {
        return reverseLookup[location] ?: location
    }

    @JvmStatic
    fun maybeInvertTexture(resolved: ResourceLocation?, state: PosableState?): ResourceLocation? {
        if (resolved == null || state == null) return resolved
        val cfg = MajestyClientConfig.aura
        if (!cfg.enabled.get() || !cfg.invertBaseColors.get()) return resolved
        if (reverseLookup.containsKey(resolved)) return resolved

        val isMajestic = MajestyFeature.isMajesticAspects(state.currentAspects) ||
            (state is PokemonClientDelegate && MajestyFeature.isMajestic(state.currentEntity.pokemon))
        if (!isMajestic) return resolved

        return getOrCreateInverted(resolved) ?: resolved
    }

    private fun getOrCreateInverted(source: ResourceLocation): ResourceLocation? {
        if (forwardCache.containsKey(source)) {
            return forwardCache[source]
        }
        if (forwardCache.size >= MAX_CACHED) {
            clearCache()
        }

        val created = guard.callOrNull {
            val mc = Minecraft.getInstance()
            val resourceOpt = mc.resourceManager.getResource(source)
            if (resourceOpt.isEmpty) return@callOrNull null

            resourceOpt.get().open().use { stream ->
                NativeImage.read(stream).use { src ->
                    val w = src.width
                    val h = src.height
                    val dst = NativeImage(w, h, true)
                    for (y in 0 until h) {
                        for (x in 0 until w) {
                            dst.setPixelRGBA(x, y, MajestyPalette.invertPixelAbgr(src.getPixelRGBA(x, y)))
                        }
                    }
                    val dynTex = DynamicTexture(dst)
                    dynTex.setFilter(false, false)
                    val dynId = ResourceLocation.fromNamespaceAndPath(
                        CobblemonMajesty.ID,
                        "dynamic/inverted_${++counter}"
                    )
                    mc.textureManager.register(dynId, dynTex)
                    reverseLookup[dynId] = source
                    dynId
                }
            }
        }

        forwardCache[source] = created
        return created
    }
}
