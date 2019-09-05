package com.samourai.wallet.send.cahoots;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialog;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.samourai.wallet.R;
import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.wallet.cahoots.CahootsUtil;
import com.samourai.wallet.cahoots.STONEWALLx2;
import com.samourai.wallet.cahoots.Stowaway;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.widgets.HorizontalStepsViewIndicator;
import com.samourai.wallet.widgets.ViewPager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class ManualCahootsActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private HorizontalStepsViewIndicator stepsViewGroup;
    private ArrayList<Fragment> steps = new ArrayList<>();
    private CahootReviewFragment cahootReviewFragment;
    private TextView stepCounts;
    private AppCompatDialog dialog;
    private int account = 0;
    private long amount = 0L;
    private String address = "";
    private static final String TAG = "ManualCahootsActivity";
    private Cahoots payload;
    private int type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_stone_wall);
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        cahootReviewFragment = CahootReviewFragment.newInstance();
        createSteps();
        stepsViewGroup = findViewById(R.id.step_view);
        stepCounts = findViewById(R.id.step_numbers);
        viewPager = findViewById(R.id.view_flipper);
        viewPager.enableSwipe(false);
        stepsViewGroup.setTotalSteps(5);
        steps.add(cahootReviewFragment);
        viewPager.setAdapter(new StepAdapter(getSupportFragmentManager()));

        if (getIntent().hasExtra("account")) {
            account = getIntent().getIntExtra("account", 0);
        }
        if (getIntent().hasExtra("amount")) {
            amount = getIntent().getLongExtra("amount", 0);
        }
        if (getIntent().hasExtra("type")) {
            type = getIntent().getIntExtra("type", Cahoots.CAHOOTS_STOWAWAY);
        }
        if (getIntent().hasExtra("address")) {
            address = getIntent().getStringExtra("address");
        }
        if (getIntent().hasExtra("payload")) {

            String cahootsPayload = getIntent().getStringExtra("payload");

            if (Cahoots.isCahoots(cahootsPayload.trim())) {
                try {
                    JSONObject obj = new JSONObject(cahootsPayload);
                    if (obj.has("cahoots") && obj.getJSONObject("cahoots").has("type")) {
                        int type = obj.getJSONObject("cahoots").getInt("type");
                        if (type == Cahoots.CAHOOTS_STOWAWAY) {
                            payload = new Stowaway(obj);
                            onScanCahootsPayload(payload.toJSON().toString());
                        }
                        if (type == Cahoots.CAHOOTS_STONEWALLx2) {
                            payload = new STONEWALLx2(obj);
                            onScanCahootsPayload(payload.toJSON().toString());
                        }
                    }
                } catch (JSONException e) {
                    finish();
                    e.printStackTrace();
                }

            }
        } else if (amount != 0L) {
            if (type == Cahoots.CAHOOTS_STOWAWAY) {
                stepsViewGroup.post(() -> stepsViewGroup.setStep(1));
                payload = CahootsUtil.getInstance(ManualCahootsActivity.this).doStowaway0(amount, account);
                return;

            }
            if (type == Cahoots.CAHOOTS_STONEWALLx2) {
                stepsViewGroup.post(() -> stepsViewGroup.setStep(1));
                payload  = CahootsUtil.getInstance(ManualCahootsActivity.this).doSTONEWALLx2_0(amount, address, account);

            }
        } else {
            finish();
        }


    }
    @Override
    protected void onResume() {
        super.onResume();

        AppUtil.getInstance(this).setIsInForeground(true);

        AppUtil.getInstance(this).checkTimeOut();

    }

    private void createSteps() {
        for (int i = 0; i < 4; i++) {
            CahootsStepFragment stepView = CahootsStepFragment.newInstance(i);
            stepView.setCahootsFragmentListener(listener);
            steps.add(stepView);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.manual_stonewall_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            finish();
        }

        if(menuItem.getItemId() == R.id.action_menu_paste_cahoots){
            try {
                ClipboardManager clipboard = (ClipboardManager)  getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                onScanCahootsPayload(item.getText().toString());
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Invalid data", Toast.LENGTH_SHORT).show();
            }
        }

        return true;
    }

    private CahootsStepFragment.CahootsFragmentListener listener = new CahootsStepFragment.CahootsFragmentListener() {
        @Override
        public void onScan(int step, String qrData) {
            onScanCahootsPayload(qrData);
        }

        @Override
        public void onShare(int step) {
            shareCahootsPayload();
        }
    };

    private void onScanCahootsPayload(String qrData) {

        Stowaway stowaway = null;
        STONEWALLx2 stonewall = null;

        try {
            JSONObject obj = new JSONObject(qrData);
            Log.d("CahootsUtil", "incoming st:" + qrData);
            Log.d("CahootsUtil", "object json:" + obj.toString());
            if (obj.has("cahoots") && obj.getJSONObject("cahoots").has("type")) {

                int type = obj.getJSONObject("cahoots").getInt("type");

                if (type == Cahoots.CAHOOTS_STOWAWAY) {
                    stowaway = new Stowaway(obj);
                } else if (type == Cahoots.CAHOOTS_STONEWALLx2) {
                    stonewall = new STONEWALLx2(obj);
                } else {
                    Toast.makeText(getApplicationContext(), R.string.unrecognized_cahoots, Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                Toast.makeText(getApplicationContext(), R.string.not_cahoots, Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (JSONException je) {
            Toast.makeText(getApplicationContext(), R.string.cannot_process_cahoots, Toast.LENGTH_SHORT).show();
            return;
        }

        if (stowaway != null) {
            int step = stowaway.getStep();
            viewPager.post(() -> viewPager.setCurrentItem(step + 1, true));
            stepsViewGroup.post(() -> stepsViewGroup.setStep(step + 2));
            stepCounts.setText(String.valueOf((step + 2)).concat("/5"));

            try {
                switch (step) {
                    case 0:
                        stowaway.setFingerprintCollab(HD_WalletFactory.getInstance(getApplicationContext()).getFingerprint());
                        payload = CahootsUtil.getInstance(getApplicationContext()).doStowaway1(stowaway);
                        if(payload == null) {
                            Toast.makeText(this, R.string.cannot_compose_cahoots, Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 1:
                        payload = CahootsUtil.getInstance(getApplicationContext()).doStowaway2(stowaway);
                        if(payload == null) {
                            Toast.makeText(this, R.string.cannot_compose_cahoots, Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 2:
                        payload = CahootsUtil.getInstance(getApplicationContext()).doStowaway3(stowaway);
                        if(payload == null) {
                            Toast.makeText(this, R.string.cannot_compose_cahoots, Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 3:
                        payload = CahootsUtil.getInstance(getApplicationContext()).doStowaway4(stowaway);
                        if(payload == null) {
                            Toast.makeText(this, R.string.cannot_compose_cahoots, Toast.LENGTH_SHORT).show();
                        }
                        else    {
                            cahootReviewFragment.setCahoots(payload);
                        }
                        break;
                    default:
                        Toast.makeText(getApplicationContext(), R.string.unrecognized_step, Toast.LENGTH_SHORT).show();
                        break;
                }
            } catch (Exception e) {
//            Toast.makeText(getApplicationContext(), R.string.cannot_process_stonewall, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }

        if (stonewall != null) {
            int step = stonewall.getStep();
            viewPager.post(() -> viewPager.setCurrentItem(step + 1, true));
            stepsViewGroup.post(() -> stepsViewGroup.setStep(step + 2));
            stepCounts.setText(String.valueOf((step + 2)).concat("/5"));
            if(step == 2){
                ( (CahootsStepFragment) steps.get(step + 1) ).setStowaway(stonewall);
            }
            try {
                switch (step) {
                    case 0:
                        stonewall.setCounterpartyAccount(account);  // set counterparty account
                        stonewall.setFingerprintCollab(HD_WalletFactory.getInstance(getApplicationContext()).getFingerprint());
                        payload = CahootsUtil.getInstance(getApplicationContext()).doSTONEWALLx2_1(stonewall);
                        if(payload == null) {
                            Toast.makeText(this, R.string.cannot_compose_cahoots, Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 1:
                        payload = CahootsUtil.getInstance(getApplicationContext()).doSTONEWALLx2_2(stonewall);
                        if(payload == null) {
                            Toast.makeText(this, R.string.cannot_compose_cahoots, Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 2:
                        payload = CahootsUtil.getInstance(getApplicationContext()).doSTONEWALLx2_3(stonewall);
                        if(payload == null) {
                            Toast.makeText(this, R.string.cannot_compose_cahoots, Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 3:
                        payload = CahootsUtil.getInstance(getApplicationContext()).doSTONEWALLx2_4(stonewall);
                        if(payload == null) {
                            Toast.makeText(this, R.string.cannot_compose_cahoots, Toast.LENGTH_SHORT).show();
                        }
                        else    {
                            cahootReviewFragment.setCahoots(payload);
                        }
                        break;
                    default:
                        Toast.makeText(this, R.string.unrecognized_step, Toast.LENGTH_SHORT).show();
                        break;
                }
            } catch (Exception e) {
                Toast.makeText(this, R.string.cannot_process_stowaway, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                Log.d("CahootsUtil", e.getMessage());
            }
        }

    }

    private class StepAdapter extends FragmentPagerAdapter {


        StepAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return steps.get(position);
        }

        @Override
        public int getCount() {
            return steps.size();
        }
    }

    private void shareCahootsPayload() {

        String strCahoots = this.payload.toJSON().toString();
        final int QR_ALPHANUM_CHAR_LIMIT = 4296;    // tx max size in bytes == 2148
        dialog = new AppCompatDialog(this, R.style.stowaway_dialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.cahoots_qr_dialog_layout);
        ImageView qrCode = dialog.findViewById(R.id.qr_code_imageview);
        Button copy = dialog.findViewById(R.id.cahoots_copy_btn);
        Button share = dialog.findViewById(R.id.cahoots_share);
        TextView qrErrorMessage = dialog.findViewById(R.id.qr_error_stowaway);

        if (strCahoots.length() <= QR_ALPHANUM_CHAR_LIMIT) {
            Display display = this.getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int imgWidth = Math.max(size.x - 240, 150);

            Bitmap bitmap = null;

            QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(strCahoots, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), imgWidth);

            try {
                bitmap = qrCodeEncoder.encodeAsBitmap();
            } catch (WriterException e) {
                qrErrorMessage.setVisibility(View.VISIBLE);
                e.printStackTrace();
            }
            qrCode.setImageBitmap(bitmap);
        } else {
            qrErrorMessage.setVisibility(View.VISIBLE);
            Toast.makeText(getApplicationContext(), R.string.tx_too_large_qr, Toast.LENGTH_SHORT).show();
        }
        share.setOnClickListener(v -> {
            if (!(strCahoots.length() <= QR_ALPHANUM_CHAR_LIMIT)) {
                Intent txtIntent = new Intent(android.content.Intent.ACTION_SEND);
                txtIntent.setType("text/plain");
                txtIntent.putExtra(android.content.Intent.EXTRA_TEXT, strCahoots);
                startActivity(Intent.createChooser(txtIntent, "Share"));
                return;
            }
            String strFileName = AppUtil.getInstance(getApplicationContext()).getReceiveQRFilename();
            File file = new File(strFileName);
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
            file.setReadable(true, false);

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
            } catch (FileNotFoundException fnfe) {
            }

            if (file != null && fos != null) {
                Bitmap bitmap1 = ((BitmapDrawable) qrCode.getDrawable()).getBitmap();
                bitmap1.compress(Bitmap.CompressFormat.PNG, 0, fos);

                try {
                    fos.close();
                } catch (IOException ioe) {
                    ;
                }

                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.setType("image/png");
                if (android.os.Build.VERSION.SDK_INT >= 24) {
                    //From API 24 sending FIle on intent ,require custom file provider
                    intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                            getApplicationContext(),
                            getApplicationContext().getApplicationContext()
                                    .getPackageName() + ".provider", file));
                } else {
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                }
                getApplicationContext().startActivity(Intent.createChooser(intent, getApplicationContext().getText(R.string.send_tx)));
            }

        });

        copy.setOnClickListener(v -> {

            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getApplicationContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = null;
            clip = android.content.ClipData.newPlainText("Cahoots", strCahoots);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getApplicationContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();

        });

        dialog.show();


    }

}
