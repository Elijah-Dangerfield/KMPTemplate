package com.dangerfield.goodtimes.features.tasks.impl.templates.drawing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dangerfield.goodtimes.system.AppTheme
import com.dangerfield.goodtimes.system.Dimension
import com.dangerfield.goodtimes.system.VerticalSpacerD300
import com.dangerfield.goodtimes.system.VerticalSpacerD500
import com.dangerfield.libraries.ui.PreviewContent
import com.dangerfield.libraries.ui.components.button.Button
import com.dangerfield.libraries.ui.components.icon.IconButton
import com.dangerfield.libraries.ui.components.icon.Icons
import com.dangerfield.libraries.ui.components.text.Text
import org.jetbrains.compose.ui.tooling.preview.Preview

private val CANVAS_COLORS = listOf(
    Color.Black,
    Color.White,
    Color(0xFFE53935),
    Color(0xFFFF9800),
    Color(0xFFFFEB3B),
    Color(0xFF4CAF50),
    Color(0xFF2196F3),
    Color(0xFF9C27B0),
    Color(0xFF795548),
    Color(0xFF607D8B),
)

@Composable
fun DrawingScreen(
    state: DrawingState,
    onAction: (DrawingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isMultiTouch by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        // Full-screen white canvas
        DrawingCanvas(
            state = state,
            onStartStroke = { onAction(DrawingAction.StartStroke(it)) },
            onContinueStroke = { onAction(DrawingAction.ContinueStroke(it)) },
            onEndStroke = { onAction(DrawingAction.EndStroke) },
            onCancelStroke = { onAction(DrawingAction.CancelStroke) },
            onZoomChange = { scale, offset -> onAction(DrawingAction.UpdateZoom(scale, offset)) },
            onMultiTouchChange = { isMultiTouch = it },
            onCanvasSizeChanged = { width, height -> onAction(DrawingAction.SetCanvasSize(width, height)) },
            isMultiTouch = isMultiTouch,
            modifier = Modifier.fillMaxSize(),
        )
        
        // Prompt header at top - flush with canvas top
        PromptHeader(
            instruction = state.instruction,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = Dimension.D400)
                .padding(top = Dimension.D400),
        )
        
        // Vertical toolbar on right side (Snapchat-style)
        // Hide while user is actively drawing
        val isDrawing = state.currentStroke != null
        AnimatedVisibility(
            visible = !isDrawing,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = Dimension.D400),
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        ) {
            VerticalToolbar(
                state = state,
                onAction = onAction,
            )
        }
        
        // Done button at bottom - hide while drawing
        AnimatedVisibility(
            visible = !isDrawing,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = Dimension.D600)
                .padding(horizontal = Dimension.D500),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            Button(
                onClick = { onAction(DrawingAction.Submit) },
                enabled = state.hasDrawn,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("DONE")
            }
        }
        
        // Popup overlay (dismiss on tap outside)
        if (state.activePopup != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onAction(DrawingAction.DismissPopup) }
                    )
            )
        }
        
        // Brush size popup
        AnimatedVisibility(
            visible = state.activePopup == DrawingPopup.BRUSH_SIZE,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 72.dp),
            enter = scaleIn(transformOrigin = TransformOrigin(1f, 0.5f)) + fadeIn(),
            exit = scaleOut(transformOrigin = TransformOrigin(1f, 0.5f)) + fadeOut(),
        ) {
            BrushSizePopup(
                selectedSize = state.brushSize,
                currentColor = state.selectedColor,
                onSizeSelected = { size ->
                    onAction(DrawingAction.SelectBrushSize(size))
                    onAction(DrawingAction.DismissPopup)
                },
            )
        }
        
        // Color picker popup
        AnimatedVisibility(
            visible = state.activePopup == DrawingPopup.COLOR,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 72.dp),
            enter = scaleIn(transformOrigin = TransformOrigin(1f, 0.5f)) + fadeIn(),
            exit = scaleOut(transformOrigin = TransformOrigin(1f, 0.5f)) + fadeOut(),
        ) {
            ColorPickerPopup(
                selectedColor = state.selectedColor,
                onColorSelected = { color ->
                    onAction(DrawingAction.SelectColor(color))
                    onAction(DrawingAction.DismissPopup)
                },
            )
        }
        
        // Pinch-to-zoom tooltip (one-time)
        AnimatedVisibility(
            visible = state.showPinchToZoomTooltip,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp),
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
        ) {
            PinchToZoomTooltip(
                onDismiss = { onAction(DrawingAction.DismissTooltip) },
            )
        }
    }
}

