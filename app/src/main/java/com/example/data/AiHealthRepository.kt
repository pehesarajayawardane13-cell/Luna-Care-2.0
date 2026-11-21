package com.example.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val AI_TAG = "AiHealthRepository"

class AiHealthRepository {

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "You are a warm, supportive Sinhala-first personal health & menstrual cycle care assistant inside LunaCare. " +
                        "Respond primarily in clear, natural Sinhala (with familiar English medical terms in parentheses where helpful). " +
                        "Never diagnose diseases or prescribe drug dosages. Highlight red-flag symptoms (severe pain 4-5, fever, dizziness, fainting, chest pain, breathing issues, heavy prolonged bleeding) and advise consulting a qualified doctor. " +
                        "Include a brief reminder that this is informational guidance only. Keep answers concise, encouraging, and practical."
                )
            }
        )
    }

    fun streamHealthAdvice(
        contextJson: String,
        userQuestion: String
    ): Flow<String> = flow {
        val fullPrompt = "User Health Context:\n$contextJson\n\nUser Question:\n$userQuestion"
        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                model.generateContentStream(fullPrompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("AI assistant returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(AI_TAG, "Gemini streaming timed out", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_TAG, "Gemini AI generation failed", e)
            throw e
        }
    }
}
