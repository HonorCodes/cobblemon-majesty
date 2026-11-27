package codes.honor.cobblemonmajesty.client

import codes.honor.cobblemonmajesty.config.MajestyConfig
import codes.honor.cobblemonmajesty.feature.MajestyFeature
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.client.gui.summary.Summary
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.pokemon.Pokemon
import java.util.Optional
import kotlin.math.roundToInt
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style

object MajestyUiBadges {
    private const val BADGE_COLOR = 0xFFD166
    private const val SUMMARY_SCALE = 0.5f

    @JvmStatic
    fun decorateWorldLabel(entity: PokemonEntity, baseLabel: MutableComponent): MutableComponent {
        if (!MajestyClientConfig.aura.enabled.get() || !MajestyClientConfig.aura.nameplateBadgeEnabled.get()) {
            return baseLabel
        }
        val text = baseLabel.string
        if (text.isEmpty() || text == "???") return baseLabel
        if (!MajestyFeature.isMajesticAspects(entity.aspects) && !MajestyFeature.isMajestic(entity.pokemon)) {
            return baseLabel
        }
        return prependStarBadge(baseLabel)
    }

    @JvmStatic
    fun decorateBattleTileName(state: PosableState?, baseName: MutableComponent): MutableComponent {
        if (state == null) return baseName
        if (!MajestyClientConfig.aura.enabled.get() || !MajestyClientConfig.aura.nameplateBadgeEnabled.get()) {
            return baseName
        }
        if (!MajestyFeature.isMajesticAspects(state.currentAspects)) return baseName
        if (baseName.string.startsWith("✦ ")) return baseName
        return prependStarBadge(baseName)
    }

    @JvmStatic
    fun renderSummaryBadge(
        screenWidth: Int,
        screenHeight: Int,
        pokemon: Pokemon?,
        context: GuiGraphics,
        mouseX: Int,
        mouseY: Int
    ) {
        if (pokemon == null) return
        if (!MajestyClientConfig.aura.enabled.get() || !MajestyClientConfig.aura.summaryBadgeEnabled.get()) return
        if (!MajestyFeature.isMajestic(pokemon)) return

        val baseX = (screenWidth - Summary.BASE_WIDTH) / 2
        val baseY = (screenHeight - Summary.BASE_HEIGHT) / 2
        val iconX = baseX + if (pokemon.shiny) 52.5f else 62.5f
        val iconY = baseY + 33.5f

        val matrices = context.pose()
        matrices.pushPose()
        matrices.translate(0.0f, 0.0f, 1100.0f)
        blitk(
            matrixStack = matrices,
            texture = MajestyRenderTypes.SUMMARY_BADGE_TEXTURE,
            x = iconX / SUMMARY_SCALE,
            y = iconY / SUMMARY_SCALE,
            width = 16,
            height = 16,
            scale = SUMMARY_SCALE
        )
        matrices.popPose()

        if (mouseX >= iconX.toInt() && mouseX <= (iconX + 8f).toInt() &&
            mouseY >= iconY.toInt() && mouseY <= (iconY + 8f).toInt()
        ) {
            val (rgbA, rgbB) = MajestyColorResolver.resolveForPokemon(pokemon)
            val p1Name = pokemon.primaryType.displayName.copy().withStyle(Style.EMPTY.withColor(rgbA))
            val typeLine = if (pokemon.secondaryType != null) {
                p1Name.append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                    .append(pokemon.secondaryType!!.displayName.copy().withStyle(Style.EMPTY.withColor(rgbB)))
            } else {
                p1Name
            }

            val lines = ArrayList<Component>(3)
            lines.add(
                Component.literal("✦ ")
                    .withStyle(Style.EMPTY.withColor(BADGE_COLOR))
                    .append(Component.translatable("cobblemon_majesty.summary.title").withStyle(Style.EMPTY.withColor(BADGE_COLOR).withBold(true)))
            )
            lines.add(Component.translatable("cobblemon_majesty.summary.types", typeLine).withStyle(ChatFormatting.GRAY))

            if (MajestyConfig.combat.damageBonusEnabled.get() && MajestyConfig.combat.damageMultiplier.get() > 1.0) {
                val pct = ((MajestyConfig.combat.damageMultiplier.get() - 1.0) * 100.0).roundToInt()
                lines.add(Component.translatable("cobblemon_majesty.summary.bonus", pct).withStyle(ChatFormatting.AQUA))
            } else {
                lines.add(Component.translatable("cobblemon_majesty.summary.aesthetic").withStyle(ChatFormatting.DARK_GRAY))
            }

            context.pose().pushPose()
            context.pose().translate(0.0f, 0.0f, 1200.0f)
            context.renderTooltip(Minecraft.getInstance().font, lines, Optional.empty(), mouseX, mouseY)
            context.pose().popPose()
        }
    }

    private fun prependStarBadge(component: MutableComponent): MutableComponent {
        return Component.literal("✦ ")
            .withStyle(Style.EMPTY.withColor(BADGE_COLOR).withBold(false))
            .append(component)
    }
}
