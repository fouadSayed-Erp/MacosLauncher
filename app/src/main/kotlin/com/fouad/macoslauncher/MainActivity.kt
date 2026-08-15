package com.fouad.macoslauncher

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object IconCache {
    private val cache = ConcurrentHashMap<String, androidx.compose.ui.graphics.ImageBitmap>()
    fun get(key: String) = cache[key]
    fun put(key: String, bmp: androidx.compose.ui.graphics.ImageBitmap) { if(cache.size>150) cache.clear(); cache[key]=bmp }
}

data class AppInfo(val label: String, val packageName: String, val icon: Drawable, val category: String = "Other")

fun getCategory(label: String, pkg: String): String {
    val l = label.lowercase()
    return when {
        l.contains("facebook") || l.contains("whatsapp") || l.contains("telegram") || l.contains("instagram") -> "Social"
        l.contains("game") -> "Games"
        l.contains("camera") || l.contains("gallery") || l.contains("photo") -> "Media"
        l.contains("chrome") || l.contains("browser") || l.contains("youtube") -> "Internet"
        l.contains("settings") || l.contains("file") -> "Tools"
        else -> "Other"
    }
}

suspend fun loadAppsOptimized(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
    pm.queryIntentActivities(intent, 0).map {
        AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.loadIcon(pm), getCategory(it.loadLabel(pm).toString(), it.activityInfo.packageName))
    }.sortedBy { it.label.lowercase() }
}

fun launchApp(context: Context, app: AppInfo) { try { context.startActivity(context.packageManager.getLaunchIntentForPackage(app.packageName)?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch(e:Exception){} }

val wallpapers = listOf(
    Brush.verticalGradient(listOf(Color(0xFF0A0E27), Color(0xFF1A2A6A), Color(0xFF3A5AC0))),
    Brush.verticalGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))),
    Brush.verticalGradient(listOf(Color(0xFF2D3436), Color(0xFF636E72), Color(0xFFB2BEC3))),
    Brush.verticalGradient(listOf(Color(0xFF6C5CE7), Color(0xFFA29BFE), Color(0xFF81ECEC))),
    Brush.linearGradient(listOf(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E)))
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { MacOSProV8() } }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MacOSProV8() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var allApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showLaunchpad by remember { mutableStateOf(false) }
    var showSpotlight by remember { mutableStateOf(false) }
    var showControlCenter by remember { mutableStateOf(false) }
    var showNotificationCenter by remember { mutableStateOf(false) }
    var showWallpaperPicker by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var search by remember { mutableStateOf("") }
    var wallpaperIndex by remember { mutableStateOf(0) }
    var launchpadFolder by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        allApps = loadAppsOptimized(context)
        isLoading = false
    }

    val filtered by remember { derivedStateOf { if(search.isBlank()) allApps else allApps.filter{ it.label.contains(search, true) } } }
    val grouped by remember { derivedStateOf { filtered.groupBy{ it.category } } }
    val dockApps = remember(allApps) { allApps.take(7) }
    val folders = remember(allApps) { allApps.groupBy{ it.category }.filter{ it.value.size>=2 } }

    Box(Modifier.fillMaxSize().background(wallpapers[wallpaperIndex])
        .pointerInput(Unit){ detectTapGestures(onLongPress={ haptic.performHapticFeedback(HapticFeedbackType.LongPress); showWallpaperPicker=true }) }
        .pointerInput(Unit){ detectDragGestures(onDragEnd={}, onDrag={ _, dragAmount -> if(dragAmount.y < -30) showLaunchpad=true }) }
    ) {
        Column(Modifier.fillMaxSize()) {
            TopBarOptimized(onSpotlight={ showSpotlight=true }, onControl={ showControlCenter=true }, onNotif={ showNotificationCenter=true })
            if(!showLaunchpad) {
                LazyVerticalGrid(columns=GridCells.Fixed(2), modifier=Modifier.fillMaxWidth().weight(1f).padding(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp), horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    item { ClockWidget() }
                    item { BatteryWidget() }
                    item { SearchWidget(onClick={ showSpotlight=true }) }
                    item { folders.entries.firstOrNull()?.let { (cat, apps) -> FolderWidget(cat, apps.take(4), onClick={ launchpadFolder=cat; showLaunchpad=true }) } ?: StorageWidget() }
                }
            } else { Spacer(Modifier.weight(1f)) }
            OptimizedDock(dockApps=dockApps, onLaunchpad={ showLaunchpad=true }, onAppClick={ launchApp(context, it) }, onAppLong={ selectedApp=it; haptic.performHapticFeedback(HapticFeedbackType.LongPress) })
            Spacer(Modifier.height(10.dp))
        }

        if(isLoading) { Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.5f)), contentAlignment=Alignment.Center){ CircularProgressIndicator(color=Color.White) } }
        if(showSpotlight) SpotlightPro(search=search, onSearch={ search=it }, apps=filtered, onClose={ showSpotlight=false; search="" }, onLaunch={ launchApp(context, it); showSpotlight=false })
        if(showLaunchpad) LaunchpadPro(apps=filtered, grouped=grouped, folders=folders, folderFilter=launchpadFolder, search=search, onSearch={ search=it }, onClose={ showLaunchpad=false; launchpadFolder=null; search="" }, onAppClick={ launchApp(context, it); showLaunchpad=false }, onAppLong={ selectedApp=it })
        if(showControlCenter) ControlCenterPro(onClose={ showControlCenter=false }, onWallpaper={ showWallpaperPicker=true; showControlCenter=false })
        if(showNotificationCenter) NotificationCenter(onClose={ showNotificationCenter=false })
        if(showWallpaperPicker) WallpaperPicker(current=wallpaperIndex, wallpapers=wallpapers, onSelect={ wallpaperIndex=it; showWallpaperPicker=false }, onClose={ showWallpaperPicker=false })
        selectedApp?.let { app ->
            AppMenuPro(app=app, onDismiss={ selectedApp=null }, onOpen={ launchApp(context, app); selectedApp=null },
                onInfo={ try{ context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}")).apply{ addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) }catch(e:Exception){}; selectedApp=null },
                onUninstall={ try{ context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}")).apply{ addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) }catch(e:Exception){}; selectedApp=null }
            )
        }
    }
}

