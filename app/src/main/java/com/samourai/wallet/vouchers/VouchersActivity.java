package com.samourai.wallet;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.support.constraint.Group;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.samourai.wallet.tor.TorManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class VouchersActivity extends AppCompatActivity {

    private String fbtc = null;
    private static int FBTC_VOCHER_SIZE = 12;
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private String addr84;
    private boolean validVoucher = false;
    private TextView log, voucherTextCount;
    private static final String TAG = "VouchersActivity";
    private String quotationSecret;
    private int quotationId;
    private String email = null;
    private String voucherCode;
    private EditText editText1, editText2, editText3, editText4;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Button redeemButton;
    Group loaderGroup;

    ValueAnimator buttonVisibilityAnimator = ValueAnimator.ofFloat(0.3f, 1);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vouchers);
        buttonVisibilityAnimator.setDuration(800);
        buttonVisibilityAnimator.setInterpolator(new AccelerateInterpolator());

        editText1 = findViewById(R.id.redeem_edttext_1);
        editText2 = findViewById(R.id.redeem_edttext_2);
        editText3 = findViewById(R.id.redeem_edttext_3);
        editText4 = findViewById(R.id.redeem_edttext_4);

        editText1.addTextChangedListener(new GenericTextWatcher(editText2, editText1));
        editText2.addTextChangedListener(new GenericTextWatcher(editText3, editText1));
        editText3.addTextChangedListener(new GenericTextWatcher(editText4, editText2));
        editText4.addTextChangedListener(new GenericTextWatcher(editText4, editText3));

        editText1.addTextChangedListener(voucherCounter);
        editText2.addTextChangedListener(voucherCounter);
        editText3.addTextChangedListener(voucherCounter);
        editText4.addTextChangedListener(voucherCounter);

        redeemButton = findViewById(R.id.voucher_redeemButton);
        loaderGroup = findViewById(R.id.redeem_loader_group);

        redeemButton.setOnClickListener(view -> {
            if (loaderGroup.getVisibility() == View.GONE)
                loaderGroup.setVisibility(View.VISIBLE);
            else
                loaderGroup.setVisibility(View.GONE);
        });
        voucherTextCount = findViewById(R.id.vouchers_code_count);
//
//        addr84 = AddressFactory.getInstance(getApplication()).getBIP84(AddressFactory.RECEIVE_CHAIN).getBech32AsString();
//        ((TextView) findViewById(R.id.voucher_receive_add)).setText("Receive address: ".concat(addr84));
//
        setSupportActionBar(findViewById(R.id.appbar_voucher));
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        buttonVisibilityAnimator.addUpdateListener(valueAnimator1 -> {
            redeemButton.setAlpha(valueAnimator1.getAnimatedFraction());
        });
//        fbtc = VouchersUtil.getInstance().getFastBitcoinsAPI();
//        email = VouchersUtil.getInstance().getFastBitcoinsEmail();
//
//        redeemButton = findViewById(R.id.redeem_button);
//        log = findViewById(R.id.voucher_log);
//        EditText edt = findViewById(R.id.redeem_edttext_1);
//
//        redeemButton.setText("Check");
//        redeemButton.setOnClickListener(view -> {
//            if (!validVoucher) {
//
//                Disposable disposable = check(edt.getText().toString())
//                        .subscribeOn(Schedulers.io())
//                        .observeOn(AndroidSchedulers.mainThread())
//                        .subscribe((jsonObject, throwable) -> {
//
//                            if (jsonObject != null) {
//                                Snackbar.make(view, "Valid voucher", Snackbar.LENGTH_SHORT).show();
//                                redeemButton.setText("Redeem");
//                                log.setText(jsonObject.toString(2));
//                                voucherCode = edt.getText().toString().toUpperCase().trim();
//                                quotationSecret = jsonObject.getString("quotation_secret");
//                                quotationId = jsonObject.getInt("quotation_id");
//                                validVoucher = VouchersUtil.getInstance().isValidFastBitcoinsCode(voucherCode);
//                            } else {
//                                Snackbar.make(view, "Error : ".concat(throwable.getMessage()), Snackbar.LENGTH_SHORT).show();
//                            }
//                        });
//                compositeDisposable.add(disposable);
//
//            } else {
//
//                Disposable disposable = redeem()
//                        .subscribeOn(Schedulers.io())
//                        .observeOn(AndroidSchedulers.mainThread())
//                        .subscribe((jsonObject, throwable) -> {
//
//                            if (jsonObject != null) {
//                                Snackbar.make(view, "Success", Snackbar.LENGTH_SHORT).show();
//                                redeemButton.setText("Check");
//                                log.setText(jsonObject.toString(2));
//                            } else {
//                                Snackbar.make(view, "Error : ".concat(throwable.getMessage()), Snackbar.LENGTH_SHORT).show();
//                            }
//                        });
//                compositeDisposable.add(disposable);
//
//            }
//
//        });
    }

    private TextWatcher voucherCounter = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            int len = getEnteredVoucher().length();
            voucherTextCount.setText(String.valueOf(len).concat("/").concat(String.valueOf(FBTC_VOCHER_SIZE)));

            if (len == FBTC_VOCHER_SIZE) {
                redeemButton.setEnabled(true);
                hideKeyboard(VouchersActivity.this);
                buttonVisibilityAnimator.start();
            } else {
                buttonVisibilityAnimator.reverse();
                redeemButton.setEnabled(false);
                redeemButton.setAlpha(0.3f);
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };

    private String getEnteredVoucher() {
        String first = editText1.getText().toString();
        String second = editText2.getText().toString();
        String third = editText3.getText().toString();
        String fourth = editText4.getText().toString();
        return first.concat(second).concat(third).concat(fourth);
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
            if (voucherCode == null) {
                validVoucher = false;
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
            json.put("code", voucherCode);
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

    public class GenericTextWatcher implements TextWatcher {
        private EditText etPrev;
        private EditText etNext;

        public GenericTextWatcher(EditText etNext, EditText etPrev) {
            this.etPrev = etPrev;
            this.etNext = etNext;
        }

        @Override
        public void afterTextChanged(Editable editable) {
            String text = editable.toString();
            if (text.length() == 3)
                etNext.requestFocus();
            else if (text.length() == 0)
                etPrev.requestFocus();
        }

        @Override
        public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
        }

        @Override
        public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
        }
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
