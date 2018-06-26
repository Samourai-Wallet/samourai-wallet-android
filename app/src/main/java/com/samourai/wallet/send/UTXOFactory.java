package com.samourai.wallet.send;

import android.content.Context;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bouncycastle.util.encoders.Hex;

import java.util.HashMap;

public class UTXOFactory {

    private static Context context = null;

    private static UTXOFactory instance = null;

    private static HashMap<String,UTXO> p2pkh = null;
    private static HashMap<String,UTXO> p2sh_p2wpkh = null;
    private static HashMap<String,UTXO> p2wpkh = null;

    private UTXOFactory() { ; }

    public static UTXOFactory getInstance() {

        if(instance == null) {
            instance = new UTXOFactory();

            p2pkh = new HashMap<String,UTXO>();
            p2sh_p2wpkh = new HashMap<String,UTXO>();
            p2wpkh = new HashMap<String,UTXO>();
        }

        return instance;
    }

    public static UTXOFactory getInstance(Context ctx) {

        context = ctx;

        if(instance == null) {
            instance = new UTXOFactory();

            p2pkh = new HashMap<String,UTXO>();
            p2sh_p2wpkh = new HashMap<String,UTXO>();
            p2wpkh = new HashMap<String,UTXO>();
        }

        return instance;
    }

    public void clear() {
        p2pkh.clear();
        p2sh_p2wpkh.clear();
        p2wpkh.clear();
    }

    public HashMap<String,UTXO> getP2PKH() {
        return p2pkh;
    }

    public void setP2PKH(HashMap<String,UTXO> p2pkh) {
        UTXOFactory.p2pkh = p2pkh;
    }

    public HashMap<String,UTXO> getP2SH_P2WPKH() {
        return p2sh_p2wpkh;
    }

    public void setP2SH_P2WPKH(HashMap<String,UTXO> p2sh_p2wpkh) {
        UTXOFactory.p2sh_p2wpkh = p2sh_p2wpkh;
    }

    public HashMap<String,UTXO> getP2WPKH() {
        return p2wpkh;
    }

    public void setP2WPKH(HashMap<String,UTXO> p2wpkh) {
        UTXOFactory.p2wpkh = p2wpkh;
    }

    public void addP2PKH(String script, UTXO utxo)  {
        p2pkh.put(script, utxo);
    }

    public void addP2SH_P2WPKH(String script, UTXO utxo)  {
        p2sh_p2wpkh.put(script, utxo);
    }

    public void addP2WPKH(String script, UTXO utxo)  {
        p2wpkh.put(script, utxo);
    }

    public long getTotalP2PKH() {

        long ret = 0L;

        for(UTXO utxo : p2pkh.values())   {
            ret += utxo.getValue();
        }

        return ret;
    }

    public long getTotalP2SH_P2WPKH() {

        long ret = 0L;

        for(UTXO utxo : p2sh_p2wpkh.values())   {
            ret += utxo.getValue();
        }

        return ret;
    }

    public long getTotalP2WPKH() {

        long ret = 0L;

        for(UTXO utxo : p2wpkh.values())   {
            ret += utxo.getValue();
        }

        return ret;
    }

    public int getCountP2PKH() {

        int ret = 0;

        for(UTXO utxo : p2pkh.values())   {
            ret += utxo.getOutpoints().size();
        }

        return ret;
    }

    public int getCountP2SH_P2WPKH() {

        int ret = 0;

        for(UTXO utxo : p2sh_p2wpkh.values())   {
            ret += utxo.getOutpoints().size();
        }

        return ret;
    }

    public int getCountP2WPKH() {

        int ret = 0;

        for(UTXO utxo : p2wpkh.values())   {
            ret += utxo.getOutpoints().size();
        }

        return ret;
    }

    public void markUTXOAsUnspendable(String hexTx)    {

        HashMap<String, Long> utxos = new HashMap<String,Long>();

        for(UTXO utxo : APIFactory.getInstance(context).getUtxos(true))   {
            for(MyTransactionOutPoint outpoint : utxo.getOutpoints())   {
                utxos.put(outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN(), outpoint.getValue().longValue());
            }
        }

        Transaction tx = new Transaction(SamouraiWallet.getInstance().getCurrentNetworkParams(), Hex.decode(hexTx));
        for(TransactionInput input : tx.getInputs())   {
            BlockedUTXO.getInstance().add(input.getOutpoint().getHash().toString(), (int)input.getOutpoint().getIndex(), utxos.get(input.getOutpoint().getHash().toString() + "-" + (int)input.getOutpoint().getIndex()));
        }

    }

}
