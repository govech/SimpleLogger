package lj.sword.simplelogger

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SimpleLogger {
    // 日志配置
    private var config: LoggerConfig = LoggerConfig.DEFAULT
    
    // SDK初始化方法 - 使用LoggerConfig实例
    @Synchronized
    fun initialize(context: Context, config: LoggerConfig) {
        // 配置Context提供者
        AppContextProvider.initialize(context.applicationContext)
        
        // 保存配置
        this.config = config
        // 首次初始化日志
        d("SimpleLogger initialized $config")
    }
    
    // SDK初始化方法 - 使用配置参数
    @Synchronized
    fun initialize(
        context: Context,
        level: LogLevel = LogLevel.DEBUG,
        tag: String = "SimpleLogger",
        enable: Boolean = true,
        enableFileLogging: Boolean = false
    ) {
        initialize(context, LoggerConfig(level, enable, tag, enableFileLogging))
    }


    // 公共API - 最常用方法
    fun d(message: String, tag: String? = null) = log(LogLevel.DEBUG, tag, message)
    fun i(message: String, tag: String? = null) = log(LogLevel.INFO, tag, message)
    fun w(message: String, tag: String? = null) = log(LogLevel.WARNING, tag, message)
    fun e(message: String, tag: String? = null) = log(LogLevel.ERROR, tag, message)
    fun v(message: String, tag: String? = null) = log(LogLevel.VERBOSE, tag, message)

    // 带异常信息的日志
    fun e(throwable: Throwable, message: String, tag: String? = null) {
        log(LogLevel.ERROR, tag, "$message\n${throwable.stackTraceToString()}")
    }

    // 核心日志方法
    @Synchronized
    private fun log(level: LogLevel, customTag: String?, message: String) {
        // 检查是否启用日志
        if (!config.isEnabled || level.value < config.logLevel.value) {
            return
        }

        // 确定使用的标签
        val tag = customTag ?: config.globalTag
        val fullMessage = formatMessage(level, message)


        when (level) {
            LogLevel.ERROR -> Log.e(tag, fullMessage)
            LogLevel.WARNING -> Log.w(tag, fullMessage)
            LogLevel.INFO -> Log.i(tag, fullMessage)
            LogLevel.DEBUG -> Log.d(tag, fullMessage)
            LogLevel.VERBOSE -> Log.v(tag, fullMessage)
            else -> {}
        }


        // 如果需要文件日志
        if (config.enableFileLogging) {
            saveToFile(fullMessage)
        }
    }

    // 格式化日志信息
    private fun formatMessage(level: LogLevel, message: String): String {
        val timestamp =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val threadName = Thread.currentThread().name
        return "[${timestamp}] [${level.name}] [${threadName}] - $message"
    }

    // 文件日志实现
    private fun saveToFile(message: String) {
        // 实现放在下一步
        // 需要Context，使用AppContextProvider
        val context = AppContextProvider.applicationContext ?: return
        LogFileManager.appendLog(context, message)
    }

    // 添加清除日志方法
    fun clearLogFiles() {
        val context = AppContextProvider.applicationContext ?: return
        LogFileManager.clearLogs(context)
    }
}