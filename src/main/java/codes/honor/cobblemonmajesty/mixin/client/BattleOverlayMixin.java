package codes.honor.cobblemonmajesty.mixin.client;

import codes.honor.cobblemonmajesty.client.MajestyUiBadges;
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.cobblemon.mod.common.pokemon.status.PersistentStatus;
import com.cobblemon.mod.common.client.battle.ClientBallDisplay;
import com.cobblemon.mod.common.client.gui.battle.BattleOverlay;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Species;
import kotlin.Triple;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = BattleOverlay.class, remap = false)
public abstract class BattleOverlayMixin {
    @ModifyVariable(
        method = "drawBattleTile",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0,
        require = 0
    )
    private MutableComponent cobblemonMajesty$decorateTileName(
        MutableComponent displayName,
        GuiGraphics context,
        float x,
        float y,
        float partialTicks,
        boolean reversed,
        Species species,
        int level,
        MutableComponent originalDisplayName,
        Gender gender,
        PersistentStatus status,
        PosableState state,
        Triple<Float, Float, Float> colour,
        float opacity,
        ClientBallDisplay ballState,
        int maxHealth,
        float health,
        boolean isSelected,
        boolean isHovered,
        boolean isCompact,
        MutableComponent actorDisplayName,
        boolean isFlatHealth,
        PokedexEntryProgress dexState
    ) {
        return MajestyUiBadges.decorateBattleTileName(state, displayName);
    }
}
