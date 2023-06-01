package a

import android.animation.Animator
import a.B
import android.animation.ObjectAnimator
import com.xuexiang.xui.widget.progress.HorizontalProgressView
import android.content.res.TypedArray
import a.B.AnimateType
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.*
import android.view.animation.Interpolator
import androidx.annotation.ColorInt
import androidx.annotation.IntDef
import com.xuexiang.xui.R
import com.xuexiang.xui.utils.DensityUtils
import com.xuexiang.xui.utils.ThemeUtils
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

class B @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.HorizontalProgressViewStyle
) : View(context, attrs, defStyleAttr) {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        ACCELERATE_DECELERATE_INTERPOLATOR,
        LINEAR_INTERPOLATOR,
        ACCELERATE_INTERPOLATOR,
        DECELERATE_INTERPOLATOR,
        OVERSHOOT_INTERPOLATOR
    )
    private annotation class AnimateType

    /**
     * the type of animation
     */
    private var mAnimateType = 0

    /**
     * the progress of start point
     */
    private var mStartProgress = 0f

    /**
     * the progress of end point
     */
    private var mEndProgress = 60f

    /**
     * the color of start progress
     */
    private var mStartColor = resources.getColor(R.color.xui_config_color_light_orange)

    /**
     * the color of end progress
     */
    private var mEndColor = resources.getColor(R.color.xui_config_color_dark_orange)

    /**
     * has track of moving or not
     */
    private var trackEnabled = false

    /**
     * the stroke width of progress
     */
    private var mTrackWidth = 6

    /**
     * the size of inner text
     */
    private var mProgressTextSize = 48

    /**
     * the color of inner text
     */
    private var mProgressTextColor = 0

    /**
     * the color of progress track
     */
    private var mTrackColor = resources.getColor(R.color.default_pv_track_color)

    /**
     * the duration of progress moving
     */
    private var mProgressDuration = 1200

    /**
     * show the inner text or not
     */
    private var textVisibility = true

    /**
     * the round rect corner radius
     */
    private var mCornerRadius = 30

    /**
     * the offset of text padding bottom
     */
    private var mTextPaddingBottomOffset = -5

    /**
     * moving the text with progress or not
     */
    private var isTextMoved = true

    /**
     * the animator of progress moving
     */
    private var progressAnimator: ObjectAnimator? = null

    /**
     * the progress of moving
     */
    private var moveProgress = 0f

    /**
     * the paint of drawing progress
     */
    private var progressPaint: Paint? = null

    /**
     * the gradient of color
     */
    private var mShader: LinearGradient? = null

    /**
     * the oval's rect shape
     */
    private var mRect: RectF? = null
    private var mTrackRect: RectF? = null
    private var mInterpolator: Interpolator? = null
    private var animatorUpdateListener: HorizontalProgressView.HorizontalProgressUpdateListener? =
        null

    private fun obtainAttrs(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.HorizontalProgressView,
            defStyleAttr,
            0
        )
        mStartProgress =
            typedArray.getInt(R.styleable.HorizontalProgressView_hpv_start_progress, 0).toFloat()
        mEndProgress =
            typedArray.getInt(R.styleable.HorizontalProgressView_hpv_end_progress, 60).toFloat()
        mStartColor = typedArray.getColor(
            R.styleable.HorizontalProgressView_hpv_start_color, resources.getColor(
                R.color.xui_config_color_light_orange
            )
        )
        mEndColor = typedArray.getColor(
            R.styleable.HorizontalProgressView_hpv_end_color, resources.getColor(
                R.color.xui_config_color_dark_orange
            )
        )
        trackEnabled =
            typedArray.getBoolean(R.styleable.HorizontalProgressView_hpv_isTracked, false)
        mProgressTextColor = typedArray.getColor(
            R.styleable.HorizontalProgressView_hpv_progress_textColor,
            ThemeUtils.getMainThemeColor(getContext())
        )
        mProgressTextSize = typedArray.getDimensionPixelSize(
            R.styleable.HorizontalProgressView_hpv_progress_textSize,
            resources.getDimensionPixelSize(
                R.dimen.default_pv_horizontal_text_size
            )
        )
        mTrackWidth = typedArray.getDimensionPixelSize(
            R.styleable.HorizontalProgressView_hpv_track_width, resources.getDimensionPixelSize(
                R.dimen.default_pv_trace_width
            )
        )
        mAnimateType = typedArray.getInt(
            R.styleable.HorizontalProgressView_hpv_animate_type,
            ACCELERATE_DECELERATE_INTERPOLATOR
        )
        mTrackColor = typedArray.getColor(
            R.styleable.HorizontalProgressView_hpv_track_color, resources.getColor(
                R.color.default_pv_track_color
            )
        )
        textVisibility = typedArray.getBoolean(
            R.styleable.HorizontalProgressView_hpv_progress_textVisibility,
            true
        )
        mProgressDuration =
            typedArray.getInt(R.styleable.HorizontalProgressView_hpv_progress_duration, 1200)
        mCornerRadius = typedArray.getDimensionPixelSize(
            R.styleable.HorizontalProgressView_hpv_corner_radius, resources.getDimensionPixelSize(
                R.dimen.default_pv_corner_radius
            )
        )
        //        mTextPaddingBottomOffset = typedArray.getDimensionPixelSize(com.xuexiang.xui.R.styleable.HorizontalProgressView_hpv_text_padding_bottom, getResources().getDimensionPixelSize(com.xuexiang.xui.R.dimen.default_pv_corner_radius));
        isTextMoved =
            typedArray.getBoolean(R.styleable.HorizontalProgressView_hpv_text_movedEnable, true)
        typedArray.recycle()
        moveProgress = mStartProgress
        setAnimateType(mAnimateType)
    }

    private fun init() {
        progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        progressPaint!!.style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        updateTheTrack()
        drawTrack(canvas)
        progressPaint!!.shader = mShader
        canvas.drawRoundRect(
            mRect!!,
            mCornerRadius.toFloat(),
            mCornerRadius.toFloat(),
            progressPaint!!
        )
        drawProgressText(canvas)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mShader = LinearGradient(
            paddingLeft - 50f,
            height - paddingTop - 50f,
            (width - paddingRight).toFloat(),
            height / 2f + paddingTop + mTrackWidth,
            mStartColor,
            mEndColor,
            Shader.TileMode.CLAMP
        )
    }

    /**
     * draw the track(moving background)
     *
     * @param canvas mCanvas
     */
    private fun drawTrack(canvas: Canvas) {
        if (trackEnabled) {
            progressPaint!!.shader = null
            progressPaint!!.color = mTrackColor
            canvas.drawRoundRect(
                mTrackRect!!,
                mCornerRadius.toFloat(),
                mCornerRadius.toFloat(),
                progressPaint!!
            )
        }
    }

    /**
     * draw the progress text
     *
     * @param canvas mCanvas
     */
    private fun drawProgressText(canvas: Canvas) {
        if (textVisibility) {
            val mRingPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val mCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val mCircleRadius = 12f
            val mRingWidth = 8f
            val mRingRadius = 18f
            // 初始化圆环的画笔
            mRingPaint.style = Paint.Style.STROKE
            mRingPaint.strokeWidth = mRingWidth
            mRingPaint.color =
                context.resources.getColor(op.asd.R.color.outer_side_of_ring)
            mCirclePaint.style = Paint.Style.FILL
            mCirclePaint.color = Color.WHITE
            if (isTextMoved) {
                // 绘制圆
                canvas.drawArc(
                    (width - paddingLeft - paddingRight - DensityUtils.dp2px(
                        context, 28f
                    )) * (moveProgress / 100) + DensityUtils.dp2px(
                        context, 10f
                    ) - mRingRadius,
                    height / 2f - paddingTop - mTextPaddingBottomOffset - mRingRadius,
                    (width - paddingLeft - paddingRight - DensityUtils.dp2px(
                        context, 28f
                    )) * (moveProgress / 100) + DensityUtils.dp2px(
                        context, 10f
                    ) + mRingRadius,
                    height / 2f - paddingTop - mTextPaddingBottomOffset + mRingRadius,
                    0f,
                    360f,
                    false,
                    mRingPaint
                )
                canvas.drawCircle(
                    (width - paddingLeft - paddingRight - DensityUtils.dp2px(
                        context, 28f
                    )) * (moveProgress / 100) + DensityUtils.dp2px(
                        context, 10f
                    ),
                    height / 2f - paddingTop - mTextPaddingBottomOffset,
                    mRingRadius - mRingWidth / 2,
                    mCirclePaint
                )
            } else {
                // 绘制圆
                canvas.drawArc(
                    (width - paddingLeft) / 2f - mRingRadius,
                    height / 2f - paddingTop - mTextPaddingBottomOffset - mRingRadius,
                    (width - paddingLeft) / 2f + mRingRadius,
                    height / 2f - paddingTop - mTextPaddingBottomOffset + mRingRadius,
                    0f,
                    360f,
                    false,
                    mRingPaint
                )
                canvas.drawCircle(
                    (width - paddingLeft) / 2f,
                    height / 2f - paddingTop - mTextPaddingBottomOffset,
                    mRingRadius - mRingWidth / 2,
                    mCirclePaint
                )
            }
        }
    }

    /**
     * set progress animate type
     *
     * @param type anim type
     */
    fun setAnimateType(@AnimateType type: Int) {
        mAnimateType = type
        setObjectAnimatorType(type)
    }

    /**
     * set object animation type by received
     *
     * @param animatorType object anim type
     */
    private fun setObjectAnimatorType(animatorType: Int) {
        when (animatorType) {
            ACCELERATE_DECELERATE_INTERPOLATOR -> {
                if (mInterpolator != null) {
                    mInterpolator = null
                }
                mInterpolator = AccelerateDecelerateInterpolator()
            }
            LINEAR_INTERPOLATOR -> {
                if (mInterpolator != null) {
                    mInterpolator = null
                }
                mInterpolator = LinearInterpolator()
            }
            ACCELERATE_INTERPOLATOR -> if (mInterpolator != null) {
                mInterpolator = null
                mInterpolator = AccelerateInterpolator()
            }
            DECELERATE_INTERPOLATOR -> {
                if (mInterpolator != null) {
                    mInterpolator = null
                }
                mInterpolator = DecelerateInterpolator()
            }
            OVERSHOOT_INTERPOLATOR -> {
                if (mInterpolator != null) {
                    mInterpolator = null
                }
                mInterpolator = OvershootInterpolator()
            }
            else -> {
            }
        }
    }

    /**
     * set move progress
     *
     * @param progress progress of moving
     */
    var progress: Float
        get() = moveProgress
        set(progress) {
            moveProgress = progress
            refreshTheView()
        }

    /**
     * set start progress
     *
     * @param startProgress start progress
     */
    fun setStartProgress(startProgress: Float) {
        require(!(startProgress < 0 || startProgress > 100)) { "Illegal progress value, please change it!" }
        mStartProgress = startProgress
        moveProgress = mStartProgress
        refreshTheView()
    }

    /**
     * set end progress
     *
     * @param endProgress end progress
     */
    fun setEndProgress(endProgress: Float) {
        require(!(endProgress < 0 || endProgress > 100)) { "Illegal progress value, please change it!" }
        mEndProgress = endProgress
        refreshTheView()
    }

    /**
     * set start color
     *
     * @param startColor start point color
     */
    fun setStartColor(@ColorInt startColor: Int) {
        mStartColor = startColor
        mShader = LinearGradient(
            paddingLeft - 50f,
            height - paddingTop - 50f,
            (width - paddingRight).toFloat(),
            height / 2f + paddingTop + mTrackWidth,
            mStartColor,
            mEndColor,
            Shader.TileMode.CLAMP
        )
        refreshTheView()
    }

    /**
     * set end color
     *
     * @param endColor end point color
     */
    fun setEndColor(@ColorInt endColor: Int) {
        mEndColor = endColor
        mShader = LinearGradient(
            paddingLeft - 50f,
            height - paddingTop - 50f,
            (width - paddingRight).toFloat(),
            height / 2f + paddingTop + mTrackWidth,
            mStartColor,
            mEndColor,
            Shader.TileMode.CLAMP
        )
        refreshTheView()
    }

    /**
     * set the width of progress stroke
     *
     * @param width stroke
     */
    fun setTrackWidth(width: Int) {
        mTrackWidth = DensityUtils.dp2px(context, width.toFloat())
        refreshTheView()
    }

    /**
     * set track color for progress background
     *
     * @param color bg color
     */
    fun setTrackColor(@ColorInt color: Int) {
        mTrackColor = color
        refreshTheView()
    }

    /**
     * set text color for progress text
     *
     * @param textColor
     */
    fun setProgressTextColor(@ColorInt textColor: Int) {
        mProgressTextColor = textColor
    }

    /**
     * set text size for inner text
     *
     * @param size text size
     */
    fun setProgressTextSize(size: Int) {
        mProgressTextSize = DensityUtils.sp2px(context, size.toFloat())
        refreshTheView()
    }

    /**
     * set duration of progress moving
     *
     * @param duration
     */
    fun setProgressDuration(duration: Int) {
        mProgressDuration = duration
    }

    /**
     * set track for progress
     *
     * @param trackAble whether track or not
     */
    fun setTrackEnabled(trackAble: Boolean) {
        trackEnabled = trackAble
        refreshTheView()
    }

    /**
     * set the visibility for progress inner text
     *
     * @param visibility text visible or not
     */
    fun setProgressTextVisibility(visibility: Boolean) {
        textVisibility = visibility
        refreshTheView()
    }

    /**
     * set progress text moving with progress view or not
     *
     * @param moved
     */
    fun setProgressTextMoved(moved: Boolean) {
        isTextMoved = moved
    }

    /**
     * start the progress's moving
     */
    fun startProgressAnimation() {
        progressAnimator = ObjectAnimator.ofFloat(this, "progress", mStartProgress, mEndProgress)
        progressAnimator?.interpolator = mInterpolator
        progressAnimator?.duration = mProgressDuration.toLong()
        progressAnimator?.addUpdateListener(AnimatorUpdateListener { animation ->
            val progress = animation.getAnimatedValue("progress") as Float
            if (animatorUpdateListener != null) {
                animatorUpdateListener!!.onHorizontalProgressUpdate(
                    this@B,
                    progress
                )
            }
        })
        progressAnimator?.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animator: Animator) {
                if (animatorUpdateListener != null) {
                    animatorUpdateListener!!.onHorizontalProgressStart(this@B)
                }
            }

            override fun onAnimationEnd(animator: Animator) {
                if (animatorUpdateListener != null) {
                    animatorUpdateListener!!.onHorizontalProgressFinished(this@B)
                }
            }

            override fun onAnimationCancel(animator: Animator) {}
            override fun onAnimationRepeat(animator: Animator) {}
        })
        progressAnimator?.start()
    }

    /**
     * stop the progress moving
     */
    fun stopProgressAnimation() {
        if (progressAnimator != null) {
            progressAnimator!!.cancel()
            progressAnimator = null
        }
    }

    /**
     * set the corner radius for the rect of progress
     *
     * @param radius the corner radius
     */
    fun setProgressCornerRadius(radius: Int) {
        mCornerRadius = DensityUtils.dp2px(context, radius.toFloat())
        refreshTheView()
    }

    /**
     * set the text padding bottom offset
     *
     * @param offset the value of padding bottom
     */
    fun setProgressTextPaddingBottom(offset: Int) {
        mTextPaddingBottomOffset = DensityUtils.dp2px(context, offset.toFloat())
    }

    /**
     * refresh the layout
     */
    private fun refreshTheView() {
        invalidate()
        //requestLayout();
    }

    /**
     * update the oval progress track
     */
    private fun updateTheTrack() {
        mRect = RectF(
            paddingLeft + mStartProgress * (width - paddingLeft - paddingRight + 60) / 100,
            height / 2f - paddingTop,
            (width - paddingRight - 20) * (moveProgress / 100),
            height / 2f + paddingTop + mTrackWidth
        )
        mTrackRect = RectF(
            paddingLeft.toFloat(), height / 2f - paddingTop,
            (width - paddingRight - 20).toFloat(),
            height / 2f + paddingTop + mTrackWidth
        )
    }

    /**
     * 进度条更新监听
     */
    interface HorizontalProgressUpdateListener {
        /**
         * 进度条开始更新
         *
         * @param view
         */
        fun onHorizontalProgressStart(view: View?)

        /**
         * 进度条更新中
         *
         * @param view
         * @param progress
         */
        fun onHorizontalProgressUpdate(view: View?, progress: Float)

        /**
         * 进度条更新结束
         *
         * @param view
         */
        fun onHorizontalProgressFinished(view: View?)
    }

    /**
     * set the progress update listener for progress view
     *
     * @param listener update listener
     */
    fun setProgressViewUpdateListener(listener: HorizontalProgressView.HorizontalProgressUpdateListener?) {
        animatorUpdateListener = listener
    }

    companion object {
        /**
         * animation types supported
         */
        const val ACCELERATE_DECELERATE_INTERPOLATOR = 0
        const val LINEAR_INTERPOLATOR = 1
        const val ACCELERATE_INTERPOLATOR = 2
        const val DECELERATE_INTERPOLATOR = 3
        const val OVERSHOOT_INTERPOLATOR = 4
    }

    init {
        obtainAttrs(context, attrs, defStyleAttr)
        init()
    }
}