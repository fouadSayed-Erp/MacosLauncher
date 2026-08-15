package com.maclauncher.core.plugin
 
interface LauncherPlugin {
    val id: String
    val name: String
    val icon: String
    val priority: Int get() = 0
    
    suspend fun query(input: String): List<PluginResult>
    suspend fun onSelect(result: PluginResult): Boolean
}
 
data class PluginResult(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val icon: String? = null,
    val action: () -> Unit
)
 