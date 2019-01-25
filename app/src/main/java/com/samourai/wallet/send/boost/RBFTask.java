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
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.bip69.BIP69OutputComparator;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.segwit.BIP49Util;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.segwit.bech32.Bech32Util;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.MyTransactionInput;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.PushTx;
import com.samourai.wallet.send.RBFSpend;
import com.samourai.wallet.send.RBFUtil;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.SuggestedFee;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.PrefsUtil;

import org.apache.commons.lang3.tuple.Triple;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.TransactionWitness;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
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

public class RBFTask extends AsyncTask<String, Void, String> {

    private Activity activity;
    private List<UTXO> utxos = null;
    private Handler handler = null;
    private RBFSpend rbf = null;
    private HashMap<String, Long> input_values = null;

    public RBFTask(Activity activity) {
        this.activity = activity;
        Log.i("RBD", "RBFTask: ");
    }

    @Override
    protected void onPreExecute() {
        handler = new Handler();
        utxos = APIFactory.getInstance(activity).getUtxos(true);
        input_values = new HashMap<String, Long>();
    }

    @Override
    protected String doInBackground(final String... params) {

        Looper.prepare();

        Log.d("RBF", "hash:" + params[0]);

        rbf = RBFUtil.getInstance().get(params[0]);
        Log.d("RBF", "rbf:" + rbf.toJSON().toString());
        final Transaction tx = new Transaction(SamouraiWallet.getInstance().getCurrentNetworkParams(), Hex.decode(rbf.getSerializedTx()));
        Log.d("RBF", "tx serialized:" + rbf.getSerializedTx());
        Log.d("RBF", "tx inputs:" + tx.getInputs().size());
        Log.d("RBF", "tx outputs:" + tx.getOutputs().size());
        JSONObject txObj = APIFactory.getInstance(activity).getTxInfo(params[0]);
        if (tx != null && txObj.has("inputs") && txObj.has("outputs")) {
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

                SuggestedFee suggestedFee = FeeUtil.getInstance().getSuggestedFee();
                FeeUtil.getInstance().setSuggestedFee(FeeUtil.getInstance().getHighFee());
                BigInteger estimatedFee = FeeUtil.getInstance().estimatedFeeSegwit(p2pkh, p2sh_p2wpkh, p2wpkh, outputs.length());

                long total_inputs = 0L;
                long total_outputs = 0L;
                long fee = 0L;
                long total_change = 0L;
                List<String> selfAddresses = new ArrayList<String>();

                for (int i = 0; i < inputs.length(); i++) {
                    JSONObject obj = inputs.getJSONObject(i);
                    if (obj.has("outpoint")) {
                        JSONObject objPrev = obj.getJSONObject("outpoint");
                        if (objPrev.has("value")) {
                            total_inputs += objPrev.getLong("value");
                            String key = objPrev.getString("txid") + ":" + objPrev.getLong("vout");
                            input_values.put(key, objPrev.getLong("value"));
                        }
                    }
                }

                for (int i = 0; i < outputs.length(); i++) {
                    JSONObject obj = outputs.getJSONObject(i);
                    if (obj.has("value")) {
                        total_outputs += obj.getLong("value");

                        String _addr = null;
                        if (obj.has("address")) {
                            _addr = obj.getString("address");
                        }

                        selfAddresses.add(_addr);
                        if (_addr != null && rbf.getChangeAddrs().contains(_addr.toString())) {
                            total_change += obj.getLong("value");
                        }
                    }
                }

                boolean feeWarning = false;
                fee = total_inputs - total_outputs;
                if (fee > estimatedFee.longValue()) {
                    feeWarning = true;
                }

                long remainingFee = (estimatedFee.longValue() > fee) ? estimatedFee.longValue() - fee : 0L;

                Log.d("RBF", "total inputs:" + total_inputs);
                Log.d("RBF", "total outputs:" + total_outputs);
                Log.d("RBF", "total change:" + total_change);
                Log.d("RBF", "fee:" + fee);
                Log.d("RBF", "estimated fee:" + estimatedFee.longValue());
                Log.d("RBF", "fee warning:" + feeWarning);
                Log.d("RBF", "remaining fee:" + remainingFee);

                List<TransactionOutput> txOutputs = new ArrayList<TransactionOutput>();
                txOutputs.addAll(tx.getOutputs());

                long remainder = remainingFee;
                if (total_change > remainder) {
                    for (TransactionOutput output : txOutputs) {
                        Script script = output.getScriptPubKey();
                        String scriptPubKey = Hex.toHexString(script.getProgram());
                        Address _p2sh = output.getAddressFromP2SH(SamouraiWallet.getInstance().getCurrentNetworkParams());
                        Address _p2pkh = output.getAddressFromP2PKHScript(SamouraiWallet.getInstance().getCurrentNetworkParams());
                        try {
                            if ((Bech32Util.getInstance().isBech32Script(scriptPubKey) && rbf.getChangeAddrs().contains(Bech32Util.getInstance().getAddressFromScript(scriptPubKey))) || (_p2sh != null && rbf.getChangeAddrs().contains(_p2sh.toString())) || (_p2pkh != null && rbf.getChangeAddrs().contains(_p2pkh.toString()))) {
                                if (output.getValue().longValue() >= (remainder + SamouraiWallet.bDust.longValue())) {
                                    output.setValue(Coin.valueOf(output.getValue().longValue() - remainder));
                                    remainder = 0L;
                                    break;
                                } else {
                                    remainder -= output.getValue().longValue();
                                    output.setValue(Coin.valueOf(0L));      // output will be discarded later
                                }
                            }
                        } catch (Exception e) {
                            ;
                        }

                    }

                }

                //
                // original inputs are not modified
                //
                List<MyTransactionInput> _inputs = new ArrayList<MyTransactionInput>();
                List<TransactionInput> txInputs = tx.getInputs();
                for (TransactionInput input : txInputs) {
                    MyTransactionInput _input = new MyTransactionInput(SamouraiWallet.getInstance().getCurrentNetworkParams(), null, new byte[0], input.getOutpoint(), input.getOutpoint().getHash().toString(), (int) input.getOutpoint().getIndex());
                    _input.setSequenceNumber(SamouraiWallet.RBF_SEQUENCE_VAL.longValue());
                    _inputs.add(_input);
                    Log.d("RBF", "add outpoint:" + _input.getOutpoint().toString());
                }

                Triple<Integer, Integer, Integer> outpointTypes = null;
                if (remainder > 0L) {
                    List<UTXO> selectedUTXO = new ArrayList<UTXO>();
                    long selectedAmount = 0L;
                    int selected = 0;
                    long _remainingFee = remainder;
                    Collections.sort(utxos, new UTXO.UTXOComparator());
                    for (UTXO _utxo : utxos) {

                        Log.d("RBF", "utxo value:" + _utxo.getValue());

                        //
                        // do not select utxo that are change outputs in current rbf tx
                        //
                        boolean isChange = false;
                        boolean isSelf = false;
                        for (MyTransactionOutPoint outpoint : _utxo.getOutpoints()) {
                            if (rbf.containsChangeAddr(outpoint.getAddress())) {
                                Log.d("RBF", "is change:" + outpoint.getAddress());
                                Log.d("RBF", "is change:" + outpoint.getValue().longValue());
                                isChange = true;
                                break;
                            }
                            if (selfAddresses.contains(outpoint.getAddress())) {
                                Log.d("RBF", "is self:" + outpoint.getAddress());
                                Log.d("RBF", "is self:" + outpoint.getValue().longValue());
                                isSelf = true;
                                break;
                            }
                        }
                        if (isChange || isSelf) {
                            continue;
                        }

                        selectedUTXO.add(_utxo);
                        selected += _utxo.getOutpoints().size();
                        Log.d("RBF", "selected utxo:" + selected);
                        selectedAmount += _utxo.getValue();
                        Log.d("RBF", "selected utxo value:" + _utxo.getValue());
                        outpointTypes = FeeUtil.getInstance().getOutpointCount(new Vector(_utxo.getOutpoints()));
                        p2pkh += outpointTypes.getLeft();
                        p2sh_p2wpkh += outpointTypes.getMiddle();
                        p2wpkh += outpointTypes.getRight();
                        _remainingFee = FeeUtil.getInstance().estimatedFeeSegwit(p2pkh, p2sh_p2wpkh, p2wpkh, outputs.length() == 1 ? 2 : outputs.length()).longValue();
                        Log.d("RBF", "_remaining fee:" + _remainingFee);
                        if (selectedAmount >= (_remainingFee + SamouraiWallet.bDust.longValue())) {
                            break;
                        }
                    }
                    long extraChange = 0L;
                    if (selectedAmount < (_remainingFee + SamouraiWallet.bDust.longValue())) {
                        handler.post(new Runnable() {
                            public void run() {
                                Toast.makeText(activity, R.string.insufficient_funds, Toast.LENGTH_SHORT).show();
                            }
                        });
                        return "KO";
                    } else {
                        extraChange = selectedAmount - _remainingFee;
                        Log.d("RBF", "extra change:" + extraChange);
                    }

                    boolean addedChangeOutput = false;
                    // parent tx didn't have change output
                    if (outputs.length() == 1 && extraChange > 0L) {
                        try {
                            boolean isSegwitChange = (FormatsUtil.getInstance().isValidBech32(outputs.getJSONObject(0).getString("address")) || Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), outputs.getJSONObject(0).getString("address")).isP2SHAddress()) || PrefsUtil.getInstance(activity).getValue(PrefsUtil.USE_LIKE_TYPED_CHANGE, true) == false;

                            String change_address = null;
                            if (isSegwitChange) {
                                int changeIdx = BIP49Util.getInstance(activity).getWallet().getAccount(0).getChange().getAddrIdx();
                                change_address = BIP49Util.getInstance(activity).getAddressAt(AddressFactory.CHANGE_CHAIN, changeIdx).getAddressAsString();
                            } else {
                                int changeIdx = HD_WalletFactory.getInstance(activity).get().getAccount(0).getChange().getAddrIdx();
                                change_address = HD_WalletFactory.getInstance(activity).get().getAccount(0).getChange().getAddressAt(changeIdx).getAddressString();
                            }

                            Script toOutputScript = ScriptBuilder.createOutputScript(Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), change_address));
                            TransactionOutput output = new TransactionOutput(SamouraiWallet.getInstance().getCurrentNetworkParams(), null, Coin.valueOf(extraChange), toOutputScript.getProgram());
                            txOutputs.add(output);
                            addedChangeOutput = true;
                        } catch (MnemonicException.MnemonicLengthException | IOException e) {
                            handler.post(new Runnable() {
                                public void run() {
                                    Toast.makeText(activity, e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Toast.makeText(activity, R.string.cannot_create_change_output, Toast.LENGTH_SHORT).show();
                                }
                            });
                            return "KO";
                        }

                    }
                    // parent tx had change output
                    else {
                        for (TransactionOutput output : txOutputs) {
                            Script script = output.getScriptPubKey();
                            String scriptPubKey = Hex.toHexString(script.getProgram());
                            String _addr = null;
                            if (Bech32Util.getInstance().isBech32Script(scriptPubKey)) {
                                try {
                                    _addr = Bech32Util.getInstance().getAddressFromScript(scriptPubKey);
                                } catch (Exception e) {
                                    ;
                                }
                            }
                            if (_addr == null) {
                                Address _address = output.getAddressFromP2PKHScript(SamouraiWallet.getInstance().getCurrentNetworkParams());
                                if (_address == null) {
                                    _address = output.getAddressFromP2SH(SamouraiWallet.getInstance().getCurrentNetworkParams());
                                }
                                _addr = _address.toString();
                            }
                            Log.d("RBF", "checking for change:" + _addr);
                            if (rbf.containsChangeAddr(_addr)) {
                                Log.d("RBF", "before extra:" + output.getValue().longValue());
                                output.setValue(Coin.valueOf(extraChange + output.getValue().longValue()));
                                Log.d("RBF", "after extra:" + output.getValue().longValue());
                                addedChangeOutput = true;
                                break;
                            }
                        }
                    }

                    // sanity check
                    if (extraChange > 0L && !addedChangeOutput) {
                        handler.post(new Runnable() {
                            public void run() {
                                Toast.makeText(activity, R.string.cannot_create_change_output, Toast.LENGTH_SHORT).show();
                            }
                        });
                        return "KO";
                    }

                    //
                    // update keyBag w/ any new paths
                    //
                    final HashMap<String, String> keyBag = rbf.getKeyBag();
                    for (UTXO _utxo : selectedUTXO) {

                        for (MyTransactionOutPoint outpoint : _utxo.getOutpoints()) {

                            MyTransactionInput _input = new MyTransactionInput(SamouraiWallet.getInstance().getCurrentNetworkParams(), null, new byte[0], outpoint, outpoint.getTxHash().toString(), outpoint.getTxOutputN());
                            _input.setSequenceNumber(SamouraiWallet.RBF_SEQUENCE_VAL.longValue());
                            _inputs.add(_input);
                            Log.d("RBF", "add selected outpoint:" + _input.getOutpoint().toString());

                            String path = APIFactory.getInstance(activity).getUnspentPaths().get(outpoint.getAddress());
                            if (path != null) {
                                if (FormatsUtil.getInstance().isValidBech32(outpoint.getAddress())) {
                                    rbf.addKey(outpoint.toString(), path + "/84");
                                } else if (Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), outpoint.getAddress()) != null && Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), outpoint.getAddress()).isP2SHAddress()) {
                                    rbf.addKey(outpoint.toString(), path + "/49");
                                } else {
                                    rbf.addKey(outpoint.toString(), path);
                                }
                                Log.d("RBF", "outpoint address:" + outpoint.getAddress());
                            } else {
                                String pcode = BIP47Meta.getInstance().getPCode4Addr(outpoint.getAddress());
                                int idx = BIP47Meta.getInstance().getIdx4Addr(outpoint.getAddress());
                                rbf.addKey(outpoint.toString(), pcode + "/" + idx);
                            }

                        }

                    }
                    rbf.setKeyBag(keyBag);

                }

                //
                // BIP69 sort of outputs/inputs
                //
                final Transaction _tx = new Transaction(SamouraiWallet.getInstance().getCurrentNetworkParams());
                List<TransactionOutput> _txOutputs = new ArrayList<TransactionOutput>();
                _txOutputs.addAll(txOutputs);
                Collections.sort(_txOutputs, new BIP69OutputComparator());
                for (TransactionOutput to : _txOutputs) {
                    // zero value outputs discarded here
                    if (to.getValue().longValue() > 0L) {
                        _tx.addOutput(to);
                    }
                }

                List<MyTransactionInput> __inputs = new ArrayList<MyTransactionInput>();
                __inputs.addAll(_inputs);
                Collections.sort(__inputs, new SendFactory.BIP69InputComparator());
                for (TransactionInput input : __inputs) {
                    _tx.addInput(input);
                }

                FeeUtil.getInstance().setSuggestedFee(suggestedFee);

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

                                Transaction __tx = signTx(_tx);
                                final String hexTx = new String(Hex.encode(__tx.bitcoinSerialize()));
                                Log.d("RBF", "hex tx:" + hexTx);

                                final String strTxHash = __tx.getHashAsString();
                                Log.d("RBF", "tx hash:" + strTxHash);

                                if (__tx != null) {

                                    boolean isOK = false;
                                    try {

                                        isOK = PushTx.getInstance(activity).pushTx(hexTx);

                                        if (isOK) {

                                            handler.post(new Runnable() {
                                                public void run() {
                                                    Toast.makeText(activity, R.string.rbf_spent, Toast.LENGTH_SHORT).show();

                                                    RBFSpend _rbf = rbf;    // includes updated 'keyBag'
                                                    _rbf.setSerializedTx(hexTx);
                                                    _rbf.setHash(strTxHash);
                                                    _rbf.setPrevHash(params[0]);
                                                    RBFUtil.getInstance().add(_rbf);

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

                                        }
                                    } catch (final DecoderException de) {
                                        handler.post(new Runnable() {
                                            public void run() {
                                                Toast.makeText(activity, "pushTx:" + de.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    } finally {
                                        ;
                                    }

                                }

                            }
                        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                dialog.dismiss();

                            }
                        });
                if (!activity.isFinishing()) {
                    dlg.show();
                }

            } catch (final JSONException je) {
                handler.post(new Runnable() {
                    public void run() {
                        Toast.makeText(activity, "rbf:" + je.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

        } else {
            Toast.makeText(activity, R.string.cpfp_cannot_retrieve_tx, Toast.LENGTH_SHORT).show();
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

    private Transaction signTx(Transaction tx) {

        HashMap<String, ECKey> keyBag = new HashMap<String, ECKey>();
        HashMap<String, ECKey> keyBag49 = new HashMap<String, ECKey>();
        HashMap<String, ECKey> keyBag84 = new HashMap<String, ECKey>();

        HashMap<String, String> keys = rbf.getKeyBag();
        for (String outpoint : keys.keySet()) {

            ECKey ecKey = null;

            String[] s = keys.get(outpoint).split("/");
            Log.i("RBF", "path length:" + s.length);
            if (s.length == 4) {
                if (s[3].equals("84")) {
                    HD_Address addr = BIP84Util.getInstance(activity).getWallet().getAccount(0).getChain(Integer.parseInt(s[1])).getAddressAt(Integer.parseInt(s[2]));
                    ecKey = addr.getECKey();
                } else {
                    HD_Address addr = BIP49Util.getInstance(activity).getWallet().getAccount(0).getChain(Integer.parseInt(s[1])).getAddressAt(Integer.parseInt(s[2]));
                    ecKey = addr.getECKey();
                }
            } else if (s.length == 3) {
                HD_Address hd_address = AddressFactory.getInstance(activity).get(0, Integer.parseInt(s[1]), Integer.parseInt(s[2]));
                String strPrivKey = hd_address.getPrivateKeyString();
                DumpedPrivateKey pk = new DumpedPrivateKey(SamouraiWallet.getInstance().getCurrentNetworkParams(), strPrivKey);
                ecKey = pk.getKey();
            } else if (s.length == 2) {
                try {
                    PaymentAddress address = BIP47Util.getInstance(activity).getReceiveAddress(new PaymentCode(s[0]), Integer.parseInt(s[1]));
                    ecKey = address.getReceiveECKey();
                } catch (Exception e) {
                    ;
                }
            } else {
                ;
            }

            Log.i("RBF", "outpoint:" + outpoint);
            Log.i("RBF", "path:" + keys.get(outpoint));
//                Log.i("RBF", "ECKey address from ECKey:" + ecKey.toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());

            if (ecKey != null) {
                if (s.length == 4) {
                    if (s[3].equals("84")) {
                        keyBag84.put(outpoint, ecKey);
                    } else {
                        keyBag49.put(outpoint, ecKey);
                    }
                } else {
                    keyBag.put(outpoint, ecKey);
                }
            } else {
                throw new RuntimeException("ECKey error: cannot process private key");
//                    Log.i("ECKey error", "cannot process private key");
            }

        }

        List<TransactionInput> inputs = tx.getInputs();
        for (int i = 0; i < inputs.size(); i++) {

            ECKey ecKey = null;
            String address = null;
            if (inputs.get(i).getValue() != null || keyBag49.containsKey(inputs.get(i).getOutpoint().toString()) || keyBag84.containsKey(inputs.get(i).getOutpoint().toString())) {
                if (keyBag84.containsKey(inputs.get(i).getOutpoint().toString())) {
                    ecKey = keyBag84.get(inputs.get(i).getOutpoint().toString());
                    SegwitAddress segwitAddress = new SegwitAddress(ecKey.getPubKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());
                    address = segwitAddress.getBech32AsString();
                } else {
                    ecKey = keyBag49.get(inputs.get(i).getOutpoint().toString());
                    SegwitAddress segwitAddress = new SegwitAddress(ecKey.getPubKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());
                    address = segwitAddress.getAddressAsString();
                }
            } else {
                ecKey = keyBag.get(inputs.get(i).getOutpoint().toString());
                address = ecKey.toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
            }
            Log.d("RBF", "pubKey:" + Hex.toHexString(ecKey.getPubKey()));
            Log.d("RBF", "address:" + address);

            if (inputs.get(i).getValue() != null || keyBag49.containsKey(inputs.get(i).getOutpoint().toString()) || keyBag84.containsKey(inputs.get(i).getOutpoint().toString())) {

                final SegwitAddress segwitAddress = new SegwitAddress(ecKey.getPubKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());
                Script scriptPubKey = segwitAddress.segWitOutputScript();
                final Script redeemScript = segwitAddress.segWitRedeemScript();
                System.out.println("redeem script:" + Hex.toHexString(redeemScript.getProgram()));
                final Script scriptCode = redeemScript.scriptCode();
                System.out.println("script code:" + Hex.toHexString(scriptCode.getProgram()));

                TransactionSignature sig = tx.calculateWitnessSignature(i, ecKey, scriptCode, Coin.valueOf(input_values.get(inputs.get(i).getOutpoint().toString())), Transaction.SigHash.ALL, false);
                final TransactionWitness witness = new TransactionWitness(2);
                witness.setPush(0, sig.encodeToBitcoin());
                witness.setPush(1, ecKey.getPubKey());
                tx.setWitness(i, witness);

                if (!FormatsUtil.getInstance().isValidBech32(address) && Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress()) {
                    final ScriptBuilder sigScript = new ScriptBuilder();
                    sigScript.data(redeemScript.getProgram());
                    tx.getInput(i).setScriptSig(sigScript.build());
                    tx.getInput(i).getScriptSig().correctlySpends(tx, i, scriptPubKey, Coin.valueOf(input_values.get(inputs.get(i).getOutpoint().toString())), Script.ALL_VERIFY_FLAGS);
                }

            } else {
                Log.i("RBF", "sign outpoint:" + inputs.get(i).getOutpoint().toString());
                Log.i("RBF", "ECKey address from keyBag:" + ecKey.toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());

                Log.i("RBF", "script:" + ScriptBuilder.createOutputScript(ecKey.toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams())));
                Log.i("RBF", "script:" + Hex.toHexString(ScriptBuilder.createOutputScript(ecKey.toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams())).getProgram()));
                TransactionSignature sig = tx.calculateSignature(i, ecKey, ScriptBuilder.createOutputScript(ecKey.toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams())).getProgram(), Transaction.SigHash.ALL, false);
                tx.getInput(i).setScriptSig(ScriptBuilder.createInputScript(sig, ecKey));
            }

        }

        return tx;
    }

}
