package codes.honor.cobblemonmajesty.client

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import org.joml.Matrix4f

object MajestyAuraLightRenderer {
    private const val GRID_CELLS = 6
    private const val MAX_GROUND_DROP = 3.5f
    private const val MAX_STEP_DELTA = 1.35f

    private val samplePos = BlockPos.MutableBlockPos()

    fun renderAreaLight(
        entity: PokemonEntity,
        partialTicks: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        boxWidth: Float,
        timeSeconds: Float,
        colA: FloatArray,
        colB: FloatArray,
        sendoutScale: Float
    ) {
        val cfg = MajestyClientConfig.aura
        if (!cfg.auraLightEnabled.get()) return

        val level = entity.level()
        val entPos = entity.getPosition(partialTicks)
        val ex = entPos.x
        val ey = entPos.y
        val ez = entPos.z

        val centerGroundY = findSurfaceY(level, ex, ey, ez, MAX_GROUND_DROP) ?: return
        val heightAboveGround = (ey - centerGroundY).toFloat().coerceAtLeast(0f)
        if (heightAboveGround >= MAX_GROUND_DROP) return

        val heightAtten = (1f - heightAboveGround / MAX_GROUND_DROP)
        val userScale = cfg.auraLightIntensity.get().toFloat()
        val pulse = 0.92f + 0.08f * sin(timeSeconds * 1.9f)
        val peakStrength = (0.165f * userScale * heightAtten * pulse * sendoutScale).coerceIn(0f, 0.42f)
        if (peakStrength <= 0.01f) return

        val areaRadius = (boxWidth * 2.15f + 1.15f).coerceIn(1.85f, 3.65f)
        val step = (areaRadius * 2f) / GRID_CELLS
        val rot = timeSeconds * 0.45f

        val heights = FloatArray((GRID_CELLS + 1) * (GRID_CELLS + 1)) { Float.NaN }
        for (gz in 0..GRID_CELLS) {
            val oz = -areaRadius + gz * step
            for (gx in 0..GRID_CELLS) {
                val ox = -areaRadius + gx * step
                val surfaceY = findSurfaceY(level, ex + ox, centerGroundY + 1.1, ez + oz, 2.5f)
                if (surfaceY != null && kotlin.math.abs(surfaceY - centerGroundY) <= MAX_STEP_DELTA) {
                    heights[gz * (GRID_CELLS + 1) + gx] = (surfaceY - ey).toFloat() + 0.014f
                }
            }
        }

        val consumer = bufferSource.getBuffer(MajestyRenderTypes.AREA_LIGHT)
        val matrix = poseStack.last().pose()

        for (gz in 0 until GRID_CELLS) {
            val z0 = -areaRadius + gz * step
            val z1 = z0 + step
            for (gx in 0 until GRID_CELLS) {
                val x0 = -areaRadius + gx * step
                val x1 = x0 + step

                val y00 = heights[gz * (GRID_CELLS + 1) + gx]
                val y10 = heights[gz * (GRID_CELLS + 1) + (gx + 1)]
                val y11 = heights[(gz + 1) * (GRID_CELLS + 1) + (gx + 1)]
                val y01 = heights[(gz + 1) * (GRID_CELLS + 1) + gx]

                if (y00.isNaN() || y10.isNaN() || y11.isNaN() || y01.isNaN()) continue
                val maxDiff = max(max(y00, y10), max(y11, y01)) - min(min(y00, y10), min(y11, y01))
                if (maxDiff > 0.65f) continue

                emitAreaVertex(matrix, consumer, x0, y00, z0, areaRadius, rot, colA, colB, peakStrength)
                emitAreaVertex(matrix, consumer, x0, y01, z1, areaRadius, rot, colA, colB, peakStrength)
                emitAreaVertex(matrix, consumer, x1, y11, z1, areaRadius, rot, colA, colB, peakStrength)
                emitAreaVertex(matrix, consumer, x1, y10, z0, areaRadius, rot, colA, colB, peakStrength)
            }
        }
    }

    private fun emitAreaVertex(
        matrix: Matrix4f,
        consumer: VertexConsumer,
        x: Float,
        y: Float,
        z: Float,
        areaRadius: Float,
        rot: Float,
        colA: FloatArray,
        colB: FloatArray,
        peakStrength: Float
    ) {
        val nx = (x / areaRadius).coerceIn(-1f, 1f)
        val nz = (z / areaRadius).coerceIn(-1f, 1f)
        val rNorm = sqrt(nx * nx + nz * nz).coerceIn(0f, 1f)
        val u = 0.5f + 0.5f * nx
        val v = 0.5f + 0.5f * nz

        val radialWeight = (1f - rNorm * rNorm).coerceAtLeast(0f)
        val strength = peakStrength * radialWeight

        val centerMerge = rNorm.coerceIn(0f, 1f)
        val theta = atan2(nz, nx)
        val rawBlend = 0.5f + 0.5f * cos(theta - rot)
        val smoothBlend = rawBlend * rawBlend * (3f - 2f * rawBlend)
        val blend = 0.5f + (smoothBlend - 0.5f) * centerMerge

        val r = (colA[0] + (colB[0] - colA[0]) * blend) * strength
        val g = (colA[1] + (colB[1] - colA[1]) * blend) * strength
        val b = (colA[2] + (colB[2] - colA[2]) * blend) * strength

        consumer.addVertex(matrix, x, y, z)
            .setColor(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f), 1f)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightTexture.FULL_BRIGHT)
            .setNormal(0f, 1f, 0f)
    }

    private fun findSurfaceY(level: Level, worldX: Double, startY: Double, worldZ: Double, maxDrop: Float): Double? {
        val bx = floor(worldX).toInt()
        val bz = floor(worldZ).toInt()
        val topBlockY = floor(startY + 0.35).toInt()
        val minBlockY = floor(startY - maxDrop).toInt()

        for (by in topBlockY downTo minBlockY) {
            samplePos.set(bx, by, bz)
            val state = level.getBlockState(samplePos)
            if (state.isAir) continue
            val shape = state.getCollisionShape(level, samplePos)
            if (!shape.isEmpty) {
                val maxY = shape.max(Direction.Axis.Y)
                if (maxY > 0.0) {
                    val surface = by + maxY
                    if (surface <= startY + 0.65) {
                        return surface
                    }
                }
            }
        }
        return null
    }
}
