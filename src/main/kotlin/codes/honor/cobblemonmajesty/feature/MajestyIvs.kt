package codes.honor.cobblemonmajesty.feature

import kotlin.math.min
import kotlin.random.Random

object MajestyIvs {
    const val MAX_IV = 31

    fun selectStatsToMaximize(
        currentIvs: Map<String, Int>,
        additionalPerfectCount: Int,
        random: Random = Random.Default
    ): List<String> {
        if (additionalPerfectCount <= 0) return emptyList()
        val nonPerfect = currentIvs.entries
            .filter { (_, value) -> value < MAX_IV }
            .map { it.key }
        if (nonPerfect.isEmpty()) return emptyList()
        return nonPerfect.shuffled(random).take(min(additionalPerfectCount, nonPerfect.size))
    }

    fun shouldRoll(oneIn: Int, random: Random = Random.Default): Boolean {
        if (oneIn <= 0) return false
        if (oneIn == 1) return true
        return random.nextInt(oneIn) == 0
    }
}
