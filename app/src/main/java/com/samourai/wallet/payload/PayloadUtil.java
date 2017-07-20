package com.samourai.wallet.payload;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.widget.Toast;
//import android.util.Log;

import com.samourai.wallet.PinEntryActivity;
import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.SendActivity;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.crypto.AESUtil;
import com.samourai.wallet.crypto.DecryptionException;
import com.samourai.wallet.hd.HD_Account;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.ricochet.RicochetMeta;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.send.RBFUtil;
import com.samourai.wallet.util.SIMUtil;
import com.samourai.wallet.util.SendAddressUtil;
import com.samourai.wallet.JSONRPC.TrustedNodeUtil;
import com.samourai.wallet.util.TorUtil;

import org.apache.commons.codec.DecoderException;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.params.MainNetParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.security.SecureRandom;

public class PayloadUtil	{

    private final static String dataDir = "wallet";
    private final static String strFilename = "samourai.dat";
    private final static String strTmpFilename = "samourai.tmp";
    private final static String strBackupFilename = "samourai.sav";

    private final static String strOptionalBackupDir = "/samourai";
    private final static String strOptionalFilename = "samourai.txt";

    private static Context context = null;

    private static PayloadUtil instance = null;

    private PayloadUtil()	{ ; }

    public static PayloadUtil getInstance(Context ctx) {

        context = ctx;

        if (instance == null) {
            instance = new PayloadUtil();
        }

        return instance;
    }

    public File getBackupFile()  {
        String directory = Environment.DIRECTORY_DOCUMENTS;
        File dir = Environment.getExternalStoragePublicDirectory(directory + strOptionalBackupDir);
        File file = new File(dir, strOptionalFilename);

        return file;
    }

    public JSONObject putPayload(String data, boolean external)    {

        JSONObject obj = new JSONObject();

        try {
            obj.put("version", 1);
            obj.put("payload", data);
            obj.put("external", external);
        }
        catch(JSONException je) {
            return null;
        }

        return obj;
    }

    public boolean hasPayload(Context ctx) {

        File dir = ctx.getDir(dataDir, Context.MODE_PRIVATE);
        File file = new File(dir, strFilename);
        if(file.exists())    {
            return true;
        }

        return false;
    }

    public synchronized void wipe() throws IOException	{

        BIP47Util.getInstance(context).reset();
        BIP47Meta.getInstance().clear();
        APIFactory.getInstance(context).reset();

        try	{
            int nbAccounts = HD_WalletFactory.getInstance(context).get().getAccounts().size();

            for(int i = 0; i < nbAccounts; i++)	{
                HD_WalletFactory.getInstance(context).get().getAccount(i).getReceive().setAddrIdx(0);
                HD_WalletFactory.getInstance(context).get().getAccount(i).getChange().setAddrIdx(0);
                AddressFactory.getInstance().setHighestTxReceiveIdx(i, 0);
                AddressFactory.getInstance().setHighestTxChangeIdx(i, 0);
            }
            HD_WalletFactory.getInstance(context).set(null);
        }
        catch(MnemonicException.MnemonicLengthException mle)	{
            mle.printStackTrace();
        }

        File dir = context.getDir(dataDir, Context.MODE_PRIVATE);
        File datfile = new File(dir, strFilename);
        File tmpfile = new File(dir, strTmpFilename);

        if(tmpfile.exists()) {
            secureDelete(tmpfile);
        }

        if(datfile.exists()) {
            secureDelete(datfile);

            try {
                serialize(new JSONObject("{}"), new CharSequenceX(""));
            }
            catch(JSONException je) {
                je.printStackTrace();
            }
            catch(Exception e) {
                e.printStackTrace();
            }

        }

    }

