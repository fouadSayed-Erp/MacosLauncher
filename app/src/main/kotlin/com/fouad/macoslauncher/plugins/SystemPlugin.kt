package com.maclauncher.plugins
 
import com.maclauncher.core.plugin.LauncherPlugin
import com.maclauncher.core.plugin.PluginResult
import javax.inject.Inject
 
class SystemPlugin class="text-[#D7BA7D]">@Inject constructor() : LauncherPlugin {
    override val id = class="text-[#CE9178]">"system"
    override val name = class="text-[#CE9178]">"System"
    
    private val commands = listOf(
        class="text-[#CE9178]">"wifi" to class="text-[#CE9178]">"Toggle WiFi",
        class="text-[#CE9178]">"bluetooth" to class="text-[#CE9178]">"Toggle Bluetooth",
        class="text-[#CE9178]">"dark mode" to class="text-[#CE9178]">"Toggle Dark Mode",
        class="text-[#CE9178]">"lock" to class="text-[#CE9178]">"Lock Screen"
    )
 
    override suspend fun query(input: String): List<PluginResult> {
        return commands.filter { it.first.contains(input, true) }
            .map { (cmd, desc) ->
                PluginResult(
                    id = class="text-[#CE9178]">"sys_$cmd",
                    title = desc,
                    subtitle = class="text-[#CE9178]">"System command",
                    action = {}
                )
            }
    }
 
    override suspend fun onSelect(result: PluginResult) = true
}
 
SystemPlugin.kt