@Composable
fun TopBarOptimized(onSpotlight:()->Unit, onControl:()->Unit, onNotif:()->Unit){
    var time by remember { mutableStateOf("") }
    LaunchedEffect(Unit){ while(true){ time = SimpleDateFormat("EEE h:mm a", Locale.ENGLISH).format(Date()); delay(1000) } }
    Row(Modifier.fillMaxWidth().height(38.dp).background(Color.Black.copy(alpha=0.4f)).padding(horizontal=14.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.SpaceBetween){
        Row(horizontalArrangement=Arrangement.spacedBy(14.dp), verticalAlignment=Alignment.CenterVertically){
            Text("Finder", color=Color.White, fontWeight=FontWeight.Bold, fontSize=14.sp)
            Text("File Edit View Go", color=Color.White.copy(alpha=0.6f), fontSize=11.sp)
        }
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp), verticalAlignment=Alignment.CenterVertically){
            Icon(Icons.Filled.Search, null, tint=Color.White, modifier=Modifier.size(18.dp).clip(CircleShape).clickable{ onSpotlight() })
            Icon(Icons.Filled.Notifications, null, tint=Color.White, modifier=Modifier.size(18.dp).clickable{ onNotif() })
            Text(time, color=Color.White, fontSize=12.sp, modifier=Modifier.clip(RoundedCornerShape(6.dp)).clickable{ onControl() }.padding(horizontal=6.dp, vertical=2.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OptimizedDock(dockApps: List<AppInfo>, onLaunchpad:()->Unit, onAppClick:(AppInfo)->Unit, onAppLong:(AppInfo)->Unit){
    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.Center){
        Row(Modifier.clip(RoundedCornerShape(26.dp)).background(Color.White.copy(alpha=0.20f)).border(1.dp, Color.White.copy(alpha=0.3f), RoundedCornerShape(26.dp)).padding(horizontal=12.dp, vertical=10.dp), verticalAlignment=Alignment.Bottom, horizontalArrangement=Arrangement.spacedBy(8.dp)){
            DockIconStatic(Icons.Filled.Apps, Color(0xFF1A2A6A), onClick=onLaunchpad)
            Box(Modifier.width(1.dp).height(30.dp).background(Color.White.copy(alpha=0.3f)).align(Alignment.CenterVertically))
            dockApps.forEach { app -> DockIconApp(app=app, onClick={ onAppClick(app) }, onLong={ onAppLong(app) }) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DockIconStatic(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, onClick:()->Unit){
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if(pressed) 1.4f else 1f, spring(dampingRatio=0.4f, stiffness=500f))
    Box(Modifier.size(54.dp).scale(scale).clip(RoundedCornerShape(14.dp)).background(Color.White).combinedClickable(interactionSource=interaction, indication=null, onClick=onClick)){ Icon(icon, null, tint=tint, modifier=Modifier.size(28.dp).align(Alignment.Center)) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DockIconApp(app: AppInfo, onClick:()->Unit, onLong:()->Unit){
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if(pressed) 1.5f else 1f, spring(dampingRatio=0.35f, stiffness=600f))
    Box(Modifier.size(54.dp).scale(scale).clip(RoundedCornerShape(14.dp)).combinedClickable(interactionSource=interaction, indication=null, onClick=onClick, onLongClick=onLong)){ FastAppIcon(app=app, modifier=Modifier.fillMaxSize(), size=56) }
}

@Composable
fun FastAppIcon(app: AppInfo, modifier: Modifier, size: Int = 64){
    var bitmap by remember(app.packageName) { mutableStateOf(IconCache.get(app.packageName)) }
    LaunchedEffect(app.packageName){
        if(bitmap==null){
            withContext(Dispatchers.Default){
                try{
                    val bmp = app.icon.toBitmap(size, size).asImageBitmap()
                    IconCache.put(app.packageName, bmp)
                    bitmap = bmp
                }catch(e:Exception){}
            }
        }
    }
    if(bitmap!=null) Image(bitmap=bitmap!!, contentDescription=null, modifier=modifier) else Box(modifier=modifier.background(Color.White.copy(alpha=0.3f), RoundedCornerShape(12.dp)))
}

@Composable
fun ClockWidget(){ var time by remember { mutableStateOf("") }; var date by remember { mutableStateOf("") }; LaunchedEffect(Unit){ while(true){ time=SimpleDateFormat("h:mm", Locale.ENGLISH).format(Date()); date=SimpleDateFormat("EEEE, MMM d", Locale.ENGLISH).format(Date()); delay(1000) } }; Card(Modifier.fillMaxWidth().height(110.dp), shape=RoundedCornerShape(20.dp), colors=CardDefaults.cardColors(containerColor=Color.White.copy(alpha=0.85f))){ Column(Modifier.padding(16.dp)){ Text(time, fontSize=28.sp, fontWeight=FontWeight.Bold, color=Color.Black); Text(date, fontSize=12.sp, color=Color.Gray) } } }
@Composable
fun BatteryWidget(){ val context=LocalContext.current; var level by remember { mutableStateOf(0) }; LaunchedEffect(Unit){ while(true){ val bm=context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager; level=bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY); delay(60000) } }; Card(Modifier.fillMaxWidth().height(110.dp), shape=RoundedCornerShape(20.dp), colors=CardDefaults.cardColors(containerColor=Color(0xFF0A84FF).copy(alpha=0.9f))){ Column(Modifier.padding(16.dp)){ Icon(Icons.Filled.BatteryFull, null, tint=Color.White); Spacer(Modifier.height(8.dp)); Text("$level%", fontSize=24.sp, fontWeight=FontWeight.Bold, color=Color.White); Text("Battery", fontSize=12.sp, color=Color.White.copy(alpha=0.8f)) } } }
@Composable
fun SearchWidget(onClick:()->Unit){ Card(Modifier.fillMaxWidth().height(60.dp).clickable{ onClick() }, shape=RoundedCornerShape(16.dp), colors=CardDefaults.cardColors(containerColor=Color.White.copy(alpha=0.7f))){ Row(Modifier.fillMaxSize().padding(horizontal=16.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(12.dp)){ Icon(Icons.Filled.Search, null, tint=Color.Gray); Text("Spotlight Search", color=Color.Gray, fontSize=14.sp) } } }
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderWidget(cat: String, apps: List<AppInfo>, onClick:()->Unit){ Card(Modifier.fillMaxWidth().height(110.dp).clickable{ onClick() }, shape=RoundedCornerShape(20.dp), colors=CardDefaults.cardColors(containerColor=Color.White.copy(alpha=0.75f))){ Column(Modifier.padding(12.dp)){ Text(cat, fontWeight=FontWeight.Bold, fontSize=12.sp); Spacer(Modifier.height(8.dp)); Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){ apps.take(4).forEach{ FastAppIcon(app=it, modifier=Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)), size=32) } } } } }
@Composable
fun StorageWidget(){ Card(Modifier.fillMaxWidth().height(110.dp), shape=RoundedCornerShape(20.dp), colors=CardDefaults.cardColors(containerColor=Color.Black.copy(alpha=0.4f))){ Column(Modifier.padding(16.dp)){ Icon(Icons.Filled.Storage, null, tint=Color.White); Text("128 GB", color=Color.White, fontWeight=FontWeight.Bold); Text("Redmi Note 15 Pro", color=Color.White.copy(alpha=0.6f), fontSize=10.sp) } } }

@Composable
fun SpotlightPro(search: String, onSearch:(String)->Unit, apps: List<AppInfo>, onClose:()->Unit, onLaunch:(AppInfo)->Unit){
    val calcResult = remember(search){ try{ if(search.matches(Regex("[0-9+\\-*/(). ]+"))){ evalCalc(search) } else null } catch(e:Exception){ null } }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.5f)).clickable{ onClose() }, contentAlignment=Alignment.TopCenter) {
        Card(Modifier.padding(top=70.dp).fillMaxWidth(0.92f), shape=RoundedCornerShape(22.dp), colors=CardDefaults.cardColors(containerColor=Color.White.copy(alpha=0.98f))){
            Column(Modifier.padding(16.dp)){
                OutlinedTextField(value=search, onValueChange=onSearch, placeholder={Text("Search apps, calculate...")}, leadingIcon={Icon(Icons.Filled.Search,null)}, trailingIcon={ if(search.isNotEmpty()) Icon(Icons.Filled.Close, null, modifier=Modifier.clickable{ onSearch("") }) }, modifier=Modifier.fillMaxWidth(), singleLine=true, shape=RoundedCornerShape(14.dp))
                calcResult?.let{ Card(Modifier.fillMaxWidth().padding(top=12.dp), colors=CardDefaults.cardColors(containerColor=Color(0xFFF0F0F0))){ Row(Modifier.padding(16.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.SpaceBetween){ Text(search, color=Color.Gray); Text("= $it", fontWeight=FontWeight.Bold, fontSize=18.sp) } } }
                Spacer(Modifier.height(12.dp))
                LazyVerticalGrid(columns=GridCells.Fixed(4), modifier=Modifier.heightIn(max=420.dp)){ items(apps.take(28), key={it.packageName}){ app -> Column(Modifier.padding(8.dp).clip(RoundedCornerShape(12.dp)).clickable{ onLaunch(app) }.padding(4.dp), horizontalAlignment=Alignment.CenterHorizontally){ FastAppIcon(app=app, modifier=Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)), size=54); Text(app.label, fontSize=10.sp, maxLines=1, textAlign=TextAlign.Center, modifier=Modifier.padding(top=4.dp)) } } }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LaunchpadPro(apps: List<AppInfo>, grouped: Map<String, List<AppInfo>>, folders: Map<String, List<AppInfo>>, folderFilter: String?, search: String, onSearch:(String)->Unit, onClose:()->Unit, onAppClick:(AppInfo)->Unit, onAppLong:(AppInfo)->Unit){
    val displayApps = if(folderFilter!=null) grouped[folderFilter] ?: emptyList() else apps
    Box(Modifier.fillMaxSize().background(Color(0xFF0B102A).copy(alpha=0.98f))){
        Column(Modifier.fillMaxSize().padding(16.dp)){
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically){
                Column{ Text(if(folderFilter!=null) folderFilter else "Launchpad", color=Color.White, fontSize=26.sp, fontWeight=FontWeight.Bold); if(folderFilter==null) Text("${apps.size} apps • Redmi Note 15 Pro 5G • 120Hz", color=Color.White.copy(alpha=0.5f), fontSize=11.sp) }
                Box(Modifier.clip(CircleShape).background(Color.White.copy(alpha=0.15f)).clickable{ onClose() }.padding(12.dp)){ Icon(Icons.Filled.Close, null, tint=Color.White) }
            }
            OutlinedTextField(value=search, onValueChange=onSearch, placeholder={Text("Search", color=Color.White.copy(alpha=0.5f))}, leadingIcon={Icon(Icons.Filled.Search,null,tint=Color.White)}, modifier=Modifier.fillMaxWidth().padding(vertical=12.dp), shape=RoundedCornerShape(14.dp), colors=OutlinedTextFieldDefaults.colors(focusedTextColor=Color.White, unfocusedTextColor=Color.White, focusedBorderColor=Color.White.copy(alpha=0.4f), unfocusedBorderColor=Color.White.copy(alpha=0.15f)))
            if(folderFilter==null && search.isBlank()){
                LazyVerticalGrid(columns=GridCells.Fixed(2), modifier=Modifier.fillMaxWidth().height(110.dp), horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    items(folders.entries.toList()){ (cat, list) -> Card(Modifier.fillMaxWidth().clickable{ onSearch(cat) }, shape=RoundedCornerShape(18.dp), colors=CardDefaults.cardColors(containerColor=Color.White.copy(alpha=0.12f))){ Column(Modifier.padding(12.dp)){ Text(cat, color=Color.White, fontWeight=FontWeight.Bold, fontSize=13.sp); Spacer(Modifier.height(8.dp)); Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){ list.take(4).forEach{ FastAppIcon(it, Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)), 28) } }; Text("${list.size} apps", color=Color.White.copy(alpha=0.5f), fontSize=10.sp, modifier=Modifier.padding(top=6.dp)) } } }
                }
                Spacer(Modifier.height(12.dp))
            }
            LazyVerticalGrid(columns=GridCells.Fixed(4), modifier=Modifier.fillMaxSize()){ items(displayApps, key={it.packageName}){ app -> Column(Modifier.padding(12.dp).clip(RoundedCornerShape(14.dp)).combinedClickable(onClick={ onAppClick(app) }, onLongClick={ onAppLong(app) }).padding(6.dp), horizontalAlignment=Alignment.CenterHorizontally){ FastAppIcon(app=app, modifier=Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)), size=64); Text(app.label, color=Color.White, fontSize=11.sp, maxLines=2, textAlign=TextAlign.Center, modifier=Modifier.padding(top=8.dp)) } } }
        }
    }
}

