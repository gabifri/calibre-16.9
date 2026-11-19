package com.example.ads

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.BuildConfig
import com.example.ui.theme.CalibreMuted
import com.example.ui.theme.CalibreSurface
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd

object AdMobManager {
    // Tes vrais identifiants de production AdMob
    const val PROD_BANNER_AD_UNIT_ID = "ca-app-pub-1651965693559528/6059801338"
    const val PROD_APP_OPEN_AD_UNIT_ID = "ca-app-pub-1651965693559528/1239874449"

    // Identifiants de test officiels Google AdMob (utilisés uniquement en mode Debug/Émulateur
    // tant que l'application n'est pas encore validée/liée au Play Store sur AdMob)
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"

    val activeBannerAdUnitId: String
        get() = if (BuildConfig.DEBUG) TEST_BANNER_AD_UNIT_ID else PROD_BANNER_AD_UNIT_ID

    val activeAppOpenAdUnitId: String
        get() = if (BuildConfig.DEBUG) TEST_APP_OPEN_AD_UNIT_ID else PROD_APP_OPEN_AD_UNIT_ID

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var hasShownLaunchAd = false

    fun isRobolectricTest(): Boolean = "robolectric".equals(Build.FINGERPRINT, ignoreCase = true)

    fun initializeAndLoadAppOpenAd(
        activity: Activity,
        canShowFullScreenAd: () -> Boolean
    ) {
        if (isRobolectricTest()) return
        try {
            MobileAds.initialize(activity) {
                loadAndShowLaunchAppOpenAd(activity, canShowFullScreenAd)
            }
        } catch (_: Throwable) {
            // Ignore in unsupported environments
        }
    }

    private fun loadAndShowLaunchAppOpenAd(
        activity: Activity,
        canShowFullScreenAd: () -> Boolean
    ) {
        if (hasShownLaunchAd || isLoadingAd || appOpenAd != null) return
        isLoadingAd = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            activity,
            activeAppOpenAdUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    if (!hasShownLaunchAd && !isShowingAd && canShowFullScreenAd() && !activity.isFinishing) {
                        showAppOpenAdIfAvailable(activity)
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                }
            }
        )
    }

    fun showAppOpenAdIfAvailable(activity: Activity) {
        val ad = appOpenAd ?: return
        if (isShowingAd || hasShownLaunchAd) return

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAd = false
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
                hasShownLaunchAd = true
            }
        }
        isShowingAd = true
        hasShownLaunchAd = true
        ad.show(activity)
    }
}

@Composable
fun CalibreBannerAd(
    modifier: Modifier = Modifier
) {
    val isPreview = LocalInspectionMode.current
    if (isPreview || AdMobManager.isRobolectricTest()) return

    var adStatusText by remember {
        mutableStateOf(
            if (BuildConfig.DEBUG) "Chargement de la bannière AdMob (mode test)…" else null
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .background(CalibreSurface)
            .navigationBarsPadding()
            .padding(vertical = 4.dp)
            .testTag("admob_banner_container")
    ) {
        if (adStatusText != null) {
            Text(
                text = adStatusText!!,
                style = MaterialTheme.typography.labelSmall,
                color = CalibreMuted
            )
        }
        AndroidView(
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = AdMobManager.activeBannerAdUnitId
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            adStatusText = null
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            if (BuildConfig.DEBUG) {
                                adStatusText = "AdMob (Debug): ${error.message}"
                            }
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
