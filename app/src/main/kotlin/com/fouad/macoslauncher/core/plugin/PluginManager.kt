package com.maclauncher.core.plugin
 
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton
 
class="text-[#D7BA7D]">@Singleton
class PluginManager class="text-[#D7BA7D]">@Inject constructor(
    private val plugins: Set<class="text-[#D7BA7D]">@JvmSuppressWildcards LauncherPlugin>
) {
    suspend fun search(query: String): List<PluginResult> = coroutineScope {
        if (query.isBlank()) returnclass="text-[#D7BA7D]">@coroutineScope emptyList()
        
        plugins
            .sortedByDescending { it.priority }
            .map { plugin ->
                async {
                    try {
                        plugin.query(query)
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }
            .awaitAll()
            .flatten()
            .sortedBy { it.title }
            .take(20)
    }
}
 
PluginManager.kt