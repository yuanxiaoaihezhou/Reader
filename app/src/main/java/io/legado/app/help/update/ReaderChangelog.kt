package io.legado.app.help.update

import android.content.res.AssetManager

object ReaderChangelog {

    private const val RELEASE_NOTES_ASSET = "readerReleaseNotes.md"
    private const val CHANGELOG_ASSET = "readerChangelog.md"
    private const val CHANGELOG_SEED_ASSET = "readerChangelogSeed.md"
    private const val UPSTREAM_CHANGELOG_ASSET = "updateLog.md"

    fun readReleaseNotes(assets: AssetManager): String {
        return readOptional(assets, RELEASE_NOTES_ASSET)
            ?: readFullChangelog(assets)
    }

    fun readFullChangelog(assets: AssetManager): String {
        return readOptional(assets, CHANGELOG_ASSET)
            ?: combineReaderAndUpstreamChangelog(
                readOptional(assets, CHANGELOG_SEED_ASSET).orEmpty(),
                readOptional(assets, UPSTREAM_CHANGELOG_ASSET).orEmpty(),
            )
    }

    internal fun combineReaderAndUpstreamChangelog(
        readerHistory: String,
        upstreamHistory: String,
    ): String {
        val reader = readerHistory.trim()
        val upstream = upstreamHistory
            .trimStart('\uFEFF', '\r', '\n', ' ')
            .removePrefix("# 更新日志")
            .trim()
        return buildString {
            append("# Reader 更新日志")
            if (reader.isNotEmpty()) {
                append("\n\n")
                append(reader)
            }
            if (upstream.isNotEmpty()) {
                append("\n\n# Legado 上游更新日志\n\n")
                append("> 以下内容随 Reader 的上游同步保留，便于查看继承自 Legado 的更新。")
                append("\n\n")
                append(upstream)
            }
            append('\n')
        }
    }

    private fun readOptional(assets: AssetManager, fileName: String): String? {
        return runCatching {
            assets.open(fileName).bufferedReader().use { it.readText() }
                .takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
