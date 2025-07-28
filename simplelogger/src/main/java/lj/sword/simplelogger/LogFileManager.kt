package lj.sword.simplelogger

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 文件日志管理器 - 整合版本
 * 与现有SimpleLogger架构完美融合
 */
internal class LogFileManager private constructor(
    private val context: Context,
    private val config: FileLogConfig = FileLogConfig()
) {

    companion object {
        private const val TAG = "SimpleLogger"

        @Volatile
        private var INSTANCE: LogFileManager? = null

        fun getInstance(context: Context, config: FileLogConfig = FileLogConfig()): LogFileManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LogFileManager(context.applicationContext, config).also { INSTANCE = it }
            }
        }

        /**
         * 重置单例实例（用于配置更新）
         */
        internal fun resetInstance() {
            synchronized(this) {
                INSTANCE?.release()
                INSTANCE = null
            }
        }
    }



    // 异步处理相关
    private val logQueue = ConcurrentLinkedQueue<String>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 缓存当前日志文件和目录
    @Volatile
    private var currentLogFile: File? = null
    @Volatile
    private var logDir: File? = null

    // 是否已释放资源
    @Volatile
    private var isReleased = false

    init {
        startLogProcessing()
    }

    /**
     * 异步添加日志
     */
    fun appendLogAsync(message: String) {
        if (isReleased) return
        logQueue.offer(message)
    }

    /**
     * 同步添加日志（兼容原有接口）
     */
    @Synchronized
    fun appendLog(message: String) {
        if (isReleased) return

        try {
            val logFile = getOrCreateLogFile()

            if (shouldRotateLog(logFile)) {
                rotateLogFiles()
                currentLogFile = null // 重置缓存
            }

            val targetFile = currentLogFile ?: getOrCreateLogFile()
            writeToFile(targetFile, message)

        } catch (e: Exception) {
            Log.e(TAG, "同步写入日志失败", e)
        }
    }

    /**
     * 启动异步日志处理
     */
    private fun startLogProcessing() {
        coroutineScope.launch {
            val batch = mutableListOf<String>()

            while (isActive && !isReleased) {
                try {
                    // 收集批次日志
                    collectBatch(batch)

                    if (batch.isNotEmpty()) {
                        processBatch(batch)
                        batch.clear()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "异步日志处理失败", e)
                    delay(1000) // 出错时延迟重试
                }
            }
        }
    }

    /**
     * 收集批次日志
     */
    private suspend fun collectBatch(batch: MutableList<String>) {
        val startTime = System.currentTimeMillis()

        // 收集一批日志或等待超时
        while (batch.size < config.batchSize &&
            System.currentTimeMillis() - startTime < config.flushIntervalMs &&
            !isReleased) {

            val message = logQueue.poll()
            if (message != null) {
                batch.add(message)
            } else {
                delay(10) // 短暂等待新日志
            }
        }

        // 如果还有剩余日志，继续收集
        while (logQueue.isNotEmpty() && batch.size < config.batchSize * 2) {
            logQueue.poll()?.let { batch.add(it) }
        }
    }

    /**
     * 处理批次日志
     */
    private suspend fun processBatch(batch: List<String>) {
        if (isReleased) return

        withContext(Dispatchers.IO) {
            try {
                val logFile = getOrCreateLogFile()

                // 检查是否需要轮转
                if (shouldRotateLog(logFile)) {
                    rotateLogFiles()
                    currentLogFile = null // 重置缓存
                }

                val targetFile = currentLogFile ?: getOrCreateLogFile()
                writeBatchToFile(targetFile, batch)

            } catch (e: Exception) {
                Log.e(TAG, "批量写入日志失败", e)
            }
        }
    }

    /**
     * 获取或创建日志文件
     */
    private fun getOrCreateLogFile(): File {
        currentLogFile?.let {
            if (it.exists()) return it
        }

        val dir = getOrCreateLogDir()
        val fileName = if (config.useTimestampInFilename) {
            "${config.filePrefix}${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}${config.fileExtension}"
        } else {
            "${config.filePrefix}0${config.fileExtension}"
        }

        val logFile = File(dir, fileName)
        if (!logFile.exists()) {
            logFile.createNewFile()
        }

        currentLogFile = logFile
        return logFile
    }

    /**
     * 获取或创建日志目录
     */
    private fun getOrCreateLogDir(): File {
        logDir?.let {
            if (it.exists() && it.isDirectory) return it
        }

        val dir = File(context.getExternalFilesDir(null), config.logDirName)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        logDir = dir
        return dir
    }

    /**
     * 判断是否需要轮转日志
     */
    private fun shouldRotateLog(file: File): Boolean {
        return file.length() > config.maxFileSizeBytes
    }

    /**
     * 轮转日志文件
     */
    private fun rotateLogFiles() {
        try {
            val dir = getOrCreateLogDir()
            val files = dir.listFiles { _, name ->
                name.startsWith(config.filePrefix) && name.endsWith(config.fileExtension)
            }?.sortedByDescending { it.lastModified() } ?: return

            // 删除超出数量限制的文件
            if (files.size >= config.maxFileCount) {
                files.drop(config.maxFileCount - 1).forEach { it.delete() }
            }

            // 重命名现有文件
            files.forEachIndexed { index, file ->
                if (index == 0) {
                    val newName = "${config.filePrefix}${System.currentTimeMillis()}${config.fileExtension}"
                    val newFile = File(dir, newName)
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            Files.move(file.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        }else{
                            file.renameTo(newFile)
                        }
                    } catch (e: Exception) {
                        // 如果 Files.move 失败，降级使用 renameTo
                        file.renameTo(newFile)
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "日志轮转失败", e)
        }
    }

    /**
     * 批量写入文件
     */
    private fun writeBatchToFile(file: File, messages: List<String>) {
        BufferedWriter(OutputStreamWriter(FileOutputStream(file, true), "UTF-8")).use { writer ->
            messages.forEach { message ->
                writer.write(message)
                writer.newLine()
            }
            writer.flush()
        }
    }

    /**
     * 写入单个日志条目
     */
    private fun writeToFile(file: File, message: String) {
        BufferedWriter(OutputStreamWriter(FileOutputStream(file, true), "UTF-8")).use { writer ->
            writer.write(message)
            writer.newLine()
            writer.flush()
        }
    }

    /**
     * 强制刷新所有待处理日志
     */
    suspend fun flush() {
        if (isReleased) return

        val remainingLogs = mutableListOf<String>()
        while (logQueue.isNotEmpty()) {
            logQueue.poll()?.let { remainingLogs.add(it) }
        }

        if (remainingLogs.isNotEmpty()) {
            processBatch(remainingLogs)
        }
    }

    /**
     * 清除所有日志文件
     */
    @Synchronized
    fun clearLogs() {
        if (isReleased) return

        try {
            // 先刷新待处理日志
            runBlocking {
                try {
                    flush()
                } catch (e: Exception) {
                    Log.w(TAG, "刷新日志时出错", e)
                }
            }

            val dir = getOrCreateLogDir()
            dir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    file.delete()
                }
            }

            currentLogFile = null
            Log.i(TAG, "所有日志文件已清除")

        } catch (e: Exception) {
            Log.e(TAG, "清除日志失败", e)
        }
    }

    /**
     * 获取日志文件列表
     */
    fun getLogFiles(): List<File> {
        if (isReleased) return emptyList()

        return try {
            val dir = getOrCreateLogDir()
            dir.listFiles { _, name ->
                name.startsWith(config.filePrefix) && name.endsWith(config.fileExtension)
            }?.sortedByDescending { it.lastModified() } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "获取日志文件列表失败", e)
            emptyList()
        }
    }

    /**
     * 获取日志目录大小
     */
    fun getLogDirectorySize(): Long {
        if (isReleased) return 0L

        return try {
            getLogFiles().sumOf { it.length() }
        } catch (e: Exception) {
            Log.e(TAG, "计算日志目录大小失败", e)
            0L
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        if (isReleased) return

        isReleased = true

        runBlocking {
            try {
                flush()
            } catch (e: Exception) {
                Log.e(TAG, "释放资源时刷新日志失败", e)
            }
        }

        coroutineScope.cancel()
        currentLogFile = null
        logDir = null
    }
}