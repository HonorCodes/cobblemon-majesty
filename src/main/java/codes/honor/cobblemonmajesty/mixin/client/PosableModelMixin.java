package codes.honor.cobblemonmajesty.mixin.client;

import codes.honor.cobblemonmajesty.client.MajestyEyeGlowRenderer;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableModel;
import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PosableModel.class, remap = false)
public abstract class PosableModelMixin {
    @Inject(
        method = "render(Lcom/cobblemon/mod/common/client/render/models/blockbench/repository/RenderContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
        at = @At("TAIL"),
        require = 1
    )
    private void cobblemonMajesty$renderModelEyeGlow(
        RenderContext context,
        PoseStack stack,
        VertexConsumer buffer,
        int packedLight,
        int packedOverlay,
        int color,
        CallbackInfo ci
    ) {
        MajestyEyeGlowRenderer.renderModelEyeGlow((PosableModel) (Object) this, context, stack, packedOverlay, color);
    }
}