    public JSONObject getPayload() {
        try {
            JSONObject wallet = new JSONObject();

            if(HD_WalletFactory.getInstance(context).get().getSeedHex() != null) {
                wallet.put("seed", HD_WalletFactory.getInstance(context).get().getSeedHex());
                wallet.put("passphrase", HD_WalletFactory.getInstance(context).get().getPassphrase());
//                obj.put("mnemonic", getMnemonic());
            }

            JSONArray accts = new JSONArray();
            for(HD_Account acct : HD_WalletFactory.getInstance(context).get().getAccounts()) {
                accts.put(acct.toJSON());
            }
            wallet.put("accounts", accts);

            //
            // can remove ???
            //
            /*
            obj.put("receiveIdx", mAccounts.get(0).getReceive().getAddrIdx());
            obj.put("changeIdx", mAccounts.get(0).getChange().getAddrIdx());
            */

            JSONObject meta = new JSONObject();
            meta.put("prev_balance", APIFactory.getInstance(context).getXpubBalance());
            meta.put("sent_tos", SendAddressUtil.getInstance().toJSON());
            meta.put("spend_type", PrefsUtil.getInstance(context).getValue(PrefsUtil.SPEND_TYPE, SendActivity.SPEND_BIP126));
            meta.put("rbf_opt_in", PrefsUtil.getInstance(context).getValue(PrefsUtil.RBF_OPT_IN, false));
            meta.put("bip47", BIP47Meta.getInstance().toJSON());
            meta.put("pin", AccessFactory.getInstance().getPIN());
            meta.put("pin2", AccessFactory.getInstance().getPIN2());
            meta.put("ricochet", RicochetMeta.getInstance(context).toJSON());
            meta.put("trusted_node", TrustedNodeUtil.getInstance().toJSON());
            meta.put("rbfs", RBFUtil.getInstance().toJSON());
            meta.put("tor", TorUtil.getInstance(context).toJSON());

            meta.put("units", PrefsUtil.getInstance(context).getValue(PrefsUtil.BTC_UNITS, 0));
            meta.put("explorer", PrefsUtil.getInstance(context).getValue(PrefsUtil.BLOCK_EXPLORER, 0));
            meta.put("trusted_no", PrefsUtil.getInstance(context).getValue(PrefsUtil.ALERT_MOBILE_NO, ""));
            meta.put("scramble_pin", PrefsUtil.getInstance(context).getValue(PrefsUtil.SCRAMBLE_PIN, false));
            meta.put("auto_backup", PrefsUtil.getInstance(context).getValue(PrefsUtil.AUTO_BACKUP, true));
            meta.put("remote", PrefsUtil.getInstance(context).getValue(PrefsUtil.ACCEPT_REMOTE, false));
            meta.put("use_trusted", PrefsUtil.getInstance(context).getValue(PrefsUtil.TRUSTED_LOCK, false));
            meta.put("check_sim", PrefsUtil.getInstance(context).getValue(PrefsUtil.CHECK_SIM, false));
            meta.put("fiat", PrefsUtil.getInstance(context).getValue(PrefsUtil.CURRENT_FIAT, "USD"));
            meta.put("fiat_sel", PrefsUtil.getInstance(context).getValue(PrefsUtil.CURRENT_FIAT_SEL, 0));
            meta.put("fx", PrefsUtil.getInstance(context).getValue(PrefsUtil.CURRENT_EXCHANGE, "LocalBitcoins.com"));
            meta.put("fx_sel", PrefsUtil.getInstance(context).getValue(PrefsUtil.CURRENT_EXCHANGE_SEL, 0));
            meta.put("use_trusted_node", PrefsUtil.getInstance(context).getValue(PrefsUtil.USE_TRUSTED_NODE, false));

            JSONObject obj = new JSONObject();
            obj.put("wallet", wallet);
            obj.put("meta", meta);

            return obj;
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }
        catch(IOException ioe) {
            throw new RuntimeException(ioe);
        }
        catch(MnemonicException.MnemonicLengthException mle) {
            throw new RuntimeException(mle);
        }
    }

