package com.samourai.wallet.send.cahoots;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.samourai.wallet.R;
import com.samourai.wallet.fragments.CameraFragmentBottomSheet;
import com.samourai.wallet.widgets.EntropyBar;
import com.samourai.wallet.widgets.StepCircleView;

public class ManualStoneWall extends AppCompatActivity {
    private ImageView qrCode;
    private ViewFlipper viewFlipper;
    private EntropyBar entropyBar;
    private Button broadCastBtn;
    private TextView instructionsTxt;
    private StepCircleView.StepCircleViewGroup stepsViewGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_stone_wall);
        setSupportActionBar(findViewById(R.id.toolbar));

        stepsViewGroup = findViewById(R.id.cahoot_step_group);
        qrCode = findViewById(R.id.qr_code_imageview);
        viewFlipper = findViewById(R.id.view_flipper);
        broadCastBtn = findViewById(R.id.manual_stonewall_broadcast);
        entropyBar = findViewById(R.id.manual_stonewall_entropy);
        instructionsTxt = findViewById(R.id.stonewall_instruction);

        qrCode.setImageBitmap(generateQRCode("PlaceHolder : A modern bitcoin wallet hand forged to keep your transactions private your identity masked and your funds secured."));
        entropyBar.setMaxBars(4);
        entropyBar.setRange(2);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        new Handler().postDelayed(() -> {
            stepsViewGroup.setStep(1);
        }, 1000);

        new Handler().postDelayed(() -> {
            stepsViewGroup.setStep(2);
        }, 3000);

        new Handler().postDelayed(this::showReview, 4000);

    }


    private void showReview() {
        stepsViewGroup.setStep(5);
        viewFlipper.showNext();
        broadCastBtn.setVisibility(View.VISIBLE);
        instructionsTxt.setText(getString(R.string.review_tx_before_braodcast));
    }

    private Bitmap generateQRCode(String uri) {

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int imgWidth = size.x - 130;


        Bitmap bitmap = null;

        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(uri, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), imgWidth);

        try {
            bitmap = qrCodeEncoder.encodeAsBitmap();
        } catch (WriterException e) {
            e.printStackTrace();
        }

        return bitmap;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.manual_stonewall_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        switch (item.getItemId()) {
            case R.id.scan_qr_manual_stonewall: {

                CameraFragmentBottomSheet cameraFragmentBottomSheet = new CameraFragmentBottomSheet();
                cameraFragmentBottomSheet.show(getSupportFragmentManager(), cameraFragmentBottomSheet.getTag());
                cameraFragmentBottomSheet.setQrCodeScanLisenter(code -> {
                    cameraFragmentBottomSheet.dismissAllowingStateLoss();
                    processScan(code);
                });
            }
        }
        return true;
    }

    private void processScan(String code) {
        Toast.makeText(getApplicationContext(), code, Toast.LENGTH_SHORT).show();
    }

}
