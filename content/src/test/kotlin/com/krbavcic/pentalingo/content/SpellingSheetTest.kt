package com.krbavcic.pentalingo.content

import com.krbavcic.pentalingo.model.PhraseItem
import com.krbavcic.pentalingo.script.Inscription
import com.krbavcic.pentalingo.script.Layout
import com.krbavcic.pentalingo.script.Mdc
import com.krbavcic.pentalingo.script.SignTable
import com.krbavcic.pentalingo.script.Translit
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.junit.Test

/**
 * Renders every spelling in the course onto contact sheets in `build/spelling-sheets`, the same
 * way the app draws them, so the whole course's hieroglyphs can be checked by eye in a minute.
 */
class SpellingSheetTest {

    private val glyphFont = File("../core-ui/src/main/res/font/noto_sans_egyptian_hieroglyphs.ttf")
    private val serifFont = File("../core-ui/src/main/res/font/noto_serif_regular.ttf")

    private fun draw(g: Graphics2D, ins: Inscription, font: Font, x: Float, y: Float, q: Float) {
        val table = SignTable.bundled
        g.color = Color(0x2B, 0x21, 0x18)
        for (ring in ins.rings) {
            g.stroke = BasicStroke(q * 0.045f)
            val bar = q * 0.14f
            g.draw(RoundRectangle2D.Float(x + ring.x * q, y + ring.y * q, ring.w * q - bar, ring.h * q, ring.h * q * 0.84f, ring.h * q * 0.84f))
            val bx = x + ring.x * q + ring.w * q - bar * 0.65f
            g.drawLine(bx.toInt(), (y + ring.y * q + q * 0.08f).toInt(), bx.toInt(), (y + (ring.y + ring.h) * q - q * 0.08f).toInt())
        }
        for (s in ins.signs) {
            val ink = s.glyph.ink
            val k = s.box.h * q / ink.height
            val saved = g.transform
            g.font = font.deriveFont(k * table.unitsPerEm)
            g.transform(AffineTransform.getTranslateInstance((x + s.box.x * q - ink.xMin * k).toDouble(), (y + s.box.y * q + ink.yMax * k).toDouble()))
            g.drawString(s.glyph.text, 0f, 0f)
            g.transform = saved
        }
    }

    @Test
    fun `render spelling sheets`() {
        if (!glyphFont.exists()) return
        val glyphs = Font.createFont(Font.TRUETYPE_FONT, glyphFont)
        val serif = Font.createFont(Font.TRUETYPE_FONT, serifFont).deriveFont(22f)
        val layout = Layout()
        val course = MIDDLE_EGYPTIAN
        val rows = course.words.map { Triple(layout.layout(it.mdc), Translit.display(it.translit), it.gloss) } +
            course.items.filterIsInstance<PhraseItem>().map { p ->
                Triple(layout.line(p.tokens.map { Mdc.parse(it.mdc) }), Translit.display(p.translit), p.translation)
            }
        val out = File("build/spelling-sheets").apply { mkdirs() }
        val q = 64f
        val rowH = 120
        val perPage = 16
        rows.chunked(perPage).forEachIndexed { page, chunk ->
            val img = BufferedImage(1400, rowH * chunk.size + 20, BufferedImage.TYPE_INT_RGB)
            val g = img.createGraphics()
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g.color = Color(0xEF, 0xE2, 0xBF)
            g.fillRect(0, 0, img.width, img.height)
            chunk.forEachIndexed { i, (ins, translit, gloss) ->
                val top = 10f + i * rowH
                val scale = minOf(q, 760f / ins.width)
                draw(g, ins, glyphs, 20f, top + (rowH - ins.height * scale) / 2f, scale)
                g.color = Color(0x2B, 0x21, 0x18)
                g.font = serif
                g.drawString(translit, 820, (top + 50).toInt())
                g.color = Color(0x5B, 0x49, 0x35)
                g.drawString(gloss.take(42), 820, (top + 84).toInt())
                g.color = Color(0xD8, 0xC2, 0x8E)
                g.drawLine(0, (top + rowH - 4).toInt(), img.width, (top + rowH - 4).toInt())
            }
            g.dispose()
            ImageIO.write(img, "png", File(out, "sheet-%02d.png".format(page + 1)))
        }
    }
}
