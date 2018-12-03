package com.example.sajin.aot_cam

import android.app.Application
import android.util.Log
import io.reactivex.plugins.RxJavaPlugins



class AotApp: Application() {
    override fun onCreate() {
        super.onCreate()
        RxJavaPlugins.setErrorHandler { throwable ->
            Log.d("AppError",throwable.message)
        } // nothing or some logging
    }
}