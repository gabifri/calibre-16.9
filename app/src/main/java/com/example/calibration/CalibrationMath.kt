package com.example.calibration

import android.graphics.Bitmap
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sqrt

const val TARGET_RATIO = 16.0 / 9.0

data class PointD(val x: Double, val y: Double)

enum class MeasureState(val order: Int, val labelFr: String) {
    NONE(0, "Non mesuré"),
    OK(1, "Bon"),
    WARN(2, "Presque"),
    BAD(3, "À régler")
}

data class GeoMeasureResult(
    val ratio: Double,
    val f: Double,
    val fRel: Double,
    val estimated: Boolean,
    val suspicious: Boolean
)

data class GeoEvaluation(
    val state: MeasureState,
    val pillText: String,
    val ratioText: String,
    val devText: String,
    val msgText: String,
    val gaugeVisible: Boolean,
    val gaugeMarkerFraction: Float,
    val tips: List<String>,
    val canKeep: Boolean,
    val rawRatio: Double = 0.0,
    val rawDev: Double = 0.0
)

data class PatchDef(
    val id: String,
    val x0: Double,
    val y0: Double,
    val x1: Double,
    val y1: Double,
    val rgb: IntArray
)

data class RgbSample(val r: Double, val g: Double, val b: Double)

data class CastReading(val temp: Double, val tint: Double)

data class StepReading(val steps: List<Boolean>, val n: Int)

data class HueReading(val name: String, val rgb: RgbSample, val dev: Double?)

data class ColorAnalysisResult(
    val exposure: String, // "ok", "over", "under"
    val l100: Double,
    val cast: CastReading?,
    val gamma: Double?,
    val blacks: StepReading,
    val whites: StepReading?,
    val hues: List<HueReading>
)

data class GaugeSpec(
    val bandStartFraction: Float,
    val bandWidthFraction: Float,
    val markerFraction: Float,
    val ticks: Triple<String, String, String>
)

data class ColorMetricRow(
    val label: String,
    val valueText: String,
    val state: MeasureState,
    val gauge: GaugeSpec? = null,
    val steps: List<Boolean>? = null,
    val hues: List<HueItemUi>? = null
)

data class HueItemUi(
    val name: String,
    val r: Int,
    val g: Int,
    val b: Int,
    val devText: String,
    val state: MeasureState
)

data class ColorEvaluation(
    val state: MeasureState,
    val pillText: String,
    val msgText: String,
    val exposureWarning: String?,
    val rows: List<ColorMetricRow>,
    val tips: List<String>,
    val canKeep: Boolean,
    val logSummaryLeft: String = ""
)

data class LogEntry(
    val state: MeasureState,
    val left: String,
    val right: String
)

object CalibrationMath {

    val FORMATS = listOf(
        "4:3" to (4.0 / 3.0),
        "5:4" to 1.25,
        "16:10" to 1.6,
        "16:9" to TARGET_RATIO,
        "21:9" to (64.0 / 27.0)
    )

    val BARS = listOf(
        "white" to intArrayOf(255, 255, 255),
        "yellow" to intArrayOf(255, 255, 0),
        "cyan" to intArrayOf(0, 255, 255),
        "green" to intArrayOf(0, 255, 0),
        "magenta" to intArrayOf(255, 0, 255),
        "red" to intArrayOf(255, 0, 0),
        "blue" to intArrayOf(0, 0, 255),
        "black" to intArrayOf(0, 0, 0)
    )

    val NEAR_BLACK = intArrayOf(0, 6, 12, 18, 24, 30)
    val NEAR_WHITE = intArrayOf(225, 231, 237, 243, 249, 255)

    val HUES = listOf(
        Triple("Rouge", "bar-red", 0.0),
        Triple("Jaune", "bar-yellow", 60.0),
        Triple("Vert", "bar-green", 120.0),
        Triple("Cyan", "bar-cyan", 180.0),
        Triple("Bleu", "bar-blue", 240.0),
        Triple("Magenta", "bar-magenta", 300.0)
    )

