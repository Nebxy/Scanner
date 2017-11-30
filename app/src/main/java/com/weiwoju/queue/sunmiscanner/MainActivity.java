package com.weiwoju.queue.sunmiscanner;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.weiwoju.queue.sunmiscanner.view.ScannerView;

public class MainActivity extends AppCompatActivity  {

    private TextView tvBarcode;

    private ScannerView scanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {

        scanner = findViewById(R.id.scanner);
        tvBarcode = findViewById(R.id.tv_barcode);
        scanner.setCodeCaughtListener(new ScannerView.CodeCaughtListener() {
            @Override
            public void onCodeCaught(String barCode) {
                tvBarcode.setText("BarCode:"  +barCode);
            }
        });
        scanner.startScan();
    }




}
