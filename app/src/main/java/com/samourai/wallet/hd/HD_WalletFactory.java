package com.samourai.wallet.hd;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.widget.Toast;
//import android.util.Log;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.params.MainNetParams;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.crypto.AESUtil;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.SendAddressUtil;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
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
import java.io.Writer;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//import org.apache.commons.lang.ArrayUtils;

public class HD_WalletFactory	{

    public static final String BIP39_ENGLISH_SHA256 = "ad90bf3beb7b0eb7e5acd74727dc0da96e0a280a258354e7293fb7e211ac03db";

    private static HD_WalletFactory instance = null;
    private static List<HD_Wallet> wallets = null;

    private static String dataDir = "wallet";
    private static String strFilename = "samourai.dat";
    private static String strTmpFilename = "samourai.tmp";
    private static String strBackupFilename = "samourai.sav";

    private static Context context = null;

    private HD_WalletFactory()	{ ; }

    public static HD_WalletFactory getInstance(Context ctx) {

        context = ctx;

        if (instance == null) {
            wallets = new ArrayList<HD_Wallet>();
            instance = new HD_WalletFactory();
        }

        return instance;
    }

    public HD_Wallet newWallet(int nbWords, String passphrase, int nbAccounts) throws IOException, MnemonicException.MnemonicLengthException   {

        HD_Wallet hdw = null;

        if((nbWords % 3 != 0) || (nbWords < 12 || nbWords > 24)) {
            nbWords = 12;
        }

        // len == 16 (12 words), len == 24 (18 words), len == 32 (24 words)
        int len = (nbWords / 3) * 4;

        if(passphrase == null) {
            passphrase = "";
        }

        NetworkParameters params = MainNetParams.get();

        AppUtil.getInstance(context).applyPRNGFixes();
        SecureRandom random = new SecureRandom();
        byte seed[] = new byte[len];
        random.nextBytes(seed);

        InputStream wis = context.getResources().getAssets().open("BIP39/en.txt");
        if(wis != null) {
            MnemonicCode mc = new MnemonicCode(wis, BIP39_ENGLISH_SHA256);
            hdw = new HD_Wallet(44, mc, params, seed, passphrase, nbAccounts);
            wis.close();
        }

        wallets.clear();
        wallets.add(hdw);

        return hdw;
    }

    public HD_Wallet restoreWallet(String data, String passphrase, int nbAccounts) throws AddressFormatException, IOException, DecoderException, MnemonicException.MnemonicLengthException, MnemonicException.MnemonicWordException, MnemonicException.MnemonicChecksumException  {

        HD_Wallet hdw = null;

        if(passphrase == null) {
            passphrase = "";
        }

        NetworkParameters params = MainNetParams.get();

        InputStream wis = context.getResources().getAssets().open("BIP39/en.txt");
        if(wis != null) {
            List<String> words = null;

            MnemonicCode mc = null;
            mc = new MnemonicCode(wis, BIP39_ENGLISH_SHA256);

            byte[] seed = null;
            if(data.matches(FormatsUtil.XPUB)) {
                String[] xpub = data.split(":");
                hdw = new HD_Wallet(params, xpub);
            }
            else if(data.matches(FormatsUtil.HEX) && data.length() % 4 == 0) {
                seed = Hex.decodeHex(data.toCharArray());
                hdw = new HD_Wallet(44, mc, params, seed, passphrase, nbAccounts);
            }
            else {
                data = data.replaceAll("[^a-z]+", " ");             // only use for BIP39 English
                words = Arrays.asList(data.trim().split("\\s+"));
                seed = mc.toEntropy(words);
                hdw = new HD_Wallet(44, mc, params, seed, passphrase, nbAccounts);
            }

            wis.close();

        }

        wallets.clear();
        wallets.add(hdw);

        return hdw;
    }

    public HD_Wallet get() throws IOException, MnemonicException.MnemonicLengthException {

        if(wallets == null || wallets.size() < 1) {
            return null;
        }

        return wallets.get(0);
    }

    public BIP47Wallet getBIP47() throws IOException, MnemonicException.MnemonicLengthException {

        if(wallets == null || wallets.size() < 1) {
            return null;
        }

        BIP47Wallet hdw47 = null;
        InputStream wis = context.getAssets().open("BIP39/en.txt");
        if (wis != null) {
            String seed = HD_WalletFactory.getInstance(context).get().getSeedHex();
            String passphrase = HD_WalletFactory.getInstance(context).get().getPassphrase();
            MnemonicCode mc = new MnemonicCode(wis, HD_WalletFactory.BIP39_ENGLISH_SHA256);
            hdw47 = new BIP47Wallet(47, mc, MainNetParams.get(), org.spongycastle.util.encoders.Hex.decode(seed), passphrase, 1);
        }

        return hdw47;
    }

    public void set(HD_Wallet wallet)	{

        if(wallet != null)	{
            wallets.clear();
            wallets.add(wallet);
        }

    }

