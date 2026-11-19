package com.example.ui.components

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.calibration.EditorState
import com.example.calibration.PointD
import com.example.ui.theme.CalibreBad
import com.example.ui.theme.CalibreBeam
import com.example.ui.theme.CalibreBeamDarkText
import com.example.ui.theme.CalibreBeamDim
import com.example.ui.theme.CalibreBg
import com.example.ui.theme.CalibreCrosshairDot
import com.example.ui.theme.CalibreFg
import com.example.ui.theme.CalibreLine
import com.example.ui.theme.CalibreMuted
import com.example.ui.theme.CalibreOk
import com.example.ui.theme.CalibreSurface
import com.example.ui.theme.CalibreSurface2
import com.example.ui.theme.CalibreWarn
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CornerEditorSection(
    uidPrefix: String,
    introText: String,
    editorState: EditorState,
    idealQuad: List<PointD>?,
    hasOverlayOption: Boolean,
    onTakePhotoClick: () -> Unit,
    onPickGalleryClick: () -> Unit,
    onSelectCorner: (Int) -> Unit,
    onStartDragCorner: (Int) -> Unit,
    onUpdateCornerPos: (Int, Double, Double) -> Unit,
    onEndDragCorner: () -> Unit,
    onNudgeCorner: (Double, Double) -> Unit,
    onSnapCorner: () -> Unit,
    onTogglePrecise: (Boolean) -> Unit,
    onToggleOverlay: (Boolean) -> Unit,
    onRestoreSample: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Card 1: 1. Prends la photo
        Surface(
            color = CalibreSurface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, CalibreLine),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.step_1_photo).uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = CalibreMuted
                )
                Text(
                    text = introText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = CalibreFg
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onTakePhotoClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CalibreBeam,
                            contentColor = CalibreBeamDarkText
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("${uidPrefix}_shoot_button")
                    ) {
                        Text(
                            text = stringResource(R.string.btn_take_photo),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                    OutlinedButton(
                        onClick = onPickGalleryClick,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = CalibreSurface2,
                            contentColor = CalibreFg
                        ),
                        border = BorderStroke(1.dp, CalibreLine),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("${uidPrefix}_pick_button")
                    ) {
                        Text(
                            text = stringResource(R.string.btn_pick_gallery),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                if (editorState.photoError) {
                    Text(
                        text = stringResource(R.string.photo_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = CalibreBad
                    )
                }
            }
        }

        // Card 2: 2. Cale les 4 coins
        Surface(
            color = CalibreSurface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, CalibreLine),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.step_2_corners).uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = CalibreMuted
                )
                Text(
                    text = stringResource(R.string.step_2_corners_desc),
                    style = MaterialTheme.typography.bodyLarge,
                    color = CalibreFg
                )
                Text(
                    text = editorState.sourceLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = CalibreMuted
                )

                // Interactive Photo Canvas + Loupe
                InteractiveQuadCanvas(
                    uidPrefix = uidPrefix,
                    editorState = editorState,
                    idealQuad = idealQuad,
                    showOverlay = hasOverlayOption && editorState.showOverlay,
                    onStartDragCorner = onStartDragCorner,
                    onUpdateCornerPos = onUpdateCornerPos,
                    onEndDragCorner = onEndDragCorner,
                    onNudgeCorner = onNudgeCorner
                )

                // 4 Corner selector chips
                val cornerNames = listOf(
                    stringResource(R.string.corner_tl),
                    stringResource(R.string.corner_tr),
                    stringResource(R.string.corner_br),
                    stringResource(R.string.corner_bl)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    cornerNames.forEachIndexed { idx, label ->
                        val selected = editorState.selectedCorner == idx
                        Surface(
                            color = if (selected) CalibreBeamDim else CalibreSurface2,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (selected) CalibreBeam else CalibreLine),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(role = Role.RadioButton) { onSelectCorner(idx) }
                                .testTag("${uidPrefix}_corner_chip_$idx")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = if (selected) CalibreBeam else CalibreMuted,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Fine adjustment box: Zoom canvas (148.dp) + 3x3 D-pad + "Aimanter sur le coin"
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CornerMagnifierCanvas(
                        bitmap = editorState.bitmap,
                        pts = editorState.pts,
                        focalPoint = editorState.pts.getOrNull(editorState.selectedCorner),
                        windowPx = 24.0,
                        modifier = Modifier
                            .size(148.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, CalibreLine, RoundedCornerShape(10.dp))
                            .background(Color.Black)
                            .testTag("${uidPrefix}_zoom_canvas")
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        NudgePad(
                            uidPrefix = uidPrefix,
                            onNudge = onNudgeCorner
                        )

                        OutlinedButton(
                            onClick = onSnapCorner,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = CalibreSurface2,
                                contentColor = CalibreFg
                            ),
                            border = BorderStroke(1.dp, CalibreLine),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("${uidPrefix}_snap_button")
                        ) {
                            Text(
                                text = stringResource(R.string.btn_snap_corner),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                // Switch 1: Mode précis
                SwitchRow(
                    checked = editorState.preciseMode,
                    text = stringResource(R.string.switch_precise),
                    testTag = "${uidPrefix}_precise_switch",
                    onCheckedChange = onTogglePrecise
                )

                // Switch 2: Montrer le cadre 16:9 attendu (Format tab only)
                if (hasOverlayOption) {
                    SwitchRow(
                        checked = editorState.showOverlay,
                        text = stringResource(R.string.switch_overlay),
                        testTag = "${uidPrefix}_overlay_switch",
                        onCheckedChange = onToggleOverlay
                    )
                }

                // "Revoir l'exemple" link button if not viewing the sample
                if (!editorState.isSample) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = onRestoreSample,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("${uidPrefix}_restore_sample_button")
                        ) {
                            Text(
                                text = stringResource(R.string.btn_restore_sample),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    textDecoration = TextDecoration.Underline
                                ),
                                color = CalibreBeam
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractiveQuadCanvas(
    uidPrefix: String,
    editorState: EditorState,
    idealQuad: List<PointD>?,
    showOverlay: Boolean,
    onStartDragCorner: (Int) -> Unit,
    onUpdateCornerPos: (Int, Double, Double) -> Unit,
    onEndDragCorner: () -> Unit,
    onNudgeCorner: (Double, Double) -> Unit
) {
    val bmp = editorState.bitmap
    val aspect = if (editorState.height > 0) {
        editorState.width.toFloat() / editorState.height.toFloat()
    } else {
        4f / 3f
    }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var isFocused by remember { mutableStateOf(false) }

    val currentPts by rememberUpdatedState(editorState.pts)
    val currentPrecise by rememberUpdatedState(editorState.preciseMode)
    val currentWidth by rememberUpdatedState(editorState.width)
    val currentHeight by rememberUpdatedState(editorState.height)

    val cdPhoto = stringResource(R.string.cd_photo_canvas)
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) CalibreBeam else CalibreLine,
                shape = RoundedCornerShape(8.dp)
            )
            .onSizeChanged { canvasSize = it }
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    val step = if (event.isShiftPressed) 5.0 else 1.0
                    when (event.key) {
                        Key.DirectionLeft -> { onNudgeCorner(-step, 0.0); true }
                        Key.DirectionRight -> { onNudgeCorner(step, 0.0); true }
                        Key.DirectionUp -> { onNudgeCorner(0.0, -step); true }
                        Key.DirectionDown -> { onNudgeCorner(0.0, step); true }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .focusable()
            .pointerInput(currentWidth, currentHeight) {
                val maxHitRadiusPx = 48.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val cw = size.width.toFloat()
                    if (cw <= 0f || currentPts.size != 4) return@awaitEachGesture
                    val scale = cw / currentWidth.toFloat()
                    var bestIdx = -1
                    var bestDistSq = maxHitRadiusPx * maxHitRadiusPx
                    for (i in 0 until 4) {
                        val cx = (currentPts[i].x * scale).toFloat()
                        val cy = (currentPts[i].y * scale).toFloat()
                        val dx = cx - down.position.x
                        val dy = cy - down.position.y
                        val d2 = dx * dx + dy * dy
                        if (d2 < bestDistSq) {
                            bestDistSq = d2
                            bestIdx = i
                        }
                    }
                    if (bestIdx < 0) return@awaitEachGesture
                    down.consume()
                    val q0 = down.position
                    val p0 = currentPts[bestIdx]
                    onStartDragCorner(bestIdx)

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                        if (change != null && change.pressed) {
                            change.consume()
                            val g = if (currentPrecise) 0.25 else 1.0
                            val newX = p0.x + ((change.position.x - q0.x) / scale) * g
                            val newY = p0.y + ((change.position.y - q0.y) / scale) * g
                            onUpdateCornerPos(bestIdx, newX, newY)
                        }
                    } while (event.changes.any { it.pressed })

                    onEndDragCorner()
                }
            }
            .semantics { contentDescription = cdPhoto }
            .testTag("${uidPrefix}_view_canvas")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (bmp != null) {
                drawImage(
                    image = bmp.asImageBitmap(),
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
                )
            }
            if (editorState.pts.size == 4 && editorState.width > 0) {
                val scale = size.width / editorState.width.toFloat()
                val P = editorState.pts.map {
                    Offset((it.x * scale).toFloat(), (it.y * scale).toFloat())
                }

                // Quad fill & outline
                val quadPath = Path().apply {
                    moveTo(P[0].x, P[0].y)
                    lineTo(P[1].x, P[1].y)
                    lineTo(P[2].x, P[2].y)
                    lineTo(P[3].x, P[3].y)
                    close()
                }
                drawPath(path = quadPath, color = CalibreBeam.copy(alpha = 0.08f))
                drawPath(path = quadPath, color = CalibreBeam, style = Stroke(width = 1.dp.toPx()))

                // Ideal 16:9 overlay + orange difference zone
                if (showOverlay && idealQuad != null && idealQuad.size == 4) {
                    val I = idealQuad.map {
                        Offset((it.x * scale).toFloat(), (it.y * scale).toFloat())
                    }
                    val diffPath = Path().apply {
                        fillType = PathFillType.EvenOdd
                        moveTo(P[0].x, P[0].y)
                        lineTo(P[1].x, P[1].y)
                        lineTo(P[2].x, P[2].y)
                        lineTo(P[3].x, P[3].y)
                        close()
                        moveTo(I[0].x, I[0].y)
                        lineTo(I[1].x, I[1].y)
                        lineTo(I[2].x, I[2].y)
                        lineTo(I[3].x, I[3].y)
                        close()
                    }
                    drawPath(path = diffPath, color = CalibreWarn.copy(alpha = 0.28f))

                    val idealPath = Path().apply {
                        moveTo(I[0].x, I[0].y)
                        lineTo(I[1].x, I[1].y)
                        lineTo(I[2].x, I[2].y)
                        lineTo(I[3].x, I[3].y)
                        close()
                    }
                    drawPath(
                        path = idealPath,
                        color = CalibreOk,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(6.dp.toPx(), 4.dp.toPx()),
                                0f
                            )
                        )
                    )

                    drawContext.canvas.nativeCanvas.apply {
                        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.parseColor("#5FD394")
                            textSize = 11.dp.toPx()
                            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                        }
                        drawText("16:9", I[0].x + 6.dp.toPx(), I[0].y + 16.dp.toPx(), labelPaint)
                    }
                }

                // 4 Corner handles
                P.forEachIndexed { idx, pt ->
                    val isSelected = idx == editorState.selectedCorner
                    val outerRadius = if (isSelected) 14.dp.toPx() else 10.dp.toPx()
                    drawCircle(
                        color = if (isSelected) CalibreBeam.copy(alpha = 0.22f) else CalibreBg.copy(alpha = 0.5f),
                        radius = outerRadius,
                        center = pt
                    )
                    drawCircle(
                        color = CalibreBeam,
                        radius = outerRadius,
                        center = pt,
                        style = Stroke(width = if (isSelected) 2.5.dp.toPx() else 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = CalibreBeam,
                        radius = 2.dp.toPx(),
                        center = pt
                    )
                }
            }
        }

        // Floating circular Loupe during drag (placed top-left or top-right opposite the dragged corner)
        val activeCorner = editorState.activeDragCorner
        if (activeCorner in 0..3 && editorState.pts.size == 4 && canvasSize.width > 0) {
            val activePt = editorState.pts[activeCorner]
            val scale = canvasSize.width.toFloat() / editorState.width.toFloat()
            val placeLeft = activePt.x * scale > canvasSize.width / 2f
            val loupeSizePx = with(density) { 112.dp.roundToPx() }
            val padPx = with(density) { 8.dp.roundToPx() }
            val offsetX = if (placeLeft) padPx else (canvasSize.width - loupeSizePx - padPx).coerceAtLeast(padPx)

            CornerMagnifierCanvas(
                bitmap = bmp,
                pts = editorState.pts,
                focalPoint = activePt,
                windowPx = 28.0,
                modifier = Modifier
                    .offset { IntOffset(offsetX, padPx) }
                    .size(112.dp)
                    .clip(CircleShape)
                    .border(2.dp, CalibreBeam, CircleShape)
                    .background(Color.Black)
            )
        }
    }
}

