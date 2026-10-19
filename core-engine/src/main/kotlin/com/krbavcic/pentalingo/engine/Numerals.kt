package com.krbavcic.pentalingo.engine

/**
 * Egyptian numbers: one sign per power of ten, repeated, largest first.
 *
 * The system is additive — 23 is two hobbles and three strokes — so any number can be both
 * read and written from seven signs, which makes numbers the one place the course can generate
 * endless fresh exercises instead of drilling a fixed list.
 */
object Numerals {

    /** Gardiner code to value, largest first. */
    val SIGNS: List<Pair<String, Int>> = listOf(
        "C11" to 1_000_000,
        "I8" to 100_000,
        "D50" to 10_000,
        "M12" to 1_000,
        "V1" to 100,
        "V20" to 10,
        "Z1" to 1,
    )

    private val valueOf = SIGNS.toMap()

    fun valueOf(code: String): Int? = valueOf[code]

    /** Rows used when a sign repeats: five is three over two, nine is three rows of three. */
    private val ROWS = mapOf(
        1 to listOf(1), 2 to listOf(2), 3 to listOf(3), 4 to listOf(4),
        5 to listOf(3, 2), 6 to listOf(3, 3), 7 to listOf(4, 3), 8 to listOf(4, 4), 9 to listOf(3, 3, 3),
    )

    /** The hieroglyphic spelling of [value], or null if it needs a sign the learner was not given. */
    fun spell(value: Int, available: Collection<String> = SIGNS.map { it.first }): String? {
        require(value in 1..9_999_999) { "Egyptian numerals here run from 1 to 9,999,999" }
        var rest = value
        val quadrats = mutableListOf<String>()
        for ((code, unit) in SIGNS) {
            val count = rest / unit
            rest %= unit
            if (count == 0) continue
            if (code !in available) return null
            quadrats += ROWS.getValue(count).joinToString(":") { n -> List(n) { code }.joinToString("*") }
        }
        return quadrats.joinToString("-")
    }

    /** One sign repeated [count] times (1..9) as a single quadrat, the way scribes grouped them. */
    fun quadrat(code: String, count: Int): String =
        ROWS.getValue(count.coerceIn(1, 9)).joinToString(":") { n -> List(n) { code }.joinToString("*") }

    /**
     * Whatever signs a learner has tapped, grouped for display: largest first, nine to a quadrat.
     * Unlike [spell] this draws non-canonical counts too, so ten strokes look like ten strokes.
     */
    fun group(codes: List<String>): String? {
        if (codes.isEmpty()) return null
        val counts = codes.groupingBy { it }.eachCount()
        return SIGNS.filter { it.first in counts }.flatMap { (code, _) ->
            val n = counts.getValue(code)
            (0 until n step 9).map { start -> quadrat(code, minOf(9, n - start)) }
        }.joinToString("-")
    }

    fun sum(codes: List<String>): Int = codes.sumOf { valueOf[it] ?: 0 }

    /** True if no sign is used ten or more times — the way a scribe would have written it. */
    fun isCanonical(codes: List<String>): Boolean = codes.groupingBy { it }.eachCount().values.all { it <= 9 }

    fun format(value: Int): String = "%,d".format(value)
}
