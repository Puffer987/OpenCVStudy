package com.adolf.opencvstudy;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import com.adolf.opencvstudy.rv.ImgRVAdapter;
import com.adolf.opencvstudy.rv.ItemRVBean;
import com.adolf.opencvstudy.utils.ImgUtil;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class IdentifyFeaturesActivity extends AppCompatActivity {
    private static final String TAG = "[jq]IdentifyFeaturesActivity";
    @BindView(R.id.rv_imgs)
    RecyclerView mRvImgs;
    @BindView(R.id.btn_do)
    Button mBtnDo;

    private List<ItemRVBean> mRVBeanList = new ArrayList<>();
    private File mImgCachePath;
    private ImgUtil mImgUtil;
    private Bitmap mOrgBtm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identify_features);
        ButterKnife.bind(this);

        mImgCachePath = new File(getExternalFilesDir(null), "/process");
        mImgUtil = new ImgUtil(mImgCachePath, mRVBeanList);

        String path = getIntent().getStringExtra("img");
        mOrgBtm = BitmapFactory.decodeFile(path);
    }

    @OnClick(R.id.btn_do)
    public void onViewClicked() {
        mBtnDo.setEnabled(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                devGaussian(mOrgBtm);
                // canny(mOrgBtm);
                sobel(mOrgBtm);
                harris(mOrgBtm);
                // houghLines(mOrgBtm);
                runOnUiThread(() -> showImg());
            }
        }).start();

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

    private void harris(Bitmap source) {

        Mat src = new Mat();
        Utils.bitmapToMat(source, src);
        Mat out = new Mat();
        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2BGRA);
        mImgUtil.saveMat(src, "原图");
        Imgproc.cvtColor(src, out, Imgproc.COLOR_BGR2GRAY);

        Mat corners = new Mat();

//找出角点
        Imgproc.cornerHarris(out, out, 2, 3, 0.04);
