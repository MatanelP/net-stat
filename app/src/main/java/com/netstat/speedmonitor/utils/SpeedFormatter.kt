package com.netstat.speedmonitor.utils

object SpeedFormatter {
    
    fun format(bytesPerSecond: Double, unit: String): String {
        return when (unit) {
            "bps" -> formatBps(bytesPerSecond * 8)
            "Bps" -> formatBps(bytesPerSecond, false)
            "kbps" -> "${(bytesPerSecond * 8 / 1000).toInt()} Kbps"
            "KBps" -> "${(bytesPerSecond / 1000).toInt()} KB/s"
            "mbps" -> String.format("%.1f Mbps", bytesPerSecond * 8 / 1_000_000)
            "MBps" -> String.format("%.1f MB/s", bytesPerSecond / 1_000_000)
            else -> formatAuto(bytesPerSecond)
        }
    }

    fun formatShort(bytesPerSecond: Double, unit: String, showUnit: Boolean = true): String {
        return when (unit) {
            "bps" -> formatBpsShort(bytesPerSecond * 8, showUnit)
            "Bps" -> formatBpsShort(bytesPerSecond, showUnit)
            "kbps" -> if (showUnit) "${(bytesPerSecond * 8 / 1000).toInt()}K" else "${(bytesPerSecond * 8 / 1000).toInt()}"
            "KBps" -> if (showUnit) "${(bytesPerSecond / 1000).toInt()}K" else "${(bytesPerSecond / 1000).toInt()}"
            "mbps" -> if (showUnit) String.format("%.1fM", bytesPerSecond * 8 / 1_000_000) else String.format("%.1f", bytesPerSecond * 8 / 1_000_000)
            "MBps" -> if (showUnit) String.format("%.1fM", bytesPerSecond / 1_000_000) else String.format("%.1f", bytesPerSecond / 1_000_000)
            else -> formatAutoShort(bytesPerSecond, showUnit)
        }
    }

    private fun formatBps(bitsPerSecond: Double, isBits: Boolean = true): String {
        val suffix = if (isBits) "bps" else "B/s"
        return when {
            bitsPerSecond >= 1_000_000_000 -> String.format("%.1f G%s", bitsPerSecond / 1_000_000_000, suffix)
            bitsPerSecond >= 1_000_000 -> String.format("%.1f M%s", bitsPerSecond / 1_000_000, suffix)
            bitsPerSecond >= 1_000 -> String.format("%.1f K%s", bitsPerSecond / 1_000, suffix)
            else -> String.format("%.0f %s", bitsPerSecond, suffix)
        }
    }

    private fun formatBpsShort(value: Double, showUnit: Boolean): String {
        return when {
            value >= 1_000_000_000 -> if (showUnit) String.format("%.1fG", value / 1_000_000_000) else String.format("%.1f", value / 1_000_000_000)
            value >= 1_000_000 -> if (showUnit) String.format("%.1fM", value / 1_000_000) else String.format("%.1f", value / 1_000_000)
            value >= 1_000 -> if (showUnit) String.format("%.0fK", value / 1_000) else String.format("%.0f", value / 1_000)
            else -> if (showUnit) String.format("%.0fB", value) else String.format("%.0f", value)
        }
    }

    private fun formatAuto(bytesPerSecond: Double): String {
        return when {
            bytesPerSecond >= 1_000_000 -> String.format("%.1f MB/s", bytesPerSecond / 1_000_000)
            bytesPerSecond >= 1_000 -> String.format("%.1f KB/s", bytesPerSecond / 1_000)
            else -> String.format("%.0f B/s", bytesPerSecond)
        }
    }

    private fun formatAutoShort(bytesPerSecond: Double, showUnit: Boolean): String {
        return when {
            bytesPerSecond >= 1_000_000 -> if (showUnit) String.format("%.1fM", bytesPerSecond / 1_000_000) else String.format("%.1f", bytesPerSecond / 1_000_000)
            bytesPerSecond >= 1_000 -> if (showUnit) String.format("%.0fK", bytesPerSecond / 1_000) else String.format("%.0f", bytesPerSecond / 1_000)
            else -> if (showUnit) String.format("%.0fB", bytesPerSecond) else String.format("%.0f", bytesPerSecond)
        }
    }
}
