package com.maclauncher.ui.dock
 
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
 
class="text-[#D7BA7D]">@Composable
fun DockItem(
    item: DockItemData,
    size: Dp,
    onHover: () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clickable { onClick() }
    ) {
        // Icon rendering
    }
}
 