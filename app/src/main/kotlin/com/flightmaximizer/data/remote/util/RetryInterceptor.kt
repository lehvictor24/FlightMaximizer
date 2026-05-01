package com.flightmaximizer.data.remote.util

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import kotlin.math.pow

class RetryInterceptor(private val maxRetries: Int = 3) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        var lastException: IOException? = null
        while (attempt <= maxRetries) {
            try {
                val response = chain.proceed(chain.request())
                if (response.isSuccessful || response.code == 404 || response.code == 429) return response
                if (attempt == maxRetries) return response
                response.close()
            } catch (e: IOException) {
                lastException = e
                if (attempt == maxRetries) throw e
            }
            Thread.sleep((1000L * (2.0.pow(attempt.toDouble())).toLong()).coerceAtMost(30_000L))
            attempt++
        }
        throw lastException ?: IOException("Max retries exceeded")
    }
}
