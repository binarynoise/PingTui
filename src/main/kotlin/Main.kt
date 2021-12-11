package de.binarynoise.pingTui

import kotlinx.coroutines.*
import java.io.IOException
import java.net.*
import java.time.Clock
import java.time.Duration
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.math.roundToInt
import kotlin.random.Random

val hosts = sortedSetOf<Host>({ o1, o2 ->
    val o1s = o1.address.split(".")
    val o2s = o2.address.split(".")
    
    for (i in 0 until 4) {
        val c = o1s[i].toInt().compareTo(o2s[i].toInt())
        if (c != 0) return@sortedSetOf c
    }
    return@sortedSetOf 0
}).apply {
    add(Host("62.171.176.31"))
}

val line = "\u2500".repeat(75)
const val clear = "\u001B[H\u001B[J"

val clock: Clock = Clock.systemUTC()

fun main() {
    Locale.setDefault(Locale.ENGLISH)
    
    while (true) {
        count++
        
        val startTime = clock.instant()
        
        if (count % 10 == 1) {
            hosts.addAll(NetworkInterface.getNetworkInterfaces()
                .asSequence()
                .flatMap { it.inetAddresses.asSequence() }
                .filterIsInstance<Inet4Address>()
                .map { Host(it.hostAddress) })
            
            hosts.addAll(Runtime.getRuntime().exec("ip neigh").inputStream.bufferedReader()
                .lineSequence()
                .filterNot { it.endsWith("FAILED") || it.endsWith("INCOMPLETE") }
                .map { it.split(" ")[0] }
                .filter { it.matches("(\\d+\\.){3}\\d+".toRegex()) }
                .map { Host(it) })
        }
        
        val statistic = runBlocking(Dispatchers.IO) {
            hosts.map { host ->
                async {
                    delay(Random.nextLong(0, 100))
                    host.ping()
                    host.toString()
                }
            }.awaitAll().joinToString("\n")
        }
        val count = count.toString().padEnd(6)
        val header = "Ping $count      now │          10 │         100 │        1000 │      total"
//				         "192.168.2.95:    0ms │    0ms   0% │   -      -  │   -      -  │   0ms   0%"
        
        val elapsed = Duration.between(startTime, clock.instant()).toMillis()
        val sleep = 1000 - elapsed
        if (sleep > 0) Thread.sleep(sleep)
        
        print(clear)
        println(line)
        println(header)
        println(statistic)
        println(line)
    }
}

var count = 0

class Host(internal val address: String) {
    private val reachedHistory: ArrayDeque<Boolean> = ArrayDeque()
    private val pingHistory: ArrayDeque<Double> = ArrayDeque()
    
    private val paddedAddress = "$address:".padEnd(15)
    private var ping = -1
    private var reached = false
    
    private val method = 2
    
    fun ping() {
        
        when (method) {
            1 -> {
                val process = ProcessBuilder().command("ping", "-c", "1", "-W", "1", address).start()
                process.waitFor()
                reached = process.exitValue() == 0
                
                reachedHistory.addFirst(reached)
                if (reached) {
                    val lastLine = process.inputStream.bufferedReader().lineSequence().last()
                    val duration = lastLine.split("=").last().split("/").first().trim().toDouble()
                    pingHistory.addFirst(duration)
                    ping = duration.roundToInt()
                }
            }
            2 -> {
                
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
                        val duration = Duration.between(startTime, stopTime).toNanos() / 1e6
                        reached = true
                        reachedHistory.addFirst(true)
                        pingHistory.addFirst(duration)
                        ping = duration.roundToInt()
                    }
                } catch (ignore: SocketTimeoutException) {
                    reached = false
                    reachedHistory.addFirst(false)
                } catch (ignore: NoRouteToHostException) {
                    reached = false
                    reachedHistory.addFirst(false)
                } catch (e: IOException) {
                    val msg = e.message!!
                    
                    //@formatter:off
                    // RST should result in ConnectException, but on macOS ConnectionException can also come with e.g. "No route to host"
                    when {
                        arrayOf(
                            /*Connection*/"refused",
                            /*Verbindungsaufbau*/ "abgelehnt",
                        ).any { msg.contains(it) } -> {
                            // we've got an RST packet from the host - it is alive
                            val stopTime = clock.instant()
                            val duration = Duration.between(startTime, stopTime).toNanos() / 1e6
                            reached = true
                            reachedHistory.addFirst(true)
                            pingHistory.addFirst(duration)
                            ping = duration.roundToInt()
                        }
                        arrayOf(
                            /*No*/ "route to host",
                            /*Host is*/ "down",
                            /*Network*/ "unreachable",
                            /*Socket*/ "closed",
                            "Invalid argument",
                            "connect failed",
                            /*Das Netzwerk ist */ "nicht erreichbar"
                        ).any { msg.contains(it) } -> {
                            // host is down
                            reached = false
                            reachedHistory.addFirst(false)
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
            }
        }
    }
    
    // "100%"
    // "  - "
    private fun paddedLoss(count: Int): String {
        if (count > reachedHistory.size || count == 0 || reachedHistory.size == 0) return "  - "
        
        val loss = (reachedHistory.subList(0, count).count { !it }.toFloat() / count * 100).roundToInt()
        val paddedLoss = loss.toString().padStart(3)
        
        return "$paddedLoss%"
    }
    
    // "1000ms"
    // "  -   "
    private fun paddedPing(count: Int): String {
        if (count > pingHistory.size || count == 0 || pingHistory.size == 0) return "  -   "
        
        val ping = (pingHistory.subList(0, count).sum() / count).roundToInt()
        val paddedPing = ping.toString().padStart(4)
        
        return "${paddedPing}ms"
    }
    
    private fun lastPingPadded(): String {
        return if (reached) {
            ping.toString().padStart(3) + "ms"
        } else "  -  "
    }
    
    //@formatter:off
    override fun toString(): String {
        return paddedAddress +
                lastPingPadded() + " │ " +
                paddedPing(10) + " " +
                paddedLoss(10) + " │ " +
                paddedPing(100) + " " +
                paddedLoss(100) + " │ " +
                paddedPing(1000) + " " +
                paddedLoss(1000) + " │" +
                paddedPing(pingHistory.size) + " " +
                paddedLoss(reachedHistory.size)
    }
    //@formatter:on
}
