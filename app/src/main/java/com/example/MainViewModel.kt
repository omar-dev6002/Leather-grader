package com.example

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.*
import com.example.history.AppDatabase
import com.example.history.GradingRepository
import com.example.history.GradingResultEntity
import com.example.model.GradingResult
import com.example.utils.ImageUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: GradingRepository
    val history: StateFlow<List<GradingResultEntity>>

    init {
        val dao = AppDatabase.getDatabase(application).dao()
        repository = GradingRepository(dao)
        history = repository.allResults.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    private val _currentResult = MutableStateFlow<GradingResult?>(null)
    val currentResult: StateFlow<GradingResult?> = _currentResult

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val isBatchMode = MutableStateFlow(false)
    
    private val _currentBatch = MutableStateFlow<List<GradingResultEntity>>(emptyList())
    val currentBatch: StateFlow<List<GradingResultEntity>> = _currentBatch

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun startBatch() {
        isBatchMode.value = true
        _currentBatch.value = emptyList()
    }

    fun endBatch() {
        isBatchMode.value = false
        _currentBatch.value = emptyList()
    }

    fun gradeLeather(context: Context, imageUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _currentResult.value = null

            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    _error.value = "Gemini API Key is missing. Please configure it in the Secrets panel."
                    return@launch
                }

                val base64Image = ImageUtils.uriToBase64(context, imageUri)
                if (base64Image == null) {
                    _error.value = "Failed to process image."
                    return@launch
                }

                val prompt = """
                    You are an expert human leather inspector. Grade this finished leather crust into Grade A, Grade B, or Grade C based on:
                    - Surface uniformity
                    - Grain texture consistency
                    - Presence of defects (scratches, scars, holes, wrinkles, loose grain)
                    - Color consistency and finishing quality
                    
                    Grade A = high quality, minimal defects
                    Grade B = moderate defects, acceptable quality
                    Grade C = significant defects, low quality
                    
                    Respond strictly in JSON format matching this schema:
                    { "grade": "A" | "B" | "C", "confidence": 0.0 to 1.0, "reasoning": "brief explanation" }
                """.trimIndent()

                val requestBody = GenerateContentRequest(
                    contents = listOf(
                        Content(
                            parts = listOf(
                                Part(text = prompt),
                                Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                            )
                        )
                    )
                )

                // The generateContent returns a JSON string now instead of GenerateContentResponse 
                // Wait, need to fix the Retrofit interface. I should just use raw String response or Response model.
                // Let's use the actual Response Model!
                val jsonResponseString = RetrofitClient.service.generateContent(apiKey, requestBody)
                
                // Parse the response format
                val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                val responseAdapter = moshi.adapter(GenerateContentResponse::class.java)
                val geminiResponse = responseAdapter.fromJson(jsonResponseString)
                
                val textResponse = geminiResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                
                if (textResponse != null) {
                    // Extract json from markdown block if any
                    val cleanJson = textResponse.substringAfter("```json").substringBefore("```").trim()
                    val resultAdapter = moshi.adapter(GradingResult::class.java)
                    val result = resultAdapter.fromJson(cleanJson)
                    _currentResult.value = result ?: throw Exception("Failed to parse result JSON.")
                } else {
                    _error.value = "Empty response from Gemini API"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveResult(imageUri: Uri, grade: String, confidence: Float, reasoning: String) {
        viewModelScope.launch {
            val entity = GradingResultEntity(
                imageUri = imageUri.toString(),
                grade = grade,
                confidence = confidence,
                reasoning = reasoning
            )
            repository.insertResult(entity)
            if (isBatchMode.value) {
                _currentBatch.value = _currentBatch.value + entity
            }
        }
    }
}
