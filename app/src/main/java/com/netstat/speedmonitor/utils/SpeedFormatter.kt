package com.netstat.speedmonitor.utils

object SpeedFormatter {

    fun format(bytesPerSecond: Double, unit: String): String {
        return when (unit) {
            "bps" -> formatBps(bytesPerSecond * 8)
            "Bps" -> formatBps(bytesPerSecond, false)
            "kbps" -> String.format("%.1f Kbps", bytesPerSecond * 8 / 1000)
            "KBps" -> String.format("%.1f KB/s", bytesPerSecond / 1000)
            "mbps" -> String.format("%.2f Mbps", bytesPerSecond * 8 / 1_000_000)
            "MBps" -> String.format("%.2f MB/s", bytesPerSecond / 1_000_000)
            else -> formatAuto(bytesPerSecond)
        }
    }

    fun formatShort(bytesPerSecond: Double, unit: String, showUnit: Boolean = true): String {
        return when (unit) {
            "bps" -> formatBpsShort(bytesPerSecond * 8, showUnit)
            "Bps" -> formatBpsShort(bytesPerSecond, showUnit)
            "kbps" ->
                    if (showUnit) String.format("%.2fK", bytesPerSecond * 8 / 1000)
                    else String.format("%.2f", bytesPerSecond * 8 / 1000)
            "KBps" ->
                    if (showUnit) String.format("%.2fK", bytesPerSecond / 1000)
                    else String.format("%.2f", bytesPerSecond / 1000)
            "mbps" ->
                    if (showUnit) String.format("%.2fM", bytesPerSecond * 8 / 1_000_000)
                    else String.format("%.2f", bytesPerSecond * 8 / 1_000_000)
            "MBps" ->
                    if (showUnit) String.format("%.2fM", bytesPerSecond / 1_000_000)
                    else String.format("%.2f", bytesPerSecond / 1_000_000)
            else -> formatAutoShort(bytesPerSecond, showUnit)
        }
    }

    private fun formatBps(bitsPerSecond: Double, isBits: Boolean = true): String {
        val suffix = if (isBits) "bps" else "B/s"
        return when {
            bitsPerSecond >= 1_000_000_000 ->
                    String.format("%.2f G%s", bitsPerSecond / 1_000_000_000, suffix)
            bitsPerSecond >= 1_000_000 ->
                    String.format("%.2f M%s", bitsPerSecond / 1_000_000, suffix)
            bitsPerSecond >= 1_000 -> String.format("%.2f K%s", bitsPerSecond / 1_000, suffix)
            else -> String.format("%.2f %s", bitsPerSecond, suffix)
        }
    }

    private fun formatBpsShort(value: Double, showUnit: Boolean): String {
        return when {
            value >= 1_000_000_000 ->
                    if (showUnit) String.format("%.2fG", value / 1_000_000_000)
                    else String.format("%.2f", value / 1_000_000_000)
            value >= 1_000_000 ->
                    if (showUnit) String.format("%.2fM", value / 1_000_000)
                    else String.format("%.2f", value / 1_000_000)
            value >= 1_000 ->
                    if (showUnit) String.format("%.2fK", value / 1_000)
                    else String.format("%.2f", value / 1_000)
            else -> if (showUnit) String.format("%.2fB", value) else String.format("%.2f", value)
        }
    }

    private fun formatAuto(bytesPerSecond: Double): String {
        return when {
            bytesPerSecond >= 1_000_000 -> String.format("%.2f MB/s", bytesPerSecond / 1_000_000)
            bytesPerSecond >= 1_000 -> String.format("%.2f KB/s", bytesPerSecond / 1_000)
            else -> String.format("%.2f B/s", bytesPerSecond)
        }
    }

    private fun formatAutoShort(bytesPerSecond: Double, showUnit: Boolean): String {
        return when {
            bytesPerSecond >= 1_000_000 ->
                    if (showUnit) String.format("%.2fM", bytesPerSecond / 1_000_000)
                    else String.format("%.2f", bytesPerSecond / 1_000_000)
            bytesPerSecond >= 1_000 ->
                    if (showUnit) String.format("%.2fK", bytesPerSecond / 1_000)
                    else String.format("%.2f", bytesPerSecond / 1_000)
            else ->
                    if (showUnit) String.format("%.2fB", bytesPerSecond)
                    else String.format("%.2f", bytesPerSecond)
        }
    }

    /**
     * Returns a Pair of (number, unit) for drawing with different sizes Example: (12.5, "M") or
     * (890, "K")
     */
    fun formatShortSplit(bytesPerSecond: Double, unit: String): Pair<String, String> {
        return when (unit) {
            "bps" -> formatBpsShortSplit(bytesPerSecond * 8)
            "Bps" -> formatBpsShortSplit(bytesPerSecond)
            "kbps" -> Pair(String.format("%.2f", bytesPerSecond * 8 / 1000), "K")
            "KBps" -> Pair(String.format("%.2f", bytesPerSecond / 1000), "K")
            "mbps" -> Pair(String.format("%.2f", bytesPerSecond * 8 / 1_000_000), "M")
            "MBps" -> Pair(String.format("%.2f", bytesPerSecond / 1_000_000), "M")
            else -> formatAutoShortSplit(bytesPerSecond)
        }
    }

    private fun formatBpsShortSplit(value: Double): Pair<String, String> {
        return when {
            value >= 1_000_000_000 -> Pair(String.format("%.2f", value / 1_000_000_000), "G")
            value >= 1_000_000 -> Pair(String.format("%.2f", value / 1_000_000), "M")
            value >= 1_000 -> Pair(String.format("%.2f", value / 1_000), "K")
            else -> Pair(String.format("%.2f", value), "B")
        }
    }

    private fun formatAutoShortSplit(bytesPerSecond: Double): Pair<String, String> {
        return when {
            bytesPerSecond >= 1_000_000 ->
                    Pair(String.format("%.2f", bytesPerSecond / 1_000_000), "M")
            bytesPerSecond >= 1_000 -> Pair(String.format("%.2f", bytesPerSecond / 1_000), "K")
            else -> Pair(String.format("%.2f", bytesPerSecond), "B")
        }
    }
}
