package com.samourai.wallet.bip47;

import android.content.Context;
import android.util.Log;
//import android.util.Log;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.bip47.rpc.PaymentCode;

import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.params.MainNetParams;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BIP47Meta {

    public static final String strSamouraiDonationPCode = "PM8TJVzLGqWR3dtxZYaTWn3xJUop3QP3itR4eYzX7XvV5uAfctEEuHhKNo3zCcqfAbneMhyfKkCthGv5werVbwLruhZyYNTxqbCrZkNNd2pPJA2e2iAh";
    public static final String strSamouraiDonationMeta = "?title=Samourai Donations&desc=Donate to help fund development of Samourai Bitcoin Wallet&user=K6tS2X8";

    public static final int INCOMING_LOOKAHEAD = 3;
    public static final int OUTGOING_LOOKAHEAD = 3;

    public static final int STATUS_NOT_SENT = -1;
    public static final int STATUS_SENT_NO_CFM = 0;
    public static final int STATUS_SENT_CFM = 1;

    private static ConcurrentHashMap<String,String> pcodeLabels = null;
    private static ConcurrentHashMap<String,Boolean> pcodeArchived = null;
    private static ConcurrentHashMap<String,ConcurrentHashMap<String,Integer>> pcodeIncomingIdxs = null;    // lookahead
    private static ConcurrentHashMap<String,String> addr2pcode = null;
    private static ConcurrentHashMap<String,Integer> addr2idx = null;
    private static ConcurrentHashMap<String,Integer> pcodeOutgoingIdxs = null;
    private static ConcurrentHashMap<String,Pair<String,Integer>> pcodeOutgoingStatus = null;
    private static ConcurrentHashMap<String,ArrayList<Integer>> pcodeIncomingUnspent = null;
    private static ConcurrentHashMap<String,String> pcodeIncomingStatus = null;
    private static ConcurrentHashMap<String,String> pcodeLatestEvent = null;
    private static ConcurrentHashMap<String,Boolean> pcodeSegwit = null;

    private static BIP47Meta instance = null;

    private BIP47Meta() { ; }

    public static BIP47Meta getInstance() {

        if(instance == null) {
            pcodeLabels = new ConcurrentHashMap<String,String>();
            pcodeArchived = new ConcurrentHashMap<String,Boolean>();
            pcodeIncomingIdxs = new ConcurrentHashMap<String,ConcurrentHashMap<String,Integer>>();
            addr2pcode = new ConcurrentHashMap<String,String>();
            addr2idx = new ConcurrentHashMap<String,Integer>();
            pcodeOutgoingIdxs = new ConcurrentHashMap<String,Integer>();
            pcodeOutgoingStatus = new ConcurrentHashMap<String,Pair<String,Integer>>();
            pcodeIncomingUnspent = new ConcurrentHashMap<String,ArrayList<Integer>>();
            pcodeIncomingStatus = new ConcurrentHashMap<String,String>();
            pcodeLatestEvent = new ConcurrentHashMap<String,String>();
            pcodeSegwit = new ConcurrentHashMap<String,Boolean>();

            instance = new BIP47Meta();
        }

        return instance;
    }

    public void clear() {
        pcodeLabels.clear();
        pcodeArchived.clear();
        pcodeIncomingIdxs.clear();
        addr2pcode.clear();
        addr2idx.clear();
        pcodeOutgoingIdxs.clear();
        pcodeOutgoingStatus.clear();
        pcodeIncomingUnspent.clear();
        pcodeIncomingStatus.clear();
        pcodeLatestEvent.clear();
        pcodeSegwit.clear();
    }

    public String getLabel(String pcode)   {
        if(!pcodeLabels.containsKey(pcode))    {
            return "";
        }
        else    {
            return pcodeLabels.get(pcode);
        }
    }

    public String getDisplayLabel(String pcode)   {
        String label = getLabel(pcode);
        if(label.length() == 0 || pcode.equals(label))    {
            return getAbbreviatedPcode(pcode);
        }
        else    {
            return label;
        }
    }

    public String getAbbreviatedPcode(String pcode)   {
        return pcode.substring(0, 12) + "..." + pcode.substring(pcode.length() - 5, pcode.length());
    }

    public void setLabel(String pcode, String label)   {
        pcodeLabels.put(pcode, label);
    }

    public Set<String> getLabels()    {
        return pcodeLabels.keySet();
    }

    public Set<String> getSortedByLabels(boolean includeArchived)    {

        ConcurrentHashMap<String, String> labels = null;

        if(includeArchived)    {
            labels = pcodeLabels;
        }
        else    {
            labels = new ConcurrentHashMap<String, String>();
            for(String key : pcodeLabels.keySet())   {
                if(!BIP47Meta.getInstance().getArchived(key))    {
                    labels.put(key, pcodeLabels.get(key));
                }
            }

        }

        Map<String, String> sortedMapAsc = valueSortByComparator(labels, true);
        return sortedMapAsc.keySet();
    }

    private static Map<String, String> valueSortByComparator(ConcurrentHashMap<String, String> unsortMap, final boolean order)  {

        List<Map.Entry<String, String>> list = new LinkedList<Map.Entry<String, String>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Map.Entry<String, String>>() {
            public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {

                if(o1.getValue() == null || o1.getValue().length() == 0)    {
                    o1.setValue(o1.getKey());
                }
                if(o2.getValue() == null || o2.getValue().length() == 0)    {
                    o2.setValue(o2.getKey());
                }

                if(order) {
                    return o1.getValue().compareTo(o2.getValue());
                }
                else {
                    return o2.getValue().compareTo(o1.getValue());

                }
            }
        });

        // Maintaining insertion order with the help of LinkedList
        Map<String, String> sortedMap = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> entry : list)    {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    public boolean getArchived(String pcode)   {
        if(!pcodeArchived.containsKey(pcode))    {
            pcodeArchived.put(pcode, false);
            return false;
        }
        else    {
            return pcodeArchived.get(pcode);
        }
    }

    public void setArchived(String pcode, boolean archived)   {
        pcodeArchived.put(pcode, archived);
    }

    public boolean getSegwit(String pcode)   {
        if(!pcodeSegwit.containsKey(pcode))    {
            pcodeSegwit.put(pcode, false);
            return false;
        }
        else    {
            return pcodeSegwit.get(pcode);
        }
    }

    public void setSegwit(String pcode, boolean segwit)   {
        pcodeSegwit.put(pcode, segwit);
    }

    public void inc(String pcode)   {
        if(!pcodeOutgoingIdxs.containsKey(pcode))    {
            pcodeOutgoingIdxs.put(pcode, 1);
        }
        else    {
            pcodeOutgoingIdxs.put(pcode, pcodeOutgoingIdxs.get(pcode) + 1);
        }
    }

    public int getOutgoingIdx(String pcode)   {
        if(!pcodeOutgoingIdxs.containsKey(pcode))    {
            return 0;
        }
        else    {
            return pcodeOutgoingIdxs.get(pcode);
        }
    }

    public void setOutgoingIdx(String pcode, int idx)   {
        pcodeOutgoingIdxs.put(pcode, idx);
    }

    public String[] getIncomingAddresses(boolean includeArchived)  {

        ArrayList<String> addrs = new ArrayList<String>();
        for(String pcode : pcodeIncomingIdxs.keySet())   {
            if(!includeArchived && pcodeArchived.get(pcode) != null)    {
                continue;
            }
            else    {
                ConcurrentHashMap<String,Integer> map = pcodeIncomingIdxs.get(pcode);
                addrs.addAll(map.keySet());
            }
        }

        String[] s = addrs.toArray(new String[addrs.size()]);

        return s;
    }

    public synchronized String[] getIncomingLookAhead(Context ctx)  {

        Set<String> pcodes = pcodeIncomingIdxs.keySet();
        Iterator<String> it = pcodes.iterator();
        ArrayList<String> addrs = new ArrayList<String>();
        while(it.hasNext())   {
            String pcode = it.next();
            if(getArchived(pcode))    {
                continue;
            }
            int idx = getIncomingIdx(pcode);

//            Log.i("APIFactory", "idx:" + idx + " , " + pcode);

            for(int i = idx; i < (idx + INCOMING_LOOKAHEAD); i++)   {
                try {
                    Log.i("APIFactory", "receive from " + i + ":" + BIP47Util.getInstance(ctx).getReceivePubKey(new PaymentCode(pcode), i));
                    BIP47Meta.getInstance().setIncomingIdx(pcode.toString(), i, BIP47Util.getInstance(ctx).getReceivePubKey(new PaymentCode(pcode), i));
                    BIP47Meta.getInstance().getIdx4AddrLookup().put(BIP47Util.getInstance(ctx).getReceivePubKey(new PaymentCode(pcode), i), i);
                    BIP47Meta.getInstance().getPCode4AddrLookup().put(BIP47Util.getInstance(ctx).getReceivePubKey(new PaymentCode(pcode), i), pcode.toString());
                    addrs.add(BIP47Util.getInstance(ctx).getReceivePubKey(new PaymentCode(pcode), i));
                }
                catch(Exception e) {
                    ;
                }
            }

        }

        String[] s = addrs.toArray(new String[addrs.size()]);

        return s;
    }

    public String getPCode4Addr(String addr)  {
        return addr2pcode.get(addr);
    }

    public Integer getIdx4Addr(String addr)  {
        return addr2idx.get(addr);
    }

    public ConcurrentHashMap<String,String> getPCode4AddrLookup() {
        return addr2pcode;
    }

    public ConcurrentHashMap<String,Integer> getIdx4AddrLookup() {
        return addr2idx;
    }

    public void setIncomingIdx(String pcode, int idx, String addr)   {

        if(!pcodeIncomingIdxs.containsKey(pcode))    {
            ConcurrentHashMap<String, Integer> addrIdx = new ConcurrentHashMap<String, Integer>();
            addrIdx.put(addr, idx);
            pcodeIncomingIdxs.put(pcode, addrIdx);
        }
        else    {
            pcodeIncomingIdxs.get(pcode).put(addr, idx);
        }
    }

    public boolean incomingExists(String pcode) {
        return pcodeIncomingIdxs.containsKey(pcode);
    }

    public int getOutgoingStatus(String pcode)   {
        if(!pcodeOutgoingStatus.containsKey(pcode))    {
            return STATUS_NOT_SENT;
        }
        else    {
            return pcodeOutgoingStatus.get(pcode).getRight();
        }
    }

    public void setOutgoingStatus(String pcode, int status)   {
        String _tx = pcodeOutgoingStatus.get(pcode).getLeft();
        Pair<String,Integer> pair = Pair.of(_tx, status);
        pcodeOutgoingStatus.put(pcode, pair);
    }

    public void setOutgoingStatus(String pcode, String tx, int status)   {
        Pair<String,Integer> pair = Pair.of(tx, status);
        pcodeOutgoingStatus.put(pcode, pair);
    }

    public boolean outgoingExists(String pcode) {
        return pcodeOutgoingIdxs.containsKey(pcode);
    }

    public ArrayList<Pair<String,String>> getOutgoingUnconfirmed()   {

        ArrayList<Pair<String,String>> ret = new ArrayList<Pair<String,String>>();
//        Log.i("BIP47Meta", "key set:" + pcodeOutgoingStatus.keySet().size());
        for(String pcode : pcodeOutgoingStatus.keySet())   {
//            Log.i("BIP47Meta", "pcode:" + pcode.toString());
//            Log.i("BIP47Meta", "tx:" + pcodeOutgoingStatus.get(pcode).getLeft());
//            Log.i("BIP47Meta", "status:" + pcodeOutgoingStatus.get(pcode).getRight());
            if(pcodeOutgoingStatus.get(pcode).getRight() != STATUS_SENT_CFM && pcodeOutgoingStatus.get(pcode).getLeft() != null && pcodeOutgoingStatus.get(pcode).getLeft().length() > 0)    {
                ret.add(Pair.of(pcode, pcodeOutgoingStatus.get(pcode).getLeft()));
            }

        }

        return ret;
    }

    public void addUnspent(String pcode, int idx)    {

        ArrayList<Integer> idxs = pcodeIncomingUnspent.get(pcode);
        if(idxs == null)    {
            idxs = new ArrayList<Integer>();
        }
        if(idxs.contains(idx))    {
            return;
        }

        idxs.add(idx);
        pcodeIncomingUnspent.put(pcode, idxs);
    }

    public void removeUnspent(String pcode, Integer idx)    {

        ArrayList<Integer> idxs = pcodeIncomingUnspent.get(pcode);
        if(idxs == null)    {
            return;
        }
        if(idxs.contains(idx))    {
            idxs.remove(idx);
        }

        pcodeIncomingUnspent.put(pcode, idxs);
    }

    public Set<String> getUnspentProviders()    {
        return pcodeIncomingUnspent.keySet();
    }

    public ArrayList<Integer> getUnspent(String pcode)    {
        return pcodeIncomingUnspent.get(pcode);
    }

    public ArrayList<String> getUnspentAddresses(Context ctx, String pcode)    {

        ArrayList<String> ret = new ArrayList<String>();

        try {
            ArrayList<Integer> idxs = getUnspent(pcode);

            if(idxs != null)    {
                for(int i = 0; i < idxs.size(); i++)   {
                    Log.i("BIP47Meta", "address has unspents:" + BIP47Util.getInstance(ctx).getReceivePubKey(new PaymentCode(pcode), idxs.get(i)));
                    ret.add(BIP47Util.getInstance(ctx).getReceivePubKey(new PaymentCode(pcode), idxs.get(i)));
                }
            }

        }
        catch(Exception e) {
            ;
        }

        return ret;
    }

    public int getIncomingIdx(String pcode)    {

        int ret = -1;

        try {
            ArrayList<Integer> idxs = getUnspent(pcode);

            if(idxs != null)    {
                for(int i = 0; i < idxs.size(); i++)   {
                    if(idxs.get(i) > ret)    {
                        ret = idxs.get(i);
                    }
                }
            }

        }
        catch(Exception e) {
            e.printStackTrace();
        }

        return ret + 1;
    }

    public void clearUnspent(String pcode)    {
        pcodeIncomingUnspent.remove(pcode);
    }

    public void remove(String pcode)    {
        pcodeLabels.remove(pcode);
//        pcodeIncomingIdxs.remove(pcode);
        pcodeOutgoingIdxs.remove(pcode);
        pcodeOutgoingStatus.remove(pcode);
        pcodeIncomingUnspent.remove(pcode);
        pcodeIncomingStatus.remove(pcode);
        pcodeLatestEvent.remove(pcode);
    }

    public String getIncomingStatus(String txHash)    {
        return pcodeIncomingStatus.get(txHash);
    }

    public void setIncomingStatus(String txHash)    {
        pcodeIncomingStatus.put(txHash, "1");
    }

    public String getLatestEvent(String pcode)    {
        return pcodeLatestEvent.get(pcode);
    }

    public void setLatestEvent(String pcode, String event)    {
        pcodeLatestEvent.put(pcode, event);
    }

    public synchronized void pruneIncoming() {

        for(String pcode : getLabels())   {

            ConcurrentHashMap<String,Integer> incomingIdxs = pcodeIncomingIdxs.get(pcode);
            ArrayList<Integer> unspentIdxs = getUnspent(pcode);
            int highestUnspentIdx = getIncomingIdx(pcode);
            boolean changed = false;

//            Log.i("BIP47Meta", "highest idx:" + highestUnspentIdx + "," + pcode);

            if(incomingIdxs != null && incomingIdxs.size() > 0)    {
                for(String addr : incomingIdxs.keySet())   {
                    if(unspentIdxs != null && incomingIdxs != null &&
                            incomingIdxs.get(addr) != null &&
                            !unspentIdxs.contains(incomingIdxs.get(addr)) && (incomingIdxs.get(addr) < (highestUnspentIdx - 5)))    {
//                        Log.i("BIP47Meta", "pruning:" + addr + "," + incomingIdxs.get(addr) + ","  + pcode);
                        incomingIdxs.remove(addr);
                        changed = true;
                    }
                }
            }

            if(changed)    {
                pcodeIncomingIdxs.put(pcode, incomingIdxs);
            }

        }

    }

    public JSONObject toJSON() {

        JSONObject jsonPayload = new JSONObject();
        JSONArray pcodes = null;
        try {
            pcodes = new JSONArray();

            for(String pcode : pcodeLabels.keySet()) {

                JSONObject pobj = new JSONObject();

                pobj.put("payment_code", pcode);
                pobj.put("label", pcodeLabels.get(pcode));
                pobj.put("archived", pcodeArchived.get(pcode));
                pobj.put("segwit", pcodeSegwit.get(pcode));

                if(pcodeIncomingIdxs.containsKey(pcode))    {
                    ConcurrentHashMap<String,Integer> incoming = pcodeIncomingIdxs.get(pcode);
                    JSONArray _incoming = new JSONArray();
                    for(String s : incoming.keySet())   {
                        JSONObject o = new JSONObject();
                        o.put("addr", s);
                        o.put("idx", incoming.get(s));
                        _incoming.put(o);
                    }
                    pobj.put("in_idx", _incoming);
                }

                // addr2pcode not save to JSON
                // addr2idx not save to JSON

                if(pcodeOutgoingIdxs.get(pcode) != null)    {
                    pobj.put("out_idx", pcodeOutgoingIdxs.get(pcode));
                }
                else    {
                    pobj.put("out_idx", 0);
                }

                if(pcodeOutgoingStatus.get(pcode) != null)    {
                    pobj.put("out_status", pcodeOutgoingStatus.get(pcode).getRight());
                    pobj.put("out_tx", pcodeOutgoingStatus.get(pcode).getLeft());
                }
                else    {
                    pobj.put("out_status", BIP47Meta.STATUS_NOT_SENT);
                    pobj.put("out_tx", "");
                }

                ArrayList<Integer> idxs = pcodeIncomingUnspent.get(pcode);
                if(idxs != null)    {
                    JSONArray _idxs = new JSONArray();
                    for(int idx : idxs) {
                        _idxs.put(idx);
                    }
                    pobj.put("in_utxo", _idxs);
                }

                String event = pcodeLatestEvent.get(pcode);
                if(event != null)    {
                    pobj.put("latest_event", event);
                }

                pcodes.put(pobj);
            }
            jsonPayload.put("pcodes", pcodes);

            JSONArray hashes = new JSONArray();
            for(String hash : pcodeIncomingStatus.keySet())   {
                hashes.put(hash);
            }
            jsonPayload.put("incoming_notif_hashes", hashes);

        }
        catch(JSONException je) {
            ;
        }

//        Log.i("BIP47Meta", jsonPayload.toString());

        return jsonPayload;
    }

    public void fromJSON(JSONObject jsonPayload) {

//        Log.i("BIP47Meta", jsonPayload.toString());

        try {

            JSONArray pcodes = new JSONArray();
            if(jsonPayload.has("pcodes"))    {
                pcodes = jsonPayload.getJSONArray("pcodes");
            }

            for(int i = 0; i < pcodes.length(); i++) {

                JSONObject obj = pcodes.getJSONObject(i);

                pcodeLabels.put(obj.getString("payment_code"), obj.getString("label"));
                pcodeArchived.put(obj.getString("payment_code"), obj.has("archived") ? obj.getBoolean("archived") : false);
                pcodeSegwit.put(obj.getString("payment_code"), obj.has("segwit") ? obj.getBoolean("segwit") : false);

                if(obj.has("in_idx"))    {
                    ConcurrentHashMap<String,Integer> incoming = new ConcurrentHashMap<String,Integer>();
                    JSONArray _incoming = obj.getJSONArray("in_idx");
                    for(int j = 0; j < _incoming.length(); j++)   {
                        JSONObject o = _incoming.getJSONObject(j);
                        String addr = o.getString("addr");
                        int idx = o.getInt("idx");
                        incoming.put(addr, idx);
//                        Log.i("BIP47Meta", addr);
//                        Log.i("BIP47Meta", obj.getString("payment_code"));
//                        Log.i("BIP47Meta", "" + idx);
                        addr2pcode.put(addr, obj.getString("payment_code"));
                        addr2idx.put(addr, idx);
                    }
                    pcodeIncomingIdxs.put(obj.getString("payment_code"), incoming);
                }

                if(obj.has("out_idx"))    {
                    pcodeOutgoingIdxs.put(obj.getString("payment_code"), obj.getInt("out_idx"));
                }

                if(obj.has("out_tx") && obj.has("out_status"))    {
                    pcodeOutgoingStatus.put(obj.getString("payment_code"), Pair.of(obj.getString("out_tx"), obj.getInt("out_status")));
                }
                else    {
                    pcodeOutgoingStatus.put(obj.getString("payment_code"), Pair.of("", BIP47Meta.STATUS_NOT_SENT));
                }

                if(obj.has("in_utxo"))    {
                    JSONArray _idxs = obj.getJSONArray("in_utxo");
                    for(int k = 0; k < _idxs.length(); k++)   {
                        addUnspent(obj.getString("payment_code"), _idxs.getInt(k));
                    }
                }

                if(obj.has("latest_event"))    {
                    pcodeLatestEvent.put(obj.getString("payment_code"), obj.getString("latest_event"));
                }

            }

            if(jsonPayload.has("incoming_notif_hashes"))    {
                JSONArray hashes = jsonPayload.getJSONArray("incoming_notif_hashes");
                for(int i = 0; i < hashes.length(); i++) {
                    pcodeIncomingStatus.put(hashes.getString(i), "1");
                }
            }

        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }

    }

}
