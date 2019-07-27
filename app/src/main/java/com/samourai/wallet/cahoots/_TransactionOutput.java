package com.samourai.wallet.cahoots;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;

import java.nio.ByteBuffer;

public class _TransactionOutput extends TransactionOutput {

    public _TransactionOutput(NetworkParameters params, Transaction parent, Coin value, byte[] scriptBytes) {
        super(params, parent, value, scriptBytes);
    }

    @Override
    public int hashCode() {

        final int prime = 31;
        byte[] val = new byte[4];
        System.arraycopy(scriptBytes, scriptBytes.length - 4, val, 0, val.length);
        ByteBuffer bb = ByteBuffer.wrap(val);
        int result = prime * bb.getInt();

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
