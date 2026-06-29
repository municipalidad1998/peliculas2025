package com.cloudstreamext.util

import com.lagradost.cloudstream3.app
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder

/**
 * Utility class for common network operations.
 * Wraps CloudStream's built-in HTTP client with additional helpers.
 *
 * Usage:
 * ```kotlin
 * val network = NetworkUtils(client)
 * val response = network.getJson("https://api.example.com/data")
 * val postResult = network.postJson("https://api.example.com/submit", jsonData)
 * ```
 */
class NetworkUtils(private val client: OkHttpClient? = null) {

    /**
     * HTTP response wrapper.
     */
    data class ApiResponse(
        val code: Int,
        val body: String,
        val headers: Map<String, String>,
        val isSuccessful: Boolean
    ) {
        fun toJson(): com.google.gson.JsonObject? {
            return com.cloudstreamext.util.JsonParser.parseObject(body)
        }

        fun toDocument(): org.jsoup.nodes.Document {
            return org.jsoup.Jsoup.parse(body)
        }
    }

    /**
     * Makes a GET request and returns the response body as a string.
     */
    suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap(),
        cookies: Map<String, String> = emptyMap()
    ): ApiResponse? {
        return try {
            val cookieHeader = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
            val allHeaders = headers.toMutableMap()
            if (cookieHeader.isNotBlank()) {
                allHeaders["Cookie"] = cookieHeader
            }
            val response = app.get(url, headers = allHeaders)
            ApiResponse(
                code = response.code,
                body = response.text,
                headers = response.okhttpResponse?.headers?.toMap() ?: emptyMap(),
                isSuccessful = response.isSuccessful
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Makes a POST request with form data.
     */
    suspend fun postForm(
        url: String,
        formData: Map<String, String>,
        headers: Map<String, String> = emptyMap()
    ): ApiResponse? {
        return try {
            val response = app.post(
                url,
                headers = headers,
                data = formData
            )
            ApiResponse(
                code = response.code,
                body = response.text,
                headers = response.okhttpResponse?.headers?.toMap() ?: emptyMap(),
                isSuccessful = response.isSuccessful
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Makes a POST request with JSON body.
     */
    suspend fun postJson(
        url: String,
        jsonBody: String,
        headers: Map<String, String> = emptyMap()
    ): ApiResponse? {
        return try {
            val response = app.post(
                url,
                headers = headers + mapOf("Content-Type" to "application/json"),
                data = jsonBody
            )
            ApiResponse(
                code = response.code,
                body = response.text,
                headers = response.okhttpResponse?.headers?.toMap() ?: emptyMap(),
                isSuccessful = response.isSuccessful
            )
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        /**
         * URL-encodes a string.
         */
        fun encodeUrl(text: String): String {
            return URLEncoder.encode(text, "UTF-8")
        }

        /**
         * Decodes a URL-encoded string.
         */
        fun decodeUrl(text: String): String {
            return java.net.URLDecoder.decode(text, "UTF-8")
        }

        /**
         * Builds a query string from parameters.
         */
        fun buildQueryString(params: Map<String, String>): String {
            return params.entries.joinToString("&") { (key, value) ->
                "${encodeUrl(key)}=${encodeUrl(value)}"
            }
        }

        /**
         * Appends query parameters to a URL.
         */
        fun appendParams(url: String, params: Map<String, String>): String {
            if (params.isEmpty()) return url
            val separator = if (url.contains("?")) "&" else "?"
            return "$url$separator${buildQueryString(params)}"
        }

        /**
         * Removes query parameters from a URL.
         */
        fun removeParams(url: String, vararg paramNames: String): String {
            var result = url
            for (name in paramNames) {
                result = result.replace(Regex("[?&]$name=[^&]*"), "")
                result = result.replace(Regex("&$name=[^&]*"), "")
            }
            return result
        }

        /**
         * Extracts the base URL (without path/params) from a full URL.
         */
        fun getBaseUrl(url: String): String {
            return try {
                val uri = java.net.URI(url)
                "${uri.scheme}://${uri.host}"
            } catch (e: Exception) {
                val match = Regex("^(https?://[^/]+)").find(url)
                match?.groupValues?.get(1) ?: url
            }
        }

        /**
         * Extracts the domain from a URL.
         */
        fun getDomain(url: String): String {
            return try {
                java.net.URI(url).host ?: ""
            } catch (e: Exception) {
                ""
            }
        }

        /**
         * Checks if a URL is absolute.
         */
        fun isAbsoluteUrl(url: String): Boolean {
            return url.startsWith("http://") || url.startsWith("https://")
        }

        /**
         * Converts a relative URL to absolute using a base URL.
         */
        fun toAbsoluteUrl(base: String, relative: String): String {
            if (isAbsoluteUrl(relative)) return relative
            if (relative.startsWith("//")) return "https:$relative"
            val baseUrl = base.trimEnd('/')
            val relPath = relative.trimStart('/')
            return "$baseUrl/$relPath"
        }
    }
}
