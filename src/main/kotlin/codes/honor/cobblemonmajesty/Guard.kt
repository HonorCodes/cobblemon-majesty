package codes.honor.cobblemonmajesty

import java.util.concurrent.atomic.AtomicLong

class Guard(private val name: String) {
    private val failures = AtomicLong()

    fun run(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            recordFailure(e)
        }
    }

    fun <T> callOrNull(block: () -> T?): T? {
        return try {
            block()
        } catch (e: Exception) {
            recordFailure(e)
            null
        }
    }

    private fun recordFailure(e: Exception) {
        val count = failures.incrementAndGet()
        if (count == 1L || count % 1000L == 0L) {
            CobblemonMajesty.LOGGER.error("{} failed, {} times so far", name, count, e)
        }
    }
}
