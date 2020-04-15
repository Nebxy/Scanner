package com.be.scanner.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.nio.ByteBuffer;
import java.util.concurrent.Executors;

import androidx.annotation.Nullable;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.lifecycle.LifecycleOwner;

/**
 * Created by Be on 2017/11/29.
 * 条码&二维码扫描
 */
public class ScanView extends TextureView {

    private Handler mHandler;
    private CodeCaughtListener mListener;

    private MultiFormatReader mReader;

    // 扫描同一条码时延迟扫描间隔
    private long mInterval = 3000;
    // 最近一次扫到的条码和时间
    private String mLastCode = "";
    private long mLastTime = 0;

    public boolean mRelease;

    public ScanView(Context context) {
        super(context);
        init();
    }


    public ScanView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ScanView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();

    }

    private void init() {
        mReader = new MultiFormatReader();
        mHandler = new Handler();
    }

    public void startScan(LifecycleOwner lifecycleOwber) {

        Size previewSize = CameraX.getSurfaceManager().getPreviewSize();

        PreviewConfig previewConfig = new PreviewConfig.Builder()
                //.setTargetAspectRatio(new Rational(1, 1))
                .setTargetResolution(new Size(previewSize.getWidth(), previewSize.getHeight()))
                .setCallbackHandler(new Handler(Looper.myLooper()))
                .build();

        Preview preview = new Preview(previewConfig);
        preview.setOnPreviewOutputUpdateListener(new Preview.OnPreviewOutputUpdateListener() {
            @Override
            public void onUpdated(Preview.PreviewOutput output) {
                setSurfaceTexture(output.getSurfaceTexture());
                updateTransform();
            }
        });

        HandlerThread handlerThread = new HandlerThread("Analyze-thread");
        handlerThread.start();
        ImageAnalysisConfig config = new ImageAnalysisConfig.Builder()
                .setBackgroundExecutor(Executors.newSingleThreadExecutor())
                //.setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .setCallbackHandler(new Handler(handlerThread.getLooper()))
                .setTargetRotation(Surface.ROTATION_0)
                .build();
        ImageAnalysis analysis = new ImageAnalysis(config);
        analysis.setTargetRotation(Surface.ROTATION_0);

        analysis.setAnalyzer(new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(ImageProxy image, int rotationDegrees) {
                if (ImageFormat.YUV_420_888 != image.getFormat()) {
                    return;
                }

                ImageProxy.PlaneProxy plane = image.getPlanes()[0];
                ByteBuffer buffer = plane.getBuffer();
                byte[] dst = new byte[buffer.remaining()];
                buffer.get(dst);

                int width = image.getWidth();
                int height = image.getHeight();
                try {

                    Result result;
                    if (rotationDegrees == 0 || rotationDegrees == 180) {
                        PlanarYUVLuminanceSource source =
                                new PlanarYUVLuminanceSource(dst, width, height, 0, 0, width, height, false);
                        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
                        result = mReader.decode(binaryBitmap);
                    } else {
                        // 相机旋转了九十度角
                        result = mReader.decode(genBinaryBitmapRotate(dst, width, height, 90));
                    }
                    final String code = result.getText();
                    if (!TextUtils.isEmpty(code) && mListener != null) {
                        final long millis = System.currentTimeMillis();
                        Log.i("BarcodeCaught >>>>>", code);
                        if (!mLastCode.equals(code)) {
                            if (millis - mLastTime > mInterval) {
                                if (mHandler != null) {
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            mListener.onCodeCaught(code);
                                        }
                                    });
                                }
                                mLastTime = millis;
                            }

                        } else {
                            // 如果扫码相同条码，考虑扫码间隔
                            if (millis - mLastTime > mInterval) {
                                if (mHandler != null) {
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            mListener.onCodeCaught(code);
                                        }
                                    });
                                }
                                mLastTime = millis;
                            }
                        }
                        mLastCode = code;
                    }

                } catch (Exception e) {
                    e.printStackTrace();

                }
            }
        });

        CameraX.bindToLifecycle(lifecycleOwber, preview, analysis);
    }

    /**
     * 设置两次扫码相同时的间隔时间
     *
     * @param mInterval
     */
    public void setInterval(float mInterval) {
        this.mInterval = (long) (mInterval * 1000);
    }

    private BinaryBitmap genBinaryBitmapRotate(byte[] data, int width, int height, float angle) {
        Matrix matrix = new Matrix();
        matrix.setRotate(angle);
        Bitmap bitmap = Bitmap.createBitmap(bt2int(data), width, height, Bitmap.Config.ALPHA_8);

        Bitmap ration90Bmp = rotateBitmap(bitmap, angle);

        ByteBuffer buf = ByteBuffer.allocate(ration90Bmp.getByteCount());
        ration90Bmp.copyPixelsToBuffer(buf);

        RGBLuminanceSource source = new RGBLuminanceSource(ration90Bmp.getWidth(), ration90Bmp.getHeight(), bt2int(buf.array()));
        return new BinaryBitmap(new HybridBinarizer(source));
    }

    private int[] bt2int(byte[] data) {
        int[] ints = new int[data.length];
        for (int i = 0; i < data.length; i++) {
            ints[i] = data[i];
        }
        return ints;
    }

    private Bitmap rotateBitmap(Bitmap origin, float rotation) {
        if (origin == null) {
            return null;
        }
        int width = origin.getWidth();
        int height = origin.getHeight();
        Matrix matrix = new Matrix();
        matrix.setRotate(rotation);
        // 围绕原地进行旋转
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (newBM.equals(origin)) {
            return newBM;
        }
        origin.recycle();
        return newBM;
    }

    private void updateTransform() {
        /*Matrix matrix = new Matrix();
        // Compute the center of the view finder
        float centerX = viewFinder.getWidth() / 2f;
        float centerY = viewFinder.getHeight() / 2f;

        float[] rotations = {0, 90, 180, 270};
        // Correct preview output to account for display rotation
        float rotationDegrees = rotations[viewFinder.getDisplay().getRotation()];

        matrix.postRotate(-rotationDegrees, centerX, centerY);

        // Finally, apply transformations to our TextureView
        viewFinder.setTransform(matrix);*/
    }

    public void release() {
        mRelease = true;
    }


    public void setListener(CodeCaughtListener mListener) {
        this.mListener = mListener;
    }

    public interface CodeCaughtListener {
        void onCodeCaught(String barCode);
    }

}
