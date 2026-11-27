package codes.honor.cobblemonmajesty.command

import codes.honor.cobblemonmajesty.battle.MajestyShowdown
import codes.honor.cobblemonmajesty.config.MajestyConfig
import codes.honor.cobblemonmajesty.spawning.MajestySpawning
import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component

object MajestyCommands {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("majesty")
                .requires { it.hasPermission(2) }
                .then(
                    Commands.literal("status").executes { ctx ->
                        val spawnCfg = MajestyConfig.spawning
                        val combatCfg = MajestyConfig.combat
                        val counts = MajestySpawning.Counts
                        val msg = Component.literal(
                            String.format(
                                "Cobblemon Majesty: spawning=%s (wild 1/%d, trainer 1/%d, +%dx31 IVs) | " +
                                    "combat=%s (x%.2f) | rolls=%d/%d, majestics=%d/%d, battles=%d",
                                spawnCfg.enabled.get(),
                                spawnCfg.oneIn.get(),
                                spawnCfg.trainerOneIn.get(),
                                spawnCfg.additionalPerfectIvs.get(),
                                combatCfg.damageBonusEnabled.get(),
                                combatCfg.damageMultiplier.get(),
                                counts.naturalRolls.get(),
                                counts.trainerRolls.get(),
                                counts.naturalMajestics.get(),
                                counts.trainerMajestics.get(),
                                MajestyShowdown.registeredBattlesCount.get()
                            )
                        )
                        ctx.source.sendSuccess({ msg }, false)
                        1
                    }
                )
        )
    }
}
