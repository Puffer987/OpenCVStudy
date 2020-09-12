package com.adolf.opencvstudy.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @program: ebagOpenCV
 * @description: OpenCV处理图片
 * @author: Adolf
 * @create: 2020-09-11 20:27
 **/
public class CropUtils {

    private static final String TAG = "[crop]CropUtils";
    private static File mImgCachePath = null;

    /**
     * @Description: 图片最大轮廓点集
     * @Return: 返回值给findBorderRect()和findCorners()做参数
     * @Author: Adolf
     */
    public static MatOfPoint2f getContourPointSet(Bitmap bitmap) {
        Mat src = btm2Mat(bitmap);
        Mat dst = new Mat();
        // saveMat(src, "原图");

        Imgproc.cvtColor(src, dst, Imgproc.COLOR_BGR2GRAY);
        // saveMat(dst, "gray");

        Imgproc.GaussianBlur(dst, dst, new Size(7, 7), 5);
        saveMat(dst, "高斯模糊");

        Imgproc.adaptiveThreshold(dst, dst, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 81, 5);
        saveMat(dst, "自适应阈值");

        /**
         * contours : 所有的闭合轮廓
         * index：最大闭合轮廓的下标
         */
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(dst, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        if (contours.size() != 0) {
            int index = 0;
            double maxim = Imgproc.contourArea(contours.get(0));
            for (int i = 0; i < contours.size(); i++) {
                double t = Imgproc.contourArea(contours.get(i));
                if (maxim < t) {
                    maxim = t;
                    index = i;
                }
            }

            Mat drawing = Mat.zeros(src.size(), CvType.CV_8UC1);
            Imgproc.drawContours(drawing, contours, index, new Scalar(255), 1);
            saveMat(drawing, "最大闭合轮廓");

            /**
             * 轮廓多边形的点集
             * cure：原始轮廓
             * approxCure：输出的多边形点集
             */
            MatOfPoint2f cure = new MatOfPoint2f(contours.get(index).toArray());
            MatOfPoint2f approxCure = new MatOfPoint2f();
            Imgproc.approxPolyDP(cure, approxCure, contours.size() * 0.05, true);
            return approxCure;
        } else {
            return null;
        }
    }

    /**
     * @Description: 点集的角点
     * @Author: Adolf
     */
    public static List<Point> findCorners(MatOfPoint2f approxCure) {
        Log.d(TAG, "角点个数: " + approxCure.rows());
        List<Point> corners = new ArrayList<>();
        for (int i = 0; i < approxCure.rows(); i++) {
            Point temp = new Point(approxCure.get(i, 0)[0], approxCure.get(i, 0)[1]);
            corners.add(temp);
        }
        return corners;
    }


    /**
     * @Description: 点集的最小包含矩阵
     * @Author: Adolf
     */
    public static Rect findBorderRect(MatOfPoint2f approxCure) {
        return Imgproc.boundingRect(approxCure);
    }

    /**
     * @Description: 自动裁剪中部内容区（四周干净时）
     * @Author: Adolf
     */
    public static Rect autoCrop(Bitmap bitmap) {
        Mat src = btm2Mat(bitmap);
        Mat dst = new Mat();
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);

        Mat binary1 = new Mat();
        Mat binary2 = new Mat();
        Imgproc.adaptiveThreshold(gray, binary1, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 9, 20);
        saveMat(binary1, "自动阈值");

        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(3, 3));
        for (int i = 0; i < 3; i++) {
            Imgproc.morphologyEx(binary1, binary1, Imgproc.MORPH_OPEN, element);
            saveMat(binary1, "开运算" + i);
        }

        Imgproc.threshold(gray, binary2, 127, 255, Imgproc.THRESH_BINARY_INV);
        saveMat(binary2, "127阈值");

        Core.bitwise_and(binary1, binary2, dst);
        saveMat(dst, "自动阈值和127阈值做与运算");


        Imgproc.Canny(dst, dst, 100, 200);
        saveMat(dst, "Canny");

        Rect rect = Imgproc.boundingRect(dst);
        Mat drawing = Mat.zeros(src.size(), CvType.CV_8UC1);
        Imgproc.rectangle(drawing, rect, new Scalar(255), 2);
        saveMat(drawing, "包含点集的最小矩形");

