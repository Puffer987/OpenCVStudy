package com.adolf.opencvstudy;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {


    private static final String TAG = "[jq]MainActivity";
    private String[] needPermissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    private List<String> askPermissions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initPermission();

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

    @OnClick({R.id.btn_binary, R.id.btn_edge,R.id.btn_mor})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_binary:
                startActivity(new Intent(this, BinaryActivity.class));
                break;
            case R.id.btn_mor:
                startActivity(new Intent(this, MorphologyActivity.class));
                break;
        }

    }
}