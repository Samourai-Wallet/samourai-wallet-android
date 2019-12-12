package com.samourai.wallet.vouchers.providers;

import android.content.Context;

import com.samourai.wallet.BuildConfig;
import com.samourai.wallet.tor.TorManager;
import com.samourai.wallet.util.LogUtil;
import com.samourai.wallet.util.VouchersUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class FBTCProvider extends VoucherProvider {

    private static final String TAG = "FBTCProvider";
    private Context mContext;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");


    public FBTCProvider(Context context) {
        this.mContext = context;
    }

    @Override
    public Single<ValidateResponse> validate(String email, String voucher) {
        String url = VouchersUtil.getInstance().getFastBitcoinsAPI().concat("quote");
        return Single.fromCallable(() -> {
            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .connectTimeout(90, TimeUnit.SECONDS)
                    .readTimeout(90, TimeUnit.SECONDS);
            if (TorManager.getInstance(mContext).isConnected()) {
                builder.proxy(TorManager.getInstance(mContext).getProxy());
            }

            if (BuildConfig.DEBUG) {
                builder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));
            }

            JSONObject json = new JSONObject();
            json.put("email_address", email);
            json.put("code", voucher);

            RequestBody body = RequestBody.create(JSON, json.toString());
            Request rb = new Request.Builder().url(url)
                    .post(body)
                    .build();

            Response response = builder.build().newCall(rb).execute();

            if (response.body() != null) {
                String responseString = response.body().string();
                LogUtil.info(TAG, "validate: ".concat(responseString));
                JSONObject object = new JSONObject(responseString);
                if (object.getInt("error") != 0) {
                    throw new Error(object.getString("error_message"));
                } else {
                    ValidateResponse validateResponse = new ValidateResponse();
                    validateResponse.setAmount(object.getLong("satoshi_amount"));
                    LogUtil.info(TAG, "validate: ".concat(String.valueOf(object.getLong("satoshi_amount"))));
                    validateResponse.setQuotationId(object.getInt("quotation_id"));
                    validateResponse.setQuotationSecret(object.getString("quotation_secret"));
                    validateResponse.setVoucher(voucher);
                    validateResponse.setEmail(email);
                    return validateResponse;
                }

            } else {
                throw new JSONException("Invalid response");
            }

        });
    }

    @Override
    public Single<Boolean> redeem(ValidateResponse response,String address) {
        String url = VouchersUtil.getInstance().getFastBitcoinsAPI().concat("redeem");

        return Single.fromCallable(() -> {
            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .connectTimeout(90, TimeUnit.SECONDS)
                    .readTimeout(90, TimeUnit.SECONDS);
            if (TorManager.getInstance(mContext).isConnected()) {
                builder.proxy(TorManager.getInstance(mContext).getProxy());
            }

            if (BuildConfig.DEBUG) {
                builder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));
            }

            JSONObject json = new JSONObject();
            json.put("currency", "voucher");
            json.put("email_address", response.getEmail());
            json.put("currency", "USD");
            json.put("value", 0);
            json.put("code", response.getVoucher());
            json.put("quotation_id", response.getQuotationId());
            json.put("quotation_secret", response.getQuotationSecret());
            json.put("delivery_address", address);

            RequestBody body = RequestBody.create(JSON, json.toString());
            Request rb = new Request.Builder().url(url)
                    .post(body)
                    .build();

            Response requestResponse = builder.build().newCall(rb).execute();

            if (requestResponse.body() != null) {
                String responseString = requestResponse.body().string();
                JSONObject object = new JSONObject(responseString);
                if (object.getInt("error") != 0) {
                    throw new Error(object.getString("error_message"));
                } else {
                    return true;
                }
            } else {
                throw new Error("Invalid response");
            }

        });
    }

    @Override
    public String getProviderName() {
        return "FastBitcoins";
    }
}
