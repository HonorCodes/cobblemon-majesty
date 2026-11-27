package codes.honor.cobblemonmajesty.client

import codes.honor.cobblemonmajesty.feature.MajestyFeature
import codes.honor.cobblemonmajesty.palette.TypeColorPair
import com.cobblemon.mod.common.client.entity.PokemonClientDelegate
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.texture.OverlayTexture
import org.joml.Matrix4f
import org.joml.Vector3f

object MajestyAuraRenderer {
    private const val WISP_STRANDS = 12
    private const val WISP_FRAMES = 8
    private const val STAR_MOTES = 8
    private const val GOLDEN_ANGLE = 2.3999632f

    private val currentFrameDistances = ArrayList<Double>(32)
    private var distanceCutoffSq: Double = Double.MAX_VALUE
    private var renderedFullThisFrame: Int = 0

    fun beginFrame() {
        val cap = MajestyClientConfig.aura.maxFullAurasOnScreen.get()
        if (currentFrameDistances.size > cap) {
            currentFrameDistances.sort()
            distanceCutoffSq = currentFrameDistances[cap - 1]
        } else {
            distanceCutoffSq = Double.MAX_VALUE
        }
        currentFrameDistances.clear()
        renderedFullThisFrame = 0
    }

    @JvmStatic
    fun renderWorldAura(
        entity: PokemonEntity,
        partialTicks: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource
    ) {
        val cfg = MajestyClientConfig.aura
        if (!cfg.enabled.get()) return
        if (entity.isInvisible) return
        if (!MajestyFeature.isMajesticAspects(entity.aspects) && !MajestyFeature.isMajestic(entity.pokemon)) return

        val delegate = entity.delegate as? PokemonClientDelegate
        val sendoutScale = delegate?.activeSendoutScale ?: 1.0f
        if (sendoutScale <= 0.02f) return

        val mc = Minecraft.getInstance()
        val camera = mc.gameRenderer.mainCamera
        val distSq = camera.position.distanceToSqr(entity.getPosition(partialTicks))
        val maxDist = cfg.fullAuraDistance.get()
        if (distSq > maxDist * maxDist) return

        currentFrameDistances.add(distSq)
        if (distSq > distanceCutoffSq || renderedFullThisFrame >= cfg.maxFullAurasOnScreen.get()) return
        renderedFullThisFrame++

        val (rgbA, rgbB) = MajestyColorResolver.resolveForEntity(entity)
        val colA = TypeColorPair.rgbToFloats(rgbA)
        val colB = TypeColorPair.rgbToFloats(rgbB)

        val boxWidth = max(0.35f, entity.boundingBox.xsize.toFloat()) * sendoutScale
        val boxHeight = max(0.45f, entity.boundingBox.ysize.toFloat()) * sendoutScale
        val radius = boxWidth * 0.58f + 0.08f
        val timeSeconds = (entity.tickCount + partialTicks + (entity.id % 97) * 3.7f) / 20.0f

        val camLeft = Vector3f(camera.leftVector).normalize()
        val camUp = Vector3f(camera.upVector).normalize()

        poseStack.pushPose()
        val matrix = poseStack.last().pose()

        if (cfg.groundHaloEnabled.get() && entity.onGround()) {
            renderGroundHalo(matrix, bufferSource.getBuffer(MajestyRenderTypes.GROUND_HALO), radius * 1.45f, timeSeconds, colA, colB, sendoutScale)
        }

        renderWispStrands(
            matrix = matrix,
            consumer = bufferSource.getBuffer(MajestyRenderTypes.AURA_WISP),
            radius = radius,
            height = boxHeight,
            timeSeconds = timeSeconds,
            camLeft = camLeft,
            colA = colA,
            colB = colB,
            alphaScale = sendoutScale
        )

        if (cfg.starMotesEnabled.get()) {
            renderStarMotes(
                matrix = matrix,
                consumer = bufferSource.getBuffer(MajestyRenderTypes.STAR_MOTE),
                radius = radius * 1.12f,
                height = boxHeight,
                timeSeconds = timeSeconds,
                camLeft = camLeft,
                camUp = camUp,
                colA = colA,
                colB = colB,
                alphaScale = sendoutScale
            )
        }

        poseStack.popPose()
    }

