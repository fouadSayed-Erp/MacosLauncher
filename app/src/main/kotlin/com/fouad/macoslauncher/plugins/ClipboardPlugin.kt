package com.maclauncher.plugins
 
import com.maclauncher.core.plugin.LauncherPlugin
import com.maclauncher.core.plugin.PluginResult
import javax.inject.Inject
 
class ClipboardPlugin class="text-[#D7BA7D]">@Inject constructor() : LauncherPlugin {
    override val id = class="text-[#CE9178]">"clipboard"
    override val name = class="text-[#CE9178]">"Clipboard History"
 
    override suspend fun query(input: String): List<PluginResult> {
        // Query clipboard history
        return emptyList()
    }
 
    override suspend fun onSelect(result: PluginResult) = true
}
 