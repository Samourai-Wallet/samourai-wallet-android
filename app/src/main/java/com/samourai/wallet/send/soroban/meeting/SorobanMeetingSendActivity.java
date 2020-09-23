package com.samourai.wallet.send.soroban.meeting;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.samourai.soroban.client.cahoots.SorobanCahootsInitiator;
import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiActivity;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.cahoots.AndroidSorobanClientService;
import com.samourai.wallet.cahoots.CahootsType;
import com.samourai.wallet.fragments.PaynymSelectModalFragment;
import com.samourai.wallet.send.cahoots.SorobanCahootsActivity;
import com.samourai.wallet.tor.TorManager;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.squareup.picasso.Picasso;

import org.apache.commons.lang3.StringUtils;

import io.matthewnelson.topl_service.TorServiceController;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SorobanMeetingSendActivity extends SamouraiActivity {

    private static final String TAG = "SorobanMeetingSend";
    private SorobanCahootsInitiator sorobanCahootsInitiator;
    private static final int TIMEOUT_MS = 120000;

    private WhirlpoolAccount account;
    private CahootsType cahootsType;
    private long sendAmount;
    private String sendAddress;

    private TextView paynymDisplayName, textViewConnecting;
    private ImageView paynymAvatar;
    private View paynymSelect;
    private Button sendButton;
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

        if (TorManager.INSTANCE.isConnected() && !PrefsUtil.getInstance(getApplication()).getValue(PrefsUtil.OFFLINE, false)) {
            parsePayloadIntent();
        } else {
            String message = "";
            if (!TorManager.INSTANCE.isConnected()) {
                message = "Tor connection is required for online cahoots ? do you want to continue ?";
            }
            new AlertDialog.Builder(this)
                    .setTitle("Confirm")
                    .setMessage(message)
                    .setCancelable(true)
                    .setPositiveButton(R.string.yes, (dialog, whichButton) -> {
                        progressBar.setVisibility(View.VISIBLE);
                        PrefsUtil.getInstance(getApplicationContext()).setValue(PrefsUtil.OFFLINE, false);
                        TorServiceController.startTor();
                        TorManager.INSTANCE.getTorStateLiveData().observe(SorobanMeetingSendActivity.this, torState -> {
                            if (torState == TorManager.TorState.ON) {
                                parsePayloadIntent();
                                progressBar.setVisibility(View.GONE);
                            }
                        });
                    }).setNegativeButton(R.string.no, (dialog, whichButton) -> {
                finish();

            }).show();
        }
    }

    private void parsePayloadIntent() {

        try {
            if (getIntent().hasExtra("_account")) {
                account = WhirlpoolAccount.find(getIntent().getIntExtra("_account", 0)).get();
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
            sorobanCahootsInitiator = AndroidSorobanClientService.getInstance(getApplicationContext()).initiator();

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
        PaynymSelectModalFragment paynymSelectModalFragment = PaynymSelectModalFragment.newInstance(code -> setPCode(code), true);
        paynymSelectModalFragment.show(getSupportFragmentManager(), "paynym_select");
    }

    private void setPCode(String pcode) {
        this.pcode = pcode;

        paynymDisplayName.setText(BIP47Meta.getInstance().getDisplayLabel(pcode));
        Picasso.with(getApplicationContext())
                .load(com.samourai.wallet.bip47.paynym.WebUtil.PAYNYM_API + pcode + "/avatar")
                .into(paynymAvatar);

        sendButton.setVisibility(View.VISIBLE);
        send();
    }

    private void send() {
        setSending(true);
        Toast.makeText(getApplicationContext(), "Sending Cahoots request...", Toast.LENGTH_LONG).show();

        try {
            PaymentCode paymentCode = new PaymentCode(pcode);
            // send meeting request
            sorobanDisposable = sorobanCahootsInitiator.sendMeetingRequest(paymentCode, cahootsType)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(meetingRequest -> {
                                Toast.makeText(getApplicationContext(), "Waiting for Cahoots response...", Toast.LENGTH_LONG).show();
                                // meeting request sent, receive response
                                sorobanDisposable = sorobanCahootsInitiator.receiveMeetingResponse(paymentCode, meetingRequest, TIMEOUT_MS)
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
