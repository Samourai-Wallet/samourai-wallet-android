//package com.invertedx.torservice.util;
//
//import com.invertedx.torservice.TorEventHandler;
//import com.invertedx.torservice.TorEventsReceiver;
//
//import org.json.JSONArray;
//import org.json.JSONObject;
//
//import java.io.BufferedReader;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.net.InetSocketAddress;
//import java.net.Proxy;
//import java.net.URL;
//import java.net.URLConnection;
//
//public class ExternalIPFetcher implements Runnable {
//
//    private TorEventsReceiver mReceiver;
//    private TorEventHandler.Node mNode;
//    private final static String ONIONOO_BASE_URL = "https://onionoo.torproject.org/details?fields=country_name,as_name,or_addresses&lookup=";
//    private int mLocalHttpProxyPort = 8118;
//
//    public ExternalIPFetcher (TorEventsReceiver receiver, TorEventHandler.Node node, int localProxyPort )
//    {
//        mReceiver = receiver;
//        mNode = node;
//        mLocalHttpProxyPort = localProxyPort;
//    }
//
//    public void run ()
//    {
//        try {
//
//            URLConnection conn = null;
//
//            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", mLocalHttpProxyPort));
//            conn = new URL(ONIONOO_BASE_URL + mNode.id).openConnection(proxy);
//
//            conn.setRequestProperty("Connection","Close");
//            conn.setConnectTimeout(60000);
//            conn.setReadTimeout(60000);
//
//            InputStream is = conn.getInputStream();
//
//            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
//
//            // getting JSON string from URL
//
//            StringBuffer json = new StringBuffer();
//            String line = null;
//
//            while ((line = reader.readLine())!=null)
//                json.append(line);
//
//            JSONObject jsonNodeInfo = new JSONObject(json.toString());
//
//            JSONArray jsonRelays = jsonNodeInfo.getJSONArray("relays");
//
//            if (jsonRelays.length() > 0)
//            {
//                mNode.ipAddress = jsonRelays.getJSONObject(0).getJSONArray("or_addresses").getString(0).split(":")[0];
//                mNode.country = jsonRelays.getJSONObject(0).getString("country_name");
//                mNode.organization = jsonRelays.getJSONObject(0).getString("as_name");
//
//                StringBuffer sbInfo = new StringBuffer();
//                sbInfo.append(mNode.name).append("(");
//                sbInfo.append(mNode.ipAddress).append(")");
//
//                if (mNode.country != null)
//                    sbInfo.append(' ').append(mNode.co untry);
//
//                if (mNode.organization != null)
//                    sbInfo.append(" (").append(mNode.organization).append(')');
//
//                mReceiver.debug(sbInfo.toString());
//
//            }
//
//            reader.close();
//            is.close();
//
//
//
//        } catch (Exception e) {
//
//        //    mService.debug ("Error getting node details from onionoo: " + e.getMessage());
//
//
//        }
//
//
//    }
//
//
//}
