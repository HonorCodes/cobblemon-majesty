package codes.honor.cobblemonmajesty.client

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

object IrisShaderBridge {
    private const val COLORED_ENTITY_BASE = 50300

    private val PALETTE_RGB = intArrayOf(
        0xF8FAFC,
        0x9E5A2B,
        0xE63946,
        0xFF6B1A,
        0xFFE600,
        0x9AE62E,
        0x38E54D,
        0x22D3EE,
        0x38BDF8,
        0x3B5BDB,
        0x7B2CBF,
        0xE040FB,
        0xFF85C0
    )

    private val capturedInstance: Any?
    private val getCurrentEntity: MethodHandle?
    private val setCurrentEntity: MethodHandle?

    init {
        var inst: Any? = null
        var getter: MethodHandle? = null
        var setter: MethodHandle? = null
        try {
            val cls = Class.forName("net.irisshaders.iris.uniforms.CapturedRenderingState")
            inst = cls.getField("INSTANCE").get(null)
            val lookup = MethodHandles.publicLookup()
            getter = lookup.findVirtual(cls, "getCurrentRenderedEntity", MethodType.methodType(Int::class.javaPrimitiveType))
            setter = lookup.findVirtual(cls, "setCurrentEntity", MethodType.methodType(Void.TYPE, Int::class.javaPrimitiveType))
        } catch (_: Throwable) {
        }
        capturedInstance = inst
        getCurrentEntity = getter
        setCurrentEntity = setter
    }

    fun nearestShaderEntityId(rgb: Int): Int {
        val r = (rgb shr 16) and 0xFF
        val g = (rgb shr 8) and 0xFF
        val b = rgb and 0xFF
        var bestIdx = 0
        var bestDist = Int.MAX_VALUE
        for (i in PALETTE_RGB.indices) {
            val p = PALETTE_RGB[i]
            val dr = r - ((p shr 16) and 0xFF)
            val dg = g - ((p shr 8) and 0xFF)
            val db = b - (p and 0xFF)
            val dist = dr * dr * 3 + dg * dg * 4 + db * db * 2
            if (dist < bestDist) {
                bestDist = dist
                bestIdx = i
            }
        }
        return COLORED_ENTITY_BASE + bestIdx
    }

    inline fun withColoredEntity(rgb: Int, enabled: Boolean, action: () -> Unit) {
        if (!enabled) {
            action()
            return
        }
        val prev = pushColoredEntity(rgb)
        try {
            action()
        } finally {
            popColoredEntity(prev)
        }
    }

    @PublishedApi
    internal fun pushColoredEntity(rgb: Int): Int {
        val inst = capturedInstance ?: return Int.MIN_VALUE
        val getter = getCurrentEntity ?: return Int.MIN_VALUE
        val setter = setCurrentEntity ?: return Int.MIN_VALUE
        return try {
            val prev = getter.invoke(inst) as Int
            setter.invoke(inst, nearestShaderEntityId(rgb))
            prev
        } catch (_: Throwable) {
            Int.MIN_VALUE
        }
    }

    @PublishedApi
    internal fun popColoredEntity(previous: Int) {
        if (previous == Int.MIN_VALUE) return
        val inst = capturedInstance ?: return
        val setter = setCurrentEntity ?: return
        try {
            setter.invoke(inst, previous)
        } catch (_: Throwable) {
        }
    }
}