//归一化Harris角点的输出
        Mat dstNorm = new Mat();
        Core.normalize(out, dstNorm, 0, 255, Core.NORM_MINMAX);
        Core.convertScaleAbs(dstNorm, corners);

        Random random = new Random();
        for (int i = 0; i < dstNorm.cols(); i++) {
            for (int j = 0; j < dstNorm.rows(); j++) {
                double[] value = dstNorm.get(j, i);
                if (value[0] > 200) {
                    Imgproc.circle(corners, new Point(i, j), 5, new Scalar(random.nextInt(255)), 2);
                }
            }
        }

        mImgUtil.saveMat(corners, "corners");

    }

    private void sobel(Bitmap source) {
        Mat src = new Mat();
        Utils.bitmapToMat(source, src);

        Mat out = new Mat();
        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2BGRA);
        mImgUtil.saveMat(src, "原图");
        Imgproc.cvtColor(src, out, Imgproc.COLOR_BGR2GRAY);

        //分别用于保存梯度和绝对梯度的Mat
        Mat grad_x = new Mat();
        Mat abs_grad_x = new Mat();
        Mat grad_y = new Mat();
        Mat abs_grad_y = new Mat();

        Imgproc.Sobel(out, grad_x, CvType.CV_16S, 1, 0, 3, 1, 0);
        Imgproc.Sobel(out, grad_y, CvType.CV_16S, 0, 1, 3, 1, 0);

        Core.convertScaleAbs(grad_x, abs_grad_x);
        Core.convertScaleAbs(grad_y, abs_grad_y);

        Core.addWeighted(abs_grad_x, 0.5, abs_grad_y, 0.5, 1, out);
        mImgUtil.saveMat(out, "sobel");
    }
    private void findLines(Mat src) {
        Mat dst = new Mat();
        Imgproc.cvtColor(src, dst, Imgproc.COLOR_BGR2GRAY);

        //--------------------------

        Mat samples = new Mat(src.rows() * src.cols(), 3, CvType.CV_32F);
        for (int y = 0; y < src.rows(); y++) {
            for (int x = 0; x < src.cols(); x++)
                for (int z = 0; z < 3; z++) {
                    samples.put(x + y * src.cols(), z, src.get(y, x)[z]);
                }
        }
        int clusterCount = 2;
        Mat labels = new Mat();
        int attempts = 5;

        Mat centers = new Mat();
        Core.kmeans(samples, clusterCount, labels, new
                        TermCriteria(TermCriteria.MAX_ITER | TermCriteria.EPS, 10000, 0.0001),
                attempts, Core.KMEANS_PP_CENTERS, centers);

        double dstCenter0 = calcWhiteDist(centers.get(0, 0)[0],
                centers.get(0, 1)[0], centers.get(0, 2)[0]);
        double dstCenter1 = calcWhiteDist(centers.get(1, 0)[0],
                centers.get(1, 1)[0], centers.get(1, 2)[0]);
        int paperCluster = (dstCenter0 < dstCenter1) ? 0 : 1;
        Mat srcRes = new Mat(src.size(), src.type());
        Mat srcGray = new Mat();
        for (int y = 0; y < src.rows(); y++) {
            for (int x = 0; x < src.cols(); x++) {
                int cluster_idx = (int) labels.get(x + y * src.cols(), 0)[0];
                if (cluster_idx != paperCluster) {
                    srcRes.put(y, x, 0, 0, 0, 255);
                } else {
                    srcRes.put(y, x, 255, 255, 255, 255);
                }
            }
        }
        mImgUtil.saveMat(srcRes, "srcRes");
        //------------------------------------------------------------------------------------------


        Imgproc.Canny(dst, dst, 50, 150);
        mImgUtil.saveMat(dst, "canny");

        //threshold:要”检测” 一条直线所需最少的的曲线交点 。
        Imgproc.HoughLinesP(dst, dst, 1, Math.PI / 180, 100, 20, 100);

        Mat lines = new Mat();
        lines.create(src.rows(), src.cols(), CvType.CV_8UC1);

        Log.d(TAG, "lines: " + dst.rows());

        for (int i = 0; i < dst.rows(); i++) {
            double[] points = dst.get(i, 0);
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
    static double calcWhiteDist(double r, double g, double b) {
        return Math.sqrt(Math.pow(255 - r, 2) +
                Math.pow(255 - g, 2) + Math.pow(255 - b, 2));
    }

    private void canny(Bitmap source) {
        Mat src = new Mat();
        Utils.bitmapToMat(source, src);
        Mat out = new Mat();
        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2BGRA);
        mImgUtil.saveMat(src, "原图");
        Imgproc.cvtColor(src, out, Imgproc.COLOR_BGR2GRAY);

        Imgproc.Canny(out, out, 10, 100);
        mImgUtil.saveMat(out, "canny：10-100");

        Imgproc.Canny(out, out, 100, 10);
        mImgUtil.saveMat(out, "canny:100-10");

        Imgproc.Canny(out, out, 50, 50);
        mImgUtil.saveMat(out, "canny:50-50");
    }

    private void devGaussian(Bitmap source) {
        Mat src = new Mat();
        Utils.bitmapToMat(source, src);
        Mat out = new Mat();
        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2BGRA);
        mImgUtil.saveMat(src, "原图");

        Imgproc.cvtColor(src, out, Imgproc.COLOR_BGR2GRAY);

        Mat blur1 = new Mat();
        Mat blur2 = new Mat();
        Imgproc.GaussianBlur(src, blur1, new Size(15, 15), 5);
        Imgproc.GaussianBlur(src, blur2, new Size(21, 21), 5);
        mImgUtil.saveMat(blur1, "模糊核15");
        mImgUtil.saveMat(blur2, "模糊核21");


        Mat doG = new Mat();
        Core.absdiff(blur1, blur2, doG);
        mImgUtil.saveMat(doG, "模糊相减");

        Core.multiply(doG, new Scalar(100), doG);
        mImgUtil.saveMat(doG, "multiply");
        Imgproc.threshold(doG, doG, 50, 255, Imgproc.THRESH_BINARY_INV);
        mImgUtil.saveMat(doG, "二值化");


        src.release();
        out.release();

    }

    private void showImg() {

        ImgRVAdapter adapter = new ImgRVAdapter(mRVBeanList, this);
        GridLayoutManager manager = new GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false);
        mRvImgs.setLayoutManager(manager);
        mRvImgs.setAdapter(adapter);
        mBtnDo.setEnabled(true);
    }

}