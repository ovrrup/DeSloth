package com.desloth.app

import android.app.Application
import com.desloth.app.di.AppContainer
import com.desloth.app.di.DefaultAppContainer

class DeSlothApp : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        appContainer = DefaultAppContainer(this)
    }

    companion object {
        lateinit var instance: DeSlothApp
            private set
    }
}
