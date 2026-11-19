package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.calibration.CalibrationMath
import com.example.calibration.MeasureState
import com.example.calibration.SampleGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun calibrationMath_measuresGeoAndColorSamplesAccurately() {
        // 1. Verify Format (Zhang & He 2007) on simulated perspective 16:9 sample
        val geoBmp = SampleGenerator.makeGeoSample()
        val geoPixels = IntArray(geoBmp.width * geoBmp.height)
        geoBmp.getPixels(geoPixels, 0, geoBmp.width, 0, 0, geoBmp.width, geoBmp.height)
        val geoDet = CalibrationMath.detectCornersAndSnap(geoBmp, geoPixels)
        assertEquals(4, geoDet.pts.size)
        val geoEval = CalibrationMath.evaluateGeo(geoDet.pts, geoBmp.width, geoBmp.height)
        assertTrue(
            "Expected valid ratio near 1.778, got ${geoEval.rawRatio}",
            abs(geoEval.rawRatio - 16.0 / 9.0) < 0.03
        )
        assertTrue(geoEval.canKeep)

        // 2. Verify Color analysis on simulated warm/gamma 2.5 perspective sample
        val colBmp = SampleGenerator.makeColorSample()
        val colPixels = IntArray(colBmp.width * colBmp.height)
        colBmp.getPixels(colPixels, 0, colBmp.width, 0, 0, colBmp.width, colBmp.height)
        val colDet = CalibrationMath.detectCornersAndSnap(colBmp, colPixels)
        val colEval = CalibrationMath.evaluateColor(colPixels, colBmp.width, colBmp.height, colDet.pts)
        assertTrue(colEval.canKeep)
        assertEquals(6, colEval.rows.size)
        assertTrue(colEval.state != MeasureState.NONE)
    }

    @Test
    fun app_displaysTabsAndNavigatesBetweenFormatCouleursAndMires() {
        composeTestRule.onNodeWithText("Calibre 16:9").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tab_mesure").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tab_couleurs").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tab_mire").assertIsDisplayed()

        // Switch to Couleurs tab and scroll to result card
        composeTestRule.onNodeWithTag("tab_couleurs").performClick()
        composeTestRule.onNodeWithTag("color_result_card").performScrollTo().assertIsDisplayed()

        // Switch to Mires tab and enter fullscreen format mire
        composeTestRule.onNodeWithTag("tab_mire").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("stage_geo").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("full_geo_button").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("fullscreen_mire_overlay").assertIsDisplayed()
    }
}
