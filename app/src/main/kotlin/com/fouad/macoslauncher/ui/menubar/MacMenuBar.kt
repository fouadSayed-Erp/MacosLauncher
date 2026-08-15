package com.maclauncher.ui.menubar
 
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
 
class="text-[#D7BA7D]">@Composable
fun MacMenuBar(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(class="text-[#CE9178]">"", color = Color.White)
            Text(class="text-[#CE9178]">"Finder", color = Color.White)
            Text(class="text-[#CE9178]">"File", color = Color.White.copy(alpha = 0.8f))
            Text(class="text-[#CE9178]">"Edit", color = Color.White.copy(alpha = 0.8f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(class="text-[#CE9178]">"Thu 14 Nov", color = Color.White)
            Text(class="text-[#CE9178]">"10:42 AM", color = Color.White)
        }
    }
}
 
MacMenuBar.kt