@Composable
private fun PromptHeader(
    instruction: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                color = AppTheme.colors.surfacePrimary.color.copy(alpha = 0.95f),
                shape = RoundedCornerShape(Dimension.D400),
            )
            .padding(horizontal = Dimension.D500, vertical = Dimension.D300),
    ) {
        Text(
            text = instruction,
            typography = AppTheme.typography.Body.B600,
            textAlign = TextAlign.Center,
            color = AppTheme.colors.text,
        )
    }
}

@Composable
private fun PinchToZoomTooltip(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(Dimension.D400),
            )
            .background(
                color = AppTheme.colors.surfacePrimary.color,
                shape = RoundedCornerShape(Dimension.D400),
            )
            .clickable(onClick = onDismiss)
            .padding(horizontal = Dimension.D500, vertical = Dimension.D400),
        horizontalArrangement = Arrangement.spacedBy(Dimension.D300),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "👌",
            typography = AppTheme.typography.Body.B500,
        )
        Text(
            text = "Pinch to zoom in and out",
            typography = AppTheme.typography.Body.B500,
            color = AppTheme.colors.text,
        )
    }
}

@Composable
private fun VerticalToolbar(
    state: DrawingState,
    onAction: (DrawingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(28.dp),
            )
            .background(
                color = AppTheme.colors.surfacePrimary.color,
                shape = RoundedCornerShape(28.dp),
            )
            .padding(vertical = Dimension.D400, horizontal = Dimension.D300),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimension.D300),
    ) {
        // Pencil button (opens brush size popup)
        ToolbarIconButton(
            icon = Icons.Pencil,
            contentDescription = "Brush",
            enabled = true,
            onClick = {
                onAction(DrawingAction.SelectTool(DrawingTool.PEN))
                onAction(DrawingAction.TogglePopup(DrawingPopup.BRUSH_SIZE))
            },
            isSelected = state.selectedTool == DrawingTool.PEN && state.activePopup == DrawingPopup.BRUSH_SIZE,
        )
        
        // Color button (opens color picker)
        ToolbarButton(
            isSelected = state.activePopup == DrawingPopup.COLOR,
            onClick = { onAction(DrawingAction.TogglePopup(DrawingPopup.COLOR)) },
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(state.selectedColor)
                    .border(
                        width = if (state.selectedColor == Color.White) 1.dp else 0.dp,
                        color = AppTheme.colors.border.color,
                        shape = CircleShape,
                    )
            )
        }
        
        // Eraser button
        ToolbarIconButton(
            icon = Icons.Eraser,
            contentDescription = "Eraser",
            enabled = true,
            onClick = { onAction(DrawingAction.SelectTool(DrawingTool.ERASER)) },
            isSelected = state.selectedTool == DrawingTool.ERASER,
        )
        
        Spacer(Modifier.height(Dimension.D200))
        
        // Undo button
        ToolbarIconButton(
            icon = Icons.Undo,
            contentDescription = "Undo",
            enabled = state.strokes.isNotEmpty(),
            onClick = { onAction(DrawingAction.Undo) },
        )
        
        // Redo button
        ToolbarIconButton(
            icon = Icons.Redo,
            contentDescription = "Redo",
            enabled = state.redoStack.isNotEmpty(),
            onClick = { onAction(DrawingAction.Redo) },
        )
        
        // Clear button
        ToolbarIconButton(
            icon = Icons.Delete,
            contentDescription = "Clear",
            enabled = state.strokes.isNotEmpty(),
            onClick = { onAction(DrawingAction.Clear) },
        )
        
        // Reset zoom button - only show when zoomed
        if (state.zoomScale != 1f || state.panOffset != Offset.Zero) {
            Spacer(Modifier.height(Dimension.D200))
            
            ToolbarIconButton(
                icon = Icons.FitScreen,
                contentDescription = "Reset zoom",
                enabled = true,
                onClick = { onAction(DrawingAction.ResetZoom) },
            )
        }
    }
}

