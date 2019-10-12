package com.samourai.wallet.cahoots;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.Toast;

import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.cahoots.psbt.PSBT;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.segwit.bech32.Bech32Segwit;
import com.samourai.wallet.segwit.bech32.Bech32Util;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.script.Script;
import org.bouncycastle.util.encoders.Hex;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static com.samourai.wallet.util.LogUtil.debug;

public class CahootsUtil {

    private static Context context = null;

    private static CahootsUtil instance = null;

    private CahootsUtil()    { ; }

    public static CahootsUtil getInstance(Context ctx) {

        context = ctx;

        if(instance == null)    {
            instance = new CahootsUtil();
        }

        return instance;
    }

    public void doPSBT(final String strPSBT)    {

        String msg = null;
        PSBT psbt = new PSBT(strPSBT, SamouraiWallet.getInstance().getCurrentNetworkParams());
        try {
            psbt.read();
            msg = psbt.dump();
        }
        catch(Exception e) {
            msg = e.getMessage();
        }

        final EditText edPSBT = new EditText(context);
        edPSBT.setSingleLine(false);
        edPSBT.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        edPSBT.setLines(10);
        edPSBT.setHint(R.string.PSBT);
        edPSBT.setGravity(Gravity.START);
        TextWatcher textWatcher = new TextWatcher() {

            public void afterTextChanged(Editable s) {
                edPSBT.setSelection(0);
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }
        };
        edPSBT.addTextChangedListener(textWatcher);
        edPSBT.setText(msg);

        AlertDialog.Builder dlg = new AlertDialog.Builder(context)
                .setTitle(R.string.app_name)
                .setMessage(R.string.PSBT)
                .setView(edPSBT)
                .setCancelable(true)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                    }

                });
        if(!((Activity)context).isFinishing())    {
            dlg.show();
        }

    }

    //
    // sender
    //
    public Cahoots doStowaway0(long spendAmount, int account) {
        NetworkParameters params = SamouraiWallet.getInstance().getCurrentNetworkParams();

        //
        //
        // step0: B sends spend amount to A,  creates step0
        //
        //
        debug("CahootsUtil", "sender account (0):" + account);
        Stowaway stowaway0 = new Stowaway(spendAmount, params, account);
        try {
            stowaway0.setFingerprint(HD_WalletFactory.getInstance(context).getFingerprint());
        }
        catch(IOException | MnemonicException.MnemonicLengthException e) {
            ;
        }
        return stowaway0;
    }

    //
    // receiver
    //
    public Cahoots doStowaway1(Stowaway stowaway0) throws Exception {

        List<UTXO> utxos = getCahootsUTXO(0);
        // sort in descending order by value
        Collections.sort(utxos, new UTXO.UTXOComparator());

        debug("CahootsUtil", "BIP84 utxos:" + utxos.size());

        List<UTXO> selectedUTXO = new ArrayList<UTXO>();
        long totalContributedAmount = 0L;
        List<UTXO> highUTXO = new ArrayList<UTXO>();
        for (UTXO utxo : utxos) {
            if (utxo.getValue() > stowaway0.getSpendAmount() + SamouraiWallet.bDust.longValue()) {
                highUTXO.add(utxo);
            }
        }
        if(highUTXO.size() > 0)    {
            SecureRandom random = new SecureRandom();
            UTXO utxo = highUTXO.get(random.nextInt(highUTXO.size()));
            debug("CahootsUtil", "BIP84 selected random utxo:" + utxo.getValue());
            selectedUTXO.add(utxo);
            totalContributedAmount = utxo.getValue();
        }
        if (selectedUTXO.size() == 0) {
            for (UTXO utxo : utxos) {
                selectedUTXO.add(utxo);
                totalContributedAmount += utxo.getValue();
                debug("CahootsUtil", "BIP84 selected utxo:" + utxo.getValue());
                if (totalContributedAmount > stowaway0.getSpendAmount() + SamouraiWallet.bDust.longValue()) {
                    break;
                }
            }
        }

        if (!(totalContributedAmount > stowaway0.getSpendAmount() + SamouraiWallet.bDust.longValue())) {
            return null;
        }

        debug("CahootsUtil", "BIP84 selected utxos:" + selectedUTXO.size());

        NetworkParameters params = stowaway0.getParams();

        //
        //
        // step1: A utxos -> B (take largest that cover amount)
        //
        //

        String zpub = BIP84Util.getInstance(context).getWallet().getAccount(0).zpubstr();
        HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>> inputsA = new HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>>();
//        inputsA.put(outpoint_A0, Triple.of(Hex.decode("0221b719bc26fb49971c7dd328a6c7e4d17dfbf4e2217bee33a65c53ed3daf041e"), FormatsUtil.getInstance().getFingerprintFromXPUB("vpub5Z3hqXewnCbxnMsWygxN8AyjNVJbFeV2VCdDKpTFazdoC29qK4Y5DSQ1aaAPBrsBZ1TzN5va6xGy4eWa9uAqh2AwifuA1eofkedh3eUgF6b"), "M/0/4"));
//        inputsA.put(outpoint_A1, Triple.of(Hex.decode("020ab261e1a3cf986ecb3cd02299de36295e804fd799934dc5c99dde0d25e71b93"), FormatsUtil.getInstance().getFingerprintFromXPUB("vpub5Z3hqXewnCbxnMsWygxN8AyjNVJbFeV2VCdDKpTFazdoC29qK4Y5DSQ1aaAPBrsBZ1TzN5va6xGy4eWa9uAqh2AwifuA1eofkedh3eUgF6b"), "M/0/2"));

        for (UTXO utxo : selectedUTXO) {
            for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                _TransactionOutPoint _outpoint = new _TransactionOutPoint(outpoint);

                ECKey eckey = SendFactory.getPrivKey(_outpoint.getAddress(), 0);
                String path = APIFactory.getInstance(context).getUnspentPaths().get(_outpoint.getAddress());
                inputsA.put(_outpoint, Triple.of(eckey.getPubKey(), stowaway0.getFingerprintCollab(), path));
            }
        }

        // destination output
        int idx = BIP84Util.getInstance(context).getWallet().getAccount(0).getReceive().getAddrIdx();
        SegwitAddress segwitAddress = BIP84Util.getInstance(context).getAddressAt(0, idx);
        HashMap<_TransactionOutput, Triple<byte[], byte[], String>> outputsA = new HashMap<_TransactionOutput, Triple<byte[], byte[], String>>();
