package com.maclauncher.utils
 
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
 
fun Modifier.macShadow() = this.drawBehind {
    // Custom shadow with blur
}
 
fun String.capitalize(): String = replaceFirstChar { it.uppercase() }
 