package com.adolf.opencvstudy.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

public class CropImgView extends androidx.appcompat.widget.AppCompatImageView {

    private static final String TAG = "[jq]CropImgView";
    private static final int MIN_GAP = 50;

    private RectF mBorderRect;
    private Paint mEdgePaint;
    private RectF mEdgeRect;
    private Paint mCornerPaint;

    private float mTouchX;
    private float mTouchY;

    private RectF initCropRect;
    private float mEdgeLeftX;
    private float mEdgeTopY;
    private float mEdgeRightX;
    private float mEdgeBottomY;

    private EdgeCorner mMoveEC;

    private float[] mMatrixValues = new float[9];
    private float mScaleX;
    private float mScaleY;
    private float mTransX;
    private float mTransY;

    public void setInitCropRect(RectF inRect) {
        this.initCropRect = inRect;
        Log.d(TAG, "原始裁切边框Rect: " + initCropRect);
        float l = Math.max(initCropRect.left * mScaleX + mTransX - 20, mBorderRect.left);
        float t = Math.max(initCropRect.top * mScaleX + mTransY - 20, mBorderRect.top);
        float r = Math.min(initCropRect.right * mScaleX + mTransX + 20, mBorderRect.right);
        float b = Math.min(initCropRect.bottom * mScaleY + mTransY + 20, mBorderRect.bottom);
        mEdgeRect = new RectF(l, t, r, b);
        // Log.d(TAG, "转换裁切边框Rect: " + mEdgeRect);
        // mEdgeRect = new RectF(mBorderRect.left + 100, mBorderRect.top + 100, mBorderRect.right - 100, mBorderRect.bottom - 100);

        Log.d(TAG, "变换后的裁切边框: " + mEdgeRect);
        mEdgeLeftX = mEdgeRect.left;
        mEdgeTopY = mEdgeRect.top;
        mEdgeRightX = mEdgeRect.right;
        mEdgeBottomY = mEdgeRect.bottom;

        postInvalidate();
    }

    public CropImgView(Context context) {
        super(context);
        init(null, 0);
    }

    public CropImgView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public CropImgView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        initCropRect = new RectF(0, 0, 0, 0);
        mEdgeRect = new RectF(initCropRect);
        // 四边边框画笔
        mEdgePaint = new Paint();
        mEdgePaint.setStyle(Paint.Style.STROKE);
        mEdgePaint.setStrokeWidth(5);
        mEdgePaint.setColor(Color.parseColor("#AAFFCCCC"));

        // 四角画笔
        mCornerPaint = new Paint();
        mCornerPaint.setStyle(Paint.Style.FILL);
        mCornerPaint.setStrokeWidth(50);
        mCornerPaint.setColor(Color.parseColor("#AAFF6666"));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        initMatrix();

