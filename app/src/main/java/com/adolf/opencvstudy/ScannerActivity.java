package com.adolf.opencvstudy;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.adolf.opencvstudy.rv.ImgRVAdapter;
import com.adolf.opencvstudy.rv.ItemRVBean;
import com.adolf.opencvstudy.utils.FreedomCropView;
import com.adolf.opencvstudy.utils.SaveImgUtil;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ScannerActivity extends AppCompatActivity {
    private static final String TAG = "[jq]ScannerActivity";
    @BindView(R.id.rv_imgs)
    RecyclerView mRvImgs;
    @BindView(R.id.fcv)
    FreedomCropView mFcv;
    @BindView(R.id.iv_crop)
    ImageView mIvCrop;

    private List<ItemRVBean> mRVBeanList = new ArrayList<>();
    private File mImgCachePath;
    private SaveImgUtil mImgUtil;
    private Bitmap mOrgBtm;
    private Mat mSrc;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        ButterKnife.bind(this);

        mImgCachePath = new File(getExternalFilesDir(null), "/process");
        mImgUtil = new SaveImgUtil(mImgCachePath, mRVBeanList);

        String path = getIntent().getStringExtra("img");
        mOrgBtm = BitmapFactory.decodeFile(path);
        mFcv.setImageBitmap(mOrgBtm);
        mIvCrop.setImageBitmap(mOrgBtm);

        mSrc = new Mat();
        Utils.bitmapToMat(mOrgBtm, mSrc);
        Imgproc.cvtColor(mSrc, mSrc, Imgproc.COLOR_RGB2BGRA);

        initProgressDialog();

        progressDialog.show();
        new Thread(() -> {
            // correctPerspective(mSrc);
            scanning(mSrc);
            // findCanny(mSrc);
            runOnUiThread(() -> showImg());
        }).start();
    }

    private void initProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(false);//循环滚动
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage("loading...");
        progressDialog.setCancelable(false);//false不能取消显示，true可以取消显示
    }

    private void scanning(Mat src) {
        Mat dst = new Mat();
        // mImgUtil.saveMat(src, "原图");

        Imgproc.cvtColor(src, dst, Imgproc.COLOR_BGR2GRAY);
        // mImgUtil.saveMat(dst, "gray");

        // Imgproc.GaussianBlur(dst, dst, new org.opencv.core.Size(7, 7), 5);
        // mImgUtil.saveMat(dst, "GaussianBlur");

        Imgproc.adaptiveThreshold(dst, dst, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 81, 5);
        mImgUtil.saveMat(dst, "自适应阈值");

        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));
        // Imgproc.morphologyEx(dst,dst, Imgproc.MORPH_OPEN,element);
        // mImgUtil.saveMat(dst, "开");

        // Imgproc.dilate(dst,dst,element);
        // mImgUtil.saveMat(dst, "膨胀");

        // Imgproc.Canny(dst, dst, 100, 50);
        // mImgUtil.saveMat(dst, "Canny");

        /**
         * contours : 所有的闭合轮廓
         * index：最大闭合轮廓的下标
         */
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(dst, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        int index = 0;
        double maxim = Imgproc.contourArea(contours.get(0));
        for (int i = 0; i < contours.size(); i++) {
            double t = Imgproc.contourArea(contours.get(i));
            if (maxim < t) {
                maxim = t;
                index = i;
            }
        }
        Mat drawing = new Mat(src.rows(), src.cols(), CvType.CV_8UC1);
        Imgproc.drawContours(drawing, contours, index, new Scalar(255), 1);
        mImgUtil.saveMat(drawing, "轮廓");

        /**
         * 轮廓多边形的点集
         * cure：原始轮廓
         * approxCure：输出的多边形点集
         */
        MatOfPoint2f cure = new MatOfPoint2f(contours.get(index).toArray());
        MatOfPoint2f approxCure = new MatOfPoint2f();
        Imgproc.approxPolyDP(cure, approxCure, contours.size() * 0.05, true);

        List<Point> corners = new ArrayList<>();
        for (int i = 0; i < approxCure.rows(); i++) {
            Point temp = new Point(approxCure.get(i, 0)[0], approxCure.get(i, 0)[1]);
            Log.d(TAG, "corners: " + temp.toString());
            Imgproc.circle(drawing, temp, 5, new Scalar(255), 10);
            corners.add(temp);
        }
        mImgUtil.saveMat(drawing, "point");

        if (corners.size() == 4) {
            List<PointF> points = new ArrayList<>();
            for (int i = 0; i < corners.size(); i++) {
                points.add(new PointF((int) corners.get(i).x, (int) corners.get(i).y));
            }
            mFcv.setOrgCorners(points);
            // perspective(src, corners);
        } else {
            Rect rect = Imgproc.boundingRect(approxCure);
            Point tl = rect.tl();
            Point br = rect.br();
            PointF lt = new PointF((float) tl.x, (float) tl.y);
            PointF rb = new PointF((float) br.x, (float) br.y);
            PointF rt = new PointF((float) tl.x, (float) br.y);
            PointF lb = new PointF((float) br.x, (float) tl.y);
            List<PointF> points = new ArrayList<>();
            points.add(lt);
            points.add(rt);
            points.add(rb);
            points.add(lb);
            mFcv.setOrgCorners(points);

            Log.d(TAG, "rect: " + rect.toString());
            Imgproc.rectangle(drawing, rect, new Scalar(255), 5);
            mImgUtil.saveMat(drawing, "rect");
        }
    }

    public Bitmap perspective(Mat src, List<Point> orgCorners) {
        List<Point> corners = new ArrayList<>();
        Point center = getCenterPoint(orgCorners);

        Point leftTop = null, rightTop = null, rightBottom = null, leftBottom = null;
        for (int i = 0; i < orgCorners.size(); i++) {
            if (orgCorners.get(i).x < center.x && orgCorners.get(i).y < center.y) {
                leftTop = new Point(orgCorners.get(i).x, orgCorners.get(i).y);
            } else if (orgCorners.get(i).x > center.x && orgCorners.get(i).y < center.y) {
                rightTop = new Point(orgCorners.get(i).x, orgCorners.get(i).y);
            } else if (orgCorners.get(i).x > center.x && orgCorners.get(i).y > center.y) {
                rightBottom = new Point(orgCorners.get(i).x, orgCorners.get(i).y);
            } else if (orgCorners.get(i).x < center.x && orgCorners.get(i).y > center.y) {
                leftBottom = new Point(orgCorners.get(i).x, orgCorners.get(i).y);
            } else {
                Log.e(TAG, "err");
            }
        }
        corners.add(leftTop);
        corners.add(rightTop);
        corners.add(rightBottom);
        corners.add(leftBottom);

        double top = Math.sqrt(Math.pow(leftTop.x - rightTop.x, 2) + Math.pow(leftTop.y - rightTop.y, 2));
        double right = Math.sqrt(Math.pow(rightTop.x - rightBottom.x, 2) + Math.pow(rightTop.y - rightBottom.y, 2));
        double bottom = Math.sqrt(Math.pow(leftBottom.x - rightBottom.x, 2) + Math.pow(leftBottom.y - rightBottom.y, 2));
        double left = Math.sqrt(Math.pow(leftTop.x - leftBottom.x, 2) + Math.pow(leftTop.y - leftBottom.y, 2));
        Mat quad = Mat.zeros(new Size(Math.max(top, bottom), Math.max(left, right)),
                CvType.CV_8UC3);
        Log.d(TAG, "quad: " + quad.size().toString());

        List<Point> result_pts = new ArrayList<Point>();
        Point p1 = new Point(0, 0);
        Point p2 = new Point(quad.cols(), 0);
        Point p3 = new Point(quad.cols(), quad.rows());
        Point p4 = new Point(0, quad.rows());

        result_pts.add(p1);
        result_pts.add(p2);
        result_pts.add(p3);
        result_pts.add(p4);

        Mat cornerPts = Converters.vector_Point2f_to_Mat(corners);
        Mat resultPts = Converters.vector_Point2f_to_Mat(result_pts);

        Mat transformation = Imgproc.getPerspectiveTransform(cornerPts, resultPts);
        Imgproc.warpPerspective(src, quad, transformation, quad.size());

        mImgUtil.saveMat(quad, "透视变换");

        Imgproc.cvtColor(quad, quad, Imgproc.COLOR_BGR2RGBA);
        Bitmap bitmap = Bitmap.createBitmap(quad.cols(), quad.rows(),
                Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(quad, bitmap);

        return bitmap;
    }

    private Point getCenterPoint(List<Point> corners) {
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
        return new Point((arrX[1] + arrX[2]) / 2, (arrY[1] + arrY[2]) / 2);
    }

    private void showImg() {
        ImgRVAdapter adapter = new ImgRVAdapter(mRVBeanList, this);
        GridLayoutManager manager = new GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false);
        mRvImgs.setLayoutManager(manager);
        mRvImgs.setAdapter(adapter);
        progressDialog.dismiss();
    }

    @OnClick(R.id.btn_crop)
    public void onViewClicked() {
        List<Point> cropCorners = mFcv.getCropCorners();
        Mat dd = mSrc.clone();
        for (Point p : cropCorners) {
            Imgproc.circle(dd, p, 5, new Scalar(0, 255, 0), 30);
        }
        // List<PointF> points = new ArrayList<>();
        // for (int i = 0; i < cropCorners.size(); i++) {
        //     points.add(new PointF((int) cropCorners.get(i).x, (int) cropCorners.get(i).y));
        // }
        // mFcv.setOrgCorners(points);

        mImgUtil.saveMat(dd, "cropCorners");
        Bitmap perspective = perspective2(mSrc, cropCorners);
        mIvCrop.setImageBitmap(perspective);
    }

    public Bitmap perspective2(Mat src, List<Point> corners) {
        double top = Math.sqrt(Math.pow(corners.get(0).x - corners.get(1).x, 2) + Math.pow(corners.get(0).y - corners.get(1).y, 2));
        double right = Math.sqrt(Math.pow(corners.get(1).x - corners.get(2).x, 2) + Math.pow(corners.get(1).y - corners.get(2).y, 2));
        double bottom = Math.sqrt(Math.pow(corners.get(3).x - corners.get(2).x, 2) + Math.pow(corners.get(3).y - corners.get(2).y, 2));
        double left = Math.sqrt(Math.pow(corners.get(0).x - corners.get(3).x, 2) + Math.pow(corners.get(0).y - corners.get(3).y, 2));
        Mat quad = Mat.zeros(new Size(Math.max(top, bottom), Math.max(left, right)), CvType.CV_8UC3);
        Log.d(TAG, "quad: " + quad.size().toString());

        List<Point> result_pts = new ArrayList<Point>();
        Point p1 = new Point(0, 0);
        Point p2 = new Point(quad.cols(), 0);
        Point p3 = new Point(quad.cols(), quad.rows());
        Point p4 = new Point(0, quad.rows());

        result_pts.add(p1);
        result_pts.add(p2);
        result_pts.add(p3);
        result_pts.add(p4);

        Mat cornerPts = Converters.vector_Point2f_to_Mat(corners);
        Mat resultPts = Converters.vector_Point2f_to_Mat(result_pts);

        Mat transformation = Imgproc.getPerspectiveTransform(cornerPts, resultPts);
        Imgproc.warpPerspective(src, quad, transformation, quad.size());

        mImgUtil.saveMat(quad, "透视变换");

        Imgproc.cvtColor(quad, quad, Imgproc.COLOR_BGR2RGBA);
        Bitmap bitmap = Bitmap.createBitmap(quad.cols(), quad.rows(),
                Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(quad, bitmap);

        return bitmap;
    }

}