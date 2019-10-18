package com.samourai.whirlpool.client.wallet;

import android.content.Context;

import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.ClientUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class WhirlpoolUtils {
    private static final Logger LOG = LoggerFactory.getLogger(WhirlpoolUtils.class);
    private static WhirlpoolUtils instance;

    public static WhirlpoolUtils getInstance() {
        if (instance == null) {
            instance = new WhirlpoolUtils();
        }
        return instance;
    }

    public String computeWalletIdentifier(HD_Wallet bip84w) {
        return ClientUtils.sha256Hash(bip84w.getAccountAt(0).zpubstr());
    }

    public File computeIndexFile(String walletIdentifier, Context ctx) throws NotifiableException {
        String path = "whirlpool-cli-state-" + walletIdentifier + ".json";
        if (LOG.isDebugEnabled()) {
            LOG.debug("indexFile: " + path);
        }
        return computeFile(path, ctx);
    }

    public File computeUtxosFile(String walletIdentifier, Context ctx) throws NotifiableException {
        String path = "whirlpool-cli-utxos-" + walletIdentifier + ".json";
        if (LOG.isDebugEnabled()) {
            LOG.debug("utxosFile: " + path);
        }
        return computeFile(path, ctx);
    }

    private File computeFile(String path, Context ctx) throws NotifiableException {
        File f = new File(ctx.getFilesDir(), path);
        if (!f.exists()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Creating file " + path);
            }
            try {
                f.createNewFile();
            } catch (Exception e) {
                throw new NotifiableException("Unable to write file " + path);
            }
        }
        return f;
    }
}
