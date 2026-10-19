package com.krbavcic.pentalingo.script

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Renders sample spellings to `build/layout-previews` with the real font, using the same
 * ink-box-to-target transform the Android renderer uses.
 *
 * Not an assertion of beauty — a way to look at quadrats without a phone. The geometry rules are
 * tested in [LayoutTest]; this exists because "does it look like a monument?" is a question only
 * a picture answers.
 */
class LayoutPreviewTest {

    private val fontFile = File("../core-ui/src/main/res/font/noto_sans_egyptian_hieroglyphs.ttf")

    @Test
    fun `render previews`() {
        if (!fontFile.exists()) return
        val font = Font.createFont(Font.TRUETYPE_FONT, fontFile)
        val table = SignTable.bundled
        val layout = Layout()
        val out = File("build/layout-previews").apply { mkdirs() }

        val samples = mapOf(
            "nfr" to "F35-I9:D21",
            "anx" to "S34-N35:Aa1",
            "htp" to "R4:X1*Q3",
            "amenemhat" to "<M17-Y5:N35-G17-F4:X1>",
            "khufu" to "<Aa1-G43-I9-G43>",
            "offering" to "M23-X1:N35-R4:X1*Q3-X8",
            "pt" to "Q3:X1-N1",
            "mixed" to "M17:M17-X1*X1:N35-O1:Z1-G17",
        )
        val px = 160f
        for ((name, mdc) in samples) {
            val ins = layout.layout(mdc)
            val margin = 24
            val w = (ins.width * px).toInt() + 2 * margin
            val h = (ins.height * px).toInt() + 2 * margin
            val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
            val g = img.createGraphics()
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.color = Color(0xEA, 0xD9, 0xB0)
            g.fillRect(0, 0, w, h)
            g.translate(margin, margin)
            // The quadrat band, for judging vertical placement.
            g.color = Color(0xD8, 0xC2, 0x8E)
            g.drawRect(0, 0, (ins.width * px).toInt(), (ins.height * px).toInt())
            for (ring in ins.rings) {
                g.color = Color(0x2B, 0x21, 0x18)
                g.stroke = BasicStroke(px * 0.035f)
                val r = RoundRectangle2D.Float(
                    ring.x * px, ring.y * px, ring.w * px, ring.h * px, ring.h * px * 0.9f, ring.h * px * 0.9f,
                )
                g.draw(r)
            }
            for (s in ins.signs) {
                val ink = s.glyph.ink
                val scale = s.box.h * px / ink.height
                val size = scale * table.unitsPerEm
                val f = font.deriveFont(size)
                val ox = s.box.x * px - ink.xMin * scale
                val baseline = s.box.y * px + ink.yMax * scale
                g.color = Color(0x2B, 0x21, 0x18)
                g.font = f
                val saved = g.transform
                g.transform(AffineTransform.getTranslateInstance(ox.toDouble(), baseline.toDouble()))
                g.drawString(s.glyph.text, 0f, 0f)
                g.transform = saved
            }
            g.dispose()
            ImageIO.write(img, "png", File(out, "$name.png"))
        }
        assertTrue(out.listFiles().orEmpty().isNotEmpty())
    }
}
