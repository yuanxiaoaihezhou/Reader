package io.legado.app.help.http

import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudflareChallengeTest {
    private fun response(value: String?, code: Int = 403) = Response.Builder()
        .request(Request.Builder().url("https://example.org/chap/1").build())
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("response")
        .apply { if (value != null) header("cf-mitigated", value) }
        .build()

    @Test fun `official header case and whitespace are handled`() {
        assertTrue(CloudflareChallenge.isChallenge(response("challenge")))
        assertTrue(CloudflareChallenge.isChallenge(response(" Challenge ", 200)))
        assertTrue(CloudflareChallenge.isChallenge(response("CHALLENGE", 503)))
    }

    @Test fun `do not infer a challenge from HTTP status alone`() {
        assertFalse(CloudflareChallenge.isChallenge(response(null)))
        assertFalse(CloudflareChallenge.isChallenge(response("something-else")))
        assertFalse(CloudflareChallenge.isChallenge(response(null, 503)))
    }

    private fun eligible(
        method: RequestMethod = RequestMethod.GET,
        source: Boolean = true,
        book: Boolean = true,
        webView: Boolean = false,
        test: Boolean = false,
        download: Boolean = false,
        proxy: Boolean = false,
        bulk: Boolean = false,
        main: Boolean = false,
        challenge: Boolean = true,
    ): Boolean = CloudflareChallenge.shouldOfferVerification(
        challenge, method, source, book, webView, test, download, proxy, bulk, main
    )

    @Test fun `only interactive book GET is eligible`() {
        assertTrue(eligible())
        assertFalse(eligible(challenge = false))
        assertFalse(eligible(method = RequestMethod.POST))
        assertFalse(eligible(method = RequestMethod.HEAD))
        assertFalse(eligible(source = false))
        assertFalse(eligible(book = false))
    }

    @Test fun `no automatic WebView in background bulk download test or proxy`() {
        assertFalse(eligible(webView = true))
        assertFalse(eligible(test = true))
        assertFalse(eligible(download = true))
        assertFalse(eligible(proxy = true))
        assertFalse(eligible(bulk = true))
        assertFalse(eligible(main = true))
    }
}
