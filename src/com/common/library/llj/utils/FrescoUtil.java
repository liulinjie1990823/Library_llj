package com.common.library.llj.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.common.library.llj.listener.OnMyControllerListener;
import com.facebook.common.internal.Supplier;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.ByteConstants;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.DraweeHolder;
import com.facebook.drawee.view.DraweeView;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.cache.MemoryCacheParams;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.producers.NetworkFetcher;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import bolts.Task;

/**
 * Created by liulj on 16/4/7.
 */
public class FrescoUtil {

    /**
     * @param context
     */
    public static void initFresco(final Context context) {
        initFresco(context, null);
    }

    /**
     * @param context
     */
    public static void initFresco(final Context context, NetworkFetcher networkFetcher) {
        //Fresco参数
        //当前应用可以从手机获取的最大内存
        final int MAX_HEAP_SIZE = (int) Runtime.getRuntime().maxMemory();
        LogUtil.LLJe("MAX_HEAP_SIZE:" + MAX_HEAP_SIZE);
        //设置多少内存来存放图片缓存
        final int MAX_MEMORY_CACHE_SIZE = MAX_HEAP_SIZE / 4;

        ImagePipelineConfig.Builder newBuilder = ImagePipelineConfig.newBuilder(context);

        //内存配置
        final MemoryCacheParams bitmapCacheParams = new MemoryCacheParams(
                getMaxCacheSize(), // 内存缓存中总图片的最大大小,以字节为单位。
                5,                     // 内存缓存中图片的最大数量。
                MAX_MEMORY_CACHE_SIZE, // 内存缓存中准备清除但尚未被删除的总图片的最大大小,以字节为单位。
                5,                     // 内存缓存中准备清除的总图片的最大数量。
                5);                    // 内存缓存中单个图片的最大大小。

        //修改内存图片缓存数量，空间策略（这个方式有点恶心）
        Supplier<MemoryCacheParams> mSupplierMemoryCacheParams = new Supplier<MemoryCacheParams>() {
            @Override
            public MemoryCacheParams get() {
                return bitmapCacheParams;
            }
        };
        //设置内存缓存
        newBuilder.setBitmapMemoryCacheParamsSupplier(mSupplierMemoryCacheParams);
        newBuilder.setDownsampleEnabled(true);

        //自定义网络请求层
        if (networkFetcher != null)
            newBuilder.setNetworkFetcher(networkFetcher);

//        //小图片的磁盘配置
//        DiskCacheConfig diskSmallCacheConfig = DiskCacheConfig.newBuilder(context)
//                .setBaseDirectoryPath(context.getApplicationContext().getCacheDir())//缓存图片基路径
//                .setBaseDirectoryName(IMAGE_PIPELINE_SMALL_CACHE_DIR)//文件夹名
//                .setCacheErrorLogger(cacheErrorLogger)//日志记录器用于日志错误的缓存。
//                .setCacheEventListener(cacheEventListener)//缓存事件侦听器。
//                .setDiskTrimmableRegistry(diskTrimmableRegistry)//类将包含一个注册表的缓存减少磁盘空间的环境。
//                .setMaxCacheSize(ConfigConstants.MAX_DISK_CACHE_SIZE)//默认缓存的最大大小。
//                .setMaxCacheSizeOnLowDiskSpace(MAX_SMALL_DISK_LOW_CACHE_SIZE)//缓存的最大大小,使用设备时低磁盘空间。
//                .setMaxCacheSizeOnVeryLowDiskSpace(MAX_SMALL_DISK_VERYLOW_CACHE_SIZE)//缓存的最大大小,当设备极低磁盘空间
//                .setVersion(version)
//                .build();
//
//        //默认图片的磁盘配置
//        DiskCacheConfig diskCacheConfig = DiskCacheConfig.newBuilder(context)
//                .setBaseDirectoryPath(Environment.getExternalStorageDirectory().getAbsoluteFile())//缓存图片基路径
//                .setBaseDirectoryName(IMAGE_PIPELINE_CACHE_DIR)//文件夹名
//                .setCacheErrorLogger(cacheErrorLogger)//日志记录器用于日志错误的缓存。
//                .setCacheEventListener(cacheEventListener)//缓存事件侦听器。
//                .setDiskTrimmableRegistry(diskTrimmableRegistry)//类将包含一个注册表的缓存减少磁盘空间的环境。
//                .setMaxCacheSize(ConfigConstants.MAX_DISK_CACHE_SIZE)//默认缓存的最大大小。
//                .setMaxCacheSizeOnLowDiskSpace(MAX_DISK_CACHE_LOW_SIZE)//缓存的最大大小,使用设备时低磁盘空间。
//                .setMaxCacheSizeOnVeryLowDiskSpace(MAX_DISK_CACHE_VERYLOW_SIZE)//缓存的最大大小,当设备极低磁盘空间
//                .setVersion(version)
//                .build();

//        newBuilder.setAnimatedImageFactory(AnimatedImageFactory animatedImageFactory);//图片加载动画
//        newBuilder.setBitmapMemoryCacheParamsSupplier(mSupplierMemoryCacheParams);//内存缓存配置（一级缓存，已解码的图片）
//        newBuilder.setCacheKeyFactory(cacheKeyFactory);//缓存Key工厂
//        newBuilder.setEncodedMemoryCacheParamsSupplier(encodedCacheParamsSupplier);//内存缓存和未解码的内存缓存的配置（二级缓存）
//        newBuilder.setExecutorSupplier(executorSupplier);//线程池配置
//        newBuilder.setImageCacheStatsTracker(imageCacheStatsTracker);//统计缓存的命中率
//        newBuilder.setImageDecoder(ImageDecoder imageDecoder); //图片解码器配置
//        newBuilder.setIsPrefetchEnabledSupplier(Supplier < Boolean > isPrefetchEnabledSupplier);//图片预览（缩略图，预加载图等）预加载到文件缓存
//        newBuilder.setMemoryTrimmableRegistry(memoryTrimmableRegistry); //内存用量的缩减,有时我们可能会想缩小内存用量。比如应用中有其他数据需要占用内存，不得不把图片缓存清除或者减小 或者我们想检查看看手机是否已经内存不够了。
//        newBuilder.setNetworkFetchProducer(networkFetchProducer);//自定的网络层配置：如OkHttp，Volley
//        newBuilder.setPoolFactory(poolFactory);//线程池工厂配置
//        newBuilder.setProgressiveJpegConfig(progressiveJpegConfig);//渐进式JPEG图
//        newBuilder.setRequestListeners(requestListeners);//图片请求监听
//        newBuilder.setResizeAndRotateEnabledForNetwork();//调整和旋转是否支持网络图片
//        newBuilder.setSmallImageDiskCacheConfig(diskSmallCacheConfig);//磁盘缓存配置（小图片，可选～三级缓存的小图优化缓存）
//        newBuilder.setMainDiskCacheConfig(diskCacheConfig);//磁盘缓存配置（总，三级缓存）
        ImagePipelineConfig config = newBuilder.build();
        Fresco.initialize(context.getApplicationContext(), config); //初始化Fresco
    }

