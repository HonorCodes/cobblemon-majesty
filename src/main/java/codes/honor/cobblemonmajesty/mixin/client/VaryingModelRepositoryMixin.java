package codes.honor.cobblemonmajesty.mixin.client;

import codes.honor.cobblemonmajesty.client.MajestyInvertedTextureCache;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = VaryingModelRepository.class, remap = false)
public abstract class VaryingModelRepositoryMixin {
    @Inject(
        method = "getTexture(Lnet/minecraft/resources/ResourceLocation;Lcom/cobblemon/mod/common/client/render/models/blockbench/PosableState;)Lnet/minecraft/resources/ResourceLocation;",
        at = @At("RETURN"),
        cancellable = true,
        require = 1
    )
    private void cobblemonMajesty$invertMajesticTexture(
        ResourceLocation name,
        PosableState state,
        CallbackInfoReturnable<ResourceLocation> cir
    ) {
        ResourceLocation original = cir.getReturnValue();
        if (original != null) {
            cir.setReturnValue(MajestyInvertedTextureCache.maybeInvertTexture(original, state));
        }
    }

    @Inject(
        method = "getTextureNoSubstitute(Lnet/minecraft/resources/ResourceLocation;Lcom/cobblemon/mod/common/client/render/models/blockbench/PosableState;)Lnet/minecraft/resources/ResourceLocation;",
        at = @At("RETURN"),
        cancellable = true,
        require = 1
    )
    private void cobblemonMajesty$invertMajesticTextureNoSub(
        ResourceLocation name,
        PosableState state,
        CallbackInfoReturnable<ResourceLocation> cir
    ) {
        ResourceLocation original = cir.getReturnValue();
        if (original != null) {
            cir.setReturnValue(MajestyInvertedTextureCache.maybeInvertTexture(original, state));
        }
    }
}
