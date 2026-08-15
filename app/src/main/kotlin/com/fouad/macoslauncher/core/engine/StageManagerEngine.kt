package com.maclauncher.core.engine
 
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
 
/**
 * StageManagerEngine - Handles window grouping logic
 * Similar to macOS Ventura Stage Manager
 * Groups windows by app, keeps recent apps on left
 */
class="text-[#D7BA7D]">@Singleton
class StageManagerEngine class="text-[#D7BA7D]">@Inject constructor() {
 
    data class WindowGroup(
        val id: String,
        val appPackage: String,
        val appName: String,
        val windows: List<MacWindow>,
        val lastActiveTimestamp: Long,
        val thumbnail: Thumbnail? = null
    )
 
    data class StageState(
        val isEnabled: Boolean = true,
        val groups: List<WindowGroup> = emptyList(),
        val activeGroupId: String? = null,
        val maxVisibleGroups: Int = 6
    )
 
    private val _state = MutableStateFlow(StageState())
    val state: StateFlow<StageState> = _state.asStateFlow()
 
    /**
     * Group windows by application
     * Most recent app is centered, others stacked on left
     */
    fun groupWindows(windows: List<MacWindow>) {
        val grouped = windows
            .groupBy { it.packageName }
            .map { (pkg, wins) ->
                WindowGroup(
                    id = pkg,
                    appPackage = pkg,
                    appName = resolveAppName(pkg),
                    windows = wins.sortedByDescending { it.lastFocused },
                    lastActiveTimestamp = wins.maxOf { it.lastFocused }
                )
            }
            .sortedByDescending { it.lastActiveTimestamp }
 
        _state.value = _state.value.copy(
            groups = grouped.take(_state.value.maxVisibleGroups),
            activeGroupId = grouped.firstOrNull()?.id
        )
    }
 
    fun activateGroup(groupId: String) {
        // Bring all windows of group to front
        val group = _state.value.groups.find { it.id == groupId } ?: return
        _state.value = _state.value.copy(activeGroupId = groupId)
        bringToFront(group.windows)
    }
 
    fun toggleStageManager() {
        _state.value = _state.value.copy(
            isEnabled = !_state.value.isEnabled
        )
    }
 
    fun addWindow(window: MacWindow) {
        val current = _state.value.groups.toMutableList()
        val existingGroup = current.find { it.appPackage == window.packageName }
        
        if (existingGroup != null) {
            val updated = existingGroup.copy(
                windows = (existingGroup.windows + window).distinctBy { it.id },
                lastActiveTimestamp = System.currentTimeMillis()
            )
            current[current.indexOf(existingGroup)] = updated
        } else {
            current.add(0, WindowGroup(
                id = window.packageName,
                appPackage = window.packageName,
                appName = window.appName,
                windows = listOf(window),
                lastActiveTimestamp = System.currentTimeMillis()
            ))
        }
        
        _state.value = _state.value.copy(groups = current)
    }
 
    private fun resolveAppName(packageName: String): String {
        // Resolve via PackageManager in real impl
        return packageName.substringAfterLast(class="text-[#CE9178]">".").capitalize()
    }
 
    private fun bringToFront(windows: List<MacWindow>) {
        // WindowManager integration
    }
}
 
data class MacWindow(
    val id: String,
    val packageName: String,
    val appName: String,
    val lastFocused: Long
)
 
data class Thumbnail(val bitmap: Any? = null)
 
StageMana