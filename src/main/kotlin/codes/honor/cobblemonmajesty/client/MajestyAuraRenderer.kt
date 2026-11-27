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
import kotlin.math.pow
import kotlin.math.sin
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.texture.OverlayTexture
import org.joml.Matrix4f
import org.joml.Vector3f

object MajestyAuraRenderer {
    private const val BASE_WISP_STRANDS = 6
    private const val WISP_FRAMES = 8
    private const val COMET_EMBERS = 12
    private const val STAR_MOTES = 6
    private const val HALO_SEGMENTS = 28
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
        MajestyEyeGlowRenderer.beginFrame()
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
        val entPos = entity.getPosition(partialTicks)
        val distSq = camera.position.distanceToSqr(entPos)
        val maxDist = cfg.fullAuraDistance.get()
        if (distSq > maxDist * maxDist) return

        currentFrameDistances.add(distSq)
        if (distSq > distanceCutoffSq || renderedFullThisFrame >= cfg.maxFullAurasOnScreen.get()) return
        renderedFullThisFrame++

        val (rgbA, rgbB) = MajestyColorResolver.resolveForEntity(entity)
        val colA = TypeColorPair.rgbToFloats(rgbA)
        val colB = TypeColorPair.rgbToFloats(rgbB)

        MajestyEyeGlowRenderer.renderLocatorFallbackIfNeeded(
            entity = entity,
            partialTicks = partialTicks,
            poseStack = poseStack,
            bufferSource = bufferSource,
            colA = colA,
            colB = colB,
            sendoutScale = sendoutScale
        )

        val boxWidth = max(0.35f, entity.boundingBox.xsize.toFloat()) * sendoutScale
        val boxHeight = max(0.45f, entity.boundingBox.ysize.toFloat()) * sendoutScale
        val radius = boxWidth * 0.58f + 0.08f
        val timeSeconds = (entity.tickCount + partialTicks + (entity.id % 97) * 3.7f) / 20.0f

        MajestyAuraLightRenderer.renderAreaLight(
            entity = entity,
            partialTicks = partialTicks,
            poseStack = poseStack,
            bufferSource = bufferSource,
            boxWidth = boxWidth,
            timeSeconds = timeSeconds,
            colA = colA,
            colB = colB,
            sendoutScale = sendoutScale
        )

        val camLeft = Vector3f(camera.leftVector).normalize()
        val camUp = Vector3f(camera.upVector).normalize()
        val toCamX = (camera.position.x - entPos.x).toFloat()
        val toCamZ = (camera.position.z - entPos.z).toFloat()
        val toCamHorizLen = kotlin.math.sqrt(toCamX * toCamX + toCamZ * toCamZ).coerceAtLeast(1e-4f)
        val toCamDirX = toCamX / toCamHorizLen
        val toCamDirZ = toCamZ / toCamHorizLen

        poseStack.pushPose()
        val matrix = poseStack.last().pose()

        if (cfg.groundHaloEnabled.get() && entity.onGround()) {
            renderSteadyMergedGroundHalo(
                matrix = matrix,
                consumer = bufferSource.getBuffer(MajestyRenderTypes.GROUND_HALO),
                haloRadius = radius * 1.52f,
                timeSeconds = timeSeconds,
                colA = colA,
                colB = colB,
                alphaScale = sendoutScale
            )
        }

        val emitShaderLight = cfg.auraLightEnabled.get()

