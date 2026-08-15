package com.fouad.macoslauncher
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize().padding(32.dp)) {
                    Text("MacOS Launcher - GitHub Build Fixed! ✅")
                }
            }
        }
    }
}
