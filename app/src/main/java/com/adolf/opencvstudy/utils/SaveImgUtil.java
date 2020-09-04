package com.adolf.opencvstudy.utils;

import android.graphics.Bitmap;
import android.widget.Toast;

import com.adolf.opencvstudy.rv.ItemRVBean;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * @program: OpenCVStudy
 * @description:
 * @author: Adolf
 * @create: 2020-09-03 17:50
 **/
public class SaveImgUtil {

    private File mImgCachePath;
    private List<ItemRVBean> mRVBeanList;

    public SaveImgUtil(File imgCachePath, List<ItemRVBean> RVBeanList) {
        mImgCachePath = imgCachePath;
        mRVBeanList = RVBeanList;

        if (mImgCachePath.exists()) {
            String[] list = mImgCachePath.list();
            if (list != null) {
                for (String s : list) {
                    File f = new File(mImgCachePath, s);
                    if (f.isFile()) {
                        f.delete();
                    }
                }
            }
        } else {
            mImgCachePath.mkdirs();
        }
    }

    public void saveBitmap(Bitmap source, String title) {
        File file = new File(mImgCachePath, "/" + System.currentTimeMillis() + ".jpg");
        mRVBeanList.add(new ItemRVBean(file.getAbsolutePath(), title));
        try {
            FileOutputStream out = new FileOutputStream(file);
            source.compress(Bitmap.CompressFormat.JPEG, 80, out);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveMat(Mat source, String title) {
        File file = new File(mImgCachePath, "/" + System.currentTimeMillis() + ".jpg");
        Imgcodecs.imwrite(file.getAbsolutePath(), source);
        mRVBeanList.add(new ItemRVBean(file.getAbsolutePath(), title));
    }

    public List<ItemRVBean> getRVBeanList() {
        return mRVBeanList;
    }

}
