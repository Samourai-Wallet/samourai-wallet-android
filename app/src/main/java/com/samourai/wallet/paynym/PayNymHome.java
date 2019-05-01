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
import com.google.common.base.Splitter;
import com.google.gson.GsonBuilder;
import com.samourai.wallet.R;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Activity;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.NotSecp256k1Exception;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.crypto.DecryptionException;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.paynym.addPaynym.AddPaynymActivity;
import com.samourai.wallet.paynym.fragments.PaynymListFragment;
import com.samourai.wallet.paynym.fragments.ShowPayNymQRBottomSheet;
import com.samourai.wallet.paynym.paynymDetails.PayNymDetailsActivity;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.MessageSignUtil;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.widgets.ViewPager;
import com.squareup.picasso.Picasso;
import com.yanzhenjie.zbar.Symbol;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.crypto.MnemonicException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

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
    private static final int CLAIM_PAYNYM = 107;

    private TabLayout paynymTabLayout;
    private ViewPager payNymViewPager;
    private static final String TAG = "PayNymHome";
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private PayNymHomeViewModel payNymHomeViewModel;
    private ProgressBar progressBar;
    private TextView paynym, paynymCode;
    private ImageView userAvatar;
    private FloatingActionButton paynymFab;
    private PaynymListFragment followersFragment, followingFragment;
    private String pcode;
    private String tabTitle[] = {"Following", "Followers"};
    private ArrayList<String> followers = new ArrayList<>();
    private Timer timer = null;

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


        Picasso.with(getApplicationContext()).load(com.samourai.wallet.bip47.paynym.WebUtil.PAYNYM_API + pcode + "/avatar")
                .into(userAvatar);


        paynymFab.setOnClickListener(view -> {
            startActivity(new Intent(this, AddPaynymActivity.class));
        });

        payNymHomeViewModel.paymentCode.observe(this, paymentCode -> {
            paynym.setText(paymentCode);
        });

        payNymHomeViewModel.followersList.observe(this, followersList -> {
            if (followersList == null || followersList.size() == 0) {
                return;
            }
            ArrayList<String> filtered = filterArchived(followersList);
            tabTitle[1] = "Followers ".concat(" (").concat(String.valueOf(filtered.size())).concat(")");
            followersFragment.addPcodes(followersList);
            adapter.notifyDataSetChanged();
            followers = followersList;
            doDirectoryTask();
        });
        payNymHomeViewModel.followingList.observe(this, followingList -> {
            if (followingList == null || followingList.size() == 0) {
                return;
            }
            ArrayList<String> filtered = filterArchived(followingList);
            followingFragment.addPcodes(filtered);
            tabTitle[0] = "Following ".concat(" (").concat(String.valueOf(filtered.size())).concat(")");
            adapter.notifyDataSetChanged();

        });
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (!PrefsUtil.getInstance(this).getValue(PrefsUtil.PAYNYM_CLAIMED, false)) {
            doClaimPayNym();
        } else {
            progressBar.setVisibility(View.VISIBLE);
            refreshPayNym();
            doTimer();
        }
    }

    private void doClaimPayNym() {
        Intent intent = new Intent(this, ClaimPayNymActivity.class);
        startActivityForResult(intent, CLAIM_PAYNYM);
    }

    private void refreshPayNym() {
        Disposable disposable = getPaynym()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(jsonObject -> {
                    progressBar.setVisibility(View.GONE);
                    payNymHomeViewModel.setPaynymPayload(jsonObject);
                }, error -> {
                    progressBar.setVisibility(View.GONE);

                });
        compositeDisposable.add(disposable);
        doDirectoryTask();
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.dispose();
        killTimer();
        try {
            PayloadUtil.getInstance(getApplicationContext()).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(getApplicationContext()).getGUID() + AccessFactory.getInstance(getApplicationContext()).getPIN()));
        } catch (MnemonicException.MnemonicLengthException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (DecryptionException e) {
            e.printStackTrace();
        }
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
            Log.i(TAG, "getPaynym: ".concat(new GsonBuilder().setPrettyPrinting().create().toJson(responseObj.getJSONArray("following"))));
            Log.i(TAG, "getPaynym: ".concat(new GsonBuilder().setPrettyPrinting().create().toJson(responseObj.getJSONArray("followers"))));
            return responseObj;

        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (payNymHomeViewModel != null && followingFragment.isVisible()) {
            payNymHomeViewModel.followersList.postValue(payNymHomeViewModel.followersList.getValue());
        }
        if (payNymHomeViewModel != null && followersFragment.isVisible()) {
            payNymHomeViewModel.followingList.postValue(payNymHomeViewModel.followingList.getValue());
        }
        AppUtil.getInstance(getApplicationContext()).checkTimeOut();

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
            case R.id.action_sync_all: {
                if (!AppUtil.getInstance(this).isOfflineMode()) {
                    doSyncAll();
                } else {
                    Toast.makeText(this, R.string.in_offline_mode, Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case R.id.sign: {
                doSign();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == SCAN_PCODE) {
            if (data != null && data.getStringExtra(ZBarConstants.SCAN_RESULT) != null) {
                String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT);
                processScan(strResult);
            }
        }
    }

    private void doSign() {

        MessageSignUtil.getInstance(this).doSign(this.getString(R.string.bip47_sign),
                this.getString(R.string.bip47_sign_text1),
                this.getString(R.string.bip47_sign_text2),
                BIP47Util.getInstance(this).getNotificationAddress().getECKey());

    }

    private void doSupport() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://support.samourai.io/section/32-paynym"));
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
        refreshPayNym();
//
    }

    private ArrayList<String> filterArchived(ArrayList<String> list) {
        ArrayList<String> filtered = new ArrayList<>();

        for (String item : list) {
            if (!BIP47Meta.getInstance().getArchived(item)) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    private void doSyncAll() {

        Set<String> _pcodes = BIP47Meta.getInstance().getSortedByLabels(false);

        //
        // check for own payment code
        //
        try {
            if (_pcodes.contains(BIP47Util.getInstance(this).getPaymentCode().toString())) {
                _pcodes.remove(BIP47Util.getInstance(this).getPaymentCode().toString());
                BIP47Meta.getInstance().remove(BIP47Util.getInstance(this).getPaymentCode().toString());
            }
        } catch (AddressFormatException afe) {
            afe.printStackTrace();
            ;
        }

        for (String pcode : _pcodes) {
            doSync(pcode);
        }


    }

    private void doSync(final String pcode) {
        progressBar.setVisibility(View.VISIBLE);

        Disposable disposable = Observable.fromCallable(() -> {

            try {
                PaymentCode payment_code = new PaymentCode(pcode);

                int idx = 0;
                boolean loop = true;
                ArrayList<String> addrs = new ArrayList<String>();
                while (loop) {
                    addrs.clear();
                    for (int i = idx; i < (idx + 20); i++) {
//                            Log.i("BIP47Activity", "sync receive from " + i + ":" + BIP47Util.getInstance(BIP47Activity.this).getReceivePubKey(payment_code, i));
                        BIP47Meta.getInstance().getIdx4AddrLookup().put(BIP47Util.getInstance(this).getReceivePubKey(payment_code, i), i);
                        BIP47Meta.getInstance().getPCode4AddrLookup().put(BIP47Util.getInstance(this).getReceivePubKey(payment_code, i), payment_code.toString());
                        addrs.add(BIP47Util.getInstance(this).getReceivePubKey(payment_code, i));
//                            Log.i("BIP47Activity", "p2pkh " + i + ":" + BIP47Util.getInstance(BIP47Activity.this).getReceiveAddress(payment_code, i).getReceiveECKey().toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
                    }
                    String[] s = addrs.toArray(new String[addrs.size()]);
                    int nb = APIFactory.getInstance(this).syncBIP47Incoming(s);
//                        Log.i("BIP47Activity", "sync receive idx:" + idx + ", nb == " + nb);
                    if (nb == 0) {
                        loop = false;
                    }
                    idx += 20;
                }

                idx = 0;
                loop = true;
                BIP47Meta.getInstance().setOutgoingIdx(pcode, 0);
                while (loop) {
                    addrs.clear();
                    for (int i = idx; i < (idx + 20); i++) {
                        PaymentAddress sendAddress = BIP47Util.getInstance(this).getSendAddress(payment_code, i);
//                            Log.i("BIP47Activity", "sync send to " + i + ":" + sendAddress.getSendECKey().toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
//                            BIP47Meta.getInstance().setOutgoingIdx(payment_code.toString(), i);
                        BIP47Meta.getInstance().getIdx4AddrLookup().put(BIP47Util.getInstance(this).getSendPubKey(payment_code, i), i);
                        BIP47Meta.getInstance().getPCode4AddrLookup().put(BIP47Util.getInstance(this).getSendPubKey(payment_code, i), payment_code.toString());
                        addrs.add(BIP47Util.getInstance(this).getSendPubKey(payment_code, i));
                    }
                    String[] s = addrs.toArray(new String[addrs.size()]);
                    int nb = APIFactory.getInstance(this).syncBIP47Outgoing(s);
//                        Log.i("BIP47Activity", "sync send idx:" + idx + ", nb == " + nb);
                    if (nb == 0) {
                        loop = false;
                    }
                    idx += 20;
                }

                BIP47Meta.getInstance().pruneIncoming();

                PayloadUtil.getInstance(this).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(this).getGUID() + AccessFactory.getInstance(this).getPIN()));

            } catch (IOException ioe) {
                ;
            } catch (JSONException je) {
                ;
            } catch (DecryptionException de) {
                ;
            } catch (NotSecp256k1Exception nse) {
                ;
            } catch (InvalidKeySpecException ikse) {
                ;
            } catch (InvalidKeyException ike) {
                ;
            } catch (NoSuchAlgorithmException nsae) {
                ;
            } catch (NoSuchProviderException nspe) {
                ;
            } catch (MnemonicException.MnemonicLengthException mle) {
                ;
            }

            return true;

        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aBoolean -> {
                    progressBar.setVisibility(View.GONE);

                }, error -> {
                    error.printStackTrace();
                    progressBar.setVisibility(View.GONE);
                });
        compositeDisposable.add(disposable);

    }

    private void processScan(String data) {

        if (data.startsWith("bitcoin://") && data.length() > 10) {
            data = data.substring(10);
        }
        if (data.startsWith("bitcoin:") && data.length() > 8) {
            data = data.substring(8);
        }

        if (FormatsUtil.getInstance().isValidPaymentCode(data)) {

            try {
                if (data.equals(BIP47Util.getInstance(this).getPaymentCode().toString())) {
                    Toast.makeText(this, R.string.bip47_cannot_scan_own_pcode, Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (AddressFormatException afe) {
                ;
            }

            Intent intent = new Intent(this, PayNymDetailsActivity.class);
            intent.putExtra("pcode", data);
            startActivityForResult(intent, EDIT_PCODE);

        } else if (data.contains("?") && (data.length() >= data.indexOf("?"))) {

            String meta = data.substring(data.indexOf("?") + 1);

            String _meta = null;
            try {
                Map<String, String> map = new HashMap<String, String>();
                if (meta != null && meta.length() > 0) {
                    _meta = URLDecoder.decode(meta, "UTF-8");
                    map = Splitter.on('&').trimResults().withKeyValueSeparator("=").split(_meta);
                }
                Intent intent = new Intent(this, AddPaynymActivity.class);
                intent.putExtra("pcode", data.substring(0, data.indexOf("?")));
                intent.putExtra("label", map.containsKey("title") ? map.get("title").trim() : "");
                startActivityForResult(intent, EDIT_PCODE);
            } catch (UnsupportedEncodingException uee) {
                ;
            } catch (Exception e) {
                ;
            }

        } else {
            Toast.makeText(this, R.string.scan_error, Toast.LENGTH_SHORT).show();
        }


    }

    private void killTimer() {

        if (timer != null) {
            timer.cancel();
            timer = null;
        }

    }

    private void doTimer() {

        if (timer == null) {
            timer = new Timer();

            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    refreshPayNym();
                                }
                            }).start();

                        }
                    });
                }
            }, 5000, 30000);

        }

    }

    private void doDirectoryTask() {

        final Set<String> pcodes = BIP47Meta.getInstance().getSortedByLabels(true);

        if (pcodes != null && pcodes.size() > 0) {
            for (String pcode : pcodes) {
                if (!followers.contains(pcode)) {
                    doUploadFollow(pcode, false);
                }
            }
        }

    }

    private void doUploadFollow(String pcode, boolean isTrust) {
        Disposable disposable = Observable.fromCallable(() -> {
            try {

                JSONObject obj = new JSONObject();
                obj.put("code", BIP47Util.getInstance(this).getPaymentCode().toString());
//                    Log.d("PayNymDetailsActivity", obj.toString());
                String res = com.samourai.wallet.bip47.paynym.WebUtil.getInstance(this).postURL("application/json", null, com.samourai.wallet.bip47.paynym.WebUtil.PAYNYM_API + "api/v1/token", obj.toString());
//                    Log.d("PayNymDetailsActivity", res);

                JSONObject responseObj = new JSONObject(res);
                if (responseObj.has("token")) {
                    String token = responseObj.getString("token");

                    String sig = MessageSignUtil.getInstance(this).signMessage(BIP47Util.getInstance(this).getNotificationAddress().getECKey(), token);
//                        Log.d("PayNymDetailsActivity", sig);

                    obj = new JSONObject();
                    obj.put("target", pcode);
                    obj.put("signature", sig);

//                        Log.d("PayNymDetailsActivity", "follow:" + obj.toString());
                    String endPoint = isTrust ? "api/v1/trust" : "api/v1/follow";
                    res = com.samourai.wallet.bip47.paynym.WebUtil.getInstance(this).postURL("application/json", token, com.samourai.wallet.bip47.paynym.WebUtil.PAYNYM_API + endPoint, obj.toString());
//                        Log.d("PayNymDetailsActivity", res);

                    responseObj = new JSONObject(res);
                    if (responseObj.has("following")) {
                        responseObj.has("token");
                    }


                }

            } catch (JSONException je) {
                je.printStackTrace();
                return false;

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

            return true;

        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(success -> {

                }, error -> {


                });
        compositeDisposable.add(disposable);
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


