package com.samourai.wallet.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BatchSendUtil {

    public class BatchSend   {
        public String pcode = null;
        public String addr = null;
        public long amount = 0L;
    }

    private static BatchSendUtil instance = null;

    private static List<BatchSend> batchSends = null;

    private BatchSendUtil() { ; }

    public static BatchSendUtil getInstance() {

        if(instance == null) {
            batchSends = new ArrayList<BatchSend>();
            instance = new BatchSendUtil();
        }

        return instance;
    }

    public void add(BatchSend send) {
        batchSends.add(send);
    }

    public void clear() {
        batchSends.clear();
    }

    public List<BatchSend> getSends() {
        return batchSends;
    }

    public BatchSend getBatchSend() {
        return new BatchSend();
    }

    public JSONArray toJSON() {

        JSONArray batch = new JSONArray();
        try {
            for(BatchSend send : batchSends) {
                JSONObject obj = new JSONObject();
                if(send.pcode != null)    {
                    obj.put("pcode", send.pcode);
                }
                obj.put("addr", send.addr);
                obj.put("amount", send.amount);
                batch.put(obj);
            }
        }
        catch(JSONException je) {
            ;
        }

        return batch;
    }

    public void fromJSON(JSONArray batch) {

        batchSends.clear();

        try {
            for(int i = 0; i < batch.length(); i++) {
                JSONObject send = batch.getJSONObject(i);
                BatchSend batchSend = new BatchSend();
                if(send.has("pcode"))    {
                    batchSend.pcode = send.getString("pcode");
                }
                batchSend.addr = send.getString("addr");
                batchSend.amount = send.getLong("amount");
                batchSends.add(batchSend);
            }
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

}
