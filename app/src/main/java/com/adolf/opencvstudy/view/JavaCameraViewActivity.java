package com.adolf.opencvstudy.view;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.adolf.opencvstudy.R;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCamera2View;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


public class JavaCameraViewActivity extends AppCompatActivity {

    private static final String TAG = "[jq]JavaCameraView";
    @BindView(R.id.jcv_test)
    JavaCamera2View mJcvTest;
    private String[] needPermissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    private List<String> askPermissions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_java_camera_view);
        ButterKnife.bind(this);


        initPermission();


        mJcvTest.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {

            }

            @Override
            public void onCameraViewStopped() {

            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                {
                    Log.d(TAG, "竖屏显示");
                }

                Mat frame = inputFrame.rgba();

                return frame;
            }
        });

        // mJcvTest.enableView();
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
}