@Composable
fun CornerMagnifierCanvas(
    bitmap: android.graphics.Bitmap?,
    pts: List<PointD>,
    focalPoint: PointD?,
    windowPx: Double,
    modifier: Modifier = Modifier
) {
    val cdZoom = stringResource(R.string.cd_zoom_corner)
    Canvas(
        modifier = modifier.semantics { contentDescription = cdZoom }
    ) {
        drawRect(color = Color.Black, size = size)
        if (bitmap == null || focalPoint == null || pts.size != 4) return@Canvas

        val N = size.width
        val k = (N / windowPx).toFloat()
        val ox = focalPoint.x - windowPx / 2.0
        val oy = focalPoint.y - windowPx / 2.0

        // Draw magnified bitmap patch
        drawContext.canvas.nativeCanvas.apply {
            save()
            scale(k, k)
            translate((-ox).toFloat(), (-oy).toFloat())
            val bmpPaint = Paint(Paint.FILTER_BITMAP_FLAG)
            drawBitmap(bitmap, 0f, 0f, bmpPaint)
            restore()
        }

        // Draw quad outline inside magnifier
        val quadPath = Path().apply {
            pts.forEachIndexed { i, s ->
                val X = ((s.x - ox) * k).toFloat()
                val Y = ((s.y - oy) * k).toFloat()
                if (i == 0) moveTo(X, Y) else lineTo(X, Y)
            }
            close()
        }
        drawPath(
            path = quadPath,
            color = CalibreBeam.copy(alpha = 0.8f),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Crosshair with center gap
        val m = N / 2f
        val gap = 9.dp.toPx()
        val chColor = Color.White.copy(alpha = 0.8f)
        val chStroke = 1.dp.toPx()
        drawLine(color = chColor, start = Offset(0f, m), end = Offset(m - gap, m), strokeWidth = chStroke)
        drawLine(color = chColor, start = Offset(m + gap, m), end = Offset(N, m), strokeWidth = chStroke)
        drawLine(color = chColor, start = Offset(m, 0f), end = Offset(m, m - gap), strokeWidth = chStroke)
        drawLine(color = chColor, start = Offset(m, m + gap), end = Offset(m, N), strokeWidth = chStroke)

        // Center yellow dot
        drawCircle(
            color = CalibreCrosshairDot,
            radius = 2.dp.toPx(),
            center = Offset(m, m)
        )
    }
}

@Composable
private fun NudgePad(
    uidPrefix: String,
    onNudge: (Double, Double) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Row 1: Up button in center
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Spacer(modifier = Modifier.size(48.dp))
            RepeatableNudgeButton(
                symbol = "↑",
                contentDescription = stringResource(R.string.cd_nudge_up),
                testTag = "${uidPrefix}_nudge_up",
                onTrigger = { onNudge(0.0, -1.0) }
            )
            Spacer(modifier = Modifier.size(48.dp))
        }
        // Row 2: Left, "1 px", Right
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RepeatableNudgeButton(
                symbol = "←",
                contentDescription = stringResource(R.string.cd_nudge_left),
                testTag = "${uidPrefix}_nudge_left",
                onTrigger = { onNudge(-1.0, 0.0) }
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp)
            ) {
                Text(
                    text = "1 px",
                    style = MaterialTheme.typography.labelSmall,
                    color = CalibreMuted
                )
            }
            RepeatableNudgeButton(
                symbol = "→",
                contentDescription = stringResource(R.string.cd_nudge_right),
                testTag = "${uidPrefix}_nudge_right",
                onTrigger = { onNudge(1.0, 0.0) }
            )
        }
        // Row 3: Down button in center
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Spacer(modifier = Modifier.size(48.dp))
            RepeatableNudgeButton(
                symbol = "↓",
                contentDescription = stringResource(R.string.cd_nudge_down),
                testTag = "${uidPrefix}_nudge_down",
                onTrigger = { onNudge(0.0, 1.0) }
            )
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
private fun RepeatableNudgeButton(
    symbol: String,
    contentDescription: String,
    testTag: String,
    onTrigger: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val currentOnTrigger by rememberUpdatedState(onTrigger)

    LaunchedEffect(isPressed) {
        if (isPressed) {
            currentOnTrigger()
            delay(380L)
            while (true) {
                currentOnTrigger()
                delay(60L)
            }
        }
    }

    Surface(
        color = if (isPressed) CalibreBeamDim else CalibreSurface2,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isPressed) CalibreBeam else CalibreLine),
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                role = Role.Button,
                onClick = { /* Triggered via press state for immediate response & hold-repeat */ }
            )
            .semantics { this.contentDescription = contentDescription }
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = symbol,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = CalibreFg
            )
        }
    }
}

@Composable
private fun SwitchRow(
    checked: Boolean,
    text: String,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Checkbox) { onCheckedChange(!checked) }
            .padding(vertical = 4.dp)
            .testTag(testTag)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = CalibreBeam,
                uncheckedColor = CalibreMuted,
                checkmarkColor = CalibreBeamDarkText
            )
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = CalibreMuted
        )
    }
}
