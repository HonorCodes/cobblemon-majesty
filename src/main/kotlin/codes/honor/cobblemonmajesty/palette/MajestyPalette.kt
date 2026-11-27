package codes.honor.cobblemonmajesty.palette

data class TypeColorPair(
    val primaryRgb: Int,
    val accentRgb: Int
) {
    fun primaryFloats(): FloatArray = rgbToFloats(primaryRgb)
    fun accentFloats(): FloatArray = rgbToFloats(accentRgb)

    companion object {
        fun rgbToFloats(rgb: Int): FloatArray = floatArrayOf(
            ((rgb shr 16) and 0xFF) / 255f,
            ((rgb shr 8) and 0xFF) / 255f,
            (rgb and 0xFF) / 255f
        )

        fun parseHexRgb(hex: String): Int? {
            val cleaned = hex.trim().removePrefix("#").removePrefix("0x").removePrefix("0X")
            if (cleaned.length != 6) return null
            return cleaned.toIntOrNull(16)
        }

        fun formatHexRgb(rgb: Int): String = String.format("#%06X", rgb and 0xFFFFFF)
    }
}

object MajestyPalette {
    val DEFAULTS: Map<String, TypeColorPair> = linkedMapOf(
        "normal" to TypeColorPair(0xF4F1DE, 0xCBD5E1),
        "fire" to TypeColorPair(0xFF5400, 0xFFD166),
        "water" to TypeColorPair(0x2EC4FF, 0x2563EB),
        "grass" to TypeColorPair(0x38E54D, 0xB5F44A),
        "electric" to TypeColorPair(0xFFE600, 0xFFF7AE),
        "ice" to TypeColorPair(0x72F5FF, 0xE0FCFF),
        "fighting" to TypeColorPair(0xE63946, 0xFF7B54),
        "poison" to TypeColorPair(0xB5179E, 0xE040FB),
        "ground" to TypeColorPair(0xE09F3E, 0x9E5A2B),
        "flying" to TypeColorPair(0xA5B4FC, 0xF1F5F9),
        "psychic" to TypeColorPair(0xFF4D94, 0xC77DFF),
        "bug" to TypeColorPair(0xAACC00, 0x55A630),
        "rock" to TypeColorPair(0xC9A66B, 0x7F674C),
        "ghost" to TypeColorPair(0x7B2CBF, 0xC77DFF),
        "dragon" to TypeColorPair(0x4361EE, 0xFF3366),
        "dark" to TypeColorPair(0x3B1D6E, 0x9D4EDD),
        "steel" to TypeColorPair(0xD8E4F0, 0x94A3B8),
        "fairy" to TypeColorPair(0xFF85C0, 0xFFE5F1)
    )

    fun resolveDualStrandColors(
        primaryTypeName: String,
        secondaryTypeName: String?,
        fallbackPrimaryHue: Int,
        fallbackSecondaryHue: Int?,
        overrides: Map<String, TypeColorPair> = emptyMap()
    ): Pair<Int, Int> {
        val key1 = primaryTypeName.lowercase()
        val pair1 = overrides[key1] ?: DEFAULTS[key1] ?: TypeColorPair(
            fallbackPrimaryHue and 0xFFFFFF,
            brighten(fallbackPrimaryHue and 0xFFFFFF, 0.35f)
        )
        if (secondaryTypeName == null || secondaryTypeName.equals(primaryTypeName, ignoreCase = true)) {
            return pair1.primaryRgb to pair1.accentRgb
        }
        val key2 = secondaryTypeName.lowercase()
        val pair2 = overrides[key2] ?: DEFAULTS[key2] ?: TypeColorPair(
            (fallbackSecondaryHue ?: fallbackPrimaryHue) and 0xFFFFFF,
            brighten((fallbackSecondaryHue ?: fallbackPrimaryHue) and 0xFFFFFF, 0.35f)
        )
        return pair1.primaryRgb to pair2.primaryRgb
    }

    fun brighten(rgb: Int, factor: Float): Int {
        val f = factor.coerceIn(0f, 1f)
        val r = ((rgb shr 16) and 0xFF)
        val g = ((rgb shr 8) and 0xFF)
        val b = (rgb and 0xFF)
        val nr = (r + ((255 - r) * f)).toInt().coerceIn(0, 255)
        val ng = (g + ((255 - g) * f)).toInt().coerceIn(0, 255)
        val nb = (b + ((255 - b) * f)).toInt().coerceIn(0, 255)
        return (nr shl 16) or (ng shl 8) or nb
    }
}
