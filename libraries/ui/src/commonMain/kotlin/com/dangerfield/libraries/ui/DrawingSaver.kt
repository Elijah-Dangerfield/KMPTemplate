package com.dangerfield.libraries.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * Platform-specific interface for saving drawings to image files.
 */
interface DrawingSaver {
    /**
     * Saves the given strokes as a PNG image.
     * 
     * @param strokes The list of strokes to render
     * @param width The width of the canvas in pixels
     * @param height The height of the canvas in pixels
     * @param backgroundColor The background color of the canvas
     * @return The file path where the image was saved, or null if saving failed
     */
    suspend fun saveDrawing(
        strokes: List<DrawingStrokeData>,
        width: Int,
        height: Int,
        backgroundColor: Color = Color.White,
    ): String?
}

/**
 * Data class representing a stroke for saving.
 * Decoupled from the ViewModel's DrawingStroke to avoid tight coupling.
 */
data class DrawingStrokeData(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val isEraser: Boolean,
)
