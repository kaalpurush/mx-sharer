package com.codelixir.mxsharer

import android.content.Context

class Application : android.app.Application() {

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }

    companion object {
        var context: Context? = null
            private set
    }

}
