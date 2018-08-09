package com.samourai.wallet.pinning;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;

import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.ConnectivityStatus;
import com.samourai.wallet.R;

public class SSLVerifierThreadUtil {

    private static SSLVerifierThreadUtil instance = null;
    private static Context context = null;

    private AlertDialog alertDialog = null;

    private SSLVerifierThreadUtil() {
        ;
    }

    public static SSLVerifierThreadUtil getInstance(Context ctx) {

        context = ctx;

        if (instance == null) {
            instance = new SSLVerifierThreadUtil();
        }

        return instance;
    }

    public void validateSSLThread() {

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                if (!AppUtil.getInstance(context).isOfflineMode()) {

                    //Pin SSL certificate
                    switch (SSLVerifierUtil.getInstance(context).certificatePinned()) {
                        case SSLVerifierUtil.STATUS_POTENTIAL_SERVER_DOWN:
                            //On connection issue: 2 choices - retry or exit
                            showAlertDialog(context.getString(R.string.ssl_no_connection), false);
                            break;
                        case SSLVerifierUtil.STATUS_PINNING_FAIL:
                            //On fail: only choice is to exit app
                            showAlertDialog(context.getString(R.string.ssl_pinning_invalid), true);
                            break;
                        case SSLVerifierUtil.STATUS_PINNING_SUCCESS:
                            //Certificate pinning successful: safe to continue
                            break;
                    }
                } else {
                    //On connection issue: 2 choices - retry or exit
                    showAlertDialog(context.getString(R.string.ssl_no_connection), false);
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ;
                    }
                });

                Looper.loop();

            }
        }).start();
    }

    private void showAlertDialog(final String message, final boolean forceExit){

        if (!((Activity) context).isFinishing()) {

            if(alertDialog != null)alertDialog.dismiss();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(message);
            builder.setCancelable(false);

            if(!forceExit) {
                builder.setPositiveButton(R.string.retry,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int id) {
                                d.dismiss();
                                //Retry
                                validateSSLThread();
                            }
                        });
            }

            builder.setNegativeButton(R.string.exit,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface d, int id) {
                            d.dismiss();
                            ((Activity) context).finish();
                        }
                    });

            alertDialog = builder.create();
            alertDialog.show();
        }
    }
}
