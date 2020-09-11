package com.adolf.opencvstudy.view;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.adolf.opencvstudy.R;
import com.adolf.opencvstudy.rv.ImgRVAdapter;
import com.adolf.opencvstudy.rv.ItemRVBean;
import com.adolf.opencvstudy.utils.ImgUtil;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
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
    private List<ItemRVBean> mRVBeanList = new ArrayList<>();
    private File mImgCachePath;
    private ImgUtil mImgUtil;
    private Bitmap mOrgBtm;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_morphology);
        ButterKnife.bind(this);
        mImgCachePath = new File(getExternalFilesDir(null), "/process");
        mImgUtil = new ImgUtil(mImgCachePath,mRVBeanList);

        String path = getIntent().getStringExtra("img");
        mOrgBtm = BitmapFactory.decodeFile(path);
    }

    @OnClick(R.id.btn_do)
    public void onViewClicked() {
        mRVBeanList.clear();
        morphological(mOrgBtm);

        ImgRVAdapter adapter = new ImgRVAdapter(mRVBeanList, this);
        GridLayoutManager manager = new GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false);
        mRvImgs.setLayoutManager(manager);
        mRvImgs.setAdapter(adapter);
    }

    private void morphological(Bitmap source) {
        Mat src = new Mat();
        Utils.bitmapToMat(source, src);
        Mat out = new Mat();
        Mat binary = new Mat();
        Mat temp = new Mat();

        Imgproc.cvtColor(src, binary, Imgproc.COLOR_BGRA2GRAY);
        Imgproc.adaptiveThreshold(binary, binary, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 13, 5);
        mImgUtil.saveMat(binary, "均值，二值");

        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
        Imgproc.dilate(binary, out, element);
        mImgUtil.saveMat(out, "膨胀:扩大高亮部分");

        Imgproc.erode(binary, out, element);
        mImgUtil.saveMat(out, "腐蚀:缩小高亮部分");

        Imgproc.morphologyEx(binary, out, Imgproc.MORPH_OPEN, element);
        mImgUtil.saveMat(out, "开运算：先腐蚀后膨胀--去除小白点");

        Imgproc.morphologyEx(binary, out, Imgproc.MORPH_CLOSE, element);
        mImgUtil.saveMat(out, "闭运算：先膨胀后腐蚀--去除小黑点");


        Mat element2 = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(4, 4));
        Imgproc.morphologyEx(binary, temp, Imgproc.MORPH_OPEN, element);
        Imgproc.erode(temp, out, element2);
        mImgUtil.saveMat(out, "开运算+腐蚀");

        binary.release();
        src.release();
        out.release();
    }

}