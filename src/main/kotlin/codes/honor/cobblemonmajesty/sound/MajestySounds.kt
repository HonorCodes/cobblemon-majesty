package codes.honor.cobblemonmajesty.sound

import codes.honor.cobblemonmajesty.CobblemonMajesty
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

object MajestySounds {
    val SOUND_EVENTS: DeferredRegister<SoundEvent> =
        DeferredRegister.create(Registries.SOUND_EVENT, CobblemonMajesty.ID)

    val WILD_PRESENCE_ID: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(CobblemonMajesty.ID, "majestic_presence")

    val MAJESTIC_SPARKLE_ID: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(CobblemonMajesty.ID, "majestic_sparkle")

    val WILD_PRESENCE: DeferredHolder<SoundEvent, SoundEvent> =
        SOUND_EVENTS.register("majestic_presence") { ->
            SoundEvent.createVariableRangeEvent(WILD_PRESENCE_ID)
        }

    val MAJESTIC_SPARKLE: DeferredHolder<SoundEvent, SoundEvent> =
        SOUND_EVENTS.register("majestic_sparkle") { ->
            SoundEvent.createVariableRangeEvent(MAJESTIC_SPARKLE_ID)
        }

    fun register(modBus: IEventBus) {
        SOUND_EVENTS.register(modBus)
    }
}
