package com.dangerfield.goodtimes.features.tasks.impl.templates.drawing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.dangerfield.goodtimes.libraries.goodtimes.AppCache
import com.dangerfield.goodtimes.libraries.goodtimes.TaskOutcome
import com.dangerfield.goodtimes.libraries.goodtimes.TaskResponse
import com.dangerfield.goodtimes.libraries.goodtimes.TaskSignal
import com.dangerfield.goodtimes.features.tasks.impl.base.BaseTaskViewModel
import com.dangerfield.goodtimes.features.tasks.impl.base.TaskScreenState
import com.dangerfield.goodtimes.libraries.goodtimes.Task
import com.dangerfield.libraries.ui.DrawingSaver
import com.dangerfield.libraries.ui.DrawingStrokeData
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import kotlin.math.abs

@Inject
class DrawingViewModel(
    @Assisted task: Task,
    private val drawingSaver: DrawingSaver,
    private val appCache: AppCache,
) : BaseTaskViewModel<DrawingState, DrawingAction>(
    task = task,
    initialState = DrawingState(
        instruction = task.instruction,
    )
) {
    
    init {
        viewModelScope.launch {
            val hasSeenTooltip = appCache.get().hasSeenPinchToZoomTooltip
            updateState { it.copy(showPinchToZoomTooltip = !hasSeenTooltip) }
        }
    }
    
    private var strokeCount = 0
    private var undoCount = 0
    private var redoCount = 0
    private var colorChanges = 0
    private var brushSizeChanges = 0
    private var firstStrokeAt: Long? = null
    private var zoomUsed = false
    private var totalStrokeLength = 0f
    
    override fun onAction(action: DrawingAction) {
        when (action) {
            is DrawingAction.StartStroke -> handleStartStroke(action.point)
            is DrawingAction.ContinueStroke -> handleContinueStroke(action.point)
            is DrawingAction.EndStroke -> handleEndStroke()
            is DrawingAction.CancelStroke -> handleCancelStroke()
            is DrawingAction.Undo -> handleUndo()
            is DrawingAction.Redo -> handleRedo()
            is DrawingAction.Clear -> handleClear()
            is DrawingAction.SelectColor -> handleSelectColor(action.color)
            is DrawingAction.SelectBrushSize -> handleSelectBrushSize(action.size)
            is DrawingAction.TogglePopup -> handleTogglePopup(action.popup)
            is DrawingAction.DismissPopup -> handleDismissPopup()
            is DrawingAction.UpdateZoom -> handleUpdateZoom(action.scale, action.offset)
            is DrawingAction.ResetZoom -> handleResetZoom()
            is DrawingAction.SetCanvasSize -> handleSetCanvasSize(action.width, action.height)
            is DrawingAction.Submit -> handleSubmit()
            is DrawingAction.SelectTool -> handleSelectTool(action.tool)
            is DrawingAction.DismissTooltip -> handleDismissTooltip()
        }
    }
    
    private fun handleSetCanvasSize(width: Int, height: Int) {
        if (currentState.canvasWidth != width || currentState.canvasHeight != height) {
            updateState { it.copy(canvasWidth = width, canvasHeight = height) }
        }
    }
    
    private fun handleStartStroke(point: Offset) {
        if (firstStrokeAt == null) {
            firstStrokeAt = System.currentTimeMillis()
        }
        
        val newStroke = DrawingStroke(
            points = listOf(point),
            color = currentState.selectedColor,
            brushSize = currentState.brushSize,
            tool = currentState.selectedTool,
        )
        
        updateState { state ->
            state.copy(
                currentStroke = newStroke,
                redoStack = emptyList(),
            )
        }
    }
    
    private fun handleContinueStroke(point: Offset) {
        val current = currentState.currentStroke ?: return
        val lastPoint = current.points.lastOrNull() ?: return
        
        totalStrokeLength += calculateDistance(lastPoint, point)
        
        updateState { state ->
            state.copy(
                currentStroke = current.copy(
                    points = current.points + point
                )
            )
        }
    }
    
    private fun handleEndStroke() {
        val stroke = currentState.currentStroke ?: return
        if (stroke.points.size < 2) {
            updateState { it.copy(currentStroke = null) }
            return
        }
        
        strokeCount++
        
        updateState { state ->
            state.copy(
                strokes = state.strokes + stroke,
                currentStroke = null,
                hasDrawn = true,
            )
        }
    }
    
    private fun handleCancelStroke() {
        // Discard the current stroke without saving it
        updateState { it.copy(currentStroke = null) }
    }
    
    private fun handleUndo() {
        val strokes = currentState.strokes
        if (strokes.isEmpty()) return
        
        undoCount++
        
        updateState { state ->
            state.copy(
                strokes = strokes.dropLast(1),
                redoStack = state.redoStack + strokes.last(),
                hasDrawn = strokes.size > 1,
            )
        }
    }
    
    private fun handleRedo() {
        val redoStack = currentState.redoStack
        if (redoStack.isEmpty()) return
        
        redoCount++
        
        updateState { state ->
            state.copy(
                strokes = state.strokes + redoStack.last(),
                redoStack = redoStack.dropLast(1),
                hasDrawn = true,
            )
        }
    }
    
    private fun handleClear() {
        updateState { state ->
            state.copy(
                strokes = emptyList(),
                redoStack = emptyList(),
                currentStroke = null,
                hasDrawn = false,
            )
        }
    }
    
    private fun handleSelectColor(color: Color) {
        if (color != currentState.selectedColor) {
            colorChanges++
        }
        updateState { it.copy(selectedColor = color) }
    }
    
    private fun handleSelectBrushSize(size: BrushSize) {
        if (size != currentState.brushSize) {
            brushSizeChanges++
        }
        updateState { it.copy(brushSize = size) }
    }
    
    private fun handleSelectTool(tool: DrawingTool) {
        updateState { it.copy(selectedTool = tool) }
    }
    
    private fun handleTogglePopup(popup: DrawingPopup) {
        updateState { state ->
            state.copy(
                activePopup = if (state.activePopup == popup) null else popup
            )
        }
    }
    
    private fun handleDismissPopup() {
        updateState { it.copy(activePopup = null) }
    }
    
    private fun handleDismissTooltip() {
        updateState { it.copy(showPinchToZoomTooltip = false) }
        viewModelScope.launch {
            appCache.update { it.copy(hasSeenPinchToZoomTooltip = true) }
        }
    }
    
    private fun handleUpdateZoom(scale: Float, offset: Offset) {
        if (scale != 1f) {
            zoomUsed = true
        }
        updateState { it.copy(zoomScale = scale, panOffset = offset) }
    }
    
    private fun handleResetZoom() {
        updateState { it.copy(zoomScale = 1f, panOffset = Offset.Zero) }
    }
    
    private fun handleSubmit() {
        if (!currentState.hasDrawn || currentState.isSaving) return
        
        updateState { it.copy(isSaving = true) }
        
        viewModelScope.launch {
            val strokeDataList = currentState.strokes.map { stroke ->
                DrawingStrokeData(
                    points = stroke.points,
                    color = stroke.color,
                    strokeWidth = stroke.brushSize.strokeWidth,
                    isEraser = stroke.tool == DrawingTool.ERASER,
                )
            }
            
            val filePath = drawingSaver.saveDrawing(
                strokes = strokeDataList,
                width = currentState.canvasWidth.takeIf { it > 0 } ?: 1080,
                height = currentState.canvasHeight.takeIf { it > 0 } ?: 1920,
                backgroundColor = Color.White,
            )
            
            val response = TaskResponse.Drawing(
                strokeData = serializeStrokes(currentState.strokes),
                filePath = filePath,
            )
            
            updateState { it.copy(isSaving = false) }
            complete(response, TaskOutcome.COMPLETED)
        }
    }
    
    private fun serializeStrokes(strokes: List<DrawingStroke>): String {
        return strokes.joinToString("|") { stroke ->
            val points = stroke.points.joinToString(",") { "${it.x}:${it.y}" }
            val colorHex = stroke.color.value.toString(16)
            "${stroke.brushSize.strokeWidth}~$colorHex~${stroke.tool.name}~$points"
        }
    }
    
    private fun calculateDistance(p1: Offset, p2: Offset): Float {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
    
    override fun computeSignals(): List<TaskSignal> = buildList {
        val timeSpent = getElapsedMs()
        
        if (strokeCount > 20) {
            add(TaskSignal("ENGAGEMENT", +2, "Drew extensively with many strokes"))
        } else if (strokeCount < 5 && currentState.hasDrawn) {
            add(TaskSignal("ENGAGEMENT", -1, "Minimal drawing effort"))
        }
        
        if (undoCount > 10) {
            add(TaskSignal("PERFECTIONISM", +1, "Frequently revised drawing"))
        }
        
        if (colorChanges > 5) {
            add(TaskSignal("CREATIVITY", +1, "Used multiple colors"))
        }
        
        if (zoomUsed) {
            add(TaskSignal("ATTENTION_TO_DETAIL", +1, "Used zoom for precision"))
        }
        
        if (timeSpent > 120_000 && strokeCount > 15) {
            add(TaskSignal("PATIENCE", +2, "Spent significant time on drawing"))
        }
        
        val hesitationMs = firstStrokeAt?.let { it - (getElapsedMs() - timeSpent) }
        if (hesitationMs != null && hesitationMs > 15_000) {
            add(TaskSignal("REFLECTION_DEPTH", +1, "Paused before drawing"))
        }
    }
}

data class DrawingState(
    val instruction: String,
    val strokes: List<DrawingStroke> = emptyList(),
    val currentStroke: DrawingStroke? = null,
    val redoStack: List<DrawingStroke> = emptyList(),
    val selectedColor: Color = Color.Black,
    val brushSize: BrushSize = BrushSize.MEDIUM,
    val selectedTool: DrawingTool = DrawingTool.PEN,
    val activePopup: DrawingPopup? = null,
    val zoomScale: Float = 1f,
    val panOffset: Offset = Offset.Zero,
    val hasDrawn: Boolean = false,
    val canvasWidth: Int = 0,
    val canvasHeight: Int = 0,
    val isSaving: Boolean = false,
    val showPinchToZoomTooltip: Boolean = false,
    override val isLoading: Boolean = false,
) : TaskScreenState

enum class DrawingPopup {
    BRUSH_SIZE,
    COLOR,
}

data class DrawingStroke(
    val points: List<Offset>,
    val color: Color,
    val brushSize: BrushSize,
    val tool: DrawingTool,
)

enum class BrushSize(val strokeWidth: Float, val displayName: String) {
    FINE(2f, "Fine"),
    SMALL(4f, "Small"),
    MEDIUM(8f, "Medium"),
    LARGE(16f, "Large"),
    THICK(24f, "Thick"),
}

enum class DrawingTool {
    PEN,
    ERASER,
}

sealed class DrawingAction {
    data class StartStroke(val point: Offset) : DrawingAction()
    data class ContinueStroke(val point: Offset) : DrawingAction()
    data object EndStroke : DrawingAction()
    data object CancelStroke : DrawingAction()
    data object Undo : DrawingAction()
    data object Redo : DrawingAction()
    data object Clear : DrawingAction()
    data class SelectColor(val color: Color) : DrawingAction()
    data class SelectBrushSize(val size: BrushSize) : DrawingAction()
    data class SelectTool(val tool: DrawingTool) : DrawingAction()
    data class TogglePopup(val popup: DrawingPopup) : DrawingAction()
    data object DismissPopup : DrawingAction()
    data class UpdateZoom(val scale: Float, val offset: Offset) : DrawingAction()
    data object ResetZoom : DrawingAction()
    data class SetCanvasSize(val width: Int, val height: Int) : DrawingAction()
    data object Submit : DrawingAction()
    data object DismissTooltip : DrawingAction()
}
