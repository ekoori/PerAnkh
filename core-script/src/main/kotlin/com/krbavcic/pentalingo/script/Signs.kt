package com.krbavcic.pentalingo.script

/** A sign's ink box in font units, y pointing up from the baseline as fonts define it. */
data class InkBox(val xMin: Int, val yMin: Int, val xMax: Int, val yMax: Int) {
    val width: Int get() = xMax - xMin
    val height: Int get() = yMax - yMin
}

/** One hieroglyph as the bundled font draws it. */
data class Glyph(val code: String, val codepoint: Int, val ink: InkBox) {
    val text: String get() = String(Character.toChars(codepoint))
}

/**
 * Every sign the bundled font can draw, by Gardiner code.
 *
 * Generated from the font itself by `tools/gen_signs.py`, so the proportions the layout engine
 * works with are the proportions that will actually be drawn. Hand-maintained metrics would drift
 * the first time the font was replaced.
 */
class SignTable(glyphs: List<Glyph>, val unitsPerEm: Int) {

    private val byCode = glyphs.associateBy { it.code }
    private val byCodepoint = glyphs.associateBy { it.codepoint }

    val size: Int get() = byCode.size

    operator fun get(code: String): Glyph? = byCode[code]

    fun require(code: String): Glyph =
        byCode[code] ?: throw IllegalArgumentException("No hieroglyph with Gardiner code $code")

    fun byCodepoint(codepoint: Int): Glyph? = byCodepoint[codepoint]

    /**
     * The height of one quadrat in font units: the ink height of the full-height signs.
     *
     * Egyptian scribes sized every sign against an imaginary square, and the font follows them —
     * the seated man A1 and the reed M17 both stand exactly this tall. Everything in the layout is
     * measured in these quadrats rather than in ems, because the em is a typographer's unit and
     * the quadrat is the scribe's.
     */
    val quadrat: Int get() = require("A1").ink.height

    companion object {
        fun parse(tsv: String): SignTable {
            var unitsPerEm = 2048
            val glyphs = mutableListOf<Glyph>()
            for (line in tsv.lineSequence()) {
                if (line.startsWith("# unitsPerEm=")) {
                    unitsPerEm = line.substringAfter('=').trim().toInt()
                    continue
                }
                if (line.isBlank() || line.startsWith("#")) continue
                val f = line.split('\t')
                glyphs += Glyph(
                    code = f[0],
                    codepoint = f[1].toInt(16),
                    ink = InkBox(f[2].toInt(), f[3].toInt(), f[4].toInt(), f[5].toInt()),
                )
            }
            return SignTable(glyphs, unitsPerEm)
        }

        /** The table generated from the bundled font, loaded once from this module's resources. */
        val bundled: SignTable by lazy {
            val stream = SignTable::class.java.getResourceAsStream("signs.tsv")
                ?: error("signs.tsv is missing from core-script resources")
            parse(stream.bufferedReader(Charsets.UTF_8).use { it.readText() })
        }
    }
}
