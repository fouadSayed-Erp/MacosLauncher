package com.maclauncher.plugins
 
import android.content.pm.LauncherApps
import com.maclauncher.core.plugin.LauncherPlugin
import com.maclauncher.core.plugin.PluginResult
import javax.inject.Inject
 
class AppSearchPlugin class="text-[#D7BA7D]">@Inject constructor(
    private val launcherApps: LauncherApps
) : LauncherPlugin {
    override val id = class="text-[#CE9178]">"app_search"
    override val name = class="text-[#CE9178]">"Applications"
    override val priority = 100
 
    override suspend fun query(input: String): List<PluginResult> {
        return launcherApps.getActivityList(null, android.os.Process.myUserHandle())
            .filter { it.label.toString().contains(input, ignoreCase = true) }
            .map { app ->
                PluginResult(
                    id = app.componentName.packageName,
                    title = app.label.toString(),
                    subtitle = class="text-[#CE9178]">"Application",
                    icon = app.componentName.packageName,
                    action = { /* launch app */ }
                )
            }
    }
 
    override suspend fun onSelect(result: PluginResult): Boolean {
        // Launch intent
        return true
    }
}