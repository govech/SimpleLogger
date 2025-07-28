# SimpleLogger SDK 使用指南

## 集成
在 app 模块的 build.gradle 中添加依赖：
```groovy
dependencies {
    implementation 'lj.sword:simplelogger:1.0.0'
}
```

## 初始化（在 Application 中）

### 方法一：简洁版初始化
```kotlin
class MyApp : Application() {
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
```

### 方法二：使用LoggerConfig实例初始化
```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val config = LoggerConfig(
            logLevel = LogLevel.DEBUG,
            globalTag = "MyApp",
            isEnabled = true,
            fileLogging = BuildConfig.DEBUG // 仅在Debug时记录文件
        )
        
        SimpleLogger.initialize(this, config)
    }
}
```

### 方法三：自定义文件日志配置
```kotlin

val fileConfig = FileLogConfig(
    maxFileSizeBytes = 5 * 1024 * 1024, // 5MB
    maxFileCount = 10,
    batchSize = 20,
    enableAsyncLogging = true,
    useTimestampInFilename = true
)

val loggerConfig = LoggerConfig(
    logLevel = LogLevel.INFO,
    globalTag = "MyApp",
    enableFileLogging = true,
    fileLogConfig = fileConfig
)

SimpleLogger.initialize(this, loggerConfig)
```

## 基本用法
```kotlin
// 带自定义标签
SimpleLogger.d("调试信息", "NetworkModule")

// 使用全局标签
SimpleLogger.i("应用已启动")

// 记录异常
try {
    // 可能抛出异常的代码
} catch (e: Exception) {
    SimpleLogger.e(e, "发生错误")
}

// 清除日志文件
SimpleLogger.clearLogFiles()
```

## 日志文件
日志文件存储在外部存储的 `Android/data/your.package.name/files/simple_logger/` 目录


## 添加 ProGuard 混淆规则
-keep class lj.sword.simplelogger.SimpleLogger { public *; }
-keep class lj.sword.simplelogger.LoggerConfig { *; }
-keep class lj.sword.simplelogger.LogLevel { public *; }