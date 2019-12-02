package com.samourai.wallet;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.samourai.wallet.tor.TorManager;
import com.samourai.wallet.util.AddressFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class VouchersActivity extends AppCompatActivity {

    private String fbtc = "https://wallet-api.fastbitcoins.com/w-api/v1/samourai/";
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Button redeemButton;
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");
    private String addr84;
    private boolean validVoucher = false;
    private TextView log;
    private static final String TAG = "VouchersActivity";
    private String quotationSecret;
    private int quotationId;
    private String email = "sarath@samourai.io";
    private String validVoucherCode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vouchers);

        addr84 = AddressFactory.getInstance(getApplication()).getBIP84(AddressFactory.RECEIVE_CHAIN).getBech32AsString();
        ((TextView) findViewById(R.id.voucher_receive_add)).setText("Receive address: ".concat(addr84));

        setSupportActionBar(findViewById(R.id.appbar_voucher));

        redeemButton = findViewById(R.id.redeem_button);
        log = findViewById(R.id.voucher_log);
        EditText edt = findViewById(R.id.redeem_edttext);

        redeemButton.setText("Check");
        redeemButton.setOnClickListener(view -> {
            if (!validVoucher) {

                Disposable disposable = check(edt.getText().toString())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe((jsonObject, throwable) -> {

                            if (jsonObject != null) {
                                Snackbar.make(view, "Valid voucher", Snackbar.LENGTH_SHORT).show();
                                redeemButton.setText("Redeem");
                                log.setText(jsonObject.toString(2));
                                validVoucherCode = edt.getText().toString();
                                quotationSecret = jsonObject.getString("quotation_secret");
                                quotationId = jsonObject.getInt("quotation_id");
                                validVoucher = true;
                            } else {
                                Snackbar.make(view, "Error : ".concat(throwable.getMessage()), Snackbar.LENGTH_SHORT).show();
                            }
                        });
                compositeDisposable.add(disposable);

            } else {

                Disposable disposable = redeem()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe((jsonObject, throwable) -> {

                            if (jsonObject != null) {
                                Snackbar.make(view, "Success", Snackbar.LENGTH_SHORT).show();
                                redeemButton.setText("Check");
                                log.setText(jsonObject.toString(2));
                            } else {
                                Snackbar.make(view, "Error : ".concat(throwable.getMessage()), Snackbar.LENGTH_SHORT).show();
                            }
                        });
                compositeDisposable.add(disposable);

            }

        });
    }


    private Single<JSONObject> check(String voucher) {

        String url = fbtc.concat("quote");

        return Single.fromCallable(() -> {
            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .connectTimeout(90, TimeUnit.SECONDS)
                    .readTimeout(90, TimeUnit.SECONDS);
            if (TorManager.getInstance(getApplication()).isConnected()) {
                builder.proxy(TorManager.getInstance(this.getApplicationContext()).getProxy());
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
//                LogUtil.info(TAG, "check: ".concat(response.body().string()));
                return new JSONObject(response.body().string());
            } else {
                throw new JSONException("Invalid response");
            }

        });
    }

    private Single<JSONObject> redeem() {

        String url = fbtc.concat("redeem");

        return Single.fromCallable(() -> {
            if (validVoucherCode == null) {
                throw new Exception("Error invalid code");
            }
            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .connectTimeout(90, TimeUnit.SECONDS)
                    .readTimeout(90, TimeUnit.SECONDS);
            if (TorManager.getInstance(getApplication()).isConnected()) {
                builder.proxy(TorManager.getInstance(this.getApplicationContext()).getProxy());
            }

            if (BuildConfig.DEBUG) {
                builder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));
            }

            JSONObject json = new JSONObject();
            json.put("currency", "voucher");
            json.put("email_address", email);
            json.put("currency", "USD");
            json.put("code", validVoucherCode);
            json.put("quotation_id", quotationId);
            json.put("quotation_secret", quotationSecret);
            json.put("delivery_address", addr84);

            RequestBody body = RequestBody.create(JSON, json.toString());
            Request rb = new Request.Builder().url(url)
                    .post(body)
                    .build();

            Response response = builder.build().newCall(rb).execute();

            if (response.body() != null) {
                return new JSONObject(response.body().string());
            } else {
                throw new JSONException("Invalid response");
            }

        });
    }

}
