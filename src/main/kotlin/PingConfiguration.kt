package de.binarynoise.pingTui

import java.io.File
import kotlin.system.exitProcess
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import io.github.config4k.extract
import io.github.config4k.toConfig

interface PingConfiguration {
    val pingMethod: PingMethod
    val interval: Long
    val enableArpScan: Boolean
    val enableInterfaceScan: Boolean
    
    val hosts: List<String>
    
    companion object : PingConfiguration by PingConfigurationSupport.loadedPingConfiguration, JarFileProvider by PingConfigurationSupport {
        val isWindows: Boolean = System.getProperty("os.name").lowercase().contains("win")
    }
    
    private object PingConfigurationSupport : JarFileProvider {
        val loadedPingConfiguration: LoadedConfiguration
        
        override val jarFile: File? = File(this::class.java.protectionDomain.codeSource.location.toURI()).takeIf { it.absolutePath.endsWith(".jar") }
        val configFile = File(jarFile?.parent, "pingTui.conf")
        
        private const val configRootPath = "pingTui"
        
        init {
            if (!configFile.exists()) createFileAndExit()
            
            val configFileText = configFile.readText()
            if (configFileText.isBlank()) createFileAndExit()
            
            val parseString = ConfigFactory.parseString(configFileText)
            loadedPingConfiguration = parseString.extract(configRootPath)
            println("loaded configuration:")
            println(loadedPingConfiguration)
        }
        
        private val configRenderOptions: ConfigRenderOptions
            get() = ConfigRenderOptions.defaults().setOriginComments(false).setComments(true).setFormatted(true).setJson(false)
        
        private fun createFileAndExit(): Nothing {
            try {
                val defaultConfig = LoadedConfiguration(
                    pingMethod = PingMethod.Tcp,
                    interval = 1000L,
                    enableArpScan = true,
                    enableInterfaceScan = true,
                    hosts = emptyList(),
                ).toConfig(configRootPath).root().render(configRenderOptions)
                
                configFile.bufferedWriter().use {
                    it.write(defaultConfig)
                    it.flush()
                }
                check(configFile.readText().isNotBlank())
                System.err.println(
                    """
                    Konnte die $configFile nicht einlesen.
                    Die Datei wurde neu erstellt und mit den Standardwerten gefüllt.
                    ${configFile.canonicalPath}
                    """.trimIndent()
                )
            } catch (e: Exception) {
                System.err.println(
                    """
                    Konnte die $configFile nicht einlesen.
                    Die Datei konnte aber auch nicht neu erstellt und mit den Standardwerten gefüllt werden:
                    ${configFile.canonicalPath}
                    """.trimIndent() + "\n" + e.stackTraceToString()
                )
            }
            exitProcess(1)
        }
    }
    
    private interface JarFileProvider {
        val jarFile: File?
    }
    
    data class LoadedConfiguration(
        override var pingMethod: PingMethod,
        override var interval: Long,
        override var enableArpScan: Boolean,
        override var enableInterfaceScan: Boolean,
        override var hosts: List<String>,
    ) : PingConfiguration
}
