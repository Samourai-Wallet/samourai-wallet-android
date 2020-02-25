package com.samourai.xmanager.client;

import android.content.Context;
import android.test.mock.MockContext;

import com.samourai.http.client.AndroidHttpClient;
import com.samourai.http.client.IHttpClient;
import com.samourai.wallet.tor.TorManager;
import com.samourai.wallet.util.WebUtil;
import com.samourai.xmanager.protocol.XManagerEnv;
import com.samourai.xmanager.protocol.XManagerService;
import com.samourai.xmanager.protocol.rest.AddressIndexResponse;

public class XManagerExample {

  protected Context context = new MockContext();

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