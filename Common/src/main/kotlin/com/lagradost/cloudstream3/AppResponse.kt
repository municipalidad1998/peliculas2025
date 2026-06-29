package com.lagradost.cloudstream3

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Stub AppResponse class for CloudStream 3 HTTP client.
 */
data class AppResponse(
    val code: Int = 200,
    val text: String = "",
    val okhttpResponse: okhttp3.Response? = null
) {
    val document: org.jsoup.nodes.Document
        get() = org.jsoup.Jsoup.parse(text)

    val isSuccessful: Boolean
        get() = code in 200..299
}

/**
 * Stub app object for CloudStream 3 HTTP client.
 */
object app {
    private val client: OkHttpClient = OkHttpClient.Builder().build()

    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): AppResponse {
        return try {
            val requestBuilder = Request.Builder().url(url)
            headers.forEach { (key, value) -> requestBuilder.header(key, value) }
            val response = client.newCall(requestBuilder.build()).execute()
            AppResponse(
                code = response.code,
                text = response.body?.string() ?: "",
                okhttpResponse = response
            )
        } catch (e: Exception) {
            AppResponse(code = 500, text = e.message ?: "Unknown error")
        }
    }

    suspend fun post(
        url: String,
        headers: Map<String, String> = emptyMap(),
        data: Any? = null
    ): AppResponse {
        return try {
            val body = when (data) {
                is Map<*, *> -> {
                    val formBuilder = okhttp3.FormBody.Builder()
                    data.forEach { (key, value) ->
                        if (key is String && value is String) {
                            formBuilder.add(key, value)
                        }
                    }
                    formBuilder.build()
                }
                is String -> {
                    data.toRequestBody("application/json; charset=utf-8".toMediaType())
                }
                else -> "".toRequestBody(null)
            }
            val requestBuilder = Request.Builder().url(url).post(body)
            headers.forEach { (key, value) -> requestBuilder.header(key, value) }
            val response = client.newCall(requestBuilder.build()).execute()
            AppResponse(
                code = response.code,
                text = response.body?.string() ?: "",
                okhttpResponse = response
            )
        } catch (e: Exception) {
            AppResponse(code = 500, text = e.message ?: "Unknown error")
        }
    }
}
