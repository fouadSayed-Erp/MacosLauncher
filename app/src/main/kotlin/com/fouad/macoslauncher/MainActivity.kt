@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.fouad.macoslauncher

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.BatteryManager
import android.os.Bundle
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

object IconCache {
    private val cache = ConcurrentHashMap<String, androidx.compose.ui.graphics.ImageBitmap>()
    fun get(key: String) = cache[key]
    fun put(key: String, bmp: androidx.compose.ui.graphics.ImageBitmap) { if(cache.size>250) cache.clear(); cache[key]=bmp }
}

object LauncherPrefs {
    private const val NAME = "macos_launcher_v12_windows"
    fun prefs(c: Context) = c.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    fun getWallpaper(c: Context) = prefs(c).getInt("wallpaper", 0)
    fun setWallpaper(c: Context, i: Int) = prefs(c).edit().putInt("wallpaper", i).apply()
    fun getRecentList(c: Context): MutableList<String> { val s = prefs(c).getString("recent", "") ?: ""; return if(s.isEmpty()) mutableListOf() else s.split(",").toMutableList() }
    fun addRecent(c: Context, pkg: String) { val list = getRecentList(c); list.remove(pkg); list.add(0, pkg); if(list.size>15) list.removeAt(list.size-1); prefs(c).edit().putString("recent", list.joinToString(",")).apply() }
}