    public synchronized void saveWalletToJSON(CharSequenceX password) throws MnemonicException.MnemonicLengthException, IOException, JSONException, DecryptionException, UnsupportedEncodingException {
//        Log.i("PayloadUtil", get().toJSON().toString());

        // save payload
        serialize(getPayload(), password);

        // save optional external storage backup
        // encrypted using passphrase; cannot be used for restored wallets that do not use a passphrase
        if(SamouraiWallet.getInstance().hasPassphrase(context) && isExternalStorageWritable() && PrefsUtil.getInstance(context).getValue(PrefsUtil.AUTO_BACKUP, true) && HD_WalletFactory.getInstance(context).get() != null) {

            final String passphrase = HD_WalletFactory.getInstance(context).get().getPassphrase();
            String encrypted = null;
            try {
                encrypted = AESUtil.encrypt(getPayload().toString(), new CharSequenceX(passphrase), AESUtil.DefaultPBKDF2Iterations);
                serialize(encrypted);

            }
            catch (Exception e) {
//            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            finally {
                if (encrypted == null) {
//                Toast.makeText(context, R.string.encryption_error, Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }

    }

    public synchronized HD_Wallet restoreWalletfromJSON(JSONObject obj) throws DecoderException, MnemonicException.MnemonicLengthException {

//        Log.i("PayloadUtil", obj.toString());

        HD_Wallet hdw = null;

        NetworkParameters params = MainNetParams.get();

        JSONObject wallet = null;
        JSONObject meta = null;
        try {
            if(obj.has("wallet"))    {
                wallet = obj.getJSONObject("wallet");
            }
            else    {
                wallet = obj;
            }
            if(obj.has("meta"))    {
                meta = obj.getJSONObject("meta");
            }
            else    {
                meta = obj;
            }
        }
        catch(JSONException je) {
            ;
        }

        try {
//            Log.i("PayloadUtil", obj.toString());
            if(wallet != null) {
                hdw = new HD_Wallet(context, 44, wallet, params);
                hdw.getAccount(SamouraiWallet.SAMOURAI_ACCOUNT).getReceive().setAddrIdx(wallet.has("receiveIdx") ? wallet.getInt("receiveIdx") : 0);
                hdw.getAccount(SamouraiWallet.SAMOURAI_ACCOUNT).getChange().setAddrIdx(wallet.has("changeIdx") ? wallet.getInt("changeIdx") : 0);

                if(wallet.has("accounts")) {
                    JSONArray accounts = wallet.getJSONArray("accounts");
                    //
                    // temporarily set to 2 until use of public XPUB
                    //
                    for(int i = 0; i < 2; i++) {
                        JSONObject account = accounts.getJSONObject(i);
                        hdw.getAccount(i).getReceive().setAddrIdx(account.has("receiveIdx") ? account.getInt("receiveIdx") : 0);
                        hdw.getAccount(i).getChange().setAddrIdx(account.has("changeIdx") ? account.getInt("changeIdx") : 0);

                        AddressFactory.getInstance().account2xpub().put(i, hdw.getAccount(i).xpubstr());
                        AddressFactory.getInstance().xpub2account().put(hdw.getAccount(i).xpubstr(), i);
                    }
                }
            }

            if(meta != null) {

                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();

                if(meta.has("prev_balance")) {
                    APIFactory.getInstance(context).setXpubBalance(meta.getLong("prev_balance"));
                }
                if(meta.has("spend_type")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.SPEND_TYPE, meta.getInt("spend_type"));
                    editor.putBoolean("bip126", meta.getInt("spend_type") == SendActivity.SPEND_BIP126 ? true : false);
                    editor.commit();
                }
                if(meta.has("rbf_opt_in")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.RBF_OPT_IN, meta.getBoolean("rbf_opt_in"));
                    editor.putBoolean("rbf", meta.getBoolean("rbf_opt_in") ? true : false);
                    editor.commit();
                }
                if(meta.has("sent_tos")) {
                    SendAddressUtil.getInstance().fromJSON((JSONArray) meta.get("sent_tos"));
                }
                if(meta.has("bip47")) {
                    try {
                        BIP47Meta.getInstance().fromJSON((JSONObject) meta.get("bip47"));
                    }
                    catch(ClassCastException cce) {
                        JSONArray _array = (JSONArray) meta.get("bip47");
                        JSONObject _obj = new JSONObject();
                        _obj.put("pcodes", _array);
                        BIP47Meta.getInstance().fromJSON(_obj);
                    }
                }
                if(meta.has("pin")) {
                    AccessFactory.getInstance().setPIN((String) meta.get("pin"));
                }
                if(meta.has("pin2")) {
                    AccessFactory.getInstance().setPIN2((String) meta.get("pin2"));
                }
                if(meta.has("ricochet")) {
                    RicochetMeta.getInstance(context).fromJSON((JSONObject) meta.get("ricochet"));
                }
                if(meta.has("trusted_node")) {
                    TrustedNodeUtil.getInstance().fromJSON((JSONObject) meta.get("trusted_node"));
                }
                if(meta.has("rbfs")) {
                    RBFUtil.getInstance().fromJSON((JSONArray) meta.get("rbfs"));
                }
                if(meta.has("tor")) {
                    TorUtil.getInstance(context).fromJSON((JSONObject) meta.get("tor"));
                }

                if(meta.has("units")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.BTC_UNITS, meta.getInt("units"));
                }
                if(meta.has("explorer")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.BLOCK_EXPLORER, meta.getInt("explorer"));
                }
                if(meta.has("trusted_no")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.ALERT_MOBILE_NO, (String) meta.get("trusted_no"));
                    editor.putString("alertSMSNo", meta.getString("trusted_no"));
                    editor.commit();
                }
                if(meta.has("scramble_pin")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.SCRAMBLE_PIN, meta.getBoolean("scramble_pin"));
                    editor.putBoolean("scramblePin", meta.getBoolean("scramble_pin"));
                    editor.commit();
                }
                if(meta.has("auto_backup")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.AUTO_BACKUP, meta.getBoolean("auto_backup"));
                    editor.putBoolean("auto_backup", meta.getBoolean("auto_backup"));
                    editor.commit();
                }
                if(meta.has("remote")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.ACCEPT_REMOTE, meta.getBoolean("remote"));
                    editor.putBoolean("stealthRemote", meta.getBoolean("remote"));
                    editor.commit();
                }
                if(meta.has("use_trusted")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.TRUSTED_LOCK, meta.getBoolean("use_trusted"));
                    editor.putBoolean("trustedLock", meta.getBoolean("use_trusted"));
                    editor.commit();
                }
                if(meta.has("check_sim")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.CHECK_SIM, meta.getBoolean("check_sim"));
                    editor.putBoolean("sim_switch", meta.getBoolean("check_sim"));
                    editor.commit();

                    if(meta.getBoolean("check_sim"))    {
                        SIMUtil.getInstance(context).setStoredSIM();
                    }
                }
                if (meta.has("fiat")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.CURRENT_FIAT, (String)meta.get("fiat"));
                }
                if (meta.has("fiat_sel")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.CURRENT_FIAT_SEL, meta.getInt("fiat_sel"));
                }
                if (meta.has("fx")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.CURRENT_EXCHANGE, (String)meta.get("fx"));
                }
                if(meta.has("fx_sel")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.CURRENT_EXCHANGE_SEL, meta.getInt("fx_sel"));
                }
                if(meta.has("use_trusted_node")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.USE_TRUSTED_NODE, meta.getBoolean("use_trusted_node"));
                }

                /*
                if(obj.has("passphrase")) {
                    Log.i("PayloadUtil", (String)obj.get("passphrase"));
                    Toast.makeText(context, "'" + (String)obj.get("passphrase") + "'", Toast.LENGTH_SHORT).show();
                }
                Toast.makeText(context, "'" + hdw.getPassphrase() + "'", Toast.LENGTH_SHORT).show();
                */

            }
        }
        catch(IOException ioe) {
            ioe.printStackTrace();
        }
        catch(JSONException je) {
            je.printStackTrace();
        }

        HD_WalletFactory.getInstance(context).getWallets().clear();
        HD_WalletFactory.getInstance(context).getWallets().add(hdw);

        return hdw;
    }

    public synchronized HD_Wallet restoreWalletfromJSON(CharSequenceX password) throws DecoderException, MnemonicException.MnemonicLengthException {

        JSONObject obj = null;
        try {
            obj = deserialize(password, false);
        }
        catch(IOException ioe) {
            ioe.printStackTrace();
        }
        catch(JSONException je0) {
            try {
                obj = deserialize(password, true);
            }
            catch(IOException ioe) {
                ioe.printStackTrace();
            }
            catch(JSONException je1) {
                je1.printStackTrace();
            }
        }

        return restoreWalletfromJSON(obj);
    }

    public synchronized boolean walletFileExists()  {
        File dir = context.getDir(dataDir, Context.MODE_PRIVATE);
        File walletfile = new File(dir, strFilename);
        return walletfile.exists();
    }

    private synchronized void serialize(JSONObject jsonobj, CharSequenceX password) throws IOException, JSONException, DecryptionException, UnsupportedEncodingException {

        File dir = context.getDir(dataDir, Context.MODE_PRIVATE);
        File newfile = new File(dir, strFilename);
        File tmpfile = new File(dir, strTmpFilename);
        File bakfile = new File(dir, strBackupFilename);
        newfile.setWritable(true, true);
        tmpfile.setWritable(true, true);
        bakfile.setWritable(true, true);

        // prepare tmp file.
        if(tmpfile.exists()) {
            tmpfile.delete();
//            secureDelete(tmpfile);
        }

        String data = null;
        String jsonstr = jsonobj.toString(4);
        if(password != null) {
            data = AESUtil.encrypt(jsonstr, password, AESUtil.DefaultPBKDF2Iterations);
        }
        else {
            data = jsonstr;
        }

        JSONObject jsonObj = putPayload(data, false);
        if(jsonObj != null)    {
            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpfile), "UTF-8"));
            try {
                out.write(jsonObj.toString());
            } finally {
                out.close();
            }

            copy(tmpfile, newfile);
            copy(tmpfile, bakfile);
