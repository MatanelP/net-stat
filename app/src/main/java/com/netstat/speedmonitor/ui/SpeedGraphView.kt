package com.netstat.speedmonitor.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.netstat.speedmonitor.R

class SpeedGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var downloadData = listOf<Float>()
    private var uploadData = listOf<Float>()
    private var animationProgress = 1f
    private val maxDataPoints = 80
    var showDownload = true
    var showUpload = true

    private val dp = resources.displayMetrics.density

    private val downloadLineColor = ContextCompat.getColor(context, R.color.download_color)
    private val uploadLineColor = ContextCompat.getColor(context, R.color.upload_color)

    private val downloadLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * dp
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = downloadLineColor
    }

    private val uploadLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * dp
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = uploadLineColor
    }

    private val downloadFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val uploadFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 0.5f * dp
        color = ContextCompat.getColor(context, R.color.graph_grid)
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 9f * resources.displayMetrics.density
        color = ContextCompat.getColor(context, R.color.graph_label)
        textAlign = Paint.Align.RIGHT
    }

    private val linePath = Path()
    private val fillPath = Path()

    fun setData(download: List<Float>, upload: List<Float>) {
        this.downloadData = download
        this.uploadData = upload
        invalidate()
    }

    fun animateIn() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            interpolator = DecelerateInterpolator(2f)
            addUpdateListener {
                animationProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val labelWidth = 30f * dp
        val paddingH = 4f * dp
        val paddingV = 8f * dp
        val graphLeft = paddingH + labelWidth
        val graphRight = width.toFloat() - paddingH
        val graphTop = paddingV
        val graphBottom = height.toFloat() - paddingV
        val graphWidth = graphRight - graphLeft
        val graphHeight = graphBottom - graphTop

        val allValues = downloadData + uploadData
        val maxValue = (allValues.maxOrNull() ?: 0f).coerceAtLeast(1024f)

        drawGrid(canvas, graphLeft, graphTop, graphRight, graphHeight, maxValue)

        if (showUpload && uploadData.isNotEmpty()) {
            updateGradient(uploadFillPaint, uploadLineColor, graphTop, graphBottom, 30)
            drawCurve(
                canvas, uploadData, uploadLinePaint, uploadFillPaint,
                graphTop, graphRight, graphBottom, graphWidth, graphHeight, maxValue
            )
        }

        if (showDownload && downloadData.isNotEmpty()) {
            updateGradient(downloadFillPaint, downloadLineColor, graphTop, graphBottom, 45)
            drawCurve(
                canvas, downloadData, downloadLinePaint, downloadFillPaint,
                graphTop, graphRight, graphBottom, graphWidth, graphHeight, maxValue
            )
        }
    }

    private fun updateGradient(paint: Paint, color: Int, top: Float, bottom: Float, alpha: Int) {
        val a = (alpha * animationProgress).toInt()
        paint.shader = LinearGradient(
            0f, top, 0f, bottom,
            Color.argb(a, Color.red(color), Color.green(color), Color.blue(color)),
            Color.argb(0, Color.red(color), Color.green(color), Color.blue(color)),
            Shader.TileMode.CLAMP
        )
    }

    private fun drawGrid(
        canvas: Canvas, left: Float, top: Float, right: Float,
        graphHeight: Float, maxValue: Float
    ) {
        val gridLines = 3
        for (i in 0..gridLines) {
            val y = top + graphHeight * i / gridLines
            canvas.drawLine(left, y, right, y, gridPaint)
            if (i < gridLines) {
                val value = maxValue * (gridLines - i) / gridLines
                canvas.drawText(formatGridLabel(value), left - 4f * dp, y + 4f * dp, labelPaint)
            }
        }
    }

    private fun formatGridLabel(bytesPerSecond: Float): String {
        return when {
            bytesPerSecond >= 1_000_000 -> String.format("%.0fM", bytesPerSecond / 1_000_000)
            bytesPerSecond >= 1_000 -> String.format("%.0fK", bytesPerSecond / 1_000)
            else -> String.format("%.0fB", bytesPerSecond)
        }
    }

    private fun drawCurve(
        canvas: Canvas, data: List<Float>,
        linePaint: Paint, fillPaint: Paint,
        graphTop: Float, graphRight: Float, graphBottom: Float,
        graphWidth: Float, graphHeight: Float, maxValue: Float
    ) {
        if (data.size < 2) return

        linePath.reset()
        fillPath.reset()

        val visibleCount = (data.size * animationProgress).toInt().coerceAtLeast(2)
        val points = data.takeLast(visibleCount)

        val stepX = graphWidth / (maxDataPoints - 1).coerceAtLeast(1)
        val startX = graphRight - (points.size - 1) * stepX

        val coords = points.mapIndexed { index, value ->
            val x = startX + index * stepX
            val y = graphBottom - (value / maxValue) * graphHeight
            PointF(x, y.coerceIn(graphTop, graphBottom))
        }

        linePath.moveTo(coords[0].x, coords[0].y)
        fillPath.moveTo(coords[0].x, graphBottom)
        fillPath.lineTo(coords[0].x, coords[0].y)

        for (i in 1 until coords.size) {
            val prev = coords[i - 1]
            val curr = coords[i]
            val cpx = (prev.x + curr.x) / 2f
            linePath.cubicTo(cpx, prev.y, cpx, curr.y, curr.x, curr.y)
            fillPath.cubicTo(cpx, prev.y, cpx, curr.y, curr.x, curr.y)
        }

        fillPath.lineTo(coords.last().x, graphBottom)
        fillPath.close()

        linePaint.alpha = (255 * animationProgress).toInt()
        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(linePath, linePaint)
    }
}
