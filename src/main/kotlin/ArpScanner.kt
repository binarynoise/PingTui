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
            return Runtime.getRuntime()
                .exec("powershell Get-NetNeighbor -AddressFamily IPv4 -State Reachable | select -ExpandProperty IPAddress").inputStream.bufferedReader()
                .lineSequence()
                .map { Host(it) }
                .toList()
        }
    }
    
    private object LinuxArpScanner : ArpScanner {
        override fun findNewHosts(): List<Host> {
            return Runtime.getRuntime().exec("ip -4 neigh show nud reachable").inputStream.bufferedReader()
                .lineSequence()
                .map { Host(it.substringBefore(' ')) }
                .toList()
        }
    }
}
