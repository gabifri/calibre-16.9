package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.calibration.ColorEvaluation
import com.example.calibration.ColorMetricRow
import com.example.calibration.GaugeSpec
import com.example.calibration.GeoEvaluation
import com.example.calibration.HueItemUi
import com.example.calibration.LogEntry
import com.example.calibration.MeasureState
import com.example.ui.theme.CalibreBad
import com.example.ui.theme.CalibreBeam
import com.example.ui.theme.CalibreFg
import com.example.ui.theme.CalibreLine
import com.example.ui.theme.CalibreMuted
import com.example.ui.theme.CalibreOk
import com.example.ui.theme.CalibreSurface
import com.example.ui.theme.CalibreSurface2
import com.example.ui.theme.CalibreWarn

fun stateColor(state: MeasureState): Color = when (state) {
    MeasureState.OK -> CalibreOk
    MeasureState.WARN -> CalibreWarn
    MeasureState.BAD -> CalibreBad
    MeasureState.NONE -> CalibreMuted
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FormatResultsSection(
    eval: GeoEvaluation,
    logs: List<LogEntry>,
    onKeepMeasure: () -> Unit,
    onClearLogs: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Result card
        Surface(
            color = CalibreSurface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, CalibreLine),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("format_result_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.format_measured_title).uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = CalibreMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = eval.ratioText,
                        style = MaterialTheme.typography.displayLarge,
                        color = CalibreFg,
                        modifier = Modifier.testTag("ratio_val_text")
                    )
                    Text(
                        text = eval.devText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize
                        ),
                        color = stateColor(eval.state),
                        modifier = Modifier.testTag("dev_val_text")
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatePill(
                        text = eval.pillText,
                        state = eval.state,
                        small = false,
                        testTag = "format_state_pill"
                    )
                    if (eval.msgText.isNotEmpty()) {
                        Text(
                            text = eval.msgText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CalibreMuted,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }

                if (eval.gaugeVisible) {
                    CalibrationGaugeBar(
                        spec = GaugeSpec(
                            bandStartFraction = 0.3333f,
                            bandWidthFraction = 0.3333f,
                            markerFraction = eval.gaugeMarkerFraction,
                            ticks = Triple("−3 %", "16:9 = 1,778", "+3 %")
                        ),
                        state = eval.state
                    )
                }

                OutlinedButton(
                    onClick = onKeepMeasure,
                    enabled = eval.canKeep,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CalibreSurface2,
                        contentColor = CalibreFg,
                        disabledContainerColor = CalibreSurface2.copy(alpha = 0.45f),
                        disabledContentColor = CalibreFg.copy(alpha = 0.45f)
                    ),
                    border = BorderStroke(1.dp, CalibreLine),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("keep_geo_button")
                ) {
                    Text(
                        text = stringResource(R.string.btn_keep_measure),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // Pour corriger card
        if (eval.tips.isNotEmpty()) {
            TipsCard(tips = eval.tips, testTag = "geo_tips_card")
        }

        // Mesures gardées card
        if (logs.isNotEmpty()) {
            LogsCard(
                logs = logs,
                onClear = onClearLogs,
                cardTag = "geo_logs_card",
                clearBtnTag = "clear_geo_logs_button"
            )
        }

        // Comment ça marche card
        HowItWorksCard(
            paragraphs = listOf(
                "La mesure suppose que l'image projetée est un rectangle. Elle retrouve son format à partir des 4 coins en tenant compte de la perspective de la photo, comme le fait un scanner de document. Corrige d'abord le trapèze avec la correction trapézoïdale du projecteur. Plus les coins sont précis, plus la mesure est fiable."
            ),
            testTag = "geo_how_it_works"
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorResultsSection(
    eval: ColorEvaluation,
    logs: List<LogEntry>,
    onKeepMeasure: () -> Unit,
    onClearLogs: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Couleurs mesurées card
        Surface(
            color = CalibreSurface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, CalibreLine),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("color_result_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.color_measured_title).uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = CalibreMuted
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatePill(
                        text = eval.pillText,
                        state = eval.state,
                        small = false,
                        testTag = "color_state_pill"
                    )
                    if (eval.msgText.isNotEmpty()) {
                        Text(
                            text = eval.msgText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CalibreMuted,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }

                if (eval.exposureWarning != null) {
                    Surface(
                        color = CalibreSurface2,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CalibreWarn),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = eval.exposureWarning,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CalibreFg,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }

                if (eval.rows.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        eval.rows.forEach { row ->
                            ColorMetricRowItem(row = row)
                        }
                    }
                }

                OutlinedButton(
                    onClick = onKeepMeasure,
                    enabled = eval.canKeep,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CalibreSurface2,
                        contentColor = CalibreFg,
                        disabledContainerColor = CalibreSurface2.copy(alpha = 0.45f),
                        disabledContentColor = CalibreFg.copy(alpha = 0.45f)
                    ),
                    border = BorderStroke(1.dp, CalibreLine),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("keep_color_button")
                ) {
                    Text(
                        text = stringResource(R.string.btn_keep_measure),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // Pour corriger card
        if (eval.tips.isNotEmpty()) {
            TipsCard(tips = eval.tips, testTag = "color_tips_card")
        }

        // Mesures gardées card
        if (logs.isNotEmpty()) {
            LogsCard(
                logs = logs,
                onClear = onClearLogs,
                cardTag = "color_logs_card",
                clearBtnTag = "clear_color_logs_button"
            )
        }

        // Comment ça marche card
        HowItWorksCard(
            paragraphs = listOf(
                "La photo est redressée grâce aux 4 coins, puis l'appli lit la couleur de chaque case de la mire. Les cases grises donnent la dominante (chaud ou froid, vert ou magenta) et le gamma. Les petits carrés sombres et clairs disent si les noirs sont bouchés ou les blancs écrêtés. Les six couleurs pures donnent l'écart de teinte.",
                "La mesure reste indicative : le téléphone règle lui-même l'exposition et la balance des blancs. Verrouille-les, garde le téléphone au même endroit d'un essai à l'autre, et compare avant et après chaque réglage."
            ),
            testTag = "color_how_it_works"
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorMetricRowItem(row: ColorMetricRow) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = row.label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = CalibreFg
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = row.valueText,
                style = MaterialTheme.typography.labelMedium,
                color = stateColor(row.state)
            )
            StatePill(
                text = row.state.labelFr,
                state = row.state,
                small = true
            )
        }

        if (row.gauge != null) {
            CalibrationGaugeBar(spec = row.gauge, state = row.state)
        }

        if (row.steps != null) {
            val stCol = stateColor(row.state)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth()
            ) {
                row.steps.forEach { isDistinct ->
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .widthIn(min = 28.dp, max = 36.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isDistinct) stCol else CalibreSurface2)
                            .border(
                                width = 1.dp,
                                color = if (isDistinct) stCol else CalibreLine,
                                shape = RoundedCornerShape(3.dp)
                            )
                    )
                }
            }
        }

        if (row.hues != null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 3,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
            ) {
                row.hues.forEach { hue ->
                    HueChipItem(
                        hue = hue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HueChipItem(
    hue: HueItemUi,
    modifier: Modifier = Modifier
) {
    Surface(
        color = CalibreSurface2,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(hue.r, hue.g, hue.b))
                    .border(1.dp, CalibreLine, RoundedCornerShape(5.dp))
            )
            Text(
                text = hue.name,
                style = MaterialTheme.typography.labelMedium,
                color = CalibreFg
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = hue.devText,
                style = MaterialTheme.typography.labelMedium,
                color = stateColor(hue.state)
            )
        }
    }
}

@Composable
fun StatePill(
    text: String,
    state: MeasureState,
    small: Boolean,
    testTag: String? = null
) {
    val col = stateColor(state)
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, col),
        modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
    ) {
        Text(
            text = text,
            style = if (small) {
                MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
            } else {
                MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
            },
            color = col,
            modifier = Modifier.padding(
                horizontal = if (small) 10.dp else 12.dp,
                vertical = if (small) 2.dp else 4.dp
            )
        )
    }
}

@Composable
fun CalibrationGaugeBar(
    spec: GaugeSpec,
    state: MeasureState
) {
    val markerColor = stateColor(state)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
        ) {
            val barHeight = 10.dp.toPx()
            val barTop = (size.height - barHeight) / 2f
            // Track background
            drawRoundRect(
                color = CalibreSurface2,
                topLeft = Offset(0f, barTop),
                size = Size(size.width, barHeight),
                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
            )
            // Green target band
            val bandX = size.width * spec.bandStartFraction
            val bandW = size.width * spec.bandWidthFraction
            drawRect(
                color = CalibreOk.copy(alpha = 0.35f),
                topLeft = Offset(bandX, barTop),
                size = Size(bandW, barHeight)
            )
            // Marker indicator
            val markerW = 4.dp.toPx()
            val markerX = (size.width * spec.markerFraction - markerW / 2f)
                .coerceIn(0f, (size.width - markerW).coerceAtLeast(0f))
            drawRoundRect(
                color = markerColor,
                topLeft = Offset(markerX, 0f),
                size = Size(markerW, size.height),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = spec.ticks.first,
                style = MaterialTheme.typography.labelSmall,
                color = CalibreMuted
            )
            Text(
                text = spec.ticks.second,
                style = MaterialTheme.typography.labelSmall,
                color = CalibreMuted
            )
            Text(
                text = spec.ticks.third,
                style = MaterialTheme.typography.labelSmall,
                color = CalibreMuted
            )
        }
    }
}