        mBorderRect = getBitmapRect();
        Log.d(TAG, "图片展示区域: " + mBorderRect);
    }

    /**
     * 获得变换Matrix
     */
    private void initMatrix() {
        getImageMatrix().getValues(mMatrixValues);
        mScaleX = mMatrixValues[Matrix.MSCALE_X];
        mScaleY = mMatrixValues[Matrix.MSCALE_Y];
        mTransX = mMatrixValues[Matrix.MTRANS_X];
        mTransY = mMatrixValues[Matrix.MTRANS_Y];
        Log.d(TAG, "Matrix[ 缩放X:" + mScaleX + ", 缩放Y:" + mScaleX + ", 平移X:" + mTransX + ", 平移Y:" + mTransY + "]");
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(mEdgeRect, mEdgePaint);
        canvas.drawCircle(mEdgeLeftX, mEdgeTopY, 10, mCornerPaint);
        canvas.drawCircle(mEdgeRightX, mEdgeTopY, 10, mCornerPaint);
        canvas.drawCircle(mEdgeLeftX, mEdgeBottomY, 10, mCornerPaint);
        canvas.drawCircle(mEdgeRightX, mEdgeBottomY, 10, mCornerPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouchX = event.getX();
                mTouchY = event.getY();
                mEdgeLeftX = mEdgeRect.left;
                mEdgeTopY = mEdgeRect.top;
                mEdgeRightX = mEdgeRect.right;
                mEdgeBottomY = mEdgeRect.bottom;
                Log.v(TAG, "onTouchEvent: Down-(" + mTouchX + ", " + mTouchY + ")" + ", Edge-(" + mEdgeLeftX + ", " + mEdgeTopY + ", " + mEdgeRightX + ", " + mEdgeBottomY + ")");
                mMoveEC = nearestEdge(mTouchX, mTouchY);
                return true;
            case MotionEvent.ACTION_UP:
                mTouchX = -1;
                mTouchY = -1;
                Log.v(TAG, "onTouchEvent: Up-(" + mTouchX + ", " + mTouchY + ")");
                return true;
            case MotionEvent.ACTION_MOVE:
                float perX = mTouchX;
                float perY = mTouchY;
                mTouchX = event.getX();
                mTouchY = event.getY();
                float moveX = mTouchX - perX;
                float moveY = mTouchY - perY;
                moveEdge(moveX, moveY, mMoveEC);
                Log.v(TAG, "onTouchEvent: Move-(" + mTouchX + ", " + mTouchY + ")" + ", Edge-(" + mEdgeLeftX + ", " + mEdgeTopY + ", " + mEdgeRightX + ", " + mEdgeBottomY + ")");
                mEdgeRect = new RectF(mEdgeLeftX, mEdgeTopY, mEdgeRightX, mEdgeBottomY);
                invalidate();
                return true;
            default:
                return false;
        }
    }

    private void moveEdge(float moveX, float moveY, EdgeCorner ec) {
        Log.v(TAG, "InsideMove:左移" + moveX + "，下移" + moveY);
        switch (ec) {
            case OUTSIDE:
                break;
            case INSIDE:
                boolean outBorder = mEdgeLeftX + moveX < mBorderRect.left || mEdgeTopY + moveY < mBorderRect.top
                        || mEdgeRightX + moveX > mBorderRect.right || mEdgeBottomY + moveY > mBorderRect.bottom;
                if (!outBorder) {
                    mEdgeLeftX += moveX;
                    mEdgeTopY += moveY;
                    mEdgeRightX += moveX;
                    mEdgeBottomY += moveY;
                }
                break;
            case EDGE_LEFT:
                mEdgeLeftX = Math.min(mEdgeLeftX + moveX, mEdgeRightX - MIN_GAP);
                break;
            case EDGE_TOP:
                mEdgeTopY = Math.min(mEdgeTopY + moveY, mEdgeBottomY - MIN_GAP);
                break;
            case EDGE_RIGHT:
                mEdgeRightX = Math.max(mEdgeRightX + moveX, mEdgeLeftX + MIN_GAP);
                break;
            case EDGE_BOTTOM:
                mEdgeBottomY = Math.max(mEdgeBottomY + moveY, mEdgeTopY + MIN_GAP);
                break;
            case CONNER_LEFT_TOP:
                mEdgeLeftX = Math.min(mEdgeLeftX + moveX, mEdgeRightX - MIN_GAP);
                mEdgeTopY = Math.min(mEdgeTopY + moveY, mEdgeBottomY - MIN_GAP);
                break;
            case CONNER_RIGHT_TOP:
                mEdgeRightX = Math.max(mEdgeRightX + moveX, mEdgeLeftX + MIN_GAP);
                mEdgeTopY = Math.min(mEdgeTopY + moveY, mEdgeBottomY - MIN_GAP);
                break;
            case CONNER_LEFT_BOTTOM:
                mEdgeLeftX = Math.min(mEdgeLeftX + moveX, mEdgeRightX - MIN_GAP);
                mEdgeBottomY = Math.max(mEdgeBottomY + moveY, mEdgeTopY + MIN_GAP);
                break;
            case CONNER_RIGHT_BOTTOM:
                mEdgeRightX = Math.max(mEdgeRightX + moveX, mEdgeLeftX + MIN_GAP);
                mEdgeBottomY = Math.max(mEdgeBottomY + moveY, mEdgeTopY + MIN_GAP);
                break;
            default:
                break;
        }
        // 不能移除图片边界
        mEdgeLeftX = Math.max(mEdgeLeftX, mBorderRect.left);
        mEdgeTopY = Math.max(mEdgeTopY, mBorderRect.top);
        mEdgeRightX = Math.min(mEdgeRightX, mBorderRect.right);
        mEdgeBottomY = Math.min(mEdgeBottomY, mBorderRect.bottom);
    }

    private EdgeCorner nearestEdge(float x, float y) {
        boolean nearLeft = Math.abs(x - mEdgeLeftX) < 30;
        boolean nearTop = Math.abs(y - mEdgeTopY) < 30;
        boolean nearRight = Math.abs(x - mEdgeRightX) < 30;
        boolean nearBottom = Math.abs(y - mEdgeBottomY) < 30;
        if (nearLeft && nearTop && nearRight && nearBottom)
            return EdgeCorner.INSIDE;
        else if (nearLeft && nearTop)
            return EdgeCorner.CONNER_LEFT_TOP;
        else if (nearLeft && nearBottom)
            return EdgeCorner.CONNER_LEFT_BOTTOM;
        else if (nearRight && nearTop)
            return EdgeCorner.CONNER_RIGHT_TOP;
        else if (nearRight && nearBottom)
            return EdgeCorner.CONNER_RIGHT_BOTTOM;
        else if (nearLeft)
            return EdgeCorner.EDGE_LEFT;
        else if (nearTop)
            return EdgeCorner.EDGE_TOP;
        else if (nearRight)
            return EdgeCorner.EDGE_RIGHT;
        else if (nearBottom)
            return EdgeCorner.EDGE_BOTTOM;
        else if (x < mEdgeRightX && x > mEdgeLeftX && y > mEdgeTopY && y < mEdgeBottomY)
            return EdgeCorner.INSIDE;
        return EdgeCorner.OUTSIDE;
    }

    public enum EdgeCorner {
        OUTSIDE,
        INSIDE,
        EDGE_LEFT,
        EDGE_TOP,
        EDGE_RIGHT,
        EDGE_BOTTOM,
        CONNER_LEFT_TOP,
        CONNER_RIGHT_TOP,
        CONNER_LEFT_BOTTOM,
        CONNER_RIGHT_BOTTOM;
    }

    /**
     * 获取图片变换后对应的Rect范围
     */
    private RectF getBitmapRect() {
        final Drawable drawable = getDrawable();
        if (drawable == null) {
            return new RectF();
        }

        final int drawableIntrinsicWidth = drawable.getIntrinsicWidth();
        final int drawableIntrinsicHeight = drawable.getIntrinsicHeight();
        Log.d(TAG, "图片真实尺寸：" + drawableIntrinsicWidth + " X " + drawableIntrinsicHeight);

        final int drawableDisplayWidth = Math.round(drawableIntrinsicWidth * mScaleX);
        final int drawableDisplayHeight = Math.round(drawableIntrinsicHeight * mScaleY);

        final float left = Math.max(mTransX, 0);// 超出边界取0
        final float top = Math.max(mTransY, 0);
        final float right = Math.min(left + drawableDisplayWidth, getWidth());// 超出边界取边
        final float bottom = Math.min(top + drawableDisplayHeight, getHeight());

        return new RectF(left, top, right, bottom);
    }

    /**
     * 将裁剪框变换后传递到外部，用于对原图进行裁切
     */
    public int[] getCropAttrs() {
        final Drawable drawable = getDrawable();
        if (drawable == null) {
            return new int[]{0, 0, 0, 0};
        }

        Log.d(TAG, "cropRect: " + mEdgeRect.toString());

        int x = (int) ((mEdgeLeftX - mTransX) / mScaleX);
        int y = (int) ((mEdgeTopY - mTransY) / mScaleY);
        int w = (int) ((mEdgeRightX - mEdgeLeftX) / mScaleX);
        int h = (int) ((mEdgeBottomY - mEdgeTopY) / mScaleY);

        int[] cropAttrs = {x, y, w, h};
        return cropAttrs;
    }
}
