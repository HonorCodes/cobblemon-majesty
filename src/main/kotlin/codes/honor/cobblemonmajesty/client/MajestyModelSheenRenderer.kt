package codes.honor.cobblemonmajesty.client

import codes.honor.cobblemonmajesty.feature.MajestyFeature
import codes.honor.cobblemonmajesty.palette.TypeColorPair
import com.cobblemon.mod.common.client.render.models.blockbench.PosableModel
import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LightTexture

object MajestyModelSheenRenderer {
    @JvmStatic
    fun renderModelSheen(
        model: PosableModel,
        context: RenderContext,
        stack: PoseStack,
        packedOverlay: Int,
        baseColor: Int
    ) {
        val cfg = MajestyClientConfig.aura
        if (!cfg.enabled.get() || !cfg.bodySheenEnabled.get()) return
        val provider = model.bufferProvider ?: return

        val renderState = context.request(RenderContext.RENDER_STATE)
        if (renderState == RenderContext.RenderState.PROFILE && !cfg.summarySheenEnabled.get()) return

        val entity = context.request(RenderContext.ENTITY) as? PokemonEntity
        if (entity != null) {
            if (entity.isInvisible) return
            val maxDist = cfg.simpleSheenDistance.get()
            val camPos = Minecraft.getInstance().gameRenderer.mainCamera.position
            if (camPos.distanceToSqr(entity.position()) > maxDist * maxDist) return
        }

        val entityAspects = entity?.aspects.orEmpty()
        val stateAspects = context.request(RenderContext.POSABLE_STATE)?.currentAspects.orEmpty()
        val ctxAspects = context.request(RenderContext.ASPECTS).orEmpty()
        val aspects = when {
            entityAspects.isNotEmpty() -> entityAspects
            stateAspects.isNotEmpty() -> stateAspects
            else -> ctxAspects
        }
        val isMajestic = MajestyFeature.isMajesticAspects(aspects) || (entity != null && MajestyFeature.isMajestic(entity.pokemon))
        if (!isMajestic) return

        val baseAlpha = ((baseColor ushr 24) and 0xFF) / 255f * model.alpha
        if (baseAlpha <= 0.03f) return

        val speciesId = entity?.pokemon?.species?.resourceIdentifier ?: context.request(RenderContext.SPECIES)
        val (rgbA, rgbB) = MajestyColorResolver.resolveForSpeciesAndAspects(speciesId, aspects)
        val colA = TypeColorPair.rgbToFloats(rgbA)
        val colB = TypeColorPair.rgbToFloats(rgbB)

        val sheenAlpha = (0.32f * baseAlpha).coerceIn(0f, 1f)
        val tintA = packArgb(sheenAlpha, colA[0] * model.red, colA[1] * model.green, colA[2] * model.blue)
        val tintB = packArgb(sheenAlpha, colB[0] * model.red, colB[1] * model.green, colB[2] * model.blue)

        stack.pushPose()
        model.rootPart.render(
            context,
            stack,
            provider.getBuffer(MajestyRenderTypes.BODY_VEINS_A),
            LightTexture.FULL_BRIGHT,
            packedOverlay,
            tintA
        )
        model.rootPart.render(
            context,
            stack,
            provider.getBuffer(MajestyRenderTypes.BODY_VEINS_B),
            LightTexture.FULL_BRIGHT,
            packedOverlay,
            tintB
        )
        stack.popPose()
    }

    private fun packArgb(a: Float, r: Float, g: Float, b: Float): Int {
        val ai = (a.coerceIn(0f, 1f) * 255f).toInt()
        val ri = (r.coerceIn(0f, 1f) * 255f).toInt()
        val gi = (g.coerceIn(0f, 1f) * 255f).toInt()
        val bi = (b.coerceIn(0f, 1f) * 255f).toInt()
        return (ai shl 24) or (ri shl 16) or (gi shl 8) or bi
    }
}
