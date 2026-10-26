package io.legado.app.ui.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CloudflareWebViewCompatibilityTest {
    private fun source(relative: String) = listOf(File(relative), File("app/$relative"))
        .first { it.isFile }.readText()

    @Test fun `verification and login WebViews permit third party cookies`() {
        val browser = source("src/main/java/io/legado/app/ui/browser/WebViewActivity.kt")
        val login = source("src/main/java/io/legado/app/ui/login/WebViewLoginFragment.kt")
        assertTrue(browser.contains("setAcceptThirdPartyCookies(currentWebView, true)"))
        assertTrue(login.contains("setAcceptThirdPartyCookies(webView, true)"))
    }

    @Test fun `pool resets third party cookie policy on acquire and release`() {
        val pool = source("src/main/java/io/legado/app/help/webView/WebViewPool.kt")
        assertTrue(pool.contains("setAcceptThirdPartyCookies(realWebView, false)"))
        assertTrue(pool.contains("setAcceptThirdPartyCookies(this, false)"))
    }

    @Test fun `cloudflare success requires a fresh HTTP check`() {
        val model = source("src/main/java/io/legado/app/ui/browser/WebViewModel.kt")
        val activity = source("src/main/java/io/legado/app/ui/browser/WebViewActivity.kt")
        assertTrue(model.contains("CloudflareChallenge.isChallenge(response.raw)"))
        assertTrue(model.contains("response.raw.code !in 200..399"))
        assertTrue(activity.contains("isCloudflareChallenge = intent.getBooleanExtra"))
        assertTrue(activity.contains("!isCheckingCloudflareVerification"))
        val analyze = source("src/main/java/io/legado/app/model/analyzeRule/AnalyzeUrl.kt")
        assertTrue(analyze.contains("cloudflareVerification = true"))
        assertTrue(analyze.contains("httpResponse = sendRequest() // Exactly one retry"))
        assertFalse(analyze.contains("while (CloudflareChallenge"))
    }
}
