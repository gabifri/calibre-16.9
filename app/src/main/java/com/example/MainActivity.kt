package com.example

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ads.AdMobManager
import com.example.ads.CalibreBannerAd
import com.example.calibration.CalibreTab
import com.example.calibration.CalibrationViewModel
import com.example.calibration.FullScreenMire
import com.example.ui.components.ColorResultsSection
import com.example.ui.components.CornerEditorSection
import com.example.ui.components.FormatResultsSection
import com.example.ui.components.FullScreenMireOverlay
import com.example.ui.components.MiresTabSection
import com.example.ui.theme.CalibreBeam
import com.example.ui.theme.CalibreBeamDim
import com.example.ui.theme.CalibreBg
import com.example.ui.theme.CalibreFg
import com.example.ui.theme.CalibreLine
import com.example.ui.theme.CalibreMuted
import com.example.ui.theme.CalibreSurface
import com.example.ui.theme.CalibreSurface2
import com.example.ui.theme.MyApplicationTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Keep screen on during projector calibration
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val isTv = isAndroidTv(this)

        setContent {
            MyApplicationTheme {
                CalibreApp(isTvDevice = isTv)
            }
        }
    }

    private fun isAndroidTv(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val isTvMode = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        val hasLeanback = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        return isTvMode || hasLeanback
    }
}

private fun decodeUriToBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }
    } catch (e: Exception) {
        null
    }
}

