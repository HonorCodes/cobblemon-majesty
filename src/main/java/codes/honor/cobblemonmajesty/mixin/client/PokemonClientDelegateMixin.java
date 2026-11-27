package codes.honor.cobblemonmajesty.mixin.client;

import codes.honor.cobblemonmajesty.client.MajestyWildPresence;
import com.cobblemon.mod.common.client.entity.PokemonClientDelegate;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PokemonClientDelegate.class, remap = false)
public abstract class PokemonClientDelegateMixin {
    @Inject(
        method = "tick(Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;)V",
        at = @At("TAIL"),
        require = 1
    )
    private void cobblemonMajesty$onClientTick(PokemonEntity entity, CallbackInfo ci) {
        MajestyWildPresence.onClientTick(entity);
    }
}
