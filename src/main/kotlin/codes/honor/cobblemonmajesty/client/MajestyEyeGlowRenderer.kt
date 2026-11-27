package codes.honor.cobblemonmajesty.client

import codes.honor.cobblemonmajesty.CobblemonMajesty
import codes.honor.cobblemonmajesty.Guard
import codes.honor.cobblemonmajesty.feature.MajestyFeature
import codes.honor.cobblemonmajesty.palette.EyeBoneClassifier
import codes.honor.cobblemonmajesty.palette.MajestyPalette
import codes.honor.cobblemonmajesty.palette.TypeColorPair
import com.cobblemon.mod.common.client.entity.PokemonClientDelegate
import com.cobblemon.mod.common.client.render.models.blockbench.PosableModel
import com.cobblemon.mod.common.client.render.models.blockbench.pose.Bone
import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import java.lang.reflect.Field
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import net.minecraft.client.Minecraft
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FastColor
import org.joml.Matrix4f
import org.joml.Vector3f

object MajestyEyeGlowRenderer {
    private const val MAX_CACHED_MASKS = 256

    private class CachedMask(
        val dynamicLocation: ResourceLocation,
        val renderType: RenderType
    )

    private val maskCache = LinkedHashMap<ResourceLocation, CachedMask?>()
    private val eyeGuard = Guard("MajestyEyeGlowRenderer")
    private var maskCounter = 0

    private val cubesField: Field? = resolveCubesField()
    private var lastModelEyeEntityId: Int = Int.MIN_VALUE

    fun beginFrame() {
        lastModelEyeEntityId = Int.MIN_VALUE
    }

    fun clearCache() {
        val texManager = Minecraft.getInstance().textureManager
        for (entry in maskCache.values) {
            if (entry != null) {
                eyeGuard.run {
                    texManager.release(entry.dynamicLocation)
                }
            }
        }
        maskCache.clear()
    }

    @JvmStatic
    fun renderModelEyeGlow(
        model: PosableModel,
        context: RenderContext,
        stack: PoseStack,
        packedOverlay: Int,
        baseColor: Int
    ) {
        val cfg = MajestyClientConfig.aura
        if (!cfg.enabled.get() || !cfg.glowingEyesEnabled.get()) return
        if (context.request(RenderContext.RENDER_STATE) != RenderContext.RenderState.WORLD) return

        val provider = model.bufferProvider ?: return
        val entity = context.request(RenderContext.ENTITY) as? PokemonEntity ?: return
        if (entity.isInvisible) return

        val delegate = entity.delegate as? PokemonClientDelegate ?: return
        val sendoutScale = delegate.activeSendoutScale
        if (sendoutScale <= 0.05f) return

        val maxDist = cfg.fullAuraDistance.get()
        val camPos = Minecraft.getInstance().gameRenderer.mainCamera.position
        if (camPos.distanceToSqr(entity.position()) > maxDist * maxDist) return

        val isMajestic = MajestyFeature.isMajesticAspects(entity.aspects) || MajestyFeature.isMajestic(entity.pokemon)
        if (!isMajestic) return

        val baseAlpha = ((baseColor ushr 24) and 0xFF) / 255f * model.alpha
        if (baseAlpha <= 0.05f) return

        val rootBone: Bone = model.rootPart
        val speciesId = entity.pokemon.species.resourceIdentifier
        val hasSeparatePupils = treeHasPupilCubes(rootBone, speciesId.path)

        val baseTexture = eyeGuard.callOrNull {
            VaryingModelRepository.getTexture(speciesId, delegate)
        } ?: return

        val eyeRenderType = getOrCreateEyeMaskRenderType(baseTexture) ?: return
        val consumer = provider.getBuffer(eyeRenderType)

        val (rgbA, rgbB) = MajestyColorResolver.resolveForEntity(entity)
        val coreRgb = MajestyPalette.brighten(rgbA, 0.34f)
        val colCore = TypeColorPair.rgbToFloats(coreRgb)
        val colOuter = TypeColorPair.rgbToFloats(rgbB)

        val timeSeconds = (entity.tickCount + delegate.getPartialTicks() + (entity.id % 53) * 1.9f) / 20.0f
        val pulse = (0.88f + 0.12f * sin(timeSeconds * 2.6f)) * baseAlpha * sendoutScale

        val primaryScale = if (hasSeparatePupils) 0.90f * pulse else 0.82f * pulse
        val scleraScale = 0.28f * pulse
        val accentMix = 0.28f

        val pupilColor = packPreMultipliedArgb(
            r = colCore[0] * (1f - accentMix) + colOuter[0] * accentMix,
            g = colCore[1] * (1f - accentMix) + colOuter[1] * accentMix,
            b = colCore[2] * (1f - accentMix) + colOuter[2] * accentMix,
            intensity = primaryScale
        )
        val scleraColor = packPreMultipliedArgb(
            r = colOuter[0],
            g = colOuter[1],
            b = colOuter[2],
            intensity = scleraScale
        )

        val drawnCount = renderEyeSubtree(
            boneName = "",
            bone = rootBone,
            speciesPath = speciesId.path,
            hasSeparatePupils = hasSeparatePupils,
            context = context,
            stack = stack,
            consumer = consumer,
            packedOverlay = packedOverlay,
            pupilColor = pupilColor,
            scleraColor = scleraColor
        )
        if (drawnCount > 0) {
            lastModelEyeEntityId = entity.id
        }
    }