    val PATCHES: List<PatchDef> = buildList {
        fun addPatch(id: String, x0: Double, y0: Double, x1: Double, y1: Double, rgb: IntArray) {
            add(
                PatchDef(
                    id = id,
                    x0 = x0 / 1920.0,
                    y0 = y0 / 1080.0,
                    x1 = x1 / 1920.0,
                    y1 = y1 / 1080.0,
                    rgb = rgb
                )
            )
        }
        for (i in 0 until 8) {
            addPatch(
                "bar-${BARS[i].first}",
                120.0 + i * 210.0,
                110.0,
                330.0 + i * 210.0,
                440.0,
                BARS[i].second
            )
        }
        for (i in 0..10) {
            val v = (255.0 * i / 10.0).roundToInt()
            addPatch(
                "g$i",
                120.0 + i * 1680.0 / 11.0,
                500.0,
                120.0 + (i + 1) * 1680.0 / 11.0,
                780.0,
                intArrayOf(v, v, v)
            )
        }
        for (i in 0 until 6) {
            val nb = NEAR_BLACK[i]
            addPatch(
                "b$i",
                120.0 + i * 130.0,
                840.0,
                250.0 + i * 130.0,
                990.0,
                intArrayOf(nb, nb, nb)
            )
            val nw = NEAR_WHITE[i]
            addPatch(
                "w$i",
                1020.0 + i * 130.0,
                840.0,
                1150.0 + i * 130.0,
                990.0,
                intArrayOf(nw, nw, nw)
            )
        }
    }

    fun fmt(n: Double, decimals: Int): String {
        return String.format(Locale.US, "%.${decimals}f", n).replace('.', ',')
    }

    fun sgn(v: Double, decimals: Int): String {
        val prefix = if (v < 0) "−" else "+"
        return prefix + fmt(abs(v), decimals)
    }

    fun clamp(v: Double, a: Double, b: Double): Double = min(b, max(a, v))

    private fun cross(a: DoubleArray, b: DoubleArray): DoubleArray = doubleArrayOf(
        a[1] * b[2] - a[2] * b[1],
        a[2] * b[0] - a[0] * b[2],
        a[0] * b[1] - a[1] * b[0]
    )

    private fun dot(a: DoubleArray, b: DoubleArray): Double =
        a[0] * b[0] + a[1] * b[1] + a[2] * b[2]

    fun isConvex(p: List<PointD>): Boolean {
        if (p.size != 4) return false
        var s = 0.0
        for (i in 0 until 4) {
            val a = p[i]
            val b = p[(i + 1) % 4]
            val c = p[(i + 2) % 4]
            val z = (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x)
            if (abs(z) < 1e-6) return false
            if (s == 0.0) s = sign(z)
            else if (sign(z) != s) return false
        }
        return true
    }

    // pts: TL, TR, BR, BL in photo pixels. Returns width/height of the real rectangle (Zhang & He, 2007).
    fun measure(pts: List<PointD>, width: Int, height: Int): GeoMeasureResult? {
        if (pts.size != 4) return null
        val cx = width / 2.0
        val cy = height / 2.0
        val L = max(width, height).toDouble()
        val m1 = doubleArrayOf(pts[0].x - cx, pts[0].y - cy, 1.0)
        val m2 = doubleArrayOf(pts[1].x - cx, pts[1].y - cy, 1.0)
        val m3 = doubleArrayOf(pts[3].x - cx, pts[3].y - cy, 1.0)
        val m4 = doubleArrayOf(pts[2].x - cx, pts[2].y - cy, 1.0)
        val c14 = cross(m1, m4)
        val d2 = dot(cross(m2, m4), m3)
        val d3 = dot(cross(m3, m4), m2)
        if (abs(d2) < 1e-6 || abs(d3) < 1e-6) return null
        val k2 = dot(c14, m3) / d2
        val k3 = dot(c14, m2) / d3
        val n2 = doubleArrayOf(k2 * m2[0] - m1[0], k2 * m2[1] - m1[1], k2 * m2[2] - m1[2])
        val n3 = doubleArrayOf(k3 * m3[0] - m1[0], k3 * m3[1] - m1[1], k3 * m3[2] - m1[2])
        var f = 0.75 * L
        var estimated = false
        var suspicious = false
        val den = n2[2] * n3[2]
        if (abs(den) > 2e-3) {
            val f2 = -(n2[0] * n3[0] + n2[1] * n3[1]) / den
            if (f2 > 0) {
                val fe = sqrt(f2)
                if (fe >= 0.35 * L && fe <= 2.2 * L) {
                    f = fe
                    estimated = true
                }
            }
            if (!estimated && abs(den) > 0.02) suspicious = true
        }
        val inv = 1.0 / (f * f)
        val a2 = (n2[0] * n2[0] + n2[1] * n2[1]) * inv + n2[2] * n2[2]
        val a3 = (n3[0] * n3[0] + n3[1] * n3[1]) * inv + n3[2] * n3[2]
        if (!(a2 > 0 && a3 > 0)) return null
        return GeoMeasureResult(
            ratio = sqrt(a2 / a3),
            f = f,
            fRel = f / L,
            estimated = estimated,
            suspicious = suspicious
        )
    }