//        secureDelete(tmpfile);
        }

        //
        // test payload
        //

    }

    private synchronized JSONObject deserialize(CharSequenceX password, boolean useBackup) throws IOException, JSONException {

        File dir = context.getDir(dataDir, Context.MODE_PRIVATE);
        File file = new File(dir, useBackup ? strBackupFilename : strFilename);
//        Log.i("PayloadUtil", "wallet file exists: " + file.exists());
        StringBuilder sb = new StringBuilder();

        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
        String str = null;

        while((str = in.readLine()) != null) {
            sb.append(str);
        }

        in.close();

        JSONObject jsonObj = null;
        try {
            jsonObj = new JSONObject(sb.toString());
        }
        catch(JSONException je)   {
            ;
        }
        String payload = null;
        if(jsonObj != null && jsonObj.has("payload"))    {
            payload = jsonObj.getString("payload");
        }

        // not a json stream, assume v0
        if(payload == null)    {
            payload = sb.toString();
        }

        JSONObject node = null;
        if(password == null) {
            node = new JSONObject(payload);
        }
        else {
            String decrypted = null;
            try {
                decrypted = AESUtil.decrypt(payload, password, AESUtil.DefaultPBKDF2Iterations);
            }
            catch(Exception e) {
                return null;
            }
            if(decrypted == null) {
                return null;
            }
            node = new JSONObject(decrypted);
        }

        return node;
    }

    private synchronized void secureDelete(File file) throws IOException {
        if (file.exists()) {
            long length = file.length();
            SecureRandom random = new SecureRandom();
            RandomAccessFile raf = new RandomAccessFile(file, "rws");
            for(int i = 0; i < 10; i++) {
                raf.seek(0);
                raf.getFilePointer();
                byte[] data = new byte[64];
                int pos = 0;
                while (pos < length) {
                    random.nextBytes(data);
                    raf.write(data);
                    pos += data.length;
                }
            }
            raf.close();
            file.delete();
        }
    }

    public synchronized void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    private boolean isExternalStorageWritable() {

        String state = Environment.getExternalStorageState();

        if(Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }

        return false;
    }

    private synchronized void serialize(String data) throws IOException    {

        String directory = Environment.DIRECTORY_DOCUMENTS;
        File dir = Environment.getExternalStoragePublicDirectory(directory + "/samourai");
        if(!dir.exists())   {
            dir.mkdirs();
            dir.setWritable(true, true);
            dir.setReadable(true, true);
        }
        File newfile = new File(dir, "samourai.txt");
        newfile.setWritable(true, true);
        newfile.setReadable(true, true);

        JSONObject jsonObj = putPayload(data, false);
        if(jsonObj != null)    {
            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newfile), "UTF-8"));
            try {
                out.write(jsonObj.toString());
            } finally {
                out.close();
            }
        }

        //
        // test payload
        //

    }

    public String getDecryptedBackupPayload(String data, CharSequenceX password)  {

        String encrypted = null;

        try {
            JSONObject jsonObj = new JSONObject(data);
            if(jsonObj != null && jsonObj.has("payload"))    {
                encrypted = jsonObj.getString("payload");
            }
            else    {
                encrypted = data;
            }
        }
        catch(JSONException je) {
            encrypted = data;
        }

        String decrypted = null;
        try {
            decrypted = AESUtil.decrypt(encrypted, password, AESUtil.DefaultPBKDF2Iterations);
        }
        catch (Exception e) {
            Toast.makeText(context, R.string.decryption_error, Toast.LENGTH_SHORT).show();
        }
        finally {
            if (decrypted == null || decrypted.length() < 1) {
                Toast.makeText(context, R.string.decryption_error, Toast.LENGTH_SHORT).show();
//                AppUtil.getInstance(context).restartApp();
            }
        }

        return decrypted;
    }

}
