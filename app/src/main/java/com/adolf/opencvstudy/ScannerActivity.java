package com.adolf.opencvstudy;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import com.adolf.opencvstudy.rv.ImgRVAdapter;
import com.adolf.opencvstudy.rv.ItemRVBean;
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
    @BindView(R.id.btn_do)
    Button mBtnDo;

    private List<ItemRVBean> mRVBeanList = new ArrayList<>();
    private File mImgCachePath;
    private SaveImgUtil mImgUtil;
    private Bitmap mOrgBtm;
    private Mat mSrc;
    private ProgressDialog progressDialog;
    private double mScaleFactor = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        ButterKnife.bind(this);
        mImgCachePath = new File(getExternalFilesDir(null), "/process");

        mImgUtil = new SaveImgUtil(mImgCachePath, mRVBeanList);

        String path = getIntent().getStringExtra("img");
        mOrgBtm = BitmapFactory.decodeFile(path);

        mSrc = new Mat();
        Utils.bitmapToMat(mOrgBtm, mSrc);
        Imgproc.cvtColor(mSrc, mSrc, Imgproc.COLOR_RGB2BGRA);


        initProgressDialog();

        progressDialog.show();
        new Thread(() -> {
            // correctPerspective(mSrc);
            houghLines(mSrc);
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

    @OnClick(R.id.btn_do)
    public void onViewClicked() {
        mBtnDo.setEnabled(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                // scan(mOrgBtm);
                // findCanny(mSrc);
                runOnUiThread(() -> showImg());
            }
        }).start();

    }

    private boolean exists(List<Point> corners, Point pt) {
        for (int i = 0; i < corners.size(); i++)
            // 到已存在点的距离
            if (Math.sqrt(Math.pow(corners.get(i).x - pt.x, 2) +
                    Math.pow(corners.get(i).y - pt.y, 2)) < 100) {
                return true;
            }
        return false;
    }

    private Point FindIntersection(double[] line1, double[] line2) {

        double x1 = line1[0], y1 = line1[1], x2 = line1[2], y2 = line1[3],
                x3 = line2[0], y3 = line2[1], x4 = line2[2], y4 = line2[3];
        double denominator = ((x1 - x2) * (y3 - y4)) - ((y1 - y2) * (x3 - x4));
        // Log.d(TAG, "denominator: "+denominator+"; line1:(" + line1[0] + ", " + line1[1] + "," + line1[2] + ", " + line1[3] + ")"
        //         + "; line2:(" + line2[0] + ", " + line2[1] + "," + line2[2] + ", " + line1[3] + ")");
        // Log.d(TAG, "denominator: "+denominator);
        if (Math.abs(denominator) != 0) {
            Point pt = new Point();
            pt.x = ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / denominator;
            pt.y = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / denominator;
            return pt;
        } else
            return new Point(-1, -1);
    }

    private void houghLines(Mat src) {
        Mat dst = new Mat();
        mImgUtil.saveMat(src, "原图");

        Imgproc.cvtColor(src, dst, Imgproc.COLOR_BGR2GRAY);
        mImgUtil.saveMat(dst, "gray");

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
            perspective(src, corners);
        }else{
            MatOfPoint2f mp = new MatOfPoint2f();
            Rect rect = Imgproc.boundingRect((Mat) corners);
            Log.d(TAG, "rect: "+rect.toString());
        }
    }

    public void perspective(Mat src, List<Point> orgCorners) {
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

    public void correctPerspective(Mat src) {
        Mat imgSource = src.clone();
        // convert the image to black and white does (8 bit)
        Imgproc.Canny(imgSource.clone(), imgSource, 50, 50);
        mImgUtil.saveMat(imgSource, "Canny");
        // apply gaussian blur to smoothen lines of dots
        Imgproc.GaussianBlur(imgSource, imgSource, new org.opencv.core.Size(5, 5), 5);
        mImgUtil.saveMat(imgSource, "GaussianBlur");
        // find the contours
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(imgSource, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        Mat d = new Mat();
        Imgproc.drawContours(d, contours, -1, new Scalar(255));
        mImgUtil.saveMat(d, "边界");

        double maxArea = -1;
        MatOfPoint temp_contour = contours.get(0); // the largest is at the index 0 for starting point
        MatOfPoint2f approxCurve = new MatOfPoint2f();

        for (int idx = 0; idx < contours.size(); idx++) {
            temp_contour = contours.get(idx);
            double contourarea = Imgproc.contourArea(temp_contour);
            // compare this contour to the previous largest contour found
            if (contourarea > maxArea) {
                // check if this contour is a square
                MatOfPoint2f new_mat = new MatOfPoint2f(temp_contour.toArray());
                int contourSize = (int) temp_contour.total();
                MatOfPoint2f approxCurve_temp = new MatOfPoint2f();
                Imgproc.approxPolyDP(new_mat, approxCurve_temp, contourSize * 0.05, true);
                if (approxCurve_temp.total() == 4) {
                    maxArea = contourarea;
                    approxCurve = approxCurve_temp;
                }
            }
        }

        Imgproc.cvtColor(imgSource, imgSource, Imgproc.COLOR_BayerBG2RGB);
        Mat sourceImage = src.clone();
        double[] temp_double;
        temp_double = approxCurve.get(0, 0);
        Point p1 = new Point(temp_double[0], temp_double[1]);
        // Core.circle(imgSource,p1,55,new Scalar(0,0,255));
        // Imgproc.warpAffine(sourceImage, dummy, rotImage,sourceImage.size());
        temp_double = approxCurve.get(1, 0);
        Point p2 = new Point(temp_double[0], temp_double[1]);
        // Core.circle(imgSource,p2,150,new Scalar(255,255,255));
        temp_double = approxCurve.get(2, 0);
        Point p3 = new Point(temp_double[0], temp_double[1]);
        // Core.circle(imgSource,p3,200,new Scalar(255,0,0));
        temp_double = approxCurve.get(3, 0);
        Point p4 = new Point(temp_double[0], temp_double[1]);
        // Core.circle(imgSource,p4,100,new Scalar(0,0,255));
        List<Point> corners = new ArrayList<Point>();
        corners.add(p1);
        corners.add(p2);
        corners.add(p3);
        corners.add(p4);

        Mat draw = new Mat();
        draw.create(src.rows(), src.cols(), CvType.CV_8UC3);
        for (int i = 0; i < corners.size(); i++) {
            Log.d(TAG, i + "corner:" + corners.get(i).toString());
            Imgproc.circle(draw, corners.get(i), 5, new Scalar(255, 255, 0, 255), 10);
        }
        mImgUtil.saveMat(draw, "角点");
        Mat startM = Converters.vector_Point2f_to_Mat(corners);
        // Mat result = warp(sourceImage, source);

        perspective(sourceImage, corners);

        // mImgUtil.saveMat(result, "结果");
    }

    public static Mat warp(Mat inputMat, List<Point> corners) {


        double top = Math.sqrt(Math.pow(corners.get(0).x - corners.get(1).x, 2) + Math.pow(corners.get(0).y - corners.get(1).y, 2));
        double right = Math.sqrt(Math.pow(corners.get(1).x - corners.get(2).x, 2) + Math.pow(corners.get(1).y - corners.get(2).y, 2));
        double bottom = Math.sqrt(Math.pow(corners.get(2).x - corners.get(3).x, 2) + Math.pow(corners.get(2).y - corners.get(3).y, 2));
        double left = Math.sqrt(Math.pow(corners.get(3).x - corners.get(1).x, 2) + Math.pow(corners.get(3).y - corners.get(1).y, 2));
        Mat quad = Mat.zeros(new Size(Math.max(top, bottom), Math.max(left, right)),
                CvType.CV_8UC3);

        int resultWidth = quad.width();
        int resultHeight = quad.height();

        Point ocvPOut4 = new Point(0, 0);
        Point ocvPOut1 = new Point(0, resultHeight);
        Point ocvPOut2 = new Point(resultWidth, resultHeight);
        Point ocvPOut3 = new Point(resultWidth, 0);
        // Point ocvPOut1 = new Point(0, 0);
        // Point ocvPOut2 = new Point(0, resultHeight);
        // Point ocvPOut3 = new Point(resultWidth, resultHeight);
        // Point ocvPOut4 = new Point(resultWidth, 0);

        if (inputMat.height() > inputMat.width()) {
            // int temp = resultWidth;
            // resultWidth = resultHeight;
            // resultHeight = temp;

            ocvPOut3 = new Point(0, 0);
            ocvPOut4 = new Point(0, resultHeight);
            ocvPOut1 = new Point(resultWidth, resultHeight);
            ocvPOut2 = new Point(resultWidth, 0);
        }

        Mat outputMat = new Mat(resultWidth, resultHeight, CvType.CV_8UC4);

        List<Point> dest = new ArrayList<Point>();
        dest.add(ocvPOut1);
        dest.add(ocvPOut2);
        dest.add(ocvPOut3);
        dest.add(ocvPOut4);

        Mat endM = Converters.vector_Point2f_to_Mat(dest);
        Mat startM = Converters.vector_Point2f_to_Mat(corners);

        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(startM, endM);

        Imgproc.warpPerspective(inputMat, outputMat, perspectiveTransform, new Size(resultWidth, resultHeight), Imgproc.INTER_CUBIC);

        return outputMat;
    }

    public void sortCorners(List<Point> corners) {
        List<Point> top = new ArrayList<>(), bottom = new ArrayList<>();
        Point center = new Point();

        for (int i = 0; i < corners.size(); i++) {
            center.x += corners.get(i).x / corners.size();
            center.y += corners.get(i).y / corners.size();
        }
        for (int i = 0; i < corners.size(); i++) {
            if (corners.get(i).y < center.y)
                top.add(corners.get(i));
            else
                bottom.add(corners.get(i));
        }
        corners.clear();

        if (top.size() == 2 && bottom.size() == 2) {
            Point top_left = top.get(0).x > top.get(1).x ?
                    top.get(1) : top.get(0);
            Point top_right = top.get(0).x > top.get(1).x ?
                    top.get(0) : top.get(1);
            Point bottom_left = bottom.get(0).x > bottom.get(1).x ?
                    bottom.get(1) : bottom.get(0);
            Point bottom_right = bottom.get(0).x > bottom.get(1).x ?
                    bottom.get(0) : bottom.get(1);
            top_left.x *= mScaleFactor;
            top_left.y *= mScaleFactor;
            top_right.x *= mScaleFactor;
            top_right.y *= mScaleFactor;
            bottom_left.x *= mScaleFactor;
            bottom_left.y *= mScaleFactor;
            bottom_right.x *= mScaleFactor;
            bottom_right.y *= mScaleFactor;
            corners.add(top_left);
            corners.add(top_right);
            corners.add(bottom_right);
            corners.add(bottom_left);
        }
    }

    private void showImg() {
        ImgRVAdapter adapter = new ImgRVAdapter(mRVBeanList, this);
        GridLayoutManager manager = new GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false);
        mRvImgs.setLayoutManager(manager);
        mRvImgs.setAdapter(adapter);
        mBtnDo.setEnabled(true);
        progressDialog.dismiss();
    }
}