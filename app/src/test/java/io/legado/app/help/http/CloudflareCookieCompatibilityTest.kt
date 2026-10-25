package io.legado.app.help.http

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CloudflareCookieCompatibilityTest {
    private fun file(path: String) = listOf(File(path), File("app/$path"))
        .first { it.isFile }.readText()

    @Test fun `do not cap combined cookie header or randomly remove cookies`() {
        val source = file("src/main/java/io/legado/app/help/http/CookieStore.kt")
        val method = source.substringAfter("override fun getCookie(url: String)")
            .substringBefore("fun getKey(")
        assertFalse(method.contains("4096)"))
        assertFalse(method.contains("random()"))
        assertFalse(method.contains("removeCookie("))
        assertTrue(method.contains("mergeCookiesToMap(cookie, sessionCookie)"))
    }

    @Test fun `session cookies must never be globally deleted on WebView handoff`() {
        for (name in listOf("CookieStore.kt", "CookieManager.kt")) {
            val file = file("src/main/java/io/legado/app/help/http/$name")
            assertFalse("$name deleted all session cookies", file.contains("removeSessionCookies("))
        }
    }

    @Test fun `explicit cookie overrides persisted cookie even after retry`() {
        val store = CookieStore.cookieToMap(
            "cf_clearance=${"X".repeat(4050)}; session=keep; __cf_bm=keep2"
        )
        assertTrue(CookieStore.mapToCookie(store)!!.length > 4096)
        val manager = file("src/main/java/io/legado/app/help/http/CookieManager.kt")
        val analyze = file("src/main/java/io/legado/app/model/analyzeRule/AnalyzeUrl.kt")
        assertTrue(manager.contains("mergeCookies(cookie, requestCookie)"))
        assertTrue(analyze.contains("mergeCookies(cookie, explicitCookie)"))
        assertTrue(analyze.contains("setCookie()  // Never reuse the stale"))
    }
}
