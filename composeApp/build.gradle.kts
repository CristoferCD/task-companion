import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.versions)
}

kotlin {
    jvm()
    compilerOptions {
        freeCompilerArgs.add("-Xexplicit-backing-fields")
    }
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.navigation)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlin.serialization)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.exposed.core)
            implementation(libs.exposed.jdbc)
            implementation(libs.exposed.json)
            implementation(libs.exposed.kotlin.datetime)
            implementation(libs.exposed.migration.core)
            implementation(libs.exposed.migration.jdbc)
            implementation(libs.sqlite)
            implementation(libs.charts)
//            implementation(libs.compose.colorpicker)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.kotlin.logging)
            implementation(libs.logback)
        }
    }
}


compose.desktop {
    application {
        mainClass = "es.cristcd.taskcompanion.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Deb)
            //suggestRuntimeModules task
            modules("java.instrument", "java.management", "java.sql", "java.naming", "jdk.unsupported")
            packageName = "es.cristcd.taskcompanion"
            packageVersion = "0.0.8"

            windows {
                dirChooser = true
                perUserInstall = true
                upgradeUuid = "7f4ce14d-1896-4550-87ed-29b333793066"
            }
        }
    }
}

tasks.register("printVersionName") {
    println(compose.desktop.application.nativeDistributions.packageVersion)
}
