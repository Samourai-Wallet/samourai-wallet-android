package com.samourai.wallet.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.EditText;
import android.widget.TextView;

import com.samourai.wallet.R;
import com.samourai.wallet.bip47.BIP47Activity;
import com.samourai.wallet.bip47.BIP47Util;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
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

    private String signMessage(ECKey key, String strMessage) {

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
            ret += "Address: " + key.toAddress(MainNetParams.get()).toString() + "\n\n";
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

    private boolean verifyMessage(ECKey key, String strMessage, String strSignature) throws SignatureException {

        if(key == null || strMessage == null || strSignature == null)    {
            return false;
        }

        return signedMessageToKey(strMessage, strSignature).getPublicKeyAsHex().equals(key.getPublicKeyAsHex());
    }

    private ECKey.ECDSASignature signMessageECDSA(ECKey key, byte[] hash) {

        if(key == null || hash == null)    {
            return null;
        }

        ECKey.ECDSASignature sig = null;

        /*
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(strMessage.getBytes());
            sig = key.sign(Sha256Hash.of(hash));
        }
        catch(NoSuchAlgorithmException nsae) {
            return null;
        }
        */

        sig = key.sign(Sha256Hash.of(hash));

        return sig;
    }

    private boolean verifyMessageECDSA(ECKey key, ECKey.ECDSASignature sig, byte[] hash) {

        if(key == null || sig == null || hash == null)    {
            return false;
        }

        return key.verify(Sha256Hash.of(hash), sig);
    }

}
