package com.example.viewmodel

import com.example.model.CanvasDimensions
import com.example.model.Layer

data class HistorySnapshot(
    val layers: List<Layer>,
    val selectedLayerId: String?,
    val canvasDimensions: CanvasDimensions,
    val canvasDpi: Int = 72
)