@Composable
fun ControlCenterPro(onClose:()->Unit, onWallpaper:()->Unit){
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.35f)).clickable{ onClose() }, contentAlignment=Alignment.TopEnd){
        Card(Modifier.padding(top=42.dp, end=10.dp).width(300.dp), shape=RoundedCornerShape(24.dp), colors=CardDefaults.cardColors(containerColor=Color(0xFF1E1E1E).copy(alpha=0.96f))){
            Column(Modifier.padding(18.dp), verticalArrangement=Arrangement.spacedBy(14.dp)){
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically){ Text("Control Center", color=Color.White, fontWeight=FontWeight.Bold, fontSize=16.sp); Box(Modifier.clip(CircleShape).background(Color.White.copy(alpha=0.15f)).clickable{ onClose() }.padding(6.dp)){ Icon(Icons.Filled.Close, null, tint=Color.White, modifier=Modifier.size(16.dp)) } }
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){ ControlTilePro(Icons.Filled.Wifi, "Wi-Fi", "Connected", true, Modifier.weight(1f)); ControlTilePro(Icons.Filled.Bluetooth, "Bluetooth", "On", true, Modifier.weight(1f)) }
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){ ControlTilePro(Icons.Filled.BatteryFull, "Battery", "73%", true, Modifier.weight(1f)); ControlTilePro(Icons.Filled.DataUsage, "5G", "Redmi 15 Pro", true, Modifier.weight(1f)) }
                Button(onClick=onWallpaper, modifier=Modifier.fillMaxWidth(), shape=RoundedCornerShape(14.dp), colors=ButtonDefaults.buttonColors(containerColor=Color.White)){ Icon(Icons.Filled.Wallpaper, null, tint=Color.Black); Spacer(Modifier.width(8.dp)); Text("Wallpapers", color=Color.Black) }
                Text("Optimized for Redmi Note 15 Pro 5G • 120Hz • HyperOS", color=Color.White.copy(alpha=0.4f), fontSize=10.sp, textAlign=TextAlign.Center, modifier=Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun ControlTilePro(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, active: Boolean, modifier: Modifier){
    Card(modifier=modifier, shape=RoundedCornerShape(16.dp), colors=CardDefaults.cardColors(containerColor=if(active) Color(0xFF0A84FF) else Color.White.copy(alpha=0.12f))){ Row(Modifier.padding(12.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp)){ Icon(icon, null, tint=Color.White, modifier=Modifier.size(22.dp)); Column{ Text(title, color=Color.White, fontSize=12.sp, fontWeight=FontWeight.Bold); Text(subtitle, color=Color.White.copy(alpha=0.7f), fontSize=10.sp) } } }
}

@Composable
fun NotificationCenter(onClose:()->Unit){
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.25f)).clickable{ onClose() }, contentAlignment=Alignment.TopStart){
        Card(Modifier.padding(top=42.dp, start=10.dp).width(320.dp), shape=RoundedCornerShape(22.dp), colors=CardDefaults.cardColors(containerColor=Color.White.copy(alpha=0.96f))){
            Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(10.dp)){
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween){ Text("Notifications", fontWeight=FontWeight.Bold); Icon(Icons.Filled.Close, null, modifier=Modifier.clickable{ onClose() }) }
                Card(colors=CardDefaults.cardColors(containerColor=Color(0xFFF5F5F7))){ ListItem(headlineContent={Text("Redmi Note 15 Pro")}, supportingContent={Text("120Hz Optimized")}, leadingContent={Icon(Icons.Filled.PhoneAndroid, null)}) }
                Text("No more notifications", color=Color.Gray, fontSize=12.sp, modifier=Modifier.fillMaxWidth(), textAlign=TextAlign.Center)
            }
        }
    }
}

