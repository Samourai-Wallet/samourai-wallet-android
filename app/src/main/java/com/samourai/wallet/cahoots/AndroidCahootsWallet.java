package com.samourai.wallet.cahoots;

import android.content.Context;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.segwit.BIP84Wallet;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bouncycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

public class AndroidCahootsWallet extends CahootsWallet {
    private APIFactory apiFactory;
    private AddressFactory addressFactory;
    private BIP84Util bip84Util;

    public AndroidCahootsWallet(Context ctx) {
        this.apiFactory = APIFactory.getInstance(ctx);
        this.addressFactory = AddressFactory.getInstance(ctx);
        this.bip84Util = BIP84Util.getInstance(ctx);
    }

    @Override
    public ECKey getPrivKey(String address, int account) {
        ECKey ecKey = SendFactory.getPrivKey(address, account);
        return ecKey;
    }

    @Override
    public String getUnspentPath(String address) {
        String unspentPath = apiFactory.getUnspentPaths().get(address);
        return unspentPath;
    }

    @Override
    public List<UTXO> getCahootsUTXO(int account) {
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
    public int getHighestPostChangeIdx() {
        return addressFactory.getHighestPostChangeIdx();
    }

    @Override
    public BIP84Wallet getBip84Wallet() {
        try {
            HD_Wallet bip84w = bip84Util.getWallet();
            NetworkParameters params = SamouraiWallet.getInstance().getCurrentNetworkParams();
            BIP84Wallet bip84Wallet = new BIP84Wallet(bip84w, params);
            return bip84Wallet;
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
