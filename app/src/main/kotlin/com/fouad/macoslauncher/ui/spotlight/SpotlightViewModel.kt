package com.maclauncher.ui.spotlight
 
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maclauncher.core.plugin.PluginManager
import com.maclauncher.core.plugin.PluginResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
 
class="text-[#D7BA7D]">@HiltViewModel
class SpotlightViewModel class="text-[#D7BA7D]">@Inject constructor(
    private val pluginManager: PluginManager
) : ViewModel() {
 
    private val _results = MutableStateFlow<List<PluginResult>>(emptyList())
    val results = _results.asStateFlow()
 
    fun search(query: String) {
        viewModelScope.launch {
            _results.value = pluginManager.search(query)
        }
    }
}
 