package com.krbavcic.pentalingo.script

/**
 * Hieroglyphic spellings in a subset of the Manuel de Codage, the encoding Egyptologists have
 * typed hieroglyphs in since 1988.
 *
 * - `-` (or a space) separates quadrats, the imaginary squares signs are grouped into;
 * - `:` stacks inside a quadrat, top to bottom;
 * - `*` sets signs side by side inside a quadrat, and binds tighter than `:`;
 * - `( )` groups, so `(X1*Q3):N35` is bread-and-stool over water;
 * - `<` and `>` enclose a royal name in a cartouche (`<-` and `->` are accepted too).
 *
 * A sign is a Gardiner code (`G17`, `Aa1`, `N35A`) or one of the standard phonetic aliases
 * (`m` for the owl G17, `H` for the twisted flax V28). Authoring the course in this notation
 * means every spelling is checkable text, and the same string drives the layout, the sign
 * palette in spelling exercises, and the validation tests.
 */
sealed interface Group

data class SignRef(val code: String) : Group

/** Signs set side by side, `A*B`. */
data class Beside(val parts: List<Group>) : Group

/** Signs stacked top to bottom, `A:B`. */
data class Stacked(val parts: List<Group>) : Group

sealed interface Element

data class Quadrat(val group: Group) : Element

data class Cartouche(val quadrats: List<Quadrat>) : Element

data class Writing(val elements: List<Element>) {

    /** Every sign in reading order. */
    val signs: List<String>
        get() = buildList {
            fun walk(g: Group) {
                when (g) {
                    is SignRef -> add(g.code)
                    is Beside -> g.parts.forEach(::walk)
                    is Stacked -> g.parts.forEach(::walk)
                }
            }
            for (e in elements) when (e) {
                is Quadrat -> walk(e.group)
                is Cartouche -> e.quadrats.forEach { walk(it.group) }
            }
        }
}

class MdcException(message: String) : IllegalArgumentException(message)

object Mdc {

    /**
     * The uniliteral aliases of the Manuel de Codage, plus the handful of common logograms whose
     * MdC names are unambiguous. Case matters: `h` is the shelter O4, `H` the twisted flax V28.
     */
    val ALIASES: Map<String, String> = mapOf(
        "A" to "G1", "i" to "M17", "y" to "Z4", "a" to "D36", "w" to "G43", "W" to "Z7",
        "b" to "D58", "p" to "Q3", "f" to "I9", "m" to "G17", "n" to "N35", "r" to "D21",
        "h" to "O4", "H" to "V28", "x" to "Aa1", "X" to "F32", "z" to "O34", "s" to "S29",
        "S" to "N37", "q" to "N29", "k" to "V31", "g" to "W11", "t" to "X1", "T" to "V13",
        "d" to "D46", "D" to "I10",
        "nfr" to "F35", "anx" to "S34", "Htp" to "R4", "nTr" to "R8", "pr" to "O1",
        "mn" to "Y5", "nb" to "V30", "sA" to "G39", "xpr" to "L1", "Dd" to "R11",
        "wAs" to "S40", "ra" to "N5", "sw" to "M23", "bit" to "L2", "wr" to "G36",
        "ms" to "F31", "km" to "I6", "tA" to "N16", "ib" to "F34", "kA" to "D28",
    )

    private val GARDINER = Regex("^(Aa|NL|NU|[A-Z])([0-9]{1,3})([A-Z]?)$")

    fun isGardinerCode(token: String): Boolean = GARDINER.matches(token)

    /** Gardiner codes in their canonical spelling: `a1` and `A001` both become `A1`. */
    fun canonicalCode(token: String): String? {
        val m = Regex("^(?i)(aa|nl|nu|[a-z])0*([0-9]{1,3})([a-z]?)$").find(token) ?: return null
        val (cat, num, suffix) = m.destructured
        val category = when (cat.lowercase()) {
            "aa" -> "Aa"
            "nl" -> "NL"
            "nu" -> "NU"
            else -> cat.uppercase()
        }
        return "$category$num${suffix.uppercase()}"
    }

    fun resolve(token: String): String =
        ALIASES[token]
            ?: token.takeIf(::isGardinerCode)
            ?: throw MdcException("Unknown sign '$token'")

    fun parse(source: String): Writing = Parser(source).writing()

    /** Parses and checks every sign exists in [table]; the content tests run this on every spelling. */
    fun parseChecked(source: String, table: SignTable = SignTable.bundled): Writing {
        val writing = parse(source)
        for (code in writing.signs) {
            if (table[code] == null) throw MdcException("Sign $code in '$source' is not in the font")
        }
        return writing
    }

    private class Parser(private val src: String) {
        private var pos = 0

        fun writing(): Writing {
            val elements = mutableListOf<Element>()
            skipSeparators()
            while (pos < src.length) {
                elements += element()
                skipSeparators()
            }
            if (elements.isEmpty()) throw MdcException("Empty spelling")
            return Writing(elements)
        }

        private fun element(): Element {
            if (peek() == '<') {
                pos++
                val quadrats = mutableListOf<Quadrat>()
                skipSeparators()
                while (peek() != '>') {
                    if (pos >= src.length) throw MdcException("Unclosed cartouche in '$src'")
                    quadrats += Quadrat(vertical())
                    skipSeparators()
                }
                pos++
                if (quadrats.isEmpty()) throw MdcException("Empty cartouche in '$src'")
                return Cartouche(quadrats)
            }
            return Quadrat(vertical())
        }

        private fun vertical(): Group {
            val parts = mutableListOf(horizontal())
            while (peek() == ':') {
                pos++
                parts += horizontal()
            }
            return parts.singleOrNull() ?: Stacked(parts)
        }

        private fun horizontal(): Group {
            val parts = mutableListOf(atom())
            while (peek() == '*') {
                pos++
                parts += atom()
            }
            return parts.singleOrNull() ?: Beside(parts)
        }

        private fun atom(): Group {
            if (peek() == '(') {
                pos++
                val inner = vertical()
                if (peek() != ')') throw MdcException("Expected ')' at $pos in '$src'")
                pos++
                return inner
            }
            val start = pos
            while (pos < src.length && src[pos].isLetterOrDigit()) pos++
            if (start == pos) {
                val found = if (pos < src.length) "'${src[pos]}'" else "end of input"
                throw MdcException("Expected a sign at $pos in '$src', found $found")
            }
            return SignRef(resolve(src.substring(start, pos)))
        }

        private fun skipSeparators() {
            // "<-" and "->" are how the Manuel itself writes cartouches; the hyphen is noise there.
            while (pos < src.length && (src[pos] == '-' || src[pos].isWhitespace())) pos++
        }

        private fun peek(): Char? = src.getOrNull(pos)
    }
}
