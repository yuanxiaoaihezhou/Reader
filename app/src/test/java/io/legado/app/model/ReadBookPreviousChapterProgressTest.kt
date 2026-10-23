package io.legado.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReadBookPreviousChapterProgressTest {

    @Test
    fun `pending previous chapter end is never persistable`() {
        assertNull(persistableReadPosition(Int.MAX_VALUE))
        assertEquals(123, persistableReadPosition(123))
    }

    @Test
    fun `pending previous chapter end normalizes only after layout completes`() {
        assertEquals(
            Int.MAX_VALUE,
            normalizePendingPreviousChapterEndPosition(
                position = Int.MAX_VALUE,
                chapterCompleted = false,
                lastReadLength = 420,
            ),
        )
        assertEquals(
            420,
            normalizePendingPreviousChapterEndPosition(
                position = Int.MAX_VALUE,
                chapterCompleted = true,
                lastReadLength = 420,
            ),
        )
        assertEquals(
            100,
            normalizePendingPreviousChapterEndPosition(
                position = 100,
                chapterCompleted = true,
                lastReadLength = 420,
            ),
        )
    }

    @Test
    fun `negative last read length is normalized safely`() {
        assertEquals(
            0,
            normalizePendingPreviousChapterEndPosition(
                position = Int.MAX_VALUE,
                chapterCompleted = true,
                lastReadLength = -1,
            ),
        )
    }

    @Test
    fun `previous chapter navigation waits for a completed layout before using last position`() {
        val source = projectFile("src/main/java/io/legado/app/model/ReadBook.kt").readText()
        val previous = source.substringAfter("fun moveToPrevChapter(")
            .substringBefore("fun skipToPage(")

        assertTrue(previous.contains("?.takeIf { it.isCompleted }"))
        assertTrue(previous.contains("previousLastReadLength ?: PENDING_PREVIOUS_CHAPTER_END_POSITION"))
    }

    @Test
    fun `pending end marker is filtered from book and read record persistence`() {
        val source = projectFile("src/main/java/io/legado/app/model/ReadBook.kt").readText()
        val saveRead = source.substringAfter("fun saveRead(pageChanged: Boolean = false)")
            .substringBefore("private fun preDownload()")
        val upReadTime = source.substringAfter("fun upReadTime()")
            .substringBefore("fun upMsg(")
        val uploadProgress = source.substringAfter("fun uploadProgress(")
            .substringBefore("fun syncProgress(")
        val syncProgress = source.substringAfter("fun syncProgress(")
            .substringBefore("private fun resetReadRecord(")

        assertTrue(saveRead.contains("val persistableDurChapterPos = persistableReadPosition(durChapterPos)"))
        assertTrue(saveRead.contains("if (persistableDurChapterPos != null)"))
        assertTrue(upReadTime.contains("persistableReadPosition(durChapterPos)?.let"))
        assertTrue(uploadProgress.contains("persistableReadPosition(durChapterPos) == null"))
        assertTrue(syncProgress.contains("persistableReadPosition(durChapterPos) == null"))
    }

    @Test
    fun `completed current chapter repairs pending end progress in both load paths`() {
        val source = projectFile("src/main/java/io/legado/app/model/ReadBook.kt").readText()

        assertTrue(
            source.lines().count {
                it.contains("if (normalizePendingPreviousChapterEnd(textChapter)) saveRead()")
            } >= 6,
        )
    }

    private fun projectFile(pathInApp: String): File =
        sequenceOf(File(pathInApp), File("app/$pathInApp"))
            .first(File::isFile)
}
