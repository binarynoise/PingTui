package de.binarynoise.pingTui

interface ArpScanner {
    fun findNewHosts(): List<Host>
    
    companion object : ArpScanner {
        override fun findNewHosts(): List<Host> = when {
            !PingConfiguration.enableArpScan -> emptyList()
            PingConfiguration.isWindows -> WindowsArpScanner.findNewHosts()
            else -> LinuxArpScanner.findNewHosts()
        }
    }
    
    private object WindowsArpScanner : ArpScanner {
        override fun findNewHosts(): List<Host> {
            val getNetNeighbor = "powershell Get-NetNeighbor -AddressFamily IPv4 -State Reachable | select -ExpandProperty IPAddress"
            return getNetNeighbor.exec().map { Host(it) }.toList()
        }
    }
    
    private object LinuxArpScanner : ArpScanner {
        override fun findNewHosts(): List<Host> {
            val ipNeigh = "ip -4 neigh show nud reachable"
            return ipNeigh.exec().map { Host(it.substringBefore(' ')) }.toList()
        }
    }
}

@Suppress("DEPRECATION")
fun String.exec() = Runtime.getRuntime().exec(this).inputStream.bufferedReader().lineSequence()
