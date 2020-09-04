package com.adolf.opencvstudy;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {


    private static final String TAG = "[jq]MainActivity";
    @BindView(R.id.iv_org)
    ImageView mIvOrg;
    private String[] needPermissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    private List<String> askPermissions = new ArrayList<>();
    private File mImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initPermission();

        mImg = new File(getExternalFilesDir(null), "/temp.jpg");
        try {
            FileOutputStream out = new FileOutputStream(mImg);
            BitmapFactory.decodeResource(getResources(), R.drawable.test).compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mIvOrg.setImageBitmap(BitmapFactory.decodeFile(mImg.getAbsolutePath()));

        // startActivity(new Intent(this,JavaCameraViewActivity.class));
    }

    private void initPermission() {
        askPermissions.clear();
        for (int i = 0; i < needPermissions.length; i++) {
            if (ActivityCompat.checkSelfPermission(this, needPermissions[i]) != PackageManager.PERMISSION_GRANTED) {
                askPermissions.add(needPermissions[i]);
            }
        }
        if (askPermissions.size() > 0) {
            ActivityCompat.requestPermissions(this, askPermissions.toArray(new String[askPermissions.size()]), 1001);
        } else {
            Log.d(TAG, "已有权限");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            int result = 0;
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    result++;
                }
            }
            if (result == grantResults.length) {
                Log.d(TAG, "权限请求成功");
            } else {
                Log.d(TAG, "权限被拒绝");
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "OpenCV library 未加载成功");
        } else {
            Log.e(TAG, "OpenCV library 加载成功");
        }
    }


    public native String stringFromJNI();

    @OnClick({R.id.btn_binary, R.id.btn_scan, R.id.btn_mor, R.id.btn_blur, R.id.btn_identify})
    public void onViewClicked(View view) {
        Intent intent = new Intent();
        intent.putExtra("img", mImg.getAbsolutePath());
        switch (view.getId()) {
            case R.id.btn_binary:
                intent.setClass(this, BinaryActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_mor:
                intent.setClass(this, MorphologyActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_blur:
                intent.setClass(this, BlurSharpenActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_identify:
                intent.setClass(this, IdentifyFeaturesActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_scan:
                intent.setClass(this, ScannerActivity.class);
                startActivity(intent);
                break;
        }

    }
}