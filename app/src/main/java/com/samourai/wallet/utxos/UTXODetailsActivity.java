package com.samourai.wallet.utxos;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiActivity;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.paynym.WebUtil;
import com.samourai.wallet.crypto.DecryptionException;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.paynym.paynymDetails.PayNymDetailsActivity;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.send.BlockedUTXO;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.SendActivity;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.LogUtil;
import com.samourai.wallet.util.MessageSignUtil;
import com.samourai.wallet.utxos.models.UTXOCoin;
import com.samourai.wallet.whirlpool.WhirlpoolMain;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.MixableStatus;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.squareup.picasso.Picasso;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.MnemonicException;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class UTXODetailsActivity extends SamouraiActivity {
    final DecimalFormat df = new DecimalFormat("#");
    private String hash, addr, hashIdx;
    private TextView addressTextView, amountTextView, statusTextView, notesTextView, hashTextView;
    private int account = 0;
    private EditText noteEditText;
    private LinearLayout paynymLayout;
    private ImageView deleteButton;
    private TextView addNote;
    private static final String TAG = "UTXODetailsActivity";
    private int idx;
    private long amount;
    private UTXOCoin utxoCoin;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Switch themes based on accounts (blue theme for whirlpool account)
        setSwitchThemes(true);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_utxodetails);

        setSupportActionBar(findViewById(R.id.toolbar_utxo_activity));

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        addressTextView = findViewById(R.id.utxo_details_address);
        amountTextView = findViewById(R.id.utxo_details_amount);
        statusTextView = findViewById(R.id.utxo_details_spendable_status);
        hashTextView = findViewById(R.id.utxo_details_hash);
        addNote = findViewById(R.id.add_note_button);
        notesTextView = findViewById(R.id.utxo_details_note);
        deleteButton = findViewById(R.id.delete_note);
        paynymLayout = findViewById(R.id.utxo_details_paynym_container);
        paynymLayout.setVisibility(View.GONE);

        df.setMinimumIntegerDigits(1);
        df.setMinimumFractionDigits(8);
        df.setMaximumFractionDigits(8);

        if (getIntent().getExtras() != null && getIntent().getExtras().containsKey("hashIdx")) {
            hashIdx = getIntent().getExtras().getString("hashIdx");
        } else {
            finish();
        }


        if (getIntent().getExtras() != null && getIntent().getExtras().containsKey("_account")) {
            account = getIntent().getExtras().getInt("_account");
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
                if (outpoint.getTxHash() != null) {
                    String hashWithIdx = outpoint.getTxHash().toString().concat("-").concat(String.valueOf(outpoint.getTxOutputN()));
                    if (hashWithIdx.equals(hashIdx)) {
                        idx = outpoint.getTxOutputN();
                        amount = outpoint.getValue().longValue();
                        hash = outpoint.getTxHash().toString();
                        addr = outpoint.getAddress();
                        utxoCoin = new UTXOCoin(outpoint, utxo);
                        if (BlockedUTXO.getInstance().contains(outpoint.getTxHash().toString(), outpoint.getTxOutputN())) {
                            utxoCoin.doNotSpend = true;
                        }
                        setUTXOState();
                    }
                }

            }

        }
        deleteButton.setOnClickListener(view -> {
            if (UTXOUtil.getInstance().getNote(hash) != null) {
                UTXOUtil.getInstance().removeNote(hash);
            }
            setNoteState();
            saveWalletState();
        });
        addNote.setOnClickListener(view -> {
            View dialogView = getLayoutInflater().inflate(R.layout.bottom_sheet_note, null);
            BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.bottom_sheet_note);
            dialog.setContentView(dialogView);
            dialog.show();
            Button submitButton = dialog.findViewById(R.id.submit_note);

            if (UTXOUtil.getInstance().getNote(hash) != null) {
                ((EditText) dialog.findViewById(R.id.utxo_details_note)).setText(UTXOUtil.getInstance().getNote(hash));
                submitButton.setText("Save");
            } else {
                submitButton.setText("Add");
            }

            dialog.findViewById(R.id.submit_note).setOnClickListener((View view1) -> {
                dialog.dismiss();
                addNote(((EditText) dialog.findViewById(R.id.utxo_details_note)).getText().toString());
            });
        });

        setNoteState();

        addressTextView.setOnClickListener((event) -> new MaterialAlertDialogBuilder(UTXODetailsActivity.this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.receive_address_to_clipboard)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, (dialog, whichButton) -> {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) UTXODetailsActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = null;
                    clip = android.content.ClipData.newPlainText("address", addr);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(UTXODetailsActivity.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                }).setNegativeButton(R.string.no, (dialog, whichButton) -> {
                }).show());

        hashTextView.setOnClickListener(view -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.txid_to_clipboard)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes, (dialog, whichButton) -> {
                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) UTXODetailsActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip;
                        clip = android.content.ClipData.newPlainText("tx id", hash);
                        if (clipboard != null) {
                            clipboard.setPrimaryClip(clip);
                        }
                        Toast.makeText(UTXODetailsActivity.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                    }).setNegativeButton(R.string.no, (dialog, whichButton) -> {
            }).show();
        });

    }

    void setUTXOState() {
        if (isBlocked()) {
            statusTextView.setText("Blocked");
        } else {
            statusTextView.setText(getText(R.string.spendable));
        }
        addressTextView.setText(addr);
        hashTextView.setText(hash);
        amountTextView.setText(df.format(((double) (amount) / 1e8)) + " BTC");

        if (isBIP47(addr)) {
            paynymLayout.setVisibility(View.VISIBLE);
            String pcode = BIP47Meta.getInstance().getPCode4AddrLookup().get(addr);
            ImageView avatar = paynymLayout.findViewById(R.id.paynym_avatar);
            paynymLayout.findViewById(R.id.paynym_list_container).setOnClickListener(view -> {
                ActivityOptionsCompat options = ActivityOptionsCompat.
                        makeSceneTransitionAnimation(this, avatar, "profile");


                startActivity(new Intent(this, PayNymDetailsActivity.class).putExtra("pcode", pcode), options.toBundle());
            });
            if (pcode != null && pcode.length() > 0) {

                ((TextView) paynymLayout.findViewById(R.id.paynym_code)).setText(BIP47Meta.getInstance().getDisplayLabel(pcode));

                Picasso.get().load(WebUtil.PAYNYM_API + pcode + "/avatar")
                        .into(avatar);

            } else {
                ((TextView) paynymLayout.findViewById(R.id.paynym_code)).setText(getText(R.string.paycode).toString());
            }
        }
    }

    boolean isBlocked() {
        return BlockedUTXO.getInstance().contains(hash, idx) ||
                BlockedUTXO.getInstance().containsPostMix(hash, idx) ||
                BlockedUTXO.getInstance().containsBadBank(hash, idx);
    }

    void setNoteState() {
        TransitionManager.beginDelayedTransition((ViewGroup) notesTextView.getRootView());
        if (UTXOUtil.getInstance().getNote(hash) == null) {
            notesTextView.setVisibility(View.GONE);
            addNote.setText("Add");
            deleteButton.setVisibility(View.GONE);
        } else {
            notesTextView.setVisibility(View.VISIBLE);
            notesTextView.setText(UTXOUtil.getInstance().getNote(hash));
            deleteButton.setVisibility(View.VISIBLE);
            addNote.setText("Edit");
        }
    }

    void setSpendStatus() {

        final String[] export_methods = new String[2];
        export_methods[0] = "Spendable";
        export_methods[1] = "Do not spend";


        int selected = 0;
        if (isBlocked()) {
            selected = 1;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Set status")
                .setSingleChoiceItems(export_methods, selected, (dialog, which) -> {

                            if (which == 0) {
                                if (amount < BlockedUTXO.BLOCKED_UTXO_THRESHOLD && BlockedUTXO.getInstance().contains(hash, idx)) {
                                    BlockedUTXO.getInstance().remove(hash, idx);
                                    BlockedUTXO.getInstance().addNotDusted(hash, idx);

                                } else if (BlockedUTXO.getInstance().contains(hash, idx)) {

                                    BlockedUTXO.getInstance().remove(hash, idx);

                                } else if (BlockedUTXO.getInstance().containsPostMix(hash, idx)) {

                                    BlockedUTXO.getInstance().removePostMix(hash, idx);

                                }
                                utxoCoin.doNotSpend = false;

                            } else {
                                if (amount < BlockedUTXO.BLOCKED_UTXO_THRESHOLD && BlockedUTXO.getInstance().contains(hash, idx)) {

                                    //No-op


                                } else if (BlockedUTXO.getInstance().contains(hash, idx)) {
                                    //No-op

                                } else if (BlockedUTXO.getInstance().containsPostMix(hash, idx)) {

                                    //No-op
                                } else {

                                    if (account == 0) {
                                        BlockedUTXO.getInstance().add(hash, idx, amount);
                                    } else {
                                        BlockedUTXO.getInstance().addPostMix(hash, idx, amount);
                                    }
                                    utxoCoin.doNotSpend = true;
                                    LogUtil.debug("UTXOActivity", "added:" + hash + "-" + idx);

                                }
                            }
                            setUTXOState();
                            dialog.dismiss();
                            saveWalletState();
                        }
                ).

                show();

    }

    void addNote(String text) {
        if(text != null && text.length() > 0) {
            UTXOUtil.getInstance().addNote(hash, text);
        }
        else {
            UTXOUtil.getInstance().removeNote(hash);
        }
        setNoteState();
        saveWalletState();
    }

    private boolean isBIP47(String address) {

        try {
            String path = APIFactory.getInstance(getApplicationContext()).getUnspentPaths().get(address);
            if (path != null) {
                return false;
            } else {
                String pcode = BIP47Meta.getInstance().getPCode4Addr(address);
                int idx = BIP47Meta.getInstance().getIdx4Addr(address);
                List<Integer> unspentIdxs = BIP47Meta.getInstance().getUnspent(pcode);

                return unspentIdxs != null && unspentIdxs.contains(Integer.valueOf(idx));

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.dispose();
        super.onDestroy();
        setResult(RESULT_OK, new Intent());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.utxo_details_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.utxo_details_add_to_whirlpool) {

            sendUTXOtoWhirlpool();
        }
        if (item.getItemId() == R.id.utxo_details_menu_action_more_options) {
            showMoreOptions();
        }
        if (item.getItemId() == R.id.utxo_details_view_in_explorer) {
            viewInExplorer();
        }

        return super.onOptionsItemSelected(item);
    }

    private void sendUTXOtoWhirlpool() {
        // get mixable WhirlpoolUtxo when available
        WhirlpoolWallet whirlpoolWallet = AndroidWhirlpoolWalletService.getInstance().getWhirlpoolWalletOrNull();
        final WhirlpoolUtxo mixableUtxo = (whirlpoolWallet != null ? getWhirlpoolUtxoWhenMixable(whirlpoolWallet) : null);

        new MaterialAlertDialogBuilder(this)
                .setMessage(mixableUtxo!=null ? "Mix now?" : "Send utxo to Whirlpool?")
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialog, whichButton) -> {
                    if (mixableUtxo != null) {
                        // premix/postmix ready to mix => start mixing
                        new Thread(() -> {
                            try {
                                whirlpoolWallet.mix(mixableUtxo);
                                runOnUiThread(() -> Toast.makeText(UTXODetailsActivity.this, "Mixing...", Toast.LENGTH_SHORT).show());
                            } catch(Exception e) {
                                runOnUiThread(() -> Toast.makeText(UTXODetailsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show());
                            }
                        }).start();
                    } else {
                        // new TX0 from this UTXO
                        ArrayList<UTXOCoin> list = new ArrayList<>();
                        list.add(utxoCoin);

                        String id = UUID.randomUUID().toString();
                        PreSelectUtil.getInstance().clear();
                        PreSelectUtil.getInstance().add(id, list);
                        if (account == WhirlpoolMeta.getInstance(getApplication()).getWhirlpoolPostmix()) {
                            if (!utxoCoin.path.startsWith("M/1/")) {
                                Snackbar.make(paynymLayout.getRootView(), R.string.only_change_utxos_allowed, Snackbar.LENGTH_LONG).show();
                                return;
                            }
                        }

                        if (id != null) {
                            Intent intent = new Intent(getApplicationContext(), WhirlpoolMain.class);
                            intent.putExtra("preselected", id);
                            intent.putExtra("_account", account);
                            startActivity(intent);
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {

                })
                .show();


    }

    private WhirlpoolUtxo getWhirlpoolUtxoWhenMixable(WhirlpoolWallet whirlpoolWallet){
        WhirlpoolUtxo whirlpoolUtxo = whirlpoolWallet.getUtxoSupplier().findUtxo(hash, idx);
        if (whirlpoolUtxo != null) {
            // premix/postmix?
            if (WhirlpoolAccount.PREMIX.equals(whirlpoolUtxo.getAccount()) || WhirlpoolAccount.POSTMIX.equals(whirlpoolUtxo.getAccount())) {
                // ready to mix?
                if (MixableStatus.MIXABLE.equals(whirlpoolUtxo.getUtxoState().getMixableStatus())) {
                    return whirlpoolUtxo;
                }
            }
        }
        // not mixable
        return null;
    }

    private void showMoreOptions() {

        View dialogView = getLayoutInflater().inflate(R.layout.utxo_details_options_bottomsheet, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(dialogView);
        dialog.show();
        TextView spendOption = dialog.findViewById(R.id.utxo_details_spending_status);


        if (isBlocked()) {
            if (spendOption != null)
                spendOption.setText(R.string.this_utxo_is_marked_as_blocked);
        } else {
            if (spendOption != null)
                spendOption.setText(R.string.this_utxo_is_marked_as_spendable);
        }

        dialog.findViewById(R.id.utxo_details_option_sign).setOnClickListener(view -> sign());
        dialog.findViewById(R.id.utxo_details_option_redeem).setOnClickListener(view -> redeem());
        dialog.findViewById(R.id.utxo_details_option_private_key).setOnClickListener(view -> viewPrivateKey());

        dialog.findViewById(R.id.utxo_details_option_status).setOnClickListener(view -> {
            setSpendStatus();
            dialog.dismiss();
        });
        dialog.findViewById(R.id.utxo_details_option_spend)
                .setOnClickListener(view -> {
                    dialog.dismiss();
                    ArrayList<UTXOCoin> list = new ArrayList<>();
                    list.add(utxoCoin);
                    String id = UUID.randomUUID().toString();
                    PreSelectUtil.getInstance().clear();
                    PreSelectUtil.getInstance().add(id, list);
                    if (utxoCoin.doNotSpend) {
                        Snackbar.make(paynymLayout.getRootView(), R.string.utxo_is_marked_as_blocked, Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    if (id != null) {
                        Intent intent = new Intent(getApplicationContext(), SendActivity.class);
                        intent.putExtra("preselected", id);
                        intent.putExtra("_account", account);
                        startActivity(intent);
                    }
                });

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

        new MaterialAlertDialogBuilder(this)
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

            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.app_name)
                    .setView(showText)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, (dialog, whichButton) -> {
                    })
                    .show();
        }
    }

    private void saveWalletState(){
        Disposable disposable = Completable.fromCallable(() -> {
            try {
                PayloadUtil.getInstance(getApplicationContext()).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(getApplicationContext()).getGUID() + AccessFactory.getInstance().getPIN()));
            } catch (MnemonicException.MnemonicLengthException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (DecryptionException e) {
                e.printStackTrace();
            }
            return true;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
        compositeDisposable.add(disposable);
    }

    private void viewInExplorer() {
        String blockExplorer = "https://m.oxt.me/transaction/";
        if (SamouraiWallet.getInstance().isTestNet()) {
            blockExplorer = "https://blockstream.info/testnet/";
        }
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(blockExplorer + hash));
        startActivity(browserIntent);
    }

}
