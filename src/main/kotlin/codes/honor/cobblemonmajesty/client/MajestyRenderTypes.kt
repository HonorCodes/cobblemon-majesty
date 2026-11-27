package codes.honor.cobblemonmajesty.client

import codes.honor.cobblemonmajesty.CobblemonMajesty
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.Util
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import org.joml.Matrix4f

object MajestyRenderTypes : RenderStateShard("cobblemon_majesty_shards", Runnable {}, Runnable {}) {
    val WISP_TEXTURE: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(CobblemonMajesty.ID, "textures/entity/aura_wisp.png")
    val STAR_TEXTURE: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(CobblemonMajesty.ID, "textures/entity/star_mote.png")
    val HALO_TEXTURE: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(CobblemonMajesty.ID, "textures/entity/ground_halo.png")
    val VEINS_A_TEXTURE: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(CobblemonMajesty.ID, "textures/entity/body_veins_a.png")
    val VEINS_B_TEXTURE: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(CobblemonMajesty.ID, "textures/entity/body_veins_b.png")
    val SUMMARY_BADGE_TEXTURE: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(CobblemonMajesty.ID, "textures/gui/icon_majestic.png")

    val AURA_WISP: RenderType = createBillboardLayer("cobblemon_majesty_wisp", WISP_TEXTURE)
    val STAR_MOTE: RenderType = createBillboardLayer("cobblemon_majesty_star", STAR_TEXTURE)
    val GROUND_HALO: RenderType = createBillboardLayer("cobblemon_majesty_halo", HALO_TEXTURE)
    val BODY_VEINS_A: RenderType = createScrollingSheenLayer("cobblemon_majesty_veins_a", VEINS_A_TEXTURE, 0.045f, -0.19f)
    val BODY_VEINS_B: RenderType = createScrollingSheenLayer("cobblemon_majesty_veins_b", VEINS_B_TEXTURE, -0.055f, -0.23f)

    private fun createBillboardLayer(name: String, texture: ResourceLocation): RenderType {
        val state = RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
            .setTextureState(TextureStateShard(texture, false, false))
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

    private fun createScrollingSheenLayer(
        name: String,
        texture: ResourceLocation,
        speedU: Float,
        speedV: Float
    ): RenderType {
        val dynamicScroll = TexturingStateShard(
            "${name}_scroll",
            Runnable {
                val seconds = (Util.getMillis() % 1_000_000L).toFloat() / 1000f
                val u = (seconds * speedU) % 1.0f
                val v = (seconds * speedV) % 1.0f
                RenderSystem.setTextureMatrix(Matrix4f().translation(u, v, 0.0f))
            },
            Runnable {
                RenderSystem.resetTextureMatrix()
            }
        )
        val state = RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
            .setTextureState(TextureStateShard(texture, false, false))
            .setTexturingState(dynamicScroll)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setCullState(CULL)
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
