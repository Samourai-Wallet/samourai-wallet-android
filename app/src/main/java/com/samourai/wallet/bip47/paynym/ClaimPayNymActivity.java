package com.samourai.wallet.bip47.paynym;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.util.AppUtil;

import com.samourai.wallet.R;
import com.samourai.wallet.util.MessageSignUtil;
import com.samourai.wallet.util.PrefsUtil;

import org.json.JSONException;
import org.json.JSONObject;

public class ClaimPayNymActivity extends Activity {

    private Button btClaim = null;
    private Button btRefuse = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paynym_claim);
        setTitle(R.string.paynym);

        btClaim = (Button)findViewById(R.id.claim);
        btClaim.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                AlertDialog.Builder dlg = new AlertDialog.Builder(ClaimPayNymActivity.this)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.claim_paynym_prompt)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                dialog.dismiss();

                                doClaimPayNymTask();

                            }

                        }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ;
                            }
                        });

                dlg.show();


            }

        });

        btRefuse = (Button)findViewById(R.id.no_claim);
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

        new Thread(new Runnable() {

            private Handler handler = new Handler();

            private ProgressDialog progress = null;

            @Override
            public void run() {

                handler.post(new Runnable() {
                    public void run() {
                        progress = new ProgressDialog(ClaimPayNymActivity.this);
                        progress.setCancelable(false);
                        progress.setTitle(R.string.app_name);
                        progress.setMessage(getString(R.string.please_wait));
                        progress.show();
                    }
                });

                Looper.prepare();

                try {

                    JSONObject obj = new JSONObject();
                    obj.put("code", BIP47Util.getInstance(ClaimPayNymActivity.this).getPaymentCode().toString());
                    Log.d("ClaimPayNymActivity", obj.toString());
                    String res = WebUtil.getInstance(ClaimPayNymActivity.this).postURL("application/json", null, WebUtil.PAYNYM_API + "api/v1/create", obj.toString());
                    Log.d("ClaimPayNymActivity", res);

                    JSONObject responseObj = new JSONObject(res);
                    if(responseObj.has("token"))    {
                        String token = responseObj.getString("token");

                        String sig = MessageSignUtil.getInstance(ClaimPayNymActivity.this).signMessage(BIP47Util.getInstance(ClaimPayNymActivity.this).getNotificationAddress().getECKey(), token);
                        Log.d("ClaimPayNymActivity", sig);

                        obj = new JSONObject();
                        obj.put("signature", sig);

                        res = WebUtil.getInstance(ClaimPayNymActivity.this).postURL("application/json", token, WebUtil.PAYNYM_API + "api/v1/claim", obj.toString());
                        Log.d("ClaimPayNymActivity", res);

                        responseObj = new JSONObject(res);
                        if(responseObj.has("claimed") && responseObj.has("token"))    {

                            doClaimed();

                        }
                        else if(responseObj.has("result") && responseObj.getInt("result") == 400)   {
                            ;
                        }
                        else    {
                            ;
                        }

                    }
                    else if(responseObj.has("claimed"))    {

                        doClaimed();

                    }
                    else if(responseObj.has("result") && responseObj.getInt("result") == 400)   {

                    }
                    else    {

                    }

                }
                catch(JSONException je) {
                    je.printStackTrace();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }

                Looper.loop();

                handler.post(new Runnable() {
                    public void run() {
                        if(progress != null && progress.isShowing())    {
                            progress.cancel();
                        }
                    }
                });

            }

            private void doClaimed()  {

                try {
                    PrefsUtil.getInstance(ClaimPayNymActivity.this).setValue(PrefsUtil.PAYNYM_CLAIMED, true);
                    Log.d("ClaimPayNymActivity", "paynym claimed:" + BIP47Util.getInstance(ClaimPayNymActivity.this).getPaymentCode().toString());

                    JSONObject obj = new JSONObject();
                    obj.put("nym", BIP47Util.getInstance(ClaimPayNymActivity.this).getPaymentCode().toString());
                    String res = WebUtil.getInstance(ClaimPayNymActivity.this).postURL("application/json", null, WebUtil.PAYNYM_API + "api/v1/nym", obj.toString());
                    Log.d("ClaimPayNymActivity", res);

                    JSONObject responseObj = new JSONObject(res);
                    if(responseObj.has("nymName"))    {

                        final String strNymName = responseObj.getString("nymName");

                        AlertDialog.Builder dlg = new AlertDialog.Builder(ClaimPayNymActivity.this)
                                .setTitle("Your PayNym has been claimed")
                                .setMessage(strNymName)
//                                    .setView(imgLayout)
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        ClaimPayNymActivity.this.finish();

                                    }
                                });
                        if(!isFinishing())    {
                            dlg.show();
                        }

                    }
                }
                catch(JSONException je) {
                    ;
                }
                catch(Exception e) {
                    ;
                }

            }

        }).start();

    }

}
