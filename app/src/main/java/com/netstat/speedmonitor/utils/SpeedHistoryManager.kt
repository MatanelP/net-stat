package com.netstat.speedmonitor.utils

class SpeedHistoryManager(private val maxDataPoints: Int = 80) {

    private val downloadHistory = ArrayDeque<Float>()
    private val uploadHistory = ArrayDeque<Float>()

    fun addDataPoint(downloadSpeed: Double, uploadSpeed: Double) {
        if (downloadHistory.size >= maxDataPoints) {
            downloadHistory.removeFirst()
            uploadHistory.removeFirst()
        }
        downloadHistory.addLast(downloadSpeed.toFloat())
        uploadHistory.addLast(uploadSpeed.toFloat())
    }

    fun getDownloadHistory(): List<Float> = downloadHistory.toList()
    fun getUploadHistory(): List<Float> = uploadHistory.toList()

    fun clear() {
        downloadHistory.clear()
        uploadHistory.clear()
    }
}
