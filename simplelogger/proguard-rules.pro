# 添加R8建议的规则以抑制警告
-dontwarn java.lang.invoke.StringConcatFactory
# 保持公共API不被混淆
-keep class lj.sword.simplelogger.SimpleLogger { public *; }
-keep class lj.sword.simplelogger.LoggerConfig { *; }
-keep class lj.sword.simplelogger.LogLevel { public *; }