package com.invertedx.torservice;

import android.text.TextUtils;
import android.util.Log;

import com.invertedx.torservice.util.Prefs;

import net.freehaven.tor.control.EventHandler;

import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * Created by n8fr8 on 9/25/16.
 */
public class TorEventHandler implements EventHandler, TorServiceConstants {

    private TorProxyManager torProxyManager;


    private long lastRead = -1;
    private long lastWritten = -1;
    private long mTotalTrafficWritten = 0;
    private long mTotalTrafficRead = 0;

    private NumberFormat mNumberFormat = null;


    private HashMap<String, Node> hmBuiltNodes = new HashMap<String, Node>();

    public class Node {
        public String status;
        public String id;
        public String name;
        public String ipAddress;
        public String country;
        public String organization;

        public boolean isFetchingInfo = false;
    }

    public HashMap<String, Node> getNodes() {
        return hmBuiltNodes;
    }

    public TorEventHandler(TorProxyManager receiver) {
        torProxyManager = receiver;
        mNumberFormat = NumberFormat.getInstance(Locale.getDefault()); //localized numbers!

    }

    @Override
    public void newDescriptors(List<String> orList) {
        Iterator<String> iterator = orList.iterator();
        StringBuilder stringBuilder = new StringBuilder();
        while (iterator.hasNext()) {
            stringBuilder.append(iterator.next());
        }
    }


    @Override
    public void orConnStatus(String status, String orName) {

        StringBuilder sb = new StringBuilder();
        sb.append("orConnStatus (");
        sb.append(parseNodeName(orName));
        sb.append("): ");
        sb.append(status);

        torProxyManager.debug(sb.toString());
    }

    @Override
    public void streamStatus(String status, String streamID, String target) {

        StringBuilder sb = new StringBuilder();
        sb.append("StreamStatus (");
        sb.append((streamID));
        sb.append("): ");
        sb.append(status);

        torProxyManager.setCircuitStatus(sb.toString());
    }


    @Override
    public void message(String severity, String msg) {

        torProxyManager.setCircuitStatus(severity + ": " + msg);
    }

    @Override
    public void unrecognized(String type, String msg) {

        torProxyManager.setCircuitStatus(msg);
        torProxyManager.logNotice(msg);
    }

    @Override
    public void bandwidthUsed(long read, long written) {

        if (read != lastRead || written != lastWritten) {
            StringBuilder sb = new StringBuilder();
            sb.append(formatCount(read));
            sb.append(" \u2193");
            sb.append(" / ");
            sb.append(formatCount(written));
            sb.append(" \u2191");

            torProxyManager.bandWidthUpdate(sb.toString(), read, written);

            mTotalTrafficWritten += written;
            mTotalTrafficRead += read;
        }

        lastWritten = written;
        lastRead = read;
        torProxyManager.bandwidthUsed(read, written);
    }

    private String formatCount(long count) {
        // Converts the supplied argument into a string.

        // Under 2Mb, returns "xxx.xKb"
        // Over 2Mb, returns "xxx.xxMb"
        if (mNumberFormat != null)
            if (count < 1e6)
                return mNumberFormat.format(Math.round((float) ((int) (count * 10 / 1024)) / 10)) + "kbps";
            else
                return mNumberFormat.format(Math.round((float) ((int) (count * 100 / 1024 / 1024)) / 100)) + "mbps";
        else
            return "";

        //return count+" kB";
    }

    public void circuitStatus(String status, String circID, String path) {

        /* once the first circuit is complete, then announce that Orbot is on*/
        if (torProxyManager.getCurrentStatus() == TorProxyManager.ConnectionStatus.CONNECTING && TextUtils.equals(status, "BUILT")) {


            torProxyManager.setStatus(TorProxyManager.ConnectionStatus.CONNECTED);
            torProxyManager.sendLogs("Connected to the Tor network");
        }


        String msg = "CircuitStatus: " + circID + " " + status;
        //String purpose = info.get("PURPOSE");
        //if(purpose != null) msg += ", purpose: " + purpose;
        //String hsState = info.get("HS_STATE");
        //if(hsState != null) msg += ", state: " + hsState;
        //String rendQuery = info.get("REND_QUERY");
        //if(rendQuery != null) msg += ", service: " + rendQuery;
        if (!path.isEmpty()) msg += ", path: " + shortenPath(path);
        torProxyManager.setCircuitStatus(msg);
//        LOG.info(msg);


        if (Prefs.viewCircuitStatus()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Circuit (");
            sb.append((circID));
            sb.append(") ");
            sb.append(status);
            sb.append(": ");

            StringTokenizer st = new StringTokenizer(path, ",");
            Node node = null;

            boolean isFirstNode = true;
            int nodeCount = st.countTokens();

            while (st.hasMoreTokens()) {
                String nodePath = st.nextToken();
                String nodeId = null, nodeName = null;

                String[] nodeParts;

                if (nodePath.contains("="))
                    nodeParts = nodePath.split("=");
                else
                    nodeParts = nodePath.split("~");

                if (nodeParts.length == 1) {
                    nodeId = nodeParts[0].substring(1);
                    if (node != null) {
                        nodeName = node.id;
                    }
                } else if (nodeParts.length == 2) {
                    nodeId = nodeParts[0].substring(1);
                    nodeName = nodeParts[1];
                }

                if (nodeId == null)
                    continue;

                node = hmBuiltNodes.get(nodeId);

                if (node == null) {
                    node = new Node();
                    node.id = nodeId;
                    node.name = nodeName;
                }

                node.status = status;

                sb.append(node.name);

                if (!TextUtils.isEmpty(node.ipAddress))
                    sb.append("(").append(node.ipAddress).append(")");

                if (st.hasMoreTokens())
                    sb.append(" > ");
//                torProxyManager.setCircuitStatus(status);

                if (status.equals("EXTENDED")) {

                    if (isFirstNode) {
                        hmBuiltNodes.put(node.id, node);

                        if (node.ipAddress == null && (!node.isFetchingInfo) && Prefs.useDebugLogging()) {
                            node.isFetchingInfo = true;
//                            torProxyManager.exec(new ExternalIPFetcher(torProxyManager, node, OrbotService.mPortHTTP));
                        }

                        isFirstNode = false;
                    }
                } else if (status.equals("BUILT")) {
                       torProxyManager.logNotice(sb.toString());
//                    torProxyManager.setCircuitStatus(hmBuiltNodes.toString());
                } else if (status.equals("CLOSED")) {
                      torProxyManager.logNotice(sb.toString());
                    hmBuiltNodes.remove(node.id);
                }

            }


        }
    }


    private String parseNodeName(String node) {
        if (node.indexOf('=') != -1) {
            return (node.substring(node.indexOf("=") + 1));
        } else if (node.indexOf('~') != -1) {
            return (node.substring(node.indexOf("~") + 1));
        } else
            return node;
    }

    @NotNull
    private String shortenPath(String id) {
        StringBuilder s = new StringBuilder();
        if (s.length() > 0) s.append(',');
        s.append(id.substring(1, 7));
        return s.toString();
    }
}
