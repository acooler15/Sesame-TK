import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties
import java.util.TimeZone

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}
var isCIBuild: Boolean = System.getenv("CI").toBoolean()

//isCIBuild = true // 没有c++源码时开启CI构建, push前关闭

val appVersionName = "0.9.9"

// 构建时间戳（GMT+8），用于产物文件名与 BuildConfig，区分每次 debug/release 构建。
// 只保留年份后两位，格式如 2608230832（yyMMddHHmm）。放在顶层，配置期求值一次。
val buildDateTime = SimpleDateFormat("yyMMddHHmm", Locale.CHINA).apply {
    timeZone = TimeZone.getTimeZone("GMT+8")
}.format(Date())

// ============ 构建物签名配置 ============
// 签名信息来源（优先级从高到低）：
//   1. 环境变量（CI 构建传入 secrets：ANDROID_KEYSTORE_PATH / ANDROID_KEYSTORE_PASSWORD / ANDROID_KEY_ALIAS / ANDROID_KEY_PASSWORD）
//   2. keystore.properties（本地开发，不入库，放项目根目录）
//   3. 项目根目录下的 keystore.jks（若存在）
// 缺少完整签名信息时 release 自动回退到 debug 签名，保证构建不中断。
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

fun signingSecret(envName: String, propName: String): String? =
    System.getenv(envName) ?: keystoreProperties.getProperty(propName)

val keystoreFile: File? = signingSecret("ANDROID_KEYSTORE_PATH", "storeFile")?.let { path ->
    val f = File(path)
    if (f.isAbsolute) f else rootProject.file(path)
} ?: rootProject.file("keystore.jks").takeIf { it.isFile }

val hasReleaseSigning = keystoreFile != null &&
    !signingSecret("ANDROID_KEYSTORE_PASSWORD", "storePassword").isNullOrBlank() &&
    !signingSecret("ANDROID_KEY_ALIAS", "keyAlias").isNullOrBlank() &&
    !signingSecret("ANDROID_KEY_PASSWORD", "keyPassword").isNullOrBlank()

