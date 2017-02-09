package com.samourai.wallet.bip47;

import android.content.Context;

import org.bitcoinj.core.Coin;
import com.samourai.wallet.SamouraiWallet;

import java.math.BigInteger;

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
