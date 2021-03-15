package com.samourai.whirlpool.client.wallet;

import com.samourai.tor.client.TorClientService;
import com.samourai.wallet.tor.TorManager;

import io.matthewnelson.topl_service.TorServiceController;

public class AndroidWhirlpoolTorService extends TorClientService {
    private TorManager torManager;

    public AndroidWhirlpoolTorService(TorManager torManager) {
        super();
        this.torManager = torManager;
    }

    @Override
    public void changeIdentity() {
        if (torManager.isRequired()) {
            TorServiceController.newIdentity();
        }
    }
}