    public boolean holding()	{
        return (wallets.size() > 0);
    }

    public void wipe() throws IOException	{

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

        }

    }

    public void saveWalletToJSON(CharSequenceX password) throws MnemonicException.MnemonicLengthException, IOException, JSONException {
//        Log.i("HD_WalletFactory", get().toJSON().toString());

        // save payload
        serialize(get().toJSON(context), password);

        // save optional external storage backup
        // encrypted using passphrase; cannot be used for restored wallets that do not use a passphrase
        if(SamouraiWallet.getInstance().hasPassphrase(context) && isExternalStorageWritable() && PrefsUtil.getInstance(context).getValue(PrefsUtil.AUTO_BACKUP, true)) {

            final String passphrase = HD_WalletFactory.getInstance(context).get().getPassphrase();
            String encrypted = null;
            try {
                encrypted = AESUtil.encrypt(HD_WalletFactory.getInstance(context).get().toJSON(context).toString(), new CharSequenceX(passphrase), AESUtil.DefaultPBKDF2Iterations);
                serialize(encrypted);

            } catch (Exception e) {
//            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
            } finally {
                if (encrypted == null) {
//                Toast.makeText(context, R.string.encryption_error, Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }

    }

    public HD_Wallet restoreWalletfromJSON(JSONObject obj) throws DecoderException, MnemonicException.MnemonicLengthException {

//        Log.i("HD_WalletFactory", obj.toString());

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
//            Log.i("HD_WalletFactory", obj.toString());
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

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();

                if(meta.has("prev_balance")) {
                    APIFactory.getInstance(context).setXpubBalance(meta.getLong("prev_balance"));
                }
                if(meta.has("spend_type")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.SPEND_TYPE, meta.getInt("spend_type"));
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

                /*
                if(obj.has("passphrase")) {
                    Log.i("HD_WalletFactory", (String)obj.get("passphrase"));
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

        wallets.clear();
        wallets.add(hdw);

        return hdw;
    }

    public HD_Wallet restoreWalletfromJSON(CharSequenceX password) throws DecoderException, MnemonicException.MnemonicLengthException {

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

    public boolean walletFileExists()  {
        File dir = context.getDir(dataDir, Context.MODE_PRIVATE);
        File walletfile = new File(dir, strFilename);
        return walletfile.exists();
    }

    private synchronized void serialize(JSONObject jsonobj, CharSequenceX password) throws IOException, JSONException {

        File dir = context.getDir(dataDir, Context.MODE_PRIVATE);
        File newfile = new File(dir, strFilename);
        File tmpfile = new File(dir, strTmpFilename);
        File bakfile = new File(dir, strBackupFilename);
        newfile.setWritable(true, true);
        tmpfile.setWritable(true, true);
        bakfile.setWritable(true, true);

        // serialize to byte array.
        String jsonstr = jsonobj.toString(4);
        byte[] cleartextBytes = jsonstr.getBytes(Charset.forName("UTF-8"));

        // prepare tmp file.
        if(tmpfile.exists()) {
            tmpfile.delete();
//            secureDelete(tmpfile);
        }

        String data = null;
        if(password != null) {
            data = AESUtil.encrypt(jsonstr, password, AESUtil.DefaultPBKDF2Iterations);
        }
        else {
            data = jsonstr;
        }

        Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpfile), "UTF-8"));
        try {
            out.write(data);
        } finally {
            out.close();
        }

        copy(tmpfile, newfile);
        copy(tmpfile, bakfile);
//        secureDelete(tmpfile);

    }

    private synchronized JSONObject deserialize(CharSequenceX password, boolean useBackup) throws IOException, JSONException {

        File dir = context.getDir(dataDir, Context.MODE_PRIVATE);
        File file = new File(dir, useBackup ? strBackupFilename : strFilename);
//        Log.i("HD_WalletFactory", "wallet file exists: " + file.exists());
        StringBuilder sb = new StringBuilder();

        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
        String str = null;

        while((str = in.readLine()) != null) {
            sb.append(str);
        }

        in.close();

        JSONObject node = null;
        if(password == null) {
            node = new JSONObject(sb.toString());
        }
        else {
            String decrypted = null;
            try {
                decrypted = AESUtil.decrypt(sb.toString(), password, AESUtil.DefaultPBKDF2Iterations);
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
            for(int i = 0; i < 100; i++) {
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

        String directory = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? Environment.DIRECTORY_DOCUMENTS : Environment.DIRECTORY_DOWNLOADS;
        File dir = Environment.getExternalStoragePublicDirectory(directory + "/samourai");
        if(!dir.exists())   {
            dir.mkdirs();
            dir.setWritable(true, true);
            dir.setReadable(true, true);
        }
        File newfile = new File(dir, "samourai.txt");
        newfile.setWritable(true, true);
        newfile.setReadable(true, true);

        Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newfile), "UTF-8"));
        try {
            out.write(data);
        } finally {
            out.close();
        }

    }

}
