package com.samourai.wallet.paynym.paynymDetails;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.send.SendActivity;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.api.Tx;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.SendNotifTxFactory;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.bip47.rpc.SecretPoint;
import com.samourai.wallet.crypto.DecryptionException;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.paynym.fragments.EditPaynymBottomSheet;
import com.samourai.wallet.paynym.fragments.ShowPayNymQRBottomSheet;
import com.samourai.wallet.segwit.BIP49Util;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.segwit.bech32.Bech32Util;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.MyTransactionInput;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.PushTx;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.SuggestedFee;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.send.UTXOFactory;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.MessageSignUtil;
import com.samourai.wallet.util.MonetaryUtil;
import com.samourai.wallet.util.SentToFromBIP47Util;
import com.samourai.wallet.widgets.ItemDividerDecorator;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.script.Script;
import org.bouncycastle.util.encoders.DecoderException;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class PayNymDetailsActivity extends AppCompatActivity {

    private static final int EDIT_PCODE = 2000;
    private static final int RECOMMENDED_PCODE = 2001;
    private static final int SCAN_PCODE = 2077;

    private String pcode = null, label = null;
    private ImageView userAvatar;
    private TextView paynymCode, followMessage, paynymLabel, confirmMessage, followsYoutext;
    private RecyclerView historyRecyclerView;
    private Button followBtn;
    private static final String TAG = "PayNymDetailsActivity";
    private List<Tx> txesList = new ArrayList<>();
    private PaynymTxListAdapter paynymTxListAdapter;
    private CompositeDisposable disposables = new CompositeDisposable();
    private ProgressBar paynymAvatarPorgress, progressBar;
    private ConstraintLayout historyLayout, followLayout;
    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paynym_details);
        setSupportActionBar(findViewById(R.id.toolbar_paynym));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        userAvatar = findViewById(R.id.paybyn_user_avatar);
        paynymCode = findViewById(R.id.paynym_payment_code);
        followMessage = findViewById(R.id.follow_message);
        historyRecyclerView = findViewById(R.id.recycler_view_paynym_history);
        followBtn = findViewById(R.id.paynym_follow_btn);
        paynymAvatarPorgress = findViewById(R.id.paynym_image_progress);
        progressBar = findViewById(R.id.progressBar);
        historyLayout = findViewById(R.id.historyLayout);
        followLayout = findViewById(R.id.followLayout);
        paynymLabel = findViewById(R.id.paynym_label);
        confirmMessage = findViewById(R.id.confirm_message);
        followsYoutext = findViewById(R.id.follows_you_text);
        historyRecyclerView.setNestedScrollingEnabled(true);
        if (getIntent().hasExtra("pcode")) {
            pcode = getIntent().getStringExtra("pcode");
        } else {
            finish();
        }
        if (getIntent().hasExtra("label")) {
            label = getIntent().getStringExtra("label");
        }

        paynymTxListAdapter = new PaynymTxListAdapter(txesList, getApplicationContext());
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyRecyclerView.setAdapter(paynymTxListAdapter);
        Drawable drawable = this.getResources().getDrawable(R.drawable.divider_grey);
        historyRecyclerView.addItemDecoration(new ItemDividerDecorator(drawable));
        setPayNym();

        loadTxes();

        followBtn.setOnClickListener(view -> {
            followPaynym();
        });
        paynymLabel.setOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("paynym", ((TextView) view).getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this,"Paynym id copied",Toast.LENGTH_SHORT).show();
        });
    }

    private void setPayNym() {

        followMessage.setText(getResources().getString(R.string.follow).concat(" ").concat(BIP47Meta.getInstance().getDisplayLabel(pcode)).concat(" ").concat(getResources().getText(R.string.paynym_follow_message_2).toString()));

        if (BIP47Meta.getInstance().getOutgoingStatus(pcode) == BIP47Meta.STATUS_NOT_SENT) {
            showFollow();
        } else {
            showHistory();
        }
//        Log.i(TAG, "setPayNym: ".concat(String.valueOf(BIP47Meta.getInstance().getIncomingIdx(pcode))));

        if(BIP47Meta.getInstance().getIncomingIdx(pcode) == BIP47Meta.STATUS_NOT_SENT)
        if (BIP47Meta.getInstance().getOutgoingStatus(pcode) == BIP47Meta.STATUS_SENT_NO_CFM) {
            showWaitingForConfirm();
        }

        if(BIP47Meta.getInstance().getIncomingIdx(pcode) >= 0){
            historyLayout.setVisibility(View.VISIBLE);
        }
//        if(BIP47Meta.getInstance().getIncomingIdx(pcode)) ){
//
//        }
//        if (BIP47Meta.getInstance().incomingExists(pcode)) {
//            followsYoutext.setVisibility(View.VISIBLE);
//        } else {
//            followsYoutext.setVisibility(View.GONE);
//        }
//        Log.i(TAG, "setPayNym: ".concat(String.valueOf(BIP47Meta.getInstance().getOutgoingStatus(pcode))));
        paynymCode.setText(BIP47Meta.getInstance().getAbbreviatedPcode(pcode));
        paynymLabel.setText(getLabel());
        paynymAvatarPorgress.setVisibility(View.VISIBLE);
        Picasso.with(getApplicationContext())
                .load(com.samourai.wallet.bip47.paynym.WebUtil.PAYNYM_API + pcode + "/avatar")
                .into(userAvatar, new Callback() {
                    @Override
                    public void onSuccess() {
                        paynymAvatarPorgress.setVisibility(View.GONE);
                    }

                    @Override
                    public void onError() {
                        paynymAvatarPorgress.setVisibility(View.GONE);
                        Toast.makeText(PayNymDetailsActivity.this, "Unable to load avatar", Toast.LENGTH_SHORT).show();
                    }
                });
        if (menu != null) {

            if (BIP47Meta.getInstance().getOutgoingStatus(pcode) == BIP47Meta.STATUS_SENT_NO_CFM) {
                menu.findItem(R.id.retry_notiftx).setVisible(true);
            } else {
                menu.findItem(R.id.retry_notiftx).setVisible(false);
            }

        }

    }

    private void showWaitingForConfirm() {
        historyLayout.setVisibility(View.VISIBLE);
        followLayout.setVisibility(View.VISIBLE);
        confirmMessage.setVisibility(View.VISIBLE);
        followBtn.setVisibility(View.GONE);
        followMessage.setVisibility(View.GONE);
    }

    private void showHistory() {
        historyLayout.setVisibility(View.VISIBLE);
        followLayout.setVisibility(View.GONE);
        confirmMessage.setVisibility(View.GONE);
    }

    private void showFollow() {
        historyLayout.setVisibility(View.GONE);
        followBtn.setVisibility(View.VISIBLE);
        followLayout.setVisibility(View.VISIBLE);
        confirmMessage.setVisibility(View.GONE);
        followMessage.setVisibility(View.VISIBLE);
    }

    private void showFollowAlert(String strAmount, View.OnClickListener onClickListener) {

        try {
            Dialog dialog = new Dialog(this, android.R.style.Theme_Dialog);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.paynym_follow_dialog);
            dialog.setCanceledOnTouchOutside(true);
            if (dialog.getWindow() != null)
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            TextView title = dialog.findViewById(R.id.follow_title_paynym_dialog);
            TextView oneTimeFeeMessage = dialog.findViewById(R.id.one_time_fee_message);
            title.setText(getResources().getText(R.string.follow).toString()
                    .concat(" ").concat(BIP47Meta.getInstance().getLabel(pcode)));

            Button followBtn = dialog.findViewById(R.id.follow_paynym_btn);

            String message = getResources().getText(R.string.paynym_follow_fee_message).toString();
            String part1 = message.substring(0, 28);
            String part2 = message.substring(29);

            oneTimeFeeMessage.setText(part1.concat(" ").concat(strAmount).concat(" ").concat(part2));

            followBtn.setOnClickListener(view -> {
                dialog.dismiss();
                if (onClickListener != null)
                    onClickListener.onClick(view);

            });

            dialog.findViewById(R.id.cancel_btn).setOnClickListener(view -> {
                dialog.dismiss();
            });


            dialog.setCanceledOnTouchOutside(false);

            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            dialog.show();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void followPaynym() {
        Bundle bundle = new Bundle();
        bundle.putString("label", getLabel());
        bundle.putString("pcode", pcode);
        bundle.putString("buttonText", "Follow");
        EditPaynymBottomSheet editPaynymBottomSheet = new EditPaynymBottomSheet();
        editPaynymBottomSheet.setArguments(bundle);
        editPaynymBottomSheet.show(getSupportFragmentManager(), editPaynymBottomSheet.getTag());
        editPaynymBottomSheet.setSaveButtonListener(view -> {
            updatePaynym(editPaynymBottomSheet.getLabel(), editPaynymBottomSheet.getPcode());
            doNotifTx();

        });

    }

    private String getLabel() {
        return label == null ? BIP47Meta.getInstance().getDisplayLabel(pcode) : label;
    }

    private void loadTxes() {

        Disposable disposable = Observable.fromCallable(() -> {
            List<Tx> txesListSelected = new ArrayList<>();
            List<Tx> txs = APIFactory.getInstance(this).getAllXpubTxs();
            try {
                APIFactory.getInstance(getApplicationContext()).getXpubAmounts().get(HD_WalletFactory.getInstance(getApplicationContext()).get().getAccount(0).xpubstr());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (MnemonicException.MnemonicLengthException e) {
                e.printStackTrace();
            }

            if (txs != null)
                for (Tx tx : txs) {
                    if (tx.getPaymentCode() != null) {

                        if (tx.getPaymentCode().equals(pcode)) {
                            txesListSelected.add(tx);
                        }

                    }
                    List<String> hashes = SentToFromBIP47Util.getInstance().get(pcode);
                    if (hashes != null)
                        for (String hash : hashes) {
                            if (hash.equals(tx.getHash())) {
                                if (!txesListSelected.contains(tx)) {
                                    txesListSelected.add(tx);
                                }
                            }
                        }
                }
            return txesListSelected;

        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(txes -> {
                    this.txesList.clear();
                    this.txesList.addAll(txes);
                    paynymTxListAdapter.notifyDataSetChanged();
                });
        disposables.add(disposable);
    }

    @Override
    protected void onDestroy() {
        disposables.dispose();
        try {
            PayloadUtil.getInstance(getApplicationContext()).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(getApplicationContext()).getGUID() + AccessFactory.getInstance(getApplicationContext()).getPIN()));
        } catch (MnemonicException.MnemonicLengthException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (DecryptionException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                supportFinishAfterTransition();
                break;
            }

            case R.id.send_menu_paynym_details: {
                if (BIP47Meta.getInstance().getOutgoingStatus(pcode) == BIP47Meta.STATUS_SENT_CFM) {
                    Intent intent = new Intent(this, SendActivity.class);
                    intent.putExtra("pcode", pcode);
                    startActivity(intent);
                } else {
                    if (BIP47Meta.getInstance().getOutgoingStatus(pcode) == BIP47Meta.STATUS_NOT_SENT) {
                        followPaynym();
                    } else {
                        Snackbar.make(historyLayout.getRootView(), "Follow transaction is still pending", Snackbar.LENGTH_SHORT).show();
                    }
                }
                break;
            }
            case R.id.edit_menu_paynym_details: {
                Bundle bundle = new Bundle();
                bundle.putString("label", BIP47Meta.getInstance().getDisplayLabel(pcode));
                bundle.putString("pcode", pcode);
                bundle.putString("buttonText", "Save");
                EditPaynymBottomSheet editPaynymBottomSheet = new EditPaynymBottomSheet();
                editPaynymBottomSheet.setArguments(bundle);
                editPaynymBottomSheet.show(getSupportFragmentManager(), editPaynymBottomSheet.getTag());
                editPaynymBottomSheet.setSaveButtonListener(view -> {
                    updatePaynym(editPaynymBottomSheet.getLabel(), editPaynymBottomSheet.getPcode());
                    setPayNym();
                });

                break;
            }
            case R.id.archive_paynym: {
                BIP47Meta.getInstance().setArchived(pcode, true);
                finish();
                break;
            }
            case R.id.resync_menu_paynym_details: {
                doSync();
                break;
            }
            case R.id.view_code_paynym_details: {
                Bundle bundle = new Bundle();
                bundle.putString("pcode", pcode);
                ShowPayNymQRBottomSheet showPayNymQRBottomSheet = new ShowPayNymQRBottomSheet();
                showPayNymQRBottomSheet.setArguments(bundle);
                showPayNymQRBottomSheet.show(getSupportFragmentManager(), showPayNymQRBottomSheet.getTag());
                break;
            }
            case R.id.retry_notiftx: {
                doNotifTx();
                break;
            }
            case R.id.paynym_indexes: {
                int outgoing = BIP47Meta.getInstance().getOutgoingIdx(pcode);
                int incoming = BIP47Meta.getInstance().getIncomingIdx(pcode);
                Toast.makeText(PayNymDetailsActivity.this, "Incoming index:" + incoming + ", Outgoing index:" + outgoing, Toast.LENGTH_SHORT).show();
                break;
            }


        }
        return super.onOptionsItemSelected(item);
    }

    private void updatePaynym(String label, String pcode) {

        if (pcode == null || pcode.length() < 1 || !FormatsUtil.getInstance().isValidPaymentCode(pcode)) {
            Snackbar.make(userAvatar.getRootView(), R.string.invalid_payment_code, Snackbar.LENGTH_SHORT).show();
        } else if (label == null || label.length() < 1) {
            Snackbar.make(userAvatar.getRootView(), R.string.bip47_no_label_error, Snackbar.LENGTH_SHORT).show();
        } else {
            BIP47Meta.getInstance().setLabel(pcode, label);


            new Thread(() -> {
                Looper.prepare();

                try {
                    PayloadUtil.getInstance(PayNymDetailsActivity.this).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(PayNymDetailsActivity.this).getGUID() + AccessFactory.getInstance().getPIN()));
                } catch (MnemonicException.MnemonicLengthException mle) {
                    mle.printStackTrace();
                    Toast.makeText(PayNymDetailsActivity.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                } catch (DecoderException de) {
                    de.printStackTrace();
                    Toast.makeText(PayNymDetailsActivity.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                } catch (JSONException je) {
                    je.printStackTrace();
                    Toast.makeText(PayNymDetailsActivity.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    Toast.makeText(PayNymDetailsActivity.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                } catch (NullPointerException npe) {
                    npe.printStackTrace();
                    Toast.makeText(PayNymDetailsActivity.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                } catch (DecryptionException de) {
                    de.printStackTrace();
                    Toast.makeText(PayNymDetailsActivity.this, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                } finally {
                    ;
                }

                Looper.loop();
                doUpdatePayNymInfo(pcode);

            }).start();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.paynym_details_menu, menu);
        if (pcode != null) {

            if (BIP47Meta.getInstance().getOutgoingStatus(pcode) == BIP47Meta.STATUS_SENT_NO_CFM) {
                menu.findItem(R.id.retry_notiftx).setVisible(true);
            } else {
                menu.findItem(R.id.retry_notiftx).setVisible(false);
            }

        }
        this.menu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == EDIT_PCODE) {
            setPayNym();
        }
    }

    private void doNotifTx() {

        //
        // get wallet balance
        //
        long balance = 0L;
        try {
            balance = APIFactory.getInstance(PayNymDetailsActivity.this).getXpubAmounts().get(HD_WalletFactory.getInstance(PayNymDetailsActivity.this).get().getAccount(0).xpubstr());
        } catch (IOException ioe) {
            balance = 0L;
        } catch (MnemonicException.MnemonicLengthException mle) {
            balance = 0L;
        } catch (java.lang.NullPointerException npe) {
            balance = 0L;
        }

        final List<UTXO> selectedUTXO = new ArrayList<UTXO>();
        long totalValueSelected = 0L;
//        long change = 0L;
        BigInteger fee = null;
        //
        // spend dust threshold amount to notification address
        //
        long amount = SendNotifTxFactory._bNotifTxValue.longValue();

        //
        // add Samourai Wallet fee to total amount
        //
        amount += SendNotifTxFactory._bSWFee.longValue();

        //
        // get unspents
        //
        List<UTXO> utxos = null;
        if (UTXOFactory.getInstance().getTotalP2SH_P2WPKH() > amount + FeeUtil.getInstance().estimatedFeeSegwit(0, 1, 4).longValue()) {
            utxos = new ArrayList<UTXO>();
            utxos.addAll(UTXOFactory.getInstance().getP2SH_P2WPKH().values());
        } else {
            utxos = APIFactory.getInstance(PayNymDetailsActivity.this).getUtxos(true);
        }

        // sort in ascending order by value
        final List<UTXO> _utxos = utxos;
        Collections.sort(_utxos, new UTXO.UTXOComparator());
        Collections.reverse(_utxos);

        //
        // get smallest 1 UTXO > than spend + fee + sw fee + dust
        //
        for (UTXO u : _utxos) {
            if (u.getValue() >= (amount + SamouraiWallet.bDust.longValue() + FeeUtil.getInstance().estimatedFee(1, 4).longValue())) {
                selectedUTXO.add(u);
                totalValueSelected += u.getValue();
                Log.d("PayNymDetailsActivity", "single output");
                Log.d("PayNymDetailsActivity", "value selected:" + u.getValue());
                Log.d("PayNymDetailsActivity", "total value selected:" + totalValueSelected);
                Log.d("PayNymDetailsActivity", "nb inputs:" + u.getOutpoints().size());
                break;
            }
        }

        //
        // use normal fee settings
        //
        SuggestedFee suggestedFee = FeeUtil.getInstance().getSuggestedFee();

        long lo = FeeUtil.getInstance().getLowFee().getDefaultPerKB().longValue() / 1000L;
        long mi = FeeUtil.getInstance().getNormalFee().getDefaultPerKB().longValue() / 1000L;
        long hi = FeeUtil.getInstance().getHighFee().getDefaultPerKB().longValue() / 1000L;

        if (lo == mi && mi == hi) {
            SuggestedFee hi_sf = new SuggestedFee();
            hi_sf.setDefaultPerKB(BigInteger.valueOf((long) (hi * 1.15 * 1000.0)));
            FeeUtil.getInstance().setSuggestedFee(hi_sf);
        } else if (lo == mi) {
            FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getHighFee());
        } else {
            FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getNormalFee());
        }

        if (selectedUTXO.size() == 0) {
            // sort in descending order by value
            Collections.sort(_utxos, new UTXO.UTXOComparator());
            int selected = 0;

            // get largest UTXOs > than spend + fee + dust
            for (UTXO u : _utxos) {

                selectedUTXO.add(u);
                totalValueSelected += u.getValue();
                selected += u.getOutpoints().size();

                if (totalValueSelected >= (amount + SamouraiWallet.bDust.longValue() + FeeUtil.getInstance().estimatedFee(selected, 4).longValue())) {
                    Log.d("PayNymDetailsActivity", "multiple outputs");
                    Log.d("PayNymDetailsActivity", "total value selected:" + totalValueSelected);
                    Log.d("PayNymDetailsActivity", "nb inputs:" + u.getOutpoints().size());
                    break;
                }
            }

//            fee = FeeUtil.getInstance().estimatedFee(selected, 4);
            fee = FeeUtil.getInstance().estimatedFee(selected, 7);

        } else {
//            fee = FeeUtil.getInstance().estimatedFee(1, 4);
            fee = FeeUtil.getInstance().estimatedFee(1, 7);
        }

        //
        // reset fee to previous setting
        //
        FeeUtil.getInstance().setSuggestedFee(suggestedFee);

        //
        // total amount to spend including fee
        //
        if ((amount + fee.longValue()) >= balance) {

            String message = getText(R.string.bip47_notif_tx_insufficient_funds_1) + " ";
            BigInteger biAmount = SendNotifTxFactory._bSWFee.add(SendNotifTxFactory._bNotifTxValue.add(FeeUtil.getInstance().estimatedFee(1, 4, FeeUtil.getInstance().getLowFee().getDefaultPerKB())));
            String strAmount = MonetaryUtil.getInstance().getBTCFormat().format(((double) biAmount.longValue()) / 1e8) + " BTC ";
            message += strAmount;
            message += " " + getText(R.string.bip47_notif_tx_insufficient_funds_2);

            AlertDialog.Builder dlg = new AlertDialog.Builder(PayNymDetailsActivity.this)
                    .setTitle(R.string.app_name)
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.help, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://support.samourai.io/article/58-connecting-to-a-paynym-contact"));
                            startActivity(browserIntent);

                        }
                    })
                    .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            dialog.dismiss();

                        }
                    });
            if (!isFinishing()) {
                dlg.show();
            }

            return;
        }

        //
        // payment code to be notified
        //
        PaymentCode payment_code;
        try {
            payment_code = new PaymentCode(pcode);
        } catch (AddressFormatException afe) {
            payment_code = null;
        }

        if (payment_code == null) {
            return;
        }

        //
        // create outpoints for spend later
        //
        final List<MyTransactionOutPoint> outpoints = new ArrayList<MyTransactionOutPoint>();
        for (UTXO u : selectedUTXO) {
            outpoints.addAll(u.getOutpoints());
        }
        //
        // create inputs from outpoints
        //
        List<MyTransactionInput> inputs = new ArrayList<MyTransactionInput>();
        for (MyTransactionOutPoint o : outpoints) {
            Script script = new Script(o.getScriptBytes());

            if (script.getScriptType() == Script.ScriptType.NO_TYPE) {
                continue;
            }

            MyTransactionInput input = new MyTransactionInput(SamouraiWallet.getInstance().getCurrentNetworkParams(), null, new byte[0], o, o.getTxHash().toString(), o.getTxOutputN());
            inputs.add(input);
        }
        //
        // sort inputs
        //
        Collections.sort(inputs, new SendFactory.BIP69InputComparator());
        //
        // find outpoint that corresponds to 0th input
        //
        MyTransactionOutPoint outPoint = null;
        for (MyTransactionOutPoint o : outpoints) {
            if (o.getTxHash().toString().equals(inputs.get(0).getTxHash()) && o.getTxOutputN() == inputs.get(0).getTxPos()) {
                outPoint = o;
                break;
            }
        }

        if (outPoint == null) {
            Toast.makeText(PayNymDetailsActivity.this, R.string.bip47_cannot_identify_outpoint, Toast.LENGTH_SHORT).show();
            return;
        }

        byte[] op_return = null;
        //
        // get private key corresponding to outpoint
        //
        try {
//            Script inputScript = new Script(outPoint.getConnectedPubKeyScript());
            byte[] scriptBytes = outPoint.getConnectedPubKeyScript();
            String address = null;
            if (Bech32Util.getInstance().isBech32Script(Hex.toHexString(scriptBytes))) {
                address = Bech32Util.getInstance().getAddressFromScript(Hex.toHexString(scriptBytes));
            } else {
                address = new Script(scriptBytes).getToAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
            }
//            String address = inputScript.getToAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
            ECKey ecKey = SendFactory.getPrivKey(address, 0);
            if (ecKey == null || !ecKey.hasPrivKey()) {
                Toast.makeText(PayNymDetailsActivity.this, R.string.bip47_cannot_compose_notif_tx, Toast.LENGTH_SHORT).show();
                return;
            }

            //
            // use outpoint for payload masking
            //
            byte[] privkey = ecKey.getPrivKeyBytes();
            byte[] pubkey = payment_code.notificationAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).getPubKey();
            byte[] outpoint = outPoint.bitcoinSerialize();
