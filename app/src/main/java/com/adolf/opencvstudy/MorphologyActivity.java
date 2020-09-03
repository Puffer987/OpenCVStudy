package com.adolf.opencvstudy;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.adolf.opencvstudy.rv.ImgRVAdapter;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MorphologyActivity extends AppCompatActivity {
    private static final String TAG = "[jq]MorphologyActivity";
    @BindView(R.id.rv_imgs)
    RecyclerView mRvImgs;
    private List<String> mImgList = new ArrayList<>();
    private File mImgCachePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_morphology);
        ButterKnife.bind(this);

        mImgCachePath = new File(getExternalFilesDir(null), "/process");
        String[] list = mImgCachePath.list();
        for (String s : list)
            new File(mImgCachePath, s).delete();
        mImgCachePath.mkdirs();
    }

    @OnClick(R.id.btn_do)
    public void onViewClicked() {
        mImgList.clear();
        morphological(BitmapFactory.decodeResource(this.getResources(), R.drawable.handwrite));

        ImgRVAdapter adapter = new ImgRVAdapter(mImgList, this);
        GridLayoutManager manager = new GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false);
        mRvImgs.setLayoutManager(manager);
        mRvImgs.setAdapter(adapter);
    }

    private void morphological(Bitmap source) {
        Mat src = new Mat();
        Utils.bitmapToMat(source, src);
        Mat out = new Mat();
        Mat binary = new Mat();

        Imgproc.cvtColor(src, binary, Imgproc.COLOR_BGRA2GRAY);
        Imgproc.adaptiveThreshold(binary, binary, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 13, 5);
        saveMat(binary);

        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
        Imgproc.dilate(binary, out, element);
        saveMat(out);

        Imgproc.erode(binary, out, element);
        saveMat(out);

        Imgproc.morphologyEx(binary, out, Imgproc.MORPH_OPEN, element);
        saveMat(out);

        Imgproc.morphologyEx(binary, out, Imgproc.MORPH_CLOSE, element);
        saveMat(out);

        binary.release();
        src.release();
        out.release();
    }

    private void saveMat(Mat source) {
        File file = new File(mImgCachePath, "/" + System.currentTimeMillis() + ".jpg");
        Imgcodecs.imwrite(file.getAbsolutePath(), source);
        mImgList.add(file.getAbsolutePath());
    }
}