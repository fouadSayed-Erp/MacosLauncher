
package com.maclauncher
 
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maclauncher.core.engine.MissionControlEngine
import com.maclauncher.core.engine.StageManagerEngine
import com.maclauncher.ui.dock.MacDock
import com.maclauncher.ui.menubar.MacMenuBar
import com.maclauncher.ui.missioncontrol.MissionControlView
import com.maclauncher.ui.stagemanager.StageManagerView
import com.maclauncher.ui.spotlight.SpotlightView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
 
/**
 * MainActivity - Integrated entry point for macOS Launcher v3
 * Combines Stage Manager + Mission Control + Spotlight
 */
class="text-[#D7BA7D]">@AndroidEntryPoint
class MainActivity : ComponentActivity() {
 
    class="text-[#D7BA7D]">@Inject lateinit var stageManager: StageManagerEngine
    class="text-[#D7BA7D]">@Inject lateinit var missionControl: MissionControlEngine
 
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge with macOS style blur
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        
        setContent {
            MacLauncherTheme {
                var isSpotlightVisible by remember { mutableStateOf(false) }
                var isMissionControlVisible by remember { mutableStateOf(false) }
                
                val stageState by stageManager.state.collectAsState()
                val spacesState by missionControl.spaces.collectAsState()
 
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E1E1E))
                ) {
                    // Wallpaper layer
                    MacWallpaper()
 
                    // Stage Manager - left side grouping
                    AnimatedVisibility(
                        visible = stageState.isEnabled,
                        enter = slideInHorizontally { -it } + fadeIn(),
                        exit = slideOutHorizontally { -it } + fadeOut()
                    ) {
                        StageManagerView(
                            groups = stageState.groups,
                            activeGroupId = stageState.activeGroupId,
                            onGroupSelected = { stageManager.activateGroup(it) }
                        )
                    }
 
                    // Main workspace with spaces
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Current space windows
                        spacesState.currentSpace.windows.forEach { window ->
                            MacWindow(
                                window = window,
                                isFocused = window.id == spacesState.focusedWindowId
                            )
                        }
                    }
 
                    // Mission Control overlay
                    if (isMissionControlVisible) {
                        MissionControlView(
                            spaces = spacesState.allSpaces,
                            onSpaceSelected = { missionControl.switchToSpace(it) },
                            onDismiss = { isMissionControlVisible = false }
                        )
                    }
 
                    // Dock at bottom - macOS style magnification
                    MacDock(
                        modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
                        onSpotlightTrigger = { isSpotlightVisible = true },
                        onMissionControlTrigger = { isMissionControlVisible = true }
                    )
 
                    // Menu bar at top
                    MacMenuBar(
                        modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter)
                    )
 
                    // Spotlight - cmd+space
                    if (isSpotlightVisible) {
                        SpotlightView(
                            onDismiss = { isSpotlightVisible = false }
                        )
                    }
                }
            }
        }
    }
}
 