//                Log.i("PayNymDetailsActivity", "outpoint:" + Hex.toHexString(outpoint));
//                Log.i("PayNymDetailsActivity", "payer shared secret:" + Hex.toHexString(new SecretPoint(privkey, pubkey).ECDHSecretAsBytes()));
            byte[] mask = PaymentCode.getMask(new SecretPoint(privkey, pubkey).ECDHSecretAsBytes(), outpoint);
//                Log.i("PayNymDetailsActivity", "mask:" + Hex.toHexString(mask));
//                Log.i("PayNymDetailsActivity", "mask length:" + mask.length);
//                Log.i("PayNymDetailsActivity", "payload0:" + Hex.toHexString(BIP47Util.getInstance(context).getPaymentCode().getPayload()));
            op_return = PaymentCode.blind(BIP47Util.getInstance(PayNymDetailsActivity.this).getPaymentCode().getPayload(), mask);
//                Log.i("PayNymDetailsActivity", "payload1:" + Hex.toHexString(op_return));
        } catch (InvalidKeyException ike) {
            Toast.makeText(PayNymDetailsActivity.this, ike.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        } catch (InvalidKeySpecException ikse) {
            Toast.makeText(PayNymDetailsActivity.this, ikse.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        } catch (NoSuchAlgorithmException nsae) {
            Toast.makeText(PayNymDetailsActivity.this, nsae.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        } catch (NoSuchProviderException nspe) {
            Toast.makeText(PayNymDetailsActivity.this, nspe.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        } catch (Exception e) {
            Toast.makeText(PayNymDetailsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        final HashMap<String, BigInteger> receivers = new HashMap<String, BigInteger>();
        receivers.put(Hex.toHexString(op_return), BigInteger.ZERO);
        receivers.put(payment_code.notificationAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).getAddressString(), SendNotifTxFactory._bNotifTxValue);
        receivers.put(SamouraiWallet.getInstance().isTestNet() ? SendNotifTxFactory.TESTNET_SAMOURAI_NOTIF_TX_FEE_ADDRESS : SendNotifTxFactory.SAMOURAI_NOTIF_TX_FEE_ADDRESS, SendNotifTxFactory._bSWFee);

        final long change = totalValueSelected - (amount + fee.longValue());
        if (change > 0L) {
            String change_address = BIP84Util.getInstance(PayNymDetailsActivity.this).getAddressAt(AddressFactory.CHANGE_CHAIN, BIP84Util.getInstance(PayNymDetailsActivity.this).getWallet().getAccount(0).getChange().getAddrIdx()).getBech32AsString();
            receivers.put(change_address, BigInteger.valueOf(change));
        }
        Log.d("PayNymDetailsActivity", "outpoints:" + outpoints.size());
        Log.d("PayNymDetailsActivity", "totalValueSelected:" + BigInteger.valueOf(totalValueSelected).toString());
        Log.d("PayNymDetailsActivity", "amount:" + BigInteger.valueOf(amount).toString());
        Log.d("PayNymDetailsActivity", "change:" + BigInteger.valueOf(change).toString());
        Log.d("PayNymDetailsActivity", "fee:" + fee.toString());

        if (change < 0L) {
            Toast.makeText(PayNymDetailsActivity.this, R.string.bip47_cannot_compose_notif_tx, Toast.LENGTH_SHORT).show();
            return;
        }

        final MyTransactionOutPoint _outPoint = outPoint;

        String strNotifTxMsg = getText(R.string.bip47_setup4_text1) + " ";
        long notifAmount = amount;
        String strAmount = MonetaryUtil.getInstance().getBTCFormat().format(((double) notifAmount + fee.longValue()) / 1e8) + " BTC ";
        strNotifTxMsg += strAmount + getText(R.string.bip47_setup4_text2);

        showFollowAlert(strAmount, view -> {
            new Thread(new Runnable() {
                @Override
                public void run() {

                    Looper.prepare();

                    Transaction tx = SendFactory.getInstance(PayNymDetailsActivity.this).makeTransaction(0, outpoints, receivers);
                    if (tx != null) {

                        String input0hash = tx.getInput(0L).getOutpoint().getHash().toString();
                        Log.d("PayNymDetailsActivity", "input0 hash:" + input0hash);
                        Log.d("PayNymDetailsActivity", "_outPoint hash:" + _outPoint.getTxHash().toString());
                        int input0index = (int) tx.getInput(0L).getOutpoint().getIndex();
                        Log.d("PayNymDetailsActivity", "input0 index:" + input0index);
                        Log.d("PayNymDetailsActivity", "_outPoint index:" + _outPoint.getTxOutputN());
                        if (!input0hash.equals(_outPoint.getTxHash().toString()) || input0index != _outPoint.getTxOutputN()) {
                            Toast.makeText(PayNymDetailsActivity.this, R.string.bip47_cannot_compose_notif_tx, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        tx = SendFactory.getInstance(PayNymDetailsActivity.this).signTransaction(tx, 0);
                        final String hexTx = new String(org.bouncycastle.util.encoders.Hex.encode(tx.bitcoinSerialize()));
                        Log.d("SendActivity", tx.getHashAsString());
                        Log.d("SendActivity", hexTx);

                        boolean isOK = false;
                        String response = null;
                        try {
                            response = PushTx.getInstance(PayNymDetailsActivity.this).samourai(hexTx);
                            Log.d("SendActivity", "pushTx:" + response);

                            if (response != null) {
                                org.json.JSONObject jsonObject = new org.json.JSONObject(response);
                                if (jsonObject.has("status")) {
                                    if (jsonObject.getString("status").equals("ok")) {
                                        isOK = true;
                                    }
                                }
                            } else {
                                Toast.makeText(PayNymDetailsActivity.this, R.string.pushtx_returns_null, Toast.LENGTH_SHORT).show();
                                return;
                            }

                            if (isOK) {
                                Toast.makeText(PayNymDetailsActivity.this, R.string.payment_channel_init, Toast.LENGTH_SHORT).show();
                                //
                                // set outgoing index for payment code to 0
                                //
                                BIP47Meta.getInstance().setOutgoingIdx(pcode, 0);
//                        Log.i("SendNotifTxFactory", "tx hash:" + tx.getHashAsString());
                                //
                                // status to NO_CFM
                                //
                                BIP47Meta.getInstance().setOutgoingStatus(pcode, tx.getHashAsString(), BIP47Meta.STATUS_SENT_NO_CFM);

                                //
                                // increment change index
                                //
                                if (change > 0L) {
                                    BIP49Util.getInstance(PayNymDetailsActivity.this).getWallet().getAccount(0).getChange().incAddrIdx();
                                }

                                savePayLoad();
                            } else {
                                Toast.makeText(PayNymDetailsActivity.this, R.string.tx_failed, Toast.LENGTH_SHORT).show();
                            }
                            runOnUiThread(() -> {

                                setPayNym();
                            });
                        } catch (JSONException je) {
                            Toast.makeText(PayNymDetailsActivity.this, "pushTx:" + je.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        } catch (MnemonicException.MnemonicLengthException mle) {
                            Toast.makeText(PayNymDetailsActivity.this, "pushTx:" + mle.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        } catch (DecoderException de) {
                            Toast.makeText(PayNymDetailsActivity.this, "pushTx:" + de.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        } catch (IOException ioe) {
                            Toast.makeText(PayNymDetailsActivity.this, "pushTx:" + ioe.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        } catch (DecryptionException de) {
                            Toast.makeText(PayNymDetailsActivity.this, "pushTx:" + de.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }

                    }

                    Looper.loop();

                }
            }).start();
        });


    }

    private void savePayLoad() throws MnemonicException.MnemonicLengthException, DecryptionException, JSONException, IOException {
        PayloadUtil.getInstance(PayNymDetailsActivity.this).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(PayNymDetailsActivity.this).getGUID() + AccessFactory.getInstance(PayNymDetailsActivity.this).getPIN()));
        Log.i(TAG, "savePayLoad: ");
    }

    private void doUpdatePayNymInfo(final String pcode) {

        Disposable disposable = Observable.fromCallable(() -> {
            JSONObject obj = new JSONObject();
            obj.put("nym", pcode);
            String res = com.samourai.wallet.bip47.paynym.WebUtil.getInstance(this).postURL("application/json", null, com.samourai.wallet.bip47.paynym.WebUtil.PAYNYM_API + "api/v1/nym", obj.toString());
//                    Log.d("PayNymDetailsActivity", res);
            JSONObject responseObj = new JSONObject(res);
            if (responseObj.has("nymName")) {
                final String strNymName = responseObj.getString("nymName");
                if (FormatsUtil.getInstance().isValidPaymentCode(BIP47Meta.getInstance().getLabel(pcode))) {
                    BIP47Meta.getInstance().setLabel(pcode, strNymName);
                }
            }
            if (responseObj.has("segwit") && responseObj.getBoolean("segwit")) {
                BIP47Meta.getInstance().setSegwit(pcode, true);
            }
            return true;

        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(success -> {
                    setPayNym();

                    if (success) {
                        savePayLoad();
                    }

                }, errror -> {
                    Log.i(TAG, "doUpdatePayNymInfo: ".concat(errror.getMessage()));
                    Toast.makeText(this, "Unable to update paynym", Toast.LENGTH_SHORT).show();
                });

        disposables.add(disposable);
    }

    private void doUploadFollow(final boolean isTrust) {
        progressBar.setVisibility(View.VISIBLE);
        Disposable disposable = Observable.fromCallable(() -> {

            try {

                JSONObject obj = new JSONObject();
                obj.put("code", BIP47Util.getInstance(this).getPaymentCode().toString());
//                    Log.d("PayNymDetailsActivity", obj.toString());
                String res = com.samourai.wallet.bip47.paynym.WebUtil.getInstance(this).postURL("application/json", null, com.samourai.wallet.bip47.paynym.WebUtil.PAYNYM_API + "api/v1/token", obj.toString());
//                    Log.d("PayNymDetailsActivity", res);

                JSONObject responseObj = new JSONObject(res);
                if (responseObj.has("token")) {
                    String token = responseObj.getString("token");

                    String sig = MessageSignUtil.getInstance(this).signMessage(BIP47Util.getInstance(this).getNotificationAddress().getECKey(), token);
//                        Log.d("PayNymDetailsActivity", sig);

                    obj = new JSONObject();
                    obj.put("target", pcode);
                    obj.put("signature", sig);

//                        Log.d("PayNymDetailsActivity", "follow:" + obj.toString());
                    String endPoint = isTrust ? "api/v1/trust" : "api/v1/follow";
                    res = com.samourai.wallet.bip47.paynym.WebUtil.getInstance(this).postURL("application/json", token, com.samourai.wallet.bip47.paynym.WebUtil.PAYNYM_API + endPoint, obj.toString());
//                        Log.d("PayNymDetailsActivity", res);

                    responseObj = new JSONObject(res);
                    if (responseObj.has("following")) {
                        responseObj.has("token");
                    }

                    savePayLoad();
                }

            } catch (JSONException je) {
                je.printStackTrace();
                return false;

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

            return true;

        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(success -> {
                    progressBar.setVisibility(View.INVISIBLE);
                    setPayNym();
                    doUpdatePayNymInfo(pcode);
                }, error -> {
                    progressBar.setVisibility(View.INVISIBLE);
                    Log.i(TAG, "doUploadFollow: Error->  ".concat(error.getMessage()));
                    setPayNym();
                });
        disposables.add(disposable);

    }

    private void doSync() {
        progressBar.setVisibility(View.VISIBLE);
        Disposable disposable = Observable.fromCallable(() -> {

            PaymentCode payment_code = new PaymentCode(pcode);
            int idx = 0;
            boolean loop = true;
            ArrayList<String> addrs = new ArrayList<String>();
            while (loop) {
                addrs.clear();
                for (int i = idx; i < (idx + 20); i++) {
//                            Log.i("PayNymDetailsActivity", "sync receive from " + i + ":" + BIP47Util.getInstance(PayNymDetailsActivity.this).getReceivePubKey(payment_code, i));
                    BIP47Meta.getInstance().getIdx4AddrLookup().put(BIP47Util.getInstance(this).getReceivePubKey(payment_code, i), i);
                    BIP47Meta.getInstance().getPCode4AddrLookup().put(BIP47Util.getInstance(this).getReceivePubKey(payment_code, i), payment_code.toString());
                    addrs.add(BIP47Util.getInstance(this).getReceivePubKey(payment_code, i));
//                            Log.i("PayNymDetailsActivity", "p2pkh " + i + ":" + BIP47Util.getInstance(PayNymDetailsActivity.this).getReceiveAddress(payment_code, i).getReceiveECKey().toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());

                }
                String[] s = addrs.toArray(new String[addrs.size()]);
                int nb = APIFactory.getInstance(this).syncBIP47Incoming(s);
//                        Log.i("PayNymDetailsActivity", "sync receive idx:" + idx + ", nb == " + nb);
                if (nb == 0) {
                    loop = false;
                }
                idx += 20;

            }

            idx = 0;
            loop = true;
            BIP47Meta.getInstance().setOutgoingIdx(pcode, 0);
            while (loop) {
                addrs.clear();
                for (int i = idx; i < (idx + 20); i++) {
                    PaymentAddress sendAddress = BIP47Util.getInstance(this).getSendAddress(payment_code, i);
//                            Log.i("PayNymDetailsActivity", "sync send to " + i + ":" + sendAddress.getSendECKey().toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
//                            BIP47Meta.getInstance().setOutgoingIdx(payment_code.toString(), i);
                    BIP47Meta.getInstance().getIdx4AddrLookup().put(BIP47Util.getInstance(this).getSendPubKey(payment_code, i), i);
                    BIP47Meta.getInstance().getPCode4AddrLookup().put(BIP47Util.getInstance(this).getSendPubKey(payment_code, i), payment_code.toString());
                    addrs.add(BIP47Util.getInstance(this).getSendPubKey(payment_code, i));

                }
                String[] s = addrs.toArray(new String[addrs.size()]);
                int nb = APIFactory.getInstance(this).syncBIP47Outgoing(s);
//                        Log.i("PayNymDetailsActivity", "sync send idx:" + idx + ", nb == " + nb);
                if (nb == 0) {
                    loop = false;
                }
                idx += 20;
            }

            BIP47Meta.getInstance().pruneIncoming();

            PayloadUtil.getInstance(this).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(this).getGUID() + AccessFactory.getInstance(this).getPIN()));

            return true;

        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aBoolean -> {
                    progressBar.setVisibility(View.INVISIBLE);
                    setPayNym();
                }, error -> {
                    error.printStackTrace();
                    progressBar.setVisibility(View.INVISIBLE);
                });

        disposables.add(disposable);

    }

}
