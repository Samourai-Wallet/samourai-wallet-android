package whirlpool;

import android.content.Context;
import android.test.mock.MockContext;

import com.samourai.http.client.AndroidHttpClient;
import com.samourai.stomp.client.AndroidStompClient;
import com.samourai.stomp.client.AndroidStompClientService;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.client.Bip84Wallet;
import com.samourai.wallet.client.indexHandler.MemoryIndexHandler;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.hd.HD_WalletFactoryGeneric;
import com.samourai.wallet.util.WebUtil;
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService;
import com.samourai.whirlpool.client.wallet.WhirlpoolUtils;

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
    protected HD_WalletFactoryGeneric hdWalletFactoryGeneric;
    protected AndroidHttpClient whirlpoolHttpClient = new AndroidHttpClient(WebUtil.getInstance(null));
    protected AndroidStompClientService stompClientService = new AndroidStompClientService();
    protected NetworkParameters networkParameters;
    protected AndroidWhirlpoolWalletService whirlpoolWalletService = AndroidWhirlpoolWalletService.getInstance();
    protected WhirlpoolUtils whirlpoolUtils = WhirlpoolUtils.getInstance();

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
        hdWalletFactoryGeneric = new HD_WalletFactoryGeneric(mc);

        // init Samourai Wallet
        SamouraiWallet.getInstance().setCurrentNetworkParams(networkParameters);
    }

    protected ECKey computeECKey(String utxoKey) {
        // input utxo key
        DumpedPrivateKey dumpedPrivateKey = new DumpedPrivateKey(networkParameters, utxoKey);
        ECKey ecKey = dumpedPrivateKey.getKey();
        return ecKey;
    }

    protected HD_Wallet computeBip84w(String seedWords, String passphrase) throws Exception {
        byte[] seed = hdWalletFactoryGeneric.computeSeedFromWords(seedWords);
        HD_Wallet bip84w = hdWalletFactoryGeneric.getBIP84(seed, passphrase, networkParameters);
        return bip84w;
    }

    protected Bip84Wallet computeBip84wallet(String seedWords, String passphrase) throws Exception {
        HD_Wallet bip84w = computeBip84w(seedWords, passphrase);
        Bip84Wallet bip84Wallet = new Bip84Wallet(bip84w, 0, new MemoryIndexHandler(), new MemoryIndexHandler());
        return bip84Wallet;
    }

    protected Context getContext()  {
        return new MockContext();
    }
}