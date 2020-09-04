package com.adolf.opencvstudy;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
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
    @BindView(R.id.rv_imgs)
    RecyclerView mRvImgs;
    @BindView(R.id.btn_do)
    Button mBtnDo;

    private List<ItemRVBean> mRVBeanList = new ArrayList<>();
    private File mImgCachePath;
    private SaveImgUtil mImgUtil;
    private Bitmap mOrgBtm;
    private Mat mSrc;

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
    }

    @OnClick(R.id.btn_do)
    public void onViewClicked() {
        mBtnDo.setEnabled(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                // scan(mOrgBtm);

                findCanny(mSrc);
                runOnUiThread(() -> showImg());
            }
        }).start();

    }

    private void findCanny(Mat src) {
        Mat gray = new Mat();
        Mat dst = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        // Imgproc.GaussianBlur(gray, dst, new Size(5,5),0,0);
        // mImgUtil.saveMat(dst, "高斯");

        Imgproc.adaptiveThreshold(gray, dst, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 17, 5);
        mImgUtil.saveMat(dst, "高斯，二值");

        // Mat e = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
        // Imgproc.morphologyEx(dst, dst, Imgproc.MORPH_CLOSE, e);


        Imgproc.Canny(dst, dst, 50, 100, 3);
        mImgUtil.saveMat(dst, "canny");

        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Imgproc.dilate(dst, dst, element);
        mImgUtil.saveMat(dst, "腐蚀");

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(dst, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(5, 5));

        int index = 0;
        double maxim = Imgproc.contourArea(contours.get(0));
        for (int i = 0; i < contours.size(); i++) {
            double t;
            t = Imgproc.contourArea(contours.get(i));
            if (maxim < t) {
                maxim = t;
                index = i;
            }
        }

        Mat drawing = new Mat(src.rows(),src.cols(),CvType.CV_8UC1);
        Imgproc.drawContours(drawing, contours, index, new Scalar(255, 0, 0), 1);
        mImgUtil.saveMat(drawing, "轮廓");


        Mat lines = new Mat();
        // lines.create(src.rows(), src.cols(), CvType.CV_8UC1);
        Imgproc.HoughLinesP(drawing, lines, 1, Math.PI / 180, 50, 20, 20);

        for (int i = 0; i < lines.cols(); i++) {
            double[] points = lines.get(0, i);
            double x1, y1, x2, y2;
            x1 = points[0];
            y1 = points[1];
            x2 = points[2];
            y2 = points[3];

            Point p1 = new Point(x1, y1);
            Point p2 = new Point(x2, y2);

            Imgproc.line(drawing, p1, p2, new Scalar(0, 255, 0), 1);

        }
        mImgUtil.saveMat(drawing, "line");

        for (int y = 0; y < lines.rows(); y++) {
            double[] vec = lines.get(y, 0);

            double x1 = vec[0],
                    y1 = vec[1],
                    x2 = vec[2],
                    y2 = vec[3];

            Point start = new Point(x1, y1);
            Point end = new Point(x2, y2);
            Imgproc.line(drawing, start, end, new Scalar(255, 255, 0), 1);
        }

        mImgUtil.saveMat(drawing, "line");

    }

    private void houghLines(Bitmap source) {
        Mat src = new Mat();
        Utils.bitmapToMat(source, src);
        Mat out = new Mat();
        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2BGRA);
        mImgUtil.saveMat(src, "原图");

        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);

        Mat cannyEdge = new Mat();
        Imgproc.Canny(gray, cannyEdge, 10, 100);

        Imgproc.HoughLinesP(cannyEdge, out, 1, Math.PI / 180, 50, 20, 20);

        Mat lines = new Mat();
        lines.create(cannyEdge.rows(), cannyEdge.cols(), CvType.CV_8UC1);

        for (int i = 0; i < out.cols(); i++) {
            double[] points = out.get(0, i);
            double x1, y1, x2, y2;
            x1 = points[0];
            y1 = points[1];
            x2 = points[2];
            y2 = points[3];

            Point p1 = new Point(x1, y1);
            Point p2 = new Point(x2, y2);

            Imgproc.line(lines, p1, p2, new Scalar(255, 0, 0), 1);

        }
        mImgUtil.saveMat(lines, "line");
    }

    private void scan(Bitmap bitmap) {
        Mat src = new Mat();
        Utils.bitmapToMat(bitmap, src);
        Mat srcGray = new Mat();
        Imgproc.cvtColor(src, srcGray, Imgproc.COLOR_BGR2GRAY);

        /*
        Canny
        低于阈值1的像素点会被认为不是边缘；
        高于阈值2的像素点会被认为是边缘；
         */
        Mat temp = new Mat();
        Imgproc.Canny(srcGray, temp, 40, 50);
        mImgUtil.saveMat(temp, "Canny");

        Imgproc.Canny(srcGray, temp, 90, 100);
        mImgUtil.saveMat(temp, "Canny");

        Imgproc.Canny(srcGray, temp, 120, 100);
        mImgUtil.saveMat(temp, "Canny");

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(temp, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        // int index = 0;
        // double maxim = Imgproc.contourArea(contours.get(0));
        //
        //
        // for (int i = 0; i < contours.size(); i++) {
        //     double t;
        //     t = Imgproc.contourArea(contours.get(i));
        //     if (maxim < t) {
        //         maxim = t;
        //         index = i;
        //     }
        // }

        Mat drawing = Mat.zeros(src.size(), CvType.CV_8UC1);
        // Imgproc.drawContours(drawing, contours, -1, new Scalar(255,0,0), 1);
        // mImgUtil.saveMat(drawing, "轮廓");

    }

    private void showImg() {
        ImgRVAdapter adapter = new ImgRVAdapter(mRVBeanList, this);
        GridLayoutManager manager = new GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false);
        mRvImgs.setLayoutManager(manager);
        mRvImgs.setAdapter(adapter);
        mBtnDo.setEnabled(true);
    }
}