        for (strandParity in 0..1) {
            val strandRgb = if (strandParity == 0) rgbA else rgbB
            val strandCol = if (strandParity == 0) colA else colB
            IrisShaderBridge.withColoredEntity(strandRgb, emitShaderLight) {
                renderBaseWisps(
                    matrix = matrix,
                    consumer = bufferSource.getBuffer(MajestyRenderTypes.AURA_WISP),
                    strandParity = strandParity,
                    radius = radius,
                    height = boxHeight,
                    timeSeconds = timeSeconds,
                    camLeft = camLeft,
                    toCamDirX = toCamDirX,
                    toCamDirZ = toCamDirZ,
                    col = strandCol,
                    alphaScale = sendoutScale
                )

                if (cfg.starMotesEnabled.get()) {
                    renderCometEmbers(
                        matrix = matrix,
                        consumer = bufferSource.getBuffer(MajestyRenderTypes.COMET_EMBER),
                        strandParity = strandParity,
                        radius = radius,
                        height = boxHeight,
                        timeSeconds = timeSeconds,
                        camLeft = camLeft,
                        camUp = camUp,
                        col = strandCol,
                        alphaScale = sendoutScale
                    )

                    renderStarMotes(
                        matrix = matrix,
                        consumer = bufferSource.getBuffer(MajestyRenderTypes.STAR_MOTE),
                        strandParity = strandParity,
                        radius = radius * 1.14f,
                        height = boxHeight,
                        timeSeconds = timeSeconds,
                        camLeft = camLeft,
                        camUp = camUp,
                        col = strandCol,
                        alphaScale = sendoutScale
                    )
                }
            }
        }

