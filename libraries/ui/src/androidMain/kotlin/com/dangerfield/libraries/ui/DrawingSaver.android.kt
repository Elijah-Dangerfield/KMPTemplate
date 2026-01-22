package com.dangerfield.libraries.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AndroidDrawingSaver @Inject constructor(
    private val context: Context,
) : DrawingSaver {

    override suspend fun saveDrawing(
        strokes: List<DrawingStrokeData>,
        width: Int,
        height: Int,
        backgroundColor: Color,
    ): String? = withContext(Dispatchers.IO) {
        try {
            // Create bitmap
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            // Fill background
            canvas.drawColor(backgroundColor.toArgb())
            
            // Draw each stroke
            val paint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            
            for (stroke in strokes) {
                if (stroke.points.size < 2) continue
                
                paint.color = if (stroke.isEraser) {
                    backgroundColor.toArgb()
                } else {
                    stroke.color.toArgb()
                }
                paint.strokeWidth = stroke.strokeWidth
                
                val path = Path()
                val first = stroke.points.first()
                path.moveTo(first.x, first.y)
                
                for (i in 1 until stroke.points.size) {
                    val point = stroke.points[i]
                    val prev = stroke.points[i - 1]
                    
                    // Use quadratic bezier for smooth curves
                    val midX = (prev.x + point.x) / 2
                    val midY = (prev.y + point.y) / 2
                    path.quadTo(prev.x, prev.y, midX, midY)
                }
                
                val last = stroke.points.last()
                path.lineTo(last.x, last.y)
                
                canvas.drawPath(path, paint)
            }
            
            // Save to file
            val fileName = "drawing_${UUID.randomUUID()}.png"
            val drawingsDir = File(context.filesDir, "drawings")
            drawingsDir.mkdirs()
            val file = File(drawingsDir, fileName)
            
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            
            bitmap.recycle()
            
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
