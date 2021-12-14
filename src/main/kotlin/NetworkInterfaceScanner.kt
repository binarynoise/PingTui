package de.binarynoise.pingTui

import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkInterfaceScanner {
    fun scanNetworkInterfaces(): List<Host> = if (!PingProperties.enableInterfaceScan) emptyList() else {
        NetworkInterface.getNetworkInterfaces()
            .asSequence()
            .flatMap { it.inetAddresses.asSequence() }
            .filterIsInstance<Inet4Address>()
            .map { Host(it.hostAddress) }
            .toList()
    }
}
