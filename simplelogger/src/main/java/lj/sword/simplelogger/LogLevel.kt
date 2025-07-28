package lj.sword.simplelogger

/**
 *  日志级别
 */
enum class LogLevel(val value: Int) {
    VERBOSE(0),
    DEBUG(1),
    INFO(2),
    WARNING(3),
    ERROR(4),
    NONE(5) // 用于完全禁用日志
}