    class Homography(
        private val a: Double,
        private val b: Double,
        private val c: Double,
        private val d: Double,
        private val e: Double,
        private val f: Double,
        private val g: Double,
        private val h: Double
    ) {
        fun map(u: Double, v: Double): PointD {
            val w = g * u + h * v + 1.0
            return PointD((a * u + b * v + c) / w, (d * u + e * v + f) / w)
        }
    }

    fun homography(q: List<PointD>): Homography? {
        if (q.size != 4) return null
        val x0 = q[0].x; val y0 = q[0].y
        val x1 = q[1].x; val y1 = q[1].y
        val x2 = q[2].x; val y2 = q[2].y
        val x3 = q[3].x; val y3 = q[3].y
        val dx1 = x1 - x2; val dx2 = x3 - x2; val dx3 = x0 - x1 + x2 - x3
        val dy1 = y1 - y2; val dy2 = y3 - y2; val dy3 = y0 - y1 + y2 - y3
        val den = dx1 * dy2 - dx2 * dy1
        if (abs(den) < 1e-9) return null
        val g = (dx3 * dy2 - dx2 * dy3) / den
        val h = (dx1 * dy3 - dx3 * dy1) / den
        val a = x1 - x0 + g * x1
        val b = x3 - x0 + h * x3
        val c = x0
        val d = y1 - y0 + g * y1
        val e = y3 - y0 + h * y3
        val f = y0
        return Homography(a, b, c, d, e, f, g, h)
    }

    fun idealQuad(pts: List<PointD>, width: Int, height: Int): List<PointD>? {
        if (!isConvex(pts)) return null
        val r = measure(pts, width, height) ?: return null
        val hm = homography(pts) ?: return null
        var u0 = 0.0
        var v0 = 0.0
        if (r.ratio >= TARGET_RATIO) {
            u0 = (r.ratio - TARGET_RATIO) / (2.0 * r.ratio)
        } else {
            v0 = (1.0 - r.ratio / TARGET_RATIO) / 2.0
        }
        return listOf(
            hm.map(u0, v0),
            hm.map(1.0 - u0, v0),
            hm.map(1.0 - u0, 1.0 - v0),
            hm.map(u0, 1.0 - v0)
        )
    }

    private fun samplePatch(
        pixels: IntArray,
        width: Int,
        height: Int,
        hm: Homography,
        p: PatchDef
    ): RgbSample? {
        val N = 8
        var r = 0.0
        var g = 0.0
        var b = 0.0
        var n = 0
        val du = p.x1 - p.x0
        val dv = p.y1 - p.y0
        for (i in 0 until N) {
            for (j in 0 until N) {
                val m = hm.map(
                    p.x0 + du * (0.2 + 0.6 * (i + 0.5) / N),
                    p.y0 + dv * (0.2 + 0.6 * (j + 0.5) / N)
                )
                val x = m.x.roundToInt()
                val y = m.y.roundToInt()
                if (x < 0 || y < 0 || x >= width || y >= height) continue
                val c = pixels[y * width + x]
                r += (c shr 16) and 0xFF
                g += (c shr 8) and 0xFF
                b += c and 0xFF
                n++
            }
        }
        return if (n >= N * N / 2) RgbSample(r / n, g / n, b / n) else null
    }

    private fun lum(c: RgbSample): Double = 0.299 * c.r + 0.587 * c.g + 0.114 * c.b

