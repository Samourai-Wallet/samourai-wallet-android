package com.samourai.wallet.send.boost;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.samourai.wallet.BalanceActivity;
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
import java.util.concurrent.Callable;

import io.reactivex.Observable;

public class CPFPTask {

    protected Context context;
    private List<UTXO> utxos = new ArrayList<>();
    private Handler handler = null;
    private List<MyTransactionOutPoint> outPoints = new ArrayList<>();
    private HashMap<String, BigInteger> receivers = new HashMap<String, BigInteger>();
    private SuggestedFee suggestedFee;
    private String addr, hash;

    public CPFPTask(Context _context, String hash) {
        this.context = _context;
        this.hash = hash;
        utxos = APIFactory.getInstance(context).getUtxos(true);
        suggestedFee = FeeUtil.getInstance().getSuggestedFee();
    }

    public Observable<String> prepareAndGetCPFPMessage() {
        return Observable.fromCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return prepareCPFPBoost();
            }
        });
    }

    private String prepareCPFPBoost() throws Exception {

        JSONObject txObj = APIFactory.getInstance(context).getTxInfo(this.hash);
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
                        Log.d("BalanceActivity", "checking address:" + addr);
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

                Log.d("BalanceActivity", "total inputs:" + total_inputs);
                Log.d("BalanceActivity", "total outputs:" + total_outputs);
                Log.d("BalanceActivity", "fee:" + fee);
                Log.d("BalanceActivity", "estimated fee:" + estimatedFee.longValue());
                Log.d("BalanceActivity", "fee warning:" + feeWarning);
                if (utxo != null) {
                    Log.d("BalanceActivity", "utxo found");

                    List<UTXO> selectedUTXO = new ArrayList<UTXO>();
                    selectedUTXO.add(utxo);
                    int selected = utxo.getOutpoints().size();

                    long remainingFee = (estimatedFee.longValue() > fee) ? estimatedFee.longValue() - fee : 0L;
                    Log.d("BalanceActivity", "remaining fee:" + remainingFee);
                    int receiveIdx = AddressFactory.getInstance(context).getHighestTxReceiveIdx(0);
                    Log.d("BalanceActivity", "receive index:" + receiveIdx);
                    final String addr;
                    if (PrefsUtil.getInstance(context).getValue(PrefsUtil.USE_LIKE_TYPED_CHANGE, true) == true) {
                        addr = utxo.getOutpoints().get(0).getAddress();
                    } else {
                        addr = outputs.getJSONObject(0).getString("address");
                    }
                    final String ownReceiveAddr;
                    if (FormatsUtil.getInstance().isValidBech32(addr)) {
                        ownReceiveAddr = AddressFactory.getInstance(context).getBIP84(AddressFactory.RECEIVE_CHAIN).getBech32AsString();
                    } else if (Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), addr).isP2SHAddress()) {
                        ownReceiveAddr = AddressFactory.getInstance(context).getBIP49(AddressFactory.RECEIVE_CHAIN).getAddressAsString();
                    } else {
                        ownReceiveAddr = AddressFactory.getInstance(context).get(AddressFactory.RECEIVE_CHAIN).getAddressString();
                    }
                    Log.d("BalanceActivity", "receive address:" + ownReceiveAddr);

                    long totalAmount = utxo.getValue();
                    Log.d("BalanceActivity", "amount before fee:" + totalAmount);
                    Triple<Integer, Integer, Integer> outpointTypes = FeeUtil.getInstance().getOutpointCount(new Vector(utxo.getOutpoints()));
                    BigInteger cpfpFee = FeeUtil.getInstance().estimatedFeeSegwit(outpointTypes.getLeft(), outpointTypes.getMiddle(), outpointTypes.getRight(), 1);
                    Log.d("BalanceActivity", "cpfp fee:" + cpfpFee.longValue());

                    p2pkh = outpointTypes.getLeft();
                    p2sh_p2wpkh = outpointTypes.getMiddle();
                    p2wpkh = outpointTypes.getRight();

                    if (totalAmount < (cpfpFee.longValue() + remainingFee)) {
                        Log.d("BalanceActivity", "selecting additional utxo");
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
                                    Toast.makeText(context, R.string.insufficient_funds, Toast.LENGTH_SHORT).show();
                                }
                            });
                            FeeUtil.getInstance().setSuggestedFee(suggestedFee);
                            return "KO";
                        }
                    }

                    cpfpFee = cpfpFee.add(BigInteger.valueOf(remainingFee));
                    Log.d("BalanceActivity", "cpfp fee:" + cpfpFee.longValue());

                    final List<MyTransactionOutPoint> outPoints = new ArrayList<MyTransactionOutPoint>();
                    for (UTXO u : selectedUTXO) {
                        outPoints.addAll(u.getOutpoints());
                    }

                    long _totalAmount = 0L;
                    for (MyTransactionOutPoint outpoint : outPoints) {
                        _totalAmount += outpoint.getValue().longValue();
                    }
                    Log.d("BalanceActivity", "checked total amount:" + _totalAmount);
                    assert (_totalAmount == totalAmount);

                    long amount = totalAmount - cpfpFee.longValue();
                    Log.d("BalanceActivity", "amount after fee:" + amount);

                    if (amount < SamouraiWallet.bDust.longValue()) {
                        Log.d("BalanceActivity", "dust output");
                        Toast.makeText(context, R.string.cannot_output_dust, Toast.LENGTH_SHORT).show();
                    }

                    final HashMap<String, BigInteger> receivers = new HashMap<String, BigInteger>();
                    receivers.put(ownReceiveAddr, BigInteger.valueOf(amount));

                    String message = "";
                    if (feeWarning) {
                        message += context.getString(R.string.fee_bump_not_necessary);
                        message += "\n\n";
                    }
                    message += context.getString(R.string.bump_fee) + " " + Coin.valueOf(remainingFee).toPlainString() + " BTC";

                    return message;

                } else {

                    FeeUtil.getInstance().setSuggestedFee(suggestedFee);
                    throw new Exception(context.getString(R.string.cannot_create_cpfp));
                }

            } catch (final JSONException je) {

                FeeUtil.getInstance().setSuggestedFee(suggestedFee);
                throw new Exception("cpfp:" + je.getMessage());
            }


        } else {
            FeeUtil.getInstance().setSuggestedFee(suggestedFee);
            throw new Exception("cpfp:" + context.getString(R.string.cpfp_cannot_retrieve_tx));
        }

    }


    private void pushCPFPtx() {

        if (AppUtil.getInstance(context.getApplicationContext()).isServiceRunning(WebSocketService.class)) {
            context.stopService(new Intent(context.getApplicationContext(), WebSocketService.class));
        }
        context.startService(new Intent(context.getApplicationContext(), WebSocketService.class));

        Transaction tx = SendFactory.getInstance(context).makeTransaction(0, outPoints, receivers);
        if (tx != null) {
            tx = SendFactory.getInstance(context).signTransaction(tx);
            final String hexTx = new String(Hex.encode(tx.bitcoinSerialize()));
            Log.d("BalanceActivity", hexTx);

            final String strTxHash = tx.getHashAsString();
            Log.d("BalanceActivity", strTxHash);

            boolean isOK = false;
            try {

                isOK = PushTx.getInstance(context).pushTx(hexTx);

                if (isOK) {

                    handler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(context, R.string.cpfp_spent, Toast.LENGTH_SHORT).show();

                            FeeUtil.getInstance().setSuggestedFee(suggestedFee);

                            Intent _intent = new Intent(context, MainActivity2.class);
                            _intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            context.startActivity(_intent);
                        }
                    });

                } else {
                    handler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(context, R.string.tx_failed, Toast.LENGTH_SHORT).show();
                        }
                    });

                    // reset receive index upon tx fail
                    if (FormatsUtil.getInstance().isValidBech32(addr)) {
                        int prevIdx = BIP84Util.getInstance(context).getWallet().getAccount(0).getReceive().getAddrIdx() - 1;
                        BIP84Util.getInstance(context).getWallet().getAccount(0).getReceive().setAddrIdx(prevIdx);
                    } else if (Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), addr).isP2SHAddress()) {
                        int prevIdx = BIP49Util.getInstance(context).getWallet().getAccount(0).getReceive().getAddrIdx() - 1;
                        BIP49Util.getInstance(context).getWallet().getAccount(0).getReceive().setAddrIdx(prevIdx);
                    } else {
                        int prevIdx = HD_WalletFactory.getInstance(context).get().getAccount(0).getReceive().getAddrIdx() - 1;
                        HD_WalletFactory.getInstance(context).get().getAccount(0).getReceive().setAddrIdx(prevIdx);
                    }

                }
            } catch (MnemonicException.MnemonicLengthException | DecoderException | IOException e) {
                handler.post(new Runnable() {
                    public void run() {
                        Toast.makeText(context, "pushTx:" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } finally {
                ;
            }

        }
    }


    private void resetBoostAddress() {

        try {
            if (Bech32Util.getInstance().isBech32Script(addr)) {
                int prevIdx = BIP84Util.getInstance(context).getWallet().getAccount(0).getReceive().getAddrIdx() - 1;
                BIP84Util.getInstance(context).getWallet().getAccount(0).getReceive().setAddrIdx(prevIdx);
            } else if (Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), addr).isP2SHAddress()) {
                int prevIdx = BIP49Util.getInstance(context).getWallet().getAccount(0).getReceive().getAddrIdx() - 1;
                BIP49Util.getInstance(context).getWallet().getAccount(0).getReceive().setAddrIdx(prevIdx);
            } else {
                int prevIdx = HD_WalletFactory.getInstance(context).get().getAccount(0).getReceive().getAddrIdx() - 1;
                HD_WalletFactory.getInstance(context).get().getAccount(0).getReceive().setAddrIdx(prevIdx);
            }
        } catch (MnemonicException.MnemonicLengthException | DecoderException | IOException e) {
            handler.post(new Runnable() {
                public void run() {
                    Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } finally {

        }

    }


    private UTXO getUTXO(String address) {

        UTXO ret = null;
        int idx = -1;

        try {
            for (int i = 0; i < utxos.size(); i++) {
                UTXO utxo = utxos.get(i);
                Log.d("BalanceActivity", "utxo address:" + utxo.getOutpoints().get(0).getAddress());
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
        } catch (Exception e) {
            Log.d("Observer", "Exception:getUTXO ".concat(e.getMessage()));
            e.printStackTrace();
        }

        return null;
    }

}