    private fun renderGroundHalo(
        matrix: Matrix4f,
        consumer: VertexConsumer,
        haloRadius: Float,
        timeSeconds: Float,
        colA: FloatArray,
        colB: FloatArray,
        alphaScale: Float
    ) {
        val rot = timeSeconds * 0.65f
        val cosR = cos(rot) * haloRadius
        val sinR = sin(rot) * haloRadius
        val y = 0.025f
        val pulse = 0.34f + 0.08f * sin(timeSeconds * 2.4f)
        val alpha = (pulse * alphaScale).coerceIn(0f, 1f)

        val x0 = -cosR - sinR
        val z0 = -sinR + cosR
        val x1 = -sinR
        val z1 = cosR
        val x2 = sinR
        val z2 = -cosR
        val x3 = -cosR + sinR
        val z3 = -sinR - cosR

        emitVertex(matrix, consumer, x0, y, z0, 0f, 0f, colA[0], colA[1], colA[2], alpha)
        emitVertex(matrix, consumer, x3, y, z3, 0f, 1f, colA[0], colA[1], colA[2], alpha)
        emitVertex(matrix, consumer, x2, y, z2, 0.5f, 1f, colA[0], colA[1], colA[2], alpha)
        emitVertex(matrix, consumer, x1, y, z1, 0.5f, 0f, colA[0], colA[1], colA[2], alpha)

        val rx0 = cosR - sinR
        val rz0 = sinR + cosR
        val rx3 = cosR + sinR
        val rz3 = sinR - cosR

        emitVertex(matrix, consumer, x1, y, z1, 0.5f, 0f, colB[0], colB[1], colB[2], alpha)
        emitVertex(matrix, consumer, x2, y, z2, 0.5f, 1f, colB[0], colB[1], colB[2], alpha)
        emitVertex(matrix, consumer, rx3, y, rz3, 1f, 1f, colB[0], colB[1], colB[2], alpha)
        emitVertex(matrix, consumer, rx0, y, rz0, 1f, 0f, colB[0], colB[1], colB[2], alpha)
    }

    private fun renderWispStrands(
        matrix: Matrix4f,
        consumer: VertexConsumer,
        radius: Float,
        height: Float,
        timeSeconds: Float,
        camLeft: Vector3f,
        colA: FloatArray,
        colB: FloatArray,
        alphaScale: Float
    ) {
        val wispHalfWidth = min(0.45f, max(0.11f, radius * 0.34f))
        val wispHeight = min(1.35f, max(0.32f, height * 0.46f))

        for (i in 0 until WISP_STRANDS) {
            val col = if (i % 2 == 0) colA else colB
            val cycleSpeed = 0.62f + (i % 3) * 0.14f
            val cycle = (timeSeconds * cycleSpeed + i * (1.0f / WISP_STRANDS)) % 1.0f
            val baseAngle = i * GOLDEN_ANGLE + timeSeconds * (if (i % 2 == 0) 0.45f else -0.45f)
            val sway = sin(timeSeconds * 3.1f + i * 1.7f) * (radius * 0.14f)
            val radial = radius * (0.82f + 0.24f * sin(cycle * PI.toFloat())) + sway

            val cx = cos(baseAngle) * radial
            val cz = sin(baseAngle) * radial
            val bottomY = cycle * (height * 0.72f) + 0.02f
            val topY = bottomY + wispHeight

            val lifeFade = sin(cycle * PI.toFloat())
            val alpha = (lifeFade * 0.86f * alphaScale).coerceIn(0f, 1f)
            if (alpha <= 0.04f) continue

            val frameIndex = ((timeSeconds * 10f + i * 2).toInt().and(0x7FFFFFFF)) % WISP_FRAMES
            val v0 = frameIndex.toFloat() / WISP_FRAMES
            val v1 = (frameIndex + 1).toFloat() / WISP_FRAMES

            val dx = camLeft.x * wispHalfWidth
            val dz = camLeft.z * wispHalfWidth
            val topSwayX = camLeft.x * (sin(timeSeconds * 4.0f + i) * wispHalfWidth * 0.35f)
            val topSwayZ = camLeft.z * (sin(timeSeconds * 4.0f + i) * wispHalfWidth * 0.35f)

            emitVertex(matrix, consumer, cx - dx + topSwayX, topY, cz - dz + topSwayZ, 0f, v0, col[0], col[1], col[2], alpha)
            emitVertex(matrix, consumer, cx - dx, bottomY, cz - dz, 0f, v1, col[0], col[1], col[2], alpha)
            emitVertex(matrix, consumer, cx + dx, bottomY, cz + dz, 1f, v1, col[0], col[1], col[2], alpha)
            emitVertex(matrix, consumer, cx + dx + topSwayX, topY, cz + dz + topSwayZ, 1f, v0, col[0], col[1], col[2], alpha)
        }
    }

