package com.adolf.opencvstudy;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.adolf.opencvstudy.view.BinaryActivity;
import com.adolf.opencvstudy.view.BlurSharpenActivity;
import com.adolf.opencvstudy.view.IdentifyFeaturesActivity;
import com.adolf.opencvstudy.view.MorphologyActivity;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {


    private static final String TAG = "[jq]MainActivity";
    private static final int REQUEST_CODE_CAMERA = 0x1001;
    @BindView(R.id.iv_org)
    ImageView mIvOrg;
    private String[] needPermissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private List<String> askPermissions = new ArrayList<>();
    private File mImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initPermission();

        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "OpenCV library 未加载成功");
        } else {
            Log.e(TAG, "OpenCV library 加载成功");
        }

        // 防止报错：exposed beyond app through ClipData.Item.getUri()
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        mImg = new File(getExternalFilesDir(null), "/temp1.jpg");
        if(mImg.exists())
        mIvOrg.setImageBitmap(BitmapFactory.decodeFile(mImg.getAbsolutePath()));
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_CAMERA:
                if (mImg.exists()) {
                    mIvOrg.setImageBitmap(BitmapFactory.decodeFile(mImg.getAbsolutePath()));
                    Intent intent = new Intent();
                    intent.putExtra("img", mImg.getAbsolutePath());
                    intent.setClass(this, ScannerActivity.class);
                    startActivity(intent);
                } else
                    Toast.makeText(this, "图片未拍摄", Toast.LENGTH_SHORT).show();
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    public native String stringFromJNI();

    @OnClick({R.id.btn_binary, R.id.btn_scan, R.id.btn_mor, R.id.btn_blur, R.id.btn_identify, R.id.btn_shot})
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
            case R.id.btn_shot:
                Log.d(TAG, "FilePath:" + mImg.getAbsolutePath());
                Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); //指定拍照
                Uri uri = Uri.fromFile(mImg);
                i.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                startActivityForResult(i, REQUEST_CODE_CAMERA);
                break;
        }

    }
}