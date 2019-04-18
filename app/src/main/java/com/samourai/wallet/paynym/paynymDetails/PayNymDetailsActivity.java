package com.samourai.wallet.paynym.paynymDetails;

import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.app.Activity;
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
import com.samourai.wallet.R;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.api.Tx;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.home.BalanceActivity;
import com.samourai.wallet.home.BalanceViewModel;
import com.samourai.wallet.util.SentToFromBIP47Util;
import com.samourai.wallet.widgets.ItemDividerDecorator;
import com.squareup.picasso.Picasso;
import com.yanzhenjie.zbar.Symbol;

import org.bitcoinj.crypto.MnemonicException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


public class PayNymDetailsActivity extends AppCompatActivity {

    private String pcode = null;
    private ImageView userAvatar;
    private TextView paynymCode, followMessage;
    private RecyclerView historyRecyclerView;
    private Button followBtn;
    private static final String TAG = "PayNymDetailsActivity";
    private List<String> addresses;
    private BalanceViewModel balanceViewModel;
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private List<Tx> txesList = new ArrayList<>();
    private PaynymTxListAdapter paynymTxListAdapter;

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

        if (getIntent().hasExtra("pcode")) {
            pcode = getIntent().getStringExtra("pcode");
        } else {
            finish();
        }

        paynymCode.setText(BIP47Meta.getInstance().getAbbreviatedPcode(pcode));
        getSupportActionBar().setTitle(BIP47Meta.getInstance().getLabel(pcode));
        Picasso.with(getApplicationContext()).load(com.samourai.wallet.bip47.paynym.WebUtil.PAYNYM_API + pcode + "/avatar")
                .into(userAvatar);
        followMessage.setText(getResources().getString(R.string.follow).concat(" ").concat(BIP47Meta.getInstance().getLabel(pcode)).concat(" ").concat(getResources().getText(R.string.paynym_follow_message_2).toString()));
        if (BIP47Meta.getInstance().getOutgoingStatus(pcode) == BIP47Meta.STATUS_NOT_SENT) {
            showFollow();
        } else {
            hideFollow();
        }


        addresses = SentToFromBIP47Util.getInstance().get(pcode);
        if(addresses!=null){
            addresses = new ArrayList<>();
        }
        paynymTxListAdapter = new PaynymTxListAdapter(txesList, getApplicationContext());
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyRecyclerView.setAdapter(paynymTxListAdapter);
        Drawable drawable = this.getResources().getDrawable(R.drawable.divider_grey);
        historyRecyclerView.addItemDecoration(new ItemDividerDecorator(drawable));

        loadTxes();

    }

    private void hideFollow() {
        followMessage.setVisibility(View.GONE);
        followBtn.setVisibility(View.GONE);
        historyRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showFollow() {
        followMessage.setVisibility(View.VISIBLE);
        followBtn.setVisibility(View.VISIBLE);
        historyRecyclerView.setVisibility(View.GONE);
    }

    private void showFollowAlert() {

        Dialog dialog = new Dialog(getApplicationContext(), android.R.style.Theme_Dialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dojo_connect_dialog);
        dialog.setCanceledOnTouchOutside(true);
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        dialog.setCanceledOnTouchOutside(false);

        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        dialog.show();

        dialog.findViewById(R.id.dojo_scan_qr).setOnClickListener(view -> {

            Intent intent = new Intent(this.getApplicationContext(), ZBarScannerActivity.class);
            intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{Symbol.QRCODE});
            this.startActivityForResult(intent, 1);

        });

        dialog.findViewById(R.id.dojo_paste_config).setOnClickListener(view -> {

        });

    }

    private void loadTxes() {
        List<Tx> txs = APIFactory.getInstance(this).getAllXpubTxs();
        Log.i(TAG, "loadTxes: 1 ---> ".concat(gson.toJson(txs)));
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
                    if (tx.getPaymentCode().equals(pcode) && tx.getAmount() > 0) {
                        txesList.add(tx);
                    }

                }
                for (String hash : addresses) {
                    if (hash.equals(tx.getHash())) {
                        txesList.add(tx);
                    }
                }
                Log.i(TAG, "loadTxes: ".concat(gson.toJson(txesList)));
                if(txesList.size() != 0){
                    hideFollow();
                }
                paynymTxListAdapter.notifyDataSetChanged();
            }


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.paynym_details_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


}
