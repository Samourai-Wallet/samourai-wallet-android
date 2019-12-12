package com.samourai.wallet.vouchers;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.support.constraint.Group;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.samourai.wallet.R;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.VouchersUtil;
import com.samourai.wallet.vouchers.providers.FBTCProvider;
import com.samourai.wallet.vouchers.providers.ValidateResponse;
import com.samourai.wallet.vouchers.providers.VoucherProvider;

import java.text.DecimalFormat;
import java.util.Objects;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class VouchersActivity extends AppCompatActivity {

    private static int FBTC_VOCHER_SIZE = 12;
    private TextView voucherTextCount, successResponseTextView;
    private static final String TAG = "VouchersActivity";
    private EditText editText1, editText2, editText3, editText4, emailInput;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Button redeemButton;
    private Group loaderGroup, successMessageGroup;
    private VoucherProvider fbtcProvider;
    private ValidateResponse validateResponse;
    private CoordinatorLayout snackBarContainer;
    private ValueAnimator buttonVisibilityAnimator = ValueAnimator.ofFloat(0.3f, 1);
    final private DecimalFormat df = new DecimalFormat("#");

    public VouchersActivity() {
    }


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
        emailInput = findViewById(R.id.voucher_email_input);

        editText1.addTextChangedListener(new SplitTextWatcher(editText2, editText1));
        editText2.addTextChangedListener(new SplitTextWatcher(editText3, editText1));
        editText3.addTextChangedListener(new SplitTextWatcher(editText4, editText2));
        editText4.addTextChangedListener(new SplitTextWatcher(editText4, editText3));

        editText1.addTextChangedListener(voucherCounter);
        editText2.addTextChangedListener(voucherCounter);
        editText3.addTextChangedListener(voucherCounter);
        editText4.addTextChangedListener(voucherCounter);

        successResponseTextView = findViewById(R.id.voucher_redeem_success_text);
        redeemButton = findViewById(R.id.voucher_redeemButton);
        successMessageGroup = findViewById(R.id.voucher_success_message_group);
        loaderGroup = findViewById(R.id.redeem_loader_group);
        snackBarContainer = findViewById(R.id.voucher_snackbar_container);
        loaderGroup.setVisibility(View.GONE);
        fbtcProvider = new FBTCProvider(getApplicationContext());
        redeemButton.setEnabled(false);
        redeemButton.setOnClickListener(view -> {
            if (validateResponse == null) {
                validate();
            } else {
                beginRedeem();
            }
        });
        voucherTextCount = findViewById(R.id.vouchers_code_count);
        setSupportActionBar(findViewById(R.id.appbar_voucher));
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        buttonVisibilityAnimator.addUpdateListener(valueAnimator1 -> redeemButton.setAlpha(valueAnimator1.getAnimatedFraction()));

        df.setMinimumIntegerDigits(1);
        df.setMinimumFractionDigits(8);
        df.setMaximumFractionDigits(8);

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
                if (Objects.requireNonNull(getCurrentFocus()).getId() == editText4.getId()) {
                    hideKeyboard(VouchersActivity.this);
                }
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

    private void enableVoucherInput(boolean enable) {
        editText1.setEnabled(enable);
        editText2.setEnabled(enable);
        editText3.setEnabled(enable);
        editText4.setEnabled(enable);
    }

    private void clearVoucherInput() {
        editText1.setText("");
        editText2.setText("");
        editText3.setText("");
        editText4.setText("");
    }

    private void validate() {

        String email = emailInput.getText().toString();
        String voucher = getEnteredVoucher();

        if (!(!TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches())) {
            emailInput.setError("Invalid email");
            return;
        }

        if (!VouchersUtil.getInstance().isValidFastBitcoinsCode(voucher)) {
            showErrorSnackBar("Invalid Voucher Code");
            return;
        }
        enableVoucherInput(false);
        loaderGroup.setVisibility(View.VISIBLE);
        Disposable disposable = fbtcProvider.validate(email, voucher)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((response, throwable) -> {
                    validateResponse = response;
                    loaderGroup.setVisibility(View.GONE);
                    enableVoucherInput(true);
                    if (validateResponse != null) {
                        redeemButton.setBackground(getDrawable(R.drawable.button_green));
                        redeemButton.setText("Redeem \n ".concat(df.format(validateResponse.getAmount().doubleValue() / 1e8)).concat(" BTC"));
                    } else {
                        String error = throwable.getMessage();
                        if (error.equals("invalid request")) {
                            error = "Invalid voucher";
                        }
                        showErrorSnackBar("Error : ".concat(error));
                    }
                });
        compositeDisposable.add(disposable);
    }

    private void beginRedeem() {

        loaderGroup.setVisibility(View.VISIBLE);
        String addr84 = AddressFactory.getInstance(getApplication()).getBIP84(AddressFactory.RECEIVE_CHAIN).getBech32AsString();

        Disposable disposable = fbtcProvider.redeem(validateResponse, addr84)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((redeemSuccess, throwable) -> {
                    loaderGroup.setVisibility(View.GONE);
                    if(throwable !=null){
                        showErrorSnackBar(throwable.getMessage());
                        return;
                    }
                    if (redeemSuccess != null && redeemSuccess) {
                        clearVoucherInput();
                        loaderGroup.setVisibility(View.INVISIBLE);
                        successMessageGroup.setVisibility(View.VISIBLE);
                        successResponseTextView.setText("Voucher code accepted by ".concat(fbtcProvider.getProviderName()).concat(" Redemption now being processed."));
                        validateResponse = null;
                    } else {
                        showErrorSnackBar("Error : ".concat(throwable.getMessage()));
                    }
                });
        compositeDisposable.add(disposable);

    }

    private void showErrorSnackBar(String message) {
        Snackbar snackbar = Snackbar.make(snackBarContainer, message, Snackbar.LENGTH_SHORT);
        snackbar.getView().setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.red));
        snackbar.show();
    }


    public class SplitTextWatcher implements TextWatcher {
        private EditText etPrev;
        private EditText etNext;

        SplitTextWatcher(EditText etNext, EditText etPrev) {
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
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
