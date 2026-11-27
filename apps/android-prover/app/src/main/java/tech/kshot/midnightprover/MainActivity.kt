package tech.kshot.midnightprover

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

/**
 * Midnight Prover — a client for Midnight's official proof server 9.0.2
 * (Route 3, CLAUDE-AP-1 verdict: the arm64 server runs in a Linux
 * environment on this phone at localhost:6300; LAN/cloud servers work too).
 * It makes NO on-device performance claims: proving on phone hardware has
 * not been benchmarked yet.
 */
class MainActivity : ComponentActivity() {
    private val vm: ProverViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            val systemReduce = remember {
                runCatching { Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
                    .getOrDefault(false)
            }
            MidnightProverTheme(reduceMotion = systemReduce || state.settings.reduceMotion) {
                ProverApp(vm, state)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** Receives a job from another app: shared text, a shared file, or an opened file. */
    private fun handleIntent(intent: Intent?) {
        intent ?: return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (text != null) { vm.importPayload(text.toByteArray(), "a shared text"); return }
                @Suppress("DEPRECATION")
                val uri: Uri? = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                else intent.getParcelableExtra(Intent.EXTRA_STREAM)
                uri?.let { vm.importDocument(it, "a shared file") }
            }
            Intent.ACTION_VIEW -> intent.data?.let { vm.importDocument(it, "an opened file") }
        }
    }

    companion object { const val MAX_IMPORT_BYTES = 2 * 1024 * 1024 }
}

internal fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buf = ByteArray(64 * 1024)
    while (out.size() < limit) {
        val n = read(buf, 0, minOf(buf.size, limit - out.size()))
        if (n < 0) break
        out.write(buf, 0, n)
    }
    return out.toByteArray()
}

enum class Tab(val label: String, val icon: ImageVector, val description: String) {
    PROVE("Prove", Icons.Filled.PlayArrow, "Prove a payload"),
    ACTIVITY("Activity", Icons.Filled.List, "Job history"),
    SERVER("Server", Icons.Filled.Build, "Proof server status and setup"),
    SETTINGS("Settings", Icons.Filled.Settings, "Settings and about")
}

/** Window width class, from the Material 3 breakpoints (600 / 840 dp). */
enum class WidthClass { COMPACT, MEDIUM, EXPANDED }

fun widthClassOf(width: Dp): WidthClass = when {
    width < 600.dp -> WidthClass.COMPACT
    width < 840.dp -> WidthClass.MEDIUM
    else -> WidthClass.EXPANDED
}

val LocalWidthClass = staticCompositionLocalOf { WidthClass.COMPACT }
val LocalSnackbar = staticCompositionLocalOf<(String) -> Unit> { {} }

@Composable
fun ProverApp(vm: ProverViewModel, state: ProverUiState, initialTab: Tab = Tab.PROVE) {
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showSnack: (String) -> Unit = { msg -> scope.launch { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(msg) } }

    // Keep the screen on while proving, if the user wants it.
    val view = LocalView.current
    val keepOn = state.session.isRunning && state.settings.keepScreenOn
    DisposableEffect(keepOn) {
        view.keepScreenOn = keepOn
        onDispose { view.keepScreenOn = false }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val widthClass = widthClassOf(maxWidth)
        CompositionLocalProvider(LocalWidthClass provides widthClass, LocalSnackbar provides showSnack) {
            // Starfield pauses while a proof runs (and under reduced motion) — Bob P1-16.
            StarfieldBackground(animate = !state.session.isRunning)
            if (!state.settings.onboardingDone) {
                OnboardingScreen(state, vm)
                return@CompositionLocalProvider
            }
            val useRail = widthClass != WidthClass.COMPACT
            // Back from another tab returns to Prove before leaving the app (Bob P1-15).
            BackHandler(enabled = tab != Tab.PROVE) { tab = Tab.PROVE }
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(containerColor = MidnightPalette.Nebula.copy(alpha = 0.94f)) {
                            Tab.entries.forEach { t ->
                                NavigationBarItem(
                                    selected = tab == t,
                                    onClick = { tab = t },
                                    icon = { Icon(t.icon, contentDescription = t.description) },
                                    label = { Text(t.label, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center) },
                                    modifier = Modifier.testTag("tab-${t.name}"),
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MidnightPalette.ProofGlow,
                                        selectedTextColor = MidnightPalette.ProofGlow,
                                        indicatorColor = MidnightPalette.NebulaHigh,
                                        unselectedIconColor = MidnightPalette.Moonlight,
                                        unselectedTextColor = MidnightPalette.Moonlight
                                    )
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                Row(Modifier.fillMaxSize().padding(padding)) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = MidnightPalette.Nebula.copy(alpha = 0.9f),
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Tab.entries.forEach { t ->
                                NavigationRailItem(
                                    selected = tab == t,
                                    onClick = { tab = t },
                                    icon = { Icon(t.icon, contentDescription = t.description) },
                                    label = { Text(t.label, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center) },
                                    modifier = Modifier.testTag("tab-${t.name}"),
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = MidnightPalette.ProofGlow,
                                        selectedTextColor = MidnightPalette.ProofGlow,
                                        indicatorColor = MidnightPalette.NebulaHigh,
                                        unselectedIconColor = MidnightPalette.Moonlight,
                                        unselectedTextColor = MidnightPalette.Moonlight
                                    )
                                )
                            }
                        }
                    }
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        val reduce = LocalReduceMotion.current
                        AnimatedContent(
                            targetState = tab,
                            transitionSpec = {
                                if (reduce) fadeIn(androidx.compose.animation.core.snap()) togetherWith fadeOut(androidx.compose.animation.core.snap())
                                else fadeIn() togetherWith fadeOut()
                            },
                            label = "tab"
                        ) { t ->
                            when (t) {
                                Tab.PROVE -> ProveScreen(state, vm, onOpenServer = { tab = Tab.SERVER })
                                Tab.ACTIVITY -> ActivityScreen(state, vm)
                                Tab.SERVER -> ServerScreen(state, vm)
                                Tab.SETTINGS -> SettingsScreen(state, vm)
                            }
                        }
                    }
                }
            }
        }
    }
}
