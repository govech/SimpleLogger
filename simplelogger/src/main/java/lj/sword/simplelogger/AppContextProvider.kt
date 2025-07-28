package lj.sword.simplelogger

import android.app.Application
import android.content.Context

// 获取全局Application Context的工具
object AppContextProvider {
    private var application: Application? = null
    
    val applicationContext: Context?
        get() = application?.applicationContext
    
    fun initialize(context: Context) {
        if (application == null && context is Application) {
            application = context
        }
    }
}