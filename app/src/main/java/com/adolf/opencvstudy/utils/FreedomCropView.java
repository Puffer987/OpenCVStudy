package com.adolf.opencvstudy.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @program: OpenCVStudy
 * @description:
 * @author: Adolf
 * @create: 2020-09-08 16:49
 **/
public class FreedomCropView extends androidx.appcompat.widget.AppCompatImageView {
    private static final String TAG = "[jq]FreedomCropView";
    private static final float KEY_CLOSE = 20.0f;

    private List<PointF> mOrgCorners = new ArrayList<>();
    private PointF lt = new PointF(-10, -10);
    private PointF rt = new PointF(-10, -10);
    private PointF rb = new PointF(-10, -10);
    private PointF lb = new PointF(-10, -10);
    private Paint mLinePaint;
    private Paint mCornerPaint;

    private float[] mMatrixValues = new float[9];
    private float mScaleX;
    private float mScaleY;
    private float mTransX;
    private float mTransY;
    private RectF mDisplayRect;
    private Corner mCloseTo;
    private float mTouchX;
    private float mTouchY;


    public FreedomCropView(@NonNull Context context) {
        super(context);
        init();
    }

    public FreedomCropView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setOrgCorners(List<PointF> points) {
        if (points.size() != 4)
            return;
        this.mOrgCorners = points;
    }

    private void init() {
        mLinePaint = new Paint();
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeWidth(5);
        mLinePaint.setColor(Color.parseColor("#AAFFCCCC"));

        mCornerPaint = new Paint();
        mCornerPaint.setStyle(Paint.Style.FILL);
        mCornerPaint.setStrokeWidth(20);
        mCornerPaint.setColor(Color.parseColor("#AAFF6666"));
    }

    private void initCorner() {
        PointF center = getCenterPoint(mOrgCorners);
        for (int i = 0; i < mOrgCorners.size(); i++) {
            if (mOrgCorners.get(i).x < center.x && mOrgCorners.get(i).y < center.y) {
                this.lt = new PointF(mOrgCorners.get(i).x * mScaleX + mTransX, mOrgCorners.get(i).y * mScaleY + mTransY);
            } else if (mOrgCorners.get(i).x > center.x && mOrgCorners.get(i).y < center.y) {
                this.rt = new PointF(mOrgCorners.get(i).x * mScaleX + mTransX, mOrgCorners.get(i).y * mScaleY + mTransY);
            } else if (mOrgCorners.get(i).x > center.x && mOrgCorners.get(i).y > center.y) {
                this.rb = new PointF(mOrgCorners.get(i).x * mScaleX + mTransX, mOrgCorners.get(i).y * mScaleY + mTransY);
            } else if (mOrgCorners.get(i).x < center.x && mOrgCorners.get(i).y > center.y) {
                this.lb = new PointF(mOrgCorners.get(i).x * mScaleX + mTransX, mOrgCorners.get(i).y * mScaleY + mTransY);
            } else {
                Log.e(TAG, "err");
            }
        }
        Log.i(TAG, "角点初始化成功！！");
        postInvalidate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        initMatrix();
        if (mScaleX != 1 || mScaleY != 1 || mTransX != 0 || mTransY != 0)
            initCorner();
        mDisplayRect = getImgDisplayRect();
        Log.d(TAG, "图片展示区域: " + mDisplayRect);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawLine(lb.x, lb.y, lt.x, lt.y, mLinePaint);
        canvas.drawLine(lt.x, lt.y, rt.x, rt.y, mLinePaint);
        canvas.drawLine(rt.x, rt.y, rb.x, rb.y, mLinePaint);
        canvas.drawLine(rb.x, rb.y, lb.x, lb.y, mLinePaint);

        canvas.drawCircle(lt.x, lt.y, 10, mCornerPaint);
        canvas.drawCircle(lb.x, lb.y, 10, mCornerPaint);
        canvas.drawCircle(rt.x, rt.y, 10, mCornerPaint);
        canvas.drawCircle(rb.x, rb.y, 10, mCornerPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouchX = event.getX();
                mTouchY = event.getY();
                mCloseTo = Corner.getCloseTo(mTouchX, mTouchY, lt, rt, rb, lb);
                return true;
            case MotionEvent.ACTION_UP:
                mTransX = -1;
                mTouchY = -1;
                return true;
            case MotionEvent.ACTION_MOVE:
                float preTouchX = mTouchX;
                float preTouchY = mTouchY;
                mTouchX = event.getX();
                mTouchY = event.getY();
                moveCorner(preTouchX, preTouchY);
                postInvalidate();
                return true;
            default:
                return false;
        }
    }

