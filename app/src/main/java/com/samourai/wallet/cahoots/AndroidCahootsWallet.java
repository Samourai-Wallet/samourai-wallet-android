package com.samourai.wallet.cahoots;

import android.content.Context;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.segwit.BIP84Wallet;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;

import java.util.LinkedList;
import java.util.List;

public class AndroidCahootsWallet extends CahootsWallet {
    private APIFactory apiFactory;
    private AddressFactory addressFactory;

    public AndroidCahootsWallet(Context ctx) {
        super(computeBip84Wallet(ctx), SamouraiWallet.getInstance().getCurrentNetworkParams());
        this.apiFactory = APIFactory.getInstance(ctx);
        this.addressFactory = AddressFactory.getInstance(ctx);
    }

    @Override
    public long fetchFeePerB() {
        long feePerB = FeeUtil.getInstance().getSuggestedFeeDefaultPerB();
        return feePerB;
    }

    @Override
    public int fetchPostChangeIndex() {
        return addressFactory.getHighestPostChangeIdx();
    }

    @Override
    protected List<CahootsUtxo> fetchUtxos(int account) {
        List<UTXO> apiUtxos;
        if(account == WhirlpoolAccount.POSTMIX.getAccountIndex())    {
            apiUtxos = apiFactory.getUtxosPostMix(true);
        }
        else    {
            apiUtxos = apiFactory.getUtxos(true);
        }

        List<CahootsUtxo> utxos = new LinkedList<>();
        for(UTXO utxo : apiUtxos)   {
            MyTransactionOutPoint outpoint = utxo.getOutpoints().get(0);
            String address = outpoint.getAddress();
            String path = apiFactory.getUnspentPaths().get(address);
            if(path != null)   {
                ECKey ecKey = SendFactory.getPrivKey(address, account);
                CahootsUtxo cahootsUtxo = new CahootsUtxo(outpoint, path, ecKey);
                utxos.add(cahootsUtxo);
            }
        }
        return utxos;
    }

    private static BIP84Wallet computeBip84Wallet(Context ctx) {
        try {
            HD_Wallet bip84w = BIP84Util.getInstance(ctx).getWallet();
            NetworkParameters params = SamouraiWallet.getInstance().getCurrentNetworkParams();
            BIP84Wallet bip84Wallet = new BIP84Wallet(bip84w, params);
            return bip84Wallet;
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
