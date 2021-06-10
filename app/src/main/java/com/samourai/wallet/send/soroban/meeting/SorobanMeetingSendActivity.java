package com.samourai.wallet.send.soroban.meeting;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.samourai.soroban.client.meeting.SorobanMeetingService;
import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiActivity;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.cahoots.AndroidSorobanCahootsService;
import com.samourai.wallet.cahoots.CahootsType;
import com.samourai.wallet.fragments.PaynymSelectModalFragment;
import com.samourai.wallet.send.cahoots.SorobanCahootsActivity;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.squareup.picasso.Picasso;

import org.apache.commons.lang3.StringUtils;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java8.util.Optional;

public class SorobanMeetingSendActivity extends SamouraiActivity {

    private static final String TAG = "SorobanMeetingSend";
    private SorobanMeetingService sorobanMeetingService;
    private static final int TIMEOUT_MS = 120000;

    private WhirlpoolAccount account;
    private CahootsType cahootsType;
    private long sendAmount;
    private String sendAddress;

    private TextView paynymDisplayName, textViewConnecting;
    private ImageView paynymAvatar;
    private View paynymSelect;
    private MaterialButton sendButton;
    private ProgressBar progressBar;
    private Disposable sorobanDisposable;

    private String pcode;

    public static Intent createIntent(Context ctx, int account, CahootsType type, long amount, String address, String pcode) {
        Intent intent = new Intent(ctx, SorobanMeetingSendActivity.class);
        intent.putExtra("_account", account);
        intent.putExtra("type", type.getValue());
        intent.putExtra("sendAmount", amount);
        intent.putExtra("sendAddress", address);
        if (!StringUtils.isEmpty(pcode)) {
            intent.putExtra("pcode", pcode);
        }
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_soroban_meeting_send);

        setSupportActionBar(findViewById(R.id.toolbar_soroban_meeting));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            setTitle("Select Cahoots counterparty");
        }
        paynymSelect = findViewById(R.id.paynym_select);
        paynymDisplayName = findViewById(R.id.paynym_display_name);
        textViewConnecting = findViewById(R.id.textViewConnecting);
        paynymAvatar = findViewById(R.id.img_paynym_avatar);
        sendButton = findViewById(R.id.send_button);
        progressBar = findViewById(R.id.progressBar);

        paynymSelect.setOnClickListener(v -> selectPCode());
        sendButton.setOnClickListener(v -> send());
        parsePayloadIntent();
        startListen();
    }

    // TODO remove on next whirlpool-client upgrade
    public static Optional<WhirlpoolAccount> findWhirlpoolAccount(int index) {
        for (WhirlpoolAccount whirlpoolAccount : WhirlpoolAccount.values()) {
            if (whirlpoolAccount.getAccountIndex() == index) {
                return Optional.of(whirlpoolAccount);
            }
        }
        return Optional.empty();
    }

    private void parsePayloadIntent() {

        try {
            if (getIntent().hasExtra("_account")) {
                account = findWhirlpoolAccount(getIntent().getIntExtra("_account", 0)).get();
            }
            if (getIntent().hasExtra("type")) {
                int type = getIntent().getIntExtra("type", -1);
                cahootsType = CahootsType.find(type).get();
            }
            if (getIntent().hasExtra("sendAmount")) {
                sendAmount = getIntent().getLongExtra("sendAmount", 0);
            }
            if (getIntent().hasExtra("sendAddress")) {
                sendAddress = getIntent().getStringExtra("sendAddress");
            }
            if (cahootsType == null || sendAmount <= 0) {
                throw new Exception("Invalid arguments");
            }

            if (getIntent().hasExtra("pcode")) {
                setPCode(getIntent().getStringExtra("pcode"));
            } else {
                selectPCode();
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Cahoots error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            finish();
        }
    }

    private void selectPCode() {
        PaynymSelectModalFragment paynymSelectModalFragment = PaynymSelectModalFragment.newInstance(code -> setPCode(code), getString(R.string.paynym), true);
        paynymSelectModalFragment.show(getSupportFragmentManager(), "paynym_select");
    }

    private void startListen(){
        sorobanMeetingService = AndroidSorobanCahootsService.getInstance(getApplicationContext()).getSorobanMeetingService();
        send();
    }
    private void setPCode(String pcode) {
        this.pcode = pcode;

        paynymDisplayName.setText(BIP47Meta.getInstance().getDisplayLabel(pcode));
        Picasso.get()
                .load(com.samourai.wallet.bip47.paynym.WebUtil.PAYNYM_API + pcode + "/avatar")
                .into(paynymAvatar);

        sendButton.setVisibility(View.VISIBLE);
    }

    private void send() {
        setSending(true);
        textViewConnecting.setText("Sending Cahoots request...");

        try {
            PaymentCode paymentCode = new PaymentCode(pcode);
            // send meeting request
            sorobanDisposable = sorobanMeetingService.sendMeetingRequest(paymentCode, cahootsType)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(meetingRequest -> {
                                textViewConnecting.setText("Have your mixing partner receive online Cahoots.");
                                // meeting request sent, receive response
                                sorobanDisposable = sorobanMeetingService.receiveMeetingResponse(paymentCode, meetingRequest, TIMEOUT_MS)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(sorobanResponse -> {
                                            if (sorobanResponse.isAccept()) {
                                                Toast.makeText(getApplicationContext(), "Cahoots request accepted!", Toast.LENGTH_LONG).show();
                                                Intent intent = SorobanCahootsActivity.createIntentSender(this, account.getAccountIndex(), cahootsType, sendAmount, sendAddress, pcode);
                                                startActivity(intent);
                                            } else {
                                                Toast.makeText(getApplicationContext(), "Cahoots request refused!", Toast.LENGTH_LONG).show();
                                            }
                                            setSending(false);
                                        }, error -> {
                                            setSending(false);
                                            Toast.makeText(getApplicationContext(), "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                                            error.printStackTrace();
                                        });
                            }, error -> {
                                setSending(false);
                                Toast.makeText(getApplicationContext(), "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                                error.printStackTrace();
                            }
                    );
        } catch (Exception e) {
            setSending(false);
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setSending(boolean sending) {
        progressBar.setVisibility(sending ? View.VISIBLE : View.INVISIBLE);
        textViewConnecting.setVisibility(sending ? View.VISIBLE : View.INVISIBLE);
        sendButton.setVisibility(sending ? View.INVISIBLE : View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        AppUtil.getInstance(this).setIsInForeground(true);

        AppUtil.getInstance(this).checkTimeOut();
    }

    private void clearDisposable() {
        if (sorobanDisposable != null && !sorobanDisposable.isDisposed()) {
            sorobanDisposable.dispose();
            sorobanDisposable = null;
        }
    }

    @Override
    public void finish() {
        clearDisposable();
        super.finish();
    }

    @Override
    public void onBackPressed() {// cancel cahoots request
        clearDisposable();
        super.onBackPressed();
    }
}
