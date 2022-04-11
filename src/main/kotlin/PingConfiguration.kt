package de.binarynoise.pingTui

import java.io.File
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.system.exitProcess

object PingConfiguration {
    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    var pingMethod: PingMethod = PingMethod.Tcp
    var interval: Long = 1000
    var enableArpScan: Boolean = true
    var enableInterfaceScan: Boolean = true
    
    var hosts: List<String> = mutableListOf()
    
    init {
        val file = File("pingtui.config.kts")
        
        val res: ResultWithDiagnostics<EvaluationResult> = evalFile(file)
        
        if (res is ResultWithDiagnostics.Failure || !file.exists() || file.readText().isBlank()) {
            try {
                file.bufferedWriter().use {
                    it.write("""
                        package de.binarynoise.pingTui
                        
                        with(PingConfiguration) {
                            
                            hosts = listOf(
                            
                            )
                            
                            pingMethod = PingMethod.Tcp
                            interval = 1000
                            enableArpScan = true
                            enableInterfaceScan = true
                        }
                        """.trimIndent())
                    it.flush()
                }
                assert(file.readText().isNotBlank())
                System.err.println("""
                    Konnte die pingtui.config.kts nicht einlesen.
                    Die Datei wurde neu erstellt und mit den Standardwerten gefüllt.
                    """.trimIndent())
            } catch (e: Exception) {
                System.err.println("""
                    Konnte die pingtui.config.kts nicht einlesen.
                    Die Datei konnte aber auch nicht neu erstellt und mit den Standardwerten gefüllt werden:
                    
                """.trimIndent() + e.stackTraceToString())
            }
            exitProcess(1)
        }
    }
}
