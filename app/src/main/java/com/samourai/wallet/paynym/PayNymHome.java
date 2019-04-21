package com.samourai.wallet.paynym;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;
import com.samourai.wallet.R;
import com.samourai.wallet.bip47.BIP47Activity;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.paynym.addPaynym.AddPaynymActivity;
import com.samourai.wallet.paynym.fragments.PaynymListFragment;
import com.samourai.wallet.paynym.fragments.ShowPayNymQRBottomSheet;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.widgets.ViewPager;
import com.squareup.picasso.Picasso;
import com.yanzhenjie.zbar.Symbol;

import org.bitcoinj.core.AddressFormatException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.samourai.wallet.bip47.paynym.WebUtil.PAYNYM_API;


public class PayNymHome extends AppCompatActivity {


    private static final int EDIT_PCODE = 2000;
    private static final int RECOMMENDED_PCODE = 2001;
    private static final int SCAN_PCODE = 2077;

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
    private String pcode;
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
        this.pcode = BIP47Util.getInstance(this).getPaymentCode().toString();

        paynymCode.setText(BIP47Meta.getInstance().getDisplayLabel(pcode));
        followersFragment = PaynymListFragment.newInstance();
        followingFragment = PaynymListFragment.newInstance();

        initPaynym();

        Picasso.with(getApplicationContext()).load(com.samourai.wallet.bip47.paynym.WebUtil.PAYNYM_API + pcode + "/avatar")
                .into(userAvatar);


        paynymFab.setOnClickListener(view -> {
            startActivity(new Intent(this, AddPaynymActivity.class));
        });

        payNymHomeViewModel.paymentCode.observe(this, paymentCode -> {
            paynym.setText(paymentCode);
        });

        payNymHomeViewModel.followersList.observe(this, followersList -> {
            if (followersList != null) {
                tabTitle[1] = "Followers ".concat(" (").concat(String.valueOf(followersList.size())).concat(")");
            }
            followersFragment.addPcodes(followersList);
            adapter.notifyDataSetChanged();
        });
        payNymHomeViewModel.followingList.observe(this, followingList -> {

            followingFragment.addPcodes(followingList);
            if (followingList != null) {
                tabTitle[0] = "Following ".concat(" (").concat(String.valueOf(followingList.size())).concat(")");
            }
            adapter.notifyDataSetChanged();

        });
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void initPaynym() {
        progressBar.setVisibility(View.VISIBLE);
        payNymViewPager.setVisibility(View.INVISIBLE);
        Disposable disposable = getPaynym()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(jsonObject -> {
                    progressBar.setVisibility(View.GONE);
                    payNymViewPager.setVisibility(View.VISIBLE);
                    payNymHomeViewModel.setPaynymPayload(jsonObject);
                }, error -> {

                    payNymViewPager.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);

                });
        compositeDisposable.add(disposable);
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.dispose();
        super.onDestroy();
    }

    private Observable<JSONObject> getPaynym() {

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
    protected void onResume() {
        if (followingFragment != null && followingFragment.isVisible()) {
            followingFragment.rebuildList();
        }
        if (followersFragment != null && followersFragment.isVisible()) {
            followersFragment.rebuildList();
        }
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                break;
            }
            case R.id.action_support: {
                doSupport();
                break;
            }
            case R.id.action_scan_qr: {
                Intent intent = new Intent(this, ZBarScannerActivity.class);
                intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{Symbol.QRCODE});
                startActivityForResult(intent, SCAN_PCODE);
                break;
            }
            case R.id.action_unarchive: {
                doUnArchive();
                break;
            }
            case R.id.action_paynym_share_qr: {
                Bundle bundle = new Bundle();
                bundle.putString("pcode", pcode);
                ShowPayNymQRBottomSheet showPayNymQRBottomSheet = new ShowPayNymQRBottomSheet();
                showPayNymQRBottomSheet.setArguments(bundle);
                showPayNymQRBottomSheet.show(getSupportFragmentManager(), showPayNymQRBottomSheet.getTag());
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bip47_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void doSupport() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://support.samourai.io/section/14-payment-codes"));
        startActivity(intent);
    }

    private void doUnArchive() {

        Set<String> _pcodes = BIP47Meta.getInstance().getSortedByLabels(true);

        //
        // check for own payment code
        //
        try {
            if (_pcodes.contains(BIP47Util.getInstance(this).getPaymentCode().toString())) {
                _pcodes.remove(BIP47Util.getInstance(this).getPaymentCode().toString());
                BIP47Meta.getInstance().remove(BIP47Util.getInstance(this).getPaymentCode().toString());
            }
        } catch (AddressFormatException afe) {
            ;
        }

        for (String pcode : _pcodes) {
            BIP47Meta.getInstance().setArchived(pcode, false);
        }

        String[] pcodes = new String[_pcodes.size()];
        int i = 0;
        for (String pcode : _pcodes) {
            pcodes[i] = pcode;
            ++i;
        }
        initPaynym();
//
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


