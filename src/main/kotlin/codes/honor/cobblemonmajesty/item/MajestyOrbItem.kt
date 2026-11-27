package codes.honor.cobblemonmajesty.item

import codes.honor.cobblemonmajesty.config.MajestyConfig
import codes.honor.cobblemonmajesty.feature.MajestyFeature
import codes.honor.cobblemonmajesty.sound.MajestySounds
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.item.PokemonSelectingItem
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.item.battle.BagItem
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.isInBattle
import net.minecraft.ChatFormatting
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level

class MajestyOrbItem : Item(Properties().stacksTo(16).rarity(Rarity.EPIC)), PokemonSelectingItem {
    override val bagItem: BagItem? = null

    override fun canUseOnPokemon(stack: ItemStack, pokemon: Pokemon): Boolean {
        return pokemon.isPlayerOwned() && !MajestyFeature.isMajestic(pokemon)
    }

    override fun interactLivingEntity(
        stack: ItemStack,
        player: Player,
        interactionTarget: LivingEntity,
        usedHand: InteractionHand
    ): InteractionResult {
        val pokemonEntity = interactionTarget as? PokemonEntity ?: return InteractionResult.PASS
        if (pokemonEntity.isBattleClone()) {
            return InteractionResult.FAIL
        }

        val ownerUuid = pokemonEntity.ownerUUID ?: pokemonEntity.pokemon.getOwnerUUID()
        if (ownerUuid == null) {
            if (player is ServerPlayer) {
                player.sendSystemMessage(
                    Component.translatable("cobblemon_majesty.orb.not_captured").withStyle(ChatFormatting.RED),
                    true
                )
            }
            return InteractionResult.FAIL
        }

        if (ownerUuid != player.uuid) {
            if (player is ServerPlayer) {
                player.sendSystemMessage(
                    Component.translatable("cobblemon_majesty.orb.not_owner").withStyle(ChatFormatting.RED),
                    true
                )
            }
            return InteractionResult.FAIL
        }

        if (pokemonEntity.isBattling || (player is ServerPlayer && player.isInBattle())) {
            if (player is ServerPlayer) {
                player.sendSystemMessage(
                    Component.translatable("cobblemon_majesty.orb.in_battle").withStyle(ChatFormatting.RED),
                    true
                )
            }
            return InteractionResult.FAIL
        }

        if (MajestyFeature.isMajestic(pokemonEntity.pokemon)) {
            if (player is ServerPlayer) {
                player.sendSystemMessage(
                    Component.translatable(
                        "cobblemon_majesty.orb.already_majestic",
                        pokemonEntity.pokemon.getDisplayName()
                    ).withStyle(ChatFormatting.YELLOW),
                    true
                )
            }
            return InteractionResult.FAIL
        }

        if (player is ServerPlayer) {
            val heldStack = player.getItemInHand(usedHand)
            if (heldStack.isEmpty || !heldStack.`is`(this)) {
                return InteractionResult.FAIL
            }
            val authoritative = resolveStoredPokemon(player, pokemonEntity.pokemon)
            if (authoritative == null) {
                player.sendSystemMessage(
                    Component.translatable("cobblemon_majesty.orb.not_captured").withStyle(ChatFormatting.RED),
                    true
                )
                return InteractionResult.FAIL
            }
            if (MajestyFeature.isMajestic(authoritative)) {
                player.sendSystemMessage(
                    Component.translatable(
                        "cobblemon_majesty.orb.already_majestic",
                        authoritative.getDisplayName()
                    ).withStyle(ChatFormatting.YELLOW),
                    true
                )
                return InteractionResult.FAIL
            }
            awakenMajesty(player, heldStack, authoritative, pokemonEntity)
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide)
    }

    override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack> {
        val heldStack = player.getItemInHand(usedHand)
        if (player is ServerPlayer) {
            if (player.isInBattle()) {
                player.sendSystemMessage(
                    Component.translatable("cobblemon_majesty.orb.in_battle").withStyle(ChatFormatting.RED),
                    true
                )
                return InteractionResultHolder.fail(heldStack)
            }
            return use(player, heldStack)
        }
        return InteractionResultHolder.success(heldStack)
    }

