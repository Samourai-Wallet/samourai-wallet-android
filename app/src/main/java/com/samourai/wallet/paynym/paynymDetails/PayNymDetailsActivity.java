package com.samourai.wallet.paynym.paynymDetails;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.constraint.Group;
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
import android.widget.TextView;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.samourai.wallet.MainActivity2;
import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.SendActivity;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.api.Tx;
import com.samourai.wallet.bip47.BIP47Activity;
import com.samourai.wallet.bip47.BIP47Add;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.SendNotifTxFactory;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.home.BalanceViewModel;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.SuggestedFee;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.send.UTXOFactory;
import com.samourai.wallet.util.MonetaryUtil;
import com.samourai.wallet.util.SentToFromBIP47Util;
import com.samourai.wallet.widgets.ItemDividerDecorator;
import com.squareup.picasso.Picasso;
import com.yanzhenjie.zbar.Symbol;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.crypto.MnemonicException;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class PayNymDetailsActivity extends AppCompatActivity {

    private static final int EDIT_PCODE = 2000;
    private static final int RECOMMENDED_PCODE = 2001;
    private static final int SCAN_PCODE = 2077;

    private String pcode = null;
    private ImageView userAvatar;
    private TextView paynymCode, followMessage;
    private RecyclerView historyRecyclerView;
    private Button followBtn;
    private static final String TAG = "PayNymDetailsActivity";
    private BalanceViewModel balanceViewModel;
    private Group followGroup, txListGroup;
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private List<Tx> txesList = new ArrayList<>();
    private PaynymTxListAdapter paynymTxListAdapter;
    private CompositeDisposable disposables = new CompositeDisposable();

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
        followGroup = findViewById(R.id.follow_group_paynym);
        txListGroup = findViewById(R.id.tx_list_group);

        if (getIntent().hasExtra("pcode")) {
            pcode = getIntent().getStringExtra("pcode");
        } else {
            finish();
        }
        setPayNym();

        followMessage.setText(getResources().getString(R.string.follow).concat(" ").concat(BIP47Meta.getInstance().getLabel(pcode)).concat(" ").concat(getResources().getText(R.string.paynym_follow_message_2).toString()));
        if (BIP47Meta.getInstance().getOutgoingStatus(pcode) == BIP47Meta.STATUS_NOT_SENT) {
            showFollow();
        } else {
            hideFollow();
        }

        paynymTxListAdapter = new PaynymTxListAdapter(txesList, getApplicationContext());
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyRecyclerView.setAdapter(paynymTxListAdapter);
        Drawable drawable = this.getResources().getDrawable(R.drawable.divider_grey);
        historyRecyclerView.addItemDecoration(new ItemDividerDecorator(drawable));

        loadTxes();

        followBtn.setOnClickListener(view -> {
            showFollowAlert();
        });

    }

    private void setPayNym() {
        paynymCode.setText(BIP47Meta.getInstance().getAbbreviatedPcode(pcode));
        getSupportActionBar().setTitle(BIP47Meta.getInstance().getLabel(pcode));
        Picasso.with(getApplicationContext()).load(com.samourai.wallet.bip47.paynym.WebUtil.PAYNYM_API + pcode + "/avatar")
                .into(userAvatar);
    }

    private void hideFollow() {
        txListGroup.setVisibility(View.VISIBLE);
        followGroup.setVisibility(View.GONE);
    }

    private void showFollow() {
        txListGroup.setVisibility(View.GONE);
        followGroup.setVisibility(View.VISIBLE);
    }

    private void showFollowAlert() {

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


            BigInteger biAmount = SendNotifTxFactory._bSWFee.add(SendNotifTxFactory._bNotifTxValue.add(FeeUtil.getInstance().estimatedFee(1, 4, FeeUtil.getInstance().getLowFee().getDefaultPerKB())));
            String strAmount = MonetaryUtil.getInstance().getBTCFormat().format(((double) biAmount.longValue()) / 1e8) + " BTC ";


            String message = getResources().getText(R.string.paynym_follow_fee_message).toString();
            String part1 = message.substring(0, 28);
            String part2 = message.substring(29);

            oneTimeFeeMessage.setText(part1.concat(" ").concat(strAmount).concat(" ").concat(part2));

            followBtn.setOnClickListener(view -> {
                followPaynym();
            });

            dialog.findViewById(R.id.cancel_btn).setOnClickListener(view -> {
                dialog.dismiss();
            });

            Disposable disposable = this.calculateFees().subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(feeInfo -> {


                    });
            disposables.add(disposable);
            dialog.setCanceledOnTouchOutside(false);

            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            dialog.show();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void followPaynym() {

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
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                break;
            }

            case R.id.send_menu_paynym_details: {
                Intent intent = new Intent(this, SendActivity.class);
                intent.putExtra("pcode", pcode);
                startActivity(intent);
                break;
            }
            case R.id.edit_menu_paynym_details: {
                Intent intent = new Intent(this, BIP47Add.class);
                intent.putExtra("label", BIP47Meta.getInstance().getLabel(pcode));
                intent.putExtra("pcode", pcode);
                startActivityForResult(intent, EDIT_PCODE);
                break;
            }
            case R.id.archive_paynym: {
                BIP47Meta.getInstance().setArchived(pcode, true);
                finish();
                break;
            }


        }
        return super.onOptionsItemSelected(item);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.paynym_details_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == EDIT_PCODE) {
            setPayNym();
        }
    }

    private Observable<HashMap<String, BigInteger>> calculateFees() {
        return Observable.fromCallable(() -> {
            long balance = 0L;
            try {
                balance = APIFactory.getInstance(getApplicationContext()).getXpubAmounts().get(HD_WalletFactory.getInstance(getApplicationContext()).get().getAccount(0).xpubstr());
            } catch (IOException ioe) {
                balance = 0L;
            } catch (MnemonicException.MnemonicLengthException mle) {
                balance = 0L;
            } catch (NullPointerException npe) {
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
                utxos = APIFactory.getInstance(getApplicationContext()).getUtxos(true);
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
                    Log.d("BIP47Activity", "single output");
                    Log.d("BIP47Activity", "value selected:" + u.getValue());
                    Log.d("BIP47Activity", "total value selected:" + totalValueSelected);
                    Log.d("BIP47Activity", "nb inputs:" + u.getOutpoints().size());
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
                        Log.d("BIP47Activity", "multiple outputs");
                        Log.d("BIP47Activity", "total value selected:" + totalValueSelected);
                        Log.d("BIP47Activity", "nb inputs:" + u.getOutpoints().size());
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
            HashMap<String, BigInteger> data = new HashMap<>();
            data.put("balance", BigInteger.valueOf(balance));
            data.put("fee", fee);
            return data;
        });


    }


}
