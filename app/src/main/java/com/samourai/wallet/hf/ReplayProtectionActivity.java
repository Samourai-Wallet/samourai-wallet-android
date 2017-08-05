package com.samourai.wallet.hf;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;

import com.samourai.wallet.MainActivity2;
import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.PushTx;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.SuggestedFee;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.service.WebSocketService;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.PrefsUtil;

import org.bitcoinj.core.Transaction;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ReplayProtectionActivity extends Activity {

    private LinearLayout layoutAlert = null;
    private LinearLayout layoutMain = null;
    private LinearLayout layoutBitcoin = null;
    private LinearLayout layoutShitcoin = null; // BCC now, 2x in Nov.
    private LinearLayout layoutFiller = null;

    private TextView tvBitcoin1 = null;
    private TextView tvBitcoin2 = null;
    private TextView tvBitcoin3 = null;

    private TextView tvShitcoin1 = null;
    private TextView tvShitcoin2 = null;
    private TextView tvShitcoin3 = null;

    private static final int COLOR_ORANGE = 0xfffb8c00;
    private static final int COLOR_GREEN = 0xff4caf50;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_replay_protection);

        layoutMain = (LinearLayout)findViewById(R.id.main);

        layoutAlert = (LinearLayout)findViewById(R.id.alert);
        layoutAlert.setBackgroundColor(COLOR_ORANGE);

        ((TextView)layoutAlert.findViewById(R.id.left)).setText(getText(R.string.replay_protection));
        ((TextView)layoutAlert.findViewById(R.id.right)).setText(getText(R.string.replay_in_progress));

        LayoutInflater inflator = ReplayProtectionActivity.this.getLayoutInflater();
        layoutBitcoin = (LinearLayout)inflator.inflate(R.layout.replay_protection_progess_layout, null);
        layoutShitcoin = (LinearLayout)inflator.inflate(R.layout.replay_protection_progess_layout, null);
        layoutFiller = (LinearLayout)inflator.inflate(R.layout.replay_protection_progess_layout, null);

        layoutMain.addView(layoutBitcoin);
        layoutMain.addView(layoutFiller);
        layoutMain.addView(layoutShitcoin);

        tvBitcoin1 = (TextView)layoutBitcoin.findViewById(R.id.text1);
        tvBitcoin2 = (TextView)layoutBitcoin.findViewById(R.id.text2);
        tvBitcoin3 = (TextView)layoutBitcoin.findViewById(R.id.text3);

        tvShitcoin1 = (TextView)layoutShitcoin.findViewById(R.id.text1);
        tvShitcoin2 = (TextView)layoutShitcoin.findViewById(R.id.text2);
        tvShitcoin3 = (TextView)layoutShitcoin.findViewById(R.id.text3);

        tvBitcoin1.setText(getText(R.string.replay_samourai_user_agent));
        tvShitcoin1.setText(getText(R.string.replay_bcc_user_agent));

        replayProtectionThread();

    }

    @Override
    public void onResume() {
        super.onResume();

        AppUtil.getInstance(ReplayProtectionActivity.this).checkTimeOut();

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if(keyCode == KeyEvent.KEYCODE_BACK) {

            Intent _intent = new Intent(ReplayProtectionActivity.this, MainActivity2.class);
            _intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(_intent);

            return true;
        }
        else	{
            ;
        }

        return false;
    }

    private void replayProtectionThread()    {

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                String rbfHash = null;

                if(PrefsUtil.getInstance(ReplayProtectionActivity.this).getValue(PrefsUtil.BCC_REPLAY1, "").length() == 0)    {

                    long balance = 0L;
                    List<MyTransactionOutPoint> outpoints = new ArrayList<MyTransactionOutPoint>();
                    List<UTXO> utxos = APIFactory.getInstance(ReplayProtectionActivity.this).getUtxos();
                    for(UTXO utxo : utxos)   {
                        balance += utxo.getValue();
                        outpoints.addAll(utxo.getOutpoints());
                    }

                    Log.d("ReplayProtectionA", "outpoints selected:" + outpoints.size());
                    Log.d("ReplayProtectionA", "balance:" + balance);

                    SuggestedFee rbfFee = FeeUtil.getInstance().getNormalFee();

                    BigInteger biFee0 = FeeUtil.getInstance().estimatedFee(outpoints.size(), 1, BigInteger.valueOf(3L * 1000L));
                    Log.d("ReplayProtectionA", "biFee0:" + biFee0.longValue());
                    BigInteger biFee1 = FeeUtil.getInstance().estimatedFee(outpoints.size(), 1, rbfFee.getDefaultPerKB());
                    Log.d("ReplayProtectionA", "biFee1:" + biFee1.longValue());

                    long amount0 = balance - biFee0.longValue();
                    Log.d("ReplayProtectionA", "amount0:" + amount0);
                    long amount1 = balance - biFee1.longValue();
                    Log.d("ReplayProtectionA", "amount1:" + amount1);

                    String ownReceiveAddr = AddressFactory.getInstance(ReplayProtectionActivity.this).get(AddressFactory.RECEIVE_CHAIN).getAddressString();
                    Log.d("ReplayProtectionA", "receive address:" + ownReceiveAddr);

                    HashMap<String, BigInteger> receivers = new HashMap<String, BigInteger>();

                    boolean currentRBF = PrefsUtil.getInstance(ReplayProtectionActivity.this).getValue(PrefsUtil.RBF_OPT_IN, false);
                    PrefsUtil.getInstance(ReplayProtectionActivity.this).setValue(PrefsUtil.RBF_OPT_IN, true);

                    receivers.put(ownReceiveAddr, BigInteger.valueOf(amount0));
                    Transaction tx0 = SendFactory.getInstance(ReplayProtectionActivity.this).makeTransaction(0, outpoints, receivers);
                    tx0 = SendFactory.getInstance(ReplayProtectionActivity.this).signTransaction(tx0);
                    final String hexTx0 = new String(Hex.encode(tx0.bitcoinSerialize()));
                    Log.d("ReplayProtectionA", "hexTx0:" + hexTx0);
                    final String strTxHash0 = tx0.getHashAsString();
                    Log.d("ReplayProtectionA", "txHash0:" + strTxHash0);

                    receivers.clear();
                    receivers.put(ownReceiveAddr, BigInteger.valueOf(amount1));
                    Transaction tx1 = SendFactory.getInstance(ReplayProtectionActivity.this).makeTransaction(0, outpoints, receivers);
                    tx1 = SendFactory.getInstance(ReplayProtectionActivity.this).signTransaction(tx1);
                    String hexTx1 = new String(Hex.encode(tx1.bitcoinSerialize()));
                    Log.d("ReplayProtectionA", "hexTx1:" + hexTx1);
                    final String strTxHash1 = tx1.getHashAsString();
                    Log.d("ReplayProtectionA", "txHash1:" + strTxHash1);

                    PrefsUtil.getInstance(ReplayProtectionActivity.this).setValue(PrefsUtil.RBF_OPT_IN, currentRBF);

                    boolean tx0pushedBTC = PushTx.getInstance(ReplayProtectionActivity.this).pushTx(hexTx0);
                    Log.d("ReplayProtectionA", "tx0 pushed BTC:" + tx0pushedBTC);
                    if(tx0pushedBTC)    {
                        handler.post(new Runnable() {
                            public void run() {
                                tvBitcoin2.setText(getText(R.string.replay_broadcast_to_network));
                            }
                        });
                    }

                    boolean tx0pushedBCC = HardForkUtil.getInstance(ReplayProtectionActivity.this).bccPushTx(hexTx0);
                    Log.d("ReplayProtectionA", "tx0 pushed BCC:" + tx0pushedBCC);
                    /*
                    if(tx0pushedBCC)    {
                        handler.post(new Runnable() {
                            public void run() {
                                tvShitcoin2.setText(getText(R.string.replay_broadcast_to_network));
                            }
                        });
                    }
                    */
                    handler.post(new Runnable() {
                        public void run() {
                            tvShitcoin2.setText(getText(R.string.replay_broadcast_to_network));
                        }
                    });

                    try {
                        Thread.sleep(2L * 1000L);
                    }
                    catch(InterruptedException ie) {
                        return;
                    }

                    JSONObject txObj0 = APIFactory.getInstance(ReplayProtectionActivity.this).getTxInfo(strTxHash0);
                    Log.d("ReplayProtectionA", "tx0 status:" + txObj0.toString());
                    boolean tx0Status = btcTxSeen(txObj0);
                    Log.d("ReplayProtectionA", "tx0 status:" + tx0Status);
                    if(tx0Status)    {
                        handler.post(new Runnable() {
                            public void run() {
                                tvBitcoin2.setText(getText(R.string.replay_seen_on_network));
                                tvBitcoin3.setText(strTxHash0.substring(0, 30) + "...");
                            }
                        });
                    }

                    String bccTxOut = HardForkUtil.getInstance(ReplayProtectionActivity.this).bccTxOut(strTxHash0, 0);
                    Log.d("ReplayProtectionA", "bcc tx out:" + bccTxOut);
                    boolean bccTxStatus = bccTxConfirmed(bccTxOut);
                    Log.d("ReplayProtectionA", "bcc confirmed:" + bccTxStatus);
                    if(bccTxStatus)    {
                        handler.post(new Runnable() {
                            public void run() {
                                tvShitcoin2.setText(getText(R.string.replay_seen_on_network));
                                tvShitcoin3.setText(strTxHash0.substring(0, 30) + "...");
                            }
                        });
                    }

                    boolean tx1pushedBTC = PushTx.getInstance(ReplayProtectionActivity.this).pushTx(hexTx1);
                    Log.d("ReplayProtectionA", "tx1 pushed BTC:" + tx1pushedBTC);
                    if(tx1pushedBTC)    {
                        handler.post(new Runnable() {
                            public void run() {
                                tvBitcoin2.setText(getText(R.string.replay_split_tx_broadcast));
                                tvBitcoin3.setText("");
                            }
                        });
                    }

                    try {
                        Thread.sleep(2L * 1000L);
                    }
                    catch(InterruptedException ie) {
                        return;
                    }

                    JSONObject txObj1 = APIFactory.getInstance(ReplayProtectionActivity.this).getTxInfo(strTxHash1);
                    Log.d("ReplayProtectionA", "tx1 status:" + txObj1.toString());
                    boolean tx1Status = btcTxSeen(txObj1);
                    Log.d("ReplayProtectionA", "tx1 status:" + tx1Status);
                    if(tx1Status)    {

                        if(PrefsUtil.getInstance(ReplayProtectionActivity.this).getValue(PrefsUtil.BCC_REPLAY1, "").length() == 0)    {
                            PrefsUtil.getInstance(ReplayProtectionActivity.this).setValue(PrefsUtil.BCC_REPLAY0, strTxHash0);
                            PrefsUtil.getInstance(ReplayProtectionActivity.this).setValue(PrefsUtil.BCC_REPLAY1, strTxHash1);
                        }

                        handler.post(new Runnable() {
                            public void run() {
                                tvBitcoin2.setText(getText(R.string.replay_seen_on_network) + ", " + getText(R.string.replay_confirmations) + "0/6");
                                tvBitcoin3.setText(strTxHash1.substring(0, 30) + "...");
                            }
                        });
                    }

                    if(tx0Status && tx1Status)    {
                        handler.post(new Runnable() {
                            public void run() {
                                Toast.makeText(ReplayProtectionActivity.this, R.string.replay_in_progress, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    rbfHash = strTxHash1;

                }
                else    {
                    rbfHash = PrefsUtil.getInstance(ReplayProtectionActivity.this).getValue(PrefsUtil.BCC_REPLAY1, "");
                }

                boolean tx1Confirmed = false;
                JSONObject txObj1 = null;
                final String _strTxHash1 = rbfHash;
                if(_strTxHash1 != null && _strTxHash1.length() > 0)    {
                    txObj1 = APIFactory.getInstance(ReplayProtectionActivity.this).getTxInfo(_strTxHash1);
                    Log.d("ReplayProtectionA", "tx1 status:" + txObj1.toString());
                    tx1Confirmed = btcTxConfirmed(txObj1);
                    Log.d("ReplayProtectionA", "tx1 confirmed:" + tx1Confirmed);
                }

                if(tx1Confirmed)    {

                    final int blockHeight = btcTxHeight(txObj1);

                    if(blockHeight != -1)    {

                        final int latestBlockHeight = (int)APIFactory.getInstance(ReplayProtectionActivity.this).getLatestBlockHeight();

                        handler.post(new Runnable() {
                            public void run() {
                                int cf = (latestBlockHeight - blockHeight) + 1;
                                if(cf >= 6)    {
                                    ((TextView)layoutAlert.findViewById(R.id.right)).setText(getText(R.string.replay_protected));
                                    layoutAlert.setBackgroundColor(COLOR_GREEN);
                                    layoutShitcoin.setVisibility(View.INVISIBLE);
                                    tvBitcoin2.setText(getText(R.string.replay_confirmed));
                                    tvBitcoin3.setText(_strTxHash1.substring(0, 30) + "...");
                                }
                                else    {
                                    tvBitcoin2.setText(getText(R.string.replay_waiting_for_confirmations) + " " + cf + "/6");
                                    tvBitcoin3.setText(_strTxHash1.substring(0, 30) + "...");
                                }
                            }
                        });

                    }

                }
                else    {
                    handler.post(new Runnable() {
                        public void run() {
                            tvBitcoin2.setText(getText(R.string.replay_seen_on_network) + ", " + getText(R.string.replay_confirmations) + "0/6");
                            Toast.makeText(ReplayProtectionActivity.this, R.string.replay_awaiting_confirmation, Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                Looper.loop();

            }

        }).start();

    }

    private int btcTxHeight(JSONObject txObj)    {

        try {
            if(txObj != null && txObj.has("block"))    {
                JSONObject blockObj = txObj.getJSONObject("block");
                if(blockObj.has("height") && blockObj.getInt("height") > 0)    {
                    return blockObj.getInt("height");
                }
                else    {
                    return -1;
                }
            }
            else    {
                return -1;
            }
        }
        catch(JSONException je) {
            return -1;
        }

    }

    private boolean btcTxConfirmed(JSONObject txObj)    {

        try {
            if(txObj != null && txObj.has("block"))    {
                JSONObject blockObj = txObj.getJSONObject("block");
                if(blockObj.has("height") && blockObj.getInt("height") > 0)    {
                    return true;
                }
                else    {
                    return false;
                }
            }
            else    {
                return false;
            }
        }
        catch(JSONException je) {
            return false;
        }

    }

    private boolean btcTxSeen(JSONObject txObj)    {

        if(txObj != null && txObj.has("txid"))    {
            return true;
        }
        else    {
            return false;
        }

    }

    private boolean bccTxConfirmed(String data)    {

        try {
            JSONObject bccTxObj = new JSONObject(data);
            if(bccTxObj.has("confirmations"))    {
                int confirmations = bccTxObj.getInt("confirmations");
                if(confirmations > 0)    {
                    return true;
                }
                else    {
                    return false;
                }
            }
            else    {
                return false;
            }
        }
        catch(JSONException je) {
            return false;
        }

    }

    private boolean bccTxSeen(String data)    {

        try {
            JSONObject bccTxObj = new JSONObject(data);
            if(bccTxObj.has("scriptPubKey"))    {
                return true;
            }
            else    {
                return false;
            }
        }
        catch(JSONException je) {
            return false;
        }

    }

}