    private fun lin(v: Double): Double = (max(v, 0.0) / 255.0).pow(2.2)

    private fun hueOf(c: RgbSample): Double? {
        val mx = max(c.r, max(c.g, c.b))
        val mn = min(c.r, min(c.g, c.b))
        val d = mx - mn
        if (d < 40.0) return null
        var h = when (mx) {
            c.r -> ((c.g - c.b) / d) % 6.0
            c.g -> (c.b - c.r) / d + 2.0
            else -> (c.r - c.g) / d + 4.0
        }
        h *= 60.0
        if (h < 0.0) h += 360.0
        return h
    }

    fun analyze(
        pixels: IntArray,
        width: Int,
        height: Int,
        quad: List<PointD>
    ): ColorAnalysisResult? {
        val hm = homography(quad) ?: return null
        val samples = HashMap<String, RgbSample>(PATCHES.size)
        for (patch in PATCHES) {
            val s = samplePatch(pixels, width, height, hm, patch) ?: return null
            samples[patch.id] = s
        }
        val g0 = samples["g0"] ?: return null
        val g10 = samples["g10"] ?: return null
        val l0 = lum(g0)
        val l100 = lum(g10)
        val exposure = when {
            l100 >= 250.0 -> "over"
            l100 < 70.0 -> "under"
            else -> "ok"
        }

        var gamma: Double? = null
        if (exposure != "over" && l100 - l0 > 30.0) {
            val L0 = lin(l0)
            val L1 = lin(l100)
            var num = 0.0
            var den = 0.0
            for (i in 2..9) {
                val x = i / 10.0
                val gi = samples["g$i"] ?: continue
                val n = (lin(lum(gi)) - L0) / (L1 - L0)
                if (n > 0.001) {
                    val lx = ln(x)
                    num += lx * ln(n)
                    den += lx * lx
                }
            }
            if (den > 0.0) {
                gamma = clamp(num / den, 0.8, 4.0)
            }
        }

        fun steps(prefix: String): List<Boolean> {
            val out = ArrayList<Boolean>(5)
            for (k in 1..5) {
                val cur = samples["$prefix$k"]!!
                val prev = samples["$prefix${k - 1}"]!!
                out.add(lum(cur) - lum(prev) >= 1.5)
            }
            return out
        }

        val bs = steps("b")
        val ws = if (exposure == "over") null else steps("w")

        var t = 0.0
        var gr = 0.0
        var m = 0
        for (i in 3..9) {
            val c = samples["g$i"] ?: continue
            if (c.g < 25.0) continue
            t += (c.r - c.b) / c.g
            gr += c.g / ((c.r + c.b) / 2.0) - 1.0
            m++
        }
        val cast = if (m > 0) CastReading(temp = 100.0 * t / m, tint = 100.0 * gr / m) else null

        val hues = HUES.map { (name, patchId, targetHue) ->
            val c = samples[patchId]!!
            val h = hueOf(c)
            val dev = if (h == null) null else ((h - targetHue + 540.0) % 360.0) - 180.0
            HueReading(name = name, rgb = c, dev = dev)
        }

        return ColorAnalysisResult(
            exposure = exposure,
            l100 = l100,
            cast = cast,
            gamma = gamma,
            blacks = StepReading(steps = bs, n = bs.count { it }),
            whites = ws?.let { StepReading(steps = it, n = it.count { b -> b }) },
            hues = hues
        )
    }

