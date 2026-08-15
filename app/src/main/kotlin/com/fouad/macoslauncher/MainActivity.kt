package com.fouad.macoslauncher

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
fun launchApp(context: Context, app: AppInfo) { try { context.startActivity(context.packageManager.getLaunchIntentForPackage(app.packageName)) } catch(e:Exception){} }

val wallpapers = listOf(
    Brush.verticalGradient(listOf(Color(0xFF0A0E27), Color(0xFF1A2A6A), Color(0xFF3A5AC0))) to "Sonoma Dark",
    Brush.verticalGradient(listOf(Color(0xFFEB4D4B), Color(0xFFF0932B), Color(0xFFEB4D4B))) to "Ventura Orange",
    Brush.verticalGradient(listOf(Color(0xFF6C5CE7), Color(0xFFA29BFE), Color(0xFF81ECEC))) to "Monterey Purple",
    Brush.verticalGradient(listOf(Color(0xFF00B894), Color(0xFF00CEC9), Color(0xFF0984E3))) to "Big Sur Green",
    Brush.linearGradient(listOf(Color(0xFF2D3436), Color(0xFF636E72), Color(0xFF2D3436))) to "Midnight"
).map { it.first }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { MacOSV7() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacOSV7() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var allApps by remember { mutableStateOf(listOf<AppInfo>()) }
    var showLaunchpad by remember { mutableStateOf(false) }
    var showSpotlight by remember { mutableStateOf(false) }
    var showControlCenter by remember { mutableStateOf(false) }
    var showWallpaperPicker by remember { mutableStateOf(false) }
    var selectedAppMenu by remember { mutableStateOf<AppInfo?>(null) }
    var search by remember { mutableStateOf("") }
    var wallpaperIndex by remember { mutableStateOf(0) }
    var dockApps by remember { mutableStateOf(listOf<AppInfo>()) }

    LaunchedEffect(Unit) { allApps = getInstalledApps(context); dockApps = allApps.take(8) }
    val filtered = remember(search, allApps) { if(search.isBlank()) allApps else allApps.filter{ it.label.contains(search,true) } }

    Box(Modifier.fillMaxSize().background(wallpapers[wallpaperIndex])) {
        // Desktop - Long press to change wallpaper
        Box(Modifier.fillMaxSize().combinedClickable(onClick = {}, onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); showWallpaperPicker = true }))

        Column(Modifier.fillMaxSize()) {
            // TopBar with Control Center
            var time by remember { mutableStateOf("") }
            LaunchedEffect(Unit){ while(true){ time = SimpleDateFormat("EEE h:mm a", Locale.ENGLISH).format(Date()); delay(1000) } }
            Row(Modifier.fillMaxWidth().height(36.dp).background(Color.Black.copy(alpha=0.35f)).padding(horizontal=16.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.SpaceBetween){
                Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(16.dp)){
                    Text("Finder", color=Color.White, fontSize=14.sp, fontWeight=FontWeight.Bold)
                    Text("File Edit View", color=Color.White.copy(alpha=0.7f), fontSize=12.sp)
                }
                Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    Icon(Icons.Filled.Search, null, tint=Color.White, modifier=Modifier.size(18.dp).clip(CircleShape).clickable{ showSpotlight=true })
                    Icon(Icons.Filled.Wifi, null, tint=Color.White, modifier=Modifier.size(16.dp))
                    Icon(Icons.Filled.BatteryFull, null, tint=Color.White, modifier=Modifier.size(18.dp))
                    Text(time, color=Color.White, fontSize=12.sp, modifier=Modifier.clip(RoundedCornerShape(6.dp)).clickable{ showControlCenter=true }.padding(4.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            // DOCK v7 with magnification
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.Center){
                Row(Modifier.clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha=0.22f)).border(1.dp, Color.White.copy(alpha=0.35f), RoundedCornerShape(24.dp)).padding(10.dp), verticalAlignment=Alignment.Bottom, horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    // Launchpad
                    val lpInteraction = remember { MutableInteractionSource() }
                    val lpPressed by lpInteraction.collectIsPressedAsState()
                    val lpScale by animateFloatAsState(if(lpPressed) 1.4f else 1f, animationSpec=spring(dampingRatio=0.5f))
                    Box(Modifier.size(56.dp).scale(lpScale).clip(RoundedCornerShape(14.dp)).background(Color.White).combinedClickable(interactionSource=lpInteraction, indication=null, onClick={ showLaunchpad=true })){ Icon(Icons.Filled.Apps, null, tint=Color(0xFF1A2A6A), modifier=Modifier.size(30.dp).align(Alignment.Center)) }
                    Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(alpha=0.4f)).align(Alignment.CenterVertically))
                    dockApps.forEach { app ->
                        DockIcon(app=app, onClick={ launchApp(context, app) }, onLongClick={ haptic.performHapticFeedback(HapticFeedbackType.LongPress); selectedAppMenu=app })
                    }
                    Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(alpha=0.4f)).align(Alignment.CenterVertically))
                    // Trash
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha=0.8f)), contentAlignment=Alignment.Center){ Icon(Icons.Filled.Delete, null, tint=Color.Gray) }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Spotlight v7
        if(showSpotlight){
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.5f)).clickable{ showSpotlight=false }, contentAlignment=Alignment.TopCenter){
                Card(Modifier.padding(top=80.dp).fillMaxWidth(0.92f), shape=RoundedCornerShape(20.dp), colors=CardDefaults.cardColors(containerColor=Color.White.copy(alpha=0.97f))){
                    Column(Modifier.padding(16.dp)){
                        OutlinedTextField(value=search, onValueChange={search=it}, placeholder={Text("Spotlight Search - Apps, Calculator, etc")}, leadingIcon={Icon(Icons.Filled.Search,null)}, modifier=Modifier.fillMaxWidth(), singleLine=true)
                        Spacer(Modifier.height(12.dp))
                        LazyVerticalGrid(columns=GridCells.Fixed(4), modifier=Modifier.heightIn(max=400.dp)){ items(filtered.take(24)){ app -> Column(Modifier.padding(8.dp).clip(RoundedCornerShape(10.dp)).clickable{ launchApp(context,app); showSpotlight=false }.padding(4.dp), horizontalAlignment=Alignment.CenterHorizontally){ AppIcon(app.icon, Modifier.size(52.dp)); Text(app.label, fontSize=10.sp, maxLines=1, textAlign=TextAlign.Center, modifier=Modifier.padding(top=4.dp)) } } }
                    }
                }
            }
        }

        // Launchpad v7
        if(showLaunchpad){
            Box(Modifier.fillMaxSize().background(Color(0xFF0B102A).copy(alpha=0.97f))){
                Column(Modifier.fillMaxSize().padding(16.dp)){
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically){
                        Text("Launchpad", color=Color.White, fontSize=24.sp, fontWeight=FontWeight.Bold)
                        Box(Modifier.clip(CircleShape).background(Color.White.copy(alpha=0.2f)).clickable{ showLaunchpad=false }.padding(12.dp)){ Text("X", color=Color.White, fontWeight=FontWeight.Bold) }
                    }
                    OutlinedTextField(value=search, onValueChange={search=it}, placeholder={Text("Search", color=Color.White.copy(alpha=0.5f))}, leadingIcon={Icon(Icons.Filled.Search,null,tint=Color.White)}, modifier=Modifier.fillMaxWidth().padding(vertical=14.dp), colors=OutlinedTextFieldDefaults.colors(focusedTextColor=Color.White, unfocusedTextColor=Color.White, focusedBorderColor=Color.White.copy(alpha=0.5f), unfocusedBorderColor=Color.White.copy(alpha=0.2f)))
                    LazyVerticalGrid(columns=GridCells.Fixed(4), modifier=Modifier.fillMaxSize()){ items(filtered){ app -> Column(Modifier.padding(14.dp).clip(RoundedCornerShape(12.dp)).combinedClickable(onClick={ launchApp(context,app); showLaunchpad=false }, onLongClick={ selectedAppMenu=app; haptic.performHapticFeedback(HapticFeedbackType.LongPress) }).padding(4.dp), horizontalAlignment=Alignment.CenterHorizontally){ AppIcon(app.icon, Modifier.size(64.dp)); Text(app.label, color=Color.White, fontSize=11.sp, maxLines=2, textAlign=TextAlign.Center, modifier=Modifier.padding(top=6.dp)) } } }
                }
            }
        }

        // Control Center
        if(showControlCenter){
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.3f)).clickable{ showControlCenter=false }, contentAlignment=Alignment.TopEnd){
                Card(Modifier.padding(top=40.dp, end=12.dp).width(280.dp), shape=RoundedCornerShape(20.dp), colors=CardDefaults.cardColors(containerColor=Color(0xFF1C1C1E))){
                    Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)){
                        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween){ Text("Control Center", color=Color.White, fontWeight=FontWeight.Bold); Icon(Icons.Filled.Close, null, tint=Color.White, modifier=Modifier.clickable{ showControlCenter=false }) }
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
                            ControlTile(Icons.Filled.Wifi, "Wi-Fi", true)
                            ControlTile(Icons.Filled.Bluetooth, "Bluetooth", true)
                        }
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
                            ControlTile(Icons.Filled.BrightnessHigh, "Brightness", true)
                            ControlTile(Icons.Filled.VolumeUp, "Volume", true)
                        }
                        Button(onClick={ showWallpaperPicker=true; showControlCenter=false }, modifier=Modifier.fillMaxWidth(), shape=RoundedCornerShape(12.dp)){ Text("Change Wallpaper") }
                    }
                }
            }
        }

        // Wallpaper Picker
        if(showWallpaperPicker){
            AlertDialog(onDismissRequest={ showWallpaperPicker=false }, title={Text("Choose Wallpaper - MacOS Style")}, text={
                LazyVerticalGrid(columns=GridCells.Fixed(2)){
                    items(wallpapers.size){ idx ->
                        Box(Modifier.padding(8.dp).height(100.dp).clip(RoundedCornerShape(14.dp)).background(wallpapers[idx]).border(if(wallpaperIndex==idx) 3.dp else 0.dp, if(wallpaperIndex==idx) Color.Blue else Color.Transparent, RoundedCornerShape(14.dp)).clickable{ wallpaperIndex=idx; showWallpaperPicker=false })
                    }
                }
            }, confirmButton={ TextButton(onClick={ showWallpaperPicker=false }){ Text("Close") } })
        }

        // App Long Press Menu
        selectedAppMenu?.let { app ->
            AlertDialog(onDismissRequest={ selectedAppMenu=null }, title={Text(app.label)}, text={
                Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Text("Package: ${app.packageName}", fontSize=12.sp, color=Color.Gray)
                    Button(onClick={ launchApp(context, app); selectedAppMenu=null }, modifier=Modifier.fillMaxWidth()){ Icon(Icons.Filled.Launch, null); Spacer(Modifier.width(8.dp)); Text("Open") }
                    OutlinedButton(onClick={ try{ context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))) }catch(e:Exception){}; selectedAppMenu=null }, modifier=Modifier.fillMaxWidth()){ Icon(Icons.Filled.Info, null); Spacer(Modifier.width(8.dp)); Text("App Info") }
                    OutlinedButton(onClick={ try{ context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))) }catch(e:Exception){}; selectedAppMenu=null }, modifier=Modifier.fillMaxWidth(), colors=ButtonDefaults.outlinedButtonColors(contentColor=Color.Red)){ Icon(Icons.Filled.Delete, null); Spacer(Modifier.width(8.dp)); Text("Uninstall") }
                }
            }, confirmButton={ TextButton(onClick={ selectedAppMenu=null }){ Text("Cancel") } })
        }
    }
}

