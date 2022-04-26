package de.binarynoise.pingTui

import java.time.Clock
import java.time.Duration
import java.util.*
import kotlin.math.min
import kotlin.random.Random
import kotlin.system.exitProcess
import kotlinx.coroutines.*
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.Ansi.ansi
import org.fusesource.jansi.AnsiConsole

object Main {
    
    private val hosts = sortedSetOf<Host>(::hostComparator)
    
    private val line = "\u2500".repeat(78)
    private val ansiClear
        get() = ansi().cursor(0, 0).eraseScreen().cursor(0, 0).toString()
    
    private var count = 0
    private val clock: Clock = Clock.systemUTC()
    
    @JvmStatic
    fun main(args: Array<String>) {
        if ((System.console() == null || System.console().reader() == null) && PingConfiguration.isWindows) { // TODO catch if running in IDE
            if (args.isNotEmpty() && args.first() == "-r") {
                exitProcess(0)
            } else {
                ProcessRestarter.restartInConsole()
            }
        }
        
        Locale.setDefault(Locale.ENGLISH)
        AnsiConsole.systemInstall()
        print(ansi().a(Ansi.Attribute.BLINK_OFF))
        
        Runtime.getRuntime().addShutdownHook(Thread {
            Runtime.getRuntime().halt(0)
        })
        
        hosts.addAll(PingConfiguration.hosts.map { Host(it) })
        
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
            val header = "Ping $count     │   now │          10 │         100 │        1000 │      total"
            //           "127.0.0.1       │   0ms │    1ms   0% │    -     -  │    -     -  │   1ms   0%"
            
            val elapsed = Duration.between(startTime, clock.instant()).toMillis()
            val sleep = PingConfiguration.interval - elapsed
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
