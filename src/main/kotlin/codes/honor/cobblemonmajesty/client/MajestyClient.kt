package codes.honor.cobblemonmajesty.client

import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.event.config.ModConfigEvent
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import net.neoforged.neoforge.common.NeoForge

object MajestyClient {
    fun init(container: ModContainer, modBus: IEventBus) {
        container.registerConfig(ModConfig.Type.CLIENT, MajestyClientConfig.SPEC)
        modBus.addListener(ModConfigEvent.Loading::class.java) { onConfig(it.config) }
        modBus.addListener(ModConfigEvent.Reloading::class.java) { onConfig(it.config) }
        modBus.addListener(RegisterClientReloadListenersEvent::class.java) { event ->
            event.registerReloadListener(ResourceManagerReloadListener { MajestyEyeGlowRenderer.clearCache() })
        }
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent::class.java) { event ->
            if (event.stage == RenderLevelStageEvent.Stage.AFTER_SKY) {
                MajestyAuraRenderer.beginFrame()
            }
        }
    }

    private fun onConfig(config: ModConfig) {
        if (config.spec === MajestyClientConfig.SPEC) {
            MajestyClientConfig.reloadPalette()
        }
    }
}
