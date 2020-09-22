package com.samourai.xmanager.client;

import android.content.Context;

import com.samourai.xmanager.protocol.XManagerService;

public class XManagerExample {

  protected Context context = null; //new MockContext(); // TODO sdk>=29 required

  public XManagerExample() throws Exception {

    // instanciation
    XManagerClient xManagerClient = AndroidXManagerClient.getInstance(getContext());

    // get next address (or default when server unavailable)
    String address = xManagerClient.getAddressOrDefault(XManagerService.RICOCHET);

  }

  private Context getContext() {
    return context;
  }
}
