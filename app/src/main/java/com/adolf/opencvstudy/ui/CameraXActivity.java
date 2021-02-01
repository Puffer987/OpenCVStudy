package com.adolf.opencvstudy.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.view.CameraView;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.adolf.opencvstudy.R;

import java.io.File;

import static androidx.camera.core.CameraX.getContext;

public class CameraXActivity extends AppCompatActivity {
    private CameraView mViewFinder;
    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_x);
        mViewFinder = findViewById(R.id.view_finder);
        mViewFinder.bindToLifecycle(this);
        //按钮点击
        mViewFinder.setCaptureMode(CameraView.CaptureMode.IMAGE);
        File file = new File(getContext().getExternalFilesDir(null) + File.separator + "/wt.jpg");
        Log.e("TAG", file.toString());
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();
        mViewFinder.takePicture(outputFileOptions, ContextCompat.getMainExecutor(getContext().getApplicationContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Uri savedUri = outputFileResults.getSavedUri();
                        if(savedUri == null){
                            savedUri = Uri.fromFile(file);
                        }
                        onFileSaved(savedUri);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {

                    }
                });
    }
    //将前面保存的文件添加到媒体中
    private void onFileSaved(Uri savedUri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            sendBroadcast(new Intent(Camera.ACTION_NEW_PICTURE, savedUri));
        }
        String mimeTypeFromExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap
                .getFileExtensionFromUrl(savedUri.getPath()));
        MediaScannerConnection.scanFile(getApplicationContext(),
                new String[]{new File(savedUri.getPath()).getAbsolutePath()},
                new String[]{mimeTypeFromExtension}, new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        Log.d("TAG", "Image capture scanned into media store: $uri" + uri);
                    }
                });
    }
}