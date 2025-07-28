package lj.sword.simplelogger

/**
 * SimpleLogger配置类
 *
 * 用于配置SimpleLogger的行为，包括日志级别、是否启用、全局标签以及文件日志相关配置
 *
 * @property logLevel 日志级别，默认为DEBUG
 * @property isEnabled 是否启用日志，默认为true
 * @property globalTag 全局日志标签，默认为"SimpleLogger"
 * @property enableFileLogging 是否启用文件日志记录，默认为false
 * @property fileLogConfig 文件日志配置，默认为FileLogConfig.DEFAULT
 */
data class LoggerConfig(
    val logLevel: LogLevel = LogLevel.DEBUG,
    val isEnabled: Boolean = true,
    val globalTag: String = "SimpleLogger",
    val enableFileLogging: Boolean = false,
    // 新增文件日志配置
    val fileLogConfig: FileLogConfig = FileLogConfig()
) {
    companion object {
        // 默认配置
        val DEFAULT = LoggerConfig()
    }
}

/**
 * 文件日志配置类
 *
 * 用于配置文件日志的具体参数，如文件大小限制、文件数量限制等
 *
 * @property maxFileSizeBytes 单个日志文件最大大小（字节），默认2MB
 * @property maxFileCount 最大日志文件数量，默认5个
 * @property logDirName 日志目录名称，默认"simple_logger"
 * @property filePrefix 日志文件前缀，默认"app_log_"
 * @property fileExtension 日志文件扩展名，默认".txt"
 * @property batchSize 批量写入日志的批次大小，默认10条
 * @property flushIntervalMs 日志刷新间隔（毫秒），默认1000ms
 * @property useTimestampInFilename 是否在文件名中使用时间戳，默认false
 * @property enableAsyncLogging 是否启用异步日志记录，默认true
 */
data class FileLogConfig(
    val maxFileSizeBytes: Long = 2 * 1024 * 1024, // 2MB
    val maxFileCount: Int = 5,
    val logDirName: String = "simple_logger",
    val filePrefix: String = "app_log_",
    val fileExtension: String = ".txt",
    val batchSize: Int = 10,
    val flushIntervalMs: Long = 1000,
    val useTimestampInFilename: Boolean = false,
    val enableAsyncLogging: Boolean = true,
) {
    companion object {
        val DEFAULT = FileLogConfig()
    }
}
