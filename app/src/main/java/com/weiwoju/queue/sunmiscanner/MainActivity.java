package com.weiwoju.queue.sunmiscanner;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.weiwoju.queue.sunmiscanner.view.ScannerView;

public class MainActivity extends AppCompatActivity {

    private TextView tvBarcode;
    private ImageView iv_scaning;
    private ScannerView scanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        iv_scaning = findViewById(R.id.iv_scaning);
        scanner = findViewById(R.id.scanner);
        tvBarcode = findViewById(R.id.tv_barcode);
        scanner.setCodeCaughtListener(new ScannerView.CodeCaughtListener() {
            @Override
            public void onCodeCaught(String barCode) {
                tvBarcode.setText("BarCode:" + barCode);
            }
        });
        //扫描动画
        iv_scaning.setAlpha(0.5f);
        Animation verticalAnimation = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, -0.3f, Animation.RELATIVE_TO_PARENT, 1.2f,
                Animation.RELATIVE_TO_PARENT, 0, Animation.RELATIVE_TO_PARENT, 0);
        verticalAnimation.setDuration(3500);
        verticalAnimation.setRepeatCount(-1);
        verticalAnimation.setRepeatMode(Animation.RESTART);
        iv_scaning.startAnimation(verticalAnimation);

    }


}
