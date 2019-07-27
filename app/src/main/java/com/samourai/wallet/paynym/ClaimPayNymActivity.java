package com.samourai.wallet.paynym;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.samourai.wallet.R;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.paynym.WebUtil;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.MessageSignUtil;
import com.samourai.wallet.util.PrefsUtil;

import org.json.JSONException;
import org.json.JSONObject;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ClaimPayNymActivity extends Activity {

    private Button btClaim = null;
    private Button btRefuse = null;
    CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paynym_claim);
        setTitle(R.string.paynym);

        btClaim = (Button) findViewById(R.id.claim);
        btClaim.setOnClickListener(v -> {

            AlertDialog.Builder dlg = new AlertDialog.Builder(ClaimPayNymActivity.this)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.claim_paynym_prompt)
                    .setPositiveButton(R.string.yes, (dialog, whichButton) -> {

                        dialog.dismiss();

                        doClaimPayNymTask();

                    }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            ;
                        }
                    });

            dlg.show();


        });

        btRefuse = (Button) findViewById(R.id.no_claim);
        btRefuse.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                PrefsUtil.getInstance(ClaimPayNymActivity.this).setValue(PrefsUtil.PAYNYM_REFUSED, true);
                finish();
            }

        });

    }

    @Override
    public void onResume() {
        super.onResume();
        AppUtil.getInstance(ClaimPayNymActivity.this).checkTimeOut();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void doClaimPayNymTask() {
        ProgressDialog progressDialog = new ProgressDialog(ClaimPayNymActivity.this);
        progressDialog.setCancelable(false);
        progressDialog.setTitle(R.string.app_name);
        progressDialog.setMessage(getString(R.string.please_wait));
        progressDialog.show();
        Disposable disposable = initClaimRequest()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(jsonObject -> {

                    Disposable disposable2 = getPaynym().subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(s -> {
                                doClaimed(s);

                                if (progressDialog.isShowing())
                                    progressDialog.dismiss();

                            }, throwable -> {
                                Toast.makeText(this, "Error ".concat(throwable.getMessage()), Toast.LENGTH_LONG).show();
                                throwable.printStackTrace();
                                if (progressDialog.isShowing())
                                    progressDialog.dismiss();

                            });
                    compositeDisposable.add(disposable2);
                }, error -> {
                    if (progressDialog.isShowing())
                        progressDialog.dismiss();
                    Toast.makeText(this, "Error ".concat(error.getMessage()), Toast.LENGTH_LONG).show();
                    error.printStackTrace();
                });
        compositeDisposable.add(disposable);

    }

    private void doClaimed(String res) {

        try {
            PrefsUtil.getInstance(ClaimPayNymActivity.this).setValue(PrefsUtil.PAYNYM_CLAIMED, true);
            Log.d("ClaimPayNymActivity", "paynym claimed:" + BIP47Util.getInstance(ClaimPayNymActivity.this).getPaymentCode().toString());
            JSONObject responseObj = new JSONObject(res);
            if (responseObj.has("nymName")) {

                final String strNymName = responseObj.getString("nymName");

                AlertDialog.Builder dlg = new AlertDialog.Builder(ClaimPayNymActivity.this)
                        .setTitle("Your PayNym has been claimed")
                        .setMessage(strNymName)
//                                    .setView(imgLayout)
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok, (dialog, whichButton) -> {
                            setResult(RESULT_OK);
                            ClaimPayNymActivity.this.finish();

                        });

                dlg.show();

            }
        } catch (JSONException je) {
            ;
        } catch (Exception e) {
            ;
        }

    }

    private Observable<JSONObject> initClaimRequest() {
        return Observable.fromCallable(() -> {

            JSONObject obj = new JSONObject();
            obj.put("code", BIP47Util.getInstance(ClaimPayNymActivity.this).getPaymentCode().toString());
            Log.d("ClaimPayNymActivity", obj.toString());
            String res = WebUtil.getInstance(ClaimPayNymActivity.this).postURL("application/json", null, WebUtil.PAYNYM_API + "api/v1/create", obj.toString());
            Log.d("ClaimPayNymActivity", res);

            JSONObject responseObj = new JSONObject(res);
            if (responseObj.has("token")) {
                String token = responseObj.getString("token");

                String sig = MessageSignUtil.getInstance(ClaimPayNymActivity.this).signMessage(BIP47Util.getInstance(ClaimPayNymActivity.this).getNotificationAddress().getECKey(), token);
                Log.d("ClaimPayNymActivity", sig);

                obj = new JSONObject();
                obj.put("signature", sig);

                res = WebUtil.getInstance(ClaimPayNymActivity.this).postURL("application/json", token, WebUtil.PAYNYM_API + "api/v1/claim", obj.toString());
                Log.d("ClaimPayNymActivity", res);

                responseObj = new JSONObject(res);
                if (responseObj.has("claimed") && responseObj.has("token")) {

                    return responseObj;

                } else if (responseObj.has("result") && responseObj.getInt("result") == 400) {
                    return null;

                } else {
                    return null;
                }

            } else if (responseObj.has("claimed")) {

                return responseObj;

            } else if (responseObj.has("result") && responseObj.getInt("result") == 400) {
                return null;
            } else {
                return null;

            }
        });
    }


    private Observable<String> getPaynym() {
        return Observable.fromCallable(() -> {

            JSONObject obj = new JSONObject();
            obj.put("nym", BIP47Util.getInstance(ClaimPayNymActivity.this).getPaymentCode().toString());
            String res = WebUtil.getInstance(ClaimPayNymActivity.this).postURL("application/json", null, WebUtil.PAYNYM_API + "api/v1/nym", obj.toString());
            Log.d("ClaimPayNymActivity", res);
            return res;
        });
    }
}