android {
    namespace = "fansirsqi.xposed.sesame"
    compileSdk = 37
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            // 双入口共存：合并依赖携带的 META-INF/xposed 声明，避免冲突（照 libxposed/example）
            merges += "META-INF/xposed/*"
        }
        splits {
            abi {
                isEnable = true
                reset()
                include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
                isUniversalApk = true
            }
        }

    }
    // 使用providers API来支持配置缓存
    val gitCommitCount: Int = providers.exec {
        commandLine("git", "rev-list", "--count", "HEAD")
    }.standardOutput.asText.get().trim().toIntOrNull() ?: 1
    defaultConfig {
        vectorDrawables.useSupportLibrary = true
        applicationId = "fansirsqi.xposed.sesame"
        minSdk = 26
        targetSdk = 36

        val buildDate = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).apply {
            timeZone = TimeZone.getTimeZone("GMT+8")
        }.format(Date())

        val buildTime = SimpleDateFormat("HH:mm:ss", Locale.CHINA).apply {
            timeZone = TimeZone.getTimeZone("GMT+8")
        }.format(Date())

        versionCode = gitCommitCount
        versionName = appVersionName

        buildConfigField("String", "BUILD_DATE", "\"$buildDate\"")
        buildConfigField("String", "BUILD_TIME", "\"$buildTime\"")
        buildConfigField("String", "BUILD_DATETIME", "\"$buildDateTime\"")
        if (isCIBuild) {
            ndk {
                abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
            }
        }

        testOptions {
            unitTests.all {
                it.enabled = false
            }
        }
    }



    buildFeatures {
        viewBinding = false
        buildConfig = true
        compose = true
        aidl = true
        mlModelBinding = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = false//关闭脱糖
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        getByName("debug") {
        }
        if (hasReleaseSigning) {
            create("release") {
                storeFile = requireNotNull(keystoreFile) { "keystore 文件未找到" }
                storePassword = signingSecret("ANDROID_KEYSTORE_PASSWORD", "storePassword")
                keyAlias = signingSecret("ANDROID_KEY_ALIAS", "keyAlias")
                keyPassword = signingSecret("ANDROID_KEY_PASSWORD", "keyPassword")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // debug 与 release 使用同一正式签名，便于互相覆盖安装；无签名配置时回退默认 debug 签名
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // 有正式签名配置时使用 release 签名，否则回退 debug 签名
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    sourceSets {
        getByName("main") {
            jniLibs.directories.add("src/main/jniLibs")
        }
    }
    val cmakeFile = file("src/main/cpp/CMakeLists.txt")
    if (!isCIBuild && cmakeFile.exists()) {
        externalNativeBuild {
            cmake {
                path = cmakeFile
//                version = "4.1.2"  //不要随意改这个了答应我
                ndkVersion = "29.0.14206865" //这个也是 答应我就这样吧
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

androidComponents {
    onVariants { variant ->
        val variantName = variant.name
        val variantCap = variantName.replaceFirstChar { if (it.isLowerCase()) it.uppercaseChar() else it }
        val abiNames = variant.outputs.map { out ->
            out.filters
                .firstOrNull { it.filterType == com.android.build.api.variant.FilterConfiguration.FilterType.ABI }
                ?.identifier ?: "universal"
        }
        // 配置期只写稳定的基础名（不含时间戳）。
        // 原因：项目开启了 org.gradle.configuration-cache，配置期求值的 buildDateTime 会被冻结到缓存创建时刻，
        // 导致后续重建文件名时间戳不刷新。因此时间戳改由下方的 rename 任务在执行期注入。
        variant.outputs.forEach { output ->
            val abiName = output.filters
                .firstOrNull { it.filterType == com.android.build.api.variant.FilterConfiguration.FilterType.ABI }
                ?.identifier ?: "universal"
            output.outputFileName.set("Sesame-TK-${abiName}-${variantName}.apk")
        }
        // 执行期任务：在 assembleXxx 收尾时把真实构建时刻（GMT+8, yyMMddHHmm）追加到文件名。
        // 写在 doLast 中意味着每次构建都会重新求值 Date()，规避配置缓存的冻结问题。
        val renameTask = tasks.register("rename${variantCap}Apk") {
            doLast {
                val ts = SimpleDateFormat("yyMMddHHmm", Locale.CHINA).apply {
                    timeZone = TimeZone.getTimeZone("GMT+8")
                }.format(Date())
                val apkDir = File(project.layout.buildDirectory.get().asFile, "outputs/apk/${variantName}")
                abiNames.forEach { abi ->
                    val src = File(apkDir, "Sesame-TK-${abi}-${variantName}.apk")
                    if (src.exists()) {
                        val dst = File(apkDir, "Sesame-TK-${abi}-${variantName}-${ts}.apk")
                        if (src.renameTo(dst)) {
                            logger.lifecycle("APK 重命名: ${dst.name}")
                        } else {
                            logger.warn("APK 重命名失败(可能被占用): ${src.name}")
                        }
                    }
                }
            }
        }
        tasks.named("assemble${variantCap}") { finalizedBy(renameTask) }
    }
}

dependencies {
    // Shizuku 相关依赖 - 用于获取系统级权限
    implementation(libs.rikka.shizuku.api)        // Shizuku API
    implementation(libs.rikka.shizuku.provider)   // Shizuku 提供者
//    implementation(libs.rikka.hidden.stub)
    // implementation(libs.ui.tooling.preview.android)
    implementation(libs.cmd.android)
    implementation(libs.bundles.litert)

    // Compose 相关依赖 - 现代化 UI 框架
    val composeBom = platform(libs.androidx.compose.bom)  // Compose BOM 版本管理
    implementation(composeBom)
    implementation(libs.androidx.material3)                // Material 3 设计组件
    implementation(libs.androidx.material.icons.extended)         // Material 3 图标
    implementation(libs.androidx.ui.text.google.fonts)     // Google Fonts 支持（版本由 BOM 管理）

    // 生命周期
    implementation(libs.androidx.lifecycle.viewmodel.compose) // Compose ViewModel 支持

    // Kotlin 协程依赖 - 异步编程（纯协程调度）
    implementation(libs.kotlinx.coroutines.core)     // 协程核心库
    implementation(libs.kotlinx.coroutines.android)  // Android 协程支持

    // HTTP 服务
    implementation(libs.nanohttpd)                   // 轻量级 HTTP 服务器

    // UI 布局和组件
    implementation(libs.activity.compose)           // Compose Activity 支持

    // Android 核心库
    implementation(libs.core.ktx)                   // Android KTX 核心扩展
    implementation(libs.slf4j.api)                  // SLF4J 日志 API
    implementation(libs.logback.android)            // Logback Android 日志实现

    // 仅编译时依赖 - Xposed 相关
    compileOnly(files("libs/api-82.jar"))          // Xposed API 82（legacy 后端编译需要，运行时由旧框架提供）
    compileOnly(libs.libxposed.api)                // libxposed API 102（框架运行时提供）
    implementation(libs.libxposed.binder)         // Binder 接口 https://github.com/libxposed/service
    implementation(libs.libxposed.service)         // 服务客户端实现 https://github.com/libxposed/service

    // 代码生成和工具库
    implementation(libs.okhttp)                    // OkHttp 网络请求库
    implementation(libs.dexkit)                    // DEX 文件分析工具
    implementation(libs.jackson.kotlin)            // Jackson Kotlin 支持

    // Jackson JSON 处理库
    implementation(libs.jackson.core)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.annotations)
}