@Composable
fun WallpaperPicker(current: Int, wallpapers: List<Brush>, onSelect:(Int)->Unit, onClose:()->Unit){
    AlertDialog(onDismissRequest=onClose, shape=RoundedCornerShape(24.dp), title={Text("Wallpapers • MacOS")}, text={ LazyVerticalGrid(columns=GridCells.Fixed(2), verticalArrangement=Arrangement.spacedBy(12.dp), horizontalArrangement=Arrangement.spacedBy(12.dp)){ items(wallpapers.size){ idx -> Box(Modifier.height(90.dp).clip(RoundedCornerShape(16.dp)).background(wallpapers[idx]).border(if(current==idx) 3.dp else 0.dp, if(current==idx) Color(0xFF0A84FF) else Color.Transparent, RoundedCornerShape(16.dp)).clickable{ onSelect(idx) }){ if(current==idx) Icon(Icons.Filled.CheckCircle, null, tint=Color.White, modifier=Modifier.align(Alignment.Center).background(Color.Black.copy(alpha=0.3f), CircleShape)) } } } }, confirmButton={ TextButton(onClick=onClose){ Text("Done") } })
}

@Composable
fun AppMenuPro(app: AppInfo, onDismiss:()->Unit, onOpen:()->Unit, onInfo:()->Unit, onUninstall:()->Unit){
    AlertDialog(onDismissRequest=onDismiss, shape=RoundedCornerShape(22.dp), title={ Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(12.dp)){ FastAppIcon(app, Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)), 48); Column{ Text(app.label, fontWeight=FontWeight.Bold); Text(app.packageName, fontSize=11.sp, color=Color.Gray) } } }, text={
        Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
            Button(onClick=onOpen, modifier=Modifier.fillMaxWidth(), shape=RoundedCornerShape(12.dp)){ Icon(Icons.Filled.Launch, null); Spacer(Modifier.width(8.dp)); Text("Open") }
            OutlinedButton(onClick=onInfo, modifier=Modifier.fillMaxWidth(), shape=RoundedCornerShape(12.dp)){ Icon(Icons.Filled.Info, null); Spacer(Modifier.width(8.dp)); Text("App Info") }
            OutlinedButton(onClick=onUninstall, modifier=Modifier.fillMaxWidth(), shape=RoundedCornerShape(12.dp), colors=ButtonDefaults.outlinedButtonColors(contentColor=Color.Red)){ Icon(Icons.Filled.Delete, null); Spacer(Modifier.width(8.dp)); Text("Uninstall") }
        }
    }, confirmButton={ TextButton(onClick=onDismiss){ Text("Cancel") } })
}

fun evalCalc(expr: String): String {
    return try {
        val clean = expr.replace(" ", "")
        var result = 0.0
        var current = ""
        var op = '+'
        for(c in clean + "+") {
            if(c.isDigit() || c=='.'){ current+=c } else {
                val num = current.toDoubleOrNull() ?: 0.0
                when(op){ '+' -> result+=num; '-' -> result-=num; '*' -> result*=num; '/' -> if(num!=0.0) result/=num }
                op=c; current=""
            }
        }
        if(result % 1 == 0.0) result.toInt().toString() else String.format("%.2f", result)
    } catch(e:Exception){ "Error" }
}
