package lj.sword.logtest

import android.app.Application
import lj.sword.simplelogger.LogLevel
import lj.sword.simplelogger.SimpleLogger

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SimpleLogger.initialize(
            context = this,
            level = LogLevel.DEBUG,
            tag = "MyApp",
            enable = true,
            enableFileLogging = BuildConfig.DEBUG // 仅在Debug时记录文件
        )
    }
}