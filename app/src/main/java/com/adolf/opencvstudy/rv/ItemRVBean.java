package com.adolf.opencvstudy.rv;


/**
 * @program: OpenCVStudy
 * @description:
 * @author: Adolf
 * @create: 2020-09-03 16:24
 **/
public class ItemRVBean {

    private String mImgPath;
    private String mImgTitle;

    public ItemRVBean(String imgPath, String imgTitle) {
        mImgPath = imgPath;
        mImgTitle = imgTitle;
    }

    public String getImgPath() {
        return mImgPath;
    }

    public void setImgPath(String imgPath) {
        mImgPath = imgPath;
    }

    public String getImgTitle() {
        return mImgTitle;
    }

    public void setImgTitle(String imgTitle) {
        mImgTitle = imgTitle;
    }
}