    private static int getMaxCacheSize() {
        final int maxMemory = (int) Runtime.getRuntime().maxMemory();
        if (maxMemory < 32 * ByteConstants.MB) {
            return 4 * ByteConstants.MB;
        } else if (maxMemory < 64 * ByteConstants.MB) {
            return 6 * ByteConstants.MB;
        } else {
            // We don't want to use more ashmem on Gingerbread for now, since it doesn't respond well to
            // native memory pressure (doesn't throw exceptions, crashes app, crashes phone)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                return 8 * ByteConstants.MB;
            } else {
                return maxMemory / 4;
            }
        }
    }

    /**
     * @return
     */
    public static long getFrescoDiskCachesSize() {
        long size = Fresco.getImagePipelineFactory().getMainFileCache().getSize() + Fresco.getImagePipelineFactory().getSmallImageFileCache().getSize();
        if (size < 0) {
            size = 0;
        }
        return size;
    }

    /**
     * 清除磁盘缓存
     */
    public static void clearDiskCaches() {
        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        imagePipeline.clearDiskCaches();
    }

    /**
     * 圆形,圆角切图
     * 静态图可以使用setCornersRadius来设置圆角
     * gif的图需要使用setOverlayColor方法来设置圆角
     *
     * @return 设置图片圆形或者圆角的RoundingParams
     */
    public static RoundingParams getRoundingParams() {
        RoundingParams roundingParams = RoundingParams.fromCornersRadius(7f);
//    roundingParams.asCircle();//圆形
//    roundingParams.setBorder(color, width);//fresco:roundingBorderWidth="2dp"边框  fresco:roundingBorderColor="@color/border_color"
//    roundingParams.setCornersRadii(radii);//半径
//    roundingParams.setCornersRadii(topLeft, topRight, bottomRight, bottomLeft)//fresco:roundTopLeft="true" fresco:roundTopRight="false" fresco:roundBottomLeft="false" fresco:roundBottomRight="true"
//    roundingParams.setCornersRadius(radius);//fresco:roundedCornerRadius="1dp"圆角
//    roundingParams.setOverlayColor(overlayColor);//fresco:roundWithOverlayColor="@color/corner_color"
//    roundingParams.setRoundAsCircle(roundAsCircle);//圆
//    roundingParams.setRoundingMethod(roundingMethod);
//    fresco:progressBarAutoRotateInterval="1000"自动旋转间隔
        // 或用 fromCornersRadii 以及 asCircle 方法
        return roundingParams;
    }

    /**
     * 图片显示的控制器,设置不同状态下的图片
     * mSimpleDraweeView.setHierarchy(hierarchy);
     *
     * @param context
     * @return
     */
    public static GenericDraweeHierarchy getGenericDraweeHierarchy(Context context) {
        GenericDraweeHierarchyBuilder genericDraweeHierarchyBuilder = new GenericDraweeHierarchyBuilder(context.getResources());
//            .reset()//重置
//            .setActualImageColorFilter(colorFilter)//颜色过滤
//            .setActualImageFocusPoint(focusPoint)//focusCrop, 需要指定一个居中点
//            .setActualImageMatrix(actualImageMatrix)
//            .setActualImageScaleType(actualImageScaleType)//fresco:actualImageScaleType="focusCrop"缩放类型
//            .setBackground(background)//fresco:backgroundImage="@color/blue"背景图片
//            .setBackgrounds(backgrounds)
//            .setFadeDuration(fadeDuration)//fresco:fadeDuration="300"加载图片动画时间
//            .setFailureImage()//fresco:failureImage="@drawable/error"失败图
//            .setFailureImage(failureDrawable, failureImageScaleType)//fresco:failureImageScaleType="centerInside"失败图缩放类型
//            .setOverlay(overlay)//fresco:overlayImage="@drawable/watermark"叠加图
//            .setOverlays(overlays)
//            .setPlaceholderImage()//fresco:placeholderImage="@color/wait_color"占位图
//            .setPlaceholderImage(placeholderDrawable, placeholderImageScaleType)//fresco:placeholderImageScaleType="fitCenter"占位图缩放类型
//            .setPressedStateOverlay(drawable)//fresco:pressedStateOverlayImage="@color/red"按压状态下的叠加图
//            .setProgressBarImage(new ProgressBarDrawable())//进度条fresco:progressBarImage="@drawable/progress_bar"进度条
//            .setProgressBarImage(progressBarImage, progressBarImageScaleType)//fresco:progressBarImageScaleType="centerInside"进度条类型
//            .setRetryImage(retryDrawable)//fresco:retryImage="@drawable/retrying"点击重新加载
//            .setRetryImage(retryDrawable, retryImageScaleType)//fresco:retryImageScaleType="centerCrop"点击重新加载缩放类型
//            .setRoundingParams(RoundingParams.asCircle())//圆形/圆角fresco:roundAsCircle="true"圆形
//            .build();
        GenericDraweeHierarchy genericDraweeHierarchy = genericDraweeHierarchyBuilder.build();
        return genericDraweeHierarchy;
    }

    /**
     * 分装请求对象ImageRequest
     *
     * @param url
     * @param width
     * @param height
     * @return
     */
    public static ImageRequest getImageRequest(String url, int width, int height) {
        if (!TextUtils.isEmpty(url)) {
            String filterUrl = url;
            if (!filterUrl.startsWith("http:/")) {
                filterUrl = "file://" + filterUrl;
            }
            ImageRequestBuilder imageRequestBuilder = ImageRequestBuilder.newBuilderWithSource(Uri.parse(filterUrl));
            imageRequestBuilder.setResizeOptions(new ResizeOptions(width, height));//设置图片像素
            imageRequestBuilder.setRotationOptions(RotationOptions.autoRotate());//自动旋转图片方向
            imageRequestBuilder.setProgressiveRenderingEnabled(false);//是否逐行加载,主要用于渐进式的JPEG图,默认false,仅仅支持网络图
            imageRequestBuilder.setLocalThumbnailPreviewsEnabled(false);//是否用缩略图进行预览,本功能仅支持本地URI，并且是JPEG图片格式,默认false
            imageRequestBuilder.setLowestPermittedRequestLevel(ImageRequest.RequestLevel.FULL_FETCH);//请求经过缓存级别  BITMAP_MEMORY_CACHE，ENCODED_MEMORY_CACHE，DISK_CACHE，FULL_FETCH
//            imageRequestBuilder.setImageDecodeOptions(getImageDecodeOptions())//  图片解码库
//            imageRequestBuilder.setImageType(ImageType.SMALL)//图片类型，设置后可调整图片放入小图磁盘空间还是默认图片磁盘空间
//            imageRequestBuilder.setPostprocessor(postprocessor)//修改图片
            ImageRequest imageRequest = imageRequestBuilder.build();
            return imageRequest;
        }
        return null;
    }

    /**
     * SimpleDraweeView显示的控制器
     *
     * @param imageRequest
     * @param view
     * @return
     */
    public static DraweeController getDraweeController(ImageRequest imageRequest, SimpleDraweeView view) {
        DraweeController draweeController = Fresco.newDraweeControllerBuilder()
                .setAutoPlayAnimations(false)//自动播放图片动画
//            .setCallerContext(callerContext)//回调
//            .setControllerListener(view.getListener())//监听图片下载完毕等
//            .setDataSourceSupplier(dataSourceSupplier)//数据源
//            .setFirstAvailableImageRequests(firstAvailableImageRequests)//本地图片复用，可加入ImageRequest数组
                .setImageRequest(imageRequest)//设置单个图片请求～～～不可与setFirstAvailableImageRequests共用，配合setLowResImageRequest为高分辨率的图
//            .setLowResImageRequest(ImageRequest.fromUri(lowResUri))//先下载显示低分辨率的图,然后再显示正常的图
                .setOldController(view.getController())//DraweeController复用
                .setTapToRetryEnabled(false)//点击重新加载图
                .build();
        return draweeController;
    }

    public static DraweeController getDraweeController(String url, int width, int height, DraweeView view) {
        return getDraweeController(url, width, height, false, view, null);
    }

    public static void setController(String url, int width, int height, DraweeView view) {
        setController(url, width, height, false, view, null);
    }


    public static void setController(String url, int width, int height, boolean autoPlayAnimations, DraweeView view) {
        setController(url, width, height, autoPlayAnimations, view, null);
    }


    public static void setController(String url, int width, int height, DraweeView view, BaseControllerListener listener) {
        setController(url, width, height, false, view, listener);
    }


    public static void setController(int resId, int width, int height, DraweeView view) {
        setController(resId, width, height, false, view, null);
    }

    public static void setController(int resId, int width, int height, boolean autoPlayAnimations, DraweeView view) {
        setController(resId, width, height, autoPlayAnimations, view, null);
    }


    /**
     * 使用Fresco加载图片,设置Controller
     *
     * @param url                图片地址,网络或者本地地址
     * @param width              图片的宽度
     * @param height             图片的高度
     * @param autoPlayAnimations gif是否自动播放
     * @param view               需要显示的view
     * @param listener           加载监听
     */
    public static void setController(String url, int width, int height, boolean autoPlayAnimations, DraweeView view, BaseControllerListener listener) {
        if (!TextUtils.isEmpty(url) && view != null) {
            String filterUrl = url;
            if (!filterUrl.startsWith("http:/")) {
                filterUrl = "file://" + filterUrl;
            }
            ImageRequestBuilder imageRequestBuilder = ImageRequestBuilder.newBuilderWithSource(Uri.parse(filterUrl));
            if (width != 0 && height != 0)
                imageRequestBuilder.setResizeOptions(new ResizeOptions(width, height));//设置图片像素
            imageRequestBuilder.setProgressiveRenderingEnabled(false);//是否逐行加载,默认false
            imageRequestBuilder.setLocalThumbnailPreviewsEnabled(false);//是否用缩略图进行预览,默认false
            imageRequestBuilder.setRotationOptions(RotationOptions.autoRotate());//是否进行自动旋转
            ImageRequest imageRequest = imageRequestBuilder.build();

            PipelineDraweeControllerBuilder draweeControllerBuilder = Fresco.newDraweeControllerBuilder();
            draweeControllerBuilder.setImageRequest(imageRequest);

            if (listener != null)
                draweeControllerBuilder.setControllerListener(listener);

            if (view.getController() != null)
                draweeControllerBuilder.setOldController(view.getController());

            draweeControllerBuilder.setAutoPlayAnimations(autoPlayAnimations);
            view.setController(draweeControllerBuilder.build());
        }
    }

    /**
     * 获得fresco中图片加载用的DraweeController
     *
     * @param url                图片地址,网络或者本地地址
     * @param width              图片的宽度
     * @param height             图片的高度
     * @param autoPlayAnimations gif是否自动播放
     * @param view               需要显示的view
     * @param listener           加载监听
     * @return 返回DraweeController
     */
    public static DraweeController getDraweeController(String url, int width, int height, boolean autoPlayAnimations, DraweeView view, BaseControllerListener listener) {
        DraweeController draweeController = null;
        if (!TextUtils.isEmpty(url)) {
            String filterUrl = url;
            if (!filterUrl.startsWith("http:/")) {
                filterUrl = "file://" + filterUrl;
            }
            ImageRequestBuilder imageRequestBuilder = ImageRequestBuilder.newBuilderWithSource(Uri.parse(filterUrl));
            if (width != 0 && height != 0)
                imageRequestBuilder.setResizeOptions(new ResizeOptions(width, height));//设置图片像素
            imageRequestBuilder.setProgressiveRenderingEnabled(false);//是否逐行加载,默认false
            imageRequestBuilder.setLocalThumbnailPreviewsEnabled(false);//是否用缩略图进行预览,默认false
            imageRequestBuilder.setRotationOptions(RotationOptions.autoRotate());//是否进行自动旋转

            ImageRequest imageRequest = imageRequestBuilder.build();

            PipelineDraweeControllerBuilder draweeControllerBuilder = Fresco.newDraweeControllerBuilder();
            draweeControllerBuilder.setImageRequest(imageRequest);
            if (listener != null)
                draweeControllerBuilder.setControllerListener(listener);
            if (view.getController() != null)
                draweeControllerBuilder.setOldController(view.getController());
            draweeControllerBuilder.setAutoPlayAnimations(autoPlayAnimations);

            draweeController = draweeControllerBuilder.build();
        }
        return draweeController;
    }

    /**
     * 获得fresco中图片加载用的DraweeController
     *
     * @param resId              图片的资源id
     * @param width              图片的宽度
     * @param height             图片的高度
     * @param autoPlayAnimations gif是否自动播放
     * @param view               需要显示的view
     * @param listener           加载监听
     * @return 返回DraweeController
     */
    public static void setController(int resId, int width, int height, boolean autoPlayAnimations, DraweeView view, BaseControllerListener listener) {
        if (resId != 0 && view != null) {
            ImageRequestBuilder imageRequestBuilder = ImageRequestBuilder.newBuilderWithResourceId(resId);
            if (width != 0 && height != 0)
                imageRequestBuilder.setResizeOptions(new ResizeOptions(width, height));//设置图片像素
            imageRequestBuilder.setProgressiveRenderingEnabled(false);//是否逐行加载,默认false
            imageRequestBuilder.setLocalThumbnailPreviewsEnabled(false);//是否用缩略图进行预览,默认false
            imageRequestBuilder.setRotationOptions(RotationOptions.autoRotate());//是否进行自动旋转

            ImageRequest imageRequest = imageRequestBuilder.build();

            PipelineDraweeControllerBuilder draweeControllerBuilder = Fresco.newDraweeControllerBuilder();
            draweeControllerBuilder.setImageRequest(imageRequest);//
            if (listener != null)
                draweeControllerBuilder.setControllerListener(listener);//
            draweeControllerBuilder.setOldController(view.getController());//
            draweeControllerBuilder.setAutoPlayAnimations(autoPlayAnimations);//

            view.setController(draweeControllerBuilder.build());
        }
    }


    /**
     * 获取图像解码对象ImageDecodeOptions
     */
    public static ImageDecodeOptions getImageDecodeOptions() {
        ImageDecodeOptions decodeOptions = ImageDecodeOptions.newBuilder()
//            .setBackgroundColor(Color.TRANSPARENT)//图片的背景颜色
//            .setForceOldAnimationCode(forceOldAnimationCode)//使用以前动画
//            .setFrom(options)//使用已经存在的图像解码
//            .setMinDecodeIntervalMs(intervalMs)//最小解码间隔（分位单位）
//            .setDecodeAllFrames(decodeAllFrames)//解码所有帧
//            .setDecodePreviewFrame(decodePreviewFrame)//解码预览框
//            .setUseLastFrameForPreview(true)//使用最后一帧进行预览
                .build();
        return decodeOptions;
    }

    /**
     * 后来证明该方法中的加载bitmap的方式必须要放在自定义的view中
     * 因为要控制DraweeHolder的生命周期
     *
     * @param context
     * @param uri
     * @param width
     * @param height
     */
    @Deprecated
    public static void setImageBitmap2(Context context, String uri, int width, int height, final OnMyControllerListener onMyControllerListener) {
        GenericDraweeHierarchy hierarchy = new GenericDraweeHierarchyBuilder(context.getResources())
                .setFadeDuration(300)
                .build();
        DraweeHolder<GenericDraweeHierarchy> mDraweeHolder = DraweeHolder.create(hierarchy, context);

        ImageRequest imageRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse(uri))
                .setRotationOptions(RotationOptions.autoRotate())
                .setResizeOptions(new ResizeOptions(width, height))
                .build();
        ImagePipeline imagePipeline = Fresco.getImagePipeline();

        final DataSource<CloseableReference<CloseableImage>> dataSource = imagePipeline.fetchDecodedImage(imageRequest, context);

        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setOldController(mDraweeHolder.getController())
                .setImageRequest(imageRequest)
                .setControllerListener(new BaseControllerListener<ImageInfo>() {
                    @Override
                    public void onFinalImageSet(String s, @Nullable ImageInfo imageInfo, @Nullable Animatable animatable) {
                        CloseableReference<CloseableImage> imageReference = null;
                        try {
                            imageReference = dataSource.getResult();
                            if (imageReference != null) {
                                CloseableImage image = imageReference.get();
                                if (image != null && image instanceof CloseableStaticBitmap) {
                                    CloseableStaticBitmap closeableStaticBitmap = (CloseableStaticBitmap) image;
                                    Bitmap bitmap = closeableStaticBitmap.getUnderlyingBitmap();
                                    if (bitmap != null) {
                                        if (onMyControllerListener != null)
                                            onMyControllerListener.onFinalImageSet(bitmap);
                                    }
                                }
                            }
                        } finally {
                            dataSource.close();
                            CloseableReference.closeSafely(imageReference);
                        }
                    }

                    @Override
                    public void onFailure(String id, Throwable throwable) {
                        super.onFailure(id, throwable);
                    }

                    @Override
                    public void onIntermediateImageFailed(String id, Throwable throwable) {
                        super.onIntermediateImageFailed(id, throwable);
                    }
                })
                .setAutoPlayAnimations(true)
                .build();
        mDraweeHolder.setController(controller);
    }

    /**
     * 使用fresco直接下载图片,由于subscribe是在子线程,所以onNewResultImpl回调也发生在子线程
     *
     * @param context                上下文
     * @param uri                    图片下载地址
     * @param width                  图片像素的宽度
     * @param height                 图片像素的高度
     * @param onMyControllerListener 加载好后的监听器
     */

    public static void setImageBitmap(Context context, String uri, int width, int height, final OnMyControllerListener onMyControllerListener) {
        if (TextUtils.isEmpty(uri)) {
            return;
        }
        ImageRequest imageRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse(uri))
                .setRotationOptions(RotationOptions.autoRotate())
                .setResizeOptions(new ResizeOptions(width, height))
                .build();
        ImagePipeline imagePipeline = Fresco.getImagePipeline();

        DataSource<CloseableReference<CloseableImage>> dataSource = imagePipeline.fetchDecodedImage(imageRequest, context);
        dataSource.subscribe(new BaseBitmapDataSubscriber() {
            @Override
            protected void onNewResultImpl(@javax.annotation.Nullable Bitmap bitmap) {
                if (bitmap != null) {
                    if (onMyControllerListener != null)
                        onMyControllerListener.onFinalImageSet(bitmap);
                }
            }

            @Override
            protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {

            }
        }, Task.BACKGROUND_EXECUTOR);
    }
}
