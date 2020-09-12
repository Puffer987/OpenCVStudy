package com.adolf.opencvstudy.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Button;

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

public class BlurSharpenActivity extends AppCompatActivity {

    @BindView(R.id.rv_imgs)
    RecyclerView mRvImgs;
    @BindView(R.id.btn_do)
    Button mBtnDo;

    private List<ItemRVBean> mRVBeanList = new ArrayList<>();
    private File mImgCachePath;
    private ImgUtil mImgUtil;
    private Bitmap mOrgBtm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blur_sharpen);
        ButterKnife.bind(this);
        mImgCachePath = new File(getExternalFilesDir(null), "/process");

        mImgUtil = new ImgUtil(mImgCachePath, mRVBeanList);

        String path = getIntent().getStringExtra("img");
        mOrgBtm = BitmapFactory.decodeFile(path);
    }


    @OnClick(R.id.btn_do)
    public void onViewClicked() {
        mBtnDo.setEnabled(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                myBlur(mOrgBtm);
                runOnUiThread(() -> showImg());
            }
        }).start();

    }

    private void showImg() {

        ImgRVAdapter adapter = new ImgRVAdapter(mRVBeanList, this);
        GridLayoutManager manager = new GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false);
        mRvImgs.setLayoutManager(manager);
        mRvImgs.setAdapter(adapter);
        mBtnDo.setEnabled(true);
    }

    private void myBlur(Bitmap source) {
        Mat src = new Mat();
        Utils.bitmapToMat(source, src);
        Mat out = new Mat();
        Imgproc.cvtColor(src,src,Imgproc.COLOR_RGBA2BGRA);
        mImgUtil.saveMat(src, "原图");

        /*
         * 模糊核
         * height > width ：有种橡皮擦往上擦的感觉
         * 还能放一个参数：锚点，默认为Point(-1,-1)，即核的中心。
         */
        Imgproc.blur(src, out, new Size(50, 10));
        mImgUtil.saveMat(out, "均值模糊");

        Imgproc.GaussianBlur(src, out, new Size(7, 13), 0);
        mImgUtil.saveMat(out, "模糊");

        Imgproc.medianBlur(src, out, 13);
        mImgUtil.saveMat(out, "中值模糊");

        // Imgproc.blur(src,out,new Size(3,3));
        // mImgUtil.saveMat(src,"模糊");


        src.release();
        out.release();
    }


}