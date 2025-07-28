package lj.sword.simplelogger

import android.content.Context
import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

// 文件日志管理器
internal object LogFileManager {
    private const val MAX_LOG_SIZE_BYTES = 1024 * 1024 // 1MB
    private const val LOG_DIR_NAME = "simple_logger"
    private const val LOG_FILE_PREFIX = "app_log_"
    private const val LOG_FILE_EXTENSION = ".txt"
    
    // 获取日志文件
    @Synchronized
    fun appendLog(context: Context, message: String) {
        try {
            val logFile = getOrCreateLogFile(context)
            
            if (logFile.length() > MAX_LOG_SIZE_BYTES) {
                rollLogFile(logFile)
                writeToFile(getOrCreateLogFile(context), message)
            } else {
                writeToFile(logFile, message)
            }
        } catch (e: Exception) {
            Log.e("SimpleLogger", "File logging failed", e)
        }
    }
    
    private fun getOrCreateLogFile(context: Context): File {
        // 创建日志目录
        val logDir = File(context.getExternalFilesDir(null), LOG_DIR_NAME)
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        
        // 获取当前日志文件
        val logFile = File(logDir, "$LOG_FILE_PREFIX${0}$LOG_FILE_EXTENSION")
        
        if (!logFile.exists()) {
            logFile.createNewFile()
        }
        
        return logFile
    }
    
    private fun rollLogFile(currentFile: File) {
        // 简单的日志轮转
        val index = getFileIndex(currentFile.name)
        val newName = "${LOG_FILE_PREFIX}${index + 1}$LOG_FILE_EXTENSION"
        val newFile = File(currentFile.parent, newName)
        
        if (newFile.exists()) {
            newFile.delete()
        }
        
        currentFile.renameTo(newFile)
    }
    
    private fun getFileIndex(fileName: String): Int {
        val regex = "$LOG_FILE_PREFIX(\\d+)$LOG_FILE_EXTENSION".toRegex()
        val matchResult = regex.find(fileName)
        return matchResult?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }
    
    private fun writeToFile(file: File, message: String) {
        FileOutputStream(file, true).use { fos ->
            BufferedWriter(OutputStreamWriter(fos)).use { writer ->
                writer.write(message)
                writer.newLine()
            }
        }
    }
    
    // 公共方法：清除所有日志
    @Synchronized
    fun clearLogs(context: Context) {
        val logDir = File(context.getExternalFilesDir(null), LOG_DIR_NAME)
        if (logDir.exists() && logDir.isDirectory) {
            logDir.listFiles()?.forEach { it.delete() }
        }
    }
}