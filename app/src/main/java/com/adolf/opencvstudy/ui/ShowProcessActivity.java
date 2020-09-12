package com.adolf.opencvstudy.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.adolf.opencvstudy.R;
import com.adolf.opencvstudy.rv.ImgRVAdapter;
import com.adolf.opencvstudy.rv.ItemRVBean;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ShowProcessActivity extends AppCompatActivity {

    @BindView(R.id.rv_imgs)
    RecyclerView mRvImgs;
    private List<ItemRVBean> mRVBeanList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_process);
        ButterKnife.bind(this);

        String cachePath = getIntent().getStringExtra("cachePath");

        File mImgCachePath = new File(cachePath,"/process");

        if (mImgCachePath.exists()) {
            String[] list = mImgCachePath.list();
            if (list != null) {
                for (String s : list) {
                    File f = new File(mImgCachePath, s);
                    if (f.isFile()) {
                        if (s.endsWith(".jpg")) {
                            mRVBeanList.add(new ItemRVBean(f.getAbsolutePath(), s));
                        }
                    }
                }
            }
        } else {

            mImgCachePath.mkdirs();
        }


        ImgRVAdapter adapter = new ImgRVAdapter(mRVBeanList, this);
        GridLayoutManager manager = new GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false);
        mRvImgs.setLayoutManager(manager);
        mRvImgs.setAdapter(adapter);
    }
}