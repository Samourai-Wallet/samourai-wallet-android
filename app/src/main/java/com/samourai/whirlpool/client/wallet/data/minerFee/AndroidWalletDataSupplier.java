package com.samourai.whirlpool.client.wallet.data.minerFee;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.whirlpool.client.utils.MessageListener;
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoChanges;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AndroidWalletDataSupplier extends WalletDataSupplier {
    private final Logger log = LoggerFactory.getLogger(AndroidWalletDataSupplier.class);

    private ObjectMapper objectMapper;

    public AndroidWalletDataSupplier(int refreshUtxoDelay,
                                     WalletSupplier walletSupplier,
                                     PoolSupplier poolSupplier,
                                     MessageListener<WhirlpoolUtxoChanges> utxoChangesListener,
                                     String utxoConfigFileName,
                                     WhirlpoolWalletConfig config) {
        super(refreshUtxoDelay, walletSupplier, poolSupplier, utxoChangesListener, utxoConfigFileName, config);

        this.objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    protected WalletResponse fetchWalletResponse() throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("fetchWalletResponse()");
        }
        JSONObject data = AndroidWhirlpoolWalletService.getInstance().getWhirlpoolWalletResponse();
        if (data == null) {
            // should never happen
            throw new Exception("No walletResponse available");
        }
        return objectMapper.readValue(data.toString(), WalletResponse.class);
    }
}
