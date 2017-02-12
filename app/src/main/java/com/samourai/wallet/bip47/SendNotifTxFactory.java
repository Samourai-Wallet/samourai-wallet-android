package com.samourai.wallet.bip47;

import com.samourai.wallet.SamouraiWallet;

import java.math.BigInteger;

public class SendNotifTxFactory	{

    public static final BigInteger _bNotifTxValue = SamouraiWallet.bDust;
    public static final BigInteger _bSWFee = SamouraiWallet.bFee;

    public static final String SAMOURAI_NOTIF_TX_FEE_ADDRESS = "3Pof32GmAoSpUfzPiCWTu3y7Ni9qxM7Hvc";

    private SendNotifTxFactory () { ; }

}
