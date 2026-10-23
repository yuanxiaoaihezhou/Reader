package io.legado.app.help.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderChangelogTest {

    @Test
    fun `fallback changelog combines Reader history with upstream history`() {
        val result = ReaderChangelog.combineReaderAndUpstreamChangelog(
            "## Reader 1.0.0-beta.1\n\n- Reader 修复。",
            "# 更新日志\n\n**2026/10/22**\n- 上游修复。",
        )

        assertTrue(result.startsWith("# Reader 更新日志"))
        assertTrue(result.contains("Reader 1.0.0-beta.1"))
        assertTrue(result.contains("# Legado 上游更新日志"))
        assertTrue(result.contains("上游修复"))
        assertFalse(result.contains("# Legado 上游更新日志\n\n# 更新日志"))
    }
}
