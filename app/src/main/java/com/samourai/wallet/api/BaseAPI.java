package com.samourai.wallet.api;

import javax.annotation.Nonnull;

public abstract class BaseAPI {

    public final static String PROTOCOL = "https://";
    public final static String API_SUBDOMAIN = "api.";
    public final static String SERVER_ADDRESS = "blockchain.info/";

    private final static String API_CODE = "25a6ad13-1633-4dfb-b6ee-9b91cdf0b5c3";

    @Nonnull
    public String getApiCode() {
//        return PersistentUrls.getInstance().getCurrentEnvironment() == PersistentUrls.Environment.PRODUCTION ? "&api_code=" + API_CODE : "";
        return API_CODE;
    }
}