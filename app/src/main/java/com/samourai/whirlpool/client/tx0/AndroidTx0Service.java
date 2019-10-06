package com.samourai.whirlpool.client.tx0;

import com.samourai.stomp.client.AndroidStompClient;
import com.samourai.wallet.api.backend.beans.UnspentResponse;
import com.samourai.wallet.bip69.BIP69OutputComparator;
import com.samourai.wallet.client.Bip84Wallet;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.wallet.util.FeeUtil;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.protocol.fee.WhirlpoolFee;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptOpCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AndroidTx0Service extends Tx0Service {
    private Logger log = LoggerFactory.getLogger(AndroidTx0Service.class.getSimpleName());

    private final Bech32UtilGeneric bech32Util = Bech32UtilGeneric.getInstance();
    private final FeeUtil feeUtil = FeeUtil.getInstance();

    public AndroidTx0Service(WhirlpoolWalletConfig config) {
        super(config);
    }

    protected long computeTx0MinerFee(int nbPremix, long feeTx0, Collection<UnspentResponse.UnspentOutput> spendFroms) {
        int nbOutputsNonOpReturn = nbPremix + 2; // outputs + change + fee

        // spend from N bech32 input
        int nbSpendFroms = (spendFroms != null ? spendFroms.size() : 1);
        long tx0MinerFee = feeUtil.estimatedFeeSegwit(0, 0, nbSpendFroms, nbOutputsNonOpReturn, 1, feeTx0);

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
    protected Transaction buildTx0(
            Collection<byte[]> spendFromPrivKeys,
            Collection<UnspentResponse.UnspentOutput> depositSpendFroms,
            Bip84Wallet depositWallet,
            Bip84Wallet premixWallet,
            long premixValue,
            long samouraiFee,
            byte[] opReturnValue,
            String feeAddressBech32,
            NetworkParameters params,
            int nbPremix,
            long tx0MinerFee,
            long changeValue)
            throws Exception {


        // TODO ADAPT FOR ANDROID DEPOSIT


        //
        // tx0
        //

        //
        // make tx:
        // 5 spendTo outputs
        // SW fee
        // change
        // OP_RETURN
        //
        List<TransactionOutput> outputs = new ArrayList<TransactionOutput>();
        Transaction tx = new Transaction(params);

        //
        // premix outputs
        //
        for (int j = 0; j < nbPremix; j++) {
            // send to PREMIX
            HD_Address toAddress = premixWallet.getNextAddress();
            String toAddressBech32 = bech32Util.toBech32(toAddress, params);
            if (log.isDebugEnabled()) {
                log.debug(
                        "Tx0 out (premix): address="
                                + toAddressBech32
                                + ", path="
                                + toAddress.toJSON().get("path")
                                + " ("
                                + premixValue
                                + " sats)");
            }

            TransactionOutput txOutSpend =
                    bech32Util.getTransactionOutput(toAddressBech32, premixValue, params);
            outputs.add(txOutSpend);
        }

        if (changeValue > 0) {
            //
            // 1 change output
            //
            HD_Address changeAddress = depositWallet.getNextChangeAddress();
            String changeAddressBech32 = bech32Util.toBech32(changeAddress, params);
            TransactionOutput txChange =
                    bech32Util.getTransactionOutput(changeAddressBech32, changeValue, params);
            outputs.add(txChange);
            if (log.isDebugEnabled()) {
                log.debug(
                        "Tx0 out (change): address="
                                + changeAddressBech32
                                + ", path="
                                + changeAddress.toJSON().get("path")
                                + " ("
                                + changeValue
                                + " sats)");
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Tx0: spending whole utx0, no change");
            }
            if (changeValue < 0) {
                throw new Exception(
                        "Negative change detected, please report this bug. changeValue="
                                + changeValue
                                + ", tx0MinerFee="
                                + tx0MinerFee);
            }
        }

        // samourai fee
        TransactionOutput txSWFee =
                bech32Util.getTransactionOutput(feeAddressBech32, samouraiFee, params);
        outputs.add(txSWFee);
        if (log.isDebugEnabled()) {
            log.debug("Tx0 out (fee): feeAddress=" + feeAddressBech32 + " (" + samouraiFee + " sats)");
        }

        // add OP_RETURN output
        Script op_returnOutputScript =
                new ScriptBuilder().op(ScriptOpCodes.OP_RETURN).data(opReturnValue).build();
        TransactionOutput txFeeOutput =
                new TransactionOutput(params, null, Coin.valueOf(0L), op_returnOutputScript.getProgram());
        outputs.add(txFeeOutput);
        if (log.isDebugEnabled()) {
            log.debug("Tx0 out (OP_RETURN): " + opReturnValue.length + " bytes");
        }
        if (opReturnValue.length != WhirlpoolFee.FEE_LENGTH) {
            throw new Exception(
                    "Invalid opReturnValue length detected, please report this bug. opReturnValue="
                            + opReturnValue
                            + " vs "
                            + WhirlpoolFee.FEE_LENGTH);
        }

        // all outputs
        Collections.sort(outputs, new BIP69OutputComparator());
        for (TransactionOutput to : outputs) {
            tx.addOutput(to);
        }

        // input
        ECKey spendFromKey = ECKey.fromPrivate(spendFromPrivKeys.iterator().next());

        final Script segwitPubkeyScript = ScriptBuilder.createP2WPKHOutputScript(spendFromKey);

        // TODO sign all depositSpendFroms
        tx.addSignedInput(depositSpendFroms.iterator().next().computeOutpoint(params), segwitPubkeyScript, spendFromKey);
        if (log.isDebugEnabled()) {
            log.debug(
                    "Tx0 in: utxo="
                            + depositSpendFroms);
            log.debug("Tx0 fee: " + tx0MinerFee + " sats");
        }
        tx.verify();
        return tx;
    }
}
