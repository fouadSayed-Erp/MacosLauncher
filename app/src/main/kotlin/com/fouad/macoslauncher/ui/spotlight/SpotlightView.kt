package com.maclauncher.ui.spotlight
 
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
 
class="text-[#D7BA7D]">@Composable
fun SpotlightView(
    viewModel: SpotlightViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf(class="text-[#CE9178]">"") }
    val results by viewModel.results.collectAsState()
 
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = androidx.compose.ui.Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .padding(top = 120.dp)
                .width(640.dp)
                .background(Color(0xFF2C2C2E), RoundedCornerShape(12.dp))
        ) {
            TextField(
                value = query,
                onValueChange = { 
                    query = it
                    viewModel.search(it)
                },
                placeholder = { Text(class="text-[#CE9178]">"Spotlight Search") },
                modifier = Modifier.fillMaxWidth()
            )
            
            results.forEach { result ->
                ListItem(
                    headlineContent = { Text(result.title) },
                    supportingContent = { result.subtitle?.let { Text(it) } }
                )
            }
        }
    }
}
 
