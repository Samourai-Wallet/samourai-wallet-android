package com.samourai.wallet;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.samourai.wallet.JSONRPC.TrustedNodeUtil;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.segwit.BIP49Util;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.segwit.bech32.Bech32Util;
import com.samourai.wallet.send.PushTx;
import com.samourai.wallet.send.RBFSpend;
import com.samourai.wallet.send.RBFUtil;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.SendParams;
import com.samourai.wallet.util.MonetaryUtil;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.SendAddressUtil;
import com.samourai.wallet.widgets.ArcProgress;
import com.samourai.wallet.widgets.TransactionProgressView;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.MnemonicException;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.util.encoders.DecoderException;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.text.SimpleDateFormat;

public class TxAnimUIActivity extends AppCompatActivity {

    private ArcProgress progress = null;
    private TransactionProgressView progressView = null;

    private boolean showSuccess = false;

    private int arcdelay = 800;
    private long signDelay = 2000L;
    private long broadcastDelay = 1599L;
    private long resultDelay = 1500L;

    private Handler resultHandler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_ui2);

        progressView = findViewById(R.id.transactionProgressView);

        /*
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
        */

        progressView.reset();

        progressView.setTxStatusMessage("Creating transaction...");
        progressView.getmArcProgress().startArc1(arcdelay);

        // make tx
        final Transaction tx = SendFactory.getInstance(TxAnimUIActivity.this).makeTransaction(0, SendParams.getInstance().getOutpoints(), SendParams.getInstance().getReceivers());

        if(tx != null)    {
            Toast.makeText(TxAnimUIActivity.this, "tx created OK", Toast.LENGTH_SHORT).show();
        }

        final RBFSpend rbf;
        if (PrefsUtil.getInstance(TxAnimUIActivity.this).getValue(PrefsUtil.RBF_OPT_IN, false) == true) {

            rbf = new RBFSpend();

            for (TransactionInput input : tx.getInputs()) {

                boolean _isBIP49 = false;
                boolean _isBIP84 = false;
                String _addr = null;
                String script = Hex.toHexString(input.getConnectedOutput().getScriptBytes());
                if (Bech32Util.getInstance().isBech32Script(script)) {
                    try {
                        _addr = Bech32Util.getInstance().getAddressFromScript(script);
                        _isBIP84 = true;
                    } catch (Exception e) {
                        ;
                    }
                } else {
                    Address _address = input.getConnectedOutput().getAddressFromP2SH(SamouraiWallet.getInstance().getCurrentNetworkParams());
                    if (_address != null) {
                        _addr = _address.toString();
                        _isBIP49 = true;
                    }
                }
                if (_addr == null) {
                    _addr = input.getConnectedOutput().getAddressFromP2PKHScript(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
                }

                String path = APIFactory.getInstance(TxAnimUIActivity.this).getUnspentPaths().get(_addr);
                if (path != null) {
                    if (_isBIP84) {
                        rbf.addKey(input.getOutpoint().toString(), path + "/84");
                    } else if (_isBIP49) {
                        rbf.addKey(input.getOutpoint().toString(), path + "/49");
                    } else {
                        rbf.addKey(input.getOutpoint().toString(), path);
                    }
                } else {
                    String pcode = BIP47Meta.getInstance().getPCode4Addr(_addr);
                    int idx = BIP47Meta.getInstance().getIdx4Addr(_addr);
                    rbf.addKey(input.getOutpoint().toString(), pcode + "/" + idx);
                }

            }

        } else {
            rbf = null;
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                progressView.getmArcProgress().startArc2(arcdelay);
                progressView.setTxStatusMessage("signing transaction...");

                final Transaction _tx = SendFactory.getInstance(TxAnimUIActivity.this).signTransaction(tx);
                if(_tx != null)    {
                    Toast.makeText(TxAnimUIActivity.this, "tx signed OK", Toast.LENGTH_SHORT).show();
                }
                final String hexTx = new String(Hex.encode(_tx.bitcoinSerialize()));
                Log.d("TxAnimUIActivity", "hex tx:" + hexTx);
                final String strTxHash = _tx.getHashAsString();

                resultHandler = new Handler();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        progressView.getmArcProgress().startArc3(arcdelay);
                        progressView.setTxStatusMessage("broadcasting transaction...");

                        new Thread(new Runnable() {
                            @Override
                            public void run() {

                                Looper.prepare();

                                boolean isOK = false;
                                String response = PushTx.getInstance(TxAnimUIActivity.this).samourai(hexTx);
                                try {
                                    if (PrefsUtil.getInstance(TxAnimUIActivity.this).getValue(PrefsUtil.USE_TRUSTED_NODE, false) == true) {
                                        if (TrustedNodeUtil.getInstance().isSet()) {
                                            response = PushTx.getInstance(TxAnimUIActivity.this).trustedNode(hexTx);
                                            JSONObject jsonObject = new org.json.JSONObject(response);
                                            if (jsonObject.has("result")) {
                                                if (jsonObject.getString("result").matches("^[A-Za-z0-9]{64}$")) {
                                                    isOK = true;
                                                } else {
                                                    Toast.makeText(TxAnimUIActivity.this, R.string.trusted_node_tx_error, Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        } else {
                                            Toast.makeText(TxAnimUIActivity.this, R.string.trusted_node_not_valid, Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        response = PushTx.getInstance(TxAnimUIActivity.this).samourai(hexTx);

                                        if (response != null) {
                                            JSONObject jsonObject = new org.json.JSONObject(response);
                                            if (jsonObject.has("status")) {
                                                if (jsonObject.getString("status").equals("ok")) {
                                                    isOK = true;
                                                }
                                            }
                                        } else {
                                            Toast.makeText(TxAnimUIActivity.this, R.string.pushtx_returns_null, Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                                catch(JSONException je) {
                                    ;
                                }

                                final boolean _isOK = isOK;

                                resultHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(_isOK)    {
                                            progressView.showCheck();
                                            progressView.setTxStatusMessage("transaction sent");
                                        }
                                        else    {
                                            progressView.offlineMode(1200);
                                            progressView.setTxStatusMessage("transaction signed....");
                                            progressView.setTxSubText(R.string.tx_connectivity_failure_msg);
                                            progressView.toggleOfflineButton();
                                        }

                                        handleResult(_isOK, rbf, strTxHash, hexTx, _tx);

                                        }

                                }, resultDelay);

                                Looper.loop();

                            }

                        }).start();

                    }

                }, broadcastDelay);

            }

        }, signDelay);

    }

/*
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
*/
/*
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
*/
    public void stub() {

        /*

        // make tx
        Transaction tx = SendFactory.getInstance(TxAnimUIActivity.this).makeTransaction(0, outPoints, receivers);

        final RBFSpend rbf;
        if (PrefsUtil.getInstance(TxAnimUIActivity.this).getValue(PrefsUtil.RBF_OPT_IN, false) == true) {

            rbf = new RBFSpend();

            for (TransactionInput input : tx.getInputs()) {

                boolean _isBIP49 = false;
                boolean _isBIP84 = false;
                String _addr = null;
                String script = Hex.toHexString(input.getConnectedOutput().getScriptBytes());
                if (Bech32Util.getInstance().isBech32Script(script)) {
                    try {
                        _addr = Bech32Util.getInstance().getAddressFromScript(script);
                        _isBIP84 = true;
                    } catch (Exception e) {
                        ;
                    }
                } else {
                    Address _address = input.getConnectedOutput().getAddressFromP2SH(SamouraiWallet.getInstance().getCurrentNetworkParams());
                    if (_address != null) {
                        _addr = _address.toString();
                        _isBIP49 = true;
                    }
                }
                if (_addr == null) {
                    _addr = input.getConnectedOutput().getAddressFromP2PKHScript(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
                }

                String path = APIFactory.getInstance(TxAnimUIActivity.this).getUnspentPaths().get(_addr);
                if (path != null) {
                    if (_isBIP84) {
                        rbf.addKey(input.getOutpoint().toString(), path + "/84");
                    } else if (_isBIP49) {
                        rbf.addKey(input.getOutpoint().toString(), path + "/49");
                    } else {
                        rbf.addKey(input.getOutpoint().toString(), path);
                    }
                } else {
                    String pcode = BIP47Meta.getInstance().getPCode4Addr(_addr);
                    int idx = BIP47Meta.getInstance().getIdx4Addr(_addr);
                    rbf.addKey(input.getOutpoint().toString(), pcode + "/" + idx);
                }

            }

        } else {
            rbf = null;
        }

        if (tx != null) {
            tx = SendFactory.getInstance(TxAnimUIActivity.this).signTransaction(tx);
            final Transaction _tx = tx;
            final String hexTx = new String(Hex.encode(tx.bitcoinSerialize()));
//                                Log.d("SendActivity", hexTx);
            final String strTxHash = tx.getHashAsString();

            if (PrefsUtil.getInstance(TxAnimUIActivity.this).getValue(PrefsUtil.BROADCAST_TX, true) == false) {

                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                }

                doShowTx(hexTx, strTxHash);

                return;

            }

            new Thread(new Runnable() {
                @Override
                public void run() {

                    Looper.prepare();

                    boolean isOK = false;
                    String response = null;
                    try {
                        if (PrefsUtil.getInstance(TxAnimUIActivity.this).getValue(PrefsUtil.USE_TRUSTED_NODE, false) == true) {
                            if (TrustedNodeUtil.getInstance().isSet()) {
                                response = PushTx.getInstance(TxAnimUIActivity.this).trustedNode(hexTx);
                                JSONObject jsonObject = new org.json.JSONObject(response);
                                if (jsonObject.has("result")) {
                                    if (jsonObject.getString("result").matches("^[A-Za-z0-9]{64}$")) {
                                        isOK = true;
                                    } else {
                                        Toast.makeText(TxAnimUIActivity.this, R.string.trusted_node_tx_error, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            } else {
                                Toast.makeText(TxAnimUIActivity.this, R.string.trusted_node_not_valid, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            response = PushTx.getInstance(TxAnimUIActivity.this).samourai(hexTx);

                            if (response != null) {
                                JSONObject jsonObject = new org.json.JSONObject(response);
                                if (jsonObject.has("status")) {
                                    if (jsonObject.getString("status").equals("ok")) {
                                        isOK = true;
                                    }
                                }
                            } else {
                                Toast.makeText(TxAnimUIActivity.this, R.string.pushtx_returns_null, Toast.LENGTH_SHORT).show();
                            }
                        }

                        if (isOK) {
                            if (PrefsUtil.getInstance(TxAnimUIActivity.this).getValue(PrefsUtil.USE_TRUSTED_NODE, false) == false) {
                                Toast.makeText(TxAnimUIActivity.this, R.string.tx_sent, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(TxAnimUIActivity.this, R.string.trusted_node_tx_sent, Toast.LENGTH_SHORT).show();
                            }

                            if (_change > 0L && SPEND_TYPE == SPEND_SIMPLE) {

                                if (changeType == 84) {
                                    BIP84Util.getInstance(TxAnimUIActivity.this).getWallet().getAccount(0).getChange().incAddrIdx();
                                } else if (changeType == 49) {
                                    BIP49Util.getInstance(TxAnimUIActivity.this).getWallet().getAccount(0).getChange().incAddrIdx();
                                } else {
                                    try {
                                        HD_WalletFactory.getInstance(TxAnimUIActivity.this).get().getAccount(0).getChange().incAddrIdx();
                                    } catch (IOException ioe) {
                                        ;
                                    } catch (MnemonicException.MnemonicLengthException mle) {
                                        ;
                                    }
                                }
                            }

                            if (PrefsUtil.getInstance(TxAnimUIActivity.this).getValue(PrefsUtil.RBF_OPT_IN, false) == true) {

                                for (TransactionOutput out : _tx.getOutputs()) {
                                    try {
                                        if (Bech32Util.getInstance().isBech32Script(Hex.toHexString(out.getScriptBytes())) && !address.equals(Bech32Util.getInstance().getAddressFromScript(Hex.toHexString(out.getScriptBytes())))) {
                                            rbf.addChangeAddr(Bech32Util.getInstance().getAddressFromScript(Hex.toHexString(out.getScriptBytes())));
                                            Log.d("SendActivity", "added change output:" + Bech32Util.getInstance().getAddressFromScript(Hex.toHexString(out.getScriptBytes())));
                                        } else if (changeType == 44 && !address.equals(out.getAddressFromP2PKHScript(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString())) {
                                            rbf.addChangeAddr(out.getAddressFromP2PKHScript(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
                                            Log.d("SendActivity", "added change output:" + out.getAddressFromP2PKHScript(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
                                        } else if (changeType != 44 && !address.equals(out.getAddressFromP2SH(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString())) {
                                            rbf.addChangeAddr(out.getAddressFromP2SH(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
                                            Log.d("SendActivity", "added change output:" + out.getAddressFromP2SH(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
                                        } else {
                                            ;
                                        }
                                    } catch (NullPointerException npe) {
                                        ;
                                    } catch (Exception e) {
                                        ;
                                    }
                                }

                                rbf.setHash(strTxHash);
                                rbf.setSerializedTx(hexTx);

                                RBFUtil.getInstance().add(rbf);
                            }

                            // increment counter if BIP47 spend
                            if (strPCode != null && strPCode.length() > 0) {
                                BIP47Meta.getInstance().getPCode4AddrLookup().put(address, strPCode);
                                BIP47Meta.getInstance().inc(strPCode);

                                SimpleDateFormat sd = new SimpleDateFormat("dd MMM");
                                String strTS = sd.format(currentTimeMillis());
                                String event = strTS + " " + TxAnimUIActivity.this.getString(R.string.sent) + " " + MonetaryUtil.getInstance().getBTCFormat().format((double) _amount / 1e8) + " BTC";
                                BIP47Meta.getInstance().setLatestEvent(strPCode, event);

                                strPCode = null;
                            }

                            if (strPrivacyWarning.length() > 0 && cbShowAgain != null) {
                                SendAddressUtil.getInstance().add(address, cbShowAgain.isChecked() ? false : true);
                            } else if (SendAddressUtil.getInstance().get(address) == 0) {
                                SendAddressUtil.getInstance().add(address, false);
                            } else {
                                SendAddressUtil.getInstance().add(address, true);
                            }

                            if (_change == 0L) {
                                Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
                                intent.putExtra("notifTx", false);
                                intent.putExtra("fetch", true);
                                LocalBroadcastManager.getInstance(TxAnimUIActivity.this).sendBroadcast(intent);
                            }

                            View view = TxAnimUIActivity.this.getCurrentFocus();
                            if (view != null) {
                                InputMethodManager imm = (InputMethodManager) TxAnimUIActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                            }

                            if (bViaMenu) {
                                TxAnimUIActivity.this.finish();
                            } else {
                                Intent _intent = new Intent(TxAnimUIActivity.this, BalanceActivity.class);
                                _intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                startActivity(_intent);
                            }

                        } else {
                            Toast.makeText(TxAnimUIActivity.this, R.string.tx_failed, Toast.LENGTH_SHORT).show();
                            // reset change index upon tx fail
                            if (changeType == 84) {
                                BIP84Util.getInstance(TxAnimUIActivity.this).getWallet().getAccount(0).getChange().setAddrIdx(_change_index);
                            } else if (changeType == 49) {
                                BIP49Util.getInstance(TxAnimUIActivity.this).getWallet().getAccount(0).getChange().setAddrIdx(_change_index);
                            } else {
                                HD_WalletFactory.getInstance(TxAnimUIActivity.this).get().getAccount(0).getChange().setAddrIdx(_change_index);
                            }
                        }
                    } catch (JSONException je) {
                        Toast.makeText(TxAnimUIActivity.this, "pushTx:" + je.getMessage(), Toast.LENGTH_SHORT).show();
                    } catch (MnemonicException.MnemonicLengthException mle) {
                        Toast.makeText(TxAnimUIActivity.this, "pushTx:" + mle.getMessage(), Toast.LENGTH_SHORT).show();
                    } catch (DecoderException de) {
                        Toast.makeText(TxAnimUIActivity.this, "pushTx:" + de.getMessage(), Toast.LENGTH_SHORT).show();
                    } catch (IOException ioe) {
                        Toast.makeText(TxAnimUIActivity.this, "pushTx:" + ioe.getMessage(), Toast.LENGTH_SHORT).show();
                    } finally {
                        TxAnimUIActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                btSend.setActivated(true);
                                btSend.setClickable(true);
                                progress.dismiss();
                                dialog.dismiss();
                            }
                        });
                    }

                    Looper.loop();

                }
            }).start();


        } else {
//                                Log.d("SendActivity", "tx error");
            Toast.makeText(TxAnimUIActivity.this, "tx error", Toast.LENGTH_SHORT).show();
        }

        */

    }
    
    private void handleResult(boolean isOK, RBFSpend rbf, String strTxHash, String hexTx, Transaction _tx)  {

        try {
            if (isOK) {
                if (PrefsUtil.getInstance(TxAnimUIActivity.this).getValue(PrefsUtil.USE_TRUSTED_NODE, false) == false) {
                    Toast.makeText(TxAnimUIActivity.this, R.string.tx_sent, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(TxAnimUIActivity.this, R.string.trusted_node_tx_sent, Toast.LENGTH_SHORT).show();
                }

                if (SendParams.getInstance().getChangeAmount() > 0L && SendParams.getInstance().getSpendType() == SendActivity.SPEND_SIMPLE) {

                    if (SendParams.getInstance().getChangeType() == 84) {
                        BIP84Util.getInstance(TxAnimUIActivity.this).getWallet().getAccount(0).getChange().incAddrIdx();
                    } else if (SendParams.getInstance().getChangeType() == 49) {
                        BIP49Util.getInstance(TxAnimUIActivity.this).getWallet().getAccount(0).getChange().incAddrIdx();
                    } else {
                        try {
                            HD_WalletFactory.getInstance(TxAnimUIActivity.this).get().getAccount(0).getChange().incAddrIdx();
                        } catch (IOException ioe) {
                            ;
                        } catch (MnemonicException.MnemonicLengthException mle) {
                            ;
                        }
                    }
                }

                if (PrefsUtil.getInstance(TxAnimUIActivity.this).getValue(PrefsUtil.RBF_OPT_IN, false) == true) {

                    for (TransactionOutput out : _tx.getOutputs()) {
                        try {
                            if (Bech32Util.getInstance().isBech32Script(Hex.toHexString(out.getScriptBytes())) && !SendParams.getInstance().getDestAddress().equals(Bech32Util.getInstance().getAddressFromScript(Hex.toHexString(out.getScriptBytes())))) {
                                rbf.addChangeAddr(Bech32Util.getInstance().getAddressFromScript(Hex.toHexString(out.getScriptBytes())));
                                Log.d("SendActivity", "added change output:" + Bech32Util.getInstance().getAddressFromScript(Hex.toHexString(out.getScriptBytes())));
                            } else if (SendParams.getInstance().getChangeType() == 44 && !SendParams.getInstance().getDestAddress().equals(out.getAddressFromP2PKHScript(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString())) {
                                rbf.addChangeAddr(out.getAddressFromP2PKHScript(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
                                Log.d("SendActivity", "added change output:" + out.getAddressFromP2PKHScript(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
                            } else if (SendParams.getInstance().getChangeType() != 44 && !SendParams.getInstance().getDestAddress().equals(out.getAddressFromP2SH(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString())) {
                                rbf.addChangeAddr(out.getAddressFromP2SH(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
                                Log.d("SendActivity", "added change output:" + out.getAddressFromP2SH(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
                            } else {
                                ;
                            }
                        } catch (NullPointerException npe) {
                            ;
                        } catch (Exception e) {
                            ;
                        }
                    }

                    rbf.setHash(strTxHash);
                    rbf.setSerializedTx(hexTx);

                    RBFUtil.getInstance().add(rbf);
                }

                // increment counter if BIP47 spend
                if (SendParams.getInstance().getPCode() != null && SendParams.getInstance().getPCode().length() > 0) {
                    BIP47Meta.getInstance().getPCode4AddrLookup().put(SendParams.getInstance().getDestAddress(), SendParams.getInstance().getPCode());
                    BIP47Meta.getInstance().inc(SendParams.getInstance().getPCode());

                    SimpleDateFormat sd = new SimpleDateFormat("dd MMM");
                    String strTS = sd.format(System.currentTimeMillis());
                    String event = strTS + " " + TxAnimUIActivity.this.getString(R.string.sent) + " " + MonetaryUtil.getInstance().getBTCFormat().format((double) SendParams.getInstance().getSpendAmount() / 1e8) + " BTC";
                    BIP47Meta.getInstance().setLatestEvent(SendParams.getInstance().getPCode(), event);

//                    strPCode = null;
                }

                if (SendParams.getInstance().hasPrivacyWarning() && SendParams.getInstance().hasPrivacyChecked()) {
                    SendAddressUtil.getInstance().add(SendParams.getInstance().getDestAddress(), false);
                } else if (SendAddressUtil.getInstance().get(SendParams.getInstance().getDestAddress()) == 0) {
                    SendAddressUtil.getInstance().add(SendParams.getInstance().getDestAddress(), false);
                } else {
                    SendAddressUtil.getInstance().add(SendParams.getInstance().getDestAddress(), true);
                }

                /*
                if (SendParams.getInstance().getChangeAmount() == 0L) {
                    Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
                    intent.putExtra("notifTx", false);
                    intent.putExtra("fetch", true);
                    LocalBroadcastManager.getInstance(TxAnimUIActivity.this).sendBroadcast(intent);
                }

                View view = TxAnimUIActivity.this.getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) TxAnimUIActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }

                if (bViaMenu) {
                    TxAnimUIActivity.this.finish();
                } else {
                    Intent _intent = new Intent(TxAnimUIActivity.this, BalanceActivity.class);
                    _intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(_intent);
                }

                */

                if (SendParams.getInstance().getChangeAmount() == 0L) {
                    Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
                    intent.putExtra("notifTx", false);
                    intent.putExtra("fetch", true);
                    LocalBroadcastManager.getInstance(TxAnimUIActivity.this).sendBroadcast(intent);
                }

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent _intent = new Intent(TxAnimUIActivity.this, BalanceActivity.class);
                        _intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(_intent);
                    }

                }, 1000L);

            } else {
                Toast.makeText(TxAnimUIActivity.this, R.string.tx_failed, Toast.LENGTH_SHORT).show();
                // reset change index upon tx fail
                if (SendParams.getInstance().getChangeType() == 84) {
                    BIP84Util.getInstance(TxAnimUIActivity.this).getWallet().getAccount(0).getChange().setAddrIdx(SendParams.getInstance().getChangeIdx());
                } else if (SendParams.getInstance().getChangeType() == 49) {
                    BIP49Util.getInstance(TxAnimUIActivity.this).getWallet().getAccount(0).getChange().setAddrIdx(SendParams.getInstance().getChangeIdx());
                } else {
                    HD_WalletFactory.getInstance(TxAnimUIActivity.this).get().getAccount(0).getChange().setAddrIdx(SendParams.getInstance().getChangeIdx());
                }
            }
    } catch (MnemonicException.MnemonicLengthException mle) {
        Toast.makeText(TxAnimUIActivity.this, "pushTx:" + mle.getMessage(), Toast.LENGTH_SHORT).show();
    } catch (DecoderException de) {
        Toast.makeText(TxAnimUIActivity.this, "pushTx:" + de.getMessage(), Toast.LENGTH_SHORT).show();
    } catch (IOException ioe) {
        Toast.makeText(TxAnimUIActivity.this, "pushTx:" + ioe.getMessage(), Toast.LENGTH_SHORT).show();
    } finally {
            ;
    }


    }

}
