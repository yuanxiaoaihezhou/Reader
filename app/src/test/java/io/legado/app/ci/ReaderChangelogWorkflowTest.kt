package io.legado.app.ci

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReaderChangelogWorkflowTest {

    @Test
    fun `pull request template reserves a user visible Chinese changelog block`() {
        val template = repoFile(".github/pull_request_template.md").readText()
        assertTrue(template.contains("## 用户可见更新日志"))
        assertTrue(template.contains("<!-- reader-changelog:start -->"))
        assertTrue(template.contains("<!-- reader-changelog:end -->"))
        assertTrue(template.contains("无用户可见变化。"))
    }

    @Test
    fun `reader ci validates changelog before a pull request is merged`() {
        val workflow = repoFile(".github/workflows/reader-ci.yml").readText()
        assertTrue(workflow.contains("validate-reader-changelog"))
        assertTrue(workflow.contains("test-reader-changelog.py"))
        assertTrue(workflow.contains("reader-changelog.py validate"))
        assertTrue(workflow.contains("pull_request.user.type != 'Bot'"))
    }

    @Test
    fun `release workflows embed generated Reader notes into APKs`() {
        val beta = repoFile(".github/workflows/TestRelease.yml").readText()
        val stable = repoFile(".github/workflows/release.yml").readText()
        listOf(beta, stable).forEach { workflow ->
            assertTrue(workflow.contains("prepare-reader-release-notes.sh"))
            assertTrue(workflow.contains("reader-release-notes"))
            assertTrue(workflow.contains("app/src/main/assets/readerReleaseNotes.md"))
            assertTrue(workflow.contains("app/src/main/assets/readerChangelog.md"))
        }
    }

    @Test
    fun `upstream update log keeps real markdown line breaks`() {
        val updateLog = repoFile("app/src/main/assets/updateLog.md").readText()
        assertFalse(updateLog.contains("`r`n"))
    }

    @Test
    fun `app upgrade and about screens use Reader changelog assets`() {
        val main = repoFile("app/src/main/java/io/legado/app/ui/main/MainActivity.kt").readText()
        val about = repoFile("app/src/main/java/io/legado/app/ui/about/AboutFragment.kt").readText()
        assertTrue(main.contains("ReaderChangelog.readReleaseNotes(assets)"))
        assertTrue(about.contains("ReaderChangelog.readFullChangelog"))
    }

    @Test
    fun `release note resolver uses commit associated pull requests`() {
        val script = repoFile(".github/scripts/prepare-reader-release-notes.sh").readText()
        assertTrue(script.contains("commits/${'$'}{sha}/pulls"))
        assertTrue(script.contains("Bump\\ web"))
        assertTrue(script.contains("--allow-missing"))
    }

    private fun repoFile(path: String): File {
        val userDir = requireNotNull(System.getProperty("user.dir"))
        val root = generateSequence(File(userDir)) { it.parentFile }
            .first { File(it, "app/src/main").isDirectory }
        return File(root, path)
    }
}