    fun renderLocatorFallbackIfNeeded(
        entity: PokemonEntity,
        partialTicks: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        colA: FloatArray,
        colB: FloatArray,
        sendoutScale: Float
    ) {
        if (!MajestyClientConfig.aura.glowingEyesEnabled.get()) return
        if (lastModelEyeEntityId == entity.id) return

        val delegate = entity.delegate as? PokemonClientDelegate ?: return
        val locatorStates = delegate.locatorStates
        if (locatorStates.isEmpty()) return

        val mc = Minecraft.getInstance()
        val camera = mc.gameRenderer.mainCamera
        val camPos = camera.position
        val camLeft = Vector3f(camera.leftVector).normalize()
        val camUp = Vector3f(camera.upVector).normalize()
        val entityPos = entity.getPosition(partialTicks)

        val form = entity.pokemon.form
        val maxDim = max(form.hitbox.width, form.hitbox.height) * form.baseScale * entity.pokemon.effectiveScale
        val baseRadius = (0.14f * sqrt(max(0.25f, maxDim / 2.2f))) * sendoutScale

        var consumer: VertexConsumer? = null
        val matrix = poseStack.last().pose()

        for ((locatorName, wrapper) in locatorStates) {
            if (!locatorName.lowercase().contains("eye")) continue
            if (!delegate.isLocatorVisible(locatorName)) continue

            val eyeLocal = wrapper.matrix.getTranslation(Vector3f())
            val eyeForward = wrapper.matrix.transformDirection(Vector3f(0f, 0f, -1f)).normalize()
            val toCam = Vector3f(
                (camPos.x - (entityPos.x + eyeLocal.x)).toFloat(),
                (camPos.y - (entityPos.y + eyeLocal.y)).toFloat(),
                (camPos.z - (entityPos.z + eyeLocal.z)).toFloat()
            ).normalize()

            val facing = (eyeForward.dot(toCam) + 0.45f).coerceIn(0f, 1f)
            if (facing <= 0.05f) continue

            val size = baseRadius * facing * facing
            val nudge = 0.045f * facing
            val cx = eyeLocal.x + toCam.x * nudge
            val cy = eyeLocal.y + toCam.y * nudge
            val cz = eyeLocal.z + toCam.z * nudge

            if (consumer == null) {
                consumer = bufferSource.getBuffer(MajestyRenderTypes.AREA_LIGHT)
            }

            val outerSize = size * 1.45f
            emitAdditiveQuad(matrix, consumer, cx, cy, cz, camLeft, camUp, outerSize, colB[0] * 0.42f, colB[1] * 0.42f, colB[2] * 0.42f)
            val coreR = min(1f, colA[0] * 0.7f + 0.3f) * 0.82f
            val coreG = min(1f, colA[1] * 0.7f + 0.3f) * 0.82f
            val coreB = min(1f, colA[2] * 0.7f + 0.3f) * 0.82f
            emitAdditiveQuad(matrix, consumer, cx, cy, cz, camLeft, camUp, size * 0.75f, coreR, coreG, coreB)
        }
    }

