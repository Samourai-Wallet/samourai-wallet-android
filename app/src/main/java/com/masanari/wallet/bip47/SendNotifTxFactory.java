package com.masanari.wallet.bip47;

import com.masanari.wallet.MasanariWallet;

import java.math.BigInteger;

public class SendNotifTxFactory	{

    public static final BigInteger _bNotifTxValue = MasanariWallet.bDust;
    public static final BigInteger _bSWFee = MasanariWallet.bFee;
//    public static final BigInteger _bSWCeilingFee = BigInteger.valueOf(50000L);

    public static final String MASANARI_NOTIF_TX_FEE_ADDRESS = "bc1qnc254nxjfj6ma093ctg79z96klvksvntmkffuq";
    public static final String TESTNET_MASANARI_NOTIF_TX_FEE_ADDRESS = "tb1qh287jqsh6mkpqmd8euumyfam00fkr78qhrdnde";

//    public static final double _dSWFeeUSD = 0.5;

    private SendNotifTxFactory () { ; }

}
