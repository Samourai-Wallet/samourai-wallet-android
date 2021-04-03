package com.samourai.whirlpool.client.wallet;

import android.content.Context;

import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.util.LogUtil;
import com.samourai.wallet.utxos.models.UTXOCoin;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.MixableStatus;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

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

    public Collection<String> getWhirlpoolTags(UTXOCoin item, Context ctx ) {
        List<String> tags = new LinkedList<>();
        WhirlpoolWallet whirlpoolWallet = AndroidWhirlpoolWalletService.getInstance().getWhirlpoolWalletOrNull();
        if (whirlpoolWallet != null) {
            WhirlpoolUtxo whirlpoolUtxo = whirlpoolWallet.getUtxoSupplier().findUtxo(item.hash, item.idx);

            if (whirlpoolUtxo != null && whirlpoolUtxo.getUtxo() != null ) {

                try {
                    if(WhirlpoolAccount.POSTMIX.equals(whirlpoolUtxo.getAccount()) && whirlpoolUtxo.getUtxo().getPath() != null && whirlpoolUtxo.getUtxo().getPath().contains("M/1/")){
                        return tags;
                    }
                } catch (Exception e) {
                    LogUtil.error("getWhirlpoolTags",e);
                    return tags;
                }
                // tag only premix & postmix utxos
                if (WhirlpoolAccount.PREMIX.equals(whirlpoolUtxo.getAccount()) || WhirlpoolAccount.POSTMIX.equals(whirlpoolUtxo.getAccount())) {
                    // show whirlpool tag
                    tags.add(whirlpoolUtxo.getMixsDone() + " MIXED");

                    // show reason when not mixable
                    MixableStatus mixableStatus = whirlpoolUtxo.getUtxoState().getMixableStatus();
                    switch (mixableStatus) {
                        case UNCONFIRMED:
                            tags.add("UNCONFIRMED");
                            break;
                        case NO_POOL:
                        case MIXABLE:
                            // ignore
                            break;
                    }

                    WhirlpoolUtxoStatus utxoStatus = whirlpoolUtxo.getUtxoState().getStatus();
                    switch (utxoStatus) {
                        case MIX_QUEUE:
                            tags.add("MIX QUEUED");
                            break;
                        case MIX_STARTED:
                            tags.add("MIXING");
                            break;
                        case MIX_SUCCESS:
                            tags.add("MIX SUCCESS");
                            break;
                        case MIX_FAILED:
                            tags.add("MIX FAILED");
                            break;
                        case STOP:
                            tags.add("MIX STOPPED");
                            break;
                        case TX0:
                        case TX0_SUCCESS:
                        case TX0_FAILED:
                        case READY:
                            // ignore
                            break;
                    }
                }
            }
        }
        return tags;
    }
}
