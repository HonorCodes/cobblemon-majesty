package codes.honor.cobblemonmajesty.palette

object EyeBoneClassifier {
    private val EXCLUDED_SUBSTRINGS = arrayOf(
        "lid",
        "closed",
        "brow",
        "lash",
        "tear",
        "socket",
        "hurt",
        "sleep",
        "sad",
        "angry",
        "dead",
        "faint",
        "wink",
        "blink",
        "locator"
    )

    private val PUPIL_OR_IRIS_REGEX = Regex(
        "(^|[_0-9])(pupil|pupils|iris|irises|cornea)([_0-9]|$)|^(left|right)(pupil|iris)|^(pupil|iris)(left|right)"
    )

    private val EYE_BONE_REGEX = Regex(
        "(^|[_0-9])(eye|eyes)([_0-9]|$)|^(left|right)(eye|eyes)([0-9]*)$|^(eye|eyes)(left|right|open|full)([0-9]*)$"
    )

    enum class Role {
        NONE,
        SCLERA_OR_EYE,
        PUPIL_OR_IRIS
    }

    fun classify(boneName: String, speciesPath: String? = null): Role {
        val lower = boneName.trim().lowercase()
        if (lower.isEmpty()) return Role.NONE
        if (speciesPath != null && lower == speciesPath.lowercase()) return Role.NONE
        for (excluded in EXCLUDED_SUBSTRINGS) {
            if (lower.contains(excluded)) return Role.NONE
        }
        if (PUPIL_OR_IRIS_REGEX.containsMatchIn(lower)) {
            return Role.PUPIL_OR_IRIS
        }
        if (EYE_BONE_REGEX.containsMatchIn(lower)) {
            return Role.SCLERA_OR_EYE
        }
        return Role.NONE
    }
}
