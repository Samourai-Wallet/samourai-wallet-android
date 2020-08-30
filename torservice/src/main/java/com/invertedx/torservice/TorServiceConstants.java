/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package com.invertedx.torservice;

import android.content.Intent;

public interface TorServiceConstants {


    String DIRECTORY_TOR_DATA = "data";

    String TOR_CONTROL_PORT_FILE = "control.txt";
    String TOR_PID_FILE = "torpid";

    //torrc (tor config file)
    String TORRC_ASSET_KEY = "torrc";

    String TOR_CONTROL_COOKIE = "control_auth_cookie";

    //geoip data file asset key
    String GEOIP_ASSET_KEY = "geoip";
    String GEOIP6_ASSET_KEY = "geoip6";

    String IP_LOCALHOST = "127.0.0.1";
    int TOR_TRANSPROXY_PORT_DEFAULT = 9040;

//    int TOR_DNS_PORT_DEFAULT = 5400;

//    String HTTP_PROXY_PORT_DEFAULT = "8118"; // like Privoxy!
    String SOCKS_PROXY_PORT_DEFAULT = "auto";


    String PREF_BINARY_TOR_VERSION_INSTALLED = "BINARY_TOR_VERSION_INSTALLED";

    //obfsproxy
    String OBFSCLIENT_ASSET_KEY = "obfs4proxy";

    String HIDDEN_SERVICES_DIR = "hidden_services";


}
