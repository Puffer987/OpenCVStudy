package com.adolf.opencvstudy;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.adolf.opencvstudy.utils.CropUtils;
import com.adolf.opencvstudy.view.FreedomCropView;
import com.adolf.opencvstudy.view.RectCropView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * @program: OpenCVStudy
 * @description: 裁剪的activity
 * @author: Adolf
 * @create: 2020-09-12 16:49
 **/
public class AutoCropperActivity extends AppCompatActivity {
    private static final String TAG = "[crop]ScannerActivity";
    private static final String CROP_IMG_NAME = "/crop.jpg";
    @BindView(R.id.freedom_cropper)
    FreedomCropView mFreedomCrop;
    @BindView(R.id.rect_cropper)
    RectCropView mRectCrop;
    @BindView(R.id.group_crop)
    FrameLayout mGroupCrop;
    @BindView(R.id.group_display)
    FrameLayout mGroupDisplay;
    @BindView(R.id.image_cropper_result)
    ImageView mIvResult;

    private ProgressDialog progressDialog;
    private Bitmap mBigBmp;
    private float mScale;
    private boolean isFreeCrop;
    private File mImgCachePath;
    private File mCropPath;
    private Bitmap mSmallBtm;
    private List<PointF> mFreeCropPoints;
    private RectF mRectCropPoints;
    private float mRotationValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_auto_cropper);
        ButterKnife.bind(this);

        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "OpenCV library 未加载成功");
        } else {
            Log.e(TAG, "OpenCV library 加载成功");
        }

        initImage();

        initProgressDialog();

        progressDialog.show();
        new Thread(() -> {
            scanning(mSmallBtm);
            runOnUiThread(() -> showImg());
        }).start();
    }

    private void initImage() {
        mGroupCrop.setVisibility(View.INVISIBLE);
        mGroupDisplay.setVisibility(View.INVISIBLE);
        mImgCachePath = getExternalFilesDir(null);
        CropUtils cropUtils = new CropUtils();

        // 输出处理图片
        cropUtils.setImgCachePath(new File(mImgCachePath, "/opencv"));

        mCropPath = new File(mImgCachePath, CROP_IMG_NAME);

        String path = getIntent().getStringExtra("img");
        mBigBmp = BitmapFactory.decodeFile(path);
        // 将bitmap缩小
        mScale = 800.0f / mBigBmp.getWidth();
        mSmallBtm = CropUtils.zoomImage(mBigBmp, mScale);
        mFreedomCrop.setImageBitmap(mSmallBtm);
        mRectCrop.setImageBitmap(mSmallBtm);
        mIvResult.setImageBitmap(mSmallBtm);
    }

    private void initProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(false);//循环滚动
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage("loading...");
        progressDialog.setCancelable(false);//false不能取消显示，true可以取消显示
    }

    private void showImg() {
        if (isFreeCrop) {
            freedomCrop();
        } else {
            rectCrop();
        }
        mGroupCrop.setVisibility(View.GONE);
        mGroupDisplay.setVisibility(View.VISIBLE);
        progressDialog.dismiss();
    }

    private void scanning(Bitmap src) {
        MatOfPoint2f approxCure = CropUtils.getContourPointSet(src);
        Rect borderRect = new Rect();
        List<Point> corners = new ArrayList<>();
        if (approxCure != null) {
            borderRect = CropUtils.findBorderRect(approxCure);
            corners = CropUtils.findCorners(approxCure);
        }

        if (borderRect.height > src.getHeight() * 0.3 && borderRect.width > src.getWidth() * 0.3 && corners.size() == 4) {
            mFreeCropPoints = new ArrayList<>();
            for (int i = 0; i < corners.size(); i++) {
                mFreeCropPoints.add(new PointF((int) corners.get(i).x, (int) corners.get(i).y));
            }
            runOnUiThread(() -> {
                mFreedomCrop.setOrgCorners(mFreeCropPoints);
                mRectCrop.setVisibility(View.GONE);
                mFreedomCrop.setVisibility(View.VISIBLE);
            });

            isFreeCrop = true;
        } else {
            Rect rect = CropUtils.autoCrop(src);
            isFreeCrop = false;
            if (rect.width < src.getWidth() * 0.15 || rect.height < src.getHeight() * 0.15) {
                mRectCropPoints = new RectF(50, 50, src.getWidth() - 50, src.getHeight() - 50);
                isFreeCrop = false;
            } else {
                mRectCropPoints = new RectF(rect.x, rect.y, rect.width + rect.x, rect.height + rect.y);
                Log.d(TAG, "包含角点的rect: " + rect);
                Log.d(TAG, "传给crop的Rect: " + mRectCropPoints);
            }
            runOnUiThread(() -> {
                mRectCrop.setInitCropRect(mRectCropPoints);
                mFreedomCrop.setVisibility(View.GONE);
                mRectCrop.setVisibility(View.VISIBLE);
            });
        }
    }

    private void freedomCrop() {
        Mat bigMat = new Mat();
        Utils.bitmapToMat(mBigBmp, bigMat);
        Imgproc.cvtColor(bigMat, bigMat, Imgproc.COLOR_RGB2BGRA);

        List<Point> cropCorners = mFreedomCrop.getCropCorners();

        if (cropCorners.size() == 0) {
            Toast.makeText(this, "截图区域不允许，请重新设置角度", Toast.LENGTH_SHORT).show();
        } else {
            List<Point> corners = new ArrayList<>();
            for (Point p : cropCorners) {
                corners.add(new Point(p.x / mScale, p.y / mScale));
            }
            Bitmap perspective = CropUtils.perspective(bigMat, corners);

            float scale = 3000000f / (perspective.getWidth() * perspective.getHeight());
            if (scale < 1) {
                perspective = CropUtils.zoomImage(perspective, scale);
            }
            saveBitmap(perspective, mCropPath);
            mIvResult.setImageBitmap(perspective);
        }
    }

    private void rectCrop() {
        int[] cropAttrs = mRectCrop.getCropAttrs();
        Bitmap cropBmp = Bitmap.createBitmap(mBigBmp, (int) (cropAttrs[0] / mScale), (int) (cropAttrs[1] / mScale),
                (int) (cropAttrs[2] / mScale), (int) (cropAttrs[3] / mScale));
        float scale = 3000000f / (cropBmp.getWidth() * cropBmp.getHeight());
        Log.d(TAG, "scale: "+scale);

        if (scale < 1) {
            cropBmp = CropUtils.zoomImage(cropBmp, scale);
        }
        saveBitmap(cropBmp, mCropPath);

        mIvResult.setImageBitmap(cropBmp);
    }

    private void saveBitmap(Bitmap source, File file) {
        try {
            FileOutputStream out = new FileOutputStream(file);
            source.compress(Bitmap.CompressFormat.JPEG, 80, out);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnClick({R.id.image_display_complete,
            R.id.image_display_rotate,
            R.id.image_display_retry,
            R.id.image_display_cut,
            R.id.image_display_close,
            R.id.image_cropper_corp,
            R.id.image_cropper_reset,
            R.id.image_cropper_cancel})
    public void onOperateClicked(View view) {
        switch (view.getId()) {
            case R.id.image_display_complete:
                Bitmap bitmap = BitmapFactory.decodeFile(mCropPath.getAbsolutePath());
                Matrix matrix = new Matrix();
                matrix.setRotate(mRotationValue);
                Bitmap rotateBtm = bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                CropUtils.saveBitmap(rotateBtm, mCropPath);
                runOnUiThread(() -> progressDialog.dismiss());

                Intent intent = new Intent();
                intent.putExtra("cropResult", mCropPath.getAbsolutePath());
                setResult(1234, intent);

                finish();
                break;
            case R.id.image_display_rotate:
                mIvResult.setPivotX(mIvResult.getWidth() / 2);
                mIvResult.setPivotY(mIvResult.getHeight() / 2);
                mRotationValue += 90;
                if (mRotationValue == 360) {
                    mRotationValue = 0;
                }
                mIvResult.setRotation(mRotationValue);
                break;
            case R.id.image_display_retry:
                Intent i = new Intent();
                i.putExtra("reShot", true);
                setResult(1234, i);

                finish();
                break;
            case R.id.image_display_cut:
                mGroupCrop.setVisibility(View.VISIBLE);
                mGroupDisplay.setVisibility(View.GONE);
                break;
            case R.id.image_display_close:
                finish();
                break;
            case R.id.image_cropper_corp:
                if (isFreeCrop) {
                    freedomCrop();
                } else {
                    rectCrop();
                }
                mGroupCrop.setVisibility(View.GONE);
                mGroupDisplay.setVisibility(View.VISIBLE);
                break;
            case R.id.image_cropper_reset:
                if (isFreeCrop) {
                    mFreedomCrop.setOrgCorners(mFreeCropPoints);
                } else {
                    mRectCrop.setInitCropRect(mRectCropPoints);
                }
                break;
            case R.id.image_cropper_cancel:
                mGroupCrop.setVisibility(View.GONE);
                mGroupDisplay.setVisibility(View.VISIBLE);
                break;
        }
    }
}