private fun createTempPhotoUri(context: Context): Uri? {
    return try {
        val dir = File(context.cacheDir, "photos").apply { mkdirs() }
        val file = File.createTempFile("calibre_capture_", ".jpg", dir)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (e: Exception) {
        null
    }
}

@Composable
fun CalibreApp(
    isTvDevice: Boolean = false,
    viewModel: CalibrationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current

    LaunchedEffect(isTvDevice) {
        viewModel.setTvMode(isTvDevice)
        if (!isTvDevice) {
            (context as? ComponentActivity)?.let { activity ->
                AdMobManager.initializeAndLoadAppOpenAd(
                    activity = activity,
                    canShowFullScreenAd = { viewModel.uiState.value.fullScreenMire == FullScreenMire.NONE }
                )
            }
        }
    }

    // Hide system bars when displaying a full-screen mire for accurate 16:9 projection
    DisposableEffect(uiState.fullScreenMire) {
        val window = (context as? ComponentActivity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (uiState.fullScreenMire != FullScreenMire.NONE) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {}
    }

    // Photo capture & gallery launchers
    var pendingForGeo by remember { mutableStateOf(true) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val takePicturePreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            viewModel.onPhotoLoaded(pendingForGeo, bmp)
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            val bmp = decodeUriToBitmap(context, uri)
            viewModel.onPhotoLoaded(pendingForGeo, bmp)
        }
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bmp = decodeUriToBitmap(context, uri)
            viewModel.onPhotoLoaded(pendingForGeo, bmp)
        }
    }

    fun launchCamera(forGeo: Boolean) {
        pendingForGeo = forGeo
        val uri = createTempPhotoUri(context)
        if (uri != null) {
            pendingCameraUri = uri
            try {
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                takePicturePreviewLauncher.launch(null)
            }
        } else {
            takePicturePreviewLauncher.launch(null)
        }
    }

    fun launchGallery(forGeo: Boolean) {
        pendingForGeo = forGeo
        pickMediaLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    // Handle back button on secondary tabs when not in full screen
    val defaultTab = if (uiState.isTvDevice) CalibreTab.MIRES else CalibreTab.FORMAT
    BackHandler(enabled = uiState.fullScreenMire == FullScreenMire.NONE && uiState.activeTab != defaultTab) {
        viewModel.selectTab(defaultTab)
    }

    if (uiState.fullScreenMire != FullScreenMire.NONE) {
        FullScreenMireOverlay(
            mire = uiState.fullScreenMire,
            onExit = { viewModel.setFullScreenMire(FullScreenMire.NONE) },
            onSwitchMire = { next -> viewModel.setFullScreenMire(next) }
        )
        return
    }

    Scaffold(
        containerColor = CalibreBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (!uiState.isTvDevice) {
                CalibreBannerAd()
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                // Header
                CalibreHeader()

                // Segmented Navigation Tabs (Format / Couleurs / Mires)
                CalibreTabsBar(
                    activeTab = uiState.activeTab,
                    onSelectTab = { viewModel.selectTab(it) }
                )

                // Tab Content
                when (uiState.activeTab) {
                    CalibreTab.FORMAT -> {
                        CornerEditorSection(
                            uidPrefix = "geo",
                            introText = "Affiche la mire de format (onglet Mires), éteins la lumière, puis photographie l'image projetée en entier, le plus en face possible.",
                            editorState = uiState.geoEditor,
                            idealQuad = uiState.geoIdealQuad,
                            hasOverlayOption = true,
                            onTakePhotoClick = { launchCamera(forGeo = true) },
                            onPickGalleryClick = { launchGallery(forGeo = true) },
                            onSelectCorner = { viewModel.selectCorner(forGeo = true, it) },
                            onStartDragCorner = { viewModel.setDragCorner(forGeo = true, it) },
                            onUpdateCornerPos = { idx, x, y ->
                                viewModel.updateCornerPosition(forGeo = true, idx, x, y)
                            },
                            onEndDragCorner = { viewModel.setDragCorner(forGeo = true, -1) },
                            onNudgeCorner = { dx, dy -> viewModel.nudgeCorner(forGeo = true, dx, dy) },
                            onSnapCorner = { viewModel.snapSelectedCorner(forGeo = true) },
                            onTogglePrecise = { viewModel.togglePreciseMode(forGeo = true, it) },
                            onToggleOverlay = { viewModel.toggleOverlay(it) },
                            onRestoreSample = { viewModel.restoreSample(forGeo = true) }
                        )

                        FormatResultsSection(
                            eval = uiState.geoEval,
                            logs = uiState.geoLogs,
                            onKeepMeasure = { viewModel.keepGeoMeasurement() },
                            onClearLogs = { viewModel.clearGeoLogs() }
                        )
                    }

                    CalibreTab.COULEURS -> {
                        CornerEditorSection(
                            uidPrefix = "col",
                            introText = "Affiche la mire de couleurs (onglet Mires). Pose le téléphone sur un support, verrouille l'exposition et la balance des blancs (appui long sur l'écran de l'appareil photo), puis photographie l'image projetée en entier.",
                            editorState = uiState.colorEditor,
                            idealQuad = null,
                            hasOverlayOption = false,
                            onTakePhotoClick = { launchCamera(forGeo = false) },
                            onPickGalleryClick = { launchGallery(forGeo = false) },
                            onSelectCorner = { viewModel.selectCorner(forGeo = false, it) },
                            onStartDragCorner = { viewModel.setDragCorner(forGeo = false, it) },
                            onUpdateCornerPos = { idx, x, y ->
                                viewModel.updateCornerPosition(forGeo = false, idx, x, y)
                            },
                            onEndDragCorner = { viewModel.setDragCorner(forGeo = false, -1) },
                            onNudgeCorner = { dx, dy -> viewModel.nudgeCorner(forGeo = false, dx, dy) },
                            onSnapCorner = { viewModel.snapSelectedCorner(forGeo = false) },
                            onTogglePrecise = { viewModel.togglePreciseMode(forGeo = false, it) },
                            onToggleOverlay = {},
                            onRestoreSample = { viewModel.restoreSample(forGeo = false) }
                        )

                        ColorResultsSection(
                            eval = uiState.colorEval,
                            logs = uiState.colorLogs,
                            onKeepMeasure = { viewModel.keepColorMeasurement() },
                            onClearLogs = { viewModel.clearColorLogs() }
                        )
                    }

                    CalibreTab.MIRES -> {
                        MiresTabSection(
                            isTvDevice = uiState.isTvDevice,
                            onOpenFullScreen = { viewModel.setFullScreenMire(it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CalibreHeader() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Glowing 16:9 mark from the web app (.mark: 32x18px, border 2px solid --beam, radius 3px)
            Box(
                modifier = Modifier
                    .size(width = 32.dp, height = 18.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(3.dp),
                        ambientColor = CalibreBeam,
                        spotColor = CalibreBeam
                    )
                    .clip(RoundedCornerShape(3.dp))
                    .background(CalibreBeamDim.copy(alpha = 0.4f))
                    .border(2.dp, CalibreBeam, RoundedCornerShape(3.dp))
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = CalibreFg
            )
        }
        Text(
            text = stringResource(R.string.app_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = CalibreMuted
        )
    }
}

@Composable
private fun CalibreTabsBar(
    activeTab: CalibreTab,
    onSelectTab: (CalibreTab) -> Unit
) {
    Surface(
        color = CalibreSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CalibreLine),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            val tabs = listOf(
                Triple(CalibreTab.FORMAT, stringResource(R.string.tab_format), "tab_mesure"),
                Triple(CalibreTab.COULEURS, stringResource(R.string.tab_couleurs), "tab_couleurs"),
                Triple(CalibreTab.MIRES, stringResource(R.string.tab_mires), "tab_mire")
            )
            tabs.forEach { (tab, label, tag) ->
                val selected = activeTab == tab
                var focused by remember { mutableStateOf(false) }

                Surface(
                    color = if (selected) CalibreSurface2 else CalibreSurface,
                    shape = RoundedCornerShape(9.dp),
                    border = if (focused) BorderStroke(2.dp, CalibreBeam) else null,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .onFocusChanged { focused = it.isFocused }
                        .clickable(role = Role.Tab) { onSelectTab(tab) }
                        .testTag(tag)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) CalibreFg else CalibreMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
