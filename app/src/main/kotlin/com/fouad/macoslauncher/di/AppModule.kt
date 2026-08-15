package com.maclauncher.di
 
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
 
class="text-[#D7BA7D]">@Module
class="text-[#D7BA7D]">@InstallIn(SingletonComponent::class)
object AppModule {
 
    class="text-[#D7BA7D]">@Provides class="text-[#D7BA7D]">@Singleton
    fun provideContext(class="text-[#D7BA7D]">@ApplicationContext ctx: Context): Context = ctx
}
 