@Composable
private fun TipsCard(
    tips: List<String>,
    testTag: String
) {
    Surface(
        color = CalibreSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CalibreLine),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.tips_title).uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = CalibreMuted
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                tips.forEach { tip ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge,
                            color = CalibreFg
                        )
                        Text(
                            text = tip,
                            style = MaterialTheme.typography.bodyLarge,
                            color = CalibreFg
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LogsCard(
    logs: List<LogEntry>,
    onClear: () -> Unit,
    cardTag: String,
    clearBtnTag: String
) {
    Surface(
        color = CalibreSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CalibreLine),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(cardTag)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.kept_measures_title).uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = CalibreMuted
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                logs.forEach { item ->
                    Surface(
                        color = CalibreSurface2,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(stateColor(item.state))
                            )
                            Text(
                                text = item.left,
                                style = MaterialTheme.typography.labelMedium,
                                color = CalibreFg
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = item.right,
                                style = MaterialTheme.typography.labelMedium,
                                color = CalibreMuted
                            )
                        }
                    }
                }
            }
            Row {
                TextButton(
                    onClick = onClear,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag(clearBtnTag)
                ) {
                    Text(
                        text = stringResource(R.string.btn_clear_all),
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

@Composable
private fun HowItWorksCard(
    paragraphs: List<String>,
    testTag: String
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        color = CalibreSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CalibreLine),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { expanded = !expanded }
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (expanded) "▾" else "▸",
                    style = MaterialTheme.typography.bodyLarge,
                    color = CalibreBeam
                )
                Text(
                    text = stringResource(R.string.how_it_works_title),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = CalibreBeam
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    paragraphs.forEach { p ->
                        Text(
                            text = p,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CalibreMuted
                        )
                    }
                }
            }
        }
    }
}
