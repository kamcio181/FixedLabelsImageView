package com.kaszubski.kamil.fixedlabelsimageview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

public class TouchImageView extends ImageView {
    private static final String TAG = TouchImageView.class.getSimpleName();
    private static final double MAX_SCALE = 5.0;
    private static final int SCALED_BITMAP_GENERATION_DELAY = 100;
    public static final int SAFETY_OFFSET = 1;
    private Bitmap bitmap;
    private double scale = 1.0;
    private double maxScale = MAX_SCALE;
    private int imageViewHeight;
    private double initScale;
    private Point center = new Point();
    private Point dragStart = new Point();
    private GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureListener());
    private ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
    private boolean isDragging;
    private boolean alwaysShowTopLabel = false;
    private boolean alwaysShowLeftLabel = false;
    private int topLabelHeight = 0;
    private int leftLabelWidth = 0;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private boolean isBitmapGenerated = true;
    private Bitmap fullBitmapScaled;

    public TouchImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initConfig();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        bitmap = getBitmapResource();
        if(bitmap == null){
            return;
        }
        initScale = (double) getWidth() / bitmap.getWidth();
        imageViewHeight = (int) (bitmap.getHeight() * initScale);
        long start = System.currentTimeMillis();
        try {
            canvas.drawBitmap(getBitmapToViewDev(), 0, 0, null);
        } catch (IllegalArgumentException e){
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
        Log.v(TAG, "PRINTED " + (System.currentTimeMillis() - start));
    }

    private void initConfig(){
        setScaleType(ScaleType.FIT_START);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        backgroundThread = new HandlerThread("TouchImageViewBackgroundThread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        backgroundThread.quitSafely();
    }

    private Bitmap getBitmapResource() {
        if (bitmap == null) {
            Drawable drawable = getDrawable();
            if (drawable == null) {
                return null;
            }

            if (getWidth() == 0 || getHeight() == 0) {
                return null;
            }

            return ((BitmapDrawable) drawable).getBitmap();
        } else {
            return bitmap;
        }
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        invalidate();
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setScale(Double scale) {
        if(scale > maxScale){
            scale = maxScale;
        }

        if(scale < 1){
            scale = 1.0;
        }
        this.scale = scale;
        invalidate();

        if(scale > 1){
            backgroundHandler.removeCallbacksAndMessages(null);
            isBitmapGenerated = false;
            backgroundHandler.postDelayed(this::scaleWholeBitmap, SCALED_BITMAP_GENERATION_DELAY);
        }
    }

    private void scaleWholeBitmap() {
        long start = System.currentTimeMillis();
        fullBitmapScaled = Bitmap.createScaledBitmap(bitmap,
                Math.max(1,(int) (bitmap.getWidth() * TouchImageView.this.scale * initScale)),
                Math.max(1,(int) (bitmap.getHeight() * TouchImageView.this.scale * initScale)), true);
        isBitmapGenerated = true;
        Log.v(TAG, "isBitmapGenerated " + (System.currentTimeMillis() - start));
    }

    public Double getScale() {
        return scale;
    }

    public void setMaxScale(double maxScale) {
        this.maxScale = maxScale;
    }

    public void setAlwaysShowTopLabel(boolean alwaysShowTopLabel) {
        this.alwaysShowTopLabel = alwaysShowTopLabel;
        invalidate();
    }

    public void setAlwaysShowLeftLabel(boolean alwaysShowLeftLabel) {
        this.alwaysShowLeftLabel = alwaysShowLeftLabel;
        invalidate();
    }

    public void setTopLabelHeight(int topLabelHeight) {
        if(topLabelHeight > bitmap.getHeight()){
            topLabelHeight = bitmap.getHeight();
        }
        this.topLabelHeight = topLabelHeight;
        invalidate();
    }

    public void setLeftLabelWidth(int leftLabelWidth) {
        if(leftLabelWidth > bitmap.getWidth()){
            leftLabelWidth = bitmap.getWidth();
        }
        this.leftLabelWidth = leftLabelWidth;
        invalidate();
    }

    public void transformCenter(int deltaX, int deltaY) {
        if(scale == 1){
            return;
        }
        if(deltaX == 0 && deltaY == 0){
            return;
        }
        center.x -= deltaX;
        center.y -= deltaY;
        invalidate();
    }

    private Bitmap getBitmapToViewDev(){
        if(scale == 1){
            center = new Point(bitmap.getWidth()/2, bitmap.getHeight()/2);
            return Bitmap.createScaledBitmap(bitmap, getWidth(), imageViewHeight, true);
        }

        if(!alwaysShowTopLabel && !alwaysShowLeftLabel){
            return getBitmapNoFixedLabels();
        }

        return getBitmapFixedLabels();
    }

    private Bitmap getBitmapNoFixedLabels(){
        int scaledWidth = scaleUp(bitmap.getWidth());
        int scaledHeight = scaleUp(bitmap.getHeight());
        int viewHeight = Math.min(getHeight(), scaledHeight);
        int halfOfViewWidth = getWidth()/2;
        int halfOfViewHeight = viewHeight/2;

        center.x = getValueWithinRange(halfOfViewWidth, scaledWidth - halfOfViewWidth, center.x);
        center.y = getValueWithinRange(halfOfViewHeight, scaledHeight - halfOfViewHeight, center.y);

        int left = center.x - halfOfViewWidth;
        int top = center.y - halfOfViewHeight;

        if(isBitmapGenerated){
            return Bitmap.createBitmap(fullBitmapScaled, left, top, getWidth(), viewHeight);
        }

        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, scaleDown(left), scaleDown(top), scaleDown(getWidth()), scaleDown(viewHeight));

        return Bitmap.createScaledBitmap(croppedBitmap, getWidth(), viewHeight, true);
    }

    private int getValueWithinRange(int minValue, int maxValue, int currentValue) {
        return Math.max(minValue, Math.min(maxValue, currentValue));
    }

    private Bitmap getBitmapFixedLabels() {
        boolean showTopLabel = alwaysShowTopLabel && topLabelHeight > 0;
        boolean showLeftLabel = alwaysShowLeftLabel && leftLabelWidth > 0;
        if(!showLeftLabel && !showTopLabel){
            return getBitmapNoFixedLabels();
        }

        int scaledTopLabelHeight = showTopLabel? scaleUp(topLabelHeight): 0;
        int scaledLeftLabelWidth = showLeftLabel? scaleUp(leftLabelWidth): 0;
        int scaledWidth = scaleUp(bitmap.getWidth());
        int scaledHeight = scaleUp(bitmap.getHeight());
        int viewWidth = getWidth();
        int viewHeight = Math.min(getHeight(), scaledHeight);
        int halfOfViewWidth = viewWidth/2;
        int halfOfViewHeight = viewHeight/2;

        center.x = getValueWithinRange(halfOfViewWidth + scaledLeftLabelWidth,
                scaledWidth - halfOfViewWidth + scaledLeftLabelWidth, center.x);
        center.y = getValueWithinRange(halfOfViewHeight + scaledTopLabelHeight,
                scaledHeight - halfOfViewHeight + scaledTopLabelHeight, center.y);

        int left = center.x - halfOfViewWidth;
        int top = center.y - halfOfViewHeight;
        int mainBitmapWidth = viewWidth - scaledLeftLabelWidth;
        int mainBitmapHeight = viewHeight - scaledTopLabelHeight;
        Bitmap baseBitmap = fullBitmapScaled;

        if(!isBitmapGenerated){
            baseBitmap = bitmap;
            left = scaleDown(left) - SAFETY_OFFSET;
            top = scaleDown(top) - SAFETY_OFFSET;
        }

        return getFinalBitmapFixedLabels(baseBitmap, viewWidth, viewHeight, left, top, mainBitmapWidth, mainBitmapHeight);
    }

    private Bitmap getFinalBitmapFixedLabels(Bitmap baseBitmap, int viewWidth, int viewHeight, int left, int top, int mainBitmapWidth, int mainBitmapHeight) {
        boolean showTopLabel = alwaysShowTopLabel && topLabelHeight > 0;
        boolean showLeftLabel = alwaysShowLeftLabel && leftLabelWidth > 0;
        int scaledTopLabelHeight = showTopLabel? scaleUp(topLabelHeight) : 0;
        int scaledLeftLabelWidth = showLeftLabel? scaleUp(leftLabelWidth) : 0;

        Bitmap finalBitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(finalBitmap);

        canvas.drawBitmap(getMainBitmap(baseBitmap, left, top, mainBitmapWidth, mainBitmapHeight),
                scaledLeftLabelWidth, scaledTopLabelHeight, null);

        if (showTopLabel) {
            canvas.drawBitmap(getTopLabelBitmap(baseBitmap, left, mainBitmapWidth, scaledTopLabelHeight),
                    scaledLeftLabelWidth, 0, null);
        }

        if (showLeftLabel) {
            canvas.drawBitmap(getLeftLabelBitmap(baseBitmap, top, mainBitmapHeight, scaledLeftLabelWidth),
                    0, scaledTopLabelHeight, null);
        }

        if (showLeftLabel && showTopLabel) {
            canvas.drawBitmap(getCornerBitmap(baseBitmap, scaledTopLabelHeight, scaledLeftLabelWidth),
                    0, 0, null);
        }

        return finalBitmap;
    }

    private Bitmap getCornerBitmap(Bitmap baseBitmap, int scaledTopLabelHeight, int scaledLeftLabelWidth) {
        if(!isBitmapGenerated){
            Bitmap cornerBitmap = Bitmap.createBitmap(baseBitmap, 0, 0,
                    scaleDown(scaledLeftLabelWidth), scaleDown(scaledTopLabelHeight));
            return Bitmap.createScaledBitmap(cornerBitmap, scaledLeftLabelWidth,
                    scaledTopLabelHeight, true);
        } else {
            return Bitmap.createBitmap(baseBitmap, 0, 0, scaledLeftLabelWidth, scaledTopLabelHeight);
        }
    }

    private Bitmap getLeftLabelBitmap(Bitmap baseBitmap, int top, int mainBitmapHeight, int scaledLeftLabelWidth) {
        if(!isBitmapGenerated){
            Bitmap leftLabel = Bitmap.createBitmap(baseBitmap, 0, top,
                    scaleDown(scaledLeftLabelWidth), scaleDown(mainBitmapHeight));
            return Bitmap.createScaledBitmap(leftLabel, scaledLeftLabelWidth, mainBitmapHeight, true);
        } else {
            return Bitmap.createBitmap(baseBitmap, 0, top,
                    scaledLeftLabelWidth, mainBitmapHeight);
        }
    }

    private Bitmap getTopLabelBitmap(Bitmap baseBitmap, int left, int mainBitmapWidth, int scaledTopLabelHeight) {
        if(!isBitmapGenerated){
            Bitmap topLabel = Bitmap.createBitmap(baseBitmap, left,
                    0, scaleDown(mainBitmapWidth), scaleDown(scaledTopLabelHeight));
            return Bitmap.createScaledBitmap(topLabel, mainBitmapWidth, scaledTopLabelHeight, true);
        } else {
            return Bitmap.createBitmap(baseBitmap, left,
                    0, mainBitmapWidth, scaledTopLabelHeight);
        }
    }

    private Bitmap getMainBitmap(Bitmap baseBitmap, int left, int top, int mainBitmapWidth, int mainBitmapHeight) {
        if(!isBitmapGenerated){
            Bitmap mainBitmap = Bitmap.createBitmap(baseBitmap, left,
                    top, scaleDown(mainBitmapWidth),
                    scaleDown(mainBitmapHeight));
            return Bitmap.createScaledBitmap(mainBitmap, mainBitmapWidth,
                    mainBitmapHeight, true);
        } else {
            return Bitmap.createBitmap(baseBitmap, left,
                    top, mainBitmapWidth, mainBitmapHeight);
        }
    }


    private int scaleUp(int i){
        return (int) (i * initScale * scale);
    }

    private int scaleDown(int i){
        return (int) (i / initScale / scale);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        scaleGestureDetector.onTouchEvent(event);
        if(scaleGestureDetector.isInProgress()){
            isDragging = false;
            return true;
        }

        switch (event.getAction() & MotionEvent.ACTION_MASK){
            case MotionEvent.ACTION_DOWN:
                isDragging = true;
                dragStart.x = (int) event.getX();
                dragStart.y = (int) event.getY();
                return true;
            case MotionEvent.ACTION_MOVE:
                if(!isDragging){
                    return true;
                }

                int dx = (int)event.getX() - dragStart.x;
                int dy = (int)event.getY() - dragStart.y;
                dragStart.x = (int)event.getX();
                dragStart.y = (int)event.getY();
                transformCenter(dx, dy);
                return true;
            case MotionEvent.ACTION_UP:
                isDragging = false;
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener{
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            setScale(scale * 1.5);
            return true;
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener{
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            setScale(scale * detector.getScaleFactor());
            return true;
        }
    }
}
