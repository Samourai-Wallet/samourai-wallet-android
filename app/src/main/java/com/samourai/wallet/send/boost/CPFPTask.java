package com.samourai.wallet.send.boost;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.samourai.wallet.MainActivity2;
import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.segwit.BIP49Util;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.segwit.bech32.Bech32Util;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.PushTx;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.SuggestedFee;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.service.WebSocketService;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.PrefsUtil;

import org.apache.commons.lang3.tuple.Triple;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.script.Script;
import org.bouncycastle.util.encoders.DecoderException;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class CPFPTask extends AsyncTask<String, Void, String> {

    private Activity activity;
    private List<UTXO> utxos = null;
    private Handler handler = null;

    public CPFPTask(Activity activity) {
        this.activity = activity;
    }

    @Override
    protected void onPreExecute() {
        handler = new Handler();
        utxos = APIFactory.getInstance(activity).getUtxos(true);
    }

    @Override
    protected String doInBackground(String... params) {

        Looper.prepare();

        Log.d("activity", "hash:" + params[0]);

        JSONObject txObj = APIFactory.getInstance(activity).getTxInfo(params[0]);
        if (txObj.has("inputs") && txObj.has("outputs")) {

            final SuggestedFee suggestedFee = FeeUtil.getInstance().getSuggestedFee();

            try {
                JSONArray inputs = txObj.getJSONArray("inputs");
                JSONArray outputs = txObj.getJSONArray("outputs");

                int p2pkh = 0;
                int p2sh_p2wpkh = 0;
                int p2wpkh = 0;

                for (int i = 0; i < inputs.length(); i++) {
                    if (inputs.getJSONObject(i).has("outpoint") && inputs.getJSONObject(i).getJSONObject("outpoint").has("scriptpubkey")) {
                        String scriptpubkey = inputs.getJSONObject(i).getJSONObject("outpoint").getString("scriptpubkey");
                        Script script = new Script(Hex.decode(scriptpubkey));
                        String address = null;
                        if (Bech32Util.getInstance().isBech32Script(scriptpubkey)) {
                            try {
                                address = Bech32Util.getInstance().getAddressFromScript(scriptpubkey);
                            } catch (Exception e) {
                                ;
                            }
                        } else {
                            address = script.getToAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
                        }
                        if (FormatsUtil.getInstance().isValidBech32(address)) {
                            p2wpkh++;
                        } else if (Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress()) {
                            p2sh_p2wpkh++;
                        } else {
                            p2pkh++;
                        }
                    }
                }

                FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getHighFee());
                BigInteger estimatedFee = FeeUtil.getInstance().estimatedFeeSegwit(p2pkh, p2sh_p2wpkh, p2wpkh, outputs.length());

                long total_inputs = 0L;
                long total_outputs = 0L;
                long fee = 0L;

                UTXO utxo = null;

                for (int i = 0; i < inputs.length(); i++) {
                    JSONObject obj = inputs.getJSONObject(i);
                    if (obj.has("outpoint")) {
                        JSONObject objPrev = obj.getJSONObject("outpoint");
                        if (objPrev.has("value")) {
                            total_inputs += objPrev.getLong("value");
                        }
                    }
                }

                for (int i = 0; i < outputs.length(); i++) {
                    JSONObject obj = outputs.getJSONObject(i);
                    if (obj.has("value")) {
                        total_outputs += obj.getLong("value");

                        String addr = obj.getString("address");
                        Log.d("activity", "checking address:" + addr);
                        if (utxo == null) {
                            utxo = getUTXO(addr);
                        } else {
                            break;
                        }
                    }
                }

                boolean feeWarning = false;
                fee = total_inputs - total_outputs;
                if (fee > estimatedFee.longValue()) {
                    feeWarning = true;
                }

                Log.d("activity", "total inputs:" + total_inputs);
                Log.d("activity", "total outputs:" + total_outputs);
                Log.d("activity", "fee:" + fee);
                Log.d("activity", "estimated fee:" + estimatedFee.longValue());
                Log.d("activity", "fee warning:" + feeWarning);
                if (utxo != null) {
                    Log.d("activity", "utxo found");

                    List<UTXO> selectedUTXO = new ArrayList<UTXO>();
                    selectedUTXO.add(utxo);
                    int selected = utxo.getOutpoints().size();

                    long remainingFee = (estimatedFee.longValue() > fee) ? estimatedFee.longValue() - fee : 0L;
                    Log.d("activity", "remaining fee:" + remainingFee);
                    int receiveIdx = AddressFactory.getInstance(activity).getHighestTxReceiveIdx(0);
                    Log.d("activity", "receive index:" + receiveIdx);
                    final String addr;
                    if (PrefsUtil.getInstance(activity).getValue(PrefsUtil.USE_LIKE_TYPED_CHANGE, true) == true) {
                        addr = utxo.getOutpoints().get(0).getAddress();
                    } else {
                        addr = outputs.getJSONObject(0).getString("address");
                    }
                    final String ownReceiveAddr;
                    if (FormatsUtil.getInstance().isValidBech32(addr)) {
                        ownReceiveAddr = AddressFactory.getInstance(activity).getBIP84(AddressFactory.RECEIVE_CHAIN).getBech32AsString();
                    } else if (Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), addr).isP2SHAddress()) {
                        ownReceiveAddr = AddressFactory.getInstance(activity).getBIP49(AddressFactory.RECEIVE_CHAIN).getAddressAsString();
                    } else {
                        ownReceiveAddr = AddressFactory.getInstance(activity).get(AddressFactory.RECEIVE_CHAIN).getAddressString();
                    }
                    Log.d("activity", "receive address:" + ownReceiveAddr);

                    long totalAmount = utxo.getValue();
                    Log.d("activity", "amount before fee:" + totalAmount);
                    Triple<Integer, Integer, Integer> outpointTypes = FeeUtil.getInstance().getOutpointCount(new Vector(utxo.getOutpoints()));
                    BigInteger cpfpFee = FeeUtil.getInstance().estimatedFeeSegwit(outpointTypes.getLeft(), outpointTypes.getMiddle(), outpointTypes.getRight(), 1);
                    Log.d("activity", "cpfp fee:" + cpfpFee.longValue());

                    p2pkh = outpointTypes.getLeft();
                    p2sh_p2wpkh = outpointTypes.getMiddle();
                    p2wpkh = outpointTypes.getRight();

                    if (totalAmount < (cpfpFee.longValue() + remainingFee)) {
                        Log.d("activity", "selecting additional utxo");
                        Collections.sort(utxos, new UTXO.UTXOComparator());
                        for (UTXO _utxo : utxos) {
                            totalAmount += _utxo.getValue();
                            selectedUTXO.add(_utxo);
                            selected += _utxo.getOutpoints().size();
                            outpointTypes = FeeUtil.getInstance().getOutpointCount(new Vector(utxo.getOutpoints()));
                            p2pkh += outpointTypes.getLeft();
                            p2sh_p2wpkh += outpointTypes.getMiddle();
                            p2wpkh += outpointTypes.getRight();
                            cpfpFee = FeeUtil.getInstance().estimatedFeeSegwit(p2pkh, p2sh_p2wpkh, p2wpkh, 1);
                            if (totalAmount > (cpfpFee.longValue() + remainingFee + SamouraiWallet.bDust.longValue())) {
                                break;
                            }
                        }
                        if (totalAmount < (cpfpFee.longValue() + remainingFee + SamouraiWallet.bDust.longValue())) {
                            handler.post(new Runnable() {
                                public void run() {
                                    Toast.makeText(activity, R.string.insufficient_funds, Toast.LENGTH_SHORT).show();
                                }
                            });
                            FeeUtil.getInstance().setSuggestedFee(suggestedFee);
                            return "KO";
                        }
                    }

                    cpfpFee = cpfpFee.add(BigInteger.valueOf(remainingFee));
                    Log.d("activity", "cpfp fee:" + cpfpFee.longValue());

                    final List<MyTransactionOutPoint> outPoints = new ArrayList<MyTransactionOutPoint>();
                    for (UTXO u : selectedUTXO) {
                        outPoints.addAll(u.getOutpoints());
                    }

                    long _totalAmount = 0L;
                    for (MyTransactionOutPoint outpoint : outPoints) {
                        _totalAmount += outpoint.getValue().longValue();
                    }
                    Log.d("activity", "checked total amount:" + _totalAmount);
                    assert (_totalAmount == totalAmount);

                    long amount = totalAmount - cpfpFee.longValue();
                    Log.d("activity", "amount after fee:" + amount);

                    if (amount < SamouraiWallet.bDust.longValue()) {
                        Log.d("activity", "dust output");
                        Toast.makeText(activity, R.string.cannot_output_dust, Toast.LENGTH_SHORT).show();
                    }

                    final HashMap<String, BigInteger> receivers = new HashMap<String, BigInteger>();
                    receivers.put(ownReceiveAddr, BigInteger.valueOf(amount));

                    String message = "";
                    if (feeWarning) {
                        message += activity.getString(R.string.fee_bump_not_necessary);
                        message += "\n\n";
                    }
                    message += activity.getString(R.string.bump_fee) + " " + Coin.valueOf(remainingFee).toPlainString() + " BTC";

                    AlertDialog.Builder dlg = new AlertDialog.Builder(activity)
                            .setTitle(R.string.app_name)
                            .setMessage(message)
                            .setCancelable(false)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {

                                    if (AppUtil.getInstance(activity.getApplicationContext()).isServiceRunning(WebSocketService.class)) {
                                        activity.stopService(new Intent(activity.getApplicationContext(), WebSocketService.class));
                                    }
                                    activity.startService(new Intent(activity.getApplicationContext(), WebSocketService.class));

                                    Transaction tx = SendFactory.getInstance(activity).makeTransaction(0, outPoints, receivers);
                                    if (tx != null) {
                                        tx = SendFactory.getInstance(activity).signTransaction(tx);
                                        final String hexTx = new String(Hex.encode(tx.bitcoinSerialize()));
                                        Log.d("activity", hexTx);

                                        final String strTxHash = tx.getHashAsString();
                                        Log.d("activity", strTxHash);

                                        boolean isOK = false;
                                        try {

                                            isOK = PushTx.getInstance(activity).pushTx(hexTx);

                                            if (isOK) {

                                                handler.post(new Runnable() {
                                                    public void run() {
                                                        Toast.makeText(activity, R.string.cpfp_spent, Toast.LENGTH_SHORT).show();

                                                        FeeUtil.getInstance().setSuggestedFee(suggestedFee);

                                                        Intent _intent = new Intent(activity, MainActivity2.class);
                                                        _intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                                        activity.startActivity(_intent);
                                                    }
                                                });

                                            } else {
                                                handler.post(new Runnable() {
                                                    public void run() {
                                                        Toast.makeText(activity, R.string.tx_failed, Toast.LENGTH_SHORT).show();
                                                    }
                                                });

                                                // reset receive index upon tx fail
                                                if (FormatsUtil.getInstance().isValidBech32(addr)) {
                                                    int prevIdx = BIP84Util.getInstance(activity).getWallet().getAccount(0).getReceive().getAddrIdx() - 1;
                                                    BIP84Util.getInstance(activity).getWallet().getAccount(0).getReceive().setAddrIdx(prevIdx);
                                                } else if (Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), addr).isP2SHAddress()) {
                                                    int prevIdx = BIP49Util.getInstance(activity).getWallet().getAccount(0).getReceive().getAddrIdx() - 1;
                                                    BIP49Util.getInstance(activity).getWallet().getAccount(0).getReceive().setAddrIdx(prevIdx);
                                                } else {
                                                    int prevIdx = HD_WalletFactory.getInstance(activity).get().getAccount(0).getReceive().getAddrIdx() - 1;
                                                    HD_WalletFactory.getInstance(activity).get().getAccount(0).getReceive().setAddrIdx(prevIdx);
                                                }

                                            }
                                        } catch (MnemonicException.MnemonicLengthException | DecoderException | IOException e) {
                                            handler.post(new Runnable() {
                                                public void run() {
                                                    Toast.makeText(activity, "pushTx:" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        } finally {
                                            ;
                                        }

                                    }

                                }
                            }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {

                                    try {
                                        if (Bech32Util.getInstance().isBech32Script(addr)) {
                                            int prevIdx = BIP84Util.getInstance(activity).getWallet().getAccount(0).getReceive().getAddrIdx() - 1;
                                            BIP84Util.getInstance(activity).getWallet().getAccount(0).getReceive().setAddrIdx(prevIdx);
                                        } else if (Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), addr).isP2SHAddress()) {
                                            int prevIdx = BIP49Util.getInstance(activity).getWallet().getAccount(0).getReceive().getAddrIdx() - 1;
                                            BIP49Util.getInstance(activity).getWallet().getAccount(0).getReceive().setAddrIdx(prevIdx);
                                        } else {
                                            int prevIdx = HD_WalletFactory.getInstance(activity).get().getAccount(0).getReceive().getAddrIdx() - 1;
                                            HD_WalletFactory.getInstance(activity).get().getAccount(0).getReceive().setAddrIdx(prevIdx);
                                        }
                                    } catch (MnemonicException.MnemonicLengthException | DecoderException | IOException e) {
                                        handler.post(new Runnable() {
                                            public void run() {
                                                Toast.makeText(activity, e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    } finally {
                                        dialog.dismiss();
                                    }

                                }
                            });
                    if (!activity.isFinishing()) {
                        dlg.show();
                    }

                } else {
                    handler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(activity, R.string.cannot_create_cpfp, Toast.LENGTH_SHORT).show();
                        }
                    });
                }

            } catch (final JSONException je) {
                handler.post(new Runnable() {
                    public void run() {
                        Toast.makeText(activity, "cpfp:" + je.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            FeeUtil.getInstance().setSuggestedFee(suggestedFee);

        } else {
            handler.post(new Runnable() {
                public void run() {
                    Toast.makeText(activity, R.string.cpfp_cannot_retrieve_tx, Toast.LENGTH_SHORT).show();
                }
            });
        }

        Looper.loop();

        return "OK";
    }

    @Override
    protected void onPostExecute(String result) {
        ;
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        ;
    }

    private UTXO getUTXO(String address) {

        UTXO ret = null;
        int idx = -1;

        for (int i = 0; i < utxos.size(); i++) {
            UTXO utxo = utxos.get(i);
            Log.d("activity", "utxo address:" + utxo.getOutpoints().get(0).getAddress());
            if (utxo.getOutpoints().get(0).getAddress().equals(address)) {
                ret = utxo;
                idx = i;
                break;
            }
        }

        if (ret != null) {
            utxos.remove(idx);
            return ret;
        }

        return null;
    }

}
