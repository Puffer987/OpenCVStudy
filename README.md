OpenCV 3 + Android

使用OpenCV 4时网上资料少，多为3的搭建方法

## Android+OpenCV3

1. 下载版本3，`https://opencv.org/releases/`，解压到任意目录

2.  新建安卓项目
2. 导入OpenCV SDK依赖

![image-20200901174223894](https://i.loli.net/2020/09/01/brODYdfWTxcgiJQ.png)

<img src="https://i.loli.net/2020/09/01/nsHCuOTRaxv9SUp.png" alt="image-20200901174514713" style="zoom:67%;" />

4. 导入模块依赖

   ![image-20200901174951445](https://i.loli.net/2020/09/01/jkLR1v6JxcgKCFN.png)

   ![image-20200901175146277](https://i.loli.net/2020/09/01/SE5gHWBdzeqvb1J.png)

4. 导入jni文件

   在`src\main`文件夹下新建一个jniLibs，并将OpenCV解压后的`\OpenCV-android-sdk\sdk\native\libs`下的文件拷入jniLibs，完成后目录结构如下

   ![image-20200901175424087](https://i.loli.net/2020/09/01/Lk49HUrdlmC3Jht.png)

4. 修改openCV模块的build.gradle配置文件

   ![image-20200901175816029](F:%5C_1Adolf%5C%E5%9D%9A%E6%9E%9C%E4%BA%91%5Creadboy%E5%B7%A5%E4%BD%9C%5C%E7%AC%94%E8%AE%B0%5COpenCV.assets%5Cimage-20200901175816029.png)

4. 修改app:build.gradle

   ```groovy
   android {
       //...
       sourceSets {
           main {
               jniLibs.srcDirs = ['src/main/jniLibs']
           }
       }
   }
   ```



## Android+OpenCV4

[参考文章：](https://blog.csdn.net/weixin_45137025/article/details/104684903)

1. 下载版本4，`https://opencv.org/releases/`，解压到任意目录

1. 新建安卓项目

   ![image-20200902145903093](https://i.loli.net/2020/09/02/G7nv1ObWxsciqVl.png)

1. 导入OpenCV SDK依赖

![image-20200901174223894](https://i.loli.net/2020/09/01/brODYdfWTxcgiJQ.png)

<img src="https://i.loli.net/2020/09/01/nsHCuOTRaxv9SUp.png" alt="image-20200901174514713" style="zoom:67%;" />

4. 修改导入的module中的build.gradle

   ```groovy
   apply plugin: 'com.android.library' // 修改为library
   
   android {
       compileSdkVersion 29 //修改和app:build.gradle一致
       buildToolsVersion "29.0.3"//修改和app:build.gradle一致
   
       // 删除applicationId
       
       buildTypes {
           release {
               minifyEnabled false
               proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.txt'
           }
       }
   }
   ```

4. 导入模块依赖

   ![image-20200901174951445](https://i.loli.net/2020/09/01/jkLR1v6JxcgKCFN.png)

   ![image-20200902150155953](https://i.loli.net/2020/09/02/U4nWkAIvt9u7lrE.png)

4. 导入jni文件

   在`app`文件夹下有个自带的lib文件夹，将OpenCV解压后的`\OpenCV-android-sdk\sdk\native\libs`下的文件拷入libs，完成后目录结构如下

   ![image-20200902150256655](https://i.loli.net/2020/09/02/ZmuoQWSD9brE21a.png)

   > libs放置的位置可以自定，但在sourceSets中第7步中要指明

   

4. 修改app:build.gradle

   ```groovy
   android {
       defaultConfig {
          // ...
           externalNativeBuild {
               cmake {
                   cppFlags ""
                   arguments "-DANDROID_STL=c++_shared"
               }
           }
       }
   
       //...
       sourceSets {
           main {
               // jniLibs.srcDirs = ['src/main/jniLibs']
               jniLibs.srcDirs = ['libs']
           }
       }
   }
   ```



### app:build.gradle

```groovy
apply plugin: 'com.android.application'

android {
    compileSdkVersion 29
    buildToolsVersion "29.0.3"

    defaultConfig {
        applicationId "com.adolf.opencv4"
        minSdkVersion 21
        targetSdkVersion 29
        versionCode 1
        versionName "1.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                cppFlags ""
                arguments "-DANDROID_STL=c++_shared"
            }
        }
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    externalNativeBuild {
        cmake {
            path "src/main/cpp/CMakeLists.txt"
            version "3.10.2"
        }
    }

    sourceSets {
        main{
            jniLibs.srcDirs = ['libs']
        }
    }
   
}

dependencies {
    implementation fileTree(dir: "libs", include: ["*.jar"])
    implementation 'androidx.appcompat:appcompat:1.2.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.0.0'
    implementation project(path: ':java')
    testImplementation 'junit:junit:4.12'
    androidTestImplementation 'androidx.test.ext:junit:1.1.2'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.3.0'

}
```



## 图片操作对象

### 一、Mat

#### 1）图像类型

>  CvType

`CV_[bite](U|S|F)C[channels]`

#### 2）创建对象

1. 加载已存在对象

   ```java
   Mat src = Imgcodecs.imread(img.getPath(), Imgcodecs.IMREAD_GRAYSCALE);
   ```

   第二个参数是加载图像类型

   - IMREAD_GRAYSCALE
   - IMREAD_UNCHANGED
   - IMREAD_COLOR

> 还可以使用`Utils.bitmapToMat(image, src);`将bitmap转换为mat



2. create创建、setTo赋值

   ```java
   Mat m = new Mat();
   m.create(new Size(4, 5),  类型);
   m.create(4, 5, 类型);
   
   Mat m = new Mat(3,3,CvType.CV_8UC3);
   m.setTo(new Scalar(255, 255, 255));
   ```


​       

3. ones、 eye、 zeros 静态方法
   ```java
   Mat m3 = Mat.eye(3, 3, CvType.CV_8UC3); 
   Mat m4 = Mat.eye(new Size(3, 3), CvType.CV_8UC3); 
   Mat m5 = Mat.zeros(new Size(3, 3), CvType.CV_8UC3); 
   Mat m6 = Mat.ones(new Size(3, 3), CvType.CV_8UC3);
   ```




4. 拷贝赋值copyTo、clone

   ```java
   Mat mat = new Mat(92,92,CvType.CV_8UC3);
   mat.setTo(new Scalar(127,127,127));
   Mat cmat = mat.clone();
   
   Mat mat = new Mat(92,92,CvType.CV_8UC3);
   mat.setTo(new Scalar(127,127,127));
   Mat rmat = new Mat();
   mat.copyTo(rmat);
   ```



#### 3）获取宽高

```java
int width = src.cols();
int height = src.rows();
int dims = src.dims();
int channels = src.channels();
int depth = src.depth();
int type = src.type();
```



#### 4）保存对象

```java
Imgcodecs.imwrite(filename, img)
```



### 二、Bitmap

#### 1）创建对象

BitmapFactory



### 三、两者注意

#### 1）图片缓冲区

在读取像素时要开辟一个像素缓冲区【二维数组】，不要平凡的直接操作对象，从对象拿像素点，在修改完后再将数组设置到对象中

```java
int[] pixels = new int[width * height];// 缓冲区
bm.getPixels(pixels, 0, width, 0, 0, width, height);
int a = 0, r = 0, g = 0, b = 0;
int index = 0;
for (int row = 0; row < height; row++) {
    for (int col = 0; col < width; col++) {
        //读取像来
        index = width * row + col;
        a = (pixels[index] >> 24) & 0xff;
        r = (pixels[index] >> 16) & 0xff;
        g = (pixels[index] >> 8) & 0xff;
        b = pixels[index] & 0xff;
        //修改像素
        r = 255 - r;
        g = 255 - g;
        b = 255 - b;
        //保存到Bitmap中
        pixels[index] = (a << 24) | (r << 16) | (g << 8) | b;
    }
}
```

- 一个一个读取：平凡访问对象，效率低
- 一行一行读取：比一个个读速度有所提高，但内存使用增加
- 一次读取全部：在内存中循环，速度最快，但高分辨率图片内存消耗大，容易OOM【内存泄露】



#### 2）相互转换

```java
Utils.matToBitmap(dstBm, mat);
Utils.bitmapToMat(dstMat, bm);
```



#### 3）释放内存

使用结束记得释放内存

```java
mat.release();
bitmap.recycle();
```





## 二值化

### adaptiveThreshold

自适应阈值二值化

```java
Imgproc.adaptiveThreshold(src, maxval, thresh_type, type, Block Size, C);
```

1. src： 输入图，只能输入单通道图像，通常来说为灰度图：`Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY);`
0. dst： 输出图
0. maxval： 当像素值超过了阈值（或者小于阈值，根据type来决定），所赋予的值
4. thresh_type： 阈值的计算方法，包含以下2种类型：
   - ADAPTIVE_THRESH_MEAN_C；
   - ADAPTIVE_THRESH_GAUSSIAN_C.
5. type：二值化操作的类型，与固定阈值函数相同，包含以下5种类型： 
   - THRESH_BINARY；  
   -  THRESH_BINARY_INV； 
   - THRESH_TRUNC；  
   - THRESH_TOZERO；
   - THRESH_TOZERO_INV.
6. Block Size： 图片中分块的大小
7. C ：阈值计算方法中的常数项



## 形态学

### dilate

膨胀算法，白色膨胀

```java
Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
Imgproc.dilate(binary, out, element);
```



### erode

腐蚀算法，白色变小

```java
Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
Imgproc.dilate(binary, out, element);
```



### morphologyEx

开运算：先腐蚀后膨胀--去除小白点

闭运算：先膨胀后腐蚀--去除小黑点

```java
Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
Imgproc.morphologyEx(binary, out, Imgproc.MORPH_OPEN, element);
```



## 特征标记

```
Imgproc.Canny(gray, cannyEdge, 10, 100);
```





### HoughLinesP

1. image为输入图像，要求是单通道，8位图像
1. lines为输出参数，4个元素表示，即直线的起始和终止端点的4个坐标（x1,y1）, (x2,y2)
1. rho为距离分辨率，一般为1
1. heta为角度的分辨率，一般CV_PI/180
1. threshold为阈值，hough变换图像空间值最大点，大于则执行
1. minLineLength为最小直线长度（像素），即如果小于该值，则不被认为是一条直线
1. maxLineGap为直线间隙最大值，如果两条直线间隙大于该值，则被认为是两条线段，否则是一条。