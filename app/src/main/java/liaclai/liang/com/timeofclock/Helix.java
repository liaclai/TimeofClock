package liaclai.liang.com.timeofclock;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by liaclai on 2016/6/13.
 */
public class Helix extends View {

    private String TAG = "Helix";

    private volatile int count = 1;


    //helix
    private int r_begin;
    private int r_increase;
    private int angle_begin;
    private int angle_increase_end;
    private float angle_increase;
    private float angle;
    private float r;
    private int border;

    private Paint mPaint = new Paint();
    private Path mPath = new Path();

    private int mHeight;
    private int mWidth;


    //animator
    private ValueAnimator animator;
    private float animatedValue;
    private long animatorDuration = 5000;
    private TimeInterpolator timeInterpolator = new LinearInterpolator();

    //touch
    private boolean touchFlag = true;
    private double beforeLength = 0, afterLength = 0;    // 两触点距离
    private List<Point> points_temp = new ArrayList<Point>();    //临时坐标点集合

    private float scale = 1;    //当前的放大倍数
    private static final int scale_max = 10;    //scale的最大值
    private static final int scale_min = 1;        //scale的最小值

    //clock
    private Path pathDim = new Path();
    private Path pathHand = new Path();
    private PathMeasure mMeasure = new PathMeasure();
    private Paint paintNum = new Paint();
    private Paint paintBigNum = new Paint();
    private Paint paintDim = new Paint();
    private Paint paintBigDim = new Paint();
    private Paint paintLargeDim = new Paint();
    private Paint hand = new Paint();
    private Paint.FontMetrics fontMetrics;
    private int mit;
    private int min;
    private int angmin = 15;
    private Calendar mCalendar;

    //时钟各标志位flag
    private int flagMinute;
    private int flagFiveMin;
    private int flagHour;
    private int flagDay;
    private int flagWeek;
    private int flagMonth;
    private int flagYear;
    private int flagDimMinute;
    private int flagDimFiveMin;
    private int flagDimHour;
    private int flagDimDay;
    private int flagDimWeek;
    private int flagDimMonth;
    private int flagDimYear;
    private int flagNumFiveMin;
    private int flagNumHour;
    private int flagNumDay;
    private int flagNumWeek;
    private int flagNumMonth;
    private int flagNumYear;

    //各个paint
    private Paint paintMinDim;
    private Paint paintFiveMinDim;
    private Paint paintHourDim;
    private Paint paintDayDim;
    private Paint paintWeekDim;
    private Paint paintMonthDim;
    private Paint paintMinNum;
    private Paint paintFiveMinNum;
    private Paint paintHourNum;
    private Paint paintDayNum;
    private Paint paintWeekNum;
    private Paint paintMonthNum;

    public Helix(Context context) {
        this(context, null);
    }

