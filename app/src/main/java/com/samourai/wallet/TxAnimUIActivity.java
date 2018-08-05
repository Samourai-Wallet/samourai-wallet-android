package com.samourai.wallet;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
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
import com.samourai.wallet.send.UTXOFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.BatchSendUtil;
import com.samourai.wallet.util.MonetaryUtil;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.SendAddressUtil;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

public class TxAnimUIActivity extends AppCompatActivity {

    private TransactionProgressView progressView = null;

    private int arcdelay = 800;
    private long signDelay = 2000L;
    private long broadcastDelay = 1599L;
    private long resultDelay = 1500L;

    private Handler resultHandler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tx_anim_ui);

        progressView = findViewById(R.id.transactionProgressView);
        progressView.reset();
        progressView.setTxStatusMessage(R.string.tx_creating_ok);
        progressView.getmArcProgress().startArc1(arcdelay);

        // make tx
        final Transaction tx = SendFactory.getInstance(TxAnimUIActivity.this).makeTransaction(0, SendParams.getInstance().getOutpoints(), SendParams.getInstance().getReceivers());
        if(tx == null)    {
            failTx(R.string.tx_creating_ko);
        }
        else    {
//            Toast.makeText(TxAnimUIActivity.this, "tx created OK", Toast.LENGTH_SHORT).show();

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
                    progressView.setTxStatusMessage(R.string.tx_signing_ok);

                    final Transaction _tx = SendFactory.getInstance(TxAnimUIActivity.this).signTransaction(tx);
                    if(_tx == null)    {
                        failTx(R.string.tx_signing_ko);
                    }
                    else    {
//                    Toast.makeText(TxAnimUIActivity.this, "tx signed OK", Toast.LENGTH_SHORT).show();
                    }
                    final String hexTx = new String(Hex.encode(_tx.bitcoinSerialize()));
                    Log.d("TxAnimUIActivity", "hex tx:" + hexTx);
                    final String strTxHash = _tx.getHashAsString();

                    resultHandler = new Handler();

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            progressView.getmArcProgress().startArc3(arcdelay);
                            progressView.setTxStatusMessage(R.string.tx_broadcast_ok);

                            if(PrefsUtil.getInstance(TxAnimUIActivity.this).getValue(PrefsUtil.BROADCAST_TX, true) == false)    {

                                doShowTx(hexTx, strTxHash);

                                return;

                            }

                            if(AppUtil.getInstance(TxAnimUIActivity.this).isOfflineMode())    {

                                offlineTx(R.string.offline_mode, hexTx, strTxHash);

                                return;

                            }

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
                                                        failTx(R.string.tx_broadcast_ko);
                                                    }
                                                }
                                            } else {
                                                Toast.makeText(TxAnimUIActivity.this, R.string.trusted_node_not_valid, Toast.LENGTH_SHORT).show();
                                                failTx(R.string.tx_broadcast_ko);
                                            }
                                        } else {

                                            if (response != null) {
                                                JSONObject jsonObject = new org.json.JSONObject(response);
                                                if (jsonObject.has("status")) {
                                                    if (jsonObject.getString("status").equals("ok")) {
                                                        isOK = true;
                                                    }
                                                }
                                            } else {
                                                Toast.makeText(TxAnimUIActivity.this, R.string.pushtx_returns_null, Toast.LENGTH_SHORT).show();
                                                failTx(R.string.tx_broadcast_ko);
                                            }
                                        }
                                    }
                                    catch(JSONException je) {
                                        failTx(R.string.tx_broadcast_ko);
                                    }

                                    final boolean _isOK = isOK;

                                    resultHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            if(_isOK)    {
                                                progressView.showCheck();
                                                progressView.setTxStatusMessage(R.string.tx_sent_ok);
                                            }
                                            else    {
                                                failTx(R.string.tx_sent_ko);
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

    }

    private void failTx(int id)   {
        progressView.reset();

        progressView.offlineMode(1200);
        progressView.setTxStatusMessage(R.string.tx_failed);
        progressView.setTxSubText(id);
//        progressView.setTxSubText(R.string.tx_connectivity_failure_msg);
//        progressView.toggleOfflineButton();
    }

    private void offlineTx(int id, final String hex, final String hash)   {
        progressView.reset();

        progressView.offlineMode(1200);
        progressView.setTxStatusMessage(R.string.tx_standby);
        progressView.setTxSubText(id);
        progressView.setTxSubText(R.string.in_offline_mode);
        progressView.toggleOfflineButton();

        progressView.getShowQRButton().setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doShowTx(hex, hash);
            }
        });

        progressView.getTxTennaButton().setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(TxAnimUIActivity.this, R.string.tx_tenna_coming_soon, Toast.LENGTH_SHORT).show();
            }
        });

    }

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
                }
                else if(SendParams.getInstance().getBatchSend() != null)   {

                    for(BatchSendUtil.BatchSend d : SendParams.getInstance().getBatchSend())   {
                        String address = d.addr;
                        String pcode = d.pcode;
                        // increment counter if BIP47 spend
                        if(pcode != null && pcode.length() > 0)    {
                            BIP47Meta.getInstance().getPCode4AddrLookup().put(address, pcode);
                            BIP47Meta.getInstance().inc(pcode);

                            SimpleDateFormat sd = new SimpleDateFormat("dd MMM");
                            String strTS = sd.format(System.currentTimeMillis());
                            String event = strTS + " " + TxAnimUIActivity.this.getString(R.string.sent) + " " + MonetaryUtil.getInstance().getBTCFormat().format((double) d.amount / 1e8) + " BTC";
                            BIP47Meta.getInstance().setLatestEvent(pcode, event);

                        }
                    }

                }
                else    {
                    ;
                }

                if (SendParams.getInstance().hasPrivacyWarning() && SendParams.getInstance().hasPrivacyChecked()) {
                    SendAddressUtil.getInstance().add(SendParams.getInstance().getDestAddress(), false);
                } else if (SendAddressUtil.getInstance().get(SendParams.getInstance().getDestAddress()) == 0) {
                    SendAddressUtil.getInstance().add(SendParams.getInstance().getDestAddress(), false);
                } else {
                    SendAddressUtil.getInstance().add(SendParams.getInstance().getDestAddress(), true);
                }

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

    private void doShowTx(final String hexTx, final String txHash) {

        final int QR_ALPHANUM_CHAR_LIMIT = 4296;    // tx max size in bytes == 2148

        TextView showTx = new TextView(TxAnimUIActivity.this);
        showTx.setText(hexTx);
        showTx.setTextIsSelectable(true);
        showTx.setPadding(40, 10, 40, 10);
        showTx.setTextSize(18.0f);

        final CheckBox cbMarkInputsUnspent = new CheckBox(TxAnimUIActivity.this);
        cbMarkInputsUnspent.setText(R.string.mark_inputs_as_unspendable);
        cbMarkInputsUnspent.setChecked(false);

        LinearLayout hexLayout = new LinearLayout(TxAnimUIActivity.this);
        hexLayout.setOrientation(LinearLayout.VERTICAL);
        hexLayout.addView(cbMarkInputsUnspent);
        hexLayout.addView(showTx);

        new AlertDialog.Builder(TxAnimUIActivity.this)
                .setTitle(txHash)
                .setView(hexLayout)
                .setCancelable(false)
                .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        if(cbMarkInputsUnspent.isChecked())    {
                            UTXOFactory.getInstance(TxAnimUIActivity.this).markUTXOAsUnspendable(hexTx);
                            Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
                            intent.putExtra("notifTx", false);
                            intent.putExtra("fetch", true);
                            LocalBroadcastManager.getInstance(TxAnimUIActivity.this).sendBroadcast(intent);
                        }

                        dialog.dismiss();
                        TxAnimUIActivity.this.finish();

                    }
                })
                .setNeutralButton(R.string.copy_to_clipboard, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager)TxAnimUIActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip = null;
                        clip = android.content.ClipData.newPlainText("TX", hexTx);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(TxAnimUIActivity.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();

                    }
                })
                .setNegativeButton(R.string.show_qr, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        if(cbMarkInputsUnspent.isChecked())    {
                            UTXOFactory.getInstance(TxAnimUIActivity.this).markUTXOAsUnspendable(hexTx);
                            Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
                            intent.putExtra("notifTx", false);
                            intent.putExtra("fetch", true);
                            LocalBroadcastManager.getInstance(TxAnimUIActivity.this).sendBroadcast(intent);
                        }

                        if(hexTx.length() <= QR_ALPHANUM_CHAR_LIMIT)    {

                            final ImageView ivQR = new ImageView(TxAnimUIActivity.this);

                            Display display = (TxAnimUIActivity.this).getWindowManager().getDefaultDisplay();
                            Point size = new Point();
                            display.getSize(size);
                            int imgWidth = Math.max(size.x - 240, 150);

                            Bitmap bitmap = null;

                            QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(hexTx, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), imgWidth);

                            try {
                                bitmap = qrCodeEncoder.encodeAsBitmap();
                            } catch (WriterException e) {
                                e.printStackTrace();
                            }

                            ivQR.setImageBitmap(bitmap);

                            LinearLayout qrLayout = new LinearLayout(TxAnimUIActivity.this);
                            qrLayout.setOrientation(LinearLayout.VERTICAL);
                            qrLayout.addView(ivQR);

                            new AlertDialog.Builder(TxAnimUIActivity.this)
                                    .setTitle(txHash)
                                    .setView(qrLayout)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            dialog.dismiss();
                                            TxAnimUIActivity.this.finish();

                                        }
                                    })
                                    .setNegativeButton(R.string.share_qr, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            String strFileName = AppUtil.getInstance(TxAnimUIActivity.this).getReceiveQRFilename();
                                            File file = new File(strFileName);
                                            if(!file.exists()) {
                                                try {
                                                    file.createNewFile();
                                                }
                                                catch(Exception e) {
                                                    Toast.makeText(TxAnimUIActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                            file.setReadable(true, false);

                                            FileOutputStream fos = null;
                                            try {
                                                fos = new FileOutputStream(file);
                                            }
                                            catch(FileNotFoundException fnfe) {
                                                ;
                                            }

                                            if(file != null && fos != null) {
                                                Bitmap bitmap = ((BitmapDrawable)ivQR.getDrawable()).getBitmap();
                                                bitmap.compress(Bitmap.CompressFormat.PNG, 0, fos);

                                                try {
                                                    fos.close();
                                                }
                                                catch(IOException ioe) {
                                                    ;
                                                }

                                                Intent intent = new Intent();
                                                intent.setAction(Intent.ACTION_SEND);
                                                intent.setType("image/png");
                                                if (android.os.Build.VERSION.SDK_INT >= 24) {
                                                    //From API 24 sending FIle on intent ,require custom file provider
                                                    intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                                                            TxAnimUIActivity.this,
                                                            getApplicationContext()
                                                                    .getPackageName() + ".provider", file));
                                                } else {
                                                    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                                                }
                                                startActivity(Intent.createChooser(intent, TxAnimUIActivity.this.getText(R.string.send_tx)));
                                            }

                                        }
                                    }).show();
                        }
                        else    {

                            Toast.makeText(TxAnimUIActivity.this, R.string.tx_too_large_qr, Toast.LENGTH_SHORT).show();

                        }

                    }
                }).show();

    }

}
