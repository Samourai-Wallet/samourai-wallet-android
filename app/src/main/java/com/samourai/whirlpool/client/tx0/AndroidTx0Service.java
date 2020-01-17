package com.samourai.whirlpool.client.tx0;

import android.util.Log;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.backend.beans.UnspentResponse;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.segwit.bech32.Bech32Util;
import com.samourai.wallet.send.MyTransactionInput;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.util.FeeUtil;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionWitness;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Collection;

public class AndroidTx0Service extends Tx0Service {
    private Logger log = LoggerFactory.getLogger(AndroidTx0Service.class.getSimpleName());

    private final FeeUtil feeUtil = FeeUtil.getInstance();

    public AndroidTx0Service(WhirlpoolWalletConfig config) {
        super(config);
    }

    @Override
    protected long computeTx0MinerFee(int nbPremix, long feeTx0, Collection<? extends UnspentResponse.UnspentOutput> spendFroms) {
        int nbOutputsNonOpReturn = nbPremix + 2; // outputs + change + fee

        int nbP2PKH = 0;
        int nbP2SH = 0;
        int nbP2WPKH = 0;
        if (spendFroms != null) { // spendFroms can be NULL (for fee simulation)
            for (UnspentResponse.UnspentOutput uo : spendFroms) {

                if (Bech32Util.getInstance().isP2WPKHScript(uo.script)) {
                    nbP2WPKH++;
                } else {
                    String address = new Script(Hex.decode(uo.script)).getToAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
                    if (Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress()) {
                        nbP2SH++;
                    } else {
                        nbP2PKH++;
                    }
                }
            }
        }
        long tx0MinerFee = feeUtil.estimatedFeeSegwit(nbP2PKH, nbP2SH, nbP2WPKH, nbOutputsNonOpReturn, 1, feeTx0);
        Log.e("TX0", "nbP2PKH="+nbP2PKH+", nbP2SH="+nbP2SH+", nbP2WPKH="+nbP2WPKH+", nbOutputsNonOpReturn="+nbOutputsNonOpReturn+", feeTx0="+feeTx0+", tx0MinerFee="+tx0MinerFee);

        if (log.isDebugEnabled()) {
            log.debug(
                    "tx0 minerFee: "
                            + tx0MinerFee
                            + "sats, totalBytes="
                            + "b for nbPremix="
                            + nbPremix
                            + ", feeTx0="
                            + feeTx0);
        }
        return tx0MinerFee;
    }

    @Override
    protected void buildTx0Input(Transaction tx, UnspentOutputWithKey input, NetworkParameters params) {
        ECKey spendFromKey = ECKey.fromPrivate(input.getKey());
        TransactionOutPoint depositSpendFrom = input.computeOutpoint(params);

        SegwitAddress segwitAddress = new SegwitAddress(spendFromKey.getPubKey(), params);
        MyTransactionOutPoint _outpoint = new MyTransactionOutPoint(depositSpendFrom.getHash(), (int)depositSpendFrom.getIndex(), BigInteger.valueOf(depositSpendFrom.getValue().longValue()), segwitAddress.segWitRedeemScript().getProgram(), segwitAddress.getBech32AsString());
        MyTransactionInput _input = new MyTransactionInput(params, null, new byte[0], _outpoint, depositSpendFrom.getHash().toString(), (int)depositSpendFrom.getIndex());

        tx.addInput(_input);
    }

    @Override
    protected void signTx0(Transaction tx, Collection<UnspentOutputWithKey> inputs, NetworkParameters params) {
        int idx = 0;
        for (UnspentOutputWithKey input : inputs) {

            String address = input.addr;
            ECKey spendFromKey = ECKey.fromPrivate(input.getKey());

            // sign input
            if(FormatsUtil.getInstance().isValidBech32(address) || Address.fromBase58(params, address).isP2SHAddress())    {

                SegwitAddress segwitAddress = new SegwitAddress(spendFromKey.getPubKey(), params);
                final Script redeemScript = segwitAddress.segWitRedeemScript();
                final Script scriptCode = redeemScript.scriptCode();

                TransactionSignature sig = tx.calculateWitnessSignature(idx, spendFromKey, scriptCode, Coin.valueOf(input.value), Transaction.SigHash.ALL, false);
                final TransactionWitness witness = new TransactionWitness(2);
                witness.setPush(0, sig.encodeToBitcoin());
                witness.setPush(1, spendFromKey.getPubKey());
                tx.setWitness(idx, witness);

                if(!FormatsUtil.getInstance().isValidBech32(address) && Address.fromBase58(params, address).isP2SHAddress())    {
                    final ScriptBuilder sigScript = new ScriptBuilder();
                    sigScript.data(redeemScript.getProgram());
                    tx.getInput(idx).setScriptSig(sigScript.build());
//                    tx.getInput(idx).getScriptSig().correctlySpends(tx, idx, new Script(Hex.decode(input.script)), Coin.valueOf(input.value), Script.ALL_VERIFY_FLAGS);
                }

            }
            else    {
                TransactionSignature sig = tx.calculateSignature(idx, spendFromKey, new Script(Hex.decode(input.script)), Transaction.SigHash.ALL, false);
                tx.getInput(idx).setScriptSig(ScriptBuilder.createInputScript(sig, spendFromKey));
            }

            idx++;

        }
        super.signTx0(tx, inputs, params);
    }
}
