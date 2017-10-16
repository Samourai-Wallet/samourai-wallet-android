package com.samourai.wallet.bip47;

import com.samourai.wallet.SamouraiWallet;

import java.math.BigInteger;

public class SendNotifTxFactory	{

    public static final BigInteger _bNotifTxValue = SamouraiWallet.bDust;
    public static final BigInteger _bSWFee = SamouraiWallet.bFee;
    public static final BigInteger _bSWCeilingFee = BigInteger.valueOf(50000L);

    public static final String SAMOURAI_NOTIF_TX_FEE_ADDRESS = "3CiUuZYNjRtBS4mpvpjLT2TudS8KC1pnn6";
    public static final String TESTNET_SAMOURAI_NOTIF_TX_FEE_ADDRESS = "mqfrnLppGyXX6D8f3UNA5977Po2UhbxDLn";

    public static final double _dSWFeeUSD = 0.5;

    private SendNotifTxFactory () { ; }

}
