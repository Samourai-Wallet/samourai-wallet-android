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
import com.samourai.wallet.cahoots.AndroidSorobanClientService;
import com.samourai.wallet.cahoots.CahootsMessage;
import com.samourai.wallet.cahoots.CahootsMode;
import com.samourai.wallet.cahoots.CahootsService;
import com.samourai.wallet.cahoots.CahootsType;
import com.samourai.wallet.cahoots.CahootsTypeUser;
import com.samourai.wallet.cahoots.psbt.PSBT;
import com.samourai.wallet.util.AppUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import androidx.appcompat.app.AppCompatDialog;
import androidx.core.content.FileProvider;

public class ManualCahootsActivity extends SamouraiActivity {
    private static final String TAG = "ManualCahootsActivity";

    private CahootsUi cahootsUi;

    private AppCompatDialog dialog;

    public static Intent createIntentResume(Context ctx, int account, String payload) throws Exception {
        CahootsService cahootsService = AndroidSorobanClientService.getInstance(ctx).getCahootsService();
        CahootsMessage msg = cahootsService.parse(payload);
        CahootsTypeUser typeUser = msg.getTypeUser().getPartner();
        Intent intent = CahootsUi.createIntent(ctx, ManualCahootsActivity.class, account, msg.getType(), typeUser);
        intent.putExtra("payload", payload);
        return intent;
    }

    public static Intent createIntentSender(Context ctx, int account, CahootsType type, long amount, String address) {
        Intent intent = CahootsUi.createIntent(ctx, ManualCahootsActivity.class, account, type, CahootsTypeUser.SENDER);
        intent.putExtra("sendAmount", amount);
        intent.putExtra("sendAddress", address);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_cahoots);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        try {
            ManualCahootsStepFragment.CahootsFragmentListener listener = new ManualCahootsStepFragment.CahootsFragmentListener() {
                @Override
                public void onScan(int step, String qrData) {
                    onScanCahootsPayload(qrData);
                }

                @Override
                public void onShare(int step) {
                    try {
                        shareCahootsPayload();
                    } catch(Exception e) {
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
            };
            cahootsUi = new CahootsUi(
                    findViewById(R.id.step_view),
                    findViewById(R.id.step_numbers),
                    findViewById(R.id.view_flipper),
                    getIntent(),
                    getSupportFragmentManager(),
                    i -> ManualCahootsStepFragment.newInstance(i, listener),
                    getApplicationContext()
            );
            this.account = cahootsUi.getAccount();
            setTitle(cahootsUi.getTitle(CahootsMode.MANUAL));

            if (getIntent().hasExtra("payload")) {
                // resume cahoots
                String cahootsPayload = getIntent().getStringExtra("payload");
                onScanCahootsPayload(cahootsPayload);
            } else {
                // start cahoots
                startSender();
            }
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    private void startSender() throws Exception {
        // send cahoots
        long sendAmount = getIntent().getLongExtra("sendAmount", 0);
        if (sendAmount <=0) {
            throw new Exception("Invalid sendAmount");
        }
        String sendAddress = getIntent().getStringExtra("sendAddress");

        CahootsService cahootsService = cahootsUi.getCahootsService();
        switch (cahootsUi.getCahootsType()) {
            case STONEWALLX2:
                cahootsUi.setCahootsMessage(cahootsService.newStonewallx2(account, sendAmount, sendAddress));
                break;
            case STOWAWAY:
                cahootsUi.setCahootsMessage(cahootsService.newStowaway(account, sendAmount));
                break;
            default:
                throw new Exception("Unknown #Cahoots");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppUtil.getInstance(this).setIsInForeground(true);
        AppUtil.getInstance(this).checkTimeOut();
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
        return true;
    }

    private void onScanCahootsPayload(String qrData) {
        try {
            // continue cahoots
            CahootsService cahootsService = cahootsUi.getCahootsService();
            cahootsUi.setCahootsMessage(cahootsService.reply(account, qrData));
        } catch(Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            finish();
        }
    }

    private void shareCahootsPayload() throws Exception {
        String strCahoots = this.cahootsUi.getCahootsMessage().toPayload();
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
            int imgWidth = Math.max(size.x - 20, 150);

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
            PSBT psbt = cahootsUi.getCahootsMessage().getCahoots().getPSBT();
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
