package com.samourai.wallet.cahoots;

import com.samourai.wallet.send.MyTransactionOutPoint;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.ProtocolException;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.TransactionOutPoint;

import java.math.BigInteger;
import java.nio.ByteBuffer;

public class _TransactionOutPoint extends MyTransactionOutPoint {

    public _TransactionOutPoint(MyTransactionOutPoint outpoint) throws ProtocolException {
        super(outpoint.getTxHash(), outpoint.getTxOutputN(), BigInteger.valueOf(outpoint.getValue().longValue()), outpoint.getScriptBytes(), outpoint.getAddress());
    }

    @Override
    public int hashCode() {

        final int prime = 31;
        byte[] val = new byte[4];
        System.arraycopy(getHash().getBytes(), getHash().getBytes().length - 4, val, 0, val.length);
        ByteBuffer bb = ByteBuffer.wrap(val);
        int result = prime + (bb.getInt() + (int)getIndex() + (int)(getValue().longValue() % Integer.MAX_VALUE));

        return result;
    }

    @Override
    public boolean equals(Object obj) {

        if(this == obj) {
            return true;
        }
        else if(obj == null)    {
            return false;
        }
        else if(getClass() != obj.getClass())   {
            return false;
        }
        else if(!this.equals(obj))  {
            return false;
        }

        return false;
    }

}
