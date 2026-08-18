package com.winnyking.bookstackcompanion

import com.winnyking.bookstackcompanion.data.api.UrlSanitizer
import org.junit.Assert.assertEquals
import org.junit.Test

class UrlSanitizerTest {

    @Test
    fun testUrlSanitization() {
        assertEquals("http://192.168.1.2:6875", UrlSanitizer.sanitizeBaseUrl(" http://192.168.1.2:6875/ "))
        assertEquals("http://192.168.1.2:6875", UrlSanitizer.sanitizeBaseUrl("http://192.168.1.2:6875/api"))
        assertEquals("http://192.168.1.2:6875", UrlSanitizer.sanitizeBaseUrl("http://192.168.1.2:6875/api/"))
        assertEquals("https://bookstack.example.com", UrlSanitizer.sanitizeBaseUrl("bookstack.example.com"))
        assertEquals("https://bookstack.example.com", UrlSanitizer.sanitizeBaseUrl("https://bookstack.example.com/api/"))
    }

    @Test
    fun testFormatErrorMessageContainsSanitizedUrl() {
        val exception = java.net.ConnectException("Failed to connect")
        val message = UrlSanitizer.formatErrorMessage("http://192.168.1.2:6875/api/", exception)
        assert(message.contains("http://192.168.1.2:6875/api/books"))
    }
}
