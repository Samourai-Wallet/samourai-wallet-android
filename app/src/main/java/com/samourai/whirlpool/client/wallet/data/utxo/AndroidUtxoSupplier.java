package com.samourai.whirlpool.client.wallet.data.utxo;

import android.util.Log;

import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.beans.UnspentResponse;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;
import com.samourai.whirlpool.client.utils.MessageListener;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoChanges;
import com.samourai.whirlpool.client.wallet.data.minerFee.WalletSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;

public class AndroidUtxoSupplier extends UtxoSupplier {
    private final Logger log = LoggerFactory.getLogger(AndroidUtxoSupplier.class);
    private APIFactory apiFactory;
    private BIP84Util bip84Util;
    private WhirlpoolMeta whirlpoolMeta;

    public AndroidUtxoSupplier(
            int refreshUtxoDelay,
            WalletSupplier walletSupplier,
            UtxoConfigSupplier utxoConfigSupplier,
            BackendApi backendApi,
            MessageListener<WhirlpoolUtxoChanges> utxoChangesListener,
            APIFactory apiFactory,
            BIP84Util bip84Util,
            WhirlpoolMeta whirlpoolMeta) {
        super(refreshUtxoDelay, walletSupplier, utxoConfigSupplier, backendApi, utxoChangesListener);
        this.apiFactory = apiFactory;
        this.bip84Util = bip84Util;
        this.whirlpoolMeta = whirlpoolMeta;
    }

    @Override
    protected List<UnspentResponse.UnspentOutput> fetchUtxos() throws Exception {
        List<UnspentResponse.UnspentOutput> unspentOutputs = new LinkedList<>();

        // deposit bip84
        String zpubDeposit = bip84Util.getWallet().getAccountAt(0).zpubstr();
        unspentOutputs.addAll(toUnspentOutputs(apiFactory.getUtxos(true), zpubDeposit));

        // premix
        String zpubPremix = bip84Util.getWallet().getAccountAt(whirlpoolMeta.getWhirlpoolPremixAccount()).zpubstr();
        unspentOutputs.addAll(toUnspentOutputs(apiFactory.getUtxosPreMix(true), zpubPremix));

        // postmix
        String zpubPostmix = bip84Util.getWallet().getAccountAt(whirlpoolMeta.getWhirlpoolPostmix()).zpubstr();
        unspentOutputs.addAll(toUnspentOutputs(apiFactory.getUtxosPostMix(true), zpubPostmix));

        return unspentOutputs;
    }

    private List<UnspentResponse.UnspentOutput> toUnspentOutputs(Collection<UTXO> utxos, String zpub) {
        return StreamSupport.stream(utxos).map(utxo -> toUnspentOutput(utxo, zpub)).collect(Collectors.toList());
    }

    private UnspentResponse.UnspentOutput toUnspentOutput(UTXO utxo, String zpub) {
        UnspentResponse.UnspentOutput u = new UnspentResponse.UnspentOutput();
        MyTransactionOutPoint o = utxo.getOutpoints().iterator().next();
        u.addr = o.getAddress();
        u.confirmations = o.getConfirmations();
        u.script = null; // TODO ?
        u.tx_hash = o.getTxHash().toString();
        u.tx_output_n = o.getTxOutputN();
        u.value = o.getValue().value;
        u.xpub = new UnspentResponse.UnspentOutput.Xpub();
        u.xpub.path = utxo.getPath();
        u.xpub.m = zpub;
        return u;
    }
}
