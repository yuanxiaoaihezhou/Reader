package io.legado.app.help.http

import okhttp3.Response

/**
 * Cloudflare documents cf-mitigated: challenge as the signal for a Challenge Page.
 * A status code (403 / 503) or HTML snippet alone is not sufficient evidence.
 * This helper never tries to solve or bypass a challenge.
 */
internal object CloudflareChallenge {

    fun isChallenge(response: Response): Boolean =
        response.header("cf-mitigated")?.trim()?.equals("challenge", ignoreCase = true) == true

    /** Deliberately conservative: only foreground-capable book reading requests may open UI. */
    fun shouldOfferVerification(
        challenge: Boolean,
        method: RequestMethod,
        hasSource: Boolean,
        isBookRequest: Boolean,
        usesWebView: Boolean,
        isTest: Boolean,
        isDownload: Boolean,
        hasSourceProxy: Boolean,
        isBulkOperation: Boolean,
        isMainThread: Boolean,
    ): Boolean = challenge &&
        method == RequestMethod.GET &&
        hasSource && isBookRequest &&
        !usesWebView && !isTest && !isDownload &&
        !hasSourceProxy && !isBulkOperation && !isMainThread
}