data class AppInfo(val label: String, val packageName: String, val icon: Drawable, val category: String = "Other")
fun getCategory(label: String): String {
    val l = label.lowercase()
    return when {
        l.contains("facebook") || l.contains("whatsapp") || l.contains("telegram") -> "Social"
        l.contains("game") -> "Games"
        l.contains("camera") || l.contains("gallery") -> "Media"
        l.contains("chrome") || l.contains("youtube") -> "Internet"
        l.contains("settings") -> "Tools"
        else -> "Other"
    }
}
suspend fun loadAppsOptimized(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
    pm.queryIntentActivities(intent, 0).map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.loadIcon(pm), getCategory(it.loadLabel(pm).toString())) }.sortedBy { it.label.lowercase() }
}
fun launchAppFull(context: Context, app: AppInfo) { try { LauncherPrefs.addRecent(context, app.packageName); context.startActivity(context.packageManager.getLaunchIntentForPackage(app.packageName)?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch(e:Exception){} }

data class FloatingWindow(
    val id: String = UUID.randomUUID().toString(),
    val app: AppInfo,
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 320f,
    val height: Float = 420f,
    val zIndex: Int = 0,
    val isMinimized: Boolean = false,
    val isMaximized: Boolean = false
)

val wallpapers = listOf(
    Brush.verticalGradient(listOf(Color(0xFF1A0B2E), Color(0xFF4A1942), Color(0xFF8B2252))),
    Brush.verticalGradient(listOf(Color(0xFF0A0E27), Color(0xFF1A2A6A), Color(0xFF3A5AC0))),
    Brush.verticalGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460)))
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { MacOSWindowManager() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacOSWindowManager() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val screenW = config.screenWidthDp.toFloat()
    val screenH = config.screenHeightDp.toFloat()

    var allApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var wallpaperIndex by remember { mutableStateOf(LauncherPrefs.getWallpaper(context)) }
    var showSpotlight by remember { mutableStateOf(false) }
    var showControlCenter by remember { mutableStateOf(false) }
    var showSidebar by remember { mutableStateOf(false) }
    var showWallpaperPicker by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var windows by remember { mutableStateOf(listOf<FloatingWindow>()) }
    var nextZ by remember { mutableStateOf(0) }
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        allApps = loadAppsOptimized(context)
        isLoading = false
    }

    val filtered by remember { derivedStateOf { if(search.isBlank()) allApps else allApps.filter{ it.label.contains(search, true) } } }
    val recentApps = remember(allApps) { val rec = LauncherPrefs.getRecentList(context); allApps.filter{ rec.contains(it.packageName) }.sortedBy{ rec.indexOf(it.packageName) } }
    val dockApps = remember(allApps) { allApps.take(6) }

    fun openWindow(app: AppInfo) {
        val exists = windows.find { it.app.packageName == app.packageName && !it.isMinimized }
        if(exists != null) {
            // Bring to front
            windows = windows.map { if(it.id==exists.id) it.copy(zIndex=nextZ) else it }
            nextZ++
            return
        }
        val newW = FloatingWindow(
            app=app,
            x = (20 + (windows.size * 25) % (screenW - 340)).toFloat(),
            y = (60 + (windows.size * 35) % (screenH - 500)).toFloat(),
            width = 340f,
            height = 460f,
            zIndex = nextZ
        )
        windows = windows + newW
        nextZ++
        LauncherPrefs.addRecent(context, app.packageName)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun closeWindow(id: String) { windows = windows.filter { it.id != id } }
    fun minimizeWindow(id: String) { windows = windows.map { if(it.id==id) it.copy(isMinimized=true) else it } }
    fun maximizeWindow(id: String) { 
        windows = windows.map { 
            if(it.id==id) {
                if(it.isMaximized) it.copy(isMaximized=false, x=it.x, y=it.y, width=340f, height=460f, zIndex=nextZ)
                else it.copy(isMaximized=true, x=10f, y=50f, width=screenW-20f, height=screenH-140f, zIndex=nextZ)
            } else it 
        }
        nextZ++
    }
    fun bringToFront(id: String) { windows = windows.map { if(it.id==id) it.copy(zIndex=nextZ) else it }; nextZ++ }
    fun updateWindowPos(id: String, dx: Float, dy: Float) {
        windows = windows.map {
            if(it.id==id && !it.isMaximized) {
                val nx = (it.x + dx).coerceIn(0f, screenW - it.width)
                val ny = (it.y + dy).coerceIn(40f, screenH - 100f)
                it.copy(x=nx, y=ny)
            } else it
        }
    }
    fun updateWindowSize(id: String, dw: Float, dh: Float) {
        windows = windows.map {
            if(it.id==id && !it.isMaximized) {
                val nw = (it.width + dw).coerceIn(260f, screenW - 20f)
                val nh = (it.height + dh).coerceIn(300f, screenH - 80f)
                it.copy(width=nw, height=nh)
            } else it
        }
    }

    Box(Modifier.fillMaxSize().background(wallpapers[wallpaperIndex])) {
        // Top Bar
        TopBarWindow(onSpotlight={ showSpotlight=true }, onControl={ showControlCenter=true }, windowsCount=windows.size)

        // Desktop Windows Area
        Box(Modifier.fillMaxSize().padding(top=40.dp)) {
            // Render windows sorted by zIndex
            windows.sortedBy { it.zIndex }.forEach { win ->
                if(!win.isMinimized) {
                    FloatingWindowComposable(
                        window=win,
                        onClose={ closeWindow(win.id) },
                        onMinimize={ minimizeWindow(win.id) },
                        onMaximize={ maximizeWindow(win.id) },
                        onBringToFront={ bringToFront(win.id) },
                        onDrag={ dx, dy -> updateWindowPos(win.id, dx, dy) },
                        onResize={ dw, dh -> updateWindowSize(win.id, dw, dh) },
                        onLaunchFull={ launchAppFull(context, win.app) }
                    )
                }
            }

            // Empty hint
            if(windows.isEmpty() && !isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment=Alignment.Center){
                    Card(shape=RoundedCornerShape(24.dp), colors=CardDefaults.cardColors(containerColor=Color.Black.copy(alpha=0.4f))){
                        Column(Modifier.padding(24.dp), horizontalAlignment=Alignment.CenterHorizontally){
                            Icon(Icons.Filled.ViewInAr, null, tint=Color.White, modifier=Modifier.size(48.dp))
                            Text("Window Manager", color=Color.White, fontWeight=FontWeight.Bold, fontSize=18.sp, modifier=Modifier.padding(top=12.dp))
                            Text("Tap any app in Dock to open as window", color=Color.White.copy(alpha=0.7f), fontSize=12.sp, textAlign=androidx.compose.ui.text.style.TextAlign.Center, modifier=Modifier.padding(top=6.dp))
                            Text("Drag title bar to move • Drag corner to resize", color=Color.White.copy(alpha=0.5f), fontSize=10.sp, modifier=Modifier.padding(top=8.dp))
                        }
                    }
                }
            }
        }

        // Minimized windows bar
        if(windows.any { it.isMinimized }) {
            Row(Modifier.align(Alignment.BottomStart).padding(start=12.dp, bottom=80.dp), horizontalArrangement=Arrangement.spacedBy(8.dp)){
                windows.filter { it.isMinimized }.forEach { win ->
                    Card(Modifier.clip(RoundedCornerShape(10.dp)).clickable{ windows = windows.map { if(it.id==win.id) it.copy(isMinimized=false, zIndex=nextZ) else it }; nextZ++ }, shape=RoundedCornerShape(10.dp), colors=CardDefaults.cardColors(containerColor=Color.White.copy(alpha=0.9f))){
                        Row(Modifier.padding(horizontal=12.dp, vertical=8.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            FastIcon(win.app, Modifier.size(20.dp), 20)
                            Text(win.app.label.take(12), fontSize=11.sp, fontWeight=FontWeight.Medium)
                            Icon(Icons.Filled.Close, null, modifier=Modifier.size(14.dp).clickable{ closeWindow(win.id) })
                        }
                    }
                }
            }
        }

        // Dock
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom=10.dp), horizontalAlignment=Alignment.CenterHorizontally){
            if(windows.isNotEmpty()) {
                Text("${windows.size} windows • Drag title to move • Corner to resize", color=Color.White.copy(alpha=0.6f), fontSize=10.sp, modifier=Modifier.padding(bottom=6.dp))
            }
            DockWindow(dockApps=dockApps, onSidebar={ showSidebar=true }, onAppClick={ openWindow(it) }, onAppLong={ selectedApp=it })
        }

        if(isLoading) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.5f)), contentAlignment=Alignment.Center){ CircularProgressIndicator(color=Color.White) }
        if(showSpotlight) SpotlightWindow(search=search, onSearch={ search=it }, apps=filtered, onClose={ showSpotlight=false; search="" }, onLaunch={ openWindow(it); showSpotlight=false })
        if(showSidebar) SidebarWindow(allApps=filtered, recent=recentApps, onClose={ showSidebar=false }, onAppClick={ openWindow(it); showSidebar=false })
        if(showControlCenter) ControlCenterWindow(onClose={ showControlCenter=false }, onWallpaper={ showWallpaperPicker=true; showControlCenter=false })
        if(showWallpaperPicker) WallpaperPickerWindow(current=wallpaperIndex, wallpapers=wallpapers, onSelect={ wallpaperIndex=it; LauncherPrefs.setWallpaper(context, it); showWallpaperPicker=false }, onClose={ showWallpaperPicker=false })
        selectedApp?.let { app ->
            AlertDialog(onDismissRequest={ selectedApp=null }, title={Text(app.label)}, text={ Column{ Button(onClick={ openWindow(app); selectedApp=null }, modifier=Modifier.fillMaxWidth()){ Text("Open as Window") }; OutlinedButton(onClick={ launchAppFull(context, app); selectedApp=null }, modifier=Modifier.fillMaxWidth()){ Text("Open Fullscreen") } } }, confirmButton={ TextButton(onClick={ selectedApp=null }){ Text("Cancel") } })
        }
    }
}