    override fun applyToPokemon(
        player: ServerPlayer,
        stack: ItemStack,
        pokemon: Pokemon
    ): InteractionResultHolder<ItemStack> {
        if (stack.isEmpty || !stack.`is`(this)) {
            return InteractionResultHolder.fail(stack)
        }
        if (player.isInBattle() || pokemon.entity?.isBattling == true) {
            player.sendSystemMessage(
                Component.translatable("cobblemon_majesty.orb.in_battle").withStyle(ChatFormatting.RED),
                true
            )
            return InteractionResultHolder.fail(stack)
        }
        if (!pokemon.isPlayerOwned() || pokemon.getOwnerUUID() != player.uuid) {
            player.sendSystemMessage(
                Component.translatable("cobblemon_majesty.orb.not_captured").withStyle(ChatFormatting.RED),
                true
            )
            return InteractionResultHolder.fail(stack)
        }
        val authoritative = resolveStoredPokemon(player, pokemon)
        if (authoritative == null) {
            player.sendSystemMessage(
                Component.translatable("cobblemon_majesty.orb.not_captured").withStyle(ChatFormatting.RED),
                true
            )
            return InteractionResultHolder.fail(stack)
        }
        if (MajestyFeature.isMajestic(authoritative)) {
            player.sendSystemMessage(
                Component.translatable("cobblemon_majesty.orb.already_majestic", authoritative.getDisplayName())
                    .withStyle(ChatFormatting.YELLOW),
                true
            )
            return InteractionResultHolder.fail(stack)
        }
        awakenMajesty(player, stack, authoritative, pokemon.entity)
        return InteractionResultHolder.success(stack)
    }

    private fun resolveStoredPokemon(player: ServerPlayer, candidate: Pokemon): Pokemon? {
        val targetUuid = candidate.uuid
        return Cobblemon.storage.getParty(player).get(targetUuid)
            ?: Cobblemon.storage.getPC(player).get(targetUuid)
    }

    private fun awakenMajesty(
        player: ServerPlayer,
        stack: ItemStack,
        pokemon: Pokemon,
        fallbackEntity: PokemonEntity? = null
    ) {
        val bonusIvs = MajestyConfig.spawning.additionalPerfectIvs.get()
        MajestyFeature.applyMajestic(pokemon, bonusIvs)
        if (fallbackEntity != null && fallbackEntity.pokemon !== pokemon) {
            MajestyFeature.applyMajestic(fallbackEntity.pokemon, 0)
            MajestyFeature.syncEntityAspects(fallbackEntity, pokemon)
        }
        stack.consume(1, player)

        val entity = pokemon.entity ?: fallbackEntity
        val serverLevel = (entity?.level() as? ServerLevel) ?: player.serverLevel()
        val px = entity?.x ?: player.x
        val py = (entity?.y ?: player.y) + ((entity?.bbHeight ?: player.bbHeight) * 0.55)
        val pz = entity?.z ?: player.z

        serverLevel.playSound(
            null,
            px,
            py,
            pz,
            MajestySounds.MAJESTIC_SPARKLE.get(),
            SoundSource.PLAYERS,
            0.95f,
            1.0f
        )
        serverLevel.sendParticles(ParticleTypes.END_ROD, px, py, pz, 28, 0.45, 0.55, 0.45, 0.06)
        serverLevel.sendParticles(ParticleTypes.FIREWORK, px, py, pz, 18, 0.35, 0.45, 0.35, 0.08)

        player.sendSystemMessage(
            Component.translatable("cobblemon_majesty.orb.success", pokemon.getDisplayName())
                .withStyle(ChatFormatting.GOLD),
            true
        )
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        tooltipComponents.add(
            Component.translatable("item.cobblemon_majesty.majesty_orb.tooltip")
                .withStyle(ChatFormatting.GRAY)
        )
    }
}
