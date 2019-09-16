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

    private static HashMap<String,UTXO> p2pkh_clean = null;
    private static HashMap<String,UTXO> p2sh_p2wpkh_clean = null;
    private static HashMap<String,UTXO> p2wpkh_clean = null;
    private static HashMap<String,UTXO> postMix_clean = null;
    private static HashMap<String,UTXO> p2pkh_toxic = null;
    private static HashMap<String,UTXO> p2sh_p2wpkh_toxic = null;
    private static HashMap<String,UTXO> p2wpkh_toxic = null;
    private static HashMap<String,UTXO> postMix_toxic = null;
    private static HashMap<String,UTXO> preMix = null;

    private UTXOFactory() { ; }

    public static UTXOFactory getInstance() {

        if(instance == null) {
            instance = new UTXOFactory();

            p2pkh_clean = new HashMap<String,UTXO>();
            p2sh_p2wpkh_clean = new HashMap<String,UTXO>();
            p2wpkh_clean = new HashMap<String,UTXO>();
            postMix_clean = new HashMap<String,UTXO>();
            p2pkh_toxic = new HashMap<String,UTXO>();
            p2sh_p2wpkh_toxic = new HashMap<String,UTXO>();
            p2wpkh_toxic = new HashMap<String,UTXO>();
            postMix_toxic = new HashMap<String,UTXO>();
            preMix = new HashMap<String,UTXO>();
        }

        return instance;
    }

    public static UTXOFactory getInstance(Context ctx) {

        context = ctx;

        if(instance == null) {
            instance = new UTXOFactory();

            p2pkh_clean = new HashMap<String,UTXO>();
            p2sh_p2wpkh_clean = new HashMap<String,UTXO>();
            p2wpkh_clean = new HashMap<String,UTXO>();
            postMix_clean = new HashMap<String,UTXO>();
            p2pkh_toxic = new HashMap<String,UTXO>();
            p2sh_p2wpkh_toxic = new HashMap<String,UTXO>();
            p2wpkh_toxic = new HashMap<String,UTXO>();
            postMix_toxic = new HashMap<String,UTXO>();
            preMix = new HashMap<String,UTXO>();
        }

        return instance;
    }

    public void clear() {
        p2pkh_clean.clear();
        p2sh_p2wpkh_clean.clear();
        p2wpkh_clean.clear();
        postMix_clean.clear();
        p2pkh_toxic.clear();
        p2sh_p2wpkh_toxic.clear();
        p2wpkh_toxic.clear();
        postMix_toxic.clear();
        preMix.clear();
    }

    public HashMap<String,UTXO> getP2PKHClean() {
        return p2pkh_clean;
    }

    public HashMap<String,UTXO> getP2SH_P2WPKHClean() {
        return p2sh_p2wpkh_clean;
    }

    public HashMap<String,UTXO> getP2WPKHClean() {
        return p2wpkh_clean;
    }

    public HashMap<String,UTXO> getPostMixClean() {
        return postMix_clean;
    }

    public HashMap<String,UTXO> getP2PKHToxic() {
        return p2pkh_toxic;
    }

    public HashMap<String,UTXO> getP2SH_P2WPKHToxic() {
        return p2sh_p2wpkh_toxic;
    }

    public HashMap<String,UTXO> getP2WPKHToxic() {
        return p2wpkh_toxic;
    }

    public HashMap<String,UTXO> getPostMixToxic() {
        return postMix_toxic;
    }

    public HashMap<String,UTXO> getPreMix() {
        return preMix;
    }

    public HashMap<String,UTXO> getAllP2PKH() {
        HashMap<String,UTXO> ret = new HashMap<String,UTXO>();
        ret.putAll(p2pkh_clean);
        ret.putAll(p2pkh_toxic);
        return ret;
    }

    public HashMap<String,UTXO> getAllP2SH_P2WPKH() {
        HashMap<String,UTXO> ret = new HashMap<String,UTXO>();
        ret.putAll(p2sh_p2wpkh_clean);
        ret.putAll(p2sh_p2wpkh_toxic);
        return ret;
    }

    public HashMap<String,UTXO> getAllP2WPKH() {
        HashMap<String,UTXO> ret = new HashMap<String,UTXO>();
        ret.putAll(p2wpkh_clean);
        ret.putAll(p2wpkh_toxic);
        return ret;
    }

    public HashMap<String,UTXO> getAllPostMix() {
        HashMap<String,UTXO> ret = new HashMap<String,UTXO>();
        ret.putAll(postMix_clean);
        ret.putAll(postMix_toxic);
        return ret;
    }

    public void addP2PKH(String hash, int id, String script, UTXO utxo)  {
        if(!BlockedUTXO.getInstance().contains(hash, id))    {
            if(isToxic(utxo))    {
                p2pkh_toxic.put(script, utxo);
            }
            else    {
                p2pkh_clean.put(script, utxo);
            }
        }
    }

    public void addP2SH_P2WPKH(String hash, int id, String script, UTXO utxo)  {
        if(!BlockedUTXO.getInstance().contains(hash, id))    {
            if(isToxic(utxo))    {
                p2sh_p2wpkh_toxic.put(script, utxo);
            }
            else    {
                p2sh_p2wpkh_clean.put(script, utxo);
            }
        }
    }

    public void addP2WPKH(String hash, int id, String script, UTXO utxo)  {
        if(!BlockedUTXO.getInstance().contains(hash, id))    {
            if(isToxic(utxo))    {
                p2wpkh_toxic.put(script, utxo);
            }
            else    {
                p2wpkh_clean.put(script, utxo);
            }
        }
    }

    public void addPostMix(String hash, int id, String script, UTXO utxo)  {
        if(!BlockedUTXO.getInstance().containsPostMix(hash, id))    {
            if(isToxic(utxo))    {
                postMix_toxic.put(script, utxo);
            }
            else    {
                postMix_clean.put(script, utxo);
            }
        }
    }

    public void addPreMix(String hash, int id, String script, UTXO utxo)  {
        preMix.put(script, utxo);
    }

    public long getTotalP2PKHClean() {
        HashMap<String,UTXO> utxos = getP2PKHClean();
        long ret = 0L;

        for(UTXO utxo : utxos.values())   {
            ret += utxo.getValue();
        }

        return ret;
    }

    public long getTotalP2SH_P2WPKHClean() {
        HashMap<String,UTXO> utxos = getP2SH_P2WPKHClean();
        long ret = 0L;

        for(UTXO utxo : utxos.values())   {
            ret += utxo.getValue();
        }

        return ret;
    }

    public long getTotalP2WPKHClean() {
        HashMap<String,UTXO> utxos = getP2WPKHClean();
        long ret = 0L;

        for(UTXO utxo : utxos.values())   {
            ret += utxo.getValue();
        }

        return ret;
    }

    public long getTotalPostMixClean() {
        HashMap<String,UTXO> utxos = getPostMixClean();
        long ret = 0L;

        for(UTXO utxo : utxos.values())   {
            ret += utxo.getValue();
        }

        return ret;
    }

    public int getCountP2PKHClean() {
        HashMap<String,UTXO> utxos = getP2PKHClean();
        int ret = 0;

        for(UTXO utxo : utxos.values())   {
            ret += utxo.getOutpoints().size();
        }

        return ret;
    }

    public int getCountP2SH_P2WPKHClean() {
        HashMap<String,UTXO> utxos = getP2SH_P2WPKHClean();
        int ret = 0;

        for(UTXO utxo : utxos.values())   {
            ret += utxo.getOutpoints().size();
        }

        return ret;
    }

    public int getCountP2WPKHClean() {
        HashMap<String,UTXO> utxos = getP2WPKHClean();
        int ret = 0;

        for(UTXO utxo : utxos.values())   {
            ret += utxo.getOutpoints().size();
        }

        return ret;
    }

    public int getCountPostMixClean() {
        HashMap<String,UTXO> utxos = getPostMixClean();
        int ret = 0;

        for(UTXO utxo : utxos.values())   {
            ret += utxo.getOutpoints().size();
        }

        return ret;
    }

    public long getTotalP2PKHToxic() {
        HashMap<String,UTXO> utxos = getP2PKHToxic();
        long ret = 0L;

        for(UTXO utxo : utxos.values())   {
            ret += utxo.getValue();
        }

        return ret;
    }

    public long getTotalP2SH_P2WPKHToxic() {
        HashMap<String,UTXO> utxos = getP2SH_P2WPKHToxic();
        long ret = 0L;

        for(UTXO utxo : utxos.values())   {
            ret += utxo.getValue();
        }

        return ret;
    }

    public long getTotalP2WPKHToxic() {
        HashMap<String,UTXO> utxos = getP2WPKHToxic();
        long ret = 0L;

        for(UTXO utxo : utxos.values())   {
            ret += utxo.getValue();
        }

        return ret;
    }

    public long getTotalPostMixToxic() {
        HashMap<String,UTXO> utxos = getPostMixToxic();
        long ret = 0L;

        for(UTXO utxo : utxos.values())   {
            ret += utxo.getValue();
        }

        return ret;
    }

    public long getTotalPreMix() {
        HashMap<String,UTXO> utxos = getPreMix();
        long ret = 0L;

        for(UTXO utxo : utxos.values())   {
            ret += utxo.getValue();
        }

        return ret;
    }

    public int getCountP2PKHToxic() {
        HashMap<String,UTXO> utxos = getP2PKHToxic();
        int ret = 0;

        for(UTXO utxo : utxos.values())   {
            ret += utxo.getOutpoints().size();
        }

        return ret;
    }

    public int getCountP2SH_P2WPKHToxic() {
        HashMap<String,UTXO> utxos = getP2SH_P2WPKHToxic();
        int ret = 0;

        for(UTXO utxo : utxos.values())   {
            ret += utxo.getOutpoints().size();
        }

        return ret;
    }

    public int getCountP2WPKHToxic() {
        HashMap<String,UTXO> utxos = getP2WPKHToxic();
        int ret = 0;

        for(UTXO utxo : utxos.values())   {
            ret += utxo.getOutpoints().size();
        }

        return ret;
    }

    public int getCountPostMixToxic() {
        HashMap<String,UTXO> utxos = getPostMixToxic();
        int ret = 0;

        for(UTXO utxo : utxos.values())   {
            ret += utxo.getOutpoints().size();
        }

        return ret;
    }

    public int getCountPreMix() {
        HashMap<String,UTXO> utxos = getPreMix();
        int ret = 0;

        for(UTXO utxo : utxos.values())   {
            ret += utxo.getOutpoints().size();
        }

        return ret;
    }

    public long getTotalP2PKH() {
        HashMap<String,UTXO> utxos = getAllP2PKH();
        long ret = 0L;

        for(UTXO utxo : utxos.values())   {
            ret += utxo.getValue();
        }

        return ret;
    }

    public long getTotalP2SH_P2WPKH() {
        HashMap<String,UTXO> utxos = getAllP2SH_P2WPKH();
        long ret = 0L;

        for(UTXO utxo : utxos.values())   {
            ret += utxo.getValue();
        }

        return ret;
    }

    public long getTotalP2WPKH() {
        HashMap<String,UTXO> utxos = getAllP2WPKH();
        long ret = 0L;

        for(UTXO utxo : utxos.values())   {
            ret += utxo.getValue();
        }

        return ret;
    }

    public long getTotalPostMix() {
        HashMap<String,UTXO> utxos = getAllPostMix();
        long ret = 0L;

        for(UTXO utxo : utxos.values())   {
            ret += utxo.getValue();
        }

        return ret;
    }

    public int getCountP2PKH() {
        HashMap<String,UTXO> utxos = getAllP2PKH();
        int ret = 0;

        for(UTXO utxo : utxos.values())   {
            ret += utxo.getOutpoints().size();
        }

        return ret;
    }

    public int getCountP2SH_P2WPKH() {
        HashMap<String,UTXO> utxos = getAllP2SH_P2WPKH();
        int ret = 0;

        for(UTXO utxo : utxos.values())   {
            ret += utxo.getOutpoints().size();
        }

        return ret;
    }

    public int getCountP2WPKH() {
        HashMap<String,UTXO> utxos = getAllP2WPKH();
        int ret = 0;

        for(UTXO utxo : utxos.values())   {
            ret += utxo.getOutpoints().size();
        }

        return ret;
    }

    public int getCountPostMix() {
        HashMap<String,UTXO> utxos = getAllPostMix();
        int ret = 0;

        for(UTXO utxo : utxos.values())   {
            ret += utxo.getOutpoints().size();
        }

        return ret;
    }

    private boolean isToxic(UTXO utxo)   {

        String path = utxo.getPath();

        // bip47 receive
        if(path == null || path.length() == 0) {
            return false;
        }
        // any account receive
        else if(path.startsWith("M/0/"))   {
            return false;
        }
        // assume starts with "M/1/"
        // any account change
        else    {
            return true;
        }

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
