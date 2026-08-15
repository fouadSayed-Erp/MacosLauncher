package com.maclauncher.ui.stagemanager
 
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maclauncher.core.engine.StageManagerEngine
 
class="text-[#D7BA7D]">@Composable
fun StageManagerView(
    groups: List<StageManagerEngine.WindowGroup>,
    activeGroupId: String?,
    onGroupSelected: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(groups) { group ->
            StageGroupCard(
                group = group,
                isActive = group.id == activeGroupId,
                onClick = { onGroupSelected(group.id) }
            )
        }
    }
}
 
class="text-[#D7BA7D]">@Composable
fun StageGroupCard(
    group: StageManagerEngine.WindowGroup,
    isActive: Boolean,
    onClick: () -> Unit
) {
    // Card with thumbnail stack
}
 
StageManagerView.kt