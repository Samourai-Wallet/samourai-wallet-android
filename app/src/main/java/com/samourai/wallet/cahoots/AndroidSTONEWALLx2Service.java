package com.samourai.wallet.cahoots;

import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bouncycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

public class AndroidSTONEWALLx2Service extends STONEWALLx2Service {
    private APIFactory apiFactory;
    private AddressFactory addressFactory;

    public AndroidSTONEWALLx2Service(HD_Wallet hdWallet, NetworkParameters params, APIFactory apiFactory, AddressFactory addressFactory) {
        super(hdWallet, params);
        this.apiFactory = apiFactory;
        this.addressFactory = addressFactory;
    }

    @Override
    ECKey getPrivKey(String address, int account) {
        ECKey ecKey = SendFactory.getPrivKey(address, account);
        return ecKey;
    }

    @Override
    String getUnspentPath(String address) {
        String unspentPath = apiFactory.getUnspentPaths().get(address);
        return unspentPath;
    }

    @Override
    List<UTXO> getCahootsUTXO(int account) {
        List<UTXO> ret = new ArrayList<UTXO>();
        List<UTXO> _utxos;
        if(account == WhirlpoolAccount.POSTMIX.getAccountIndex())    {
            _utxos = apiFactory.getUtxosPostMix(true);
        }
        else    {
            _utxos = apiFactory.getUtxos(true);
        }
        for(UTXO utxo : _utxos)   {
            String script = Hex.toHexString(utxo.getOutpoints().get(0).getScriptBytes());
            if(script.startsWith("0014") && apiFactory.getUnspentPaths().get(utxo.getOutpoints().get(0).getAddress()) != null)   {
                ret.add(utxo);
            }
        }
        return ret;
    }

    @Override
    long getFeePerB() {
        return 0;
    }

    @Override
    int getHighestPostChangeIdx() {
        return addressFactory.getHighestPostChangeIdx();
    }
}
