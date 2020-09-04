package com.adolf.opencvstudy;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.adolf.opencvstudy.rv.ImgRVAdapter;
import com.adolf.opencvstudy.rv.ItemRVBean;
import com.adolf.opencvstudy.utils.SaveImgUtil;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class BinaryActivity extends AppCompatActivity {
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private static final String TAG = "[jq]BinaryActivity";
    @BindView(R.id.rv_imgs)
    RecyclerView mRvImgs;

    private List<ItemRVBean> mRVBeanList = new ArrayList<>();
    private File mImgCachePath;
    private SaveImgUtil mImgUtil;
    private Bitmap mOrgBtm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_binary);
        ButterKnife.bind(this);
        mImgCachePath = new File(getExternalFilesDir(null), "/process");

        mImgUtil = new SaveImgUtil(mImgCachePath,mRVBeanList);

        String path = getIntent().getStringExtra("img");
        mOrgBtm = BitmapFactory.decodeFile(path);
    }

    public void getContoursPic(Bitmap source) {
        Mat src = new Mat();
        Mat out = new Mat();
        Utils.bitmapToMat(source, src);
        Imgproc.cvtColor(src,src,Imgproc.COLOR_RGBA2BGRA);
        mImgUtil.saveMat(src,"原图");
        Imgproc.cvtColor(src, out, Imgproc.COLOR_BGR2GRAY);
        mImgUtil.saveMat(out,"灰度");
        Imgproc.threshold(out, out, 127, 255, Imgproc.THRESH_BINARY);

        mImgUtil.saveMat(out,"阈值127");

        ArrayList<MatOfPoint> contours = new ArrayList<>();
        // Imgproc.findContours(gray, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
        // Imgproc.findContours(gray,contours,new Mat(),Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_NONE);

        Log.d(TAG, "getContoursPic: " + contours.size());

        Imgproc.findContours(out, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        double maxVal = 0;
        int maxValIdx = 0;
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
            double contourArea = Imgproc.contourArea(contours.get(contourIdx));
            if (maxVal < contourArea) {
                maxVal = contourArea;
                maxValIdx = contourIdx;
            }
        }
        Mat mRgba = new Mat();
        mRgba.create(src.rows(), src.cols(), CvType.CV_8UC3);
        //绘制检测到的轮廓
        Imgproc.drawContours(mRgba, contours, -1, new Scalar(0, 255, 0), 5);
        mImgUtil.saveMat(mRgba,"轮廓");

        src.release();
        out.release();
        mRgba.release();
    }


    public void adaptiveThreshold(Bitmap btm) {

        /*
         * 原文地址: https://docs.opencv.org/3.0-beta/modules/imgproc/doc/miscellaneous_transformations.html?highlight=adaptivethreshold
         *
         * 原型方法
         *      adaptiveThreshold(Mat src, Mat dst, double maxValue, int adaptiveMethod, int thresholdType, int blockSize, double C)
         *
         *      参数说明:
         *          src : Mat 输入图像 单通道 8位图像
         *          dst : Mat 输出图像 阈值操作结果填充在此图像
         *          maxValue : double 分配给满足条件的像素的非零值
         *          adaptiveMethod : int 自定义使用的阈值算法，ADAPTIVE_THRESH_MEAN_C 、 ADAPTIVE_THRESH_GAUSSIAN_C
         *              -- ADAPTIVE_THRESH_MEAN_C 时，T(x,y) = blockSize * blockSize【b】
         *                  blockSize【b】= 邻域内(x,y) - C
         *              -- ADAPTIVE_THRESH_GAUSSIAN_C 时，T(x,y) = blockSize * blockSize【b】
         *                  blockSize【b】= 邻域内(x,y) - C与高斯窗交叉相关的加权总和
         *
         *          thresholdType : int 阈值类型，只能是THRESH_BINARY 、 THRESH_BINARY_INV
         *              -- THRESH_BINARY 时， src(x,y) > T(x,y) ? maxValue : 0 。当前像素点的灰度值 > T(x,y) 则为 maxValue，反之为0
         *              -- THRESH_BINARY_INV 时， src(x,y) > T(x,y) ? 0 : maxValue 。当前像素点的灰度值 > T(x,y) 则为 0，反之为maxValue
         *              >>>>> T(x,y) 根据 adaptiveMethod 算法计算出的像素阈值。<<<<<
         *
         *          blockSize : int 用来计算阈值的邻域尺寸 3，5，7等等，奇数
         *          C : double 减去平均值或加权平均值的常数，通常情况下，它是正的，但也可能是零或负。
         *
         */

        // blocksize 和 C 最关键， 需要不断的调整来找到最佳的值
        // blocksize 就是区域，以一个像素点辐射周围的范围来找阈值，在通用处理中，设置一个较大的居中值即可，需要提取文字等信息，反正控制在10以内最好。
        // 在设置 C 的时候，默认我会把 maxValue 设置为255（白色），
        // 当 C 为正数，会过滤掉灰色区域，最终是白底，黑字。
        // 为 C 为负数，会过滤掉白色区域，文字区域在 blockSize 范围内的白色保留， 这样就变成了 黑底白字。也就是取反。

        Mat src = new Mat(btm.getHeight(),btm.getWidth(),CvType.CV_8UC3);
        Utils.bitmapToMat(btm, src);

        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY);
        Mat out = new Mat();

        Imgproc.adaptiveThreshold(src, out, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 13, 5);
        mImgUtil.saveMat(out,"均值，二值");

        Imgproc.adaptiveThreshold(src, out, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 13, 5);
        mImgUtil.saveMat(out,"均值，二值反");

        Imgproc.adaptiveThreshold(src, out, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 13, 5);
        mImgUtil.saveMat(out,"高斯，二值");

        Imgproc.adaptiveThreshold(src, out, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 13, 5);
        mImgUtil.saveMat(out,"高斯，二值反");

        src.release();
        out.release();
    }


    @OnClick(R.id.btn_do)
    public void onViewClicked() {
        mRVBeanList.clear();
        getContoursPic(mOrgBtm);
        adaptiveThreshold(mOrgBtm);

        ImgRVAdapter adapter = new ImgRVAdapter(mRVBeanList, this);
        GridLayoutManager manager = new GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false);
        mRvImgs.setLayoutManager(manager);
        mRvImgs.setAdapter(adapter);
    }


    public native String stringFromJNI();
}