    private fun renderStarMotes(
        matrix: Matrix4f,
        consumer: VertexConsumer,
        radius: Float,
        height: Float,
        timeSeconds: Float,
        camLeft: Vector3f,
        camUp: Vector3f,
        colA: FloatArray,
        colB: FloatArray,
        alphaScale: Float
    ) {
        val baseSize = min(0.16f, max(0.055f, radius * 0.13f))
        for (i in 0 until STAR_MOTES) {
            val col = if (i % 2 == 0) colA else colB
            val progress = (timeSeconds * (0.42f + (i % 3) * 0.09f) + i * 0.125f) % 1.0f
            val twinkle = sin(progress * PI.toFloat()) * (0.65f + 0.35f * sin(timeSeconds * 7.5f + i * 2.3f))
            val alpha = (twinkle * 0.92f * alphaScale).coerceIn(0f, 1f)
            if (alpha <= 0.08f) continue

            val angle = i * (2.0f * PI.toFloat() / STAR_MOTES) + timeSeconds * 0.55f
            val r = radius * (0.85f + 0.22f * sin(progress * PI.toFloat()))
            val cx = cos(angle) * r
            val cy = 0.12f + progress * (height * 1.05f)
            val cz = sin(angle) * r
            val size = baseSize * (0.7f + 0.45f * sin(progress * PI.toFloat()))

            val lx = camLeft.x * size
            val ly = camLeft.y * size
            val lz = camLeft.z * size
            val ux = camUp.x * size
            val uy = camUp.y * size
            val uz = camUp.z * size

            emitVertex(matrix, consumer, cx - lx + ux, cy - ly + uy, cz - lz + uz, 0f, 0f, col[0], col[1], col[2], alpha)
            emitVertex(matrix, consumer, cx - lx - ux, cy - ly - uy, cz - lz - uz, 0f, 1f, col[0], col[1], col[2], alpha)
            emitVertex(matrix, consumer, cx + lx - ux, cy + ly - uy, cz + lz - uz, 1f, 1f, col[0], col[1], col[2], alpha)
            emitVertex(matrix, consumer, cx + lx + ux, cy + ly + uy, cz + lz + uz, 1f, 0f, col[0], col[1], col[2], alpha)
        }
    }

    private fun emitVertex(
        matrix: Matrix4f,
        consumer: VertexConsumer,
        x: Float,
        y: Float,
        z: Float,
        u: Float,
        v: Float,
        r: Float,
        g: Float,
        b: Float,
        a: Float
    ) {
        consumer.addVertex(matrix, x, y, z)
            .setColor(r, g, b, a)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightTexture.FULL_BRIGHT)
            .setNormal(0f, 1f, 0f)
    }
}
