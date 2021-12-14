package de.binarynoise.pingTui

import java.io.File
import java.util.*
import kotlin.system.exitProcess

object PingProperties {
    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    val pingMethod: PingMethod
    val interval: Long
    val enableArpScan: Boolean
    val enableInterfaceScan: Boolean
    
    init {
        val propertiesFile = File("config.properties")
        val properties = Properties()
        try {
            propertiesFile.bufferedReader().use { inputStream ->
                properties.load(inputStream)
            }
            
            pingMethod = PingMethod.valueOf(properties["pingMethod"].toString())
            interval = properties["interval"].toString().toLong()
            enableArpScan = properties["enableArpScan"].toString().toBooleanStrict()
            enableInterfaceScan = properties["enableInterfaceScan"].toString().toBooleanStrict()
        } catch (e: Exception) {
            try {
                properties["pingMethod"] = PingMethod.Tcp.toString()
                properties["interval"] = 1000.toString()
                properties["enableArpScan"] = true.toString()
                properties["enableInterfaceScan"] = true.toString()
                
                properties.store(propertiesFile.bufferedWriter(), null)
                
                System.err.println("""
                    Konnte die config.properties nicht einlesen.
                    Die Datei wurde neu erstellt und mit den Standardwerten gefüllt.
                    """.trimIndent())
            } catch (e2: Exception) {
                e2.addSuppressed(e)
                System.err.println("""
                    Konnte die config.properties nicht einlesen.
                    Die Datei konnte aber auch nicht neu erstellt und mit den Standardwerten gefüllt werden:
                    
                """.trimIndent() + e2.stackTraceToString())
            }
            exitProcess(1)
        }
    }
}
