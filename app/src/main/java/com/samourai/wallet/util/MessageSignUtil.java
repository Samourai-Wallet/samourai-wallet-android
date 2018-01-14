package com.samourai.wallet.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.EditText;
import android.widget.TextView;

import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiWallet;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.params.MainNetParams;

import java.security.SignatureException;
import java.sql.Date;

public class MessageSignUtil {

    private static Context context = null;
    private static MessageSignUtil instance = null;

    private MessageSignUtil() { ; }

    public static MessageSignUtil getInstance() {

        if(instance == null) {
            instance = new MessageSignUtil();
        }

        return instance;
    }

    public static MessageSignUtil getInstance(Context ctx) {

        context = ctx;

        if(instance == null) {
            instance = new MessageSignUtil();
        }

        return instance;
    }

    public void doSign(String title, String message1, String message2, final ECKey ecKey) {

        final String strDate = new Date(System.currentTimeMillis()).toLocaleString();
        final String message = message2 + " " + strDate;

        final EditText etMessage = new EditText(context);
        etMessage.setHint(message);

        AlertDialog.Builder dlg = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message1)
                .setView(etMessage)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                        String strSignedMessage = null;
                        String result = etMessage.getText().toString();
                        if(result == null || result.length() == 0)    {
                            strSignedMessage = MessageSignUtil.getInstance().signMessageArmored(ecKey, message);
                        }
                        else    {
                            strSignedMessage = MessageSignUtil.getInstance().signMessageArmored(ecKey, result);
                        }

                        TextView showText = new TextView(context);
                        showText.setText(strSignedMessage);
                        showText.setTextIsSelectable(true);
                        showText.setPadding(40, 10, 40, 10);
                        showText.setTextSize(18.0f);
                        new AlertDialog.Builder(context)
                                .setTitle(R.string.app_name)
                                .setView(showText)
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        dialog.dismiss();

                                    }
                                }).show();

                    }

                }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                });

        dlg.show();

    }

    public boolean verifySignedMessage(String address, String strMessage, String strSignature) throws SignatureException {

        if(address == null || strMessage == null || strSignature == null)    {
            return false;
        }

        ECKey ecKey = signedMessageToKey(strMessage, strSignature);
        if(ecKey != null)   {
            return ecKey.toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString().equals(address);
        }
        else    {
            return false;
        }
    }

    public String signMessage(ECKey key, String strMessage) {

        if(key == null || strMessage == null || !key.hasPrivKey())    {
            return null;
        }

        return key.signMessage(strMessage);
    }

    private String signMessageArmored(ECKey key, String strMessage) {

        String sig = signMessage(key, strMessage);
        String ret = null;

        if(sig != null)    {
            ret = "-----BEGIN BITCOIN SIGNED MESSAGE-----\n";
            ret += strMessage;
            ret += "\n";
            ret += "-----BEGIN BITCOIN SIGNATURE-----\n";
            ret += "Version: Bitcoin-qt (1.0)\n";
            ret += "Address: " + key.toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString() + "\n\n";
            ret += sig;
            ret += "\n";
            ret += "-----END BITCOIN SIGNATURE-----\n";
        }

        return ret;
    }

    private ECKey signedMessageToKey(String strMessage, String strSignature) throws SignatureException {

        if(strMessage == null || strSignature == null)    {
            return null;
        }

        return ECKey.signedMessageToKey(strMessage, strSignature);
    }

}