    public Helix(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.helix_style);
    }

    public Helix(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.helix, defStyleAttr, 0);
        int n = array.getIndexCount();
        for (int i = 0; i < n; i++) {
            int attr = array.getIndex(i);
            switch (attr) {
                case R.styleable.helix_r_begin:
                    r_begin = array.getDimensionPixelSize(attr, 0);
                    Log.d(TAG, "r_begin = " + r_begin);
                    break;
                case R.styleable.helix_r_increase:
                    r_increase = array.getDimensionPixelSize(attr, 0);
                    Log.d(TAG, "r_increase = " + r_increase);
                    break;
                case R.styleable.helix_angle_begin:
                    angle_begin = array.getInt(attr, 0);
                    Log.d(TAG, "angle_begin = " + angle_begin);
                    break;
                case R.styleable.helix_angle_increase_end:
                    angle_increase_end = array.getInt(attr, 0);
                    Log.d(TAG, "angle_increase = " + angle_increase_end);
                    break;
            }
        }
        array.recycle();
        initPaint();
        initAnimator(animatorDuration);
    }


    /************************
     * 生命流程部分
     ******************/

    @Override
    protected void onSizeChanged(int w, int h, int oidw, int oidh) {
        super.onSizeChanged(w, h, oidw, oidh);
        mWidth = w - getPaddingLeft() - getPaddingRight();
        mHeight = h - getPaddingBottom() - getPaddingTop();
        border = Math.min(mWidth, mHeight) / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        initFlag();
        initClockPaint();
        canvas.translate(mWidth / 2, mHeight / 2);
        int multiple = 1;
        r = r_begin / scale;
        angle = angle_begin;
        mit = 0;
        mCalendar = Calendar.getInstance();
        min = mCalendar.get(Calendar.MINUTE);
        int hour = mCalendar.get(Calendar.HOUR_OF_DAY);
        int day = mCalendar.get(Calendar.DAY_OF_MONTH);
//        动画（已取消）
//        int a =(int) ((border -r_begin / scale) * animatedValue/3000 + r );
        int a = (int) ((border - r_begin / scale) + r);
        for (int sca = (int) scale; sca > 0; sca--) {
            if (sca >= 3) {
                multiple = 5;
                min -=min%5;
            }
            else {
                multiple = 1;
            }
            while (r < a / sca) {
                RectF rectF = new RectF(-r, -r, r, r);
                mPath.addArc(rectF, angle % 360, angle_increase_end * border / r / scale * multiple);

                //指针
                if (flagMinute == 1) {
                    canvas.drawLine(0, 0, 0, (r_begin / scale) * 0.8f, hand);
                    canvas.drawLines(new float[]{
                            0, (r_begin / scale) * 0.8f, (r_begin / scale) * 0.8f * 0.2f, (r_begin / scale) * 0.8f - (r_begin / scale) * 0.8f * 0.2f,
                            0, (r_begin / scale) * 0.8f, -(r_begin / scale) * 0.8f * 0.2f, (r_begin / scale) * 0.8f - (r_begin / scale) * 0.8f * 0.2f,
                    }, hand);
                }
                if (flagHour == 1 && mit == 60) {
                    canvas.save();
                    canvas.rotate(angle);
                    canvas.drawLine(0, 0, r * 0.8f, 0, hand);
                    canvas.drawLines(new float[]{
                            r * 0.8f, 0, r * 0.8f - r * 0.8f * 0.1f, r * 0.8f * 0.1f,
                            r * 0.8f, 0, r * 0.8f - r * 0.8f * 0.1f, -r * 0.8f * 0.1f,
                    }, hand);
                    canvas.restore();
                }
                if (flagDay == 1 && mit == 1440){
                    canvas.save();
                    canvas.rotate(angle);
                    canvas.drawLine(0, 0, r * 0.8f, 0, hand);
                    canvas.drawLines(new float[]{
                            r * 0.8f, 0, r * 0.8f - r * 0.8f * 0.1f, r * 0.8f * 0.1f,
                            r * 0.8f, 0, r * 0.8f - r * 0.8f * 0.1f, -r * 0.8f * 0.1f,
                    }, hand);
                    canvas.restore();
                }

                //刻度
                pathDim.addArc(rectF, angle % 360, angle_increase_end * border / r / scale + 30);
                if (flagDimDay == 1 && (mit - hour * 60 - min) % 1440 == 0){
                    canvas.drawTextOnPath("I", pathDim, 0, 2, paintDayDim);
                    if (flagNumDay == 1){
                        int clo = (mit - hour * 60 - min) == 0 ? day + 1 : day + 1 - (mit - min) / 60 / 24 % 30;
                        if (scale < 12){
                            canvas.drawTextOnPath("" + clo, pathDim, -5, -25, paintBigNum);
                        }else {
                            canvas.drawTextOnPath("" + clo, pathDim, -5, -25, paintNum);
                        }
                    }
                }else if (flagDimHour == 1 && (mit - min) % 60 == 0 && sca < 6) {
                    canvas.drawTextOnPath("I", pathDim, 0, 2, paintHourDim);
                    if (flagNumHour == 1   && sca < 4){
                        int clo = (mit - min) == 0 ? hour : Math.abs(hour - (mit - min) / 60) % 24;
                        if (scale < 2){
                            canvas.drawTextOnPath("" + clo, pathDim, -5, -25, paintBigNum);
                        }else
                            canvas.drawTextOnPath("" + clo, pathDim, -5, -25, paintNum);
                    }
                } else if (flagDimFiveMin == 1 && (mit - min) % 5 == 0) {
                    canvas.drawTextOnPath("I", pathDim, 0, 2, paintFiveMinDim);
                    if (flagNumFiveMin == 1){
                        int clo = (mit - min) >= 0 ? 12 - (mit - min) / 5 % 12 : (min - mit) / 5;
                        canvas.drawTextOnPath("" + clo, pathDim, -6, -12, paintNum);
                    }
                } else if (flagDimMinute == 1){
                    canvas.drawTextOnPath("I", pathDim, 0, 2, paintMinDim);
                }
                pathDim.reset();

                //累加
                mit = mit + multiple;
                angle += angle_increase_end * border / r / scale * multiple;
                r += r_increase / scale * multiple;
            }
            mPaint.setAlpha(255 - 200 / (scale_max /2 - (sca+1)/2 + 1));
            canvas.drawPath(mPath, mPaint);
            mPath.reset();
        }
        if (r >= border) {
            while (r * Math.abs(Math.cos((angle + angle_increase_end * border / r / scale) % 360)) < border - 10) {
                RectF rectF = new RectF(-r, -r, r, r);
                mPath.addArc(rectF, angle % 360, angle_increase_end * border / r / scale);
                angle += angle_increase_end * border / r / scale;
                r += r_increase / scale;
            }
            mPaint.setAlpha(255);
            canvas.drawPath(mPath, mPaint);
            mPath.reset();
        }
//        Log.d(TAG, "onDraw: " + count++ + "canvas = " + canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        if (widthSpecMode == MeasureSpec.AT_MOST && heightSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(widthSpecSize, (int)(heightSpecSize * 0.8));
        } else if (widthSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(widthSpecSize, heightSpecSize);
        } else if (heightSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(widthSpecSize, (int)(heightSpecSize * 0.8));
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (touchFlag) {
            try {

                /**
                 * 位置关系的处理
                 */
                if (event.getX() > 0 && event.getX() < mWidth && event.getY() > 0 && event.getY() < mHeight) {
                    /** 处理多点触摸 **/
                    switch (event.getAction() & MotionEvent.ACTION_MASK) {
                        case MotionEvent.ACTION_DOWN:
                            break;
                        // 多点触摸
                        case MotionEvent.ACTION_POINTER_DOWN:
                            onPointerDown(event);
                            break;
                        case MotionEvent.ACTION_MOVE:
                            onTouchMove(event);
                            break;
                        case MotionEvent.ACTION_UP:
                            break;
                        // 多点松开
                        case MotionEvent.ACTION_POINTER_UP:
                            break;
                    }
                    return true;
                }
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "onTouchEvent: " + e);
            }
        }
        return super.onTouchEvent(event);
    }

    /***********************
     * 初始化部分
     *******************/

    private void initPaint() {
        //螺旋线
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.YELLOW);//设置画笔颜色
        mPaint.setStrokeWidth(5);//为了看得清楚,设置了较大的画笔宽度
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        //数字
        paintBigNum.setStyle(Paint.Style.STROKE);
        paintBigNum.setAntiAlias(true);//抗锯齿
        paintBigNum.setColor(Color.GREEN);//设置画笔颜色
        paintBigNum.setTextSize(35);
        paintBigNum.setTextAlign(Paint.Align.LEFT);
        fontMetrics = paintNum.getFontMetrics();
        //数字
        paintNum.setStyle(Paint.Style.STROKE);
        paintNum.setAntiAlias(true);//抗锯齿
        paintNum.setColor(Color.GREEN);//设置画笔颜色
        paintNum.setTextSize(25);
        paintNum.setTextAlign(Paint.Align.LEFT);
        fontMetrics = paintNum.getFontMetrics();
        //刻度
        paintDim.setStyle(Paint.Style.FILL_AND_STROKE);
        paintDim.setAntiAlias(true);//抗锯齿
        paintDim.setColor(Color.WHITE);//设置画笔颜色
        paintDim.setStrokeWidth(2);
        paintDim.setTextSize(10);
        paintDim.setTextAlign(Paint.Align.LEFT);
        //大刻度
        paintBigDim.setStyle(Paint.Style.FILL_AND_STROKE);
        paintBigDim.setAntiAlias(true);//抗锯齿
        paintBigDim.setColor(Color.CYAN);//设置画笔颜色
        paintBigDim.setStrokeWidth(3);
        paintBigDim.setTextSize(15);
        paintBigDim.setTextAlign(Paint.Align.LEFT);
        //chao大刻度
        paintLargeDim.setStyle(Paint.Style.FILL_AND_STROKE);
        paintLargeDim.setAntiAlias(true);//抗锯齿
        paintLargeDim.setColor(Color.CYAN);//设置画笔颜色
        paintLargeDim.setStrokeWidth(5);
        paintLargeDim.setTextSize(30);
        paintLargeDim.setTextAlign(Paint.Align.LEFT);
        //指针
        hand.setStyle(Paint.Style.STROKE);
        hand.setAntiAlias(true);
        hand.setColor(Color.RED);//设置画笔颜色
        hand.setAlpha(180);
        hand.setStrokeWidth(8);//为了看得清楚,设置了较大的画笔宽度
        hand.setStrokeCap(Paint.Cap.ROUND);
    }

    private void initAnimator(long duration) {
        if (animator != null && animator.isRunning()) {
            animator.cancel();
            animator.start();
        } else {
            animator = ValueAnimator.ofFloat(0, 3000).setDuration(duration);
            animator.setInterpolator(timeInterpolator);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    animatedValue = (float) animation.getAnimatedValue();
//                    Log.d(TAG, "in " + animatedValue);
//                    invalidate();
                }
            });
            animator.start();
        }
    }

    private void initClockPaint(){
        paintMinDim = null;
        paintFiveMinDim = null;
        paintHourDim = null;
        paintDayDim = null;
        paintWeekDim = null;
        paintMonthDim = null;
        paintMinNum = null;
        paintFiveMinNum = null;
        paintHourNum = null;
        paintDayNum = null;
        paintWeekNum = null;
        paintMonthNum = null;
        switch ((int)scale){
            case 1:
                paintMinDim = paintDim;
                paintFiveMinDim = paintBigDim;
                paintHourDim = paintLargeDim;
                break;
            case 2:
            case 3:
                paintFiveMinDim = paintDim;
                paintHourDim = paintBigDim;
                paintDayDim = paintLargeDim;
                break;
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            default:
                paintFiveMinDim = paintDim;
                paintHourDim = paintBigDim;
                paintDayDim = paintLargeDim;
                break;
        }
    }

    private void initFlag(){
        flagMinute = 0;
        flagDimFiveMin = 0;
        flagHour = 0;
        flagDay = 0;
        flagWeek = 0;
        flagMonth = 0;
        flagYear = 0;
        flagDimMinute = 0;
        flagDimFiveMin = 0;
        flagDimHour = 0;
        flagDimDay = 0;
        flagDimWeek = 0;
        flagDimMonth = 0;
        flagDimYear = 0;
        flagNumFiveMin = 0;
        flagNumHour = 0;
        flagNumDay = 0;
        flagNumWeek = 0;
        flagNumMonth = 0;
        flagNumYear = 0;
        switch ((int)scale){
            case 1:
                flagMinute = 1;
                flagDimMinute = 1;
                flagNumFiveMin = 1;
            case 2:
                flagHour = 1;
            case 3:
                flagFiveMin = 1;
                flagDimFiveMin = 1;
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            default:
                flagDay = 1;
                flagDimHour = 1;
                flagDimDay = 1;
                flagDimWeek = 1;
                flagNumHour = 1;
                flagNumDay = 1;
                flagNumWeek = 1;

        }
    }


    /**************************缩放效果部分**********************/

    /**
     * 多触点
     *
     * @param event
     */
    private boolean onPointerDown(MotionEvent event) {
        if (event.getPointerCount() == 2) {
            beforeLength = getDistance(event);// 获取两点的距离
            return true;
        }
        return false;
    }

    /**
     * 获取缩放比例
     *
     * @param event
     */
    private void onTouchMove(MotionEvent event) {
        afterLength = getDistance(event);// 获取两点的距离
        double gapLenght = afterLength - beforeLength;// 变化的长度
        if (Math.abs(gapLenght) > 5f && beforeLength != 0) {
            float scale_temp = (float) (beforeLength / afterLength);// 求的缩放的比例
            resetPoints(scale_temp);    //重设置
            this.invalidate();    //重新绘制
            beforeLength = afterLength;
        }
    }


    /**
     * @param event
     * @return 获取两手指之间的距离
     */
    private double getDistance(MotionEvent event) {
        double x = event.getX(0) - event.getX(1);
        double y = event.getY(0) - event.getY(1);
        return Math.sqrt(x * x + y * y);
    }

    /**
     * 重新设置比例
     *
     * @param scale_temp
     */
    private void resetPoints(float scale_temp) {

        /**
         * 缩放比例在最小比例和最大比例范围内
         */
        if (scale * scale_temp >= scale_max) {
            scale_temp = scale_max / scale;
            scale = scale_max;
        } else if (scale * scale_temp <= scale_min) {
            scale_temp = scale_min / scale;
            scale = scale_min;
        } else {
            scale = scale * scale_temp;
        }
        Log.d(TAG, "scale = " + scale);
    }
}
