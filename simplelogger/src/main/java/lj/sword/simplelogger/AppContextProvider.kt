package lj.sword.simplelogger

import android.app.Application
import android.content.Context

// 获取全局Application Context的工具
object AppContextProvider {
    @Volatile
    private var application: Application? = null

    val applicationContext: Context
        get() = checkNotNull(application) { "AppContextProvider not initialized" }.applicationContext

    fun initialize(context: Context) {
        if (application == null && context is Application) {
            synchronized(this) {
                if (application == null) {
                    application = context
                }
            }
        }
    }
}