package com.fouad.macoslauncher
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text("MacOS Launcher v4 - GitHub Build Success!", modifier = Modifier.padding(32.dp))
                }
            }
        }
    }
}
