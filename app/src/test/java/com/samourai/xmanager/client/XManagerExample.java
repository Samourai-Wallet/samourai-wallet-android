package com.samourai.xmanager.client;

import android.content.Context;

import com.samourai.http.client.AndroidHttpClient;
import com.samourai.http.client.IHttpClient;
import com.samourai.wallet.tor.TorManager;
import com.samourai.wallet.util.WebUtil;
import com.samourai.xmanager.protocol.XManagerService;

public class XManagerExample {

  protected Context context = null; //new MockContext(); // TODO sdk>=29 required

  public XManagerExample() throws Exception {
    TorManager torManager = TorManager.getInstance(getContext());
    IHttpClient httpClient = new AndroidHttpClient(WebUtil.getInstance(getContext()), torManager);

    // instanciation
    boolean testnet = true;
    boolean onion = false;
    XManagerClient xManagerClient = new XManagerClient(testnet, onion, httpClient);

    // get next address (or default when server unavailable)
    String address = xManagerClient.getAddressOrDefault(XManagerService.RICOCHET);

  }

  private Context getContext() {
    return context;
  }
}
