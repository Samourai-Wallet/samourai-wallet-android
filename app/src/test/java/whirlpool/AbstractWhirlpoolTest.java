package whirlpool;

import android.content.Context;
import android.test.mock.MockContext;

import com.masanari.wallet.MasanariWallet;
import com.masanari.wallet.bip47.BIP47Util;

import com.masanari.wallet.hd.HD_WalletFactory;
import com.masanari.wallet.util.WebUtil;
import com.masanari.http.client.AndroidHttpClient;
import com.masanari.stomp.client.AndroidStompClient;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.hd.HD_Wallet;

import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.concurrent.Callable;

import io.reactivex.Scheduler;
import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public abstract class AbstractWhirlpoolTest {
    private Logger log = LoggerFactory.getLogger(AndroidStompClient.class.getSimpleName());

    protected Context context = new MockContext();
    protected BIP47Util bip47Util = BIP47Util.getInstance(context);
    protected HD_WalletFactory hdWalletFactory = HD_WalletFactory.getInstance(context);
    protected AndroidHttpClient whirlpoolHttpClient = new AndroidHttpClient(WebUtil.getInstance(null));
    protected AndroidStompClient stompClient = new AndroidStompClient();
    protected NetworkParameters networkParameters;

    public void setUp(NetworkParameters networkParameters) throws Exception {
        this.networkParameters = networkParameters;

        // mock main thread
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(new Function<Callable<Scheduler>, Scheduler>() {
            @Override
            public Scheduler apply(Callable<Scheduler> schedulerCallable) throws Exception {
                return Schedulers.trampoline();
            }
        });

        // resolve mnemonicCode
        InputStream wis = getClass().getResourceAsStream("/BIP39/en.txt");
        MnemonicCode mc = new MnemonicCode(wis, HD_WalletFactory.BIP39_ENGLISH_SHA256);
        hdWalletFactory.__setMnemonicCode(mc);
        wis.close();

        // init Masanari Wallet
        MasanariWallet.getInstance().setCurrentNetworkParams(networkParameters);
    }

    protected ECKey computeECKey(String utxoKey) {
        // input utxo key
        DumpedPrivateKey dumpedPrivateKey = new DumpedPrivateKey(networkParameters, utxoKey);
        ECKey ecKey = dumpedPrivateKey.getKey();
        return ecKey;
    }

    protected BIP47Wallet computeBip47wallet(String seedWords, String passphrase) throws Exception {
        HD_Wallet bip44w = hdWalletFactory.restoreWallet(seedWords, passphrase, 1);
        BIP47Wallet bip47w = hdWalletFactory.getBIP47();
        return bip47w;
    }
}
