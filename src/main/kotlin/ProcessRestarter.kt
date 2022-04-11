package de.binarynoise.pingTui

import java.io.File
import kotlin.system.exitProcess

object ProcessRestarter {
    private val jarFilePath = File(this::class.java.protectionDomain.codeSource.location.toURI()).absolutePath
    
    fun restartInConsole(): Nothing {
        if (PingConfiguration.isWindows) {
            restartInConsoleWindows()
        } else {
            restartInConsoleLinux()
        }
        exitProcess(0)
    }
    
    private fun restartInConsoleWindows() {
        Runtime.getRuntime().exec("cmd /c start cmd /k java -jar $jarFilePath -r")
    }
    
    private fun restartInConsoleLinux() {
        TODO()
    }
}
