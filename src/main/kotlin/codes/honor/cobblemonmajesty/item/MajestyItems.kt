package codes.honor.cobblemonmajesty.item

import codes.honor.cobblemonmajesty.CobblemonMajesty
import java.util.function.Supplier
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredItem
import net.neoforged.neoforge.registries.DeferredRegister

object MajestyItems {
    val ITEMS: DeferredRegister.Items = DeferredRegister.createItems(CobblemonMajesty.ID)
    val TABS: DeferredRegister<CreativeModeTab> =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CobblemonMajesty.ID)

    val MAJESTY_ORB: DeferredItem<MajestyOrbItem> =
        ITEMS.register("majesty_orb", Supplier { MajestyOrbItem() })

    val TAB: DeferredHolder<CreativeModeTab, CreativeModeTab> =
        TABS.register("cobblemon_majesty", Supplier {
            CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.cobblemon_majesty"))
                .icon { ItemStack(MAJESTY_ORB.get()) }
                .displayItems { _, output -> output.accept(MAJESTY_ORB.get()) }
                .build()
        })

    fun register(modBus: IEventBus) {
        ITEMS.register(modBus)
        TABS.register(modBus)
        modBus.addListener(BuildCreativeModeTabContentsEvent::class.java) { event ->
            if (event.tabKey == CreativeModeTabs.TOOLS_AND_UTILITIES || event.tabKey == CreativeModeTabs.INGREDIENTS) {
                event.accept(MAJESTY_ORB.get())
            }
        }
    }
}
