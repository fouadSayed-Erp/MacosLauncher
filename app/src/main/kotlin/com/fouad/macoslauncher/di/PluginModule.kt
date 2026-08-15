package com.maclauncher.di
 
import com.maclauncher.core.plugin.LauncherPlugin
import com.maclauncher.plugins.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
 
class="text-[#D7BA7D]">@Module
class="text-[#D7BA7D]">@InstallIn(SingletonComponent::class)
abstract class PluginModule {
 
    class="text-[#D7BA7D]">@Binds class="text-[#D7BA7D]">@IntoSet
    abstract fun bindAppSearch(plugin: AppSearchPlugin): LauncherPlugin
 
    class="text-[#D7BA7D]">@Binds class="text-[#D7BA7D]">@IntoSet
    abstract fun bindCalculator(plugin: CalculatorPlugin): LauncherPlugin
 
    class="text-[#D7BA7D]">@Binds class="text-[#D7BA7D]">@IntoSet
    abstract fun bindSystem(plugin: SystemPlugin): LauncherPlugin
 
    class="text-[#D7BA7D]">@Binds class="text-[#D7BA7D]">@IntoSet
    abstract fun bindClipboard(plugin: ClipboardPlugin): LauncherPlugin
}
 