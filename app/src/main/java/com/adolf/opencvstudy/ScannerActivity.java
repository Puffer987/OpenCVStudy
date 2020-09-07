package com.adolf.opencvstudy;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
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
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

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
                    Math.pow(corners.get(i).y - pt.y, 2)) < 25) {
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

        Imgproc.adaptiveThreshold(dst, dst, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 21, 3);
        mImgUtil.saveMat(dst, "自适应阈值");

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(dst, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
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

        Imgproc.Canny(drawing, drawing, 50, 100, 3);
        mImgUtil.saveMat(drawing, "canny");

        Mat line = new Mat();
        Imgproc.HoughLinesP(drawing, line, 1, Math.PI / 180, 100, 20, 100);
        Mat draw = new Mat();
        draw.create(dst.rows(), dst.cols(), CvType.CV_8UC3);
        Log.d(TAG, line.size() + ", " + line.cols() + ", " + line.rows());
        Log.d(TAG, "line: " + line.size());
        for (int r = 0; r < line.rows(); r++) {
            // for (int c = 0; c < line.cols(); c++) {
            double[] points = line.get(r, 0);
            double x1 = points[0], y1 = points[1], x2 = points[2], y2 = points[3];
            Point p1 = new Point(x1, y1);
            Point p2 = new Point(x2, y2);
            Imgproc.line(draw, p1, p2, new Scalar(0, 0, 255), 1);
            // }
        }
        mImgUtil.saveMat(draw, "line");

        Log.d("jq", line.size() + ", " + line.cols() + ", " + line.rows());
        List<Point> corners = new ArrayList<>();
        for (int i = 0; i < line.rows(); i++) {
            for (int j = i + 1; j < line.rows(); j++) {
                double[] line1 = line.get(i, 0);
                double[] line2 = line.get(j, 0);
                Point pt = FindIntersection(line1, line2);

                if ((pt.x >= 0 && pt.y >= 0 && pt.x <= draw.cols()) && pt.y < draw.rows()) {
                    if (!exists(corners, pt)) {
                        // Log.d(TAG, "line1:(" + line1[0] + ", " + line1[1] + "," + line1[2] + ", " + line1[3] + ")"
                        //         + "; line2:(" + line2[0] + ", " + line2[1] + "," + line2[2] + ", " + line1[3] + ")");
                        // Log.d(TAG, "i=" + i +"j="+j+ "; pt: " + pt.x + "," + pt.y);
                        //
                        // double x1 = line1[0], y1 = line1[1], x2 = line1[2], y2 = line1[3],
                        //         x3 = line2[0], y3 = line2[1], x4 = line2[2], y4 = line2[3];
                        // double denominator = ((x1 - x2) * (y3 - y4)) - ((y1 - y2) * (x3 - x4));
                        // Log.d(TAG, "denominator: "+denominator);
                        corners.add(pt);
                    }
                }
            }
        }

        if (corners.size() != 4) {
            Log.d(TAG, "没找到完美角点" + corners.size());
        }
        for (int i = 0; i < corners.size(); i++) {
            Imgproc.circle(draw, corners.get(i), 5, new Scalar(255, 255, 0, 255), 10);
        }
        mImgUtil.saveMat(draw, "corner");
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