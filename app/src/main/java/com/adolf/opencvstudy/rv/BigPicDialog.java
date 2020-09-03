package com.adolf.opencvstudy.rv;

import android.app.Dialog;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import com.adolf.opencvstudy.MainActivity;
import com.adolf.opencvstudy.R;
import com.bumptech.glide.Glide;
import com.squareup.picasso.Picasso;

import butterknife.BindView;

/**
 * @program: OpenCVStudy
 * @description:
 * @author: Adolf
 * @create: 2020-09-03 14:57
 **/
public class BigPicDialog extends Dialog {

private ImageView mBigImg;
private String imgPath;

    public BigPicDialog(@NonNull Context context, @StyleRes int themeResId) {
        super(context, themeResId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_big_pic);
        // //空白处不能取消动画
        // setCanceledOnTouchOutside(false);

        //初始化界面控件
        initView();

        //初始化界面数据
        initData();
        //初始化界面控件的事件

        mBigImg.setOnClickListener(v -> {
            this.dismiss();
        });
    }

    /**
     * 初始化界面控件
     */
    private void initView() {
        mBigImg = findViewById(R.id.iv_big);
    }

    /**
     * 初始化界面控件的显示数据
     */
    private void initData() {
        //如果用户自定了title和message
        mBigImg.setImageBitmap(BitmapFactory.decodeFile(imgPath));
    }


    public void setImgPath(String imgPath) {
        this.imgPath = imgPath;
    }

}