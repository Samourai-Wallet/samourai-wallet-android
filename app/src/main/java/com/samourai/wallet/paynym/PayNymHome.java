package com.samourai.wallet.paynym;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.samourai.wallet.R;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.paynym.addPaynym.AddPaynymActivity;
import com.samourai.wallet.paynym.fragments.PaynymListFragment;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.widgets.ViewPager;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.samourai.wallet.bip47.paynym.WebUtil.PAYNYM_API;


public class PayNymHome extends AppCompatActivity {

    private TabLayout paynymTabLayout;
    private ViewPager payNymViewPager;
    private static final String TAG = "PayNymHome";
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    private PayNymHomeViewModel payNymHomeViewModel;
    private ProgressBar progressBar;
    private TextView paynym, paynymCode;
    private ImageView userAvatar;
    private FloatingActionButton paynymFab;
    private PaynymListFragment followersFragment, followingFragment;
    private String tabTitle[] = {"Following", "Followers"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay_nym_home);
        setSupportActionBar(findViewById(R.id.toolbar_paynym));

        paynymTabLayout = findViewById(R.id.paynym_tabs);
        payNymViewPager = findViewById(R.id.paynym_viewpager);
        paynymTabLayout.setupWithViewPager(payNymViewPager);
        progressBar = findViewById(R.id.paynym_progress);
        paynym = findViewById(R.id.txtview_paynym);
        paynymCode = findViewById(R.id.paynym_payment_code);
        userAvatar = findViewById(R.id.paybyn_user_avatar);
        paynymFab = findViewById(R.id.paynym_fab);

        payNymViewPager.enableSwipe(true);
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        payNymViewPager.setAdapter(adapter);

        payNymHomeViewModel = ViewModelProviders.of(this).get(PayNymHomeViewModel.class);
        final String strPaymentCode = BIP47Util.getInstance(this).getPaymentCode().toString();
        paynymCode.setText(BIP47Meta.getInstance().getDisplayLabel(strPaymentCode));
        followersFragment = PaynymListFragment.newInstance();
        followingFragment = PaynymListFragment.newInstance();

        initPaynym();

        Picasso.with(getApplicationContext()).load(com.samourai.wallet.bip47.paynym.WebUtil.PAYNYM_API + strPaymentCode + "/avatar")
                .into(userAvatar);


        paynymFab.setOnClickListener(view -> {
            startActivity(new Intent(this, AddPaynymActivity.class));
        });

        payNymHomeViewModel.paymentCode.observe(this, paymentCode -> {
            paynym.setText(paymentCode);
        });

        payNymHomeViewModel.followersList.observe(this, followersList -> {
            tabTitle[1] = "Followers ".concat(" (").concat(String.valueOf(followersList.size())).concat(")");
            followersFragment.addPcodes(followersList);
            adapter.notifyDataSetChanged();
        });
        payNymHomeViewModel.followingList.observe(this, followingList -> {

            followingFragment.addPcodes(followingList);
            tabTitle[0] = "Following ".concat(" (").concat(String.valueOf(followingList.size())).concat(")");
            adapter.notifyDataSetChanged();

        });
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void initPaynym() {
        progressBar.setVisibility(View.VISIBLE);
        Disposable disposable = getPanym()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(jsonObject -> {
                    progressBar.setVisibility(View.GONE);
                    payNymHomeViewModel.setPaynymPayload(jsonObject);
                }, error -> {
                    progressBar.setVisibility(View.GONE);

                });
        compositeDisposable.add(disposable);
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.dispose();
        super.onDestroy();
    }

    private Observable<JSONObject> getPanym() {

        return Observable.fromCallable(() -> {
            final String strPaymentCode = BIP47Util.getInstance(PayNymHome.this).getPaymentCode().toString();
            JSONObject obj = new JSONObject();
            obj.put("nym", strPaymentCode);
            String res = "{}";

            if (!AppUtil.getInstance(PayNymHome.this).isOfflineMode()) {
                res = com.samourai.wallet.bip47.paynym.WebUtil.getInstance(PayNymHome.this).postURL("application/json", null, PAYNYM_API + "api/v1/nym", obj.toString());
            } else {
                res = PayloadUtil.getInstance(PayNymHome.this).deserializePayNyms().toString();
            }
            JSONObject responseObj = new JSONObject(res);
            if (responseObj.has("codes")) {
                JSONArray array = responseObj.getJSONArray("codes");

            }
            return responseObj;

        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {


        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return followingFragment;
            } else {
                return followersFragment;
            }
        }

        @Override
        public int getCount() {
            return tabTitle.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return tabTitle[position];
        }
    }
}


