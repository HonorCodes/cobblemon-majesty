package codes.honor.cobblemonmajesty.client

import codes.honor.cobblemonmajesty.feature.MajestyFeature
import codes.honor.cobblemonmajesty.sound.MajestySounds
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import java.util.Collections
import java.util.WeakHashMap
import net.minecraft.client.Minecraft
import net.minecraft.sounds.SoundSource

object MajestyWildPresence {
    private val announcedEntities: MutableSet<PokemonEntity> =
        Collections.newSetFromMap(WeakHashMap())

    @JvmStatic
    fun onClientTick(entity: PokemonEntity) {
        if (!MajestyClientConfig.aura.enabled.get() || !MajestyClientConfig.aura.wildChimeEnabled.get()) return
        if (entity.ownerUUID != null || entity.isSilent || entity.isBattling) return
        if (!MajestyFeature.isMajesticAspects(entity.aspects) && !MajestyFeature.isMajestic(entity.pokemon)) return

        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        if (player.isSpectator) return

        val noticeRange = Cobblemon.config.shinyNoticeParticlesDistance.toDouble()
        val inRange = player.position().distanceToSqr(entity.position()) <= noticeRange * noticeRange
        if (inRange) {
            if (announcedEntities.add(entity)) {
                mc.level?.playLocalSound(
                    entity.x,
                    entity.y + entity.bbHeight * 0.5,
                    entity.z,
                    MajestySounds.WILD_PRESENCE.get(),
                    SoundSource.NEUTRAL,
                    0.75f,
                    1.0f,
                    false
                )
            }
        } else {
            announcedEntities.remove(entity)
        }
    }
}
