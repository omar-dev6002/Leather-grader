package com.example.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GradingResult(
    val grade: String,
    val confidence: Float,
    val reasoning: String
)

