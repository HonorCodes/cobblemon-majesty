package codes.honor.cobblemonmajesty.client

import codes.honor.cobblemonmajesty.feature.MajestyFeature
import codes.honor.cobblemonmajesty.sound.MajestySounds
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.client.entity.PokemonClientDelegate
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import java.util.Collections
import java.util.WeakHashMap
import net.minecraft.client.Minecraft
import net.minecraft.sounds.SoundSource

object MajestyWildPresence {
    private val announcedWildEntities: MutableSet<PokemonEntity> =
        Collections.newSetFromMap(WeakHashMap())
    private val pendingSendOutSparkle: MutableSet<PokemonEntity> =
        Collections.newSetFromMap(WeakHashMap())
    private val lastBeamModeByEntity: WeakHashMap<PokemonEntity, Int> = WeakHashMap()

    @JvmStatic
    fun onClientTick(entity: PokemonEntity) {
        val cfg = MajestyClientConfig.aura
        if (!cfg.enabled.get()) return
        if (!MajestyFeature.isMajesticAspects(entity.aspects) && !MajestyFeature.isMajestic(entity.pokemon)) return

        handleSendOutSparkle(entity, cfg.sendOutSfxEnabled.get())
        handleWildNotice(entity, cfg.wildChimeEnabled.get())
    }

    private fun handleSendOutSparkle(entity: PokemonEntity, enabled: Boolean) {
        val beam = entity.beamMode
        val prevBeam = lastBeamModeByEntity.put(entity, beam) ?: 0

        if (!enabled) {
            pendingSendOutSparkle.remove(entity)
            return
        }

        if (beam == 1 && prevBeam != 1) {
            pendingSendOutSparkle.add(entity)
        } else if (beam == 3 || beam == 0 && prevBeam == 2) {
            pendingSendOutSparkle.remove(entity)
        }

        if (entity in pendingSendOutSparkle) {
            val delegate = entity.delegate as? PokemonClientDelegate
            val scale = delegate?.activeSendoutScale ?: 1.0f
            if (scale >= 0.08f && !entity.isInvisible) {
                pendingSendOutSparkle.remove(entity)
                val mc = Minecraft.getInstance()
                val player = mc.player ?: return
                if (player.position().distanceToSqr(entity.position()) <= 48.0 * 48.0) {
                    mc.level?.playLocalSound(
                        entity.x,
                        entity.y + entity.bbHeight * 0.55,
                        entity.z,
                        MajestySounds.MAJESTIC_SPARKLE.get(),
                        SoundSource.PLAYERS,
                        0.82f,
                        1.0f,
                        false
                    )
                }
            }
        }
    }

    private fun handleWildNotice(entity: PokemonEntity, enabled: Boolean) {
        if (!enabled) return
        if (entity.ownerUUID != null || entity.isSilent || entity.isBattling) return

        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        if (player.isSpectator) return

        val noticeRange = Cobblemon.config.shinyNoticeParticlesDistance.toDouble()
        val inRange = player.position().distanceToSqr(entity.position()) <= noticeRange * noticeRange
        if (inRange) {
            if (announcedWildEntities.add(entity)) {
                mc.level?.playLocalSound(
                    entity.x,
                    entity.y + entity.bbHeight * 0.5,
                    entity.z,
                    MajestySounds.WILD_PRESENCE.get(),
                    SoundSource.NEUTRAL,
                    0.80f,
                    1.0f,
                    false
                )
            }
        } else {
            announcedWildEntities.remove(entity)
        }
    }
}
