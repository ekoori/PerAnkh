package com.krbavcic.pentalingo.content

import com.krbavcic.pentalingo.model.Paint
import com.krbavcic.pentalingo.model.SignItem
import com.krbavcic.pentalingo.model.SignRole

/**
 * The traditional pigment for a sign, from its Gardiner category: flesh red-brown, birds and
 * water blue, plants and reptiles green, architecture and objects ochre, sacred furniture gold.
 */
internal fun paintFor(code: String): Paint {
    when (code) {
        "N5" -> return Paint.CARNELIAN
        "N16", "N17", "N23", "N25", "N29" -> return Paint.OCHRE
        "H6", "S34" -> return Paint.TURQUOISE
        "V28" -> return Paint.MALACHITE
        "B1" -> return Paint.OCHRE
    }
    val category = Regex("^(Aa|NL|NU|[A-Z])").find(code)?.value ?: return Paint.INK
    return when (category) {
        "A", "C", "D", "T" -> Paint.CARNELIAN
        "B", "E", "F", "O", "P", "Q", "U", "V", "X", "Aa" -> Paint.OCHRE
        "G", "H", "N", "Y" -> Paint.LAPIS
        "I", "K", "L", "M" -> Paint.MALACHITE
        "R", "S" -> Paint.GOLD
        "W" -> Paint.TURQUOISE
        else -> Paint.INK
    }
}

private fun sign(
    code: String,
    role: SignRole,
    name: String,
    sounds: List<String> = emptyList(),
    meaning: String? = null,
    reading: String? = null,
    hint: String? = null,
    note: String? = null,
    like: List<String> = emptyList(),
) = SignItem(
    id = "s:$code", code = code, name = name, role = role, sounds = sounds, meaning = meaning,
    reading = reading, hint = hint, note = note, paint = paintFor(code), lookalikes = like,
)

private fun uni(code: String, sound: String, name: String, hint: String, note: String? = null, like: List<String> = emptyList()) =
    sign(code, SignRole.UNILITERAL, name, listOf(sound), hint = hint, note = note, like = like)

private fun det(code: String, name: String, meaning: String, note: String? = null, like: List<String> = emptyList()) =
    sign(code, SignRole.DETERMINATIVE, name, meaning = meaning, note = note, like = like)

private fun multi(code: String, sound: String, name: String, meaning: String? = null, hint: String? = null, note: String? = null, like: List<String> = emptyList()) =
    sign(code, if (sound.length == 2) SignRole.BILITERAL else SignRole.TRILITERAL, name, listOf(sound), meaning, null, hint, note, like)

private fun logo(code: String, sound: String, name: String, meaning: String, hint: String? = null, note: String? = null, like: List<String> = emptyList()) =
    sign(code, SignRole.LOGOGRAM, name, listOf(sound), meaning, null, hint, note, like)

private fun numeral(code: String, name: String, reading: String, note: String) =
    sign(code, SignRole.NUMERAL, name, reading = reading, note = note)

private val BIRDS = listOf("G1", "G17", "G43", "G39", "G36", "G14", "G5", "G25", "G29", "G37")
private fun birdsBut(code: String) = BIRDS - code

