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
import kotlin.math.sqrt
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.texture.OverlayTexture
import org.joml.Matrix4f
import org.joml.Vector3f

object MajestyAuraRenderer {
    private const val BASE_WISP_STRANDS = 6
    private const val WISP_FRAMES = 8
    private const val GROUND_FILAMENTS = 4
    private const val FILAMENT_SEGMENTS = 14
    private const val GROUND_SPARKS = 10
    private const val COMET_EMBERS = 12
    private const val STAR_MOTES = 6
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
        val camRelX = (camera.position.x - entPos.x).toFloat()
        val camRelY = (camera.position.y - entPos.y).toFloat()
        val camRelZ = (camera.position.z - entPos.z).toFloat()
        val toCamHorizLen = sqrt(camRelX * camRelX + camRelZ * camRelZ).coerceAtLeast(1e-4f)
        val toCamDirX = camRelX / toCamHorizLen
        val toCamDirZ = camRelZ / toCamHorizLen

        poseStack.pushPose()
        val matrix = poseStack.last().pose()

        val emitShaderLight = cfg.auraLightEnabled.get()
        val drawGroundParticles = cfg.groundParticlesEnabled.get()
        val drawUpperMotes = cfg.starMotesEnabled.get()

        for (strandParity in 0..1) {
            val strandRgb = if (strandParity == 0) rgbA else rgbB
            val strandCol = if (strandParity == 0) colA else colB
            IrisShaderBridge.withColoredEntity(strandRgb, emitShaderLight) {
                if (drawGroundParticles) {
                    renderGroundFilaments(
                        matrix = matrix,
                        consumer = bufferSource.getBuffer(MajestyRenderTypes.GROUND_FILAMENT),
                        strandParity = strandParity,
                        radius = radius,
                        height = boxHeight,
                        timeSeconds = timeSeconds,
                        camRelX = camRelX,
                        camRelY = camRelY,
                        camRelZ = camRelZ,
                        col = strandCol,
                        alphaScale = sendoutScale
                    )

                    renderGroundSparks(
                        matrix = matrix,
                        consumer = bufferSource.getBuffer(MajestyRenderTypes.SPARK_DOT),
                        strandParity = strandParity,
                        radius = radius,
                        height = boxHeight,
                        timeSeconds = timeSeconds,
                        camLeft = camLeft,
                        camUp = camUp,
                        col = strandCol,
                        alphaScale = sendoutScale
                    )
                }

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

                if (drawUpperMotes) {
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

    private fun renderGroundFilaments(
        matrix: Matrix4f,
        consumer: VertexConsumer,
        strandParity: Int,
        radius: Float,
        height: Float,
        timeSeconds: Float,
        camRelX: Float,
        camRelY: Float,
        camRelZ: Float,
        col: FloatArray,
        alphaScale: Float
    ) {
        val maxHalfWidth = min(0.042f, max(0.018f, radius * 0.042f))
        val sweepHeight = min(0.68f, max(0.20f, height * 0.30f))
        val strandSpan = 0.48f

        val ptsX = FloatArray(FILAMENT_SEGMENTS + 1)
        val ptsY = FloatArray(FILAMENT_SEGMENTS + 1)
        val ptsZ = FloatArray(FILAMENT_SEGMENTS + 1)
        val ptsW = FloatArray(FILAMENT_SEGMENTS + 1)
        val ptsA = FloatArray(FILAMENT_SEGMENTS + 1)

        for (fIdx in strandParity until GROUND_FILAMENTS step 2) {
            val speed = 0.44f + (fIdx % 2) * 0.08f
            val headU = (timeSeconds * speed + fIdx * (1.0f / GROUND_FILAMENTS)) % 1.0f
            val spinDir = if (fIdx % 2 == 0) 0.55f else -0.55f
            val baseTheta = fIdx * (2.0f * PI.toFloat() / GROUND_FILAMENTS) - timeSeconds * spinDir

            for (s in 0..FILAMENT_SEGMENTS) {
                val frac = s.toFloat() / FILAMENT_SEGMENTS
                val rawU = headU - frac * strandSpan
                val wrappedU = ((rawU % 1.0f) + 1.0f) % 1.0f
                val alongEnv = sin(frac * PI.toFloat())
                val heightEnv = sin((wrappedU * 0.92f + 0.08f).coerceAtMost(1.0f) * PI.toFloat()).pow(0.65f)
                val alpha = (alongEnv * heightEnv * 0.88f * alphaScale).coerceIn(0f, 1f)

                val radialSpread = radius * (0.92f + 0.28f * sin(wrappedU * PI.toFloat()))
                val theta = baseTheta + rawU * (2.0f * PI.toFloat() * 0.85f)
                ptsX[s] = cos(theta) * radialSpread
                ptsY[s] = 0.03f + wrappedU * sweepHeight
                ptsZ[s] = sin(theta) * radialSpread
                ptsW[s] = maxHalfWidth * (0.25f + 0.75f * alongEnv)
                ptsA[s] = alpha
            }

            for (s in 0 until FILAMENT_SEGMENTS) {
                val a0 = ptsA[s]
                val a1 = ptsA[s + 1]
                if (a0 <= 0.02f && a1 <= 0.02f) continue

                val dx = ptsX[s + 1] - ptsX[s]
                val dy = ptsY[s + 1] - ptsY[s]
                val dz = ptsZ[s + 1] - ptsZ[s]
                if (dx * dx + dy * dy + dz * dz > radius * radius * 1.5f) continue

                val vx0 = camRelX - ptsX[s]
                val vy0 = camRelY - ptsY[s]
                val vz0 = camRelZ - ptsZ[s]

                var px = dy * vz0 - dz * vy0
                var py = dz * vx0 - dx * vz0
                var pz = dx * vy0 - dy * vx0
                val pLen = sqrt(px * px + py * py + pz * pz)
                if (pLen < 1e-5f) continue
                px /= pLen
                py /= pLen
                pz /= pLen

                val w0 = ptsW[s]
                val w1 = ptsW[s + 1]

                emitVertex(matrix, consumer, ptsX[s] - px * w0, ptsY[s] - py * w0, ptsZ[s] - pz * w0, 0f, 0f, col[0], col[1], col[2], a0)
                emitVertex(matrix, consumer, ptsX[s] + px * w0, ptsY[s] + py * w0, ptsZ[s] + pz * w0, 1f, 0f, col[0], col[1], col[2], a0)
                emitVertex(matrix, consumer, ptsX[s + 1] + px * w1, ptsY[s + 1] + py * w1, ptsZ[s + 1] + pz * w1, 1f, 1f, col[0], col[1], col[2], a1)
                emitVertex(matrix, consumer, ptsX[s + 1] - px * w1, ptsY[s + 1] - py * w1, ptsZ[s + 1] - pz * w1, 0f, 1f, col[0], col[1], col[2], a1)
            }
        }
    }

    private fun renderGroundSparks(
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
        val baseSize = min(0.052f, max(0.024f, radius * 0.048f))
        val maxSparkY = min(0.62f, max(0.20f, height * 0.28f))

        for (i in strandParity until GROUND_SPARKS step 2) {
            val speed = 0.45f + (i % 3) * 0.14f
            val cycle = (timeSeconds * speed + i * (1.0f / GROUND_SPARKS)) % 1.0f
            val twinkle = sin(cycle * PI.toFloat()) * (0.72f + 0.28f * cos(timeSeconds * 6.2f + i * 1.9f))
            val alpha = (twinkle * 0.94f * alphaScale).coerceIn(0f, 1f)
            if (alpha <= 0.05f) continue

            val spread = 0.88f + 0.36f * (((i * 3) % 4) / 3.0f)
            val angle = i * GOLDEN_ANGLE - timeSeconds * 0.48f
            val r = radius * spread
            val cx = cos(angle) * r
            val cy = 0.035f + cycle * maxSparkY
            val cz = sin(angle) * r
            val size = baseSize * (0.75f + 0.35f * sin(cycle * PI.toFloat()))

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
