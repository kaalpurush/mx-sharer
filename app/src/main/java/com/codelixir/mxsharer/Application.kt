package com.codelixir.mxsharer

import android.content.Context

class Application : android.app.Application() {

    companion object {
        var context: Context? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }

}
