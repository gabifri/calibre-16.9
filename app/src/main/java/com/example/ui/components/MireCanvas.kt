package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.calibration.CalibrationMath
import com.example.calibration.FullScreenMire
import com.example.ui.theme.CalibreBeam
import com.example.ui.theme.CalibreBeamDarkText
import com.example.ui.theme.CalibreBeamDim
import com.example.ui.theme.CalibreFg
import com.example.ui.theme.CalibreLine
import com.example.ui.theme.CalibreMuted
import com.example.ui.theme.CalibreOk
import com.example.ui.theme.CalibreSurface

@Composable
fun FormatMireCanvas(
    modifier: Modifier = Modifier
) {
    val cd = stringResource(R.string.cd_mire_format)
    Canvas(
        modifier = modifier.semantics { contentDescription = cd }
    ) {
        val sx = size.width / 1920f
        val sy = size.height / 1080f

        // Background #000
        drawRect(color = Color.Black, size = size)

        // Grid lines every 120px in #59606e stroke-width 2
        val gridColor = Color(0xFF59606E)
        var x = 120f
        while (x < 1920f) {
            drawLine(
                color = gridColor,
                start = Offset(x * sx, 0f),
                end = Offset(x * sx, 1080f * sy),
                strokeWidth = 2f * minOf(sx, sy).coerceAtLeast(0.5f)
            )
            x += 120f
        }
        var y = 120f
        while (y < 1080f) {
            drawLine(
                color = gridColor,
                start = Offset(0f, y * sy),
                end = Offset(1920f * sx, y * sy),
                strokeWidth = 2f * minOf(sx, sy).coerceAtLeast(0.5f)
            )
            y += 120f
        }

        // White diagonals stroke-width 3
        val diagStroke = 3f * minOf(sx, sy).coerceAtLeast(0.5f)
        drawLine(
            color = Color.White,
            start = Offset(0f, 0f),
            end = Offset(1920f * sx, 1080f * sy),
            strokeWidth = diagStroke
        )
        drawLine(
            color = Color.White,
            start = Offset(1920f * sx, 0f),
            end = Offset(0f, 1080f * sy),
            strokeWidth = diagStroke
        )

        // Circle cx=960, cy=540, r=360, stroke-width 6
        // Draw oval scaled by sx, sy so if display aspect ratio is stretched, it reflects the true 1920x1080 SVG coordinate space
        drawOval(
            color = Color.White,
            topLeft = Offset((960f - 360f) * sx, (540f - 360f) * sy),
            size = Size(720f * sx, 720f * sy),
            style = Stroke(width = 6f * minOf(sx, sy).coerceAtLeast(0.5f))
        )

        // Outer white frame: rect x=20 y=20 width=1880 height=1040 stroke-width=40
        val borderW = 40f * sx
        val borderH = 40f * sy
        // Top bar (0..40)
        drawRect(color = Color.White, topLeft = Offset(0f, 0f), size = Size(1920f * sx, borderH))
        // Bottom bar (1040..1080)
        drawRect(color = Color.White, topLeft = Offset(0f, 1040f * sy), size = Size(1920f * sx, borderH))
        // Left bar (0..40)
        drawRect(color = Color.White, topLeft = Offset(0f, 0f), size = Size(borderW, 1080f * sy))
        // Right bar (1880..1920)
        drawRect(color = Color.White, topLeft = Offset(1880f * sx, 0f), size = Size(borderW, 1080f * sy))

        // Center text "16:9" at (960, 560) font-size 64 bold
        drawContext.canvas.nativeCanvas.apply {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textAlign = Paint.Align.CENTER
                textSize = 64f * sy
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            drawText("16:9", 960f * sx, 560f * sy, textPaint)
        }
    }
}

@Composable
fun ColorMireCanvas(
    modifier: Modifier = Modifier
) {
    val cd = stringResource(R.string.cd_mire_color)
    Canvas(
        modifier = modifier.semantics { contentDescription = cd }
    ) {
        val sx = size.width / 1920f
        val sy = size.height / 1080f

        // Background #000
        drawRect(color = Color.Black, size = size)

        // Outer white frame: x=20, y=20, w=1880, h=1040, stroke-width=40
        val borderW = 40f * sx
        val borderH = 40f * sy
        drawRect(color = Color.White, topLeft = Offset(0f, 0f), size = Size(1920f * sx, borderH))
        drawRect(color = Color.White, topLeft = Offset(0f, 1040f * sy), size = Size(1920f * sx, borderH))
        drawRect(color = Color.White, topLeft = Offset(0f, 0f), size = Size(borderW, 1080f * sy))
        drawRect(color = Color.White, topLeft = Offset(1880f * sx, 0f), size = Size(borderW, 1080f * sy))

        // Outlines in #3a3f4a stroke-width 2
        val outlineColor = Color(0xFF3A3F4A)
        val outlineStroke = Stroke(width = 2f * minOf(sx, sy).coerceAtLeast(0.5f))
        drawRect(
            color = outlineColor,
            topLeft = Offset(120f * sx, 110f * sy),
            size = Size(1680f * sx, 330f * sy),
            style = outlineStroke
        )
        drawRect(
            color = outlineColor,
            topLeft = Offset(120f * sx, 840f * sy),
            size = Size(780f * sx, 150f * sy),
            style = outlineStroke
        )
        drawRect(
            color = outlineColor,
            topLeft = Offset(1020f * sx, 840f * sy),
            size = Size(780f * sx, 150f * sy),
            style = outlineStroke
        )

        // Patches
        for (p in CalibrationMath.PATCHES) {
            drawRect(
                color = Color(p.rgb[0], p.rgb[1], p.rgb[2]),
                topLeft = Offset((p.x0 * 1920.0).toFloat() * sx, (p.y0 * 1080.0).toFloat() * sy),
                size = Size(
                    ((p.x1 - p.x0) * 1920.0).toFloat() * sx + 0.5f,
                    ((p.y1 - p.y0) * 1080.0).toFloat() * sy
                )
            )
        }

        // Labels in #8a8f9a font-size 28
        drawContext.canvas.nativeCanvas.apply {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#8A8F9A")
                textAlign = Paint.Align.LEFT
                textSize = 28f * sy
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            drawText("Gris de 0 à 100 %", 120f * sx, 488f * sy, textPaint)
            drawText("Noirs", 120f * sx, 1030f * sy, textPaint)
            drawText("Blancs", 1020f * sx, 1030f * sy, textPaint)
        }
    }
}

