package util;

import com.samourai.wallet.tor.TorManager;

public class MockTorManager extends TorManager {

    public MockTorManager() {
        super();
    }

    @Override
    public boolean isRequired() {
        return false;
    }
}