@Composable
fun DockIcon(app: AppInfo, onClick: ()->Unit, onLongClick: ()->Unit){
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if(pressed) 1.5f else 1f, animationSpec=spring(dampingRatio=0.4f, stiffness=400f))
    Box(Modifier.size(56.dp).scale(scale).clip(RoundedCornerShape(14.dp)).combinedClickable(interactionSource=interaction, indication=null, onClick=onClick, onLongClick=onLongClick)){ AppIcon(app.icon, Modifier.fillMaxSize()) }
}

@Composable
fun AppIcon(icon: Drawable, modifier: Modifier = Modifier){
    val bmp = remember(icon){ try{ icon.toBitmap(128,128).asImageBitmap() }catch(e:Exception){ null } }
    if(bmp!=null){ Image(bitmap=bmp, contentDescription=null, modifier=modifier) } else { Box(modifier=modifier.background(Color.Gray, RoundedCornerShape(14.dp))) }
}

@Composable
fun ControlTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean){
    Row(Modifier.clip(RoundedCornerShape(12.dp)).background(if(active) Color(0xFF0A84FF) else Color.White.copy(alpha=0.2f)).padding(12.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp)){
        Icon(icon, null, tint=Color.White, modifier=Modifier.size(20.dp))
        Text(label, color=Color.White, fontSize=12.sp, fontWeight=FontWeight.Medium)
    }
}
