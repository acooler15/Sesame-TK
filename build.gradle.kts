buildscript {
    dependencies {
        // 将 AGP 9 内置 Kotlin（默认 KGP 2.2.10）提升到与 Compose 编译器插件一致的版本，
        // 否则 produceReleaseComposeMapping 会去解析不存在的 compose-group-mapping:2.2.10
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
}

allprojects {
    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.addAll(listOf("-Xlint:unchecked", "-Xlint:deprecation"))
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
