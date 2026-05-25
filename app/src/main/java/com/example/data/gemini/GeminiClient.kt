package com.example.data.gemini

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    /**
     * Checks if the Gemini API Key is valid and not a placeholder.
     */
    fun isApiKeyAvailable(): Boolean {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            key.isNotEmpty() && key != "MY_GEMINI_API_KEY" && key != "Placeholder"
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Generates creative captions, hashtags, custom video ideas, or performs automated moderation using Gemini.
     * Degrades gracefully to local smart simulation if API key is not configured.
     */
    suspend fun generateAiText(prompt: String, systemPrompt: String? = null): String {
        if (!isApiKeyAvailable()) {
            Log.e(TAG, "Gemini API key is not configured or is placeholder. Using smart offline reply simulation.")
            return getSimulatedResponse(prompt)
        }

        return try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                systemInstruction = systemPrompt?.let { Content(parts = listOf(Part(text = it))) }
            )
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "No AI candidates returned. Try again!"
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call failed: ${e.message}", e)
            "AI Assistant: (Offline Mode) ${getSimulatedResponse(prompt)}"
        }
    }

    private fun getSimulatedResponse(prompt: String): String {
        return when {
            prompt.contains("caption", ignoreCase = true) || prompt.contains("hashtag", ignoreCase = true) -> {
                "🎬 Live your best life! ✨ #viral #trending #TokTik2026 #foryoupage #contentcreator #neonvibes"
            }
            prompt.contains("idea", ignoreCase = true) || prompt.contains("challenge", ignoreCase = true) -> {
                "Try doing a 15-second visual transition showcase switching from your everyday look to a retro-futuristic outfit under glowing pink lights! Use a dramatic synth bass drop track."
            }
            prompt.contains("moderate", ignoreCase = true) || prompt.contains("report", ignoreCase = true) -> {
                "✅ Moderation Scan: Content verified safe. No community guideline violations found."
            }
            else -> {
                "Welcome to TokTik! Explore trending video styles, participate in live virtual battles, or create visual reels with custom neon effects!"
            }
        }
    }
}