    private void moveCorner(float preTouchX, float preTouchY) {
        float moveX = mTouchX - preTouchX;
        float moveY = mTouchY - preTouchY;
        switch (mCloseTo) {

            case CORNER_LEFT_TOP:
                lt.x = move(lt.x, moveX, mDisplayRect.left, rt.x);
                lt.y = move(lt.y, moveY, mDisplayRect.top, lb.y);
                break;
            case CORNER_RIGHT_TOP:
                rt.x = move(rt.x, moveX, lt.x, mDisplayRect.right);
                rt.y = move(rt.y, moveY, mDisplayRect.top, rb.y);
                break;
            case CORNER_LEFT_BOTTOM:
                lb.x = move(lb.x, moveX, mDisplayRect.left, rb.x);
                lb.y = move(lb.y, moveY, lt.y, mDisplayRect.bottom);
                break;
            case CORNER_RIGHT_BOTTOM:
                rb.x = move(rb.x, moveX, lb.x, mDisplayRect.right);
                rb.y = move(rb.y, moveY, rt.y, mDisplayRect.bottom);
                break;
            case INSIDE:
                float[] temp = {lt.x, lt.y, rt.x, rt.y, lb.x, lb.y, rb.x, rb.y};

                lt.x = move(lt.x, moveX, mDisplayRect.left, rt.x);
                lt.y = move(lt.y, moveY, mDisplayRect.top, lb.y);
                rt.x = move(rt.x, moveX, lt.x, mDisplayRect.right);
                rt.y = move(rt.y, moveY, mDisplayRect.top, rb.y);
                lb.x = move(lb.x, moveX, mDisplayRect.left, rb.x);
                lb.y = move(lb.y, moveY, lt.y, mDisplayRect.bottom);
                rb.x = move(rb.x, moveX, lb.x, mDisplayRect.right);
                rb.y = move(rb.y, moveY, rt.y, mDisplayRect.bottom);
                if (lt.x == mDisplayRect.left + KEY_CLOSE || lt.y == mDisplayRect.top + KEY_CLOSE ||
                        rt.x == mDisplayRect.right - KEY_CLOSE || rt.y == mDisplayRect.top + KEY_CLOSE ||
                        lb.x == mDisplayRect.left + KEY_CLOSE || lb.y == mDisplayRect.bottom - KEY_CLOSE ||
                        rb.x == mDisplayRect.right - KEY_CLOSE || rb.y == mDisplayRect.bottom - KEY_CLOSE) {
                    lt.x = temp[0];
                    lt.y = temp[1];
                    rt.x = temp[2];
                    rt.y = temp[3];
                    lb.x = temp[4];
                    lb.y = temp[5];
                    rb.x = temp[6];
                    rb.y = temp[7];
                }
                break;
            case OUTSIDE:
                break;
        }
    }

    private float move(float org, float move, float lower, float upper) {
        if (org + move > lower + KEY_CLOSE && org + move < upper - KEY_CLOSE) {
            org += move;
        } else if (org + move < lower + KEY_CLOSE) {
            org = lower + KEY_CLOSE;
        } else if (org + move > upper - KEY_CLOSE) {
            org = upper - KEY_CLOSE;
        }
        return org;
    }

    public enum Corner {
        OUTSIDE,
        INSIDE,
        CORNER_LEFT_TOP,
        CORNER_RIGHT_TOP,
        CORNER_LEFT_BOTTOM,
        CORNER_RIGHT_BOTTOM;

        private static Corner getCloseTo(float x, float y, PointF lt, PointF rt, PointF rb, PointF lb) {
            boolean close2LT = Math.sqrt(Math.pow(lt.x - x, 2) + Math.pow(lt.y - y, 2)) < KEY_CLOSE;
            boolean close2RT = Math.sqrt(Math.pow(rt.x - x, 2) + Math.pow(rt.y - y, 2)) < KEY_CLOSE;
            boolean close2RB = Math.sqrt(Math.pow(rb.x - x, 2) + Math.pow(rb.y - y, 2)) < KEY_CLOSE;
            boolean close2LB = Math.sqrt(Math.pow(lb.x - x, 2) + Math.pow(lb.y - y, 2)) < KEY_CLOSE;
            boolean outside = (x > rt.x || x > rb.x) || (x < lt.x || x < lb.x)||(y > lb.y || y > rb.y) || (y < lt.y || y < rt.y);

            if (close2LT) {
                return Corner.CORNER_LEFT_TOP;
            } else if (close2RT) {
                return Corner.CORNER_RIGHT_TOP;
            } else if (close2RB) {
                return Corner.CORNER_RIGHT_BOTTOM;
            } else if (close2LB) {
                return Corner.CORNER_LEFT_BOTTOM;
            } else if (outside) {
                return Corner.OUTSIDE;
            }else {
                return Corner.INSIDE;
            }

        }
    }

    private PointF getCenterPoint(List<PointF> corners) {
        double[] arrX = {0, 0, 0, 0};
        double[] arrY = {0, 0, 0, 0};
        for (int i = 0; i < corners.size(); i++) {
            arrX[i] = corners.get(i).x;
            arrY[i] = corners.get(i).y;
        }
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 3; j++) {
                if (arrX[j] > arrX[j + 1]) {
                    double temp = arrX[j];
                    arrX[j] = arrX[j + 1];
                    arrX[j + 1] = temp;
                }
                if (arrY[j] > arrY[j + 1]) {
                    double temp = arrY[j];
                    arrY[j] = arrY[j + 1];
                    arrY[j + 1] = temp;
                }
            }
        }
        return new PointF((float) ((arrX[1] + arrX[2]) / 2), (float) ((arrY[1] + arrY[2]) / 2));
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
        Log.d(TAG, "Matrix[ 缩放X:" + mScaleX + ", 缩放Y:" + mScaleX
                + ", 平移X:" + mTransX + ", 平移Y:" + mTransY + "]");
    }

    /**
     * 获取图片变换后对应的Rect范围
     */
    private RectF getImgDisplayRect() {
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
}