        return rect;
    }

    /**
     * @Description: 进行投影变换，将图片摆正
     * @Param: corners四点的顺序是左上，右上，右下，左下
     * @Author: Adolf
     */
    public static Bitmap perspective(Mat src, List<Point> corners) {
        double top = Math.sqrt(Math.pow(corners.get(0).x - corners.get(1).x, 2) + Math.pow(corners.get(0).y - corners.get(1).y, 2));
        double right = Math.sqrt(Math.pow(corners.get(1).x - corners.get(2).x, 2) + Math.pow(corners.get(1).y - corners.get(2).y, 2));
        double bottom = Math.sqrt(Math.pow(corners.get(3).x - corners.get(2).x, 2) + Math.pow(corners.get(3).y - corners.get(2).y, 2));
        double left = Math.sqrt(Math.pow(corners.get(0).x - corners.get(3).x, 2) + Math.pow(corners.get(0).y - corners.get(3).y, 2));
        Mat quad = Mat.zeros(new Size(Math.max(top, bottom), Math.max(left, right)), CvType.CV_8UC3);
        Log.d(TAG, "quad: " + quad.size().toString());

        List<Point> result_pts = new ArrayList<Point>();
        Point p1 = new Point(0, 0);
        Point p2 = new Point(quad.cols(), 0);
        Point p3 = new Point(quad.cols(), quad.rows());
        Point p4 = new Point(0, quad.rows());

        result_pts.add(p1);
        result_pts.add(p2);
        result_pts.add(p3);
        result_pts.add(p4);

        Mat cornerPts = Converters.vector_Point2f_to_Mat(corners);
        Mat resultPts = Converters.vector_Point2f_to_Mat(result_pts);

        Mat transformation = Imgproc.getPerspectiveTransform(cornerPts, resultPts);
        Imgproc.warpPerspective(src, quad, transformation, quad.size());

        saveMat(quad, "透视变换");

        Imgproc.cvtColor(quad, quad, Imgproc.COLOR_BGR2RGBA);
        Bitmap bitmap = Bitmap.createBitmap(quad.cols(), quad.rows(),
                Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(quad, bitmap);

        return bitmap;
    }


    public static Mat btm2Mat(Bitmap bitmap) {
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2BGRA);
        return mat;
    }

    public static void saveBitmap(Bitmap source, File file) {
        try {
            FileOutputStream out = new FileOutputStream(file);
            source.compress(Bitmap.CompressFormat.JPEG, 80, out);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void saveMat(Mat source, String title) {
        if (mImgCachePath != null) {
            File file = new File(mImgCachePath, "/" + System.currentTimeMillis() + title + ".jpg");
            Imgcodecs.imwrite(file.getAbsolutePath(), source);
        }
    }

    /**
     * @Description: 图片锐化
     * @Param: 图片路径
     * @Author: Adolf
     */
    public static String  sharpen(String imgPath) {
        Mat src = Imgcodecs.imread(imgPath, Imgcodecs.IMREAD_COLOR);
        Mat dst = new Mat();
        Mat kernel = new Mat(3, 3, CvType.CV_16SC1);
        kernel.put(0, 0, 0, -1, 0, -1, 5, -1, 0, -1, 0);
        Imgproc.filter2D(src, dst, -1, kernel);
        Imgcodecs.imwrite(imgPath, dst);
        return imgPath;
    }

    /**
    * @Description: 图片缩放
    * @Param: scale>1放大，scale<1缩小
    * @Author: Adolf
    */
    public static Bitmap zoomImage(Bitmap orgBtm, float scale) {
        // scale = 指定像素 / 原始像素
        // 获取这个图片的宽和高
        int width = orgBtm.getWidth();
        int height = orgBtm.getHeight();

        // 创建操作图片用的matrix对象
        Matrix matrix = new Matrix();

        // 缩放图片动作
        matrix.postScale(scale, scale);
        return Bitmap.createBitmap(orgBtm, 0, 0, width, height, matrix, true);
    }

    public void setImgCachePath(File imgCachePath) {
        mImgCachePath = imgCachePath;
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
}