        poseStack.popPose()
    }

    private fun renderSteadyMergedGroundHalo(
        matrix: Matrix4f,
        consumer: VertexConsumer,
        haloRadius: Float,
        timeSeconds: Float,
        colA: FloatArray,
        colB: FloatArray,
        alphaScale: Float
    ) {
        val rot = timeSeconds * 0.45f
        val y = 0.024f
        val pulse = 0.92f + 0.08f * sin(timeSeconds * 2.1f)
        val innerAlpha = (0.14f * pulse * alphaScale).coerceIn(0f, 1f)
        val midAlpha = (0.20f * pulse * alphaScale).coerceIn(0f, 1f)

        val midR = (colA[0] + colB[0]) * 0.5f
        val midG = (colA[1] + colB[1]) * 0.5f
        val midB = (colA[2] + colB[2]) * 0.5f

        val rMidFrac = 0.54f
        val stepAngle = (2.0 * PI / HALO_SEGMENTS).toFloat()

        for (i in 0 until HALO_SEGMENTS) {
            val a0 = i * stepAngle
            val a1 = (i + 1) * stepAngle

            val cos0 = cos(a0)
            val sin0 = sin(a0)
            val cos1 = cos(a1)
            val sin1 = sin(a1)

            val b0 = angularSmoothBlend(a0 - rot)
            val b1 = angularSmoothBlend(a1 - rot)

            val r0 = colA[0] + (colB[0] - colA[0]) * b0
            val g0 = colA[1] + (colB[1] - colA[1]) * b0
            val bl0 = colA[2] + (colB[2] - colA[2]) * b0

            val r1 = colA[0] + (colB[0] - colA[0]) * b1
            val g1 = colA[1] + (colB[1] - colA[1]) * b1
            val bl1 = colA[2] + (colB[2] - colA[2]) * b1

            val xm0 = cos0 * haloRadius * rMidFrac
            val zm0 = sin0 * haloRadius * rMidFrac
            val xm1 = cos1 * haloRadius * rMidFrac
            val zm1 = sin1 * haloRadius * rMidFrac

            val um0 = 0.5f + 0.5f * cos0 * rMidFrac
            val vm0 = 0.5f + 0.5f * sin0 * rMidFrac
            val um1 = 0.5f + 0.5f * cos1 * rMidFrac
            val vm1 = 0.5f + 0.5f * sin1 * rMidFrac

            emitVertex(matrix, consumer, 0f, y, 0f, 0.5f, 0.5f, midR, midG, midB, innerAlpha)
            emitVertex(matrix, consumer, 0f, y, 0f, 0.5f, 0.5f, midR, midG, midB, innerAlpha)
            emitVertex(matrix, consumer, xm1, y, zm1, um1, vm1, r1, g1, bl1, midAlpha)
            emitVertex(matrix, consumer, xm0, y, zm0, um0, vm0, r0, g0, bl0, midAlpha)

            val xo0 = cos0 * haloRadius
            val zo0 = sin0 * haloRadius
            val xo1 = cos1 * haloRadius
            val zo1 = sin1 * haloRadius

            val uo0 = 0.5f + 0.5f * cos0
            val vo0 = 0.5f + 0.5f * sin0
            val uo1 = 0.5f + 0.5f * cos1
            val vo1 = 0.5f + 0.5f * sin1

            emitVertex(matrix, consumer, xm0, y, zm0, um0, vm0, r0, g0, bl0, midAlpha)
            emitVertex(matrix, consumer, xm1, y, zm1, um1, vm1, r1, g1, bl1, midAlpha)
            emitVertex(matrix, consumer, xo1, y, zo1, uo1, vo1, r1, g1, bl1, 0f)
            emitVertex(matrix, consumer, xo0, y, zo0, uo0, vo0, r0, g0, bl0, 0f)
        }
    }

    private fun angularSmoothBlend(deltaAngle: Float): Float {
        val raw = 0.5f + 0.5f * cos(deltaAngle)
        return raw * raw * (3f - 2f * raw)
    }

    private fun renderBaseWisps(
        matrix: Matrix4f,
        consumer: VertexConsumer,
        strandParity: Int,
        radius: Float,
        height: Float,
        timeSeconds: Float,
        camLeft: Vector3f,
        toCamDirX: Float,
        toCamDirZ: Float,
        col: FloatArray,
        alphaScale: Float
    ) {
        val wispHalfWidth = min(0.28f, max(0.085f, radius * 0.24f))
        val wispHeight = min(0.72f, max(0.22f, height * 0.28f))

        for (i in strandParity until BASE_WISP_STRANDS step 2) {
            val cycleSpeed = 0.48f + (i % 3) * 0.08f
            val cycle = (timeSeconds * cycleSpeed + i * (1.0f / BASE_WISP_STRANDS)) % 1.0f
            val spin = if (i % 2 == 0) 0.36f else -0.36f
            val angle = i * (2.0f * PI.toFloat() / BASE_WISP_STRANDS) + timeSeconds * spin
            val dirX = cos(angle)
            val dirZ = sin(angle)

            val frontDot = (dirX * toCamDirX + dirZ * toCamDirZ).coerceAtLeast(0f)
            val rimFactor = 1.0f - 0.68f * frontDot * frontDot

            val radial = radius * (0.94f + 0.12f * sin(cycle * PI.toFloat()))
            val cx = dirX * radial
            val cz = dirZ * radial
            val bottomY = cycle * (height * 0.24f) + 0.02f
            val topY = bottomY + wispHeight

            val lifeFade = sin(cycle * PI.toFloat())
            val alpha = (lifeFade * 0.76f * rimFactor * alphaScale).coerceIn(0f, 1f)
            if (alpha <= 0.03f) continue

            val frameIndex = ((timeSeconds * 6.5f + i * 2).toInt().and(0x7FFFFFFF)) % WISP_FRAMES
            val v0 = frameIndex.toFloat() / WISP_FRAMES
            val v1 = (frameIndex + 1).toFloat() / WISP_FRAMES

            val dx = camLeft.x * wispHalfWidth
            val dz = camLeft.z * wispHalfWidth
            val swayAmt = sin(timeSeconds * 2.8f + i * 1.5f) * wispHalfWidth * 0.28f
            val topSwayX = camLeft.x * swayAmt
            val topSwayZ = camLeft.z * swayAmt

            emitVertex(matrix, consumer, cx - dx + topSwayX, topY, cz - dz + topSwayZ, 0f, v0, col[0], col[1], col[2], alpha)
            emitVertex(matrix, consumer, cx - dx, bottomY, cz - dz, 0f, v1, col[0], col[1], col[2], alpha)
            emitVertex(matrix, consumer, cx + dx, bottomY, cz + dz, 1f, v1, col[0], col[1], col[2], alpha)
            emitVertex(matrix, consumer, cx + dx + topSwayX, topY, cz + dz + topSwayZ, 1f, v0, col[0], col[1], col[2], alpha)
        }
    }

    private fun renderCometEmbers(
        matrix: Matrix4f,
        consumer: VertexConsumer,
        strandParity: Int,
        radius: Float,
        height: Float,
        timeSeconds: Float,
        camLeft: Vector3f,
        camUp: Vector3f,
        col: FloatArray,
        alphaScale: Float
    ) {
        val baseHalfW = min(0.095f, max(0.038f, radius * 0.085f))
        val baseHalfH = baseHalfW * 2.1f

        for (i in strandParity until COMET_EMBERS step 2) {
            val speed = 0.36f + (i % 4) * 0.07f
            val progress = (timeSeconds * speed + i * (1.0f / COMET_EMBERS)) % 1.0f
            val life = sin(progress * PI.toFloat()) * (0.76f + 0.24f * sin(timeSeconds * 5.2f + i * 1.7f))
            val alpha = (life * 0.90f * alphaScale).coerceIn(0f, 1f)
            if (alpha <= 0.05f) continue

            val curl = sin(progress * 2.0f * PI.toFloat() + i) * 0.32f
            val spread = 0.86f + 0.32f * (((i * 7) % 5) / 4.0f)
            val angle = i * GOLDEN_ANGLE + timeSeconds * 0.32f + curl
            val r = radius * spread

            val cx = cos(angle) * r
            val cy = 0.08f + progress.pow(0.92f) * (height * 1.08f)
            val cz = sin(angle) * r

            val scale = 0.78f + 0.35f * sin(progress * PI.toFloat())
            val hw = baseHalfW * scale
            val hh = baseHalfH * scale

            val lx = camLeft.x * hw
            val ly = camLeft.y * hw
            val lz = camLeft.z * hw
            val ux = camUp.x * hh
            val uy = camUp.y * hh
            val uz = camUp.z * hh

            emitVertex(matrix, consumer, cx - lx + ux, cy - ly + uy, cz - lz + uz, 0f, 0f, col[0], col[1], col[2], alpha)
            emitVertex(matrix, consumer, cx - lx - ux, cy - ly - uy, cz - lz - uz, 0f, 1f, col[0], col[1], col[2], alpha)
            emitVertex(matrix, consumer, cx + lx - ux, cy + ly - uy, cz + lz - uz, 1f, 1f, col[0], col[1], col[2], alpha)
            emitVertex(matrix, consumer, cx + lx + ux, cy + ly + uy, cz + lz + uz, 1f, 0f, col[0], col[1], col[2], alpha)
        }
    }

    private fun renderStarMotes(
        matrix: Matrix4f,
        consumer: VertexConsumer,
        strandParity: Int,
        radius: Float,
        height: Float,
        timeSeconds: Float,
        camLeft: Vector3f,
        camUp: Vector3f,
        col: FloatArray,
        alphaScale: Float
    ) {
        val baseSize = min(0.145f, max(0.052f, radius * 0.12f))
        for (i in strandParity until STAR_MOTES step 2) {
            val progress = (timeSeconds * (0.38f + (i % 3) * 0.08f) + i * (1.0f / STAR_MOTES)) % 1.0f
            val twinkle = sin(progress * PI.toFloat()) * (0.68f + 0.32f * sin(timeSeconds * 6.8f + i * 2.3f))
            val alpha = (twinkle * 0.94f * alphaScale).coerceIn(0f, 1f)
            if (alpha <= 0.07f) continue

            val angle = i * (2.0f * PI.toFloat() / STAR_MOTES) + timeSeconds * 0.45f
            val r = radius * (0.88f + 0.22f * sin(progress * PI.toFloat()))
            val cx = cos(angle) * r
            val cy = 0.12f + progress * (height * 1.08f)
            val cz = sin(angle) * r
            val size = baseSize * (0.72f + 0.42f * sin(progress * PI.toFloat()))

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
