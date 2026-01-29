package com.netstat.speedmonitor.utils

object SpeedFormatter {

    /**
     * Formats a number to at most 3 digits: >= 100: no decimals (e.g., 321) >= 10: 1 decimal (e.g.,
     * 23.5) < 10: 2 decimals (e.g., 1.23)
     */
    private fun formatMax3Digits(value: Double): String {
        return when {
            value >= 100 -> String.format("%.0f", value)
            value >= 10 -> String.format("%.1f", value)
            else -> String.format("%.2f", value)
        }
    }

    fun format(bytesPerSecond: Double, unit: String): String {
        return when (unit) {
            "bps" -> formatBps(bytesPerSecond * 8)
            "Bps" -> formatBps(bytesPerSecond, false)
            "kbps" -> "${formatMax3Digits(bytesPerSecond * 8 / 1000)} Kbps"
            "KBps" -> "${formatMax3Digits(bytesPerSecond / 1000)} KB/s"
            "mbps" -> "${formatMax3Digits(bytesPerSecond * 8 / 1_000_000)} Mbps"
            "MBps" -> "${formatMax3Digits(bytesPerSecond / 1_000_000)} MB/s"
            else -> formatAuto(bytesPerSecond)
        }
    }

    fun formatShort(bytesPerSecond: Double, unit: String, showUnit: Boolean = true): String {
        return when (unit) {
            "bps" -> formatBpsShort(bytesPerSecond * 8, showUnit)
            "Bps" -> formatBpsShort(bytesPerSecond, showUnit)
            "kbps" -> {
                val num = formatMax3Digits(bytesPerSecond * 8 / 1000)
                if (showUnit) "${num}K" else num
            }
            "KBps" -> {
                val num = formatMax3Digits(bytesPerSecond / 1000)
                if (showUnit) "${num}K" else num
            }
            "mbps" -> {
                val num = formatMax3Digits(bytesPerSecond * 8 / 1_000_000)
                if (showUnit) "${num}M" else num
            }
            "MBps" -> {
                val num = formatMax3Digits(bytesPerSecond / 1_000_000)
                if (showUnit) "${num}M" else num
            }
            else -> formatAutoShort(bytesPerSecond, showUnit)
        }
    }

    private fun formatBps(bitsPerSecond: Double, isBits: Boolean = true): String {
        val suffix = if (isBits) "bps" else "B/s"
        return when {
            bitsPerSecond >= 1_000_000_000 ->
                    "${formatMax3Digits(bitsPerSecond / 1_000_000_000)} G$suffix"
            bitsPerSecond >= 1_000_000 -> "${formatMax3Digits(bitsPerSecond / 1_000_000)} M$suffix"
            bitsPerSecond >= 1_000 -> "${formatMax3Digits(bitsPerSecond / 1_000)} K$suffix"
            else -> "${formatMax3Digits(bitsPerSecond)} $suffix"
        }
    }

    private fun formatBpsShort(value: Double, showUnit: Boolean): String {
        return when {
            value >= 1_000_000_000 -> {
                val num = formatMax3Digits(value / 1_000_000_000)
                if (showUnit) "${num}G" else num
            }
            value >= 1_000_000 -> {
                val num = formatMax3Digits(value / 1_000_000)
                if (showUnit) "${num}M" else num
            }
            value >= 1_000 -> {
                val num = formatMax3Digits(value / 1_000)
                if (showUnit) "${num}K" else num
            }
            else -> {
                val num = formatMax3Digits(value)
                if (showUnit) "${num}B" else num
            }
        }
    }

    private fun formatAuto(bytesPerSecond: Double): String {
        return when {
            bytesPerSecond >= 1_000_000 -> "${formatMax3Digits(bytesPerSecond / 1_000_000)} MB/s"
            bytesPerSecond >= 1_000 -> "${formatMax3Digits(bytesPerSecond / 1_000)} KB/s"
            else -> "${formatMax3Digits(bytesPerSecond)} B/s"
        }
    }

    private fun formatAutoShort(bytesPerSecond: Double, showUnit: Boolean): String {
        return when {
            bytesPerSecond >= 1_000_000 -> {
                val num = formatMax3Digits(bytesPerSecond / 1_000_000)
                if (showUnit) "${num}M" else num
            }
            bytesPerSecond >= 1_000 -> {
                val num = formatMax3Digits(bytesPerSecond / 1_000)
                if (showUnit) "${num}K" else num
            }
            else -> {
                val num = formatMax3Digits(bytesPerSecond)
                if (showUnit) "${num}B" else num
            }
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
            "kbps" -> Pair(formatMax3Digits(bytesPerSecond * 8 / 1000), "K")
            "KBps" -> Pair(formatMax3Digits(bytesPerSecond / 1000), "K")
            "mbps" -> Pair(formatMax3Digits(bytesPerSecond * 8 / 1_000_000), "M")
            "MBps" -> Pair(formatMax3Digits(bytesPerSecond / 1_000_000), "M")
            else -> formatAutoShortSplit(bytesPerSecond)
        }
    }

    private fun formatBpsShortSplit(value: Double): Pair<String, String> {
        return when {
            value >= 1_000_000_000 -> Pair(formatMax3Digits(value / 1_000_000_000), "G")
            value >= 1_000_000 -> Pair(formatMax3Digits(value / 1_000_000), "M")
            value >= 1_000 -> Pair(formatMax3Digits(value / 1_000), "K")
            else -> Pair(formatMax3Digits(value), "B")
        }
    }

    private fun formatAutoShortSplit(bytesPerSecond: Double): Pair<String, String> {
        return when {
            bytesPerSecond >= 1_000_000 -> Pair(formatMax3Digits(bytesPerSecond / 1_000_000), "M")
            bytesPerSecond >= 1_000 -> Pair(formatMax3Digits(bytesPerSecond / 1_000), "K")
            else -> Pair(formatMax3Digits(bytesPerSecond), "B")
        }
    }
}