    fun evaluateGeo(pts: List<PointD>, width: Int, height: Int): GeoEvaluation {
        val r = if (isConvex(pts)) measure(pts, width, height) else null
        if (r == null) {
            return GeoEvaluation(
                state = MeasureState.NONE,
                pillText = "Coins à replacer",
                ratioText = "–",
                devText = " ",
                msgText = "Les quatre points doivent former un quadrilatère sans croisement.",
                gaugeVisible = false,
                gaugeMarkerFraction = 0.5f,
                tips = emptyList(),
                canKeep = false
            )
        }
        val dev = r.ratio / TARGET_RATIO - 1.0
        val adev = abs(dev)
        val tips = mutableListOf<String>()
        val near = FORMATS.map { (name, target) -> name to abs(r.ratio / target - 1.0) }
            .minByOrNull { it.second }!!

        val state: MeasureState
        val label: String
        val msg: String
        if (adev <= 0.01) {
            state = MeasureState.OK
            label = "Nickel"
            msg = "Ton image est en 16:9, à la précision de la mesure près (environ ±1 %)."
        } else {
            state = if (adev <= 0.025) MeasureState.WARN else MeasureState.BAD
            label = if (adev <= 0.025) "Presque" else "À régler"
            msg = if (near.first != "16:9" && near.second < 0.03) {
                "Le projecteur affiche du ${near.first}."
            } else if (dev > 0) {
                "L'image est étirée en largeur de ${fmt(adev * 100.0, 1)} %."
            } else {
                "L'image est étirée en hauteur de ${fmt(adev * 100.0, 1)} %."
            }
            tips.add("Menu du projecteur, format d'image : choisis 16:9 (évite Auto, Plein ou Zoom).")
            tips.add("Sortie de l'ordinateur en 1920 × 1080 ou 3840 × 2160, sans mise à l'échelle.")
            tips.add("Corrige le trapèze (keystone) si les bords ne sont pas parallèles, puis refais une photo.")
        }
        if (state == MeasureState.WARN) {
            tips.add(
                0,
                "Cet écart est proche de la précision d'une mesure sur photo (environ ±1 %). Refais une ou deux photos, un peu plus loin en zoomant, et compare avec « Garder cette mesure » avant de toucher aux réglages."
            )
        }
        if (r.suspicious) {
            tips.add(
                0,
                "Les coins semblent mal placés, ou l'image n'est pas rectangulaire. Vérifie chaque coin avec la loupe."
            )
        }
        val markerPos = clamp((dev / 0.03 + 1.0) / 2.0, 0.0, 1.0).toFloat()
        return GeoEvaluation(
            state = state,
            pillText = label,
            ratioText = fmt(r.ratio, 3),
            devText = "${sgn(dev * 100.0, 1)} %",
            msgText = msg,
            gaugeVisible = true,
            gaugeMarkerFraction = markerPos,
            tips = tips,
            canKeep = true,
            rawRatio = r.ratio,
            rawDev = dev
        )
    }

    private fun castState(v: Double): MeasureState {
        val a = abs(v)
        return when {
            a <= 3.0 -> MeasureState.OK
            a <= 7.0 -> MeasureState.WARN
            else -> MeasureState.BAD
        }
    }

    private fun gammaState(g: Double): MeasureState = when {
        g in 2.0..2.4 -> MeasureState.OK
        g in 1.8..2.7 -> MeasureState.WARN
        else -> MeasureState.BAD
    }

    private fun stepState(n: Int): MeasureState = when {
        n >= 3 -> MeasureState.OK
        n == 2 -> MeasureState.WARN
        else -> MeasureState.BAD
    }

    private fun hueState(dv: Double): MeasureState {
        val a = abs(dv)
        return when {
            a <= 12.0 -> MeasureState.OK
            a <= 25.0 -> MeasureState.WARN
            else -> MeasureState.BAD
        }
    }

    private fun worst(states: List<MeasureState>): MeasureState =
        states.maxByOrNull { it.order } ?: MeasureState.NONE

