# SimpleLogger SDK 使用指南 ![](https://jitpack.io/v/govech/simplelogger.svg)

## 概述
    SimpleLogger: 是一款专为 Android 平台设计的轻量级日志记录工具库，提供 多级别日志输出、灵活配置和可扩展的日志存储能力。核心设计理念是：
    - 简洁易用：通过 DSL 风格配置降低集成门槛
    - 高性能：基于 Kotlin 协程优化异步日志处理
    - 可观测性：支持日志分级和上下文追踪
    - 生产就绪：完善的异常处理和线程安全机制

## 集成

在根目录的 settings.gradle 中添加：

```groovy
dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		mavenCentral()
		maven { url 'https://jitpack.io' }
	}
}
```

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
- -keep class lj.sword.simplelogger.SimpleLogger { public *; }
- -keep class lj.sword.simplelogger.LoggerConfig { *; }
- -keep class lj.sword.simplelogger.LogLevel { public *; }