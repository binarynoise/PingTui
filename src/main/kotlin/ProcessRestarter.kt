package de.binarynoise.pingTui

import kotlin.system.exitProcess
import de.binarynoise.pingTui.PingConfiguration.Companion.jarFilePath

object ProcessRestarter {
    
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
        // TODO
    }
}
