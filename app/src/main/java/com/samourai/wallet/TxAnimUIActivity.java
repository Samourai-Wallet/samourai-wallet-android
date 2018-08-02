package com.samourai.wallet;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.samourai.wallet.widgets.ArcProgress;
import com.samourai.wallet.widgets.TransactionProgressView;

public class TxAnimUIActivity extends AppCompatActivity {

    private ArcProgress progress;
    TransactionProgressView progressView;
    private boolean showSucces = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_ui2);

        progressView = findViewById(R.id.transactionProgressView);

        progressView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (showSucces) {
                    successTx();
                } else {
                    failureTx();
                }
                showSucces = !showSucces;
            }
        });


    }

    private void successTx() {
        progressView.reset();

        progressView.setTxStatusMessage("Creating transaction...");
        progressView.getmArcProgress().startArc1(800);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                progressView.getmArcProgress().startArc2(800);
                progressView.setTxStatusMessage("signing transaction...");

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        progressView.getmArcProgress().startArc3(800);
                        progressView.setTxStatusMessage("broadcasting transaction...");

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                progressView.showCheck();
                                progressView.setTxStatusMessage("transaction sent");

                            }
                        }, 1500);
                    }
                }, 1599);

            }
        }, 2000);


    }


    private void failureTx() {
        progressView.reset();

        progressView.setTxStatusMessage("Creating transaction...");
        progressView.getmArcProgress().startArc1(800);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                progressView.getmArcProgress().startArc2(800);
                progressView.setTxStatusMessage("signing transaction...");

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        progressView.setTxStatusMessage("broadcasting transaction...");
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                progressView.offlineMode(1200);
                                progressView.setTxStatusMessage("transaction signed....");
                                progressView.setTxSubText(R.string.tx_connectivity_failure_msg);
                                progressView.toggleOfflineButton();
                            }
                        }, 1400);
                    }
                }, 1200);


            }
        }, 1200);


    }
}