internal val SIGNS: List<SignItem> = listOf(
    // ---- the 25 uniliterals ------------------------------------------------------------------
    uni("G1", "ꜣ", "Egyptian vulture",
        "Written ꜣ — it looks like a 3, but it is a letter. A catch in the throat, like the stop in “uh-oh”; Egyptologists say it as “a”.",
        "The first letter of every Egyptological dictionary. Its exact sound is debated; it may once have been an r or l.",
        birdsBut("G1")),
    uni("M17", "j", "flowering reed",
        "Said “i”, as in “ink”. The reed is the hieroglyph that most often starts a word.",
        "Older books write it ỉ or i; this course follows Allen and writes j.",
        listOf("Z1", "M23")),
    uni("Z4", "y", "two strokes",
        "A “y”. Also written with two reeds side by side.",
        "Two diagonal strokes also mark a pair: the dual ending of nouns."),
    uni("D36", "ꜥ", "forearm",
        "A throaty sound made deep in the throat — the ʿayn of Arabic. Egyptologists say “a”.",
        "The palm-up forearm. Words like ꜥnḫ, “life”, begin with it.",
        listOf("D46", "D58")),
    uni("G43", "w", "quail chick",
        "A “w”, or “u” between consonants.",
        "Scribes writing quickly turned the chick into a little coil, Z7 — the same letter.",
        birdsBut("G43")),
    uni("D58", "b", "foot", "A plain “b”.", "Strictly the lower leg and foot.", listOf("D46", "D36")),
    uni("Q3", "p", "stool", "A plain “p”.", "A woven reed seat seen from the side.", listOf("N37", "O4")),
    uni("I9", "f", "horned viper",
        "An “f”. It also means “he, his” when it follows a word: rn=f, “his name”.",
        "The horns are the cerastes viper’s — a real desert snake.",
        listOf("I10")),
    uni("G17", "m", "owl",
        "An “m”. The owl is the only bird that faces you: its face is turned to the reader.",
        "On its own the owl also writes m, “in, with”.",
        birdsBut("G17")),
    uni("N35", "n", "water ripple",
        "An “n”. A single ripple of water.",
        "Three ripples stacked (N35A) mean water itself, mw.",
        listOf("V13", "D21")),
    uni("D21", "r", "mouth",
        "An “r”. On its own it writes r, “to, towards”.",
        "Egyptian has no l; the mouth stands in for it in foreign names like Cleopatra.",
        listOf("N35", "N37")),
    uni("O4", "h", "reed shelter",
        "A plain “h”, as in “house”.",
        "A courtyard or shelter of reeds, seen from above.",
        listOf("O1", "Q3")),
    uni("V28", "ḥ", "twisted flax",
        "An emphatic h, breathed hard from the throat — the ḥāʾ of Arabic. Said “h”.",
        "A wick of twisted flax. Watch for the dot under the ḥ: it is a different letter from h.",
        listOf("V13", "V31")),
    uni("Aa1", "ḫ", "placenta",
        "“kh”, like the ch in Scottish “loch”.",
        "Perhaps a placenta, perhaps a sieve — nobody is sure. Its sound is not in doubt.",
        listOf("N5", "O49", "X1")),
    uni("F32", "ẖ", "belly with udder",
        "A softer “kh”, like the ch in German “ich”.",
        "An animal’s belly with teats and tail. The rarest of the four h’s.",
        listOf("Aa1")),
    uni("O34", "z", "door bolt",
        "Originally z, but by Middle Egyptian it sounded just like s. Answers accept either.",
        "A wooden bolt that slid through rings on a door.",
        listOf("S29")),
    uni("S29", "s", "folded cloth", "A plain “s”.", "A length of folded linen.", listOf("O34")),
    uni("N37", "š", "garden pool",
        "“sh”, as in “shell”.",
        "A rectangular pool, as in the gardens painted in tombs.",
        listOf("Q3", "O4")),
    uni("N29", "q", "sandy hill slope",
        "A “k” made further back in the throat. Older books write ḳ.",
        "A hillside dotted with sand grains.",
        listOf("X1", "V31")),
    uni("V31", "k", "basket with handle",
        "A plain “k”. After a word it means “you, your”: pr=k, “your house”.",
        "A wicker basket with a loop handle on the right.",
        listOf("X1", "N29", "V30")),
    uni("W11", "g", "jar stand", "A hard “g”.", "A ring-shaped pot stand.", listOf("V31")),
    uni("X1", "t", "bread loaf",
        "A “t”. At the end of a noun it usually marks the feminine: sꜣ “son”, sꜣt “daughter”.",
        "A small round loaf, seen from the side.",
        listOf("N29", "V31")),
    uni("V13", "ṯ", "tethering rope",
        "“tch”, as in “church”.",
        "A hobbling rope for animals.",
        listOf("V28", "N35")),
    uni("D46", "d", "hand", "A plain “d”.", "An open hand, palm down.", listOf("D36", "D58")),
    uni("I10", "ḏ", "cobra",
        "“dj”, as in “judge”.",
        "The cobra at rest. Raised and crowned, it becomes the uraeus on the king’s brow.",
        listOf("I9")),

    // ---- determinatives -----------------------------------------------------------------------
    det("A1", "seated man", "man, male person",
        "Also writes j, “I, my”, after a word: jt=j, “my father”.",
        listOf("A40", "B1", "A2")),
    det("B1", "seated woman", "woman, female person", null, listOf("A1", "A40")),
    det("A40", "seated god", "god, divine being", "A man wearing the curved beard of a god.", listOf("A1", "B1")),
    det("A2", "man with hand to mouth", "eating, speaking, thinking",
        "Anything done with the mouth or the mind: eat, speak, love, think.",
        listOf("A1")),
    det("N1", "sky", "sky, above", "The sky as a flat slab held up at its ends.", listOf("N2")),
    det("N2", "night sky", "night, darkness", "The sky with a sceptre hanging from it: the dark.", listOf("N1")),
    sign("N5", SignRole.LOGOGRAM, "sun disk", listOf("rꜥ"), "sun; day; time", null,
        "On its own, the sun and the sun god Ra. After a word, a determinative for times and days.",
        null, listOf("Aa1", "O49")),
    det("Y1", "papyrus roll", "writing, abstract ideas",
        "A tied papyrus scroll. It marks words for things you cannot touch: knowledge, plans, greatness.",
        listOf("Y3")),
    det("G37", "sparrow", "small, bad, weak", "The little brown sparrow marks anything small or unfortunate.", birdsBut("G37")),
    det("E16", "jackal on a shrine", "Anubis", "Anubis, guardian of the necropolis, couched on a shrine.", listOf("E20")),
    det("Z2", "three strokes", "plural, many things",
        "Three is the Egyptian way of saying “many”: the strokes mark a plural."),
    det("H8", "egg", "goddess, female being", "An egg after a name marks a goddess or a woman."),
    det("O49", "village crossroads", "town, inhabited place",
        "A round settlement crossed by two streets. Also the word njwt, “town”.",
        listOf("Aa1", "N5")),
    det("E20", "Seth animal", "Seth; chaos, storm",
        "An animal no zoologist has identified, with a forked tail and squared ears: Seth’s creature.",
        listOf("E16")),
    det("N23", "irrigation canal", "land, territory", "A canal through fields: cultivated land."),
    det("N17", "strip of land", "land; eternity in ḏt", "A flat strip of earth. In ḏt, “forever”, it stands for the enduring land.", listOf("N16")),
    det("C10", "goddess with a feather", "Maat", "A seated goddess with Maat’s feather on her head.", listOf("A40", "B1")),

    // ---- signs of two and three sounds, and pictures that are words ---------------------------
    logo("O1", "pr", "house", "house",
        "A house plan seen from above, with its door on one side.",
        "pr-ꜥꜣ, “great house”, meant the palace — and, much later, the king himself: pharaoh.",
        listOf("O4", "Q3")),
    multi("Y5", "mn", "game board", "remain, endure",
        "A senet board seen from the side, with its pieces.",
        "Senet was a board game played for two thousand years — and, later, a game of the soul’s journey."),
    logo("V30", "nb", "basket", "lord; every",
        "A wicker basket. Two meanings, one sound: nb “lord” and nb “every, all”.",
        null, listOf("V31", "X1")),
    logo("S34", "ꜥnḫ", "ankh", "life, to live",
        "Probably a sandal strap — sandal was ꜥnḫ too — borrowed to write “life”.",
        "Gods hold the ankh to the noses of kings: the breath of life."),
    logo("F35", "nfr", "heart and windpipe", "good, beautiful, perfect",
        "Usually followed by f and r, repeating its own sounds.",
        "Nefertiti means “the beautiful one has come”."),
    logo("R4", "ḥtp", "loaf on an offering mat", "offering; peace, contentment",
        "A loaf set on a reed mat: an offering, and the contentment it brings.",
        "Amenhotep is jmn-ḥtp, “Amun is content”."),
    logo("R8", "nṯr", "cloth on a pole", "god",
        "A banner on a pole, like those that stood before temple gateways.",
        "Hieroglyphs were mdw-nṯr: “the god’s words”."),
    logo("G39", "sꜣ", "pintail duck", "son", "With a seated man after it: “son”.", null, birdsBut("G39")),
    multi("G36", "wr", "swallow", "great, big", "The swallow writes wr, “great”.", null, birdsBut("G36")),
    multi("F31", "ms", "three fox skins", "give birth",
        "Three skins tied together. In names like Ramesses and Thutmose: “born of”."),
    logo("L1", "ḫpr", "scarab beetle", "come into being, become",
        "The dung beetle rolls its ball as the god Khepri rolls the sun into the morning.",
        "The heart scarab, placed on a mummy, asked the heart not to testify against its owner."),
    logo("R11", "ḏd", "djed pillar", "stability, endurance",
        "A pillar with stacked crossbars: the backbone of Osiris.",
        "The king raised a djed at festivals to show the order of the world restored."),
    logo("S40", "wꜣs", "was sceptre", "dominion, power",
        "A staff with an animal head and forked foot, held by gods.",
        "Thebes was wꜣst, “the city of the sceptre”."),
    multi("I6", "km", "piece of crocodile skin", "black",
        "A strip of crocodile hide with its scales. It writes km, “black”.",
        "Egypt was kmt, “the Black Land”, for the dark silt the Nile flood left behind."),
    logo("N16", "tꜣ", "land with grains of sand", "land, earth",
        "A strip of land with three grains beneath it.",
        "Two of them, tꜣwj, are “the Two Lands”: Upper and Lower Egypt."),
    logo("G14", "mwt", "vulture", "mother",
        "The griffon vulture writes mwt, “mother” — and the goddess Mut.",
        null, birdsBut("G14")),
    multi("N41", "ḥm", "well with water", null,
        "A well full of water. It writes ḥm in ḥmt, “woman, wife”."),
    multi("T22", "sn", "arrowhead", null,
        "An arrowhead: it writes sn, as in sn, “brother”."),
    multi("Q1", "st", "throne", "seat, place",
        "A carrying chair seen from the side. It begins the names of Isis and Osiris."),
    multi("D4", "jr", "eye", "do, make; eye",
        "The human eye, with brow. It writes jr — and jrj, “to do, to make”."),
    logo("G5", "ḥr", "falcon", "Horus",
        "The falcon is Horus, god of kingship. Every king was Horus on earth."),
    logo("G26", "ḏḥwtj", "ibis on a standard", "Thoth",
        "The sacred ibis on its perch: Thoth, god of writing, counting and wisdom."),
    logo("H6", "mꜣꜥt", "feather", "truth, order, Maat",
        "An ostrich feather: the feather of Maat, weighed against the heart.",
        "Maat is not just truth but the right order of everything: justice, balance, the sun rising."),
    logo("D28", "kꜣ", "two raised arms", "ka, life force",
        "Two arms raised to embrace: the ka, the vital force each person is born with.",
        "Offerings were made “for the ka” of the dead."),
    logo("G29", "bꜣ", "jabiru stork", "ba, personality",
        "The saddle-billed stork writes bꜣ: the part of a person that could fly from the tomb by day.",
        null, birdsBut("G29")),
    logo("G25", "ꜣḫ", "crested ibis", "akh, transfigured spirit",
        "The crested ibis writes ꜣḫ: the blessed, shining form of the dead among the stars.",
        null, birdsBut("G25")),
    logo("F34", "jb", "heart", "heart, mind",
        "The heart as a vessel with handles. The Egyptians thought with it.",
        "The heart was left in the mummy: it would be weighed in the judgement."),
    logo("M23", "sw", "sedge plant", "king of Upper Egypt",
        "The sedge of Upper Egypt. In nswt, “king”, it comes first out of respect."),
    logo("L2", "bjt", "bee", "king of Lower Egypt; honey",
        "The bee of Lower Egypt. bjtj is the king as ruler of the north."),
    logo("U36", "ḥm", "fuller’s club", "majesty; servant",
        "A launderer’s club. ḥm=f, “his majesty”, is how texts refer to the king."),
    logo("U28", "wḏꜣ", "fire drill", "prosperity, soundness",
        "A bow drill for making fire. It writes wḏꜣ, “sound, prosperous”."),
    logo("X8", "dj", "conical loaf", "give",
        "A pointed loaf being handed over: “give”. It is the dj of ḥtp dj nswt."),
    logo("F4", "ḥꜣt", "forepart of a lion", "front, beginning",
        "A lion’s head and shoulders: the front of anything, and the start of things."),
    logo("O29", "ꜥꜣ", "wooden column", "great, large",
        "A wooden column. ꜥꜣ means “great” — as in nṯr ꜥꜣ, “the great god”."),
    logo("E9", "jw", "newborn hartebeest", "is (a particle)",
        "A newborn antelope. It writes jw, the little word that starts statements: “is”."),
    multi("E34", "wn", "hare", "exist, be",
        "The desert hare writes wn, “to exist” — as in “there was a man…”."),
    logo("O3", "prt-ḫrw", "house with bread and beer", "invocation offering",
        "Bread and a beer jug before a house: “going forth of the voice”, the spoken offering."),
    logo("W22", "ḥnqt", "beer jug", "beer",
        "A beer jug with a stopper. Beer was a staple food, brewed from barley bread."),
    logo("F1", "kꜣ", "ox head", "ox, cattle", "The head of an ox stands for the whole animal in offering lists."),
    logo("H1", "ꜣpd", "duck head", "bird, fowl", "A duck’s head stands for fowl in offering lists."),
    logo("F39", "jmꜣḫ", "spine and spinal cord", "reverence, honour",
        "A backbone with its cord. jmꜣḫy, “the revered one”, is what the dead hoped to be called."),
    logo("T18", "šms", "follower’s bundle", "follow",
        "A crook with a knife and bag: the gear of a royal follower."),
    logo("V10", "šnw", "cartouche", "royal name ring",
        "An oval loop of rope with its knot at the end, drawn around a king’s name.",
        "From šnj, “to encircle”: the name is held safe in eternity."),
    logo("Y3", "sš", "scribe’s palette", "scribe, writing",
        "Palette, water pot and brush holder slung together: the scribe’s kit, and the word for scribe."),
    multi("W24", "nw", "pot", null, "A round pot. It writes nw, as in Nut, the sky goddess."),
    logo("O10", "ḥwt-ḥr", "falcon in a house", "Hathor",
        "A falcon inside a building: ḥwt-ḥr, “the house of Horus” — the goddess Hathor."),
    logo("N35A", "mw", "three water ripples", "water", "Three ripples: water itself.", null, listOf("N35")),

    // ---- the commonest words -------------------------------------------------------------------
    logo("D2", "ḥr", "face", "face; on, upon",
        "A face seen from the front, which Egyptian art almost never drew. It writes ḥr, “face”, and the preposition ḥr, “on”.",
        "Not the falcon G5: the god Horus is also ḥr, but written with his bird.",
        listOf("D4", "D28")),
    logo("D35", "n", "arms of negation", "not, there is no",
        "Two arms flung out, palms down: the shrug of “no”. It writes the negation n, or nn.",
        null, listOf("D28", "D36")),
    logo("F21", "sḏm", "ox ear", "hear, listen",
        "An ox’s ear: sḏm, “to hear”. Grammar books name the basic Egyptian verb form after it: the sḏm=f, “he hears”.",
        null, listOf("F34")),
    det("D54", "walking legs", "coming and going",
        "Two legs striding to the right. After a verb it says the verb is about moving: come, go, walk, run.",
        listOf("D58")),

    // ---- numerals ------------------------------------------------------------------------------
    numeral("Z1", "stroke", "wꜥ", "One stroke for each unit. After a sign, a single stroke also says “this sign means what it shows”."),
    numeral("V20", "cattle hobble", "mḏ", "A hobble for cattle: ten."),
    numeral("V1", "coil of rope", "št", "A coil of measuring rope: a hundred."),
    numeral("M12", "lotus", "ḫꜣ", "The lotus, which carpeted the marshes in thousands."),
    numeral("D50", "raised finger", "ḏbꜥ", "A pointing finger: ten thousand."),
    numeral("I8", "tadpole", "ḥfn", "Tadpoles swarm the shallows by the hundred thousand."),
    numeral("C11", "god Heh", "ḥḥ",
        "Heh, god of infinity, kneels with arms raised. A million — and, in prayers, “millions of years”."),
)
