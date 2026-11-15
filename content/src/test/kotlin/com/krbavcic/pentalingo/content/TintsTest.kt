package com.krbavcic.pentalingo.content

import com.krbavcic.pentalingo.engine.LineTints
import com.krbavcic.pentalingo.engine.grammarTints
import com.krbavcic.pentalingo.engine.soundTints
import com.krbavcic.pentalingo.model.PhraseItem
import com.krbavcic.pentalingo.model.SignTint.DETERMINATIVE
import com.krbavcic.pentalingo.model.SignTint.ECHO
import com.krbavcic.pentalingo.model.SignTint.ENDING
import com.krbavcic.pentalingo.model.SignTint.NOUN
import com.krbavcic.pentalingo.model.SignTint.PRONOUN
import com.krbavcic.pentalingo.model.SignTint.READ
import com.krbavcic.pentalingo.model.SignTint.READ_OTHER
import com.krbavcic.pentalingo.model.SignTint.VERB
import com.krbavcic.pentalingo.model.Token
import com.krbavcic.pentalingo.model.WordItem
import com.krbavcic.pentalingo.script.Mdc
import org.junit.Assert.assertEquals
import org.junit.Test

/** Signs are coloured by what they do: in a word by sound, in a sentence by kind of word. */
class TintsTest {

    private val course = MIDDLE_EGYPTIAN

    private fun word(id: String) = course.require(id) as WordItem

    @Test
    fun `in a word, each sign by how it is read`() {
        // The throne read ws, not its usual st; the eye jr; the god not read.
        assertEquals(listOf(READ_OTHER, READ, DETERMINATIVE), soundTints(course, word("w:wsjr")))
        // nfr: the heart-and-windpipe spells it, the viper and mouth repeat its f and r.
        assertEquals(listOf(READ, ECHO, ECHO), soundTints(course, word("w:nfr")))
    }

    @Test
    fun `in a sentence, a word by its kind, its endings and suffix apart`() {
        assertEquals(listOf(NOUN, NOUN, NOUN, NOUN, PRONOUN), grammarTints(course, Token("M17-X1:I9-A1-I9", "jt=f", "his father", "w:jt")))
        assertEquals(listOf(VERB, VERB, ENDING, PRONOUN), grammarTints(course, Token("F21-G17-N35:I9", "sḏm.n=f", "he heard", "w:sDm")))
        assertEquals(listOf(VERB, VERB, ENDING, PRONOUN, PRONOUN, PRONOUN),
            grammarTints(course, Token("F21-G17-N35-S29-N35:Z2", "sḏm.n=sn", "they heard", "w:sDm")))
    }

    @Test
    fun `every sign of every line has a colour`() {
        for (phrase in course.items.filterIsInstance<PhraseItem>()) {
            val tints = LineTints.grammar(course, phrase.tokens)
            var index = 0
            phrase.tokens.forEachIndexed { t, token ->
                repeat(Mdc.parse(token.mdc).signs.size) {
                    requireNotNull(tints.at(t, index)) { "${phrase.id}: no colour for sign $index" }
                    index++
                }
            }
        }
    }
}
