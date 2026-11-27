package codes.honor.cobblemonmajesty.mixin.client;

import codes.honor.cobblemonmajesty.client.MajestyUiBadges;
import com.cobblemon.mod.common.client.gui.summary.Summary;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Summary.class, remap = false)
public abstract class SummaryMixin extends Screen {
    @Shadow(remap = false)
    private Pokemon selectedPokemon;

    protected SummaryMixin(Component title) {
        super(title);
    }

    @Inject(
        method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
        at = @At("TAIL"),
        require = 1
    )
    private void cobblemonMajesty$renderSummaryBadge(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MajestyUiBadges.renderSummaryBadge(this.width, this.height, this.selectedPokemon, context, mouseX, mouseY);
    }
}
