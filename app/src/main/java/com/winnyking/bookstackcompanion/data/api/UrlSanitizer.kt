package com.winnyking.bookstackcompanion.data.api

import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Utility helper to normalize BookStack Base URLs.
 * Example:
 *  " http://192.168.1.2:6875/ " -> "http://192.168.1.2:6875"
 *  "http://example.com/api/" -> "http://example.com"
 *  "example.com" -> "https://example.com"
 */
object UrlSanitizer {
    fun sanitizeBaseUrl(inputUrl: String): String {
        var url = inputUrl.trim()
        if (url.isBlank()) return ""

        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            url = "https://$url"
        }

        while (url.endsWith("/")) {
            url = url.substring(0, url.length - 1)
        }

        if (url.endsWith("/api", ignoreCase = true)) {
            url = url.substring(0, url.length - 4)
        }

        while (url.endsWith("/")) {
            url = url.substring(0, url.length - 1)
        }

        return url
    }

    /**
     * Helper to format human-readable error messages including the exact cleaned URL tested.
     */
    fun formatErrorMessage(baseUrl: String, exception: Throwable): String {
        val sanitized = sanitizeBaseUrl(baseUrl)
        val fullTestedUrl = if (sanitized.isNotEmpty()) "$sanitized/api/books" else baseUrl

        return when (exception) {
            is HttpException -> {
                val code = exception.code()
                when (code) {
                    401 -> "Erreur 401 (Non autorisé) sur $fullTestedUrl : Identifiants ou jetons API invalides."
                    403 -> "Erreur 403 (Accès interdit) sur $fullTestedUrl : Privilèges insuffisants pour le jeton."
                    404 -> "Erreur 404 (Introuvable) sur $fullTestedUrl : Vérifiez l'URL du serveur BookStack."
                    500 -> "Erreur 500 (Erreur interne du serveur) sur $fullTestedUrl."
                    else -> "Erreur HTTP $code sur $fullTestedUrl : ${exception.message()}"
                }
            }
            is UnknownHostException -> {
                "Hôte introuvable pour $fullTestedUrl. Vérifiez l'adresse IP ou le nom de domaine."
            }
            is ConnectException -> {
                "Impossible de se connecter à $fullTestedUrl. Vérifiez que le serveur est démarré et accessible sur le port spécifié."
            }
            is SocketTimeoutException -> {
                "Délai d'attente dépassé (Timeout) lors de la connexion à $fullTestedUrl."
            }
            is IOException -> {
                val msg = exception.message ?: ""
                if (msg.contains("CLEARTEXT", ignoreCase = true) || msg.contains("not permitted", ignoreCase = true)) {
                    "Trafic HTTP (en clair) non autorisé pour $fullTestedUrl. Veuillez autoriser le HTTP dans l'application."
                } else {
                    "Erreur réseau ($fullTestedUrl) : $msg"
                }
            }
            else -> {
                val msg = exception.localizedMessage ?: exception.message ?: "Erreur inconnue"
                "Erreur de connexion ($fullTestedUrl) : $msg"
            }
        }
    }
}
