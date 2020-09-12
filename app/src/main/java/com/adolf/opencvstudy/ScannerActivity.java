package com.adolf.opencvstudy;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.adolf.opencvstudy.rv.ItemRVBean;
import com.adolf.opencvstudy.utils.CropImgView;
import com.adolf.opencvstudy.utils.FreedomCropView;
import com.adolf.opencvstudy.utils.ImgUtil;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
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
    private static final String CROP_IMG_NAME = "/crop.jpg";
    @BindView(R.id.rv_imgs)
    RecyclerView mRvImgs;
    @BindView(R.id.fcv)
    FreedomCropView mFcv;
    @BindView(R.id.iv_display)
    ImageView mIvDisplay;
    @BindView(R.id.group_crop)
    RelativeLayout mGroupCrop;
    @BindView(R.id.group_display)
    RelativeLayout mGroupDisplay;
    @BindView(R.id.civ)
    CropImgView mCiv;

    private List<ItemRVBean> mRVBeanList = new ArrayList<>();
    private ImgUtil mImgUtil;
    private Mat mSamllMat;
    private ProgressDialog progressDialog;
    private Bitmap mBigBmp;
    private float mScale;
    private boolean isFreeCrop;
    private File mImgCachePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        ButterKnife.bind(this);

        initImage();

        initProgressDialog();

        progressDialog.show();
        new Thread(() -> {
            scanning(mSamllMat);
            runOnUiThread(() -> showImg());
        }).start();
    }

    private void initImage() {
        mImgCachePath = new File(getExternalFilesDir(null), "/process");
        mImgUtil = new ImgUtil(mImgCachePath, mRVBeanList);
        String path = getIntent().getStringExtra("img");
        mBigBmp = BitmapFactory.decodeFile(path);
        // 将bitmap缩小
        mScale = 963f / mBigBmp.getWidth();
        Bitmap smallBtm = mImgUtil.zoomImage(mBigBmp, mScale);
        mFcv.setImageBitmap(smallBtm);
        mCiv.setImageBitmap(smallBtm);
        mIvDisplay.setImageBitmap(smallBtm);
        // 将缩小后的bitmap转为Mat，用于后续OpenCV操作，减少时间
        mSamllMat = new Mat();
        Utils.bitmapToMat(smallBtm, mSamllMat);
        Imgproc.cvtColor(mSamllMat, mSamllMat, Imgproc.COLOR_RGB2BGRA);
    }

    private void initProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(false);//循环滚动
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage("loading...");
        progressDialog.setCancelable(false);//false不能取消显示，true可以取消显示
    }

    private void showImg() {
        // mGroupDisplay.setVisibility(View.VISIBLE);
        // mGroupCrop.setVisibility(View.GONE);
        // ImgRVAdapter adapter = new ImgRVAdapter(mRVBeanList, this);
        // GridLayoutManager manager = new GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false);
        // mRvImgs.setLayoutManager(manager);
        // mRvImgs.setAdapter(adapter);
        if (isFreeCrop) {
            freedomCrop();
        } else {
            rectCrop();
        }
        progressDialog.dismiss();
    }

    private void scanning(Mat src) {
        Mat dst = new Mat();
        // mImgUtil.saveMat(src, "原图");

        Imgproc.cvtColor(src, dst, Imgproc.COLOR_BGR2GRAY);
        // mImgUtil.saveMat(dst, "gray");

        Imgproc.GaussianBlur(dst, dst, new Size(7, 7), 5);
        mImgUtil.saveMat(dst, "GaussianBlur");

        Imgproc.adaptiveThreshold(dst, dst, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 81, 5);
        mImgUtil.saveMat(dst, "自适应阈值");


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
        Mat drawing = Mat.zeros(src.size(), CvType.CV_8UC1);
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


        Log.d(TAG, "角点个数: " + approxCure.rows());
        List<Point> corners = new ArrayList<>();
        for (int i = 0; i < approxCure.rows(); i++) {
            Point temp = new Point(approxCure.get(i, 0)[0], approxCure.get(i, 0)[1]);
            // Log.d(TAG, "corners: " + temp.toString());
            Imgproc.circle(drawing, temp, 5, new Scalar(255), 1);
            corners.add(temp);
        }
        mImgUtil.saveMat(drawing, "point");


        Rect borderRect = Imgproc.boundingRect(approxCure);


        if (borderRect.height > mSamllMat.height() * 0.3 && borderRect.width > mSamllMat.width() * 0.3&&corners.size() == 4) {
            // if (corners.size() == 4) {
                List<PointF> points = new ArrayList<>();
                for (int i = 0; i < corners.size(); i++) {
                    points.add(new PointF((int) corners.get(i).x, (int) corners.get(i).y));
                }
                runOnUiThread(() -> {
                    mFcv.setOrgCorners(points);
                    mCiv.setVisibility(View.GONE);
                    mFcv.setVisibility(View.VISIBLE);
                });

                isFreeCrop = true;
            // } else if (corners.size() > 1) {
            //     Rect rect = Imgproc.boundingRect(approxCure);
            //
            //     Log.d(TAG, "rect: " + rect.toString());
            //     Imgproc.rectangle(drawing, rect, new Scalar(255), 1);
            //     mImgUtil.saveMat(drawing, "rect");
            //
            //     // android.graphics.RectF构造方法是从左，上，右，下各自的坐标
            //     // org.opencv.core.Rect构造方法是左上坐标，宽，高
            //     RectF initCropRect = new RectF(rect.x, rect.y, rect.width + rect.x, rect.height + rect.y);
            //     Log.d(TAG, "包含角点的rect: " + rect);
            //     Log.d(TAG, "传给crop的Rect: " + initCropRect);
            //
            //     isFreeCrop = false;
            //
            //     runOnUiThread(() -> {
            //         mCiv.setInitCropRect(initCropRect);
            //         mFcv.setVisibility(View.GONE);
            //         mCiv.setVisibility(View.VISIBLE);
            //     });
            // }
        } else {
            Rect rect = autoCrop(mSamllMat);
            RectF initCropRect;
            isFreeCrop = false;
            if (rect.width < mSamllMat.width() * 0.15 || rect.height < mSamllMat.height() * 0.15) {
                 initCropRect = new RectF(50, 50, mSamllMat.width() - 50, mSamllMat.height() - 50);
                isFreeCrop = false;
            }else {
                initCropRect = new RectF(rect.x, rect.y, rect.width + rect.x, rect.height + rect.y);
                Log.d(TAG, "包含角点的rect: " + rect);
                Log.d(TAG, "传给crop的Rect: " + initCropRect);
            }
            runOnUiThread(() -> {
                mCiv.setInitCropRect(initCropRect);
                mFcv.setVisibility(View.GONE);
                mCiv.setVisibility(View.VISIBLE);
            });
        }
    }

    private Rect autoCrop(Mat src) {
        Mat dst = new Mat();
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);


        Mat binary1 = new Mat();
        Mat binary2 = new Mat();
        Imgproc.adaptiveThreshold(gray, binary1, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 9, 20);
        mImgUtil.saveMat(binary1, "自动阈值");

        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(3, 3));
        for (int i = 0; i < 3; i++) {
            Imgproc.morphologyEx(binary1, binary1, Imgproc.MORPH_OPEN, element);
            mImgUtil.saveMat(binary1, "binary1开" + i);
        }

        Imgproc.threshold(gray, binary2, 127, 255, Imgproc.THRESH_BINARY_INV);
        mImgUtil.saveMat(binary2, "127阈值");

        Core.bitwise_and(binary1, binary2, dst);
        mImgUtil.saveMat(dst, "与运算");


        Imgproc.Canny(dst, dst, 100, 200);
        mImgUtil.saveMat(dst, "Canny");

        Rect rect = Imgproc.boundingRect(dst);
        Mat drawing = Mat.zeros(src.size(), CvType.CV_8UC1);
        Imgproc.rectangle(drawing, rect, new Scalar(255), 2);
        mImgUtil.saveMat(drawing, "rect");

        return rect;
    }

    public Bitmap perspective(Mat src, List<Point> corners) {
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

    @OnClick({R.id.btn_cancel, R.id.btn_crop, R.id.btn_recrop, R.id.btn_strength})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_cancel:
                mGroupCrop.setVisibility(View.GONE);
                mGroupDisplay.setVisibility(View.VISIBLE);
                break;
            case R.id.btn_crop:
                if (isFreeCrop) {
                    freedomCrop();
                } else {
                    rectCrop();
                }
                break;
            case R.id.btn_recrop:
                mGroupCrop.setVisibility(View.VISIBLE);
                mGroupDisplay.setVisibility(View.GONE);
                break;
            case R.id.btn_strength:
                Mat src = Imgcodecs.imread(new File(mImgCachePath, CROP_IMG_NAME).getAbsolutePath(), Imgcodecs.IMREAD_COLOR);
                Mat dst = new Mat();


                Mat kernel = new Mat(3, 3, CvType.CV_16SC1);
                kernel.put(0, 0, 0, -1, 0, -1, 5, -1, 0, -1, 0);
                Imgproc.filter2D(src, dst, -1, kernel);


                // // Imgproc.cvtColor(src, dst, Imgproc.COLOR_BGR2GRAY);
                // Imgproc.adaptiveThreshold(src, dst, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 21, 5);
                // // mImgUtil.saveMat(dst, "自适应阈值");
                //
                // Mat ele = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
                // Imgproc.morphologyEx(dst, dst, Imgproc.MORPH_CLOSE, ele);
                // mImgUtil.saveMat(dst, "b");
                Imgcodecs.imwrite(new File(mImgCachePath, CROP_IMG_NAME).getAbsolutePath(), dst);

                mIvDisplay.setImageBitmap(BitmapFactory.decodeFile(new File(mImgCachePath, CROP_IMG_NAME).getAbsolutePath()));
                break;
        }
    }

    private void freedomCrop() {
        Mat bigMat = new Mat();
        Utils.bitmapToMat(mBigBmp, bigMat);
        Imgproc.cvtColor(bigMat, bigMat, Imgproc.COLOR_RGB2BGRA);

        List<Point> cropCorners = mFcv.getCropCorners();

        if (cropCorners.size() == 0) {
            Toast.makeText(this, "截图区域不允许，请重新设置角度", Toast.LENGTH_SHORT).show();
        } else {
            // Mat dd = mSamllMat.clone();
            // for (Point p : cropCorners) {
            //     Imgproc.circle(dd, p, 5, new Scalar(0, 255, 0), 30);
            // }
            // mImgUtil.saveMat(dd, "cropCorners");
            List<Point> corners = new ArrayList<>();
            for (Point p : cropCorners) {
                corners.add(new Point(p.x / mScale, p.y / mScale));
            }
            Bitmap perspective = perspective(bigMat, corners);

            File file = new File(mImgCachePath, CROP_IMG_NAME);
            mImgUtil.saveBitmap(perspective, file);

            mIvDisplay.setImageBitmap(perspective);
            mGroupCrop.setVisibility(View.GONE);
            mGroupDisplay.setVisibility(View.VISIBLE);
        }
    }

    private void rectCrop() {
        int[] cropAttrs = mCiv.getCropAttrs();
        Bitmap cropBmp = Bitmap.createBitmap(mBigBmp, (int) (cropAttrs[0] / mScale), (int) (cropAttrs[1] / mScale), (int) (cropAttrs[2] / mScale), (int) (cropAttrs[3] / mScale));

        File file = new File(mImgCachePath, CROP_IMG_NAME);
        mImgUtil.saveBitmap(cropBmp, file);

        mIvDisplay.setImageBitmap(cropBmp);
        mGroupCrop.setVisibility(View.GONE);
        mGroupDisplay.setVisibility(View.VISIBLE);
    }

}