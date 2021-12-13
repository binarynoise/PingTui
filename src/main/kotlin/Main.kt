package de.binarynoise.pingTui

import kotlinx.coroutines.*
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.Ansi.ansi
import org.fusesource.jansi.AnsiConsole
import java.io.File
import java.io.IOException
import java.lang.reflect.Method
import java.net.*
import java.time.Clock
import java.time.Duration
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

val hosts = sortedSetOf<Host>({ o1, o2 ->
    val o1s = o1.address.split(".")
    val o2s = o2.address.split(".")
    
    for (i in 0 until min(o1s.size, o2s.size)) {
        val o1si = o1s[i].toIntOrNull()
        val o2si = o2s[i].toIntOrNull()
        val c = if (o1si != null && o2si != null) {
            o1si.compareTo(o2si)
        } else {
            o1s[i].compareTo(o2s[i])
        }
        if (c != 0) return@sortedSetOf c
    }
    return@sortedSetOf o1s.size - o2s.size
}).apply {
    val file = File("hosts.txt")
    if (file.exists()) {
        file.inputStream()
            .bufferedReader()
            .lineSequence()
            .map { it.trim() }
            .filterNot { it.startsWith("//") || it.isEmpty() }
            .map { Host(it) }
            .addAllTo(this)
    } else {
        with(file) {
            createNewFile()
            bufferedWriter().use {
                it.appendLine("// Add hosts (ip addresses) here you always want to ping")
                it.appendLine("// Use // as comment or to disable an entry")
            }
        }
    }
}

val isWindows = System.getProperty("os.name").lowercase().contains("win")
val line = "\u2500".repeat(77)
val ansiClear //= "\u001B[H\u001B[J"
    //    get() = ansi().appendEscapeSequence('H').appendEscapeSequence('j').toString()
    get() = ansi().cursor(0, 0).eraseScreen().cursor(0, 0).toString()

var count = 0
val clock: Clock = Clock.systemUTC()
val interval = if (isWindows) 5000 else 1000

val ansiAppendEscapeSequenceMethod: Method =
    Ansi::class.java.getDeclaredMethod("appendEscapeSequence", Char::class.java).apply { isAccessible = true }

fun Ansi.appendEscapeSequence(command: Char): Ansi {
    ansiAppendEscapeSequenceMethod(this, command)
    return this
}

fun main() {
    Locale.setDefault(Locale.ENGLISH)
    AnsiConsole.systemInstall()
    ansi().a(Ansi.Attribute.BLINK_SLOW).a(Ansi.Attribute.BLINK_OFF)
    
    Runtime.getRuntime().addShutdownHook(Thread {
        Runtime.getRuntime().halt(0)
    })
    
    while (true) {
        count++
        
        val startTime = clock.instant()
        
        if (count % 10 == 1) {
            hosts.addAll(NetworkInterface.getNetworkInterfaces()
                .asSequence()
                .flatMap { it.inetAddresses.asSequence() }
                .filterIsInstance<Inet4Address>()
                .map { Host(it.hostAddress) })
            
            if (isWindows) {
                Runtime.getRuntime()
                    .exec("powershell Get-NetNeighbor -AddressFamily IPv4 -State Reachable | select -ExpandProperty IPAddress").inputStream.bufferedReader()
                    .lineSequence()
                    .map { Host(it) }
                    .addAllTo(hosts)
            } else {
                Runtime.getRuntime().exec("ip -4 neigh show nud reachable").inputStream.bufferedReader()
                    .lineSequence()
                    .map { Host(it.substringBefore(' ')) }
                    .addAllTo(hosts)
            }
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
        
        hosts.removeAll { it.isUnreachable() }
        
        val count = count.toString().padEnd(6)
        val header = "Ping $count        now │          10 │         100 │        1000 │      total"
//				     "192.168.2.95:      0ms │    0ms   0% │   -      -  │   -      -  │   0ms   0%"
        
        val elapsed = Duration.between(startTime, clock.instant()).toMillis()
        val sleep = interval - elapsed
        if (sleep > 0) Thread.sleep(sleep)
        
        print(if (isWindows) ansi().appendEscapeSequence('H').eraseScreen() else ansiClear)
        println(line)
        println(header)
        println(statistic)
        println(line)
    }
}

private fun <T> Sequence<T>.addAllTo(set: MutableCollection<T>) {
    set.addAll(this)
}

class Host(internal val address: String) {
    private val reachedHistory: ArrayDeque<Boolean> = ArrayDeque()
    private val pingHistory: ArrayDeque<Double> = ArrayDeque()
    
    private val paddedAddress = "$address: ".padEnd(17)
    private var ping = -1
    private var reached = false
    
    private val method = if (isWindows) 1 else 2
    
    fun ping() {
        
        when (method) {
            1 -> {
                val process = if (isWindows) {
                    ProcessBuilder().command("ping", "-n", "1", "-w", "1", address).start()
                } else { // linux
                    ProcessBuilder().command("ping", "-c", "1", "-W", "1", address).start()
                }
                
                process.waitFor()
                reached = process.exitValue() == 0
                
                reachedHistory.addFirst(reached)
                if (reached) {
                    val lastLine =
                        process.inputStream.bufferedReader().use { reader -> reader.lineSequence().filterNot { line -> line.isBlank() }.last() }
                    val duration = if (isWindows) {
                        lastLine.split("=").last().trim().removeSuffix("ms").toDouble()
                    } else { // linux
                        lastLine.split("=").last().split("/").first().trim().toDouble()
                    }
                    pingHistory.addFirst(duration)
                    ping = duration.roundToInt()
                }
            }
            2 -> {
                // adapted from https://github.com/angryip/ipscan/blob/master/src/net/azib/ipscan/core/net/TCPPinger.java
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
                        arrayOf(
                            /* Software caused connection abort */ "connection abort"
                        ).any { msg.contains(it) } -> {
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
    // "   -  "
    private fun paddedPing(count: Int): String {
        if (count > pingHistory.size || count == 0 || pingHistory.size == 0) return "   -  "
        
        val ping = (pingHistory.subList(0, count).sum() / count).roundToInt()
        val paddedPing = ping.toString().padStart(4)
        
        return "${paddedPing}ms"
    }
    
    private fun lastPingPadded(): String {
        return if (reached) {
            ping.toString().padStart(3) + "ms"
        } else "  -  "
    }
    
    fun isUnreachable(): Boolean = reachedHistory.size >= 1000 && reachedHistory.subList(0, 100).none { it /* == true */ }
    
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
