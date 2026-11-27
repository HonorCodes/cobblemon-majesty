package codes.honor.cobblemonmajesty.mixin;

import codes.honor.cobblemonmajesty.battle.MajestyShowdown;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.runner.graal.GraalShowdownService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GraalShowdownService.class, remap = false)
public abstract class GraalShowdownServiceMixin {
    @Inject(method = "openConnection()V", at = @At("TAIL"), require = 1)
    private void cobblemonMajesty$onOpenConnection(CallbackInfo ci) {
        MajestyShowdown.onShowdownBoot(this);
    }

    @Inject(method = "endBattle(Lcom/cobblemon/mod/common/api/battles/model/PokemonBattle;)V", at = @At("HEAD"), require = 1)
    private void cobblemonMajesty$onEndBattle(PokemonBattle battle, CallbackInfo ci) {
        MajestyShowdown.onBattleEnd(battle.getBattleId());
    }
}
