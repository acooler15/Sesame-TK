# ---------- 全局策略 ----------
# 目标是缩减体积（shrink 删除无用代码），不做混淆改名：
# 类名/成员名原样保留，崩溃栈直接可读，按名反射（配置键/JSON 键/TypeReference 签名）天然安全
-dontobfuscate
# 保留源文件名与行号，配合 -dontobfuscate 使崩溃栈完整可读
-keepattributes SourceFile,LineNumberTable

# ---------- 框架 ----------
-keep class de.robv.android.xposed.** { *; }
-keep class io.github.libxposed.service.** { *; }
-dontwarn io.github.libxposed.service.**

# ---------- Shizuku ----------
-keep class dev.rikka.shizuku.** { *; }
-dontwarn dev.rikka.shizuku.**

# ---------- cmd-android ----------
-keep class com.niki.** { *; }
-dontwarn com.niki.**

# ---------- 日志 ----------
-keep class ch.qos.logback.** { *; }
-keep class org.slf4j.** { *; }
-dontwarn ch.qos.logback.**, org.slf4j.**

# ---------- 本工程 ----------

# Hook 入口（legacy 框架按 assets/xposed_init 声明反射加载）
-keep class fansirsqi.xposed.sesame.hook.entry.legacy.** { *; }
# libxposed service 入口（保守保留）
-keep class fansirsqi.xposed.sesame.hook.entry.modern.** { *; }

# ---------- libxposed 现代路径（照官方 example 规则） ----------
-dontwarn io.github.libxposed.annotation.**
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}

# 配置体系（混淆将导致用户配置丢失，最高风险）
# 1) 配置文件以模型类简单名为键（ModelConfig.code = javaClass.simpleName）
-keepnames class * extends fansirsqi.xposed.sesame.model.Model
# 2) Model 实例化走反射无参构造（Model.initAllModel）
-keepclassmembers class * extends fansirsqi.xposed.sesame.model.Model {
    public <init>();
}
# 3) 配置 JSON 键名来自 getter 名（modelFieldsMap / value），加载侧手动按名解析
-keep class fansirsqi.xposed.sesame.data.Config { *; }
-keep class fansirsqi.xposed.sesame.model.ModelFields { *; }
-keep class fansirsqi.xposed.sesame.model.ModelField { *; }
-keep class * extends fansirsqi.xposed.sesame.model.ModelField { *; }

# Jackson 数据绑定实体（DataStore TypeReference 反序列化，字段名敏感）
-keep class fansirsqi.xposed.sesame.entity.** { *; }

# 反射实例化的无参构造器（IdMaps / IdMapManager 子类）
-keepclassmembers class * extends fansirsqi.xposed.sesame.util.maps.IdMaps {
    public <init>();
}
-keepclassmembers class * extends fansirsqi.xposed.sesame.util.maps.IdMapManager {
    public <init>();
}

# Kotlin 元数据（jackson-module-kotlin 依赖 Kotlin 反射读取 @Metadata）
# 注：注解属性已由现有 Jackson 段的 -keepattributes Signature, *Annotation* 覆盖，无需重复声明
-keepclassmembers,allowobfuscation class * {
    @kotlin.Metadata *;
}

# ---------- Jackson（最小必要） ----------
-keep class com.fasterxml.jackson.** { *; }
-keepattributes Signature, *Annotation*
# TypeReference 子类（含匿名类）依赖 getGenericSuperclass() 读取泛型签名，
# R8 类合并优化会破坏签名导致运行时抛 "constructed without actual type information"
-keep class * extends com.fasterxml.jackson.core.type.TypeReference
-keepclassmembers class * {
    @com.fasterxml.jackson.annotation.** *;
}

# ---------- 序列化 & 缺失类 ----------
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable { *; }
-dontwarn java.beans.ConstructorProperties, java.beans.Transient

# tensorflow
-dontwarn org.tensorflow.lite.gpu.GpuDelegateFactory$Options$GpuBackend
-dontwarn org.tensorflow.lite.gpu.GpuDelegateFactory$Options