package com.example.calibration

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class CalibreTab {
    FORMAT,
    COULEURS,
    MIRES
}

enum class FullScreenMire {
    NONE,
    FORMAT,
    COULEURS
}

data class EditorState(
    val bitmap: Bitmap? = null,
    val width: Int = 1600,
    val height: Int = 1200,
    val pts: List<PointD> = emptyList(),
    val selectedCorner: Int = 0,
    val activeDragCorner: Int = -1,
    val isSample: Boolean = true,
    val sourceLabel: String = "",
    val preciseMode: Boolean = true,
    val showOverlay: Boolean = true,
    val threshold: Int = 128,
    val photoError: Boolean = false
)

data class CalibreUiState(
    val activeTab: CalibreTab = CalibreTab.FORMAT,
    val fullScreenMire: FullScreenMire = FullScreenMire.NONE,
    val isTvDevice: Boolean = false,
    val geoEditor: EditorState = EditorState(
        sourceLabel = "Exemple simulé : un écran 16:9 photographié de biais."
    ),
    val geoEval: GeoEvaluation = GeoEvaluation(
        state = MeasureState.NONE,
        pillText = "En attente",
        ratioText = "–",
        devText = " ",
        msgText = "",
        gaugeVisible = false,
        gaugeMarkerFraction = 0.5f,
        tips = emptyList(),
        canKeep = false
    ),
    val geoIdealQuad: List<PointD>? = null,
    val geoLogs: List<LogEntry> = emptyList(),
    val colorEditor: EditorState = EditorState(
        sourceLabel = "Exemple simulé : image chaude, gamma 2,5, photographiée de biais."
    ),
    val colorEval: ColorEvaluation = ColorEvaluation(
        state = MeasureState.NONE,
        pillText = "En attente",
        msgText = "",
        exposureWarning = null,
        rows = emptyList(),
        tips = emptyList(),
        canKeep = false
    ),
    val colorLogs: List<LogEntry> = emptyList()
)

class CalibrationViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CalibreUiState())
    val uiState: StateFlow<CalibreUiState> = _uiState.asStateFlow()

    private var geoPixels: IntArray = IntArray(0)
    private var colorPixels: IntArray = IntArray(0)

    init {
        viewModelScope.launch(Dispatchers.Default) {
            val geoBmp = SampleGenerator.makeGeoSample()
            val colBmp = SampleGenerator.makeColorSample()
            setGeoSourceInternal(geoBmp, isSample = true)
            setColorSourceInternal(colBmp, isSample = true)
        }
    }

    fun setTvMode(isTv: Boolean) {
        if (isTv && !_uiState.value.isTvDevice) {
            _uiState.update {
                it.copy(
                    isTvDevice = true,
                    activeTab = CalibreTab.MIRES
                )
            }
        }
    }

    fun selectTab(tab: CalibreTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun setFullScreenMire(mire: FullScreenMire) {
        _uiState.update { it.copy(fullScreenMire = mire) }
    }

    private fun extractScaledBitmapAndPixels(raw: Bitmap): Pair<Bitmap, IntArray> {
        val L = max(raw.width, raw.height)
        val k = min(1.0, 2400.0 / L.toDouble())
        val targetW = max(1, (raw.width * k).roundToInt())
        val targetH = max(1, (raw.height * k).roundToInt())
        val scaled = if (targetW == raw.width && targetH == raw.height && raw.config == Bitmap.Config.ARGB_8888) {
            raw
        } else {
            Bitmap.createScaledBitmap(raw, targetW, targetH, true).let {
                if (it.config != Bitmap.Config.ARGB_8888) {
                    it.copy(Bitmap.Config.ARGB_8888, false)
                } else {
                    it
                }
            }
        }
        val pixels = IntArray(scaled.width * scaled.height)
        scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
        return scaled to pixels
    }

    private fun setGeoSourceInternal(rawBitmap: Bitmap, isSample: Boolean) {
        val (bmp, pixels) = extractScaledBitmapAndPixels(rawBitmap)
        geoPixels = pixels
        val det = CalibrationMath.detectCornersAndSnap(bmp, pixels)
        val eval = CalibrationMath.evaluateGeo(det.pts, bmp.width, bmp.height)
        val ideal = CalibrationMath.idealQuad(det.pts, bmp.width, bmp.height)
        val label = if (isSample) {
            "Exemple simulé : un écran 16:9 photographié de biais."
        } else {
            "Ta photo (${bmp.width} × ${bmp.height} px)"
        }
        _uiState.update { state ->
            state.copy(
                geoEditor = state.geoEditor.copy(
                    bitmap = bmp,
                    width = bmp.width,
                    height = bmp.height,
                    pts = det.pts,
                    selectedCorner = 0,
                    activeDragCorner = -1,
                    isSample = isSample,
                    sourceLabel = label,
                    threshold = det.threshold,
                    photoError = false
                ),
                geoEval = eval,
                geoIdealQuad = ideal
            )
        }
    }

    private fun setColorSourceInternal(rawBitmap: Bitmap, isSample: Boolean) {
        val (bmp, pixels) = extractScaledBitmapAndPixels(rawBitmap)
        colorPixels = pixels
        val det = CalibrationMath.detectCornersAndSnap(bmp, pixels)
        val eval = CalibrationMath.evaluateColor(pixels, bmp.width, bmp.height, det.pts)
        val label = if (isSample) {
            "Exemple simulé : image chaude, gamma 2,5, photographiée de biais."
        } else {
            "Ta photo (${bmp.width} × ${bmp.height} px)"
        }
        _uiState.update { state ->
            state.copy(
                colorEditor = state.colorEditor.copy(
                    bitmap = bmp,
                    width = bmp.width,
                    height = bmp.height,
                    pts = det.pts,
                    selectedCorner = 0,
                    activeDragCorner = -1,
                    isSample = isSample,
                    sourceLabel = label,
                    threshold = det.threshold,
                    photoError = false
                ),
                colorEval = eval
            )
        }
    }

    fun onPhotoLoaded(forGeo: Boolean, bitmap: Bitmap?) {
        if (bitmap == null) {
            _uiState.update { state ->
                if (forGeo) state.copy(geoEditor = state.geoEditor.copy(photoError = true))
                else state.copy(colorEditor = state.colorEditor.copy(photoError = true))
            }
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            if (forGeo) setGeoSourceInternal(bitmap, isSample = false)
            else setColorSourceInternal(bitmap, isSample = false)
        }
    }

    fun restoreSample(forGeo: Boolean) {
        viewModelScope.launch(Dispatchers.Default) {
            if (forGeo) {
                setGeoSourceInternal(SampleGenerator.makeGeoSample(), isSample = true)
            } else {
                setColorSourceInternal(SampleGenerator.makeColorSample(), isSample = true)
            }
        }
    }

    fun selectCorner(forGeo: Boolean, cornerIndex: Int) {
        _uiState.update { state ->
            if (forGeo) {
                state.copy(geoEditor = state.geoEditor.copy(selectedCorner = cornerIndex.coerceIn(0, 3)))
            } else {
                state.copy(colorEditor = state.colorEditor.copy(selectedCorner = cornerIndex.coerceIn(0, 3)))
            }
        }
    }

    fun setDragCorner(forGeo: Boolean, cornerIndex: Int) {
        _uiState.update { state ->
            if (forGeo) {
                state.copy(
                    geoEditor = state.geoEditor.copy(
                        selectedCorner = if (cornerIndex >= 0) cornerIndex else state.geoEditor.selectedCorner,
                        activeDragCorner = cornerIndex
                    )
                )
            } else {
                state.copy(
                    colorEditor = state.colorEditor.copy(
                        selectedCorner = if (cornerIndex >= 0) cornerIndex else state.colorEditor.selectedCorner,
                        activeDragCorner = cornerIndex
                    )
                )
            }
        }
    }

    fun updateCornerPosition(forGeo: Boolean, cornerIndex: Int, newX: Double, newY: Double) {
        _uiState.update { state ->
            if (forGeo) {
                val ed = state.geoEditor
                if (ed.pts.size != 4) return@update state
                val clampedX = CalibrationMath.clamp(newX, 0.0, ed.width.toDouble())
                val clampedY = CalibrationMath.clamp(newY, 0.0, ed.height.toDouble())
                val newPts = ed.pts.toMutableList().apply {
                    this[cornerIndex] = PointD(clampedX, clampedY)
                }
                val eval = CalibrationMath.evaluateGeo(newPts, ed.width, ed.height)
                val ideal = CalibrationMath.idealQuad(newPts, ed.width, ed.height)
                state.copy(
                    geoEditor = ed.copy(pts = newPts),
                    geoEval = eval,
                    geoIdealQuad = ideal
                )
            } else {
                val ed = state.colorEditor
                if (ed.pts.size != 4 || colorPixels.isEmpty()) return@update state
                val clampedX = CalibrationMath.clamp(newX, 0.0, ed.width.toDouble())
                val clampedY = CalibrationMath.clamp(newY, 0.0, ed.height.toDouble())
                val newPts = ed.pts.toMutableList().apply {
                    this[cornerIndex] = PointD(clampedX, clampedY)
                }
                val eval = CalibrationMath.evaluateColor(colorPixels, ed.width, ed.height, newPts)
                state.copy(
                    colorEditor = ed.copy(pts = newPts),
                    colorEval = eval
                )
            }
        }
    }

    fun nudgeCorner(forGeo: Boolean, dx: Double, dy: Double) {
        val state = _uiState.value
        val ed = if (forGeo) state.geoEditor else state.colorEditor
        if (ed.pts.size != 4) return
        val idx = ed.selectedCorner
        val cur = ed.pts[idx]
        updateCornerPosition(forGeo, idx, cur.x + dx, cur.y + dy)
    }

    fun snapSelectedCorner(forGeo: Boolean) {
        val state = _uiState.value
        val ed = if (forGeo) state.geoEditor else state.colorEditor
        val pixels = if (forGeo) geoPixels else colorPixels
        if (ed.pts.size != 4 || pixels.isEmpty()) return
        val mutPts = ed.pts.toMutableList()
        val changed = CalibrationMath.snapCorner(
            pixels = pixels,
            width = ed.width,
            height = ed.height,
            pts = mutPts,
            cornerIndex = ed.selectedCorner,
            radius = 20,
            thr = ed.threshold
        )
        if (changed) {
            val p = mutPts[ed.selectedCorner]
            updateCornerPosition(forGeo, ed.selectedCorner, p.x, p.y)
        }
    }

    fun togglePreciseMode(forGeo: Boolean, precise: Boolean) {
        _uiState.update { state ->
            if (forGeo) state.copy(geoEditor = state.geoEditor.copy(preciseMode = precise))
            else state.copy(colorEditor = state.colorEditor.copy(preciseMode = precise))
        }
    }

    fun toggleOverlay(show: Boolean) {
        _uiState.update { state ->
            state.copy(geoEditor = state.geoEditor.copy(showOverlay = show))
        }
    }

    private fun nowText(): String {
        val fmt = SimpleDateFormat("HH:mm", Locale.FRANCE)
        return fmt.format(Date())
    }

    fun keepGeoMeasurement() {
        val eval = _uiState.value.geoEval
        if (!eval.canKeep) return
        val entry = LogEntry(
            state = eval.state,
            left = CalibrationMath.fmt(eval.rawRatio, 3),
            right = "${nowText()} · ${CalibrationMath.sgn(eval.rawDev * 100.0, 1)} %"
        )
        _uiState.update { state ->
            state.copy(geoLogs = (listOf(entry) + state.geoLogs).take(8))
        }
    }

    fun clearGeoLogs() {
        _uiState.update { it.copy(geoLogs = emptyList()) }
    }

    fun keepColorMeasurement() {
        val eval = _uiState.value.colorEval
        if (!eval.canKeep) return
        val entry = LogEntry(
            state = eval.state,
            left = eval.logSummaryLeft,
            right = nowText()
        )
        _uiState.update { state ->
            state.copy(colorLogs = (listOf(entry) + state.colorLogs).take(8))
        }
    }

    fun clearColorLogs() {
        _uiState.update { it.copy(colorLogs = emptyList()) }
    }
}
