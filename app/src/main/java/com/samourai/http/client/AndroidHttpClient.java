package com.samourai.http.client;

import com.samourai.wallet.util.WebUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * HTTP client used by Whirlpool.
 */
public class AndroidHttpClient extends JacksonHttpClient {
    private WebUtil webUtil;

    public AndroidHttpClient(WebUtil webUtil) {
        this.webUtil = webUtil;
    }

    @Override
    protected String requestJsonGet(String url, Map<String, String> headers) throws Exception {
        return webUtil.getURL(url, headers);
    }

    @Override
    protected String requestJsonPostOverTor(String url, Map<String, String> headers, String jsonBody) throws Exception {
        return webUtil.postURL(WebUtil.CONTENT_TYPE_APPLICATION_JSON, url, jsonBody, headers);
    }

    @Override
    protected String requestJsonPostUrlEncoded(String url, Map<String, String> headers, Map<String, String> body) throws Exception {
        String jsonString = queryString(body);
        return webUtil.postURL(null, url, jsonString, headers);
    }

    public String queryString(final Map<String,String> parameters) throws UnsupportedEncodingException {
        String url = "";
        for (Map.Entry<String,String> parameter : parameters.entrySet()) {
            final String encodedKey = URLEncoder.encode(parameter.getKey(), "UTF-8");
            final String encodedValue = URLEncoder.encode(parameter.getValue(), "UTF-8");
            url += encodedKey + "=" + encodedValue+"&";
        }
        return url;
    }
}
