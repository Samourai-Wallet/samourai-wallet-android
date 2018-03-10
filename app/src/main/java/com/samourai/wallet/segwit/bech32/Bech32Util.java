package com.samourai.wallet.segwit.bech32;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactory;

import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.params.TestNet3Params;
import org.bouncycastle.util.encoders.Hex;

import java.io.IOException;

public class Bech32Util {

    private static Bech32Util instance = null;

    private Bech32Util() { ; }

    public static Bech32Util getInstance() {

        if(instance == null) {
            instance = new Bech32Util();
        }

        return instance;
    }

    public boolean isBech32Script(String script) {
        return isP2WPKHScript(script) || isP2WSHScript(script);
    }

    public boolean isP2WPKHScript(String script) {
        return script.startsWith("0014");
    }

    public boolean isP2WSHScript(String script) {
        return script.startsWith("0020");
    }

    public String getAddressFromScript(String script) throws Exception    {

        String hrp = null;
        if(SamouraiWallet.getInstance().getCurrentNetworkParams() instanceof TestNet3Params)    {
            hrp = "tb";
        }
        else    {
            hrp = "bc";
        }

        return Bech32Segwit.encode(hrp, (byte)0x00, Hex.decode(script.substring(4).getBytes()));
    }

}
