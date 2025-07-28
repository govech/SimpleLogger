package lj.sword.simplelogger

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SimpleLogger {
    // 日志配置
    private var config: LoggerConfig = LoggerConfig.DEFAULT

    // 文件日志管理器实例
    @Volatile
    private var fileManager: LogFileManager? = null

    // SDK初始化方法 - 使用LoggerConfig实例
    @Synchronized
    fun initialize(context: Context, config: LoggerConfig) {
        // 配置Context提供者
        AppContextProvider.initialize(context.applicationContext)
        // 保存配置
        this.config = config

        // 初始化文件日志管理器
        if (config.enableFileLogging) {
            initializeFileManager(context)
        }

        // 首次初始化日志
        d("SimpleLogger initialized $config")
    }

    // SDK初始化方法 - 使用配置参数（兼容旧版本）
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

    // SDK初始化方法 - 完整配置参数
    @Synchronized
    fun initialize(
        context: Context,
        level: LogLevel = LogLevel.DEBUG,
        tag: String = "SimpleLogger",
        enable: Boolean = true,
        enableFileLogging: Boolean = false,
        fileLogConfig: FileLogConfig = FileLogConfig.DEFAULT
    ) {
        initialize(context, LoggerConfig(level, enable, tag, enableFileLogging, fileLogConfig))
    }

    /**
     * 初始化文件日志管理器
     */
    private fun initializeFileManager(context: Context) {
        try {
            fileManager = LogFileManager.getInstance(context, config.fileLogConfig)
        } catch (e: Exception) {
            Log.e(config.globalTag, "Failed to initialize file manager", e)
        }
    }

    /**
     * 运行时更新配置
     */
    @Synchronized
    fun updateConfig(newConfig: LoggerConfig) {
        val oldConfig = config
        config = newConfig

        // 如果文件日志配置发生变化，重新初始化文件管理器
        if (oldConfig.enableFileLogging != newConfig.enableFileLogging ||
            oldConfig.fileLogConfig != newConfig.fileLogConfig) {

            val context = AppContextProvider.applicationContext
            if (context != null && newConfig.enableFileLogging) {
                initializeFileManager(context)
            } else if (!newConfig.enableFileLogging) {
                fileManager?.release()
                fileManager = null
            }
        }

        i("Logger configuration updated")
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

    // 带格式化参数的日志方法
    fun d(tag: String? = null, message: String, vararg args: Any?) =
        log(LogLevel.DEBUG, tag, message.format(*args))

    fun i(tag: String? = null, message: String, vararg args: Any?) =
        log(LogLevel.INFO, tag, message.format(*args))

    fun w(tag: String? = null, message: String, vararg args: Any?) =
        log(LogLevel.WARNING, tag, message.format(*args))

    fun e(tag: String? = null, message: String, vararg args: Any?) =
        log(LogLevel.ERROR, tag, message.format(*args))

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

        // 控制台输出
        outputToConsole(level, tag, fullMessage)

        // 文件日志输出
        if (config.enableFileLogging) {
            outputToFile(fullMessage)
        }
    }

    /**
     * 输出到控制台
     */
    private fun outputToConsole(level: LogLevel, tag: String, message: String) {
        when (level) {
            LogLevel.ERROR -> Log.e(tag, message)
            LogLevel.WARNING -> Log.w(tag, message)
            LogLevel.INFO -> Log.i(tag, message)
            LogLevel.DEBUG -> Log.d(tag, message)
            LogLevel.VERBOSE -> Log.v(tag, message)
            else -> {}
        }
    }

    /**
     * 输出到文件
     */
    private fun outputToFile(message: String) {
        try {
            val manager = fileManager
            if (manager != null) {
                if (config.fileLogConfig.enableAsyncLogging) {
                    manager.appendLogAsync(message)
                } else {
                    manager.appendLog(message)
                }
            } else {
                // 降级处理：使用简单的文件写入
                val context = AppContextProvider.applicationContext ?: return
                LogFileManager.getInstance(context, config.fileLogConfig)
                    .appendLog(message)
            }
        } catch (e: Exception) {
            Log.e(config.globalTag, "Failed to write log to file", e)
        }
    }

    // 格式化日志信息
    private fun formatMessage(level: LogLevel, message: String): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val threadName = Thread.currentThread().name
        return "[${timestamp}] [${level.name}] [${threadName}] - $message"
    }

    // 文件日志管理方法
    fun clearLogFiles() {
        try {
            fileManager?.clearLogs() ?: run {
                val context = AppContextProvider.applicationContext ?: return
                LogFileManager.getInstance(context, config.fileLogConfig).clearLogs()
            }
            i("Log files cleared")
        } catch (e: Exception) {
            Log.e(config.globalTag, "Failed to clear log files", e)
        }
    }

    /**
     * 获取日志文件列表
     */
    fun getLogFiles(): List<java.io.File> {
        return try {
            fileManager?.getLogFiles() ?: run {
                val context = AppContextProvider.applicationContext ?: return emptyList()
                LogFileManager.getInstance(context, config.fileLogConfig).getLogFiles()
            }
        } catch (e: Exception) {
            Log.e(config.globalTag, "Failed to get log files", e)
            emptyList()
        }
    }

    /**
     * 获取日志目录总大小
     */
    fun getLogDirectorySize(): Long {
        return try {
            fileManager?.getLogDirectorySize() ?: run {
                val context = AppContextProvider.applicationContext ?: return 0L
                LogFileManager.getInstance(context, config.fileLogConfig).getLogDirectorySize()
            }
        } catch (e: Exception) {
            Log.e(config.globalTag, "Failed to get log directory size", e)
            0L
        }
    }

    /**
     * 强制刷新所有待处理日志
     */
    suspend fun flushLogs() {
        try {
            fileManager?.flush()
        } catch (e: Exception) {
            Log.e(config.globalTag, "Failed to flush logs", e)
        }
    }

    /**
     * 获取当前配置
     */
    fun getConfig(): LoggerConfig = config

    /**
     * 释放资源（应用退出时调用）
     */
    fun release() {
        try {
            fileManager?.release()
            fileManager = null
            i("SimpleLogger released")
        } catch (e: Exception) {
            Log.e(config.globalTag, "Failed to release SimpleLogger", e)
        }
    }
}

