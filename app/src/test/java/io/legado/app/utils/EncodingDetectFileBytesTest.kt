package io.legado.app.utils

import org.junit.Assert.assertArrayEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.charset.Charset

class EncodingDetectFileBytesTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun sample(bytes: ByteArray): ByteArray {
        val file = temporaryFolder.newFile()
        file.writeBytes(bytes)
        val method = EncodingDetect::class.java.getDeclaredMethod("getFileBytes", File::class.java)
        method.isAccessible = true
        return method.invoke(EncodingDetect, file) as ByteArray
    }

    @Test
    fun `ASCII content is not discarded`() {
        val bytes = "Plain ASCII text 123\n".toByteArray(Charsets.US_ASCII)
        assertArrayEquals(bytes, sample(bytes))
    }

    @Test
    fun `UTF-8 content preserves ASCII and multibyte sequences`() {
        val bytes = "Title: 中文\nChapter 1: 阅读".toByteArray(Charsets.UTF_8)
        assertArrayEquals(bytes, sample(bytes))
    }

    @Test
    fun `GBK content preserves the original byte sequence`() {
        val bytes = "书名: 测试章节\n123".toByteArray(Charset.forName("GBK"))
        assertArrayEquals(bytes, sample(bytes))
    }

    @Test
    fun `empty and short files retain their exact lengths`() {
        assertArrayEquals(byteArrayOf(), sample(byteArrayOf()))
        assertArrayEquals(byteArrayOf(0, 65, -1), sample(byteArrayOf(0, 65, -1)))
    }

    @Test
    fun `sampling stops after eight thousand bytes`() {
        val bytes = ByteArray(8123) { index -> (index * 37).toByte() }
        assertArrayEquals(bytes.copyOf(8000), sample(bytes))
    }
}