@Composable
fun TopBarWindow(onSpotlight:()->Unit, onControl:()->Unit, windowsCount: Int){
    var time by remember { mutableStateOf("") }
    LaunchedEffect(Unit){ while(true){ time = SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date()); delay(1000) } }
    Row(Modifier.fillMaxWidth().height(40.dp).background(Color.Black.copy(alpha=0.4f)).padding(horizontal=14.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.SpaceBetween){
        Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(12.dp)){
            Text("Finder", color=Color.White, fontWeight=FontWeight.Bold, fontSize=14.sp)
            if(windowsCount>0) Card(shape=RoundedCornerShape(10.dp), colors=CardDefaults.cardColors(containerColor=Color(0xFF0A84FF))){ Text("$windowsCount windows", color=Color.White, fontSize=10.sp, modifier=Modifier.padding(horizontal=8.dp, vertical=4.dp)) }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp), verticalAlignment=Alignment.CenterVertically){
            Icon(Icons.Filled.Search, null, tint=Color.White, modifier=Modifier.size(18.dp).clickable{ onSpotlight() })
            Icon(Icons.Filled.Tune, null, tint=Color.White, modifier=Modifier.size(18.dp).clickable{ onControl() })
            Text(time, color=Color.White, fontSize=11.sp)
        }
    }
}

