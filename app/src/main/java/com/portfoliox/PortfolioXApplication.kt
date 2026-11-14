package com.portfoliox

import android.app.Application
import com.portfoliox.di.AppContainer

/**
 * Created by Android once, before any screen, and alive as long as the app process.
 * That makes it the right owner for app-wide singletons such as the database.
 * Registered in AndroidManifest.xml via android:name.
 */
class PortfolioXApplication : Application() {

    // lateinit: assigned in onCreate rather than the constructor, but guaranteed set before use.
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
