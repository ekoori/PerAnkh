package com.krbavcic.pentalingo.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseCheckTest {

    private val pages = "https://github.com/ekoori/PerAnkh/releases"
    private val github = "https://api.github.com/repos/ekoori/PerAnkh/releases/latest"
    private val server = "https://perankh.hrana.app/v1/latest"

    private fun answer(tag: String = "v0.3.0", url: String = "$pages/tag/$tag", extra: String = "") =
        """{"tag_name": "$tag", "html_url": "$url", "name": "Per Ankh", "assets": [{"name": "PerAnkh.apk"}]$extra}"""

    @Test
    fun `GitHub's answer gives the version and its page`() {
        assertEquals(Release("0.3.0", "$pages/tag/v0.3.0"), parseRelease(answer(), pages))
    }

    @Test
    fun `drafts, pre-releases, other projects and nonsense do not count`() {
        assertNull(parseRelease(answer(extra = """, "draft": true"""), pages))
        assertNull(parseRelease(answer(extra = """, "prerelease": true"""), pages))
        assertNull(parseRelease(answer(url = "https://github.com/someone/else/releases/tag/v9.0.0"), pages))
        assertNull(parseRelease(answer(url = "https://example.com/PerAnkh.apk"), pages))
        assertNull(parseRelease(answer(tag = "latest"), pages))
        assertNull(parseRelease("""{"tag_name": {}, "html_url": 3}""", pages))
        assertNull(parseRelease("<html>rate limited</html>", pages))
        assertNull(parseRelease("[]", pages))
    }

    @Test
    fun `the server is asked only when GitHub cannot answer`() = runBlocking {
        val asked = mutableListOf<String>()
        fun releases(replies: Map<String, String?>) = ReleaseCheck(listOf(github, server), pages) { url ->
            asked += url
            replies[url]
        }

        assertEquals("0.3.0", releases(mapOf(github to answer(), server to answer("v0.2.0"))).latest()?.version)
        assertEquals(listOf(github), asked)

        asked.clear()
        assertEquals("0.2.0", releases(mapOf(github to null, server to answer("v0.2.0"))).latest()?.version)
        assertEquals(listOf(github, server), asked)

        // An answer that does not hold up is as good as none.
        assertEquals("0.2.0", releases(mapOf(github to "{}", server to answer("v0.2.0"))).latest()?.version)
        assertNull(releases(mapOf(github to null, server to null)).latest())
    }

    @Test
    fun `versions compare as numbers, pre-releases before their release`() {
        val ordered = listOf("0.2.0", "0.3.0-beta.2", "0.3.0-beta.10", "0.3.0", "0.10.0", "1.0.0")
        for (i in ordered.indices) {
            for (j in ordered.indices) {
                val order = compareVersions(ordered[i], ordered[j])
                assertTrue("${ordered[i]} vs ${ordered[j]}: $order", Integer.signum(order) == Integer.signum(i.compareTo(j)))
            }
        }
    }
}
