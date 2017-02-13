package com.samourai.wallet.JSONRPC;

import com.samourai.wallet.util.CharSequenceX;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

//import org.json.JSONObject;
import org.json.JSONTokener;

import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class JSONRPC {

    private static final String COMMAND_GET_BALANCE = "getbalance";
    private static final String COMMAND_GET_INFO = "getinfo";
    private static final String COMMAND_GET_BLOCKCOUNT = "getblockcount";

    private String user = null;
    private CharSequenceX password = null;
    private String node = null;
    private int port = 8332;

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

    private org.json.simple.JSONObject invokeRPC(String id, String method, List<String> params) {

        DefaultHttpClient httpclient = new DefaultHttpClient();

        org.json.simple.JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("method", method);
        if (null != params) {
            JSONArray array = new JSONArray();
            array.addAll(params);
            json.put("params", params);
        }
        JSONObject responseJsonObj = null;
        try {
            httpclient.getCredentialsProvider().setCredentials(new AuthScope(node, port), new UsernamePasswordCredentials(user, password.toString()));
            StringEntity myEntity = new StringEntity(json.toJSONString());
            System.out.println(json.toString());
            HttpPost httppost = new HttpPost("http://" + node + ":" + port);
            httppost.setEntity(myEntity);

            System.out.println("executing request" + httppost.getRequestLine());
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();

            System.out.println(response.getStatusLine());
            if (entity != null) {
                System.out.println("Response content length: " + entity.getContentLength());
            }
            JSONParser parser = new JSONParser();
            responseJsonObj = (JSONObject)parser.parse(EntityUtils.toString(entity));
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
        catch (org.json.simple.parser.ParseException e) {
            e.printStackTrace();
        }
        finally {
            httpclient.getConnectionManager().shutdown();
        }

        return responseJsonObj;
    }

    public Double getBalance(String account) {
        String[] params = { account };
        JSONObject json = invokeRPC(UUID.randomUUID().toString(), COMMAND_GET_BALANCE, Arrays.asList(params));
        return (Double)json.get("result");
    }

    public JSONObject getInfo() {
        JSONObject json = invokeRPC(UUID.randomUUID().toString(), COMMAND_GET_INFO, null);
        return (JSONObject)json.get("result");
    }

    public String getInfoAsString() {
        JSONObject json = invokeRPC(UUID.randomUUID().toString(), COMMAND_GET_INFO, null);
        return ((JSONObject)json.get("result")).toString();
    }

    public String getBlockCountAsString() {
        JSONObject json = invokeRPC(UUID.randomUUID().toString(), COMMAND_GET_BLOCKCOUNT, null);
        return ((JSONObject)json.get("result")).toString();
    }

}