    fun evaluateColor(
        pixels: IntArray,
        width: Int,
        height: Int,
        pts: List<PointD>
    ): ColorEvaluation {
        val res = if (isConvex(pts)) analyze(pixels, width, height, pts) else null
        if (res == null) {
            return ColorEvaluation(
                state = MeasureState.NONE,
                pillText = "Coins à replacer",
                msgText = "Les quatre points doivent entourer toute l'image projetée.",
                exposureWarning = null,
                rows = emptyList(),
                tips = emptyList(),
                canKeep = false
            )
        }

        val tips = mutableListOf<String>()
        val states = mutableListOf<MeasureState>()
        val rows = mutableListOf<ColorMetricRow>()

        val exposureWarning = when (res.exposure) {
            "over" -> "Photo surexposée : le blanc est saturé. Baisse l'exposition (touche l'écran de l'appareil photo et fais glisser le soleil vers le bas), puis refais la photo. Gamma et blancs ne sont pas mesurés."
            "under" -> "Photo trop sombre : monte l'exposition (touche l'écran de l'appareil photo et fais glisser le soleil vers le haut), puis refais la photo."
            else -> null
        }

        val t = res.cast?.temp
        val v = res.cast?.tint
        if (t != null && v != null) {
            val stTemp = castState(t)
            states.add(stTemp)
            val tempSuffix = when {
                t > 3.0 -> "chaud"
                t < -3.0 -> "froid"
                else -> "neutre"
            }
            rows.add(
                ColorMetricRow(
                    label = "Température",
                    valueText = "${sgn(t, 1)} % $tempSuffix",
                    state = stTemp,
                    gauge = GaugeSpec(
                        bandStartFraction = 0.35f,
                        bandWidthFraction = 0.30f,
                        markerFraction = clamp((t / 10.0 + 1.0) / 2.0, 0.0, 1.0).toFloat(),
                        ticks = Triple("Froid −10 %", "0", "Chaud +10 %")
                    )
                )
            )
            if (t > 3.0) tips.add("Image trop chaude : choisis une température de couleur plus haute (6500 K) ou baisse le rouge et monte le bleu dans les gains RVB.")
            if (t < -3.0) tips.add("Image trop froide : choisis une température de couleur plus basse (6500 K) ou monte le rouge et baisse le bleu dans les gains RVB.")

            val stTint = castState(v)
            states.add(stTint)
            val tintSuffix = when {
                v > 3.0 -> "vert"
                v < -3.0 -> "magenta"
                else -> "neutre"
            }
            rows.add(
                ColorMetricRow(
                    label = "Teinte",
                    valueText = "${sgn(v, 1)} % $tintSuffix",
                    state = stTint,
                    gauge = GaugeSpec(
                        bandStartFraction = 0.35f,
                        bandWidthFraction = 0.30f,
                        markerFraction = clamp((v / 10.0 + 1.0) / 2.0, 0.0, 1.0).toFloat(),
                        ticks = Triple("Magenta −10 %", "0", "Vert +10 %")
                    )
                )
            )
            if (v > 3.0) tips.add("Dominante verte : baisse le vert, ou monte un peu le rouge et le bleu.")
            if (v < -3.0) tips.add("Dominante magenta : monte le vert, ou baisse un peu le rouge et le bleu.")
        }

        if (res.gamma != null) {
            val stGamma = gammaState(res.gamma)
            states.add(stGamma)
            rows.add(
                ColorMetricRow(
                    label = "Gamma (indicatif)",
                    valueText = "≈ ${fmt(res.gamma, 2)}",
                    state = stGamma,
                    gauge = GaugeSpec(
                        bandStartFraction = 0.3333f,
                        bandWidthFraction = 0.3333f,
                        markerFraction = clamp((res.gamma - 1.6) / 1.2, 0.0, 1.0).toFloat(),
                        ticks = Triple("1,6", "visé 2,2", "2,8")
                    )
                )
            )
            if (res.gamma < 2.0) tips.add("Tons moyens trop clairs, image délavée : monte le gamma (autour de 2,2) ou baisse la luminosité.")
            if (res.gamma > 2.4) tips.add("Tons moyens trop sombres : baisse le gamma vers 2,2.")
        } else {
            rows.add(
                ColorMetricRow(
                    label = "Gamma (indicatif)",
                    valueText = "Mesure impossible",
                    state = MeasureState.NONE
                )
            )
        }

        val stBlacks = stepState(res.blacks.n)
        states.add(stBlacks)
        rows.add(
            ColorMetricRow(
                label = "Noirs",
                valueText = "${res.blacks.n} / 5 carrés distincts",
                state = stBlacks,
                steps = res.blacks.steps
            )
        )
        if (stBlacks != MeasureState.OK) {
            tips.add("Noirs bouchés : monte un peu la luminosité (Brightness) jusqu'à voir chaque carré sombre.")
        }

        if (res.whites != null) {
            val stWhites = stepState(res.whites.n)
            states.add(stWhites)
            rows.add(
                ColorMetricRow(
                    label = "Blancs",
                    valueText = "${res.whites.n} / 5 carrés distincts",
                    state = stWhites,
                    steps = res.whites.steps
                )
            )
            if (stWhites != MeasureState.OK) {
                tips.add("Blancs écrêtés : baisse le contraste (Contrast, Gain) jusqu'à voir chaque carré clair.")
            }
        } else {
            rows.add(
                ColorMetricRow(
                    label = "Blancs",
                    valueText = "Mesure impossible",
                    state = MeasureState.NONE
                )
            )
        }

        val hueUiList = mutableListOf<HueItemUi>()
        val hueStates = mutableListOf<MeasureState>()
        for (h in res.hues) {
            val hs = if (h.dev == null) MeasureState.NONE else hueState(h.dev)
            hueStates.add(hs)
            hueUiList.add(
                HueItemUi(
                    name = h.name,
                    r = h.rgb.r.roundToInt().coerceIn(0, 255),
                    g = h.rgb.g.roundToInt().coerceIn(0, 255),
                    b = h.rgb.b.roundToInt().coerceIn(0, 255),
                    devText = if (h.dev == null) "–" else "${sgn(h.dev, 0)}°",
                    state = hs
                )
            )
        }
        val stHues = worst(hueStates)
        states.add(stHues)
        rows.add(
            ColorMetricRow(
                label = "Teintes des 6 couleurs",
                valueText = if (stHues == MeasureState.OK) "Écarts faibles" else "Écarts à revoir",
                state = stHues,
                hues = hueUiList
            )
        )
        if (stHues == MeasureState.WARN || stHues == MeasureState.BAD) {
            tips.add("Teintes décalées : choisis un mode image neutre (Cinéma, Standard), coupe les options Dynamique ou Vivid, puis ajuste Teinte et Saturation dans la gestion des couleurs.")
        }

        val total = worst(states)
        val badCount = states.count { it == MeasureState.WARN || it == MeasureState.BAD }
        val pillText = when (total) {
            MeasureState.OK -> "Nickel"
            MeasureState.WARN -> "Presque"
            else -> "À régler"
        }
        val msgText = if (total == MeasureState.OK) {
            "Couleurs proches de la cible."
        } else {
            "$badCount ${if (badCount > 1) "réglages à revoir." else "réglage à revoir."}"
        }
        if (tips.isEmpty() && res.exposure == "ok") {
            tips.add("Rien à corriger. Vérifie à l'œil les carrés sombres et clairs de la mire.")
        }

        val leftSummary = (if (t != null && v != null) "T ${sgn(t, 1)} · V ${sgn(v, 1)}" else "–") +
            " · γ " + (if (res.gamma != null) fmt(res.gamma, 2) else "–")

        return ColorEvaluation(
            state = total,
            pillText = pillText,
            msgText = msgText,
            exposureWarning = exposureWarning,
            rows = rows,
            tips = tips,
            canKeep = true,
            logSummaryLeft = leftSummary
        )
    }

