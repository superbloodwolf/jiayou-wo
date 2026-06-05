package com.fueltracker

import android.app.Application
import com.fueltracker.data.AppDatabase
import java.util.Locale

/**
 * Application 类
 * 初始化全局数据库实例，固定中文环境
 */
class FuelTrackerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // 固定中文 locale，确保日期/数字格式统一
        Locale.setDefault(Locale.CHINA)
        // 预初始化数据库（在首屏前完成）
        AppDatabase.getInstance(applicationContext)
    }
}
