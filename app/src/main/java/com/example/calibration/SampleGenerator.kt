package com.example.calibration

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

object SampleGenerator {

    private fun cameraProj(width: Int): (Double, Double) -> PointD {
        val f = 0.75 * width
        val th = -24.0 * PI / 180.0
        val ph = 7.0 * PI / 180.0
        val d = 22.0
        return { X, Y ->
            val X1 = X * cos(th) + 1.2
            val Z1 = -X * sin(th)
            val Y2 = Y * cos(ph) - Z1 * sin(ph) - 0.8
            val Z2 = Y * sin(ph) + Z1 * cos(ph)
            val Zc = Z2 + d
            PointD(
                x = width / 2.0 + f * X1 / Zc,
                y = 600.0 + f * Y2 / Zc
            )
        }
    }

    private fun sampleBase(width: Int, height: Int): Pair<Bitmap, Canvas> {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = RadialGradient(
            width / 2f,
            height / 2f,
            950f,
            intArrayOf(Color.parseColor("#1B1E27"), Color.parseColor("#07080B")),
            floatArrayOf(100f / 950f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bmp to canvas
    }

    private fun poly(canvas: Canvas, list: List<PointD>, colorInt: Int) {
        if (list.isEmpty()) return
        val path = Path()
        list.forEachIndexed { idx, p ->
            if (idx == 0) path.moveTo(p.x.toFloat(), p.y.toFloat())
            else path.lineTo(p.x.toFloat(), p.y.toFloat())
        }
        path.close()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = colorInt
        }
        canvas.drawPath(path, paint)
    }

    fun makeGeoSample(): Bitmap {
        val W = 1600
        val H = 1200
        val (bmp, canvas) = sampleBase(W, H)
        val proj = cameraProj(W)
        poly(
            canvas,
            listOf(proj(-8.0, -4.5), proj(8.0, -4.5), proj(8.0, 4.5), proj(-8.0, 4.5)),
            Color.parseColor("#F3F3F0")
        )
        poly(
            canvas,
            listOf(proj(-7.6, -4.2), proj(7.6, -4.2), proj(7.6, 4.2), proj(-7.6, 4.2)),
            Color.parseColor("#10131A")
        )
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.parseColor("#3A404C")
        }
        for (i in -7..7) {
            val a = proj(i.toDouble(), -4.2)
            val b = proj(i.toDouble(), 4.2)
            canvas.drawLine(a.x.toFloat(), a.y.toFloat(), b.x.toFloat(), b.y.toFloat(), linePaint)
        }
        for (j in -4..4) {
            val a2 = proj(-7.6, j.toDouble())
            val b2 = proj(7.6, j.toDouble())
            canvas.drawLine(a2.x.toFloat(), a2.y.toFloat(), b2.x.toFloat(), b2.y.toFloat(), linePaint)
        }
        return bmp
    }

    // Warm cast, gamma 2.5 and slightly lifted blacks, so the example shows real findings.
    fun makeColorSample(): Bitmap {
        val W = 1600
        val H = 1200
        val (bmp, canvas) = sampleBase(W, H)
        val proj = cameraProj(W)
        val quad = listOf(proj(-8.0, -4.5), proj(8.0, -4.5), proj(8.0, 4.5), proj(-8.0, 4.5))
        val hm = CalibrationMath.homography(quad) ?: return bmp
        val gain = doubleArrayOf(1.03, 1.01, 0.97)
        val expo = 0.86
        val bl = 0.0008
        val gam = 2.5

        fun col(rgb: IntArray): Int {
            val mapped = IntArray(3)
            for (i in 0..2) {
                val L = bl + (1.0 - bl) * (rgb[i] / 255.0).pow(gam)
                mapped[i] = CalibrationMath.clamp(
                    L.pow(1.0 / 2.2) * 255.0 * expo * gain[i],
                    0.0,
                    255.0
                ).roundToInt()
            }
            return Color.rgb(mapped[0], mapped[1], mapped[2])
        }

        fun rect(u0: Double, v0: Double, u1: Double, v1: Double, rgb: IntArray) {
            poly(
                canvas,
                listOf(hm.map(u0, v0), hm.map(u1, v0), hm.map(u1, v1), hm.map(u0, v1)),
                col(rgb)
            )
        }

        rect(0.0, 0.0, 1.0, 1.0, intArrayOf(255, 255, 255))
        rect(40.0 / 1920.0, 40.0 / 1080.0, 1880.0 / 1920.0, 1040.0 / 1080.0, intArrayOf(0, 0, 0))
        for (p in CalibrationMath.PATCHES) {
            rect(p.x0, p.y0, p.x1, p.y1, p.rgb)
        }
        return bmp
    }
}
