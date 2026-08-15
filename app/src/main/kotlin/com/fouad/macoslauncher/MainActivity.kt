package com.fouad.macoslauncher

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

data class AppInfo(val label: String, val packageName: String, val icon: Drawable)
fun getInstalledApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
    return pm.queryIntentActivities(intent, 0).map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.loadIcon(pm)) }.sortedBy { it.label.lowercase() }
}
fun launchApp(context: Context, app: AppInfo) {
    try { context.startActivity(context.packageManager.getLaunchIntentForPackage(app.packageName)) } catch(e:Exception){}
}
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { MacOSScreen() } }
}
@Composable
fun MacOSScreen() {
    val context = LocalContext.current
    var allApps by remember { mutableStateOf(listOf<AppInfo>()) }
    var showLaunchpad by remember { mutableStateOf(false) }
    var showSpotlight by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { allApps = getInstalledApps(context) }
    val filtered = remember(search, allApps) { if(search.isBlank()) allApps else allApps.filter{ it.label.contains(search,true) } }
    val dockApps = remember(allApps){ allApps.take(7) }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0A0E27), Color(0xFF1A2A6A), Color(0xFF3A5AC0)))))
        Column(Modifier.fillMaxSize()) {
            var time by remember { mutableStateOf("") }
            LaunchedEffect(Unit){ while(true){ time = SimpleDateFormat("EEE h:mm a", Locale.ENGLISH).format(Date()); delay(1000) } }
            Row(Modifier.fillMaxWidth().height(34.dp).background(Color.Black.copy(alpha=0.35f)).padding(horizontal=14.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.SpaceBetween){
                Text("Finder", color=Color.White, fontSize=14.sp, fontWeight=FontWeight.Bold)
                Row(verticalAlignment=Alignment.CenterVertically){ Icon(Icons.Filled.Search, null, tint=Color.White, modifier=Modifier.size(18.dp).clickable{ showSpotlight=true }); Spacer(Modifier.width(12.dp)); Text(time, color=Color.White, fontSize=12.sp) }
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.Center){
                Row(Modifier.clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha=0.28f)).border(1.dp, Color.White.copy(alpha=0.35f), RoundedCornerShape(22.dp)).padding(10.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Box(Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)).background(Color.White).clickable{ showLaunchpad=true }, contentAlignment=Alignment.Center){ Icon(Icons.Filled.Apps, null, tint=Color(0xFF1A2A6A), modifier=Modifier.size(30.dp)) }
                    Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(alpha=0.4f)))
                    dockApps.forEach{ app -> AppIcon(app.icon, Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)).clickable{ launchApp(context, app) }) }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        if(showSpotlight){
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.45f)).clickable{ showSpotlight=false }, contentAlignment=Alignment.TopCenter){
                Card(Modifier.padding(top=80.dp).fillMaxWidth(0.92f), shape=RoundedCornerShape(18.dp), colors=CardDefaults.cardColors(containerColor=Color.White.copy(alpha=0.96f))){
                    Column(Modifier.padding(16.dp)){
                        OutlinedTextField(value=search, onValueChange={search=it}, placeholder={Text("Spotlight Search")}, leadingIcon={Icon(Icons.Filled.Search,null)}, modifier=Modifier.fillMaxWidth(), singleLine=true)
                        Spacer(Modifier.height(10.dp))
                        LazyVerticalGrid(columns=GridCells.Fixed(4), modifier=Modifier.heightIn(max=380.dp)){ items(filtered.take(24)){ app -> Column(Modifier.padding(8.dp).clickable{ launchApp(context,app); showSpotlight=false }, horizontalAlignment=Alignment.CenterHorizontally){ AppIcon(app.icon, Modifier.size(48.dp)); Text(app.label, fontSize=10.sp, maxLines=1, textAlign=TextAlign.Center, modifier=Modifier.padding(top=4.dp)) } } }
                    }
                }
            }
        }
        if(showLaunchpad){
            Box(Modifier.fillMaxSize().background(Color(0xFF0B102A).copy(alpha=0.96f))){
                Column(Modifier.fillMaxSize().padding(14.dp)){
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically){
                        Text("Launchpad", color=Color.White, fontSize=22.sp, fontWeight=FontWeight.Bold)
                        Text("X", color=Color.White, fontSize=20.sp, modifier=Modifier.clip(RoundedCornerShape(8.dp)).clickable{ showLaunchpad=false }.padding(12.dp))
                    }
                    OutlinedTextField(value=search, onValueChange={search=it}, placeholder={Text("Search", color=Color.White.copy(alpha=0.5f))}, leadingIcon={Icon(Icons.Filled.Search,null,tint=Color.White)}, modifier=Modifier.fillMaxWidth().padding(vertical=12.dp), colors=OutlinedTextFieldDefaults.colors(focusedTextColor=Color.White, unfocusedTextColor=Color.White, focusedBorderColor=Color.White.copy(alpha=0.5f), unfocusedBorderColor=Color.White.copy(alpha=0.2f)))
                    LazyVerticalGrid(columns=GridCells.Fixed(4), modifier=Modifier.fillMaxSize()){ items(filtered){ app -> Column(Modifier.padding(12.dp).clickable{ launchApp(context,app); showLaunchpad=false }, horizontalAlignment=Alignment.CenterHorizontally){ AppIcon(app.icon, Modifier.size(62.dp)); Text(app.label, color=Color.White, fontSize=11.sp, maxLines=2, textAlign=TextAlign.Center, modifier=Modifier.padding(top=6.dp)) } } }
                }
            }
        }
    }
}
@Composable
fun AppIcon(icon: Drawable, modifier: Modifier = Modifier){
    val bmp = remember(icon){ try{ icon.toBitmap(128,128).asImageBitmap() }catch(e:Exception){ null } }
    if(bmp!=null){ Image(bitmap=bmp, contentDescription=null, modifier=modifier) } else { Box(modifier=modifier.background(Color.Gray)) }
}
