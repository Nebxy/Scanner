package com.be.scanner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.be.scanner.view.ScanView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {

    private TextView tvBarcode;
    private ImageView iv_scanning;
    private ScanView scanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        iv_scanning = findViewById(R.id.iv_scanning);
        scanner = findViewById(R.id.scanner);
        tvBarcode = findViewById(R.id.tv_barcode);
        scanner.setListener(new ScanView.CodeCaughtListener() {
            @Override
            public void onCodeCaught(final String barCode) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvBarcode.setText(String.format("Barcode:%s\nTimeStamp:%s",
                                barCode, System.currentTimeMillis()));
                    }
                });
            }
        });
        //扫描动画
        iv_scanning.setAlpha(0.5f);
        Animation verticalAnimation = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, -0.3f, Animation.RELATIVE_TO_PARENT, 1.2f,
                Animation.RELATIVE_TO_PARENT, 0, Animation.RELATIVE_TO_PARENT, 0);
        verticalAnimation.setDuration(3500);
        verticalAnimation.setRepeatCount(-1);
        verticalAnimation.setRepeatMode(Animation.RESTART);
        iv_scanning.startAnimation(verticalAnimation);

        if (hasPermissions(Manifest.permission.CAMERA)) {
            scanner.startScan(this);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }
    }

    @Override
    protected void onDestroy() {
        scanner.release();
        super.onDestroy();
    }

    public boolean hasPermissions(String... permissions) {
        for (String permission : permissions) {
            int result = ActivityCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (hasPermissions(Manifest.permission.CAMERA)) {
                scanner.startScan(this);
            }
        }
    }
}
