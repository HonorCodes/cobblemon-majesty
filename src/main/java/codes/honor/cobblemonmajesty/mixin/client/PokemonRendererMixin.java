package codes.honor.cobblemonmajesty.mixin.client;

import codes.honor.cobblemonmajesty.client.MajestyAuraRenderer;
import codes.honor.cobblemonmajesty.client.MajestyUiBadges;
import com.cobblemon.mod.common.client.render.pokemon.PokemonRenderer;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PokemonRenderer.class, remap = false)
public abstract class PokemonRendererMixin {
    @Inject(
        method = "render(Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At("TAIL"),
        require = 1
    )
    private void cobblemonMajesty$renderAura(
        PokemonEntity entity,
        float entityYaw,
        float partialTicks,
        PoseStack poseMatrix,
        MultiBufferSource buffer,
        int packedLight,
        CallbackInfo ci
    ) {
        MajestyAuraRenderer.renderWorldAura(entity, partialTicks, poseMatrix, buffer);
    }

    @Inject(
        method = "resolveBaseLabel(Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;)Lnet/minecraft/network/chat/MutableComponent;",
        at = @At("RETURN"),
        cancellable = true,
        require = 1
    )
    private void cobblemonMajesty$decorateLabel(PokemonEntity entity, CallbackInfoReturnable<MutableComponent> cir) {
        MutableComponent original = cir.getReturnValue();
        if (original != null) {
            cir.setReturnValue(MajestyUiBadges.decorateWorldLabel(entity, original));
        }
    }
}
