package com.samourai.whirlpool.client.wallet;

import com.samourai.wallet.api.backend.beans.UnspentResponse;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;

import java.util.List;

public class AndroidWhirlpoolDataService extends WhirlpoolDataService {

    public AndroidWhirlpoolDataService(WhirlpoolWalletConfig config, WhirlpoolWalletService whirlpoolWalletService) {
        super(config, whirlpoolWalletService);
    }

    @Override
    protected List<UnspentResponse.UnspentOutput> fetchUtxos(WhirlpoolAccount whirlpoolAccount, WhirlpoolWallet whirlpoolWallet) throws Exception {
        // TODO read Whirlpool UTXOs from ApiFactory
        return super.fetchUtxos(whirlpoolAccount, whirlpoolWallet);
    }
}
