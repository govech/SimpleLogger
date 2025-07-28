package lj.sword.simplelogger

data class LoggerConfig(
    val logLevel: LogLevel = LogLevel.DEBUG,
    val isEnabled: Boolean = true,
    val globalTag: String = "SimpleLogger",
    val enableFileLogging: Boolean = false
) {
    companion object {
        // 默认配置
        val DEFAULT = LoggerConfig()
    }
}