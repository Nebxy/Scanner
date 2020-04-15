package com.be.scanner.view;

import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.util.List;

import androidx.annotation.Nullable;

/**
 * Created by Be on 2017/11/29.
 * 条码&二维码扫描
 */
@Deprecated
public class ScannerView extends SurfaceView implements SurfaceHolder.Callback {

    private Camera mCamera;
    private Handler mHandler;
    private CodeCaughtListener mListener;

    private MultiFormatReader mReader;

    // 扫描同一条码时延迟扫描间隔
    private long mInterval = 500;
    // 最近一次扫到的条码和时间
    private String mLastCode = "";
    private long mLastTime = 0;

    public boolean mWorking;
    public boolean mRelease;
    public byte[] mData;

    private Thread mTreadAnalysis;

    public ScannerView(Context context) {
        super(context);
        init();
    }


    public ScannerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ScannerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();

    }

    private void init() {
        mReader = new MultiFormatReader();
        mHandler = new Handler();
    }

    public void startScan() {
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        getHolder().addCallback(this);

        startAnalysisTask();
        if (mCamera == null) {
            try {
                mCamera = Camera.open();
                open();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 单独的线程来解析扫码图片
     */
    private void startAnalysisTask() {
        if (mTreadAnalysis != null) {
            mTreadAnalysis.interrupt();
            mTreadAnalysis = null;
        }
        mTreadAnalysis = new Thread() {
            @Override
            public void run() {
                while (!mRelease && !interrupted()) {
                    byte[] data = mData;
                    if (data == null || mCamera == null) {
                        mWorking = false;
                        continue;
                    }
                    mWorking = true;

                    try {
                        Camera.Parameters parameters = mCamera.getParameters();
                        Camera.Size size = parameters.getPreviewSize();
                        int width = size.width;
                        int height = size.height;

                        BinaryBitmap binaryBitmap = genBinaryBitmap(data, width, height);
                        Result result = mReader.decode(binaryBitmap);

                        String code = result.getText();
                        if (!TextUtils.isEmpty(code) && mListener != null) {
                            final long millis = System.currentTimeMillis();
                            Log.i("BarcodeCaught >>>>>", code);
                            if (!mLastCode.equals(code)) {
                                if (millis - mLastTime > mInterval) {
                                    mListener.onCodeCaught(code);
                                    mLastTime = millis;
                                }

                            } else {
                                // 如果扫码相同条码，考虑扫码间隔
                                if (millis - mLastTime > mInterval) {
                                    mListener.onCodeCaught(code);
                                    mLastTime = millis;
                                }
                            }

                            mLastCode = code;
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mWorking = false;
                    mData = null;
                }

            }
        };
        mTreadAnalysis.start();
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera = Camera.open();
        } catch (Exception e) {
            mCamera = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        open();
    }

    private void open() {
        if (getHolder().getSurface() == null) {
            return;
        }
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
        }
        try {
            // 摄像头预览分辨率设置和图像放大参数设置，非必须，根据实际解码效果可取舍
            Camera.Parameters parameters = mCamera.getParameters();
            int width_camera = 320;
            int height_camera = 240;

            List<Camera.Size> supportSizes = parameters.getSupportedPreviewSizes();

            for (Camera.Size size : supportSizes) {
                if (size.height > 300) {
                    width_camera = size.width;
                    height_camera = size.height;
                    break;
                }

            }
            parameters.setPreviewSize(width_camera, height_camera); // 设置预览分辨率
            // parameters.set("zoom", String.valueOf(27 / 10.0));   //放大

            mCamera.setParameters(parameters);
            mCamera.setDisplayOrientation(90);   // 竖屏显示
            mCamera.setPreviewDisplay(getHolder());
            mCamera.setPreviewCallback(previewCallback);
            mCamera.startPreview();
            mCamera.autoFocus(mCallbackAutoFocus);
        } catch (Exception e) {
            Log.d("DBG", "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }


    public void setListener(CodeCaughtListener mListener) {
        this.mListener = mListener;
    }

    public interface CodeCaughtListener {
        void onCodeCaught(String barCode);
    }

    Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        public void onPreviewFrame(final byte[] data, final Camera camera) {

            if (!mWorking) {
                mData = data;
            }

        }
    };

    private BinaryBitmap genBinaryBitmap(byte[] data, int width, int height) {
        RGBLuminanceSource source = new RGBLuminanceSource(width, height, rotateAndToIntArr(data, width, height));
        return new BinaryBitmap(new HybridBinarizer(source));
    }

    /**
     * 自动对焦回调
     */
    Camera.AutoFocusCallback mCallbackAutoFocus = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            mHandler.postDelayed(doAutoFocus, 1000);
        }
    };
    // 自动对焦
    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (null == mCamera || null == mCallbackAutoFocus) {
                return;
            }
            mCamera.autoFocus(mCallbackAutoFocus);
        }
    };


    /**
     * 设置两次扫码相同时的间隔时间
     *
     * @param mInterval
     */
    public void setInterval(float mInterval) {
        this.mInterval = (long) (mInterval * 1000);
    }


    /**
     * 转成int数组并且旋转90度
     *
     * @param data
     * @param imageWidth
     * @param imageHeight
     * @return
     */
    private int[] rotateAndToIntArr(byte[] data, int imageWidth, int imageHeight) {
        int[] yuv = new int[imageWidth * imageHeight * 3 / 2];
        int i = 0;
        for (int x = 0; x < imageWidth; x++) {
            for (int y = imageHeight - 1; y >= 0; y--) {
                yuv[i] = data[y * imageWidth + x];
                i++;
            }
        }
        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (int x = imageWidth - 1; x > 0; x = x - 2) {
            for (int y = 0; y < imageHeight / 2; y++) {
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
                i--;
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + (x - 1)];
                i--;
            }
        }
        return yuv;
    }

    public void release() {
        mData = null;
        mRelease = true;
        if (mCamera != null) {
            mCamera.release();
        }

        if (mTreadAnalysis != null) {
            mTreadAnalysis.interrupt();
            mTreadAnalysis = null;
        }

    }
}