@Composable
private fun ToolbarButton(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) AppTheme.colors.accentPrimary.color.copy(alpha = 0.2f)
                else Color.Transparent
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) AppTheme.colors.accentPrimary.color else Color.Transparent,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun ToolbarIconButton(
    icon: Icons,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) AppTheme.colors.accentPrimary.color.copy(alpha = 0.2f)
                else Color.Transparent
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) AppTheme.colors.accentPrimary.color else Color.Transparent,
                shape = CircleShape,
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            onClick = onClick,
            icon = icon(contentDescription = contentDescription),
            enabled = enabled,
        )
    }
}

@Composable
private fun BrushSizePopup(
    selectedSize: BrushSize,
    currentColor: Color,
    onSizeSelected: (BrushSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
            )
            .background(
                color = AppTheme.colors.surfacePrimary.color,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(Dimension.D400),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimension.D300),
    ) {
        Text(
            text = "Brush Size",
            typography = AppTheme.typography.Label.L600,
            color = AppTheme.colors.textSecondary,
        )
        
        VerticalSpacerD300()
        
        BrushSize.entries.forEach { size ->
            BrushSizeOption(
                size = size,
                color = currentColor,
                isSelected = size == selectedSize,
                onClick = { onSizeSelected(size) },
            )
        }
    }
}

@Composable
private fun ColorPickerPopup(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
            )
            .background(
                color = AppTheme.colors.surfacePrimary.color,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(Dimension.D400),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Color",
            typography = AppTheme.typography.Label.L600,
            color = AppTheme.colors.textSecondary,
        )
        
        VerticalSpacerD500()
        
        // Color grid (2 columns, 5 rows)
        Column(
            verticalArrangement = Arrangement.spacedBy(Dimension.D300),
        ) {
            CANVAS_COLORS.chunked(2).forEach { rowColors ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimension.D300),
                ) {
                    rowColors.forEach { color ->
                        ColorDot(
                            color = color,
                            isSelected = color == selectedColor,
                            onClick = { onColorSelected(color) },
                            size = 40.dp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawingCanvas(
    state: DrawingState,
    onStartStroke: (Offset) -> Unit,
    onContinueStroke: (Offset) -> Unit,
    onEndStroke: () -> Unit,
    onCancelStroke: () -> Unit,
    onZoomChange: (Float, Offset) -> Unit,
    onMultiTouchChange: (Boolean) -> Unit,
    onCanvasSizeChanged: (Int, Int) -> Unit,
    isMultiTouch: Boolean,
    modifier: Modifier = Modifier,
) {
    var currentZoom by remember { mutableStateOf(state.zoomScale) }
    var currentPan by remember { mutableStateOf(state.panOffset) }
    
    // Sync local state when ViewModel state changes (e.g., reset zoom)
    LaunchedEffect(state.zoomScale, state.panOffset) {
        currentZoom = state.zoomScale
        currentPan = state.panOffset
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(0.dp)) // Clip strokes to canvas bounds
            .background(Color.White)
            .onSizeChanged { size ->
                onCanvasSizeChanged(size.width, size.height)
            },
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = currentZoom
                    scaleY = currentZoom
                    translationX = currentPan.x
                    translationY = currentPan.y
                }
                .pointerInput(state.selectedTool) {
                    awaitEachGesture {
                        val firstPointer = awaitFirstDown(requireUnconsumed = false)
                        var pointerId = firstPointer.id
                        var isDrawing = true
                        var zoom = currentZoom
                        var pan = currentPan
                        
                        val transformedPoint = transformPoint(
                            firstPointer.position,
                            zoom,
                            pan,
                            size.width.toFloat(),
                            size.height.toFloat()
                        )
                        onStartStroke(transformedPoint)
                        
                        do {
                            val event = awaitPointerEvent()
                            val pointerCount = event.changes.count { it.pressed }
                            
                            if (pointerCount > 1) {
                                if (isDrawing) {
                                    onCancelStroke()
                                    isDrawing = false
                                    onMultiTouchChange(true)
                                }
                                
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                
                                zoom = (zoom * zoomChange).coerceIn(0.5f, 5f)
                                pan = Offset(
                                    x = pan.x + panChange.x,
                                    y = pan.y + panChange.y
                                )
                                
                                currentZoom = zoom
                                currentPan = pan
                                onZoomChange(zoom, pan)
                                
                                event.changes.forEach { it.consume() }
                            } else if (pointerCount == 1) {
                                if (!isDrawing && !isMultiTouch) {
                                    val pointer = event.changes.first { it.pressed }
                                    val transformedStart = transformPoint(
                                        pointer.position,
                                        zoom,
                                        pan,
                                        size.width.toFloat(),
                                        size.height.toFloat()
                                    )
                                    onStartStroke(transformedStart)
                                    isDrawing = true
                                }
                                
                                onMultiTouchChange(false)
                                
                                val pointer = event.changes.firstOrNull { it.id == pointerId }
                                    ?: event.changes.first { it.pressed }
                                pointerId = pointer.id
                                
                                if (isDrawing && pointer.positionChange() != Offset.Zero) {
                                    val transformedCurrent = transformPoint(
                                        pointer.position,
                                        zoom,
                                        pan,
                                        size.width.toFloat(),
                                        size.height.toFloat()
                                    )
                                    onContinueStroke(transformedCurrent)
                                }
                                
                                pointer.consume()
                            }
                        } while (event.changes.any { it.pressed })
                        
                        if (isDrawing) {
                            onEndStroke()
                        }
                        onMultiTouchChange(false)
                    }
                },
        ) {
            drawStrokes(
                strokes = state.strokes,
                currentStroke = state.currentStroke,
            )
        }
    }
}

