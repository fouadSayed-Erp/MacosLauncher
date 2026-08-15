package com.maclauncher.core.engine
 
import javax.inject.Inject
import javax.inject.Singleton
 
class="text-[#D7BA7D]">@Singleton
class WindowManagerEngine class="text-[#D7BA7D]">@Inject constructor() {
    fun minimizeWindow(windowId: String) { /* ... */ }
    fun maximizeWindow(windowId: String) { /* ... */ }
    fun closeWindow(windowId: String) { /* ... */ }
    fun snapToLeft(windowId: String) { /* ... */ }
    fun snapToRight(windowId: String) { /* ... */ }
}
 