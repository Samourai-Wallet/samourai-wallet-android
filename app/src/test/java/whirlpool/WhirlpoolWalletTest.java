package whirlpool;

import com.google.common.util.concurrent.SettableFuture;
import com.samourai.api.client.SamouraiApi;
import com.samourai.http.client.AndroidHttpClient;
import com.samourai.http.client.IHttpClient;
import com.samourai.stomp.client.AndroidStompClientService;
import com.samourai.stomp.client.IStompClientService;
import com.samourai.wallet.MainActivity2;
import com.samourai.wallet.api.backend.BackendServer;
import com.samourai.wallet.api.backend.beans.UnspentResponse;
import com.samourai.wallet.client.Bip84Wallet;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.util.WebUtil;
import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.handler.Bip84PostmixHandler;
import com.samourai.whirlpool.client.mix.handler.IPostmixHandler;
import com.samourai.whirlpool.client.mix.handler.IPremixHandler;
import com.samourai.whirlpool.client.mix.handler.PremixHandler;
import com.samourai.whirlpool.client.mix.handler.UtxoWithBalance;
import com.samourai.whirlpool.client.mix.listener.MixFailReason;
import com.samourai.whirlpool.client.mix.listener.MixStep;
import com.samourai.whirlpool.client.mix.listener.MixSuccess;
import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletService;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolServer;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolWalletState;
import com.samourai.whirlpool.client.wallet.persist.FileWhirlpoolWalletPersistHandler;
import com.samourai.whirlpool.client.wallet.persist.WhirlpoolWalletPersistHandler;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientImpl;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.beans.Pools;
import com.samourai.whirlpool.client.whirlpool.listener.LoggingWhirlpoolClientListener;
import com.samourai.whirlpool.client.whirlpool.listener.WhirlpoolClientListener;

import junit.framework.Assert;

import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.TestNet3Params;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;

@Ignore
public class WhirlpoolWalletTest extends AbstractWhirlpoolTest {
    private Logger log = LoggerFactory.getLogger(WhirlpoolWalletTest.class.getSimpleName());

    @Before
    public void setUp() throws Exception {
        super.setUp(networkParameters);
    }

    @Test
    public void test() throws Exception {
        // configure wallet
        WhirlpoolWallet whirlpoolWallet = AndroidWhirlpoolWalletService.getInstance().openWallet(null);

        // start whirlpool wallet
        whirlpoolWallet.start();

        // get state
        WhirlpoolWalletState whirlpoolWalletState = whirlpoolWallet.getState();

        // list pools
        Collection<Pool> pools = whirlpoolWallet.getPools();

        // find pool by poolId
        Pool pool05btc = whirlpoolWallet.findPoolById("0.5btc");

        // tx0 method 1: spending a whirlpool-managed utxo
        {
            // whirlpool utxo for tx0
            String utxoHash = "6517ece36402a89d76d075c60a8d3d0e051e4e5efa42a01c9033328707631b61";
            int utxoIndex = 2;
            WhirlpoolUtxo whirlpoolUtxo = whirlpoolWallet.findUtxo(utxoHash, utxoIndex);
            if (whirlpoolUtxo == null) {} // utxo not found

            // find eligible pools for this utxo
            Tx0FeeTarget feeTarget = Tx0FeeTarget.BLOCKS_4;
            Collection<Pool> eligiblePools =
                    whirlpoolWallet.findPoolsForTx0(whirlpoolUtxo.getUtxo().value, 1, feeTarget);

            // choose pool
            whirlpoolWallet.setPool(whirlpoolUtxo, "0.01btc");

            // execute tx0
            try {
                Tx0 tx0 = whirlpoolWallet.tx0(whirlpoolUtxo, feeTarget);
                String txid = tx0.getTx().getHashAsString(); // get txid
            } catch (Exception e) {
                // tx0 failed
            }
        }

        // tx0 method 2: spending an external utxo
        {
            // external utxo for tx0
            UnspentResponse.UnspentOutput spendFrom = null; // provide utxo outpoint
            byte[] spendFromPrivKey = null; // provide utxo private key
            long spendFromValue = 12345678; // provide utxo value

            // pool for tx0
            Pool pool = whirlpoolWallet.findPoolById("0.01btc"); // provide poolId
            Tx0FeeTarget feeTarget = Tx0FeeTarget.BLOCKS_4;

            // execute tx0
            try {
                Tx0 tx0 =
                        whirlpoolWallet.tx0(
                                spendFrom, spendFromPrivKey, spendFromValue, pool, feeTarget);
                String txid = tx0.getTx().getHashAsString(); // get txid
            } catch (Exception e) {
                // tx0 failed
            }
        }

        // list premix utxos
        Collection<WhirlpoolUtxo> utxosPremix = whirlpoolWallet.getUtxosPremix();

        // mix specific utxo
        WhirlpoolUtxo whirlpoolUtxo = utxosPremix.iterator().next();
        WhirlpoolClientListener listener =
                new WhirlpoolClientListener() {
                    @Override
                    public void success(MixSuccess mixSuccess) {
                        // mix success
                    }

                    @Override
                    public void fail(MixFailReason reason, String notifiableError) {
                        // mix failed
                    }

                    @Override
                    public void progress(MixStep step) {
                        // mix progress
                    }
                };
        whirlpoolWallet.mix(whirlpoolUtxo, listener);

        // stop mixing specific utxo
        whirlpoolWallet.mixStop(whirlpoolUtxo);

        // get global mix state
        WhirlpoolWalletState whirlpoolState = whirlpoolWallet.getState();
    }
}