@Composable
fun FloatingWindowComposable(
    window: FloatingWindow,
    onClose:()->Unit,
    onMinimize:()->Unit,
    onMaximize:()->Unit,
    onBringToFront:()->Unit,
    onDrag:(Float, Float)->Unit,
    onResize:(Float, Float)->Unit,
    onLaunchFull:()->Unit
){
    val scale by animateFloatAsState(if(window.isMaximized) 1f else 1f)
    Box(
        Modifier
            .offset(x = window.x.dp, y = window.y.dp)
            .width(window.width.dp)
            .height(window.height.dp)
            .scale(scale)
            .shadow(24.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E1E1E))
            .border(1.dp, Color.White.copy(alpha=0.15f), RoundedCornerShape(16.dp))
            .clickable(interactionSource=remember { MutableInteractionSource() }, indication=null){ onBringToFront() }
    ){
        Column(Modifier.fillMaxSize()){
            // Title Bar - Draggable
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .background(Color(0xFF2C2C2E))
                    .pointerInput(Unit){
                        detectDragGestures { change, dragAmount ->
                            onDrag(dragAmount.x, dragAmount.y)
                        }
                    }
                    .clickable{ onBringToFront() }
                    .padding(horizontal=12.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.SpaceBetween
            ){
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp), verticalAlignment=Alignment.CenterVertically){
                    Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFFF5F57)).clickable{ onClose() })
                    Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFFFBD2E)).clickable{ onMinimize() })
                    Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF28CA42)).clickable{ onMaximize() })
                    Spacer(Modifier.width(8.dp))
                    FastIcon(window.app, Modifier.size(18.dp), 18)
                    Text(window.app.label, color=Color.White, fontSize=12.sp, fontWeight=FontWeight.Medium, maxLines=1, modifier=Modifier.widthIn(max=120.dp))
                }
                Icon(Icons.Filled.OpenInNew, null, tint=Color.White.copy(alpha=0.5f), modifier=Modifier.size(16.dp).clickable{ onLaunchFull() })
            }

            // Content
            Box(Modifier.fillMaxSize().background(Color(0xFF1E1E1E))){
                Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.Center){
                    FastIcon(window.app, Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)), 72)
                    Text(window.app.label, color=Color.White, fontWeight=FontWeight.Bold, fontSize=16.sp, modifier=Modifier.padding(top=12.dp))
                    Text(window.app.packageName, color=Color.White.copy(alpha=0.5f), fontSize=10.sp, modifier=Modifier.padding(top=2.dp))
                    Text(window.app.category, color=Color(0xFF0A84FF), fontSize=11.sp, modifier=Modifier.padding(top=4.dp))
                    Spacer(Modifier.height(16.dp))
                    Button(onClick=onLaunchFull, shape=RoundedCornerShape(10.dp), modifier=Modifier.fillMaxWidth(0.8f)){ Icon(Icons.Filled.Launch, null, modifier=Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Open Fullscreen") }
                    OutlinedButton(onClick={}, shape=RoundedCornerShape(10.dp), modifier=Modifier.fillMaxWidth(0.8f).padding(top=8.dp), colors=ButtonDefaults.outlinedButtonColors(contentColor=Color.White)){ Text("${window.width.toInt()} x ${window.height.toInt()} • Drag corner to resize", fontSize=10.sp) }
                }

                // Resize handle - Bottom Right
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .pointerInput(Unit){
                            detectDragGestures { change, dragAmount ->
                                onResize(dragAmount.x, dragAmount.y)
                            }
                        },
                    contentAlignment=Alignment.BottomEnd
                ){
                    Icon(Icons.Filled.OpenInFull, null, tint=Color.White.copy(alpha=0.3f), modifier=Modifier.size(18.dp).padding(4.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DockWindow(dockApps: List<AppInfo>, onSidebar:()->Unit, onAppClick:(AppInfo)->Unit, onAppLong:(AppInfo)->Unit){
    Row(Modifier.clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha=0.18f)).border(1.dp, Color.White.copy(alpha=0.25f), RoundedCornerShape(22.dp)).padding(horizontal=12.dp, vertical=10.dp), verticalAlignment=Alignment.Bottom, horizontalArrangement=Arrangement.spacedBy(10.dp)){
        Column(horizontalAlignment=Alignment.CenterHorizontally, modifier=Modifier.clickable{ onSidebar() }){
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(Color.White), contentAlignment=Alignment.Center){ Text("F", color=Color(0xFF0A84FF), fontWeight=FontWeight.Bold, fontSize=20.sp) }
            Text("Finder", color=Color.White, fontSize=9.sp)
        }
        Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(alpha=0.3f)))
        dockApps.forEach { app ->
            Column(horizontalAlignment=Alignment.CenterHorizontally, modifier=Modifier.combinedClickable(onClick={ onAppClick(app) }, onLongClick={ onAppLong(app) })){
                FastIcon(app, Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)), 48)
                Box(Modifier.size(4.dp).clip(CircleShape).background(Color.Transparent).padding(top=2.dp))
            }
        }
    }
}

