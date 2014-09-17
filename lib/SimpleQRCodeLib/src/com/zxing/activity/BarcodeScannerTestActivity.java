package com.zxing.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import com.zxing.R;
import com.zxing.encoding.EncodingHandler;

public class BarcodeScannerTestActivity extends Activity {

    private static final int SCAN = 0;
    private TextView txtScanResult;
    private Button btnScan;
    private EditText edtBarcodeContent;
    private Button btnBarcode;
    private ImageView imgBarcode;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simple_qrcode_lib_main);

        initView();
        setListener();
    }

    private void initView() {
        // TODO Auto-generated method stub
        txtScanResult = (TextView) findViewById(R.id.txtScanResult);
        btnScan = (Button) findViewById(R.id.btnScan);
        edtBarcodeContent = (EditText) findViewById(R.id.edtBarcodeContent);
        btnBarcode = (Button) findViewById(R.id.btnBarcode);
        imgBarcode = (ImageView) findViewById(R.id.imgBarcode);
    }

    private void setListener() {
        // TODO Auto-generated method stub
        btnScan.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                //打开扫描界面扫描条形码或二维码
                Intent openCameraIntent = new Intent(BarcodeScannerTestActivity.this, CaptureActivity.class);
                startActivityForResult(openCameraIntent, SCAN);
            }
        });
        btnBarcode.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                //根据字符串生成二维码图片并显示在界面上，第二个参数为图片的大小（350*350）
                try {
                    String strBarcodeContent = edtBarcodeContent.getText().toString();
                    if (strBarcodeContent.equals("")) {
                        Toast.makeText(BarcodeScannerTestActivity.this, "Text can not be empty", Toast.LENGTH_SHORT).show();
                    } else {
                        Bitmap barcodeBitmap = EncodingHandler.createQRCode(strBarcodeContent, 350,null);
                        imgBarcode.setImageBitmap(barcodeBitmap);
                    }

                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            String strScanResult = data.getExtras().getString("scanResult");
            txtScanResult.setText(strScanResult);
        }
    }
}