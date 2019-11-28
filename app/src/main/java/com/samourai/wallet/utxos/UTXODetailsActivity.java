package com.samourai.wallet.utxos;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.BottomSheetDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.send.BlockedUTXO;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.MessageSignUtil;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UTXODetailsActivity extends AppCompatActivity {
    final DecimalFormat df = new DecimalFormat("#");
    private String hash, addr;
    private TextView addressTextView, amountTextView, statusTextView, notesTextView, hashTextView;
    private int account = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_utxodetails);

        setSupportActionBar(findViewById(R.id.toolbar_utxo_activity));

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        addressTextView = findViewById(R.id.utxo_details_address);
        amountTextView = findViewById(R.id.utxo_details_amount);
        statusTextView = findViewById(R.id.utxo_details_spendable_status);
        hashTextView = findViewById(R.id.utxo_details_hash);

        df.setMinimumIntegerDigits(1);
        df.setMinimumFractionDigits(8);
        df.setMaximumFractionDigits(8);

        if (getIntent().getExtras() != null && getIntent().getExtras().containsKey("hash")) {
            hash = getIntent().getExtras().getString("hash");
        } else {
            finish();
        }

        if (getIntent().getExtras() != null && getIntent().getExtras().containsKey("account")) {
            account = getIntent().getExtras().getInt("account");
        } else {
            finish();
        }
        List<UTXO> utxos = new ArrayList<>();
        if (account == WhirlpoolMeta.getInstance(getApplicationContext()).getWhirlpoolPostmix()) {
            utxos.addAll(APIFactory.getInstance(getApplicationContext()).getUtxosPostMix(false));

        } else {
            utxos.addAll(APIFactory.getInstance(getApplicationContext()).getUtxos(false));

        }
        utxos.addAll(APIFactory.getInstance(getApplicationContext()).getUtxos(false));
        for (UTXO utxo : utxos) {
            for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                if (outpoint.getTxHash() != null && outpoint.getTxHash().toString().equals(hash)) {
                    addressTextView.setText(outpoint.getAddress());
                    amountTextView.setText(df.format(((double) (outpoint.getValue().longValue()) / 1e8)) + " BTC");
                    addr = outpoint.getAddress();
                    hashTextView.setText(outpoint.getTxHash().toString());
                    if (BlockedUTXO.getInstance().contains(outpoint.getTxHash().toString(), outpoint.getTxOutputN())) {
                        statusTextView.setText("Blocked");
                    } else {
                        statusTextView.setText(getText(R.string.spendable));
                    }
                }
            }

        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.utxo_details_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        if (item.getItemId() == R.id.utxo_details_add_to_whirlpool) {
//TODO:
        }
        if (item.getItemId() == R.id.utxo_details_menu_action_more_options) {
            showMoreOptions();
        }
        if (item.getItemId() == R.id.utxo_details_view_in_explorer) {
            viewInExplorer();
        }

        return super.onOptionsItemSelected(item);
    }

    private void showMoreOptions() {
        View dialogView = getLayoutInflater().inflate(R.layout.utxo_details_options_bottomsheet, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(dialogView);
        dialog.show();


        dialog.findViewById(R.id.utxo_details_option_sign).setOnClickListener(view -> sign());
        dialog.findViewById(R.id.utxo_details_option_redeem).setOnClickListener(view -> redeem());
        dialog.findViewById(R.id.utxo_details_option_private_key).setOnClickListener(view -> viewPrivateKey());

        //TODO
        dialog.findViewById(R.id.utxo_details_option_status).setOnClickListener(view -> viewPrivateKey());
        dialog.findViewById(R.id.utxo_details_option_spend).setOnClickListener(view -> viewPrivateKey());

    }

    private void sign() {
        ECKey ecKey = SendFactory.getPrivKey(addr, account);
        String msg = null;

        if (FormatsUtil.getInstance().isValidBech32(addr) || Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), addr).isP2SHAddress()) {

            msg = getString(R.string.utxo_sign_text3);

            try {
                JSONObject obj = new JSONObject();
                obj.put("pubkey", ecKey.getPublicKeyAsHex());
                obj.put("address", addr);
                msg += " " + obj.toString();
            } catch (JSONException je) {
                msg += ":";
                msg += addr;
                msg += ", ";
                msg += "pubkey:";
                msg += ecKey.getPublicKeyAsHex();
            }

        } else {

            msg = getString(R.string.utxo_sign_text2);

        }

        if (ecKey != null) {
            MessageSignUtil.getInstance(this).doSign(this.getString(R.string.utxo_sign),
                    this.getString(R.string.utxo_sign_text1),
                    msg,
                    ecKey);
        }
    }

    private void viewPrivateKey() {
        ECKey ecKey = SendFactory.getPrivKey(addr, account);
        String strPrivKey = ecKey.getPrivateKeyAsWiF(SamouraiWallet.getInstance().getCurrentNetworkParams());

        ImageView showQR = new ImageView(this);
        Bitmap bitmap = null;
        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(strPrivKey, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), 500);
        try {
            bitmap = qrCodeEncoder.encodeAsBitmap();
        } catch (WriterException e) {
            e.printStackTrace();
        }
        showQR.setImageBitmap(bitmap);

        TextView showText = new TextView(this);
        showText.setText(strPrivKey);
        showText.setTextIsSelectable(true);
        showText.setPadding(40, 10, 40, 10);
        showText.setTextSize(18.0f);

        LinearLayout privkeyLayout = new LinearLayout(this);
        privkeyLayout.setOrientation(LinearLayout.VERTICAL);
        privkeyLayout.addView(showQR);
        privkeyLayout.addView(showText);

        new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setView(privkeyLayout)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialog, whichButton) -> {
                }).show();
    }

    private void redeem() {
        ECKey ecKey = SendFactory.getPrivKey(addr, account);
        SegwitAddress segwitAddress = new SegwitAddress(ecKey.getPubKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());

        if (ecKey != null && segwitAddress != null) {

            String redeemScript = Hex.toHexString(segwitAddress.segWitRedeemScript().getProgram());

            TextView showText = new TextView(this);
            showText.setText(redeemScript);
            showText.setTextIsSelectable(true);
            showText.setPadding(40, 10, 40, 10);
            showText.setTextSize(18.0f);

            new AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setView(showText)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, (dialog, whichButton) -> {
                    })
                    .show();
        }
    }