@Composable
fun FastIcon(app: AppInfo, modifier: Modifier, size: Int){
    var bitmap by remember(app.packageName, size) { mutableStateOf(IconCache.get(app.packageName+size)) }
    LaunchedEffect(app.packageName, size){
        if(bitmap==null){
            withContext(Dispatchers.Default){
                try{
                    val bmp = app.icon.toBitmap(size, size).asImageBitmap()
                    IconCache.put(app.packageName+size, bmp)
                    bitmap = bmp
                }catch(e:Exception){}
            }
        }
    }
    if(bitmap!=null) Image(bitmap=bitmap!!, contentDescription=null, modifier=modifier) else Box(modifier=modifier.background(Color.White.copy(alpha=0.3f), RoundedCornerShape(10.dp)))
}

@Composable
fun SpotlightWindow(search: String, onSearch:(String)->Unit, apps: List<AppInfo>, onClose:()->Unit, onLaunch:(AppInfo)->Unit){
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.5f)).clickable{ onClose() }, contentAlignment=Alignment.TopCenter){
        Card(Modifier.padding(top=60.dp).fillMaxWidth(0.92f), shape=RoundedCornerShape(18.dp)){ Column(Modifier.padding(14.dp)){ OutlinedTextField(value=search, onValueChange=onSearch, placeholder={Text("Search to open as window")}, leadingIcon={Icon(Icons.Filled.Search,null)}, modifier=Modifier.fillMaxWidth(), singleLine=true); LazyVerticalGrid(columns=GridCells.Fixed(4), modifier=Modifier.heightIn(max=380.dp).padding(top=10.dp)){ items(apps.take(20), key={it.packageName}){ app -> Column(Modifier.padding(8.dp).clickable{ onLaunch(app) }, horizontalAlignment=Alignment.CenterHorizontally){ FastIcon(app, Modifier.size(48.dp), 48); Text(app.label, fontSize=10.sp, maxLines=1) } } } } }
    }
}

