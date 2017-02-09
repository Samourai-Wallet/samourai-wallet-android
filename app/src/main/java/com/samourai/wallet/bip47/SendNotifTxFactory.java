package com.samourai.wallet.bip47;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;
import android.widget.Toast;
//import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Transaction.SigHash;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import com.samourai.wallet.OpCallback;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.crypto.DecryptionException;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.send.MyTransactionInput;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.UnspentOutputsBundle;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.Hash;
import com.samourai.wallet.util.PushTx;
import com.samourai.wallet.util.WebUtil;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.bip47.rpc.SecretPoint;
import com.samourai.wallet.R;

import org.bitcoinj.script.ScriptOpCodes;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SendNotifTxFactory	{

    public static final BigInteger _bNotifTxValue = SamouraiWallet.bDust;
    public static final BigInteger _bFee = BigInteger.valueOf(Coin.parseCoin("0.000225").longValue());
    public static final BigInteger _bSWFee = SamouraiWallet.bFee;

    public static final BigInteger _bNotifTxTotalAmount = _bFee.add(_bSWFee).add(_bNotifTxValue);

    public static final String SAMOURAI_NOTIF_TX_FEE_ADDRESS = "3Pof32GmAoSpUfzPiCWTu3y7Ni9qxM7Hvc";

    private static SendNotifTxFactory instance = null;
    private static Context context = null;

    private SendNotifTxFactory () { ; }

    public static SendNotifTxFactory getInstance(Context ctx) {

        context = ctx;

        if(instance == null)	{
            instance = new SendNotifTxFactory();
        }

        return instance;
    }

}
