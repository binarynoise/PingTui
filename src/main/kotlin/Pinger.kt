package de.binarynoise.pingTui

import java.io.IOException
import java.net.InetSocketAddress
import java.net.NoRouteToHostException
import java.net.Socket
import java.net.SocketTimeoutException
import java.time.Clock
import java.time.Duration

interface Pinger {
    fun ping(address: String): PingResult
    
    companion object : Pinger {
        private val clock: Clock = Clock.systemUTC()
        
        override fun ping(address: String): PingResult {
            return when (PingProperties.pingMethod) {
                PingMethod.Tcp -> TCPPinger.ping(address)
                PingMethod.Command -> {
                    if (PingProperties.isWindows) {
                        WindowsPinger.ping(address)
                    } else {
                        LinuxPinger.ping(address)
                    }
                }
            }
        }
    }
    
    private object WindowsPinger : Pinger {
        override fun ping(address: String): PingResult {
            val reachable: Boolean?
            var duration: Double? = null
            
            val process = ProcessBuilder().command("ping", "-n", "1", "-w", "1", address).start()
            
            process.waitFor()
            reachable = process.exitValue() == 0
            
            val outLines = process.inputStream.bufferedReader().readLines().filterNot { it.isBlank() }
            if (reachable) {
                val split = outLines.last().split("=").last().trim()
                if (split.contains("ms")) { // Windows is weird and doesn't always print the whole ping-summary
                    duration = split.removeSuffix("ms").toDouble()
                }
            }
            
            return PingResult(reachable, duration)
        }
    }
    
    private object LinuxPinger : Pinger {
        override fun ping(address: String): PingResult {
            val reachable: Boolean?
            var duration: Double? = null
            
            val process = ProcessBuilder().command("ping", "-c", "1", "-W", "1", address).start()
            
            process.waitFor()
            reachable = process.exitValue() == 0
            
            val outLines = process.inputStream.bufferedReader().readLines().filterNot { it.isBlank() }
            if (reachable) {
                duration = outLines.last().split("=").last().split("/").first().trim().toDouble()
            }
            
            return PingResult(reachable, duration)
        }
    }
    
    // adapted from https://github.com/angryip/ipscan/blob/master/src/net/azib/ipscan/core/net/TCPPinger.java
    private object TCPPinger : Pinger {
        override fun ping(address: String): PingResult {
            var reachable: Boolean? = null
            var duration: Double? = null
            
            val socket = Socket()
            val probePort = 80
            val startTime = clock.instant()
            try {
                // set some optimization options
                socket.reuseAddress = true
                socket.receiveBufferSize = 32
                socket.connect(InetSocketAddress(address, probePort), 950)
                if (socket.isConnected) {
                    // it worked - success
                    val stopTime = clock.instant()
                    duration = Duration.between(startTime, stopTime).toNanos() / 1e6
                    reachable = true
                }
            } catch (ignore: SocketTimeoutException) {
                reachable = false
            } catch (ignore: NoRouteToHostException) {
                reachable = false
            } catch (e: IOException) {
                val msg = e.message!!
                
                //@formatter:off
                // RST should result in ConnectException, but on macOS ConnectionException can also come with e.g. "No route to host"
                when {
                    msg.containsAny(
                        /*Connection*/"refused",
                        /*Verbindungsaufbau*/ "abgelehnt",
                    ) -> {
                        // we've got an RST packet from the host - it is alive
                        val stopTime = clock.instant()
                        duration = Duration.between(startTime, stopTime).toNanos() / 1e6
                        reachable = true
                    }
                    e is NoRouteToHostException || msg.containsAny(
                        /*No*/ "route to host",
                        /*Host is*/ "down",
                        /*Network*/ "unreachable",
                        /*Socket*/ "closed",
                        "Invalid argument",
                        "connect failed",
                        /*Das Netzwerk ist */ "nicht erreichbar"
                    ) -> {
                        // host is down
                        reachable = false
                    }
                    msg.containsAny(
                        /* Software caused connection abort */ "connection abort"
                    ) -> {
                        // network is down, ignore
                    }
                    else -> {
                        // something unknown
                        throw e
                    }
                }
                //@formatter:on
            } finally {
                socket.close()
            }
            
            return PingResult(reachable, duration)
        }
    }
}

enum class PingMethod {
    Tcp, Command
}

data class PingResult(val reachable: Boolean?, val pingTime: Double?)

fun String.containsAny(vararg strings: String) = strings.any { this.contains(it) }
