package codes.honor.cobblemonmajesty

import codes.honor.cobblemonmajesty.battle.MajestyShowdown
import codes.honor.cobblemonmajesty.client.MajestyClient
import codes.honor.cobblemonmajesty.command.MajestyCommands
import codes.honor.cobblemonmajesty.config.MajestyConfig
import codes.honor.cobblemonmajesty.feature.MajestyFeature
import codes.honor.cobblemonmajesty.item.MajestyItems
import codes.honor.cobblemonmajesty.sound.MajestySounds
import codes.honor.cobblemonmajesty.spawning.MajestySpawning
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.mojang.logging.LogUtils
import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.ModList
import net.neoforged.fml.ModLoadingContext
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.fml.loading.FMLEnvironment
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.server.ServerStoppedEvent
import org.slf4j.Logger
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

@Mod(CobblemonMajesty.ID)
object CobblemonMajesty {
    const val ID = "cobblemon_majesty"
    val LOGGER: Logger = LogUtils.getLogger()

    init {
        val container = ModLoadingContext.get().activeContainer
        container.registerConfig(ModConfig.Type.COMMON, MajestyConfig.SPEC)
        MajestySounds.register(MOD_BUS)
        MajestyItems.register(MOD_BUS)
        MOD_BUS.addListener(FMLCommonSetupEvent::class.java) { onCommonSetup(it) }
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent::class.java) { MajestyCommands.register(it.dispatcher) }
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent::class.java) { MajestyShowdown.clearAll() }

        CobblemonEvents.POKEMON_PROPERTY_INITIALISED.subscribe { MajestyFeature.registerProvider() }
        MajestySpawning.register()
        MajestyShowdown.register()

        if (FMLEnvironment.dist == Dist.CLIENT) {
            MajestyClient.init(container, MOD_BUS)
        }
    }

    private fun onCommonSetup(event: FMLCommonSetupEvent) {
        LOGGER.info("Cobblemon Majesty {} loaded beside Cobblemon {}", versionOf(ID), versionOf("cobblemon"))
        event.enqueueWork(Runnable {
            MajestyFeature.registerProvider()
        })
    }

    private fun versionOf(modId: String): String =
        ModList.get().getModContainerById(modId).map { it.modInfo.version.toString() }.orElse("absent")
}
