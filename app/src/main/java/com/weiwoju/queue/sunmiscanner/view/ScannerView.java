package com.weiwoju.queue.sunmiscanner.view;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.sunmi.scan.Config;
import com.sunmi.scan.Image;
import com.sunmi.scan.ImageScanner;
import com.sunmi.scan.Symbol;
import com.sunmi.scan.SymbolSet;
import com.weiwoju.queue.sunmiscanner.R;
import com.weiwoju.queue.sunmiscanner.SoundUtils;

/**
 * Created by Be on 2017/11/29.
 * 条码&二维码扫描
 */

public class ScannerView extends SurfaceView implements SurfaceHolder.Callback {

    private Camera mCamera;
    //  扫描器
    private ImageScanner scanner;
    private Handler autoFocusHandler;
    private AsyncDecode asyncDecode;
    private Context context;
    private boolean playSound = true;
    private CodeCaughtListener codeCaughtListener;
    private int measureedWidth;
    private int measureedHeight;
    // 扫描同一条码时延迟扫描间隔
    private long delayTime = 3000;
    // 最近一次扫到的条码和时间
    private String lastBarcode = "";
    private long lastScannedTima = 0;

    public ScannerView(Context context) {
        super(context);
        init(context);
    }



    public ScannerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);

    }

    public ScannerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);

    }

    private void init(Context context) {
        this.context = context;
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        getHolder().addCallback(this);

        scanner = new ImageScanner();// 创建扫描器
        scanner.setConfig(0, Config.X_DENSITY, 2);// 行扫描间隔
        scanner.setConfig(0, Config.Y_DENSITY, 2);// 列扫描间隔
        scanner.setConfig(0, Config.ENABLE_MULTILESYMS, 0);// 是否开启同一幅图一次解多个条码,0表示只解一个，1为多个
        scanner.setConfig(0, Config.ENABLE_INVERSE, 0);// 是否解反色的条码

        autoFocusHandler = new Handler();
        asyncDecode = new AsyncDecode();
        setBackgroundResource(R.drawable.scanbackground);
        SoundUtils.getInstance(context).putSound(0, R.raw.beep);

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
            parameters.setPreviewSize(200, 400); // 设置预览分辨率
            // parameters.set("zoom", String.valueOf(27 / 10.0));   放大
            mCamera.setParameters(parameters);
            mCamera.setDisplayOrientation(90);   // 竖屏显示
            mCamera.setPreviewDisplay(getHolder());
            mCamera.setPreviewCallback(previewCallback);
            mCamera.startPreview();
            mCamera.autoFocus(autoFocusCallback);
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


    public void setCodeCaughtListener(CodeCaughtListener codeCaughtListener) {
        this.codeCaughtListener = codeCaughtListener;
    }


    private class AsyncDecode extends AsyncTask<Image, Void, String> {
        private boolean stoped = true;

        @Override
        protected String doInBackground(Image... params) {
            stoped = false;
            Image src_data = params[0];// 获取灰度数据

            // 解码，返回值为0代表失败，>0表示成功
            int nsyms = scanner.scanImage(src_data);
            if (nsyms != 0) {
                SymbolSet syms = scanner.getResults();// 获取解码结果
                for (Symbol sym : syms) {
                    String barCode = sym.getResult();
                    return barCode;
                }
            }
            return "";
        }


        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            stoped = true;
            if (!TextUtils.isEmpty(result)) {
                //  解码结果
                if (codeCaughtListener != null) {
                    long millis = System.currentTimeMillis();
                    Log.i("BarcodeCaught >>>>>", result);
                    if (!lastBarcode.equals(result)) {
                        codeCaughtListener.onCodeCaught(result);
                        playBeepSoundAndVibrate();// 解码成功播放提示音
                        lastScannedTima = millis;

                    } else {
                        // 如果扫码相同条码，考虑扫码间隔
                        if (millis - lastScannedTima > delayTime) {
                            codeCaughtListener.onCodeCaught(result);
                            playBeepSoundAndVibrate();// 解码成功播放提示音
                            lastScannedTima = millis;

                        }
                    }

                    lastBarcode = result;

                }
            }
        }

        public boolean isStoped() {
            return stoped;
        }
    }

    private void playBeepSoundAndVibrate() {
        if (playSound) {
            SoundUtils.getInstance(context).playSound(0, SoundUtils.SINGLE_PLAY);
        }
    }

    public interface CodeCaughtListener {
        void onCodeCaught(String barCode);
    }

    private Rect topRect = new Rect();
    private Rect bottomRect = new Rect();
    private Rect rightRect = new Rect();
    private Rect leftRect = new Rect();
    private Rect middleRect = new Rect();

    public Rect getScanImageRect(int w, int h) {
        //先求出实际矩形
        Rect rect = new Rect();
        float tempw = w / (float) measureedWidth;
        float temph = h / (float) measureedHeight;
        rect.left = (int) (middleRect.left * tempw);
        rect.right = (int) (middleRect.right * tempw);
        rect.top = (int) (middleRect.top * temph);
        rect.bottom = (int) (middleRect.bottom * temph);
        return rect;
    }

    Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (asyncDecode.isStoped()) {
                Camera.Parameters parameters = camera.getParameters();
                Camera.Size size = parameters.getPreviewSize(); // 获取预览分辨率

                // 创建解码图像，并转换为原始灰度数据，注意图片是被旋转了90度的
                Image source = new Image(size.width, size.height, "Y800");
                Rect scanImageRect = getScanImageRect(size.height,
                        size.width);
                // 图片旋转了90度，将扫描框的TOP作为left裁剪
                source.setCrop(scanImageRect.top, scanImageRect.left,
                        scanImageRect.height(), scanImageRect.width());
                source.setData(data);
                asyncDecode = new AsyncDecode();
                asyncDecode.execute(source);
            }
        }
    };

    /**
     * 自动对焦回调
     */
    Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            autoFocusHandler.postDelayed(doAutoFocus, 1000);
        }
    };
    // 自动对焦
    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (null == mCamera || null == autoFocusCallback) {
                return;
            }
            mCamera.autoFocus(autoFocusCallback);
        }
    };


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureedWidth = MeasureSpec.getSize(widthMeasureSpec);
        measureedHeight = MeasureSpec.getSize(heightMeasureSpec);
        int borderWidth = measureedWidth / 2 + 160;
        middleRect.set((measureedWidth - borderWidth) / 2, (measureedHeight - borderWidth) / 2,
                (measureedWidth - borderWidth) / 2 + borderWidth, (measureedHeight - borderWidth) / 2 + borderWidth);
        leftRect.set(0, middleRect.top, middleRect.left, middleRect.bottom);
        topRect.set(0, 0, measureedWidth, middleRect.top);
        rightRect.set(middleRect.right, middleRect.top, measureedWidth, middleRect.bottom);
        bottomRect.set(0, middleRect.bottom, measureedWidth, measureedHeight);
    }

    /**
     * 开/关扫码提示声
     */
    public void setSoundEnabled(boolean enabled) {
        playSound = enabled;
    }

    public void setSoundResource(int resourceId) {
        SoundUtils.getInstance(context).putSound(0, resourceId);
    }

    /**
     * 设置两次扫码相同时的间隔时间
     *
     * @param delayTime
     */
    public void setDelayTime(float delayTime) {
        this.delayTime = (long) (delayTime * 1000);
    }
}