    private fun emitAdditiveQuad(
        matrix: Matrix4f,
        consumer: VertexConsumer,
        cx: Float,
        cy: Float,
        cz: Float,
        camLeft: Vector3f,
        camUp: Vector3f,
        halfSize: Float,
        r: Float,
        g: Float,
        b: Float
    ) {
        val lx = camLeft.x * halfSize
        val ly = camLeft.y * halfSize
        val lz = camLeft.z * halfSize
        val ux = camUp.x * halfSize
        val uy = camUp.y * halfSize
        val uz = camUp.z * halfSize

        addEyeQuadVertex(matrix, consumer, cx - lx + ux, cy - ly + uy, cz - lz + uz, 0f, 0f, r, g, b)
        addEyeQuadVertex(matrix, consumer, cx - lx - ux, cy - ly - uy, cz - lz - uz, 0f, 1f, r, g, b)
        addEyeQuadVertex(matrix, consumer, cx + lx - ux, cy + ly - uy, cz + lz - uz, 1f, 1f, r, g, b)
        addEyeQuadVertex(matrix, consumer, cx + lx + ux, cy + ly + uy, cz + lz + uz, 1f, 0f, r, g, b)
    }

    private fun addEyeQuadVertex(
        matrix: Matrix4f,
        consumer: VertexConsumer,
        x: Float,
        y: Float,
        z: Float,
        u: Float,
        v: Float,
        r: Float,
        g: Float,
        b: Float
    ) {
        consumer.addVertex(matrix, x, y, z)
            .setColor(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f), 1f)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightTexture.FULL_BRIGHT)
            .setNormal(0f, 1f, 0f)
    }

    private fun treeHasPupilCubes(bone: Bone, speciesPath: String): Boolean {
        for ((name, child) in bone.children) {
            val role = EyeBoneClassifier.classify(name, speciesPath)
            if (role == EyeBoneClassifier.Role.PUPIL_OR_IRIS && hasOwnGeometry(child)) {
                return true
            }
            if (treeHasPupilCubes(child, speciesPath)) {
                return true
            }
        }
        return false
    }

    private fun renderEyeSubtree(
        boneName: String,
        bone: Bone,
        speciesPath: String,
        hasSeparatePupils: Boolean,
        context: RenderContext,
        stack: PoseStack,
        consumer: VertexConsumer,
        packedOverlay: Int,
        pupilColor: Int,
        scleraColor: Int
    ): Int {
        val modelPart = asModelPart(bone)
        if (modelPart != null) {
            if (!modelPart.visible) return 0
            if (abs(modelPart.xScale) < 1e-3f || abs(modelPart.yScale) < 1e-3f || abs(modelPart.zScale) < 1e-3f) return 0
        }

        var drawn = 0
        val role = if (boneName.isEmpty()) EyeBoneClassifier.Role.NONE else EyeBoneClassifier.classify(boneName, speciesPath)
        val children = bone.children
        val skipOwn = modelPart != null && modelPart.skipDraw

        if (role != EyeBoneClassifier.Role.NONE && !skipOwn && hasOwnGeometry(bone)) {
            val drawColor = if (role == EyeBoneClassifier.Role.SCLERA_OR_EYE && hasSeparatePupils) {
                scleraColor
            } else {
                pupilColor
            }
            renderOwnBoneWithoutChildren(bone, children.values, context, stack, consumer, packedOverlay, drawColor)
            drawn++
        }

        if (children.isNotEmpty()) {
            stack.pushPose()
            bone.transform(stack)
            for ((childName, child) in children) {
                drawn += renderEyeSubtree(
                    boneName = childName,
                    bone = child,
                    speciesPath = speciesPath,
                    hasSeparatePupils = hasSeparatePupils,
                    context = context,
                    stack = stack,
                    consumer = consumer,
                    packedOverlay = packedOverlay,
                    pupilColor = pupilColor,
                    scleraColor = scleraColor
                )
            }
            stack.popPose()
        }
        return drawn
    }

    private fun renderOwnBoneWithoutChildren(
        bone: Bone,
        directChildren: Collection<Bone>,
        context: RenderContext,
        stack: PoseStack,
        consumer: VertexConsumer,
        packedOverlay: Int,
        color: Int
    ) {
        if (directChildren.isEmpty()) {
            bone.render(context, stack, consumer, LightTexture.FULL_BRIGHT, packedOverlay, color)
            return
        }
        val childParts = ArrayList<ModelPart>(directChildren.size)
        for (child in directChildren) {
            val mp = asModelPart(child)
            if (mp != null) childParts.add(mp)
        }
        val savedStates = BooleanArray(childParts.size)
        for (i in childParts.indices) {
            savedStates[i] = childParts[i].visible
            childParts[i].visible = false
        }
        try {
            bone.render(context, stack, consumer, LightTexture.FULL_BRIGHT, packedOverlay, color)
        } finally {
            for (i in childParts.indices) {
                childParts[i].visible = savedStates[i]
            }
        }
    }

    private fun getOrCreateEyeMaskRenderType(baseTexture: ResourceLocation): RenderType? {
        if (maskCache.containsKey(baseTexture)) {
            return maskCache[baseTexture]?.renderType
        }
        if (maskCache.size >= MAX_CACHED_MASKS) {
            clearCache()
        }

        val built = eyeGuard.callOrNull {
            val mc = Minecraft.getInstance()
            val resourceOpt = mc.resourceManager.getResource(baseTexture)
            if (resourceOpt.isEmpty) return@callOrNull null

            resourceOpt.get().open().use { input ->
                NativeImage.read(input).use { src ->
                    val w = src.width
                    val h = src.height
                    val dst = NativeImage(w, h, true)
                    for (y in 0 until h) {
                        for (x in 0 until w) {
                            val pixel = src.getPixelRGBA(x, y)
                            val a = FastColor.ABGR32.alpha(pixel)
                            if (a < 24) {
                                dst.setPixelRGBA(x, y, 0)
                            } else {
                                val r = FastColor.ABGR32.red(pixel)
                                val g = FastColor.ABGR32.green(pixel)
                                val b = FastColor.ABGR32.blue(pixel)
                                val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                                val maxC = max(r, max(g, b))
                                val minC = min(r, min(g, b))
                                val sat = (maxC - minC) / 255f

                                val rawIntensity = when {
                                    lum > 0.82f && sat < 0.15f -> 110
                                    sat >= 0.15f || lum in 0.18f..0.82f -> 255
                                    else -> 218
                                }
                                val premul = ((rawIntensity * a) / 255).coerceIn(0, 255)
                                val packedGrayAbgr = (a shl 24) or (premul shl 16) or (premul shl 8) or premul
                                dst.setPixelRGBA(x, y, packedGrayAbgr)
                            }
                        }
                    }
                    val dynId = ResourceLocation.fromNamespaceAndPath(
                        CobblemonMajesty.ID,
                        "dynamic/eye_glow_${++maskCounter}"
                    )
                    mc.textureManager.register(dynId, DynamicTexture(dst))
                    CachedMask(dynId, RenderType.eyes(dynId))
                }
            }
        }

        maskCache[baseTexture] = built
        return built?.renderType
    }

    private fun packPreMultipliedArgb(r: Float, g: Float, b: Float, intensity: Float): Int {
        val k = intensity.coerceIn(0f, 1f)
        val ri = ((r * k).coerceIn(0f, 1f) * 255f).toInt()
        val gi = ((g * k).coerceIn(0f, 1f) * 255f).toInt()
        val bi = ((b * k).coerceIn(0f, 1f) * 255f).toInt()
        val ai = (k * 255f).toInt()
        return (ai shl 24) or (ri shl 16) or (gi shl 8) or bi
    }

    private fun asModelPart(bone: Bone): ModelPart? {
        val raw: Any = bone
        return raw as? ModelPart
    }

    private fun hasOwnGeometry(bone: Bone): Boolean {
        val mp = asModelPart(bone) ?: return true
        val f = cubesField ?: return true
        return try {
            val raw = f.get(mp)
            raw is List<*> && raw.isNotEmpty()
        } catch (_: Throwable) {
            true
        }
    }

    private fun resolveCubesField(): Field? {
        for (field in ModelPart::class.java.declaredFields) {
            if (field.name == "cubes" && List::class.java.isAssignableFrom(field.type)) {
                field.isAccessible = true
                return field
            }
        }
        for (field in ModelPart::class.java.declaredFields) {
            if (List::class.java.isAssignableFrom(field.type)) {
                field.isAccessible = true
                return field
            }
        }
        return null
    }
}
