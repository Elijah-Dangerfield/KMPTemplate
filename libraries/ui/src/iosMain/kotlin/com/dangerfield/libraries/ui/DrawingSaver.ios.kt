package com.dangerfield.libraries.ui

import androidx.compose.ui.graphics.Color
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import platform.CoreGraphics.CGContextAddQuadCurveToPoint
import platform.CoreGraphics.CGContextBeginPath
import platform.CoreGraphics.CGContextMoveToPoint
import platform.CoreGraphics.CGContextSetLineCap
import platform.CoreGraphics.CGContextSetLineJoin
import platform.CoreGraphics.CGContextSetLineWidth
import platform.CoreGraphics.CGContextSetRGBFillColor
import platform.CoreGraphics.CGContextSetRGBStrokeColor
import platform.CoreGraphics.CGContextStrokePath
import platform.CoreGraphics.CGPathCreateMutable
import platform.CoreGraphics.CGPathMoveToPoint
import platform.CoreGraphics.CGPathAddQuadCurveToPoint
import platform.CoreGraphics.CGPathAddLineToPoint
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.kCGLineCapRound
import platform.CoreGraphics.kCGLineJoinRound
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSUUID
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class IosDrawingSaver @Inject constructor() : DrawingSaver {

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun saveDrawing(
        strokes: List<DrawingStrokeData>,
        width: Int,
        height: Int,
        backgroundColor: Color,
    ): String? = withContext(Dispatchers.IO) {
        try {
            val size = platform.CoreGraphics.CGSizeMake(width.toDouble(), height.toDouble())
            
            UIGraphicsBeginImageContextWithOptions(size, false, 1.0)
            val context = UIGraphicsGetCurrentContext() ?: run {
                UIGraphicsEndImageContext()
                return@withContext null
            }
            
            // Fill background
            CGContextSetRGBFillColor(
                context,
                backgroundColor.red.toDouble(),
                backgroundColor.green.toDouble(),
                backgroundColor.blue.toDouble(),
                backgroundColor.alpha.toDouble()
            )
            platform.CoreGraphics.CGContextFillRect(
                context,
                CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble())
            )
            
            // Draw strokes
            for (stroke in strokes) {
                if (stroke.points.size < 2) continue
                
                val strokeColor = if (stroke.isEraser) backgroundColor else stroke.color
                
                CGContextSetRGBStrokeColor(
                    context,
                    strokeColor.red.toDouble(),
                    strokeColor.green.toDouble(),
                    strokeColor.blue.toDouble(),
                    strokeColor.alpha.toDouble()
                )
                CGContextSetLineWidth(context, stroke.strokeWidth.toDouble())
                CGContextSetLineCap(context, kCGLineCapRound)
                CGContextSetLineJoin(context, kCGLineJoinRound)
                
                CGContextBeginPath(context)
                
                val first = stroke.points.first()
                CGContextMoveToPoint(context, first.x.toDouble(), first.y.toDouble())
                
                for (i in 1 until stroke.points.size) {
                    val point = stroke.points[i]
                    val prev = stroke.points[i - 1]
                    
                    val midX = (prev.x + point.x) / 2.0
                    val midY = (prev.y + point.y) / 2.0
                    CGContextAddQuadCurveToPoint(
                        context,
                        prev.x.toDouble(),
                        prev.y.toDouble(),
                        midX,
                        midY
                    )
                }
                
                val last = stroke.points.last()
                platform.CoreGraphics.CGContextAddLineToPoint(
                    context,
                    last.x.toDouble(),
                    last.y.toDouble()
                )
                
                CGContextStrokePath(context)
            }
            
            val image = UIGraphicsGetImageFromCurrentImageContext()
            UIGraphicsEndImageContext()
            
            if (image == null) return@withContext null
            
            val pngData = UIImagePNGRepresentation(image) ?: return@withContext null
            
            // Get documents directory
            val paths = NSSearchPathForDirectoriesInDomains(
                NSDocumentDirectory,
                NSUserDomainMask,
                true
            )
            val documentsDir = paths.firstOrNull() as? String ?: return@withContext null
            
            val fileName = "drawing_${NSUUID().UUIDString}.png"
            val filePath = "$documentsDir/drawings"
            
            // Create drawings directory if needed
            val fileManager = platform.Foundation.NSFileManager.defaultManager
            if (!fileManager.fileExistsAtPath(filePath)) {
                fileManager.createDirectoryAtPath(filePath, true, null, null)
            }
            
            val fullPath = "$filePath/$fileName"
            val success = pngData.writeToFile(fullPath, true)
            
            if (success) fullPath else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
