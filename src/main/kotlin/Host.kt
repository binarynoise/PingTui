package de.binarynoise.pingTui

import kotlin.math.roundToInt

class Host(val address: String) {
    private val reachedHistory: ArrayDeque<Boolean> = ArrayDeque()
    private val pingHistory: ArrayDeque<Double> = ArrayDeque()
    
    private val paddedAddress = "$address: ".padEnd(17)
    private var ping = -1
    private var reached = false
    
    fun ping() {
        val pingResult = Pinger.ping(address)
        if (pingResult.reachable != null) {
            reached = pingResult.reachable
            reachedHistory.addFirst(pingResult.reachable)
        }
        if (pingResult.pingTime != null) {
            ping = pingResult.pingTime.roundToInt()
            pingHistory.addFirst(pingResult.pingTime)
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
