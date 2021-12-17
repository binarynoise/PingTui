package de.binarynoise.pingTui

import kotlinx.coroutines.*
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.Ansi.ansi
import org.fusesource.jansi.AnsiConsole
import java.io.File
import java.time.Clock
import java.time.Duration
import java.util.*
import kotlin.math.min
import kotlin.random.Random

object Main {
    
    private val hosts = sortedSetOf<Host>(::hostComparator)
    
    init {
        val file = File("hosts.txt")
        if (file.exists()) {
            file.inputStream()
                .bufferedReader()
                .lineSequence()
                .map { it.trim() }
                .filterNot { it.startsWith("//") || it.isEmpty() }
                .map { Host(it) }
                .addAllTo(hosts)
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
    
    private val line = "\u2500".repeat(77)
    private val ansiClear
        get() = ansi().cursor(0, 0).eraseScreen().cursor(0, 0).toString()
    
    private var count = 0
    private val clock: Clock = Clock.systemUTC()
    
    @JvmStatic
    fun main(args: Array<String>) {
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
                NetworkInterfaceScanner.scanNetworkInterfaces().addAllTo(hosts)
                ArpScanner.findNewHosts().addAllTo(hosts)
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

//            hosts.removeAll { it.isUnreachable() }
            
            val count = count.toString().padEnd(6)
            val header = "Ping $count        now │          10 │         100 │        1000 │      total"
            //           "192.168.2.95:      0ms │    0ms   0% │   -      -  │   -      -  │   0ms   0%"
            
            val elapsed = Duration.between(startTime, clock.instant()).toMillis()
            val sleep = PingProperties.interval - elapsed
            if (sleep > 0) Thread.sleep(sleep)
            
            print(ansiClear)
            println(line)
            println(header)
            println(statistic)
            println(line)
        }
    }
    
    private fun hostComparator(o1: Host, o2: Host): Int {
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
            if (c != 0) return c
        }
        return o1s.size - o2s.size
    }
}

private fun <T> Sequence<T>.addAllTo(collection: MutableCollection<T>) {
    collection.addAll(this)
}

private fun <T> List<T>.addAllTo(collection: MutableCollection<T>) {
    collection.addAll(this)
}
