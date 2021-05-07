package com.samourai.wallet;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.payload.ExternalBackupManager;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.LogUtil;
import com.samourai.wallet.util.TimeOutUtil;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


@SuppressLint("Registered")
public class SamouraiActivity extends AppCompatActivity {

    protected int account = 0;
    private boolean switchThemes = false;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private static final String TAG = "SamouraiActivity";
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getExtras() != null && getIntent().getExtras().containsKey("_account")) {
            if (getIntent().getExtras().getInt("_account") == WhirlpoolMeta.getInstance(getApplicationContext()).getWhirlpoolPostmix()) {
                account = WhirlpoolMeta.getInstance(getApplicationContext()).getWhirlpoolPostmix();
            }
        }
        setUpTheme();
    }

    private void setUpTheme() {
        if (switchThemes)
            if (account == WhirlpoolMeta.getInstance(getApplication()).getWhirlpoolPostmix()) {
                setTheme(R.style.Theme_Samourai_Whirlpool_Material);
            }
    }


    protected void saveState() {
        if(TimeOutUtil.getInstance().isTimedOut()){
            return;
        }
        Disposable disposable = Observable.fromCallable(() -> {
            PayloadUtil.getInstance(getApplicationContext()).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(getApplicationContext()).getGUID() + AccessFactory.getInstance(getApplicationContext()).getPIN()));
            return true;
        })      .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribe((aBoolean) -> {
                },throwable -> {
                    LogUtil.error(TAG,throwable);
                });
        compositeDisposable.add(disposable);
    }


    @Override
    protected void onDestroy() {
        compositeDisposable.dispose();
        super.onDestroy();
    }

    public void setSwitchThemes(boolean switchThemes) {
        this.switchThemes = switchThemes;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        ExternalBackupManager.onActivityResult(requestCode, resultCode, data, getApplication());
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void hidekeyboard(){
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

}