private fun transformPoint(
    screenPoint: Offset,
    zoom: Float,
    pan: Offset,
    canvasWidth: Float,
    canvasHeight: Float,
): Offset {
    val centerX = canvasWidth / 2
    val centerY = canvasHeight / 2
    return Offset(
        x = (screenPoint.x - centerX - pan.x) / zoom + centerX,
        y = (screenPoint.y - centerY - pan.y) / zoom + centerY,
    )
}

private fun DrawScope.drawStrokes(
    strokes: List<DrawingStroke>,
    currentStroke: DrawingStroke?,
) {
    strokes.forEach { stroke ->
        drawStroke(stroke)
    }
    
    currentStroke?.let { stroke ->
        drawStroke(stroke)
    }
}

private fun DrawScope.drawStroke(stroke: DrawingStroke) {
    if (stroke.points.size < 2) return
    
    val path = Path().apply {
        val first = stroke.points.first()
        moveTo(first.x, first.y)
        
        for (i in 1 until stroke.points.size) {
            val point = stroke.points[i]
            val prev = stroke.points[i - 1]
            
            val midX = (prev.x + point.x) / 2
            val midY = (prev.y + point.y) / 2
            quadraticTo(prev.x, prev.y, midX, midY)
        }
        
        val last = stroke.points.last()
        lineTo(last.x, last.y)
    }
    
    val color = if (stroke.tool == DrawingTool.ERASER) Color.White else stroke.color
    
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = stroke.brushSize.strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
    )
}

@Composable
private fun ColorDot(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 28.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else if (color == Color.White) 1.dp else 0.dp,
                color = if (isSelected) AppTheme.colors.accentPrimary.color else AppTheme.colors.border.color,
                shape = CircleShape,
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun BrushSizeOption(
    size: BrushSize,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .width(140.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) AppTheme.colors.accentPrimary.color.copy(alpha = 0.1f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Dimension.D400, vertical = Dimension.D300),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimension.D400),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(size.strokeWidth.dp.coerceAtLeast(4.dp))
                    .clip(CircleShape)
                    .background(color)
            )
        }
        
        Text(
            text = size.displayName,
            typography = AppTheme.typography.Body.B500,
            color = if (isSelected) AppTheme.colors.accentPrimary else AppTheme.colors.text,
        )
    }
}

// ============ Previews ============

@Preview
@Composable
private fun DrawingScreenPreview() {
    PreviewContent {
        DrawingScreen(
            state = DrawingState(
                instruction = "Draw what you see when you close your eyes right now.",
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun DrawingScreenWithContentPreview() {
    PreviewContent {
        DrawingScreen(
            state = DrawingState(
                instruction = "Sketch something that made you smile today.",
                hasDrawn = true,
                selectedColor = Color(0xFF2196F3),
                brushSize = BrushSize.LARGE,
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun DrawingScreenWithBrushPopupPreview() {
    PreviewContent {
        DrawingScreen(
            state = DrawingState(
                instruction = "Draw a turtle. In the dark.",
                activePopup = DrawingPopup.BRUSH_SIZE,
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun DrawingScreenWithColorPopupPreview() {
    PreviewContent {
        DrawingScreen(
            state = DrawingState(
                instruction = "Draw a turtle. In the dark.",
                activePopup = DrawingPopup.COLOR,
            ),
            onAction = {},
        )
    }
}
