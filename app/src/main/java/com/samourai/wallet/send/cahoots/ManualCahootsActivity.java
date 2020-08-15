package com.samourai.wallet.send.cahoots;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Display;
import android.view.Gravity;
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
import com.samourai.wallet.SamouraiActivity;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.cahoots.AndroidCahootsWallet;
import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.wallet.cahoots.CahootsService;
import com.samourai.wallet.cahoots.CahootsWallet;
import com.samourai.wallet.cahoots.STONEWALLx2;
import com.samourai.wallet.cahoots.psbt.PSBT;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.widgets.HorizontalStepsViewIndicator;
import com.samourai.wallet.widgets.ViewPager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import androidx.appcompat.app.AppCompatDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class ManualCahootsActivity extends SamouraiActivity {

    private ViewPager viewPager;
    private HorizontalStepsViewIndicator stepsViewGroup;
    private ArrayList<Fragment> steps = new ArrayList<>();
    private CahootReviewFragment cahootReviewFragment;
    private TextView stepCounts;
    private AppCompatDialog dialog;
    private long amount = 0L;
    private String address = "";
    private static final String TAG = "ManualCahootsActivity";
    private Cahoots payload;
    private int type;
    private CahootsService cahootsService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_cahoots);
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

        CahootsWallet cahootsWallet = new AndroidCahootsWallet(getApplicationContext());
        cahootsService = new CahootsService(SamouraiWallet.getInstance().getCurrentNetworkParams(), cahootsWallet);

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
            // continue cahoots
            String cahootsPayload = getIntent().getStringExtra("payload");
            try {
                onScanCahootsPayload(cahootsPayload);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        } else if (amount != 0L) {
            // start cahoots
            try {
                payload = cahootsService.startInitiator(type, amount, address, account);
                stepsViewGroup.post(() -> stepsViewGroup.setStep(1));
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
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
        getMenuInflater().inflate(R.menu.manual_cahoots_menu, menu);

        menu.findItem(R.id.action_menu_display_psbt).setVisible(false);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            finish();
        }

        if (menuItem.getItemId() == R.id.action_menu_paste_cahoots) {
            try {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                onScanCahootsPayload(item.getText().toString());
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Invalid data", Toast.LENGTH_SHORT).show();
            }
        }
        else if (menuItem.getItemId() == R.id.action_menu_display_psbt) {

            doDisplayPSBT();

        }
        else {
            ;
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

        try {
            // continue cahoots
            long feePerB = FeeUtil.getInstance().getSuggestedFeeDefaultPerB();
            payload = cahootsService.resume(qrData, feePerB, account);

            int step = payload.getStep();
            viewPager.post(() -> viewPager.setCurrentItem(step + 1, true));
            stepsViewGroup.post(() -> stepsViewGroup.setStep(step + 2));
            stepCounts.setText(String.valueOf((step + 2)).concat("/5"));

            if (payload.getType() == Cahoots.CAHOOTS_STONEWALLx2 && step == 3) {
                ((CahootsStepFragment) steps.get(step + 1)).setStowaway((STONEWALLx2) payload);
            }
            if (step == 4) {
                cahootReviewFragment.setCahoots(payload);
            }
        } catch(Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
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
                ManualCahootsActivity.this.startActivity(Intent.createChooser(intent, getApplicationContext().getText(R.string.send_tx)));
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

    private void doDisplayPSBT()    {

        try {
            PSBT psbt = payload.getPSBT();
            if(psbt == null)    {
                Toast.makeText(ManualCahootsActivity.this, R.string.psbt_error, Toast.LENGTH_SHORT).show();
            }

            String strPSBT = psbt.toString();

            final TextView tvHexTx = new TextView(ManualCahootsActivity.this);
            float scale = getResources().getDisplayMetrics().density;
            tvHexTx.setSingleLine(false);
            tvHexTx.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            tvHexTx.setLines(10);
            tvHexTx.setGravity(Gravity.START);
            tvHexTx.setText(strPSBT);
            tvHexTx.setPadding((int) (8 * scale + 0.5f), (int) (6 * scale + 0.5f), (int) (8 * scale + 0.5f), (int) (6 * scale + 0.5f));

            AlertDialog.Builder dlg = new AlertDialog.Builder(ManualCahootsActivity.this)
                    .setTitle(R.string.app_name)
                    .setView(tvHexTx)
                    .setCancelable(false)
                    .setPositiveButton(R.string.copy_to_clipboard, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.dismiss();
                            android.content.ClipboardManager clipboard = (android.content.ClipboardManager)getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                            android.content.ClipData clip = null;
                            clip = android.content.ClipData.newPlainText("tx", strPSBT);
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(ManualCahootsActivity.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.dismiss();
                        }
                    });
            if(!isFinishing())    {
                dlg.show();
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }

    }

}