@Composable
fun MiresTabSection(
    isTvDevice: Boolean,
    onOpenFullScreen: (FullScreenMire) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isTvDevice) {
            Surface(
                color = CalibreBeamDim,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CalibreBeam),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "MODE ANDROID TV / VIDÉOPROJECTEUR",
                        style = MaterialTheme.typography.titleSmall,
                        color = CalibreBeam
                    )
                    Text(
                        text = "Sélectionne une mire avec la télécommande pour l'afficher en plein écran 16:9 sur ton projecteur, puis prends-la en photo avec ton téléphone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CalibreFg
                    )
                }
            }
        }

        // Mire de format card
        MireCard(
            title = stringResource(R.string.mire_format_title),
            description = "Ouvre cette page sur l'ordinateur ou l'Android TV relié au projecteur, puis touche la mire pour la mettre en plein écran. Touche-la de nouveau (ou Retour) pour quitter.",
            tips = listOf(
                "Quand le format est bon, les cases sont carrées et le cercle est rond.",
                "Règle la sortie de l'ordinateur ou de la box TV sur 1920 × 1080 ou 3840 × 2160, et le format d'image du projecteur sur 16:9.",
                "Le cadre blanc doit tenir entier sur le mur, sans être rogné."
            ),
            stageTag = "stage_geo",
            buttonTag = "full_geo_button",
            onEnterFullScreen = { onOpenFullScreen(FullScreenMire.FORMAT) }
        ) {
            FormatMireCanvas(modifier = Modifier.fillMaxSize())
        }

        // Mire de couleurs card
        MireCard(
            title = stringResource(R.string.mire_color_title),
            description = "Barres de couleurs pures, rampe de gris, petits carrés très sombres et très clairs.",
            tips = listOf(
                "Choisis un mode image neutre (Cinéma ou Standard, pas Dynamique) et laisse le projecteur chauffer 15 minutes.",
                "À l'œil : tu dois distinguer chaque carré sombre du suivant, et chaque carré clair du suivant.",
                "Éteins la lumière de la pièce avant de photographier."
            ),
            stageTag = "stage_col",
            buttonTag = "full_col_button",
            onEnterFullScreen = { onOpenFullScreen(FullScreenMire.COULEURS) }
        ) {
            ColorMireCanvas(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun MireCard(
    title: String,
    description: String,
    tips: List<String>,
    stageTag: String,
    buttonTag: String,
    onEnterFullScreen: () -> Unit,
    mireContent: @Composable () -> Unit
) {
    var stageFocused by remember { mutableStateOf(false) }

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
                text = title.uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = CalibreMuted
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = CalibreFg
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = if (stageFocused) 2.dp else 1.dp,
                        color = if (stageFocused) CalibreBeam else CalibreLine,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .onFocusChanged { stageFocused = it.isFocused }
                    .focusable()
                    .clickable(onClick = onEnterFullScreen)
                    .testTag(stageTag)
            ) {
                mireContent()
            }

            Button(
                onClick = onEnterFullScreen,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CalibreBeam,
                    contentColor = CalibreBeamDarkText
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag(buttonTag)
            ) {
                Text(
                    text = stringResource(R.string.btn_fullscreen),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                tips.forEach { tip ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = CalibreMuted
                        )
                        Text(
                            text = tip,
                            style = MaterialTheme.typography.bodySmall,
                            color = CalibreMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FullScreenMireOverlay(
    mire: FullScreenMire,
    onExit: () -> Unit,
    onSwitchMire: (FullScreenMire) -> Unit
) {
    BackHandler(onBack = onExit)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Escape, Key.Back, Key.DirectionCenter, Key.Enter -> {
                            onExit()
                            true
                        }
                        Key.DirectionLeft, Key.DirectionRight -> {
                            val next = if (mire == FullScreenMire.FORMAT) FullScreenMire.COULEURS else FullScreenMire.FORMAT
                            onSwitchMire(next)
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .focusable()
            .clickable(onClick = onExit)
            .testTag("fullscreen_mire_overlay")
    ) {
        when (mire) {
            FullScreenMire.FORMAT -> FormatMireCanvas(modifier = Modifier.fillMaxSize())
            FullScreenMire.COULEURS -> ColorMireCanvas(modifier = Modifier.fillMaxSize())
            FullScreenMire.NONE -> {}
        }
    }
}
