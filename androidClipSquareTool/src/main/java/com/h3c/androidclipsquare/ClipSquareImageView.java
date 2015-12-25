package com.h3c.androidclipsquare;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

/**
 * Created by H3c on 12/13/14.
 */
public class ClipSquareImageView extends ImageView implements View.OnTouchListener, ViewTreeObserver.OnGlobalLayoutListener {
    private final Paint mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);// 边框画笔
    private int BORDER_DISTANCE;// 距离屏幕的边距
    private int OUTSIDE_COLOR;// 外部背景颜色
    private int BORDER_LINE_COLOR;// 边框颜色
    private float BORDER_LINE_WIDTH;// 框框宽度
    private int WIDTH_WEIGHT = 1;// 宽边比例
    private int HEIGHT_WEIGHT = 1;// 高边比例
    private int BORDER_LONG;// 边长

    public static final float DEFAULT_MAX_SCALE = 4.0f;
    public static final float DEFAULT_MID_SCALE = 2.0f;
    public static final float DEFAULT_MIN_SCALE = 1.0f;

    private float minScale = DEFAULT_MIN_SCALE;
    private float midScale = DEFAULT_MID_SCALE;
    private float maxScale = DEFAULT_MAX_SCALE;

    private MultiGestureDetector multiGestureDetector;
    private boolean isIniting;// 正在初始化
    private Rect outsideRect = new Rect();// View的Rect
    private Rect cutRect = new Rect();// 裁剪的Rect

    private Matrix defaultMatrix = new Matrix();// 初始化的图片矩阵，控制图片撑满屏幕及显示区域
    private Matrix dragMatrix = new Matrix();// 拖拽放大过程中动态的矩阵
    private Matrix finalMatrix = new Matrix();// 最终显示的矩阵
    private final RectF displayRect = new RectF();// 图片的真实大小
    private final float[] matrixValues = new float[9];

    private SHAPE mShape = SHAPE.REGTANGLE; // 遮罩样式
    public enum SHAPE {
        ROUND,
        REGTANGLE
    }

    public ClipSquareImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ClipSquareImageView);
        setBorderDistance((int) a.getDimension(R.styleable.ClipSquareImageView_ClipSquareIV_BorderDistance, 50));
        setBorderWidth((int) a.getDimension(R.styleable.ClipSquareImageView_ClipSquareIV_BorderWidth, 2));
        setBorderColor(a.getColor(R.styleable.ClipSquareImageView_ClipSquareIV_BorderColor, Color.WHITE));
        setOutsideColor(a.getColor(R.styleable.ClipSquareImageView_ClipSquareIV_OutsideColor, Color.parseColor("#76000000")));
        a.recycle();

        super.setScaleType(ScaleType.MATRIX);
        setOnTouchListener(this);
        multiGestureDetector = new MultiGestureDetector(context);
    }

    /**
     * 边框距离屏幕宽度
     * @param distance
     */
    public void setBorderDistance(int distance) {
        this.BORDER_DISTANCE = distance;
    }

    /**
     * 边框宽度
     * @param width
     */
    public void setBorderWidth(int width) {
        this.BORDER_LINE_WIDTH = width;
    }

    /**
     * 边框颜色
     * @param color
     */
    public void setBorderColor(int color) {
        this.BORDER_LINE_COLOR = color;
    }

    /**
     * 非裁剪区域颜色
     * @param color
     */
    public void setOutsideColor(int color) {
        this.OUTSIDE_COLOR = color;
    }

    /**
     * 设置裁剪比例
     * @param widthWeight
     * @param heightWeight
     */
    public void setBorderWeight(int widthWeight, int heightWeight) {
        this.WIDTH_WEIGHT = widthWeight;
        this.HEIGHT_WEIGHT = heightWeight;

        initBmpPosition();
        invalidate();
    }

    /**
     * 设置是圆的还是方的
     */
    public void setShape(SHAPE shape) {
        mShape = shape;
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeGlobalOnLayoutListener(this);
    }

    @Override
    public void onGlobalLayout() {
        if(isIniting) {
            return;
        }
        // 调整视图位置
        initBmpPosition();
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        // 调整视图位置
        initBmpPosition();
    }

    /**
     * 初始化图片位置
     */
    private void initBmpPosition() {
        isIniting = true;
        super.setScaleType(ScaleType.MATRIX);
        Drawable drawable = getDrawable();

        if(drawable == null) {
            return;
        }

        outsideRect.set(0, 0, getWidth(), getHeight());
        BORDER_LONG = Math.min(outsideRect.width(), outsideRect.height()) - 2 * BORDER_DISTANCE;

        // 裁剪后方向
        boolean isCutToHorizontal = false;
        if(WIDTH_WEIGHT >= HEIGHT_WEIGHT) {
            if(WIDTH_WEIGHT == HEIGHT_WEIGHT && (outsideRect.width() > outsideRect.height())) {
            } else {
                isCutToHorizontal = true;
            }
        }

        // 裁剪区域
        int inLeft = BORDER_DISTANCE + (isCutToHorizontal ? 0 : (outsideRect.width() - outsideRect.height() * WIDTH_WEIGHT / HEIGHT_WEIGHT) / 2);
        int inTop = isCutToHorizontal ? (outsideRect.height() - BORDER_LONG * HEIGHT_WEIGHT / WIDTH_WEIGHT) >> 1 : BORDER_DISTANCE;
        int inRight = outsideRect.width() - inLeft;
        int inBottom = outsideRect.height() - inTop;
        cutRect.set(inLeft, inTop, inRight, inBottom);

        // 获得图片高宽
        final float drawableWidth = drawable.getIntrinsicWidth();
        final float drawableHeight = drawable.getIntrinsicHeight();

        // 按图片最小边比例缩放图片，用于撑满裁剪区
        float screenScale;
        float photoX = cutRect.left;
        float photoY = cutRect.top;
        // 先统一比例
        float scaleWidthWeight = cutRect.width() / drawableWidth;
        // 区域比图片宽，按照图片宽撑满
        if(drawableHeight * scaleWidthWeight > cutRect.height()) {
            screenScale = cutRect.width() / drawableWidth;
            photoY = photoY - ((drawableHeight * screenScale - cutRect.height()) / 2);
        } else {// 图片比区域宽，按照图片高度撑满
            screenScale = cutRect.height() / drawableHeight;
            photoX = photoX - ((drawableWidth * screenScale - cutRect.width()) / 2);
        }
        defaultMatrix.setScale(screenScale, screenScale);

        // 设置图片位置居中
        defaultMatrix.postTranslate(photoX, photoY);

        resetMatrix();
    }

    /**
     * Resets the Matrix back to FIT_CENTER, and then displays it.s
     */
    private void resetMatrix() {
        if(dragMatrix == null) {
            return;
        }

        dragMatrix.reset();
        setImageMatrix(getDisplayMatrix());
    }

    private Matrix getDisplayMatrix() {
        finalMatrix.set(defaultMatrix);
        finalMatrix.postConcat(dragMatrix);
        return finalMatrix;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return multiGestureDetector.onTouchEvent(motionEvent);
    }

    private class MultiGestureDetector extends GestureDetector.SimpleOnGestureListener implements
            ScaleGestureDetector.OnScaleGestureListener {

        private final ScaleGestureDetector scaleGestureDetector;
        private final GestureDetector gestureDetector;
        private final float scaledTouchSlop;

        private VelocityTracker velocityTracker;
        private boolean isDragging;

        private float lastTouchX;
        private float lastTouchY;
        private float lastPointerCount;// 上一次是几个手指事件

        public MultiGestureDetector(Context context) {
            scaleGestureDetector = new ScaleGestureDetector(context, this);
            gestureDetector = new GestureDetector(context, this);
            gestureDetector.setOnDoubleTapListener(this);

            final ViewConfiguration configuration = ViewConfiguration.get(context);
            scaledTouchSlop = configuration.getScaledTouchSlop();
        }

        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            float scale = getScale();
            float scaleFactor = scaleGestureDetector.getScaleFactor();
            if(getDrawable() != null && ((scale < maxScale && scaleFactor > 1.0f) || (scale > minScale && scaleFactor < 1.0f))){
                if(scaleFactor * scale < minScale){
                    scaleFactor = minScale / scale;
                }
                if(scaleFactor * scale > maxScale){
                    scaleFactor = maxScale / scale;
                }
                dragMatrix.postScale(scaleFactor, scaleFactor, getWidth() / 2, getHeight() / 2);
                checkAndDisplayMatrix();
            }
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {}

        public boolean onTouchEvent(MotionEvent event) {
            if (gestureDetector.onTouchEvent(event)) {
                return true;
            }

            scaleGestureDetector.onTouchEvent(event);

            /*
             * Get the center x, y of all the pointers
             */
            float x = 0, y = 0;
            final int pointerCount = event.getPointerCount();
            for (int i = 0; i < pointerCount; i++) {
                x += event.getX(i);
                y += event.getY(i);
            }
            x = x / pointerCount;
            y = y / pointerCount;

            /*
             * If the pointer count has changed cancel the drag
             */
            if (pointerCount != lastPointerCount) {
                isDragging = false;
                if (velocityTracker != null) {
                    velocityTracker.clear();
                }
                lastTouchX = x;
                lastTouchY = y;
                lastPointerCount = pointerCount;
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (velocityTracker == null) {
                        velocityTracker = VelocityTracker.obtain();
                    } else {
                        velocityTracker.clear();
                    }
                    velocityTracker.addMovement(event);

                    lastTouchX = x;
                    lastTouchY = y;
                    isDragging = false;
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    lastPointerCount = 0;
                    if (velocityTracker != null) {
                        velocityTracker.recycle();
                        velocityTracker = null;
                    }
                    break;
                case MotionEvent.ACTION_MOVE: {
                    final float dx = x - lastTouchX, dy = y - lastTouchY;

                    if (isDragging == false) {
                        // Use Pythagoras to see if drag length is larger than
                        // touch slop
                        isDragging = Math.sqrt((dx * dx) + (dy * dy)) >= scaledTouchSlop;
                    }

                    if (isDragging) {
                        if (getDrawable() != null) {
                            dragMatrix.postTranslate(dx, dy);
                            checkAndDisplayMatrix();
                        }

                        lastTouchX = x;
                        lastTouchY = y;

                        if (velocityTracker != null) {
                            velocityTracker.addMovement(event);
                        }
                    }
                    break;
                }
            }

            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent event) {
            try {
                float scale = getScale();
                float x = getWidth() / 2;
                float y = getHeight() / 2;

                if (scale < midScale) {
                    post(new AnimatedZoomRunnable(scale, midScale, x, y));
                } else if ((scale >= midScale) && (scale < maxScale)) {
                    post(new AnimatedZoomRunnable(scale, maxScale, x, y));
                } else {// 双击缩小小于最小值
                    post(new AnimatedZoomRunnable(scale, minScale, x, y));
                }
            } catch (Exception e) {
                // Can sometimes happen when getX() and getY() is called
            }

            return true;
        }
    }

    private class AnimatedZoomRunnable implements Runnable {
        // These are 'postScale' values, means they're compounded each iteration
        static final float ANIMATION_SCALE_PER_ITERATION_IN = 1.07f;
        static final float ANIMATION_SCALE_PER_ITERATION_OUT = 0.93f;

        private final float focalX, focalY;
        private final float targetZoom;
        private final float deltaScale;

        public AnimatedZoomRunnable(final float currentZoom, final float targetZoom,
                                    final float focalX, final float focalY) {
            this.targetZoom = targetZoom;
            this.focalX = focalX;
            this.focalY = focalY;

            if (currentZoom < targetZoom) {
                deltaScale = ANIMATION_SCALE_PER_ITERATION_IN;
            } else {
                deltaScale = ANIMATION_SCALE_PER_ITERATION_OUT;
            }
        }

        @Override
        public void run() {
            dragMatrix.postScale(deltaScale, deltaScale, focalX, focalY);
            checkAndDisplayMatrix();

            final float currentScale = getScale();

            if (((deltaScale > 1f) && (currentScale < targetZoom))
                    || ((deltaScale < 1f) && (targetZoom < currentScale))) {
                // We haven't hit our target scale yet, so post ourselves
                // again
                postOnAnimation(ClipSquareImageView.this, this);

            } else {
                // We've scaled past our target zoom, so calculate the
                // necessary scale so we're back at target zoom
                final float delta = targetZoom / currentScale;
                dragMatrix.postScale(delta, delta, focalX, focalY);
                checkAndDisplayMatrix();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void postOnAnimation(View view, Runnable runnable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.postOnAnimation(runnable);
        } else {
            view.postDelayed(runnable, 16);
        }
    }

    /**
     * Returns the current scale value
     *
     * @return float - current scale value
     */
    public final float getScale() {
        dragMatrix.getValues(matrixValues);
        return matrixValues[Matrix.MSCALE_X];
    }

    /**
     * Helper method that simply checks the Matrix, and then displays the result
     */
    private void checkAndDisplayMatrix() {
        checkMatrixBounds();
        setImageMatrix(getDisplayMatrix());
    }

    private void checkMatrixBounds() {
        final RectF rect = getDisplayRect(getDisplayMatrix());
        if (null == rect) {
            return;
        }

        float deltaX = 0, deltaY = 0;
        // 判断移动或缩放后，图片显示是否超出裁剪框边界
        if(rect.top > cutRect.top){
            deltaY = cutRect.top - rect.top;
        }
        if(rect.bottom < cutRect.bottom){
            deltaY = cutRect.bottom - rect.bottom;
        }
        if(rect.left > cutRect.left){
            deltaX = cutRect.left - rect.left;
        }
        if(rect.right < cutRect.right){
            deltaX = cutRect.right - rect.right;
        }
        // Finally actually translate the matrix
        dragMatrix.postTranslate(deltaX, deltaY);


    }

    /**
     * 获取图片相对Matrix的距离
     *
     * @param matrix
     *            - Matrix to map Drawable against
     * @return RectF - Displayed Rectangle
     */
    private RectF getDisplayRect(Matrix matrix) {
        Drawable d = getDrawable();
        if (null != d) {
            displayRect.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            matrix.mapRect(displayRect);
            return displayRect;
        }

        return null;
    }

    /**
     * 剪切图片，返回剪切后的bitmap对象
     *
     * @return
     */
    public Bitmap clip(){
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas);
        return Bitmap.createBitmap(bitmap, cutRect.left, cutRect.top, cutRect.width(), cutRect.height());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawBorder(canvas);
    }

    /**
     * 画边框
     */
    private void drawBorder(Canvas canvas) {
        mBorderPaint.setColor(OUTSIDE_COLOR);

        switch (mShape) {
            case ROUND:
                Path path = new Path();
                path.addCircle(outsideRect.centerX(), outsideRect.centerY(), BORDER_LONG / 2, Path.Direction.CW);
                canvas.clipPath(path, Region.Op.DIFFERENCE);
                canvas.drawRect(outsideRect, mBorderPaint);
                break;
            case REGTANGLE:
                canvas.drawRect(outsideRect.left, outsideRect.top, outsideRect.right, cutRect.top, mBorderPaint);
                canvas.drawRect(outsideRect.left, cutRect.bottom, outsideRect.right, outsideRect.bottom, mBorderPaint);
                canvas.drawRect(outsideRect.left, cutRect.top, cutRect.left, cutRect.bottom, mBorderPaint);
                canvas.drawRect(cutRect.right, cutRect.top, outsideRect.right, cutRect.bottom, mBorderPaint);

                // 画边框
                mBorderPaint.setColor(BORDER_LINE_COLOR);
                mBorderPaint.setStrokeWidth(BORDER_LINE_WIDTH);
                canvas.drawLine(cutRect.left, cutRect.top, cutRect.left, cutRect.bottom, mBorderPaint);
                canvas.drawLine(cutRect.right, cutRect.top, cutRect.right, cutRect.bottom, mBorderPaint);
                canvas.drawLine(cutRect.left, cutRect.top, cutRect.right, cutRect.top, mBorderPaint);
                canvas.drawLine(cutRect.left, cutRect.bottom, cutRect.right, cutRect.bottom, mBorderPaint);
                break;
        }
    }
}
