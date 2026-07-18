package com.vibenav

import android.content.Context
import android.graphics.*
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class TutorialOverlay(context: Context, private val steps: List<Step>) : FrameLayout(context) {

    data class Step(val viewId: Int, val title: String, val description: String)

    private var currentIndex = 0
    private var onFinished: (() -> Unit)? = null

    private val titleView = TextView(context)
    private val descView = TextView(context)
    private val counterText = TextView(context)
    private val prevButton = Button(context)
    private val nextButton = Button(context)
    private val skipButton = TextView(context)
    private val dotIndicator = LinearLayout(context)

    private val holeView = object : View(context) {
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val step = steps.getOrNull(currentIndex) ?: return
            val target = findViewById<View>(step.viewId) ?: return

            val loc = IntArray(2)
            target.getLocationInWindow(loc)
            val parentLoc = IntArray(2)
            (parent as? View)?.getLocationInWindow(parentLoc)

            val ox = parentLoc[0]
            val oy = parentLoc[1]

            val pad = 16
            val left = loc[0] - ox - pad
            val top = loc[1] - oy - pad
            val right = left + target.width + pad * 2
            val bottom = top + target.height + pad * 2

            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }

            canvas.save()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                canvas.clipOutRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
            }
            canvas.drawColor(Color.argb(200, 0, 0, 0))
            canvas.restore()

            canvas.drawRoundRect(
                left.toFloat(), top.toFloat(),
                right.toFloat(), bottom.toFloat(),
                12f, 12f, strokePaint
            )
        }
    }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        setBackgroundColor(Color.TRANSPARENT)
        isClickable = true
        isFocusable = true

        addView(holeView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        val bottomBar = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 32)
            background = null
        }
        val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        bottomBar.layoutParams = params
        (bottomBar.layoutParams as? FrameLayout.LayoutParams)?.gravity = Gravity.BOTTOM
        addView(bottomBar)

        titleView.apply {
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            textAlignment = TEXT_ALIGNMENT_CENTER
        }
        bottomBar.addView(titleView)

        descView.apply {
            textSize = 15f
            setTextColor(Color.argb(220, 255, 255, 255))
            textAlignment = TEXT_ALIGNMENT_CENTER
            setPadding(0, 8, 0, 0)
        }
        bottomBar.addView(descView)

        dotIndicator.apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 12)
        }
        bottomBar.addView(dotIndicator)

        val navRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        bottomBar.addView(navRow)

        prevButton.apply {
            text = "← Back"
            setTextColor(Color.WHITE)
            textSize = 14f
            setBackgroundColor(Color.argb(80, 255, 255, 255))
            setPadding(24, 12, 24, 12)
            setOnClickListener { goToStep(currentIndex - 1) }
        }
        navRow.addView(prevButton)

        val spacer = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }
        navRow.addView(spacer)

        nextButton.apply {
            text = "Next →"
            setTextColor(Color.WHITE)
            textSize = 14f
            setBackgroundColor(Color.argb(80, 255, 255, 255))
            setPadding(24, 12, 24, 12)
            setOnClickListener {
                if (currentIndex < steps.size - 1) {
                    goToStep(currentIndex + 1)
                } else {
                    dismiss()
                    onFinished?.invoke()
                }
            }
        }
        navRow.addView(nextButton)

        skipButton.apply {
            text = "Skip tutorial"
            setTextColor(Color.argb(150, 255, 255, 255))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 0)
            setOnClickListener { dismiss(); onFinished?.invoke() }
        }
        bottomBar.addView(skipButton)

        goToStep(0)
    }

    fun setOnFinished(callback: () -> Unit) { onFinished = callback }

    private fun goToStep(index: Int) {
        currentIndex = index.coerceIn(0, steps.size - 1)
        val step = steps[currentIndex]
        titleView.text = step.title
        descView.text = step.description
        prevButton.visibility = if (currentIndex == 0) View.INVISIBLE else View.VISIBLE
        nextButton.text = if (currentIndex < steps.size - 1) "Next →" else "Done ✓"
        updateDots()
        holeView.invalidate()
    }

    private fun updateDots() {
        dotIndicator.removeAllViews()
        for (i in steps.indices) {
            val dot = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(10, 10).apply {
                    setMargins(4, 0, 4, 0)
                }
                background = if (i == currentIndex) {
                    android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.OVAL
                        setSize(10, 10)
                        setColor(Color.WHITE)
                    }
                } else {
                    android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.OVAL
                        setSize(10, 10)
                        setColor(Color.argb(100, 255, 255, 255))
                    }
                }
            }
            dotIndicator.addView(dot)
        }
    }

    private fun dismiss() {
        (parent as? ViewGroup)?.removeView(this)
    }
}