    data class DetectionResult(val pts: List<PointD>, val threshold: Int)

    // Otsu threshold corner detection on a downsampled copy + diagonal snap on full image
    fun detectCornersAndSnap(bitmap: Bitmap, fullPixels: IntArray): DetectionResult {
        val srcW = bitmap.width
        val srcH = bitmap.height
        val w = min(320, srcW)
        val h = max(2, (srcH.toDouble() * w / srcW).roundToInt())
        val smallBmp = Bitmap.createScaledBitmap(bitmap, w, h, true)
        val n = w * h
        val smallPixels = IntArray(n)
        smallBmp.getPixels(smallPixels, 0, w, 0, 0, w, h)

        val lm = IntArray(n)
        val hist = IntArray(256)
        for (i in 0 until n) {
            val c = smallPixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            val l = (0.299 * r + 0.587 * g + 0.114 * b).toInt().coerceIn(0, 255)
            lm[i] = l
            hist[l]++
        }

        var sum = 0.0
        for (t in 0 until 256) sum += t * hist[t]
        var sumB = 0.0
        var wB = 0
        var best = 0.0
        var thr = 128
        for (t in 0 until 256) {
            wB += hist[t]
            if (wB == 0) continue
            val wF = n - wB
            if (wF == 0) break
            sumB += t * hist[t]
            val mB = sumB / wB
            val mF = (sum - sumB) / wF
            val bt = wB.toDouble() * wF.toDouble() * (mB - mF) * (mB - mF)
            if (bt > best) {
                best = bt
                thr = t
            }
        }

        var tl: PointD? = null
        var br: PointD? = null
        var tr: PointD? = null
        var bl: PointD? = null
        var tlv = Double.POSITIVE_INFINITY
        var brv = Double.NEGATIVE_INFINITY
        var trv = Double.NEGATIVE_INFINITY
        var blv = Double.POSITIVE_INFINITY
        var count = 0

        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val i = y * w + x
                if (lm[i] <= thr || lm[i - 1] <= thr || lm[i + 1] <= thr || lm[i - w] <= thr || lm[i + w] <= thr) continue
                count++
                val s = (x + y).toDouble()
                val df = (x - y).toDouble()
                if (s < tlv) { tlv = s; tl = PointD(x.toDouble(), y.toDouble()) }
                if (s > brv) { brv = s; br = PointD(x.toDouble(), y.toDouble()) }
                if (df > trv) { trv = df; tr = PointD(x.toDouble(), y.toDouble()) }
                if (df < blv) { blv = df; bl = PointD(x.toDouble(), y.toDouble()) }
            }
        }

        val frac = count.toDouble() / n.toDouble()
        if (tl == null || tr == null || br == null || bl == null || frac < 0.01 || frac > 0.85) {
            return DetectionResult(
                pts = listOf(
                    PointD(srcW * 0.18, srcH * 0.18),
                    PointD(srcW * 0.82, srcH * 0.18),
                    PointD(srcW * 0.82, srcH * 0.82),
                    PointD(srcW * 0.18, srcH * 0.82)
                ),
                threshold = thr
            )
        }

        val k = srcW.toDouble() / w.toDouble()
        val initialPts = mutableListOf(
            PointD((tl.x + 0.5) * k, (tl.y + 0.5) * k),
            PointD((tr.x + 0.5) * k, (tr.y + 0.5) * k),
            PointD((br.x + 0.5) * k, (br.y + 0.5) * k),
            PointD((bl.x + 0.5) * k, (bl.y + 0.5) * k)
        )
        val snapRadius = kotlin.math.ceil(srcW / 320.0 * 3.0).toInt()
        for (i in 0 until 4) {
            snapCorner(fullPixels, srcW, srcH, initialPts, i, snapRadius, thr)
        }
        return DetectionResult(pts = initialPts, threshold = thr)
    }

    fun snapCorner(
        pixels: IntArray,
        width: Int,
        height: Int,
        pts: MutableList<PointD>,
        cornerIndex: Int,
        radius: Int,
        thr: Int
    ): Boolean {
        val p = pts[cornerIndex]
        val dirs = arrayOf(
            intArrayOf(-1, -1),
            intArrayOf(1, -1),
            intArrayOf(1, 1),
            intArrayOf(-1, 1)
        )
        val dir = dirs[cornerIndex]
        val ix = -dir[0]
        val iy = -dir[1]
        var best = Double.NEGATIVE_INFINITY
        var bx = -1
        var by = -1
        val x0 = max(1, (p.x - radius).roundToInt())
        val x1 = min(width - 2, (p.x + radius).roundToInt())
        val y0 = max(1, (p.y - radius).roundToInt())
        val y1 = min(height - 2, (p.y + radius).roundToInt())

        fun lumAt(x: Int, y: Int): Double {
            val c = pixels[y * width + x]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            return 0.299 * r + 0.587 * g + 0.114 * b
        }

        for (y in y0..y1) {
            for (x in x0..x1) {
                if (lumAt(x, y) <= thr ||
                    lumAt(x + ix, y) <= thr ||
                    lumAt(x, y + iy) <= thr ||
                    lumAt(x + ix, y + iy) <= thr
                ) continue
                val s = (dir[0] * x + dir[1] * y).toDouble()
                if (s > best) {
                    best = s
                    bx = x
                    by = y
                }
            }
        }
        if (bx < 0) return false
        pts[cornerIndex] = PointD(bx + 0.5, by + 0.5)
        return true
    }
}
