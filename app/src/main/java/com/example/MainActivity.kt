package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.example.model.OfflineTestRepository
import com.example.model.TestHistoryRepository
import com.example.model.UnlockedTestsRepository
import com.example.service.AuthManager
import com.example.service.RewardedAdManager
import com.example.ui.components.AuthDialog
import com.example.ui.components.ExamMitraBottomNav
import com.example.ui.components.FirstLaunchOfflineDialog
import com.example.ui.components.rememberIsOnline
import com.example.ui.components.NavTab
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.AnalysisScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("EXTRA_OPEN_AI_STUDIO", false)) {
            com.example.service.AiBackgroundExecutionManager.openDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (intent?.getBooleanExtra("EXTRA_OPEN_AI_STUDIO", false) == true) {
            com.example.service.AiBackgroundExecutionManager.openDialog()
        }

        // Pre-create WebView/Chromium Code Cache directories & clean corrupt variation seeds
        try {
            val codeCacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache")
            java.io.File(codeCacheDir, "js").mkdirs()
            java.io.File(codeCacheDir, "wasm").mkdirs()

            // Clean any 0-byte or invalid variations seed cache files that trigger Chromium loader warnings
            val webViewDir = java.io.File(applicationContext.dataDir, "app_webview")
            if (webViewDir.exists()) {
                val seedFiles = webViewDir.listFiles { file -> file.name.contains("variations_seed") || file.name.contains("variations_stamp") }
                seedFiles?.forEach { seedFile ->
                    if (seedFile.length() == 0L) {
                        seedFile.delete()
                    }
                }
            }
        } catch (_: Exception) {}

        // 1. Critical lightweight initializations for immediate UI render
        com.example.ui.theme.ThemeManager.init(applicationContext)
        AuthManager.init(applicationContext)
        UnlockedTestsRepository.init(applicationContext)
        com.example.service.AiBackgroundExecutionManager.init(applicationContext)

        // 2. Offload heavy disk I/O, cache building and network services to background thread
        // to achieve instantaneous zero-lag app startup
        lifecycleScope.launch(Dispatchers.IO) {
            com.example.service.security.AppSecurityManager.init(applicationContext)
            OfflineTestRepository.init(applicationContext)
            TestHistoryRepository.init(applicationContext)
            com.example.model.WeakAreaAnalyticsRepository.init(applicationContext)

            com.example.model.DynamicTestRepository.init(applicationContext)
            com.example.model.CustomChapterRepository.init(applicationContext)
            com.example.model.RealtimeExamStatsManager.init(applicationContext)
            com.example.service.OnlineTestSyncManager.init(applicationContext)
            com.example.model.ExamNotificationRepository.init(applicationContext)

            // Initialize Coil image caching pipeline asynchronously
            val imageLoader = coil.ImageLoader.Builder(applicationContext)
                .memoryCache {
                    coil.memory.MemoryCache.Builder(applicationContext)
                        .maxSizePercent(0.25)
                        .build()
                }
                .diskCache {
                    coil.disk.DiskCache.Builder()
                        .directory(applicationContext.cacheDir.resolve("image_cache"))
                        .maxSizeBytes(64L * 1024 * 1024)
                        .build()
                }
                .crossfade(true)
                .respectCacheHeaders(false)
                .build()
            coil.Coil.setImageLoader(imageLoader)
        }

        setContent {
            MyApplicationTheme {
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val currentUser = AuthManager.currentUser

                    if (currentUser == null) {
                        // 🌟 New User: Show Google One-Tap Login FIRST before 100% loading screen
                        AuthDialog(
                            initialTabIndex = 0, // 0 = Log In, 1 = Sign Up
                            canDismiss = false,
                            onDismiss = { /* Non-dismissable */ },
                            onSuccess = { /* Automatically re-composes when currentUser is updated */ }
                        )
                    } else {
                        // Returning User or Logged In User: Show 100% Startup Loading Screen -> ExamMitraApp
                        var isAppReady by remember { mutableStateOf(false) }

                        AnimatedContent(
                            targetState = isAppReady,
                            transitionSpec = {
                                fadeIn(androidx.compose.animation.core.tween(400)) togetherWith
                                    fadeOut(androidx.compose.animation.core.tween(300))
                            },
                            label = "startup_transition"
                        ) { ready ->
                            if (!ready) {
                                com.example.ui.screens.AppStartupLoadingScreen(
                                    onLoadingComplete = { isAppReady = true }
                                )
                            } else {
                                ExamMitraApp()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExamMitraApp() {
    val context = LocalContext.current
    val currentUser = AuthManager.currentUser
    var currentTab by remember { mutableStateOf(NavTab.HOME) }

    val isAiDialogOpen by com.example.service.AiBackgroundExecutionManager.isDialogOpen.collectAsState()
    val draftSession by com.example.service.AiBackgroundExecutionManager.draftSession.collectAsState()
    val bgTaskState by com.example.service.AiBackgroundExecutionManager.taskState.collectAsState()

    val shouldShowGlobalAiDialog = isAiDialogOpen || bgTaskState.isRunning || (bgTaskState.isSuccess && bgTaskState.parsedQuestions.isNotEmpty())

    if (shouldShowGlobalAiDialog) {
        com.example.ui.screens.AdminAiTestCreatorDialog(
            onDismiss = {
                com.example.service.AiBackgroundExecutionManager.closeDialog()
                com.example.service.AiBackgroundExecutionManager.clearState()
            },
            onLaunchTest = { testPaper ->
                com.example.service.AiBackgroundExecutionManager.closeDialog()
                com.example.service.AiBackgroundExecutionManager.clearState()
            },
            initialMode = draftSession.testMode,
            initialSubExamId = draftSession.subExamId,
            initialSubjectKey = draftSession.subjectKey,
            initialChapterTitle = draftSession.chapterTitle
        )
    }

    // First launch offline check: If user opens the app while offline, proactively prompt to turn on internet
    val isOnline = rememberIsOnline()
    var showOfflineDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!OfflineTestRepository.isNetworkAvailable(context)) {
            showOfflineDialog = true
        }
    }

    FirstLaunchOfflineDialog(
        isOpen = showOfflineDialog,
        isOnline = isOnline,
        onDismiss = { showOfflineDialog = false }
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            ExamMitraBottomNav(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tab_navigation",
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) { targetTab ->
            when (targetTab) {
                NavTab.HOME -> HomeScreen(
                    onNavigateToPractice = { currentTab = NavTab.ANALYSIS },
                    onNavigateToNotifications = { currentTab = NavTab.NOTIFICATIONS }
                )
                NavTab.ANALYSIS -> AnalysisScreen(
                    onNavigateToHome = { currentTab = NavTab.HOME }
                )
                NavTab.NOTIFICATIONS -> NotificationsScreen()
                NavTab.PROFILE -> ProfileScreen()
            }
        }
    }
}

