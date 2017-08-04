package com.samourai.wallet.JSONRPC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.samourai.wallet.util.CharSequenceX;

import android.util.Log;

public class JSONRPC {

    private static final String COMMAND_GET_BALANCE = "getbalance";
    private static final String COMMAND_GET_INFO = "getinfo";
    private static final String COMMAND_GET_NETWORK_INFO = "getnetworkinfo";
    private static final String COMMAND_GET_BLOCKCOUNT = "getblockcount";
    private static final String COMMAND_PUSHTX = "sendrawtransaction";
    private static final String COMMAND_GET_BLOCK = "getblock";
    private static final String COMMAND_GET_BLOCKHEADER = "getblockheader";
    private static final String COMMAND_GET_FEE_ESTIMATE = "estimatefee";

    private String user = null;
    private CharSequenceX password = null;
    private String node = null;
    private int port = TrustedNodeUtil.DEFAULT_PORT;

    public JSONRPC()    {
        ;
    }

    public JSONRPC(String user, CharSequenceX password, String node, int port) {
        this.user = user;
        this.password = password;
        this.node = node;
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public CharSequenceX getPassword() {
        return password;
    }

    public void setPassword(CharSequenceX password) {
        this.password = password;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Double getBalance(String account) {
        String[] params = { account };
        JSONObject json = doRPC(UUID.randomUUID().toString(), COMMAND_GET_BALANCE, null);
        if(json == null)    {
            return null;
        }
        try {
             return json.getDouble("result");
        }
        catch(JSONException je) {
            return null;
        }
    }

    public JSONObject getInfo() {
        JSONObject json = doRPC(UUID.randomUUID().toString(), COMMAND_GET_INFO, null);
        if(json == null)    {
            return null;
        }
        try {
            return json.getJSONObject("result");
        }
        catch(JSONException je) {
            return null;
        }
    }

    public String getInfoAsString() {
        JSONObject json = getInfo();
        if(json == null)    {
            return null;
        }
        else    {
            try {
                return json.getString("result").toString();
            }
            catch(JSONException je) {
                return null;
            }
        }
    }

    public JSONObject getBlock(String hash) {
        JSONArray array = new JSONArray();
        array.put(hash);
        array.put(true);
        JSONObject json = doRPC(UUID.randomUUID().toString(), COMMAND_GET_BLOCK, array);
        if(json == null)    {
            return null;
        }
        try {
            return json.getJSONObject("result");
        }
        catch(JSONException je) {
            return null;
        }
    }

    public String getBlockAsString(String hash) {
        JSONObject json = getBlock(hash);
        if(json == null)    {
            return null;
        }
        else    {
            try {
                return json.getString("result").toString();
            }
            catch(JSONException je) {
                return null;
            }
        }
    }

    public JSONObject getFeeEstimate(int nbBlocks) {
        JSONArray array = new JSONArray();
        array.put(nbBlocks);
        JSONObject json = doRPC(UUID.randomUUID().toString(), COMMAND_GET_FEE_ESTIMATE, array);
        if(json == null)    {
            return null;
        }
        try {
            JSONObject retObj = new JSONObject();
            retObj.put("result", json.getDouble("result"));
            return retObj;
        }
        catch(JSONException je) {
            return null;
        }
    }

    public JSONObject getBlockHeader(String hash) {
        JSONArray array = new JSONArray();
        array.put(hash);
        array.put(true);
        JSONObject json = doRPC(UUID.randomUUID().toString(), COMMAND_GET_BLOCKHEADER, array);
        if(json == null)    {
            return null;
        }
        try {
            return json.getJSONObject("result");
        }
        catch(JSONException je) {
            return null;
        }
    }

    public String getBlockHeaderAsString(String hash) {
        JSONObject json = getBlockHeader(hash);
        if(json == null)    {
            return null;
        }
        else    {
            try {
                return json.getString("result").toString();
            }
            catch(JSONException je) {
                return null;
            }
        }
    }

    public JSONObject getNetworkInfo() {
        JSONObject json = doRPC(UUID.randomUUID().toString(), COMMAND_GET_NETWORK_INFO, null);
        if(json == null)    {
            return null;
        }
        try {
            return json.getJSONObject("result");
        }
        catch(JSONException je) {
            return null;
        }
    }

    public String getNetworkInfoAsString() {
        JSONObject json = doRPC(UUID.randomUUID().toString(), COMMAND_GET_NETWORK_INFO, null);
        if(json == null)    {
            return null;
        }
        try {
            return json.getString("result").toString();
        }
        catch(JSONException je) {
            return null;
        }
    }

    public long getBlockCountAsLong() {
        JSONObject json = doRPC(UUID.randomUUID().toString(), COMMAND_GET_BLOCKCOUNT, null);
        if(json == null)    {
            return -1L;
        }
        try {
            return json.getLong("result");
        }
        catch(JSONException je) {
            return -1L;
        }
    }

    public JSONObject pushTx(String hexTx) {
        JSONArray array = new JSONArray();
        array.put(hexTx);
        JSONObject json = doRPC(UUID.randomUUID().toString(), COMMAND_PUSHTX, array);
        Log.d("JSONRPC", "response:" + json.toString());
        return json;
    }

    private JSONObject doRPC(String id, String method, JSONArray params) {

        DefaultHttpClient httpclient = new DefaultHttpClient();

        JSONObject responseJsonObj = null;
        try {

            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("method", method);
            if (null != params && params.length() > 0) {
                json.put("params", params);
            }

            httpclient.getCredentialsProvider().setCredentials(new AuthScope(node, port), new UsernamePasswordCredentials(user, password.toString()));
            StringEntity myEntity = new StringEntity(json.toString());
            Log.d("JSONRPC", json.toString());
            HttpPost httppost = new HttpPost("http://" + node + ":" + port);
            httppost.setEntity(myEntity);

            Log.d("JSONRPC", "Request:" + httppost.getRequestLine());
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            Log.d("JSONRPC", response.getStatusLine().toString());
//            Log.d("JSONRPC", entity == null ? "entity is null" : "entity is not null, content length:" + entity.getContentLength());
            if(response.getStatusLine().getStatusCode() != 200)    {
                return null;
            }

            String inputLine = null;
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            try {
                while ((inputLine = br.readLine()) != null) {
                    sb.append(inputLine);
                }
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(sb.length() > 0)    {
                responseJsonObj = new JSONObject(sb.toString());
            }
            else    {
                return null;
            }

        }
        catch (ClientProtocolException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        finally {
            httpclient.getConnectionManager().shutdown();
        }

        return responseJsonObj;
    }

}
