package com.backcam.recorder

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BackCamApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 初始化Hilt依赖注入
    }
}