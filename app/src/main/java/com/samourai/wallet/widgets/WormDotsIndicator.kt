package com.samourai.wallet.widgets

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.LinearLayout.HORIZONTAL
import android.widget.RelativeLayout
import androidx.annotation.StyleableRes
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.samourai.wallet.R

/**
 * samourai-wallet-android
 * based on https://github.com/tommybuonomo/dotsindicator
 */
open class WormDotsIndicator @JvmOverloads constructor(context: Context,
                                                       attrs: AttributeSet? = null,
                                                       defStyleAttr: Int = 0) :
        FrameLayout(context, attrs, defStyleAttr) {
    private val defaultSize: Float = 16f
    private val defaultSpacing: Float = 4f
    var pager: Pager? = null

    companion object {
        const val DEFAULT_POINT_COLOR = Color.CYAN
    }

    @JvmField
    protected val dots = ArrayList<ImageView>()
    @StyleableRes
    var dotsColorId: Int = R.styleable.WormDotsIndicator_dotsColor
    @StyleableRes
    var dotsSizeId: Int = R.styleable.WormDotsIndicator_dotsSize
    @StyleableRes
    var dotsSpacingId: Int = R.styleable.WormDotsIndicator_dotsSpacing
    @StyleableRes
    var dotsCornerRadiusId: Int = R.styleable.WormDotsIndicator_dotsCornerRadius
    @StyleableRes
    var dotsClickableId: Int = R.styleable.WormDotsIndicator_dotsClickable

    var dotsClickable: Boolean = true
    private var dotsColor: Int = DEFAULT_POINT_COLOR
        set(value) {
            field = value
            refreshDotsColors()
        }

    private var dotsSize = dpToPxF(defaultSize)
    private var dotsCornerRadius = dotsSize / 2f
    private var dotsSpacing = dpToPxF(defaultSpacing)


    private var dotIndicatorView: ImageView? = null
    private var dotIndicatorLayout: View? = null

    // Attributes
    private var dotsStrokeWidth: Int = 0
    private var dotIndicatorColor: Int = 0
    private var dotsStrokeColor: Int = 0

    private var dotIndicatorXSpring: SpringAnimation? = null
    private var dotIndicatorWidthSpring: SpringAnimation? = null
    private val strokeDotsLinearLayout: LinearLayout = LinearLayout(context)

    init {
        val linearParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
        val horizontalPadding = dpToPx(24)
        setPadding(horizontalPadding, 0, horizontalPadding, 0)

        clipToPadding = false
        strokeDotsLinearLayout.layoutParams = linearParams
        strokeDotsLinearLayout.orientation = HORIZONTAL
        addView(strokeDotsLinearLayout)

        dotsStrokeWidth = dpToPx(2) // 2dp
        dotIndicatorColor = context.getThemePrimaryColor()
        dotsStrokeColor = dotIndicatorColor

        if (attrs != null) {
            val a = getContext().obtainStyledAttributes(attrs, R.styleable.WormDotsIndicator)

            dotsColor = a.getColor(dotsColorId, DEFAULT_POINT_COLOR)
            dotsSize = a.getDimension(dotsSizeId, dotsSize)

            dotsCornerRadius = a.getDimension(dotsCornerRadiusId, dotsCornerRadius)
            dotsSpacing = a.getDimension(dotsSpacingId, dotsSpacing)
            dotsClickable = a.getBoolean(dotsClickableId, true)

            // Dots attributes
            dotIndicatorColor = a.getColor(R.styleable.WormDotsIndicator_dotsColor, dotIndicatorColor)
            dotsStrokeColor = a.getColor(R.styleable.WormDotsIndicator_dotsStrokeColor, dotsStrokeColor)

            // Spring dots attributes
            dotsStrokeWidth = a.getDimension(R.styleable.WormDotsIndicator_dotsStrokeWidth,
                    dotsStrokeWidth.toFloat()).toInt()

            a.recycle()
        }

        if (isInEditMode) {
            addDots(5)
            addView(buildDot(false))
        }

        setUpDotIndicator()
    }

    private fun setUpDotIndicator() {
        if (pager?.isEmpty == true) {
            return
        }

        if (dotIndicatorView != null && indexOfChild(dotIndicatorView) != -1) {
            removeView(dotIndicatorView)
        }

        dotIndicatorLayout = buildDot(false)
        dotIndicatorView = dotIndicatorLayout!!.findViewById(R.id.worm_dot)
        addView(dotIndicatorLayout)
        dotIndicatorXSpring = SpringAnimation(dotIndicatorLayout, SpringAnimation.TRANSLATION_X)
        val springForceX = SpringForce(0f)
        springForceX.dampingRatio = 1f
        springForceX.stiffness = 300f
        dotIndicatorXSpring!!.spring = springForceX

        val floatPropertyCompat = object : FloatPropertyCompat<View>("DotsWidth") {
            override fun getValue(`object`: View): Float {
                return dotIndicatorView!!.layoutParams.width.toFloat()
            }

            override fun setValue(`object`: View, value: Float) {
                val params = dotIndicatorView!!.layoutParams
                params.width = value.toInt()
                dotIndicatorView!!.requestLayout()
            }
        }
        dotIndicatorWidthSpring = SpringAnimation(dotIndicatorLayout, floatPropertyCompat)
        val springForceWidth = SpringForce(0f)
        springForceWidth.dampingRatio = 1f
        springForceWidth.stiffness = 300f
        dotIndicatorWidthSpring!!.spring = springForceWidth
    }

    private fun addDot(index: Int) {
        val dot = buildDot(true)
        dot.setOnClickListener {
            if (dotsClickable && index < pager?.count ?: 0) {
                pager!!.setCurrentItem(index, true)
            }
        }

        dots.add(dot.findViewById<View>(R.id.worm_dot) as ImageView)
        strokeDotsLinearLayout.addView(dot)
    }

    private fun buildDot(stroke: Boolean): ViewGroup {
        val dot = LayoutInflater.from(context).inflate(R.layout.worm_dot_layout, this,
                false) as ViewGroup
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
            dot.layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        val dotImageView = dot.findViewById<View>(R.id.worm_dot)
        dotImageView.setBackgroundResource(
                if (stroke) R.drawable.worm_dot_stroke_background else R.drawable.worm_dot_background)
        val params = dotImageView.layoutParams as RelativeLayout.LayoutParams
        params.height = dotsSize.toInt()
        params.width = params.height
        params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE)

        params.setMargins(dotsSpacing.toInt(), 0, dotsSpacing.toInt(), 0)

        setUpDotBackground(stroke, dotImageView)
        return dot
    }

    private fun setUpDotBackground(stroke: Boolean, dotImageView: View) {
        val dotBackground = dotImageView.background as GradientDrawable
        if (stroke) {
            dotBackground.setStroke(dotsStrokeWidth, dotsStrokeColor)
        } else {
            dotBackground.setColor(dotIndicatorColor)
        }
        dotBackground.cornerRadius = dotsCornerRadius
    }

    private fun refreshDotColor(index: Int) {
        setUpDotBackground(true, dots[index])
    }

    private fun removeDot(index: Int) {
        strokeDotsLinearLayout.removeViewAt(strokeDotsLinearLayout.childCount - 1)
        dots.removeAt(dots.size - 1)
    }

    /**
     * Set the indicator dot color.
     *
     * @param color the color fo the indicator dot.
     */
    fun setDotIndicatorColor(color: Int) {
        if (dotIndicatorView != null) {
            dotIndicatorColor = color
            setUpDotBackground(false, dotIndicatorView!!)
        }
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        refreshDots()
    }

    private fun refreshDotsCount() {
        if (dots.size < pager!!.count) {
            addDots(pager!!.count - dots.size)
        } else if (dots.size > pager!!.count) {
            removeDots(dots.size - pager!!.count)
        }
    }

    private fun refreshDotsColors() {
        for (i in dots.indices) {
            refreshDotColor(i)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (context.resources.displayMetrics.density * dp).toInt()
    }

    private fun dpToPxF(dp: Float): Float {
        return context.resources.displayMetrics.density * dp
    }

    private fun addDots(count: Int) {
        for (i in 0 until count) {
            addDot(i)
        }
    }

    private fun removeDots(count: Int) {
        for (i in 0 until count) {
            removeDot(i)
        }
    }

    fun refreshDots() {
        if (pager == null) {
            return
        }
        post {
            // Check if we need to refresh the dots count
            refreshDotsCount()
            refreshDotsColors()
            refreshDotsSize()
            refreshOnPageChangedListener()
        }
    }

    private fun refreshOnPageChangedListener() {
        if (pager!!.isNotEmpty) {
            pager!!.removeOnPageChangeListener()
            val onPageChangeListenerHelper = buildOnPageChangedListener()
            pager!!.addOnPageChangeListener(onPageChangeListenerHelper)
            onPageChangeListenerHelper.onPageScrolled(pager!!.currentItem, 0f)
        }
    }

    private fun refreshDotsSize() {
        for (i in 0 until pager!!.currentItem) {
            dots[i].setWidth(dotsSize.toInt())
        }
    }

    /**
     * Set the stroke indicator dots color.
     *
     * @param color the color fo the stroke indicator dots.
     */
    fun setStrokeDotsIndicatorColor(color: Int) {
        dotsStrokeColor = color
        for (v in dots) {
            setUpDotBackground(true, v)
        }
    }


    fun View.setWidth(width: Int) {
        layoutParams.apply {
            this.width = width
            requestLayout()
        }
    }

    fun <T> ArrayList<T>.isInBounds(index: Int) = index in 0 until size

    fun Context.getThemePrimaryColor(): Int {
        val value = TypedValue()
        this.theme.resolveAttribute(R.attr.colorPrimary, value, true)
        return value.data
    }

    protected val ViewPager2.isNotEmpty: Boolean get() = adapter!!.itemCount > 0

    protected val ViewPager?.isEmpty: Boolean
        get() = this != null && this.adapter != null &&
                adapter!!.count == 0

    protected val ViewPager2?.isEmpty: Boolean
        get() = this != null && this.adapter != null &&
                adapter!!.itemCount == 0

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1 && layoutDirection == View.LAYOUT_DIRECTION_RTL) {
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            rotation = 180f
            requestLayout()
        }
    }


    fun setViewPager2(viewPager2: ViewPager2) {
        if (viewPager2.adapter == null) {
            throw IllegalStateException("You have to set an adapter to the view pager before " +
                    "initializing the dots indicator !")
        }


        viewPager2.adapter!!.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                refreshDots()
            }
        })

        pager = object : Pager {
            var onPageChangeCallback: ViewPager2.OnPageChangeCallback? = null

            override val isNotEmpty: Boolean
                get() = viewPager2.isNotEmpty
            override val currentItem: Int
                get() = viewPager2.currentItem
            override val isEmpty: Boolean
                get() = viewPager2.isEmpty
            override val count: Int
                get() = viewPager2.adapter?.itemCount ?: 0

            override fun setCurrentItem(item: Int, smoothScroll: Boolean) {
                viewPager2.setCurrentItem(item, smoothScroll)
            }

            override fun removeOnPageChangeListener() {
                onPageChangeCallback?.let { viewPager2.unregisterOnPageChangeCallback(it) }
            }

            override fun addOnPageChangeListener(
                    onPageChangeListenerHelper: OnPageChangeListenerHelper) {
                onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageScrolled(position: Int, positionOffset: Float,
                                                positionOffsetPixels: Int) {
                        super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                        onPageChangeListenerHelper.onPageScrolled(position, positionOffset)
                    }
                }
                viewPager2.registerOnPageChangeCallback(onPageChangeCallback!!)
            }
        }

        refreshDots()
    }

    private fun buildOnPageChangedListener(): OnPageChangeListenerHelper {
        return object : OnPageChangeListenerHelper() {

            override val pageCount: Int
                get() = dots.size

            override fun onPageScrolled(selectedPosition: Int, nextPosition: Int, positionOffset: Float) {
                val x = (dots[selectedPosition].parent as ViewGroup).left.toFloat()
                val nextX = (dots[if (nextPosition == -1) selectedPosition else nextPosition].parent as ViewGroup).left
                        .toFloat()
                val xFinalPosition: Float
                val widthFinalPosition: Float

                when (positionOffset) {
                    in 0.0f..0.1f -> {
                        xFinalPosition = x
                        widthFinalPosition = dotsSize
                    }
                    in 0.1f..0.9f -> {
                        xFinalPosition = x
                        widthFinalPosition = nextX - x + dotsSize
                    }
                    else -> {
                        xFinalPosition = nextX
                        widthFinalPosition = dotsSize
                    }
                }

                dotIndicatorXSpring?.animateToFinalPosition(xFinalPosition)
                dotIndicatorWidthSpring?.animateToFinalPosition(widthFinalPosition)
            }

            override fun resetPosition(position: Int) {
                // Empty
            }
        }
    }


    interface Pager {
        val isNotEmpty: Boolean
        val currentItem: Int
        val isEmpty: Boolean
        val count: Int
        fun setCurrentItem(item: Int, smoothScroll: Boolean)
        fun removeOnPageChangeListener()
        fun addOnPageChangeListener(onPageChangeListenerHelper: OnPageChangeListenerHelper)
    }

    abstract class OnPageChangeListenerHelper {
        private var lastLeftPosition: Int = -1
        private var lastRightPosition: Int = -1

        internal abstract val pageCount: Int

        fun onPageScrolled(position: Int, positionOffset: Float) {
            var offset = (position + positionOffset)
            val lastPageIndex = (pageCount - 1).toFloat()
            if (offset == lastPageIndex) {
                offset = lastPageIndex - .0001f
            }
            val leftPosition = offset.toInt()
            val rightPosition = leftPosition + 1

            if (rightPosition > lastPageIndex || leftPosition == -1) {
                return
            }

            onPageScrolled(leftPosition, rightPosition, offset % 1)

            if (lastLeftPosition != -1) {
                if (leftPosition > lastLeftPosition) {
                    (lastLeftPosition until leftPosition).forEach {
                        resetPosition(it)
                    }
                }

                if (rightPosition < lastRightPosition) {
                    resetPosition(lastRightPosition)
                    ((rightPosition + 1)..lastRightPosition).forEach {
                        resetPosition(it)
                    }
                }
            }

            lastLeftPosition = leftPosition
            lastRightPosition = rightPosition
        }

        internal abstract fun onPageScrolled(selectedPosition: Int, nextPosition: Int,
                                             positionOffset: Float)

        internal abstract fun resetPosition(position: Int)
    }


}