package com.adolf.opencvstudy.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
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
 * @description: 四角可以移动的裁剪框。setOrgCorners()设置初始位置，getCropCorners()获取裁剪框对应四点
 * @author: Adolf
 * @create: 2020-09-08 16:49
 **/
public class FreedomCropView extends androidx.appcompat.widget.AppCompatImageView {
    private static final String TAG = "[jq]FreedomCropView";
    private static final float CORNER_TOUCH_AREA = 50.0f;

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
    private Paint mErrLinePaint;
    private Paint mErrCornerPaint;


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
        initCorner();
    }

    private void init() {
        mLinePaint = new Paint();
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeWidth(5);
        mLinePaint.setColor(Color.parseColor("#AA99cc00"));

        mCornerPaint = new Paint();
        mCornerPaint.setStyle(Paint.Style.FILL);
        mCornerPaint.setStrokeWidth(20);
        mCornerPaint.setColor(Color.parseColor("#AA99ff00"));

        mErrLinePaint = new Paint();
        mErrLinePaint.setStyle(Paint.Style.STROKE);
        mErrLinePaint.setStrokeWidth(5);
        mErrLinePaint.setColor(Color.parseColor("#AAff6600"));

        mErrCornerPaint = new Paint();
        mErrCornerPaint.setStyle(Paint.Style.FILL);
        mErrCornerPaint.setStrokeWidth(20);
        mErrCornerPaint.setColor(Color.parseColor("#AAff0033"));
    }

    /**
     * 将传入的Corners付给对应的Point
     */
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
        // Log.i(TAG, "角点初始化成功：lt="+lt.toString()+", rt="+rt.toString()+", rb="+rb.toString()+", lb="+lb.toString());
        postInvalidate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        initMatrix();
        mDisplayRect = getImgDisplayRect();
        Log.d(TAG, "图片展示区域: " + mDisplayRect);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (isAllAngleValid()) {
            Log.d(TAG, "onDraw: 角度允许");
            canvas.drawLine(lb.x, lb.y, lt.x, lt.y, mLinePaint);
            canvas.drawLine(lt.x, lt.y, rt.x, rt.y, mLinePaint);
            canvas.drawLine(rt.x, rt.y, rb.x, rb.y, mLinePaint);
            canvas.drawLine(rb.x, rb.y, lb.x, lb.y, mLinePaint);

            canvas.drawCircle(lt.x, lt.y, 10, mCornerPaint);
            canvas.drawCircle(lb.x, lb.y, 10, mCornerPaint);
            canvas.drawCircle(rt.x, rt.y, 10, mCornerPaint);
            canvas.drawCircle(rb.x, rb.y, 10, mCornerPaint);
        } else {
            Log.d(TAG, "onDraw: 非法角度");
            canvas.drawLine(lb.x, lb.y, lt.x, lt.y, mErrLinePaint);
            canvas.drawLine(lt.x, lt.y, rt.x, rt.y, mErrLinePaint);
            canvas.drawLine(rt.x, rt.y, rb.x, rb.y, mErrLinePaint);
            canvas.drawLine(rb.x, rb.y, lb.x, lb.y, mErrLinePaint);

            canvas.drawCircle(lt.x, lt.y, 10, mErrCornerPaint);
            canvas.drawCircle(lb.x, lb.y, 10, mErrCornerPaint);
            canvas.drawCircle(rt.x, rt.y, 10, mErrCornerPaint);
            canvas.drawCircle(rb.x, rb.y, 10, mErrCornerPaint);
        }
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
                mTouchX = -1;
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
                if (lt.x == mDisplayRect.left + 0 || lt.y == mDisplayRect.top + 0 ||
                        rt.x == mDisplayRect.right - 0 || rt.y == mDisplayRect.top + 0 ||
                        lb.x == mDisplayRect.left + 0 || lb.y == mDisplayRect.bottom - 0 ||
                        rb.x == mDisplayRect.right - 0 || rb.y == mDisplayRect.bottom - 0) {
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

    /**
     * 移动操作
     *
     * @param org   原始坐标
     * @param move  移动的偏移
     * @param lower 移动后的下限
     * @param upper 移动后的上限
     * @return 返回移动后的值
     */
    private float move(float org, float move, float lower, float upper) {
        if (org + move > lower && org + move < upper) {
            org += move;
        } else if (org + move < lower) {
            org = lower;
        } else if (org + move > upper) {
            org = upper;
        }
        return org;
    }


    private boolean isAllAngleValid() {
        return cosAngle(lt, lb, rt) && cosAngle(rt, lt, rb) && cosAngle(rb, rt, lb) && cosAngle(lb, rb, lt);
    }

    private boolean cosAngle(PointF pointA, PointF pointB, PointF pointC) {
        double lengthAB = Math.sqrt(Math.pow(pointA.x - pointB.x, 2) + Math.pow(pointA.y - pointB.y, 2)),
                lengthAC = Math.sqrt(Math.pow(pointA.x - pointC.x, 2) + Math.pow(pointA.y - pointC.y, 2)),
                lengthBC = Math.sqrt(Math.pow(pointB.x - pointC.x, 2) + Math.pow(pointB.y - pointC.y, 2));
        double cosA = (Math.pow(lengthAB, 2) + Math.pow(lengthAC, 2) - Math.pow(lengthBC, 2)) / (2 * lengthAB * lengthAC);
        // float angleA = Math.round( Math.acos(cosA) * 180 / Math.PI );
        Log.d(TAG, "cosAngle: " + Math.round(Math.acos(cosA) * 180 / Math.PI));
        return cosA > -Math.sqrt(2) / 2 && cosA < Math.sqrt(2) / 2;
    }


    public enum Corner {
        OUTSIDE,
        INSIDE,
        CORNER_LEFT_TOP,
        CORNER_RIGHT_TOP,
        CORNER_LEFT_BOTTOM,
        CORNER_RIGHT_BOTTOM;

        private static Corner getCloseTo(float x, float y, PointF lt, PointF rt, PointF rb, PointF lb) {
            boolean close2LT = Math.sqrt(Math.pow(lt.x - x, 2) + Math.pow(lt.y - y, 2)) < CORNER_TOUCH_AREA;
            boolean close2RT = Math.sqrt(Math.pow(rt.x - x, 2) + Math.pow(rt.y - y, 2)) < CORNER_TOUCH_AREA;
            boolean close2RB = Math.sqrt(Math.pow(rb.x - x, 2) + Math.pow(rb.y - y, 2)) < CORNER_TOUCH_AREA;
            boolean close2LB = Math.sqrt(Math.pow(lb.x - x, 2) + Math.pow(lb.y - y, 2)) < CORNER_TOUCH_AREA;
            boolean outside = (x > rt.x || x > rb.x) || (x < lt.x || x < lb.x) || (y > lb.y || y > rb.y) || (y < lt.y || y < rt.y);

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
            } else {
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
     * 提供外界截图框对应的点坐标
     */
    public List<org.opencv.core.Point> getCropCorners() {
        List<org.opencv.core.Point> cropCorners = new ArrayList<>();
        if (isAllAngleValid()) {
            cropCorners.add(new org.opencv.core.Point((lt.x - mTransX) / mScaleX, (lt.y - mTransY) / mScaleY));
            cropCorners.add(new org.opencv.core.Point((rt.x - mTransX) / mScaleX, (rt.y - mTransY) / mScaleY));
            cropCorners.add(new org.opencv.core.Point((rb.x - mTransX) / mScaleX, (rb.y - mTransY) / mScaleY));
            cropCorners.add(new org.opencv.core.Point((lb.x - mTransX) / mScaleX, (lb.y - mTransY) / mScaleY));
        } else {

        }
        return cropCorners;
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

        Log.i(TAG, "Matrix[ 缩放X:" + mScaleX + ", 缩放Y:" + mScaleX + ", 平移X:" + mTransX + ", 平移Y:" + mTransY + "]");
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
