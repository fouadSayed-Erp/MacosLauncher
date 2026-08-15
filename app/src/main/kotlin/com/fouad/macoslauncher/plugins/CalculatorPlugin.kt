package com.maclauncher.plugins
 
import com.maclauncher.core.plugin.LauncherPlugin
import com.maclauncher.core.plugin.PluginResult
import javax.inject.Inject
import javax.script.ScriptEngineManager
 
class CalculatorPlugin class="text-[#D7BA7D]">@Inject constructor() : LauncherPlugin {
    override val id = class="text-[#CE9178]">"calculator"
    override val name = class="text-[#CE9178]">"Calculator"
    
    override suspend fun query(input: String): List<PluginResult> {
        if (!input.matches(Regex(class="text-[#CE9178]">"[0-9+\\-*/(). ]+"))) return emptyList()
        
        return try {
            val engine = ScriptEngineManager().getEngineByName(class="text-[#CE9178]">"rhino")
            val result = engine.eval(input)
            listOf(
                PluginResult(
                    id = class="text-[#CE9178]">"calc_$input",
                    title = class="text-[#CE9178]">"= $result",
                    subtitle = class="text-[#CE9178]">"Copy result",
                    action = {}
                )
            )
        } catch (e: Exception) {
            emptyList()
        }
    }
 
    override suspend fun onSelect(result: PluginResult) = true
}
 
