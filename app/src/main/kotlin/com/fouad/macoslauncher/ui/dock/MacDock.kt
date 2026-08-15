package com.maclauncher.ui.dock
 
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
 
class="text-[#D7BA7D]">@Composable
fun MacDock(
    modifier: Modifier = Modifier,
    onSpotlightTrigger: () -> Unit,
    onMissionControlTrigger: () -> Unit
) {
    var hoveredIndex by remember { mutableStateOf<Int?>(null) }
 
    Box(
        modifier = modifier
            .padding(bottom = 12.dp)
            .background(
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp)
            )
            .blur(20.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DockItems.forEachIndexed { index, item ->
                val isHovered = hoveredIndex == index
                val size by animateDpAsState(
                    targetValue = if (isHovered) 64.dp else 48.dp,
                    label = class="text-[#CE9178]">"dock_magnify"
                )
                
                DockItem(
                    item = item,
                    size = size,
                    onHover = { hoveredIndex = index },
                    onClick = {
                        when (item.id) {
                            class="text-[#CE9178]">"spotlight" -> onSpotlightTrigger()
                            class="text-[#CE9178]">"mission" -> onMissionControlTrigger()
                            else -> {}
                        }
                    }
                )
            }
        }
    }
}
 
val DockItems = listOf(
    DockItemData(class="text-[#CE9178]">"finder", class="text-[#CE9178]">"Finder"),
    DockItemData(class="text-[#CE9178]">"launchpad", class="text-[#CE9178]">"Launchpad"),
    DockItemData(class="text-[#CE9178]">"spotlight", class="text-[#CE9178]">"Spotlight"),
    DockItemData(class="text-[#CE9178]">"mission", class="text-[#CE9178]">"Mission Control"),
    DockItemData(class="text-[#CE9178]">"settings", class="text-[#CE9178]">"Settings")
)
 
data class DockItemData(val id: String, val name: String)
 
MacDock.kt