@Composable
fun SidebarWindow(allApps: List<AppInfo>, recent: List<AppInfo>, onClose:()->Unit, onAppClick:(AppInfo)->Unit){
    val config = LocalConfiguration.current
    val width = (config.screenWidthDp * 0.88f).dp
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.45f)).clickable{ onClose() }){
        Card(Modifier.width(width).fillMaxHeight(), shape=RoundedCornerShape(topEnd=24.dp, bottomEnd=24.dp), colors=CardDefaults.cardColors(containerColor=Color(0xFF1C1C1E).copy(alpha=0.98f))){
            Column(Modifier.fillMaxSize().padding(16.dp)){
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically){ Text("All Apps • Open as Window", color=Color.White, fontWeight=FontWeight.Bold, fontSize=16.sp); Icon(Icons.Filled.Close, null, tint=Color.White, modifier=Modifier.clickable{ onClose() }) }
                LazyVerticalGrid(columns=GridCells.Fixed(4), modifier=Modifier.fillMaxSize().padding(top=12.dp)){ items(allApps, key={it.packageName}){ app -> Column(Modifier.padding(8.dp).clickable{ onAppClick(app) }, horizontalAlignment=Alignment.CenterHorizontally){ FastIcon(app, Modifier.size(52.dp), 52); Text(app.label, color=Color.White, fontSize=10.sp, maxLines=1) } } }
            }
        }
    }
}

@Composable
fun ControlCenterWindow(onClose:()->Unit, onWallpaper:()->Unit){
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.35f)).clickable{ onClose() }, contentAlignment=Alignment.BottomCenter){
        Card(Modifier.fillMaxWidth().fillMaxHeight(0.7f), shape=RoundedCornerShape(topStart=28.dp, topEnd=28.dp), colors=CardDefaults.cardColors(containerColor=Color(0xFF1C1C1E).copy(alpha=0.98f))){
            Column(Modifier.padding(20.dp), verticalArrangement=Arrangement.spacedBy(16.dp)){
                Box(Modifier.width(40.dp).height(4.dp).background(Color.White.copy(alpha=0.3f), RoundedCornerShape(2.dp)).align(Alignment.CenterHorizontally))
                Text("Window Manager • ${LocalConfiguration.current.screenWidthDp} x ${LocalConfiguration.current.screenHeightDp}", color=Color.White, fontWeight=FontWeight.Bold)
                Text("• Tap app in Dock to open as floating window\n• Drag title bar to move window\n• Drag bottom-right corner to resize\n• Red = Close, Yellow = Minimize, Green = Maximize\n• Minimized windows appear above Dock", color=Color.White.copy(alpha=0.7f), fontSize=12.sp)
                Button(onClick=onWallpaper, modifier=Modifier.fillMaxWidth()){ Text("Change Wallpaper") }
                Button(onClick=onClose, modifier=Modifier.fillMaxWidth()){ Text("Close") }
            }
        }
    }
}

@Composable
fun WallpaperPickerWindow(current: Int, wallpapers: List<Brush>, onSelect:(Int)->Unit, onClose:()->Unit){
    AlertDialog(onDismissRequest=onClose, title={Text("Wallpapers")}, text={ LazyVerticalGrid(columns=GridCells.Fixed(2)){ items(wallpapers.size){ idx -> Box(Modifier.height(80.dp).clip(RoundedCornerShape(12.dp)).background(wallpapers[idx]).border(if(current==idx) 2.dp else 0.dp, if(current==idx) Color.Blue else Color.Transparent, RoundedCornerShape(12.dp)).clickable{ onSelect(idx) }.padding(8.dp)) } } }, confirmButton={ TextButton(onClick=onClose){ Text("Done") } })
}
