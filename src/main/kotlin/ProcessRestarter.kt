package de.binarynoise.pingTui

import kotlin.system.exitProcess
import de.binarynoise.pingTui.PingConfiguration.Companion.jarFile

object ProcessRestarter {
    
    fun restartInConsole() {
        if (PingConfiguration.isWindows) {
            restartInConsoleWindows()
        } else {
            restartInConsoleLinux()
        }
    }
    
    private fun restartInConsoleWindows() {
        val canonicalPath = jarFile?.canonicalPath
        if (canonicalPath != null) {
            Runtime.getRuntime().exec("cmd /c start cmd /k java -jar $canonicalPath -r")
            println("restarted process in cmd, $jarFile")
            exitProcess(0)
        }
    }
    
    private fun restartInConsoleLinux() {
        // TODO
    }
}
