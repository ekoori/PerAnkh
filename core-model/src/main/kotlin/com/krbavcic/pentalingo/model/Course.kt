package com.krbavcic.pentalingo.model

/** The painted colours units are told apart by on the path. */
enum class UnitColor { LAPIS, OCHRE, TURQUOISE, CARNELIAN, GOLD, MALACHITE }

/**
 * One stop on the path.
 *
 * @property teach the items this lesson introduces, in the order they are taught. Each gets a
 *   card the first time it is met; a lesson replayed later reviews them instead.
 * @property practice items this lesson exercises whether or not they are due — the old signs a
 *   new word is built from, for instance.
 * @property reading a text read as the lesson's finale.
 * @property keySign the sign drawn on the lesson's medallion.
 */
data class Lesson(
    val id: String,
    val title: String,
    val focus: String,
    val keySign: String,
    val teach: List<String> = emptyList(),
    val practice: List<String> = emptyList(),
    val reading: String? = null,
    /** A checkpoint reviews its unit at full difficulty and teaches nothing new. */
    val checkpoint: Boolean = false,
)

data class CourseUnit(
    val id: String,
    val title: String,
    val subtitle: String,
    val color: UnitColor,
    /** A short scene-setting paragraph shown on the unit's banner. */
    val intro: String,
    val lessons: List<Lesson>,
)

/**
 * A real text, for the library.
 *
 * Texts are the point of the whole course: the measure of progress that matters is not how many
 * lessons are done but which real inscriptions can now be read. Each line is a [PhraseItem], so
 * the lesson that reads a text teaches its lines like any other phrase and the memory of reading
 * them fades and is refreshed like everything else.
 */
data class ReadingText(
    val id: String,
    val title: String,
    /** Where it comes from: monument, papyrus, period. */
    val source: String,
    val lines: List<String>,
    val note: String,
)

data class Course(
    val units: List<CourseUnit>,
    val items: List<Item>,
    val texts: List<ReadingText>,
    val awards: List<Award>,
) {
    private val byId: Map<String, Item> = items.associateBy { it.id }

    operator fun get(id: String): Item? = byId[id]

    fun require(id: String): Item = byId[id] ?: throw IllegalArgumentException("No item $id")

    val lessons: List<Lesson> get() = units.flatMap { it.lessons }

    fun lesson(id: String): Lesson? = lessons.firstOrNull { it.id == id }

    fun unitOf(lessonId: String): CourseUnit? = units.firstOrNull { u -> u.lessons.any { it.id == lessonId } }

    fun text(id: String): ReadingText? = texts.firstOrNull { it.id == id }

    val signs: List<SignItem> get() = items.filterIsInstance<SignItem>()

    val words: List<WordItem> get() = items.filterIsInstance<WordItem>()

    /** The sign item teaching a Gardiner code, if the course teaches it. */
    fun signFor(code: String): SignItem? = signs.firstOrNull { it.code == code }

    /** The lesson that first teaches an item. */
    fun lessonTeaching(itemId: String): Lesson? = lessons.firstOrNull { itemId in it.teach }
}
