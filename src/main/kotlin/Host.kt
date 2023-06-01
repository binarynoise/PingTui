package de.binarynoise.pingTui

import kotlin.math.roundToInt
import de.binarynoise.pingTui.Main.statCount1
import de.binarynoise.pingTui.Main.statCount2
import de.binarynoise.pingTui.Main.statCount3

class Host(val address: String) {
    
    private val reachedHistory: ArrayDeque<Boolean> = ArrayDeque()
    private var reachedHistoryOverflow: Int = 0
    private var reachedHistoryOverflowCount: Int = 0
    
    private val pingHistory: ArrayDeque<Double> = ArrayDeque()
    private var pingHistoryOverflow: Double = 0.0
    private var pingHistoryOverflowCount: Int = 0
    
    private val paddedAddress = address.padOrEllipsise(15)
    private var ping = -1
    private var reached = false
    
    fun ping() {
        val pingResult = Pinger.ping(address)
        if (pingResult.reachable != null) {
            reached = pingResult.reachable
            reachedHistory.addFirst(pingResult.reachable)
            
            while (reachedHistory.size > statCount3) {
                val last = reachedHistory.removeLast()
                if (last) reachedHistoryOverflow++
                reachedHistoryOverflowCount++
            }
        }
        if (pingResult.pingTime != null) {
            ping = pingResult.pingTime.roundToInt()
            pingHistory.addFirst(pingResult.pingTime)
            
            while (pingHistory.size > statCount3) {
                val last = pingHistory.removeLast()
                pingHistoryOverflow += last
                pingHistoryOverflowCount++
            }
        }
    }
    
    // "100%"
    // "  - "
    private fun paddedLoss(count: Int): String {
        if (count > statCount3 && reachedHistory.size >= statCount3) {
            val sum = reachedHistory.count { !it } + reachedHistoryOverflow
            val totalCount = reachedHistory.size + (reachedHistoryOverflowCount - reachedHistoryOverflow)
            
            val loss = (sum.toFloat() / totalCount * 100).roundToInt()
            val paddedLoss = loss.toString().padStart(3)
            
            return "$paddedLoss%"
        }
        
        if (count > reachedHistory.size || count == 0 || reachedHistory.size == 0) return "  - "
        
        val loss = (reachedHistory.subList(0, count).count { !it }.toFloat() / count * 100).roundToInt()
        val paddedLoss = loss.toString().padStart(3)
        
        return "$paddedLoss%"
    }
    
    // "1000ms"
    // "   -  "
    private fun paddedPing(count: Int): String {
        if (count > statCount3 && pingHistory.size >= statCount3) {
            val sum = pingHistory.sum() + pingHistoryOverflow
            val totalCount = pingHistory.size + pingHistoryOverflowCount
            
            val ping = (sum.toFloat() / totalCount).roundToInt()
            val paddedPing = ping.toString().padStart(4)
            
            return "${paddedPing}ms"
        }
        
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
    
    fun isUnreachable(): Boolean = reachedHistory.size >= statCount3 && reachedHistory.subList(0, statCount2).none { it /* == true */ }
    
    //@formatter:off
    override fun toString(): String {
        
        return paddedAddress + " │ " +
                lastPingPadded() + " │ " +
                paddedPing(statCount1) + " " +
                paddedLoss(statCount1) + " │ " +
                paddedPing(statCount2) + " " +
                paddedLoss(statCount2) + " │ " +
                paddedPing(statCount3) + " " +
                paddedLoss(statCount3) + " │" +
                paddedPing(pingHistory.size) + " " +
                paddedLoss(reachedHistory.size)
    }
    //@formatter:on
    
    private fun String.padOrEllipsise(targetSize: Int): String {
        return when {
            length == targetSize -> this
            length < targetSize -> this.padEnd(targetSize)
            else /* length > size */ -> this.substring(0, targetSize - 1) + "…"
        }
    }
}