//    private void doNotSpend(int position) {
//
//        if (data.get(position).amount < BlockedUTXO.BLOCKED_UTXO_THRESHOLD && BlockedUTXO.getInstance().contains(data.get(position).hash, data.get(position).idx)) {
//
//            AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            builder.setTitle(R.string.dusting_tx);
//            builder.setMessage(R.string.dusting_tx_unblock);
//            builder.setCancelable(true);
//            builder.setPositiveButton(R.string.no, (dialog, whichButton) -> {
//                ;
//            });
//            builder.setNegativeButton(R.string.yes, (dialog, whichButton) -> {
//
//                BlockedUTXO.getInstance().remove(data.get(position).hash, data.get(position).idx);
//                BlockedUTXO.getInstance().addNotDusted(data.get(position).hash, data.get(position).idx);
//                adapter.notifyItemChanged(position);
//
//                //to recalculate amounts
//                loadUTXOs(true);
//
//            });
//
//            AlertDialog alert = builder.create();
//            alert.show();
//
//        } else if (BlockedUTXO.getInstance().contains(data.get(position).hash, data.get(position).idx)) {
//
//            AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            builder.setTitle(R.string.mark_spend);
//            builder.setMessage(R.string.mark_utxo_spend);
//            builder.setCancelable(true);
//            builder.setPositiveButton(R.string.yes, (dialog, whichButton) -> {
//
//                BlockedUTXO.getInstance().remove(data.get(position).hash, data.get(position).idx);
//
//                adapter.notifyItemChanged(position);
//
//                //to recalculate amounts
//                loadUTXOs(true);
//
//
//            });
//            builder.setNegativeButton(R.string.no, (dialog, whichButton) -> {
//                ;
//            });
//
//            AlertDialog alert = builder.create();
//            alert.show();
//
//        } else if (BlockedUTXO.getInstance().containsPostMix(data.get(position).hash, data.get(position).idx)) {
//
//            AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            builder.setTitle(R.string.mark_spend);
//            builder.setMessage(R.string.mark_utxo_spend);
//            builder.setCancelable(true);
//            builder.setPositiveButton(R.string.yes, (dialog, whichButton) -> {
//
//                BlockedUTXO.getInstance().removePostMix(data.get(position).hash, data.get(position).idx);
//
//                Log.d("UTXOActivity", "removed:" + data.get(position).hash + "-" + data.get(position).idx);
//
//                adapter.notifyItemChanged(position);
//
//                //to recalculate amounts
//                loadUTXOs(true);
//
//
//            });
//            builder.setNegativeButton(R.string.no, (dialog, whichButton) -> {
//                ;
//            });
//
//            AlertDialog alert = builder.create();
//            alert.show();
//
//        } else {
//
//            AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            builder.setTitle(R.string.mark_do_not_spend);
//            builder.setMessage(R.string.mark_utxo_do_not_spend);
//            builder.setCancelable(true);
//            builder.setPositiveButton(R.string.yes, (dialog, whichButton) -> {
//
//                if (account == 0) {
//                    BlockedUTXO.getInstance().add(data.get(position).hash, data.get(position).idx, data.get(position).amount);
//                } else {
//                    BlockedUTXO.getInstance().addPostMix(data.get(position).hash, data.get(position).idx, data.get(position).amount);
//                }
//
//                Log.d("UTXOActivity", "added:" + data.get(position).hash + "-" + data.get(position).idx);
//
//                adapter.notifyItemChanged(position);
//
//                //to recalculate amounts
//                loadUTXOs(true);
//
//
//            });
//            builder.setNegativeButton(R.string.no, (dialog, whichButton) -> {
//                ;
//            });
//
//            AlertDialog alert = builder.create();
//            alert.show();
//
//        }
//        utxoChange = true;
//
//    }
//
//    private void tagItem(int position) {
//
//        final EditText edTag = new EditText(this);
//        edTag.setSingleLine(true);
//        if (UTXOUtil.getInstance().get(data.get(position).hash + "-" + data.get(position).idx) != null) {
//            edTag.setText(UTXOUtil.getInstance().get(data.get(position).hash + "-" + data.get(position).idx));
//        }
//
//        AlertDialog.Builder dlg = new AlertDialog.Builder(this)
//                .setTitle(R.string.app_name)
//                .setView(edTag)
//                .setMessage(R.string.label)
//                .setCancelable(false)
//                .setPositiveButton(R.string.ok, (dialog, whichButton) -> {
//
//                    final String strTag = edTag.getText().toString().trim();
//
//                    if (strTag != null && strTag.length() > 0) {
//                        UTXOUtil.getInstance().add(data.get(position).hash + "-" + data.get(position).idx, strTag);
//                    } else {
//                        UTXOUtil.getInstance().remove(data.get(position).hash + "-" + data.get(position).idx);
//                    }
//
////
//                    adapter.notifyItemChanged(position);
//
//                }).setNegativeButton(R.string.cancel, (dialog, whichButton) -> {
//                    ;
//                });
//        if (!isFinishing()) {
//            dlg.show();
//        }
//
//    }

    private void viewInExplorer() {
        String blockExplorer = "https://m.oxt.me/transaction/";
        if (SamouraiWallet.getInstance().isTestNet()) {
            blockExplorer = "https://blockstream.info/testnet/";
        }
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(blockExplorer + hash));
        startActivity(browserIntent);
    }

}
