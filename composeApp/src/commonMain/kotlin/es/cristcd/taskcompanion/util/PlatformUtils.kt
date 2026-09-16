package es.cristcd.taskcompanion.util

import java.io.File

enum class Platform {
    WINDOWS,
    LINUX,
    MACOS
}

fun getCurrentPlatform(): Platform? {
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("win") -> Platform.WINDOWS
        os.contains("linux") -> Platform.LINUX
        os.contains("mac") -> Platform.MACOS
        else -> null
    }
}

fun getAppDataDir(): File {
    return when(getCurrentPlatform()) {
        Platform.WINDOWS -> File(System.getenv("LOCALAPPDATA"), ".TaskCompanion")
        Platform.MACOS -> File(System.getProperty("user.home"), "Library/Application Support/.TaskCompanion")
        else -> File(System.getProperty("user.home"), ".TaskCompanion")
    }
}