//        byte[] scriptPubKey_A = getScriptPubKey("tb1qewwlc2dksuez3zauf38d82m7uqd4ewkf2avdl8", params);
        Pair<Byte, byte[]> pair = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress.getBech32AsString());
        byte[] scriptPubKey_A = Bech32Segwit.getScriptPubkey(pair.getLeft(), pair.getRight());
        _TransactionOutput output_A0 = new _TransactionOutput(params, null, Coin.valueOf(stowaway0.getSpendAmount()), scriptPubKey_A);
        outputsA.put(output_A0, Triple.of(segwitAddress.getECKey().getPubKey(), stowaway0.getFingerprintCollab(), "M/0/" + idx));

        stowaway0.setDestination(segwitAddress.getBech32AsString());

        Stowaway stowaway1 = new Stowaway(stowaway0);
        stowaway1.inc(inputsA, outputsA, null);

        return stowaway1;
    }

    //
    // sender
    //
    public Cahoots doStowaway2(Stowaway stowaway1) throws Exception {

        debug("CahootsUtil", "sender account (2):" + stowaway1.getAccount());

        Transaction transaction = stowaway1.getTransaction();
        debug("CahootsUtil", "step2 tx:" + org.spongycastle.util.encoders.Hex.toHexString(transaction.bitcoinSerialize()));
        int nbIncomingInputs = transaction.getInputs().size();

        List<UTXO> utxos = getCahootsUTXO(stowaway1.getAccount());
        // sort in ascending order by value
        Collections.sort(utxos, new UTXO.UTXOComparator());
        Collections.reverse(utxos);

        debug("CahootsUtil", "BIP84 utxos:" + utxos.size());

        List<UTXO> selectedUTXO = new ArrayList<UTXO>();
        int nbTotalSelectedOutPoints = 0;
        long totalSelectedAmount = 0L;
        List<UTXO> lowUTXO = new ArrayList<UTXO>();
        for (UTXO utxo : utxos) {
            if(utxo.getValue() < stowaway1.getSpendAmount())    {
                lowUTXO.add(utxo);
            }
        }

        List<List<UTXO>> listOfLists = new ArrayList<List<UTXO>>();
        Collections.shuffle(lowUTXO);
        listOfLists.add(lowUTXO);
        listOfLists.add(utxos);
        for(List<UTXO> list : listOfLists)   {

            selectedUTXO.clear();
            totalSelectedAmount = 0L;
            nbTotalSelectedOutPoints = 0;

            for (UTXO utxo : list) {
                selectedUTXO.add(utxo);
                totalSelectedAmount += utxo.getValue();
                debug("BIP84 selected utxo:", "" + utxo.getValue());
                nbTotalSelectedOutPoints += utxo.getOutpoints().size();
                if (totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue() + stowaway1.getSpendAmount() + SamouraiWallet.bDust.longValue()) {

                    // discard "extra" utxo, if any
                    List<UTXO> _selectedUTXO = new ArrayList<UTXO>();
                    Collections.reverse(selectedUTXO);
                    int _nbTotalSelectedOutPoints = 0;
                    long _totalSelectedAmount = 0L;
                    for (UTXO utxoSel : selectedUTXO) {
                        _selectedUTXO.add(utxoSel);
                        _totalSelectedAmount += utxoSel.getValue();
                        debug("CahootsUtil", "BIP84 post selected utxo:" + utxoSel.getValue());
                        _nbTotalSelectedOutPoints += utxoSel.getOutpoints().size();
                        if (_totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, _nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue() + stowaway1.getSpendAmount() + SamouraiWallet.bDust.longValue()) {
                            selectedUTXO.clear();
                            selectedUTXO.addAll(_selectedUTXO);
                            totalSelectedAmount = _totalSelectedAmount;
                            nbTotalSelectedOutPoints = _nbTotalSelectedOutPoints;
                            break;
                        }
                    }

                    break;
                }
            }
            if (totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue() + stowaway1.getSpendAmount() + SamouraiWallet.bDust.longValue()) {
                break;
            }
        }

        /*
        if(lowUTXO.size() > 0)    {
            Collections.shuffle(lowUTXO);
            for (UTXO utxo : lowUTXO) {
                selectedUTXO.add(utxo);
                totalSelectedAmount += utxo.getValue();
                debug("BIP84 selected utxo:", "" + utxo.getValue());
                nbTotalSelectedOutPoints += utxo.getOutpoints().size();
                if (totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue() + stowaway1.getSpendAmount() + SamouraiWallet.bDust.longValue()) {

                    // discard "extra" utxo, if any
                    List<UTXO> _selectedUTXO = new ArrayList<UTXO>();
                    Collections.reverse(selectedUTXO);
                    int _nbTotalSelectedOutPoints = 0;
                    long _totalSelectedAmount = 0L;
                    for (UTXO utxoSel : selectedUTXO) {
                        _selectedUTXO.add(utxoSel);
                        _totalSelectedAmount += utxoSel.getValue();
                        debug("CahootsUtil", "BIP84 post selected utxo:" + utxoSel.getValue());
                        _nbTotalSelectedOutPoints += utxoSel.getOutpoints().size();
                        if (_totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, _nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue() + stowaway1.getSpendAmount() + SamouraiWallet.bDust.longValue()) {
                            selectedUTXO.clear();
                            selectedUTXO.addAll(_selectedUTXO);
                            totalSelectedAmount = _totalSelectedAmount;
                            nbTotalSelectedOutPoints = _nbTotalSelectedOutPoints;
                            break;
                        }
                    }

                    break;
                }
            }

        }
        if (!(totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue() + stowaway1.getSpendAmount() + SamouraiWallet.bDust.longValue())) {
            selectedUTXO.clear();
            totalSelectedAmount = 0L;
            nbTotalSelectedOutPoints = 0;
            for (UTXO utxo : utxos) {
                selectedUTXO.add(utxo);
                totalSelectedAmount += utxo.getValue();
                debug("BIP84 selected utxo:", "" + utxo.getValue());
                nbTotalSelectedOutPoints += utxo.getOutpoints().size();
                if (totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue() + stowaway1.getSpendAmount() + SamouraiWallet.bDust.longValue()) {

                    // discard "extra" utxo, if any
                    List<UTXO> _selectedUTXO = new ArrayList<UTXO>();
                    Collections.reverse(selectedUTXO);
                    int _nbTotalSelectedOutPoints = 0;
                    long _totalSelectedAmount = 0L;
                    for (UTXO utxoSel : selectedUTXO) {
                        _selectedUTXO.add(utxoSel);
                        _totalSelectedAmount += utxoSel.getValue();
                        debug("CahootsUtil", "BIP84 post selected utxo:" + utxoSel.getValue());
                        _nbTotalSelectedOutPoints += utxoSel.getOutpoints().size();
                        if (_totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, _nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue() + stowaway1.getSpendAmount() + SamouraiWallet.bDust.longValue()) {
                            selectedUTXO.clear();
                            selectedUTXO.addAll(_selectedUTXO);
                            totalSelectedAmount = _totalSelectedAmount;
                            nbTotalSelectedOutPoints = _nbTotalSelectedOutPoints;
                            break;
                        }
                    }

                    break;
                }
            }
        }
        */
        if (!(totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue() + stowaway1.getSpendAmount() + SamouraiWallet.bDust.longValue())) {
            return null;
        }

        debug("CahootsUtil", "BIP84 selected utxos:" + selectedUTXO.size());

        long fee = FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue();
        debug("CahootsUtil", "fee:" + fee);

        NetworkParameters params = stowaway1.getParams();

        //
        //
        // step2: B verif, utxos -> A (take smallest that cover amount)
        //
        //

        String zpub = BIP84Util.getInstance(context).getWallet().getAccountAt(stowaway1.getAccount()).zpubstr();
        HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>> inputsB = new HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>>();

        for (UTXO utxo : selectedUTXO) {
            for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                _TransactionOutPoint _outpoint = new _TransactionOutPoint(outpoint);

                ECKey eckey = SendFactory.getPrivKey(_outpoint.getAddress(), stowaway1.getAccount());
                String path = APIFactory.getInstance(context).getUnspentPaths().get(_outpoint.getAddress());
                inputsB.put(_outpoint, Triple.of(eckey.getPubKey(), stowaway1.getFingerprint(), path));
            }
        }

        debug("CahootsUtil", "inputsB:" + inputsB.size());

        // change output
        SegwitAddress segwitAddress = null;
        int idx = 0;
        if (stowaway1.getAccount() == WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix()) {
            idx = AddressFactory.getInstance(context).getHighestPostChangeIdx();
            HD_Address addr = BIP84Util.getInstance(context).getWallet().getAccountAt(stowaway1.getAccount()).getChange().getAddressAt(idx);
            segwitAddress = new SegwitAddress(addr.getPubKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());
        } else {
            idx = BIP84Util.getInstance(context).getWallet().getAccount(0).getChange().getAddrIdx();
            segwitAddress = BIP84Util.getInstance(context).getAddressAt(1, idx);
        }
        HashMap<_TransactionOutput, Triple<byte[], byte[], String>> outputsB = new HashMap<_TransactionOutput, Triple<byte[], byte[], String>>();
        Pair<Byte, byte[]> pair = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress.getBech32AsString());
        byte[] scriptPubKey_B = Bech32Segwit.getScriptPubkey(pair.getLeft(), pair.getRight());
        _TransactionOutput output_B0 = new _TransactionOutput(params, null, Coin.valueOf((totalSelectedAmount - stowaway1.getSpendAmount()) - fee), scriptPubKey_B);
        outputsB.put(output_B0, Triple.of(segwitAddress.getECKey().getPubKey(), stowaway1.getFingerprint(), "M/1/" + idx));

        debug("CahootsUtil", "outputsB:" + outputsB.size());

        Stowaway stowaway2 = new Stowaway(stowaway1);
        stowaway2.inc(inputsB, outputsB, null);
        stowaway2.setFeeAmount(fee);

        return stowaway2;

    }

    //
    // receiver
    //
    public Cahoots doStowaway3(Stowaway stowaway2) throws Exception {

        debug("CahootsUtil", "sender account (3):" + stowaway2.getAccount());

        HashMap<String, String> utxo2Address = new HashMap<String, String>();
        List<UTXO> utxos = APIFactory.getInstance(context).getUtxos(true);
        for (UTXO utxo : utxos) {
            for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                utxo2Address.put(outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN(), outpoint.getAddress());
                debug("CahootsUtil", "outpoint address:" + outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN() + "," + outpoint.getAddress());
            }
        }

        Transaction transaction = stowaway2.getPSBT().getTransaction();
        HashMap<String, ECKey> keyBag_A = new HashMap<String, ECKey>();
        for (TransactionInput input : transaction.getInputs()) {
            TransactionOutPoint outpoint = input.getOutpoint();
            String key = outpoint.getHash().toString() + "-" + outpoint.getIndex();
            if (utxo2Address.containsKey(key)) {
                String address = utxo2Address.get(key);
                ECKey eckey = SendFactory.getPrivKey(address, 0);
                keyBag_A.put(outpoint.toString(), eckey);
            }
        }

        Stowaway stowaway3 = new Stowaway(stowaway2);
        stowaway3.inc(null, null, keyBag_A);

        return stowaway3;

    }

    //
    // sender
    //
    public Cahoots doStowaway4(Stowaway stowaway3) throws Exception {

        debug("CahootsUtil", "sender account (4):" + stowaway3.getAccount());

        HashMap<String, String> utxo2Address = new HashMap<String, String>();
        List<UTXO> utxos = null;
        if (stowaway3.getAccount() == WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix()) {
            utxos = APIFactory.getInstance(context).getUtxosPostMix(true);
        } else {
            utxos = APIFactory.getInstance(context).getUtxos(true);
        }
        for (UTXO utxo : utxos) {
            for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                utxo2Address.put(outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN(), outpoint.getAddress());
                debug("CahootsUtil", "outpoint address:" + outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN() + "," + outpoint.getAddress());
            }
        }

        Transaction transaction = stowaway3.getPSBT().getTransaction();
        HashMap<String, ECKey> keyBag_B = new HashMap<String, ECKey>();
        for (TransactionInput input : transaction.getInputs()) {
            TransactionOutPoint outpoint = input.getOutpoint();
            String key = outpoint.getHash().toString() + "-" + outpoint.getIndex();
            if (utxo2Address.containsKey(key)) {
                String address = utxo2Address.get(key);
                ECKey eckey = SendFactory.getPrivKey(address, stowaway3.getAccount());
                keyBag_B.put(outpoint.toString(), eckey);
            }
        }

        //
        //
        // step4: B verif, sig, broadcast
        //
        //

        Stowaway stowaway4 = new Stowaway(stowaway3);
        stowaway4.inc(null, null, keyBag_B);

        return stowaway4;

    }

    //
    // sender
    //
    public Cahoots doSTONEWALLx2_0(long spendAmount, String address, int account) {
        NetworkParameters params = SamouraiWallet.getInstance().getCurrentNetworkParams();

        //
        //
        // step0: B sends spend amount to A,  creates step0
        //
        //
        STONEWALLx2 stonewall0 = new STONEWALLx2(spendAmount, address, params, account);
        try {
            stonewall0.setFingerprint(HD_WalletFactory.getInstance(context).getFingerprint());
        }
        catch(IOException | MnemonicException.MnemonicLengthException e) {
            ;
        }

        return stonewall0;
    }

    //
    // counterparty
    //
    public Cahoots doSTONEWALLx2_1(STONEWALLx2 stonewall0) throws Exception {

        List<UTXO> utxos = getCahootsUTXO(stonewall0.getCounterpartyAccount());
        Collections.shuffle(utxos);

        debug("CahootsUtil", "BIP84 utxos:" + utxos.size());

        List<UTXO> selectedUTXO = new ArrayList<UTXO>();
        long totalContributedAmount = 0L;
        for (int step = 0; step < 3; step++) {

            if (stonewall0.getCounterpartyAccount() == 0) {
                step = 2;
            }

            List<String> seenTxs = new ArrayList<String>();
            selectedUTXO = new ArrayList<UTXO>();
            totalContributedAmount = 0L;
            for (UTXO utxo : utxos) {

                switch (step) {
                    case 0:
                        if (utxo.getPath() != null && utxo.getPath().length() > 3 && utxo.getPath().charAt(2) != '0') {
                            continue;
                        }
                        break;
                    case 1:
                        if (utxo.getPath() != null && utxo.getPath().length() > 3 && utxo.getPath().charAt(2) != '1') {
                            continue;
                        }
                        break;
                    default:
                        break;
                }

                UTXO _utxo = new UTXO();
                for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                    if (!seenTxs.contains(outpoint.getTxHash().toString())) {
                        _utxo.getOutpoints().add(outpoint);
                        seenTxs.add(outpoint.getTxHash().toString());
                    }
                }

                if (_utxo.getOutpoints().size() > 0) {
                    selectedUTXO.add(_utxo);
                    totalContributedAmount += _utxo.getValue();
                    debug("CahootsUtil", "BIP84 selected utxo:" + _utxo.getValue());
                }

                if (totalContributedAmount > stonewall0.getSpendAmount() + SamouraiWallet.bDust.longValue()) {
                    break;
                }
            }
            if (totalContributedAmount > stonewall0.getSpendAmount() + SamouraiWallet.bDust.longValue()) {
                break;
            }
        }
        if (!(totalContributedAmount > stonewall0.getSpendAmount() + SamouraiWallet.bDust.longValue())) {
            return null;
        }

        debug("CahootsUtil", "BIP84 selected utxos:" + selectedUTXO.size());

        NetworkParameters params = stonewall0.getParams();

        //
        //
        // step1: A utxos -> B (take largest that cover amount)
        //
        //

        String zpub = BIP84Util.getInstance(context).getWallet().getAccountAt(stonewall0.getCounterpartyAccount()).zpubstr();
        HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>> inputsA = new HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>>();

        for (UTXO utxo : selectedUTXO) {
            for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                _TransactionOutPoint _outpoint = new _TransactionOutPoint(outpoint);

                ECKey eckey = SendFactory.getPrivKey(_outpoint.getAddress(), stonewall0.getCounterpartyAccount());
                String path = APIFactory.getInstance(context).getUnspentPaths().get(_outpoint.getAddress());
                inputsA.put(_outpoint, Triple.of(eckey.getPubKey(), stonewall0.getFingerprintCollab(), path));
            }
        }

        HashMap<_TransactionOutput, Triple<byte[], byte[], String>> outputsA = new HashMap<_TransactionOutput, Triple<byte[], byte[], String>>();
        if (stonewall0.getCounterpartyAccount() == WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix()) {
            // contributor mix output
            int idx = AddressFactory.getInstance(context).getHighestPostChangeIdx();
            SegwitAddress segwitAddress0 = BIP84Util.getInstance(context).getAddressAt(stonewall0.getCounterpartyAccount(), 1, idx);
            Pair<Byte, byte[]> pair0 = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress0.getBech32AsString());
            byte[] scriptPubKey_A0 = Bech32Segwit.getScriptPubkey(pair0.getLeft(), pair0.getRight());
            _TransactionOutput output_A0 = new _TransactionOutput(params, null, Coin.valueOf(stonewall0.getSpendAmount()), scriptPubKey_A0);
            outputsA.put(output_A0, Triple.of(segwitAddress0.getECKey().getPubKey(), stonewall0.getFingerprintCollab(), "M/1/" + idx));

            // contributor change output
            ++idx;
            SegwitAddress segwitAddress1 = BIP84Util.getInstance(context).getAddressAt(stonewall0.getCounterpartyAccount(), 1, idx);
            Pair<Byte, byte[]> pair1 = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress1.getBech32AsString());
            byte[] scriptPubKey_A1 = Bech32Segwit.getScriptPubkey(pair1.getLeft(), pair1.getRight());
            _TransactionOutput output_A1 = new _TransactionOutput(params, null, Coin.valueOf(totalContributedAmount - stonewall0.getSpendAmount()), scriptPubKey_A1);
            outputsA.put(output_A1, Triple.of(segwitAddress1.getECKey().getPubKey(), stonewall0.getFingerprintCollab(), "M/1/" + idx));
        } else {
            // contributor mix output
            int idx = BIP84Util.getInstance(context).getWallet().getAccount(0).getReceive().getAddrIdx();
            SegwitAddress segwitAddress0 = BIP84Util.getInstance(context).getAddressAt(0, 0, idx);
            if (segwitAddress0.getBech32AsString().equalsIgnoreCase(stonewall0.getDestination())) {
                segwitAddress0 = BIP84Util.getInstance(context).getAddressAt(0, 0, idx + 1);
            }
            Pair<Byte, byte[]> pair0 = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress0.getBech32AsString());
            byte[] scriptPubKey_A0 = Bech32Segwit.getScriptPubkey(pair0.getLeft(), pair0.getRight());
            _TransactionOutput output_A0 = new _TransactionOutput(params, null, Coin.valueOf(stonewall0.getSpendAmount()), scriptPubKey_A0);
            outputsA.put(output_A0, Triple.of(segwitAddress0.getECKey().getPubKey(), stonewall0.getFingerprintCollab(), "M/0/" + idx));

            // contributor change output
            idx = BIP84Util.getInstance(context).getWallet().getAccount(0).getChange().getAddrIdx();
            SegwitAddress segwitAddress1 = BIP84Util.getInstance(context).getAddressAt(0, 1, idx);
            Pair<Byte, byte[]> pair1 = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress1.getBech32AsString());
            byte[] scriptPubKey_A1 = Bech32Segwit.getScriptPubkey(pair1.getLeft(), pair1.getRight());
            _TransactionOutput output_A1 = new _TransactionOutput(params, null, Coin.valueOf(totalContributedAmount - stonewall0.getSpendAmount()), scriptPubKey_A1);
            outputsA.put(output_A1, Triple.of(segwitAddress1.getECKey().getPubKey(), stonewall0.getFingerprintCollab(), "M/1/" + idx));
        }

        STONEWALLx2 stonewall1 = new STONEWALLx2(stonewall0);
        stonewall1.inc(inputsA, outputsA, null);

        return stonewall1;
    }

    //
    // sender
    //
    public Cahoots doSTONEWALLx2_2(STONEWALLx2 stonewall1) throws Exception {

        Transaction transaction = stonewall1.getTransaction();
        debug("CahootsUtil", "step2 tx:" + org.spongycastle.util.encoders.Hex.toHexString(transaction.bitcoinSerialize()));
        int nbIncomingInputs = transaction.getInputs().size();

        List<UTXO> utxos = getCahootsUTXO(stonewall1.getAccount());
        Collections.shuffle(utxos);

        debug("CahootsUtil", "BIP84 utxos:" + utxos.size());

        List<String> seenTxs = new ArrayList<String>();
        for (TransactionInput input : transaction.getInputs()) {
            if (!seenTxs.contains(input.getOutpoint().getHash().toString())) {
                seenTxs.add(input.getOutpoint().getHash().toString());
            }
        }

        List<UTXO> selectedUTXO = new ArrayList<UTXO>();
        long totalSelectedAmount = 0L;
        int nbTotalSelectedOutPoints = 0;
        for (int step = 0; step < 3; step++) {

            if (stonewall1.getCounterpartyAccount() == 0) {
                step = 2;
            }

            List<String> _seenTxs = seenTxs;
            selectedUTXO = new ArrayList<UTXO>();
            nbTotalSelectedOutPoints = 0;
            for (UTXO utxo : utxos) {

                switch (step) {
                    case 0:
                        if (utxo.getPath() != null && utxo.getPath().length() > 3 && utxo.getPath().charAt(2) != '0') {
                            continue;
                        }
                        break;
                    case 1:
                        if (utxo.getPath() != null && utxo.getPath().length() > 3 && utxo.getPath().charAt(2) != '1') {
                            continue;
                        }
                        break;
                    default:
                        break;
                }

                UTXO _utxo = new UTXO();
                for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                    if (!_seenTxs.contains(outpoint.getTxHash().toString())) {
                        _utxo.getOutpoints().add(outpoint);
                        _seenTxs.add(outpoint.getTxHash().toString());
                    }
                }

                if (_utxo.getOutpoints().size() > 0) {
                    selectedUTXO.add(_utxo);
                    totalSelectedAmount += _utxo.getValue();
                    nbTotalSelectedOutPoints += _utxo.getOutpoints().size();
                    debug("CahootsUtil", "BIP84 selected utxo:" + _utxo.getValue());
                }

                if (totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 4).longValue() + stonewall1.getSpendAmount() + SamouraiWallet.bDust.longValue()) {
                    break;
                }
            }
            if (totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 4).longValue() + stonewall1.getSpendAmount() + SamouraiWallet.bDust.longValue()) {
                break;
            }
        }
        if (!(totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 4).longValue() + stonewall1.getSpendAmount() + SamouraiWallet.bDust.longValue())) {
            return null;
        }

        debug("CahootsUtil", "BIP84 selected utxos:" + selectedUTXO.size());

        long fee = FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 4).longValue();
        debug("CahootsUtil", "fee:" + fee);
        if (fee % 2L != 0) {
            fee++;
        }
        debug("CahootsUtil", "fee pair:" + fee);
        stonewall1.setFeeAmount(fee);

        debug("CahootsUtil", "destination:" + stonewall1.getDestination());
        if (transaction.getOutputs() != null && transaction.getOutputs().size() == 2) {
            for (int i = 0; i < 2; i++) {
                byte[] buf = transaction.getOutputs().get(i).getScriptBytes();
                byte[] script = new byte[buf.length - 1];
                System.arraycopy(buf, 1, script, 0, script.length);
                debug("CahootsUtil", "script:" + new Script(script).toString());
                debug("CahootsUtil", "address from script:" + Bech32Util.getInstance().getAddressFromScript(new Script(script)));
                if (Bech32Util.getInstance().getAddressFromScript(new Script(script)) == null ||
                        (!Bech32Util.getInstance().getAddressFromScript(new Script(script)).equalsIgnoreCase(stonewall1.getDestination())
                                && transaction.getOutputs().get(i).getValue().longValue() != stonewall1.getSpendAmount())
                ) {
                    debug("CahootsUtil", "output value:" + transaction.getOutputs().get(i).getValue().longValue());
                    Coin value = transaction.getOutputs().get(i).getValue();
                    Coin _value = Coin.valueOf(value.longValue() - (fee / 2L));
                    debug("CahootsUtil", "output value post fee:" + _value);
                    transaction.getOutputs().get(i).setValue(_value);
                    stonewall1.getPSBT().setTransaction(transaction);
                    break;
                }
            }
        } else {
            return null;
        }

        NetworkParameters params = stonewall1.getParams();

        //
        //
        // step2: B verif, utxos -> A (take smallest that cover amount)
        //
        //

        String zpub = BIP84Util.getInstance(context).getWallet().getAccountAt(stonewall1.getAccount()).zpubstr();
        HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>> inputsB = new HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>>();

        for (UTXO utxo : selectedUTXO) {
            for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                _TransactionOutPoint _outpoint = new _TransactionOutPoint(outpoint);

                ECKey eckey = SendFactory.getPrivKey(_outpoint.getAddress(), stonewall1.getAccount());
                String path = APIFactory.getInstance(context).getUnspentPaths().get(_outpoint.getAddress());
                inputsB.put(_outpoint, Triple.of(eckey.getPubKey(), FormatsUtil.getInstance().getFingerprintFromXPUB(zpub), path));
            }
        }

        // spender change output
        HashMap<_TransactionOutput, Triple<byte[], byte[], String>> outputsB = new HashMap<_TransactionOutput, Triple<byte[], byte[], String>>();
        if (stonewall1.getAccount() == WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix()) {
            int idx = AddressFactory.getInstance(context).getHighestPostChangeIdx();
            SegwitAddress segwitAddress = BIP84Util.getInstance(context).getAddressAt(stonewall1.getAccount(), 1, idx);
            Pair<Byte, byte[]> pair0 = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress.getBech32AsString());
            byte[] scriptPubKey_B0 = Bech32Segwit.getScriptPubkey(pair0.getLeft(), pair0.getRight());
            _TransactionOutput output_B0 = new _TransactionOutput(params, null, Coin.valueOf((totalSelectedAmount - stonewall1.getSpendAmount()) - (fee / 2L)), scriptPubKey_B0);
            outputsB.put(output_B0, Triple.of(segwitAddress.getECKey().getPubKey(), stonewall1.getFingerprint(), "M/1/" + idx));
        } else {
            int idx = BIP84Util.getInstance(context).getWallet().getAccount(0).getChange().getAddrIdx();
            SegwitAddress segwitAddress = BIP84Util.getInstance(context).getAddressAt(0, 1, idx);
            Pair<Byte, byte[]> pair0 = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress.getBech32AsString());
            byte[] scriptPubKey_B0 = Bech32Segwit.getScriptPubkey(pair0.getLeft(), pair0.getRight());
            _TransactionOutput output_B0 = new _TransactionOutput(params, null, Coin.valueOf((totalSelectedAmount - stonewall1.getSpendAmount()) - (fee / 2L)), scriptPubKey_B0);
            outputsB.put(output_B0, Triple.of(segwitAddress.getECKey().getPubKey(), stonewall1.getFingerprint(), "M/1/" + idx));
        }

        STONEWALLx2 stonewall2 = new STONEWALLx2(stonewall1);
        stonewall2.inc(inputsB, outputsB, null);

        return stonewall2;
    }

    //
    // counterparty
    //
    public Cahoots doSTONEWALLx2_3(STONEWALLx2 stonewall2) throws Exception {

        HashMap<String, String> utxo2Address = new HashMap<String, String>();
        List<UTXO> utxos = null;
        if (stonewall2.getCounterpartyAccount() == WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix()) {
            utxos = APIFactory.getInstance(context).getUtxosPostMix(true);
        } else {
            utxos = APIFactory.getInstance(context).getUtxos(true);
        }
        for (UTXO utxo : utxos) {
            for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                utxo2Address.put(outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN(), outpoint.getAddress());
            }
        }

        Transaction transaction = stonewall2.getTransaction();
        HashMap<String, ECKey> keyBag_A = new HashMap<String, ECKey>();
        for (TransactionInput input : transaction.getInputs()) {
            TransactionOutPoint outpoint = input.getOutpoint();
            String key = outpoint.getHash().toString() + "-" + outpoint.getIndex();
            if (utxo2Address.containsKey(key)) {
                String address = utxo2Address.get(key);
                ECKey eckey = SendFactory.getPrivKey(address, stonewall2.getCounterpartyAccount());
                keyBag_A.put(outpoint.toString(), eckey);
            }
        }

        STONEWALLx2 stonewall3 = new STONEWALLx2(stonewall2);
        stonewall3.inc(null, null, keyBag_A);

        return stonewall3;
    }

    //
    // sender
    //
    public Cahoots doSTONEWALLx2_4(STONEWALLx2 stonewall3) throws Exception {

        HashMap<String, String> utxo2Address = new HashMap<String, String>();
        List<UTXO> utxos = null;
        if (stonewall3.getAccount() == WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix()) {
            utxos = APIFactory.getInstance(context).getUtxosPostMix(true);
        } else {
            utxos = APIFactory.getInstance(context).getUtxos(true);
        }
        for (UTXO utxo : utxos) {
            for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                utxo2Address.put(outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN(), outpoint.getAddress());
            }
        }

        Transaction transaction = stonewall3.getTransaction();
        HashMap<String, ECKey> keyBag_B = new HashMap<String, ECKey>();
        for (TransactionInput input : transaction.getInputs()) {
            TransactionOutPoint outpoint = input.getOutpoint();
            String key = outpoint.getHash().toString() + "-" + outpoint.getIndex();
            if (utxo2Address.containsKey(key)) {
                String address = utxo2Address.get(key);
                ECKey eckey = SendFactory.getPrivKey(address, stonewall3.getAccount());
                keyBag_B.put(outpoint.toString(), eckey);
            }
        }

        STONEWALLx2 stonewall4 = new STONEWALLx2(stonewall3);
        stonewall4.inc(null, null, keyBag_B);

        return stonewall4;

    }

    public static List<UTXO> getCahootsUTXO(int account) {
        List<UTXO> ret = new ArrayList<UTXO>();
        List<UTXO> _utxos = null;
        if(account == WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix())    {
            _utxos = APIFactory.getInstance(context).getUtxosPostMix(true);
        }
        else    {
            _utxos = APIFactory.getInstance(context).getUtxos(true);
        }
        for(UTXO utxo : _utxos)   {
            String script = Hex.toHexString(utxo.getOutpoints().get(0).getScriptBytes());
            if(script.startsWith("0014") && APIFactory.getInstance(context).getUnspentPaths().get(utxo.getOutpoints().get(0).getAddress()) != null)   {
                ret.add(utxo);
            }
        }

        return ret;
    }

    public long getCahootsValue(int account) {
        long ret = 0L;
        List<UTXO> _utxos = getCahootsUTXO(account);
        for(UTXO utxo : _utxos)   {
            ret += utxo.getValue();
        }

        return ret;
    }

}
