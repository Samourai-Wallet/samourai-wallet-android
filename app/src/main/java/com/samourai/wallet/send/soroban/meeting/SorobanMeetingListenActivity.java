package com.samourai.wallet.send.soroban.meeting;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.samourai.soroban.client.meeting.SorobanMeetingService;
import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiActivity;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.cahoots.AndroidSorobanCahootsService;
import com.samourai.wallet.send.cahoots.SorobanCahootsActivity;
import com.samourai.wallet.util.AppUtil;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SorobanMeetingListenActivity extends SamouraiActivity {

    private static final String TAG = "SorobanMeetingListen";
    private static final BIP47Meta bip47Meta = BIP47Meta.getInstance();
    private SorobanMeetingService sorobanMeetingService;
    private Disposable sorobanDisposable;
    private static final int TIMEOUT_MS = 60000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_soroban_meeting_listen);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        sorobanMeetingService = AndroidSorobanCahootsService.getInstance(getApplicationContext()).getSorobanMeetingService();

        try {
            startListen();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Cahoots error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            finish();
        }
    }

    private void startListen() throws Exception {
        Toast.makeText(this, "Waiting for online Cahoots requests...", Toast.LENGTH_SHORT).show();
        sorobanDisposable = sorobanMeetingService.receiveMeetingRequest(TIMEOUT_MS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(cahootsRequest -> {

                    // trusted paynyms only
                    if (!bip47Meta.exists(cahootsRequest.getSender(), false)) {
                        String senderDisplayLabel = bip47Meta.getDisplayLabel(cahootsRequest.getSender());
                        Toast.makeText(this, "Ignored Cahoots request from unknown sender: "+senderDisplayLabel, Toast.LENGTH_LONG).show();
                        return;
                    }
                    String senderDisplayLabel = bip47Meta.getDisplayLabel(cahootsRequest.getSender());
                    PaymentCode senderPaymentCode = new PaymentCode(cahootsRequest.getSender());

                    String alert = "From: "+senderDisplayLabel+"\n" +
                            "Type: "+cahootsRequest.getType().getLabel()+"\n" +
                            "Miner fee: "+(cahootsRequest.getType().isMinerFeeShared() ? "shared" : "paid by sender")+"\n";
                    alert += "Do you want to collaborate?";
                    new AlertDialog.Builder(SorobanMeetingListenActivity.this)
                            .setTitle("Cahoots request received!")
                            .setMessage(alert)
                            .setCancelable(true)
                            .setPositiveButton(R.string.yes, (dialog, whichButton) -> {
                                try {
                                    Toast.makeText(this, "Accepting Cahoots request...", Toast.LENGTH_SHORT).show();
                                    sorobanMeetingService.sendMeetingResponse(senderPaymentCode, cahootsRequest, true).subscribe(sorobanResponseMessage -> {
                                        Intent intent = SorobanCahootsActivity.createIntentCounterparty(getApplicationContext(), account, cahootsRequest.getType(), cahootsRequest.getSender());
                                        startActivity(intent);
                                        finish();
                                    });
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Toast.makeText(this, "Error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }).setNegativeButton(R.string.no, (dialog, whichButton) -> {
                                try {
                                    Toast.makeText(this, "Refusing Cahoots request...", Toast.LENGTH_SHORT).show();
                                    sorobanMeetingService.sendMeetingResponse(senderPaymentCode, cahootsRequest, false).subscribe(sorobanResponseMessage -> {
                                        finish();
                                    });
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Toast.makeText(this, "Error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                    }).show();
                }, error->{
                    Log.i(TAG, "Error: "+error.getMessage());
                    Toast.makeText(this, "Error: "+ error.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
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
