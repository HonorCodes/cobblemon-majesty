package codes.honor.cobblemonmajesty.client

import codes.honor.cobblemonmajesty.CobblemonMajesty
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation

object MajestyRenderTypes : RenderStateShard("cobblemon_majesty_shards", Runnable {}, Runnable {}) {
    val WISP_TEXTURE: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(CobblemonMajesty.ID, "textures/entity/aura_wisp.png")
    val COMET_TEXTURE: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(CobblemonMajesty.ID, "textures/entity/comet_ember.png")
    val STAR_TEXTURE: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(CobblemonMajesty.ID, "textures/entity/star_mote.png")
    val HALO_TEXTURE: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(CobblemonMajesty.ID, "textures/entity/ground_halo.png")
    val AREA_GLOW_TEXTURE: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(CobblemonMajesty.ID, "textures/entity/area_glow.png")
    val SUMMARY_BADGE_TEXTURE: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(CobblemonMajesty.ID, "textures/gui/icon_majestic.png")

    val AURA_WISP: RenderType = createBillboardLayer("cobblemon_majesty_wisp", WISP_TEXTURE)
    val COMET_EMBER: RenderType = createBillboardLayer("cobblemon_majesty_comet", COMET_TEXTURE)
    val STAR_MOTE: RenderType = createBillboardLayer("cobblemon_majesty_star", STAR_TEXTURE)
    val GROUND_HALO: RenderType = createBillboardLayer("cobblemon_majesty_halo", HALO_TEXTURE)
    val AREA_LIGHT: RenderType = RenderType.eyes(AREA_GLOW_TEXTURE)

    private fun createBillboardLayer(name: String, texture: ResourceLocation): RenderType {
        val state = RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
            .setTextureState(TextureStateShard(texture, true, false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setWriteMaskState(COLOR_WRITE)
            .setLightmapState(LIGHTMAP)
            .setOverlayState(OVERLAY)
            .createCompositeState(false)
        val created: RenderType = RenderType.create(
            name,
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            state
        )
        return created
    }
}
