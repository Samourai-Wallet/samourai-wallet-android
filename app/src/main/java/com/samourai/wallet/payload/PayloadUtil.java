package com.samourai.wallet.payload;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.widget.Toast;
//import android.util.Log;

import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.cahoots.CahootsFactory;
import com.samourai.wallet.crypto.AESUtil;
import com.samourai.wallet.crypto.DecryptionException;
import com.samourai.wallet.hd.HD_Account;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.network.dojo.DojoUtil;
import com.samourai.wallet.ricochet.RicochetMeta;
import com.samourai.wallet.segwit.BIP49Util;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.send.BlockedUTXO;
import com.samourai.wallet.send.SendActivity;
import com.samourai.wallet.tor.TorManager;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.BatchSendUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.send.RBFUtil;
import com.samourai.wallet.util.SIMUtil;
import com.samourai.wallet.util.SendAddressUtil;
import com.samourai.wallet.JSONRPC.TrustedNodeUtil;
import com.samourai.wallet.util.SentToFromBIP47Util;
import com.samourai.wallet.util.UTXOUtil;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;

import org.apache.commons.codec.DecoderException;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.params.MainNetParams;

import org.bitcoinj.params.TestNet3Params;
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
import java.util.List;

import static com.samourai.wallet.send.SendActivity.SPEND_BOLTZMANN;

public class PayloadUtil	{

    private final static String dataDir = "wallet";
    private final static String strFilename = "samourai.dat";
    private final static String strTmpFilename = "samourai.tmp";
    private final static String strBackupFilename = "samourai.sav";

    private final static String strMultiAddrFilename = "samourai.multi";
    private final static String strUTXOFilename = "samourai.utxo";
    private final static String strFeesFilename = "samourai.fees";
    private final static String strPayNymFilename = "samourai.paynyms";
    private final static String strMultiAddrPostFilename = "samourai.multi.post";
    private final static String strUTXOPostFilename = "samourai.utxo.post";

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
        File dir = null;
        if(context.getPackageName().contains("staging"))    {
            dir = Environment.getExternalStoragePublicDirectory(directory + strOptionalBackupDir + "/staging");
        }
        else    {
            dir = Environment.getExternalStoragePublicDirectory(directory + strOptionalBackupDir);
        }
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

    public void serializeMultiAddr(JSONObject obj)  throws IOException, JSONException, DecryptionException, UnsupportedEncodingException    {
        if(!AppUtil.getInstance(context).isOfflineMode())    {
            serializeAux(obj, new CharSequenceX(AccessFactory.getInstance(context).getGUID() + AccessFactory.getInstance().getPIN()), strMultiAddrFilename);
        }
    }

    public void serializeMultiAddrPost(JSONObject obj)  throws IOException, JSONException, DecryptionException, UnsupportedEncodingException    {
        if(!AppUtil.getInstance(context).isOfflineMode())    {
            serializeAux(obj, new CharSequenceX(AccessFactory.getInstance(context).getGUID() + AccessFactory.getInstance().getPIN()), strMultiAddrPostFilename);
        }
    }

    public void serializeUTXO(List<JSONObject> objs)  throws IOException, JSONException, DecryptionException, UnsupportedEncodingException    {

        if(!AppUtil.getInstance(context).isOfflineMode())    {

            JSONArray utxos = new JSONArray();

            for(JSONObject obj : objs)   {
                if(obj != null && obj.has("unspent_outputs"))    {
                    JSONArray array = obj.getJSONArray("unspent_outputs");
                    for(int i = 0; i < array.length(); i++)   {
                        JSONObject _obj = array.getJSONObject(i);
                        utxos.put(_obj);
                    }
                }
            }

            JSONObject utxoObj = new JSONObject();
            utxoObj.put("unspent_outputs", utxos);
            serializeAux(utxoObj, new CharSequenceX(AccessFactory.getInstance(context).getGUID() + AccessFactory.getInstance().getPIN()), strUTXOFilename);
        }
    }

    public void serializeUTXOPost(JSONObject obj)  throws IOException, JSONException, DecryptionException, UnsupportedEncodingException    {

        if(!AppUtil.getInstance(context).isOfflineMode())    {

            if(obj != null) {
                JSONObject utxoObj = new JSONObject();
                utxoObj.put("unspent_outputs", obj);
                serializeAux(utxoObj, new CharSequenceX(AccessFactory.getInstance(context).getGUID() + AccessFactory.getInstance().getPIN()), strUTXOPostFilename);
            }
        }
    }

    public void serializeFees(JSONObject obj)  throws IOException, JSONException, DecryptionException, UnsupportedEncodingException    {
        if(!AppUtil.getInstance(context).isOfflineMode())    {
            serializeAux(obj, null, strFeesFilename);
        }
    }

    public void serializePayNyms(JSONObject obj)  throws IOException, JSONException, DecryptionException, UnsupportedEncodingException    {
        if(!AppUtil.getInstance(context).isOfflineMode())    {
            serializeAux(obj, new CharSequenceX(AccessFactory.getInstance(context).getGUID() + AccessFactory.getInstance().getPIN()), strPayNymFilename);
        }
    }

    public JSONObject deserializeMultiAddr()  throws IOException, JSONException {
        return deserializeAux(new CharSequenceX(AccessFactory.getInstance(context).getGUID() + AccessFactory.getInstance().getPIN()), strMultiAddrFilename);
    }

    public JSONObject deserializeUTXO()  throws IOException, JSONException  {
        return deserializeAux(new CharSequenceX(AccessFactory.getInstance(context).getGUID() + AccessFactory.getInstance().getPIN()), strUTXOFilename);
    }

    public JSONObject deserializeMultiAddrPost()  throws IOException, JSONException {
        return deserializeAux(new CharSequenceX(AccessFactory.getInstance(context).getGUID() + AccessFactory.getInstance().getPIN()), strMultiAddrPostFilename);
    }

    public JSONObject deserializeUTXOPost()  throws IOException, JSONException  {
        return deserializeAux(new CharSequenceX(AccessFactory.getInstance(context).getGUID() + AccessFactory.getInstance().getPIN()), strUTXOPostFilename);
    }

    public JSONObject deserializeFees()  throws IOException, JSONException  {
        return deserializeAux(null, strFeesFilename);
    }

    public JSONObject deserializePayNyms()  throws IOException, JSONException  {
        return deserializeAux(new CharSequenceX(AccessFactory.getInstance(context).getGUID() + AccessFactory.getInstance().getPIN()), strPayNymFilename);
    }

    public synchronized void wipe() throws IOException	{

        BIP47Util.getInstance(context).reset();
        BIP47Meta.getInstance().clear();
        BIP49Util.getInstance(context).reset();
        BIP84Util.getInstance(context).reset();
        DojoUtil.getInstance(context).clear();
        APIFactory.getInstance(context).reset();

        PrefsUtil.getInstance(context).setValue(PrefsUtil.ENABLE_TOR, false);
        PrefsUtil.getInstance(context).setValue(PrefsUtil.IS_RESTORE, false);
        PrefsUtil.getInstance(context).setValue(PrefsUtil.USE_TRUSTED_NODE, false);

        PrefsUtil.getInstance(context).clear();

        try	{
            int nbAccounts = HD_WalletFactory.getInstance(context).get().getAccounts().size();

            for(int i = 0; i < nbAccounts; i++)	{
                HD_WalletFactory.getInstance(context).get().getAccount(i).getReceive().setAddrIdx(0);
                HD_WalletFactory.getInstance(context).get().getAccount(i).getChange().setAddrIdx(0);
                AddressFactory.getInstance().setHighestTxReceiveIdx(i, 0);
                AddressFactory.getInstance().setHighestTxChangeIdx(i, 0);
            }

            AddressFactory.getInstance().setHighestBIP49ReceiveIdx(0);
            AddressFactory.getInstance().setHighestBIP49ChangeIdx(0);
            AddressFactory.getInstance().setHighestBIP84ReceiveIdx(0);
            AddressFactory.getInstance().setHighestBIP84ChangeIdx(0);
            BIP49Util.getInstance(context).getWallet().getAccount(0).getReceive().setAddrIdx(0);
            BIP49Util.getInstance(context).getWallet().getAccount(0).getChange().setAddrIdx(0);
            BIP84Util.getInstance(context).getWallet().getAccount(0).getReceive().setAddrIdx(0);
            BIP84Util.getInstance(context).getWallet().getAccount(0).getChange().setAddrIdx(0);
            AddressFactory.getInstance().setHighestPreReceiveIdx(0);
            AddressFactory.getInstance().setHighestPreChangeIdx(0);
            AddressFactory.getInstance().setHighestPostReceiveIdx(0);
            AddressFactory.getInstance().setHighestPostChangeIdx(0);

            HD_WalletFactory.getInstance(context).set(null);
        }
        catch(MnemonicException.MnemonicLengthException mle)	{
            mle.printStackTrace();
        }

        HD_WalletFactory.getInstance(context).clear();

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

            wallet.put("testnet", SamouraiWallet.getInstance().isTestNet() ? true : false);

            if(HD_WalletFactory.getInstance(context).get().getSeedHex() != null) {
                wallet.put("seed", HD_WalletFactory.getInstance(context).get().getSeedHex());
                wallet.put("passphrase", HD_WalletFactory.getInstance(context).get().getPassphrase());
//                obj.put("mnemonic", getMnemonic());
            }

            JSONArray accts = new JSONArray();
            for(HD_Account acct : HD_WalletFactory.getInstance(context).get().getAccounts()) {
                accts.put(acct.toJSON(44));
            }
            wallet.put("accounts", accts);

            //
            // export BIP47 payment codes for debug payload
            //
            try {
                wallet.put("payment_code", BIP47Util.getInstance(context).getPaymentCode().toString());
                wallet.put("payment_code_feature", BIP47Util.getInstance(context).getFeaturePaymentCode().toString());
            }
            catch(AddressFormatException afe) {
                ;
            }

            //
            // export BIP49 account for debug payload
            //
            JSONArray bip49_account = new JSONArray();
            bip49_account.put(BIP49Util.getInstance(context).getWallet().getAccount(0).toJSON(49));
            wallet.put("bip49_accounts", bip49_account);

            //
            // export BIP84 account for debug payload
            //
            JSONArray bip84_account = new JSONArray();
            bip84_account.put(BIP84Util.getInstance(context).getWallet().getAccount(0).toJSON(84));
            wallet.put("bip84_accounts", bip84_account);

            //
            // export Whirlpool accounts for debug payload
            //
            JSONArray whirlpool_account = new JSONArray();
            JSONObject preObj = BIP84Util.getInstance(context).getWallet().getAccountAt(WhirlpoolMeta.getInstance(context).getWhirlpoolPremixAccount()).toJSON(84);
            preObj.put("receiveIdx", AddressFactory.getInstance(context).getHighestPreReceiveIdx());
            preObj.put("changeIdx", AddressFactory.getInstance(context).getHighestPreChangeIdx());
            whirlpool_account.put(preObj);
            JSONObject postObj = BIP84Util.getInstance(context).getWallet().getAccountAt(WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix()).toJSON(84);
            postObj.put("receiveIdx", AddressFactory.getInstance(context).getHighestPostReceiveIdx());
            postObj.put("changeIdx", AddressFactory.getInstance(context).getHighestPostChangeIdx());
            whirlpool_account.put(postObj);
            wallet.put("whirlpool_account", whirlpool_account);

            //
            // can remove ???
            //
            /*
            obj.put("receiveIdx", mAccounts.get(0).getReceive().getAddrIdx());
            obj.put("changeIdx", mAccounts.get(0).getChange().getAddrIdx());
            */

            JSONObject meta = new JSONObject();
            meta.put("version_name", context.getText(R.string.version_name));
            meta.put("android_release", Build.VERSION.RELEASE == null ? "" : Build.VERSION.RELEASE);
            meta.put("device_manufacturer", Build.MANUFACTURER == null ? "" : Build.MANUFACTURER);
            meta.put("device_model", Build.MODEL == null ? "" : Build.MODEL);
            meta.put("device_product", Build.PRODUCT == null ? "" : Build.PRODUCT);

            meta.put("prev_balance", APIFactory.getInstance(context).getXpubBalance());
            meta.put("sent_tos", SendAddressUtil.getInstance().toJSON());
            meta.put("sent_tos_from_bip47", SentToFromBIP47Util.getInstance().toJSON());
            meta.put("batch_send", BatchSendUtil.getInstance().toJSON());
            meta.put("use_segwit", PrefsUtil.getInstance(context).getValue(PrefsUtil.USE_SEGWIT, true));
            meta.put("use_like_typed_change", PrefsUtil.getInstance(context).getValue(PrefsUtil.USE_LIKE_TYPED_CHANGE, true));
            meta.put("spend_type", PrefsUtil.getInstance(context).getValue(PrefsUtil.SPEND_TYPE, SPEND_BOLTZMANN));
            meta.put("rbf_opt_in", PrefsUtil.getInstance(context).getValue(PrefsUtil.RBF_OPT_IN, false));
            meta.put("use_ricochet", PrefsUtil.getInstance(context).getValue(PrefsUtil.USE_RICOCHET, false));
            meta.put("ricochet_staggered_delivery", PrefsUtil.getInstance(context).getValue(PrefsUtil.RICOCHET_STAGGERED, false));
            meta.put("bip47", BIP47Meta.getInstance().toJSON());
            meta.put("pin", AccessFactory.getInstance().getPIN());
            meta.put("pin2", AccessFactory.getInstance().getPIN2());
            meta.put("ricochet", RicochetMeta.getInstance(context).toJSON());
            meta.put("cahoots", CahootsFactory.getInstance().toJSON());
            meta.put("whirlpool", WhirlpoolMeta.getInstance(context).toJSON());
            meta.put("trusted_node", TrustedNodeUtil.getInstance().toJSON());
            meta.put("rbfs", RBFUtil.getInstance().toJSON());
            meta.put("tor", TorManager.getInstance(context).toJSON());
            meta.put("blocked_utxos", BlockedUTXO.getInstance().toJSON());
            meta.put("utxo_tags", UTXOUtil.getInstance().toJSON());

            meta.put("trusted_no", PrefsUtil.getInstance(context).getValue(PrefsUtil.ALERT_MOBILE_NO, ""));
            meta.put("scramble_pin", PrefsUtil.getInstance(context).getValue(PrefsUtil.SCRAMBLE_PIN, false));
            meta.put("haptic_pin", PrefsUtil.getInstance(context).getValue(PrefsUtil.HAPTIC_PIN, true));
            meta.put("auto_backup", PrefsUtil.getInstance(context).getValue(PrefsUtil.AUTO_BACKUP, true));
            meta.put("remote", PrefsUtil.getInstance(context).getValue(PrefsUtil.ACCEPT_REMOTE, false));
            meta.put("use_trusted", PrefsUtil.getInstance(context).getValue(PrefsUtil.TRUSTED_LOCK, false));
            meta.put("check_sim", PrefsUtil.getInstance(context).getValue(PrefsUtil.CHECK_SIM, false));
            meta.put("use_trusted_node", PrefsUtil.getInstance(context).getValue(PrefsUtil.USE_TRUSTED_NODE, false));
            meta.put("fee_provider_sel", PrefsUtil.getInstance(context).getValue(PrefsUtil.FEE_PROVIDER_SEL, 0));
            meta.put("broadcast_tx", PrefsUtil.getInstance(context).getValue(PrefsUtil.BROADCAST_TX, true));
            meta.put("xpubreg44", PrefsUtil.getInstance(context).getValue(PrefsUtil.XPUB44REG, false));
            meta.put("xpubreg49", PrefsUtil.getInstance(context).getValue(PrefsUtil.XPUB49REG, false));
            meta.put("xpubreg84", PrefsUtil.getInstance(context).getValue(PrefsUtil.XPUB84REG, false));
            meta.put("xpublock44", PrefsUtil.getInstance(context).getValue(PrefsUtil.XPUB44LOCK, false));
            meta.put("xpublock49", PrefsUtil.getInstance(context).getValue(PrefsUtil.XPUB49LOCK, false));
            meta.put("xpublock84", PrefsUtil.getInstance(context).getValue(PrefsUtil.XPUB84LOCK, false));
            meta.put("xpubprelock", PrefsUtil.getInstance(context).getValue(PrefsUtil.XPUBPRELOCK, false));
            meta.put("xpubpostlock", PrefsUtil.getInstance(context).getValue(PrefsUtil.XPUBPOSTLOCK, false));
            meta.put("paynym_claimed", PrefsUtil.getInstance(context).getValue(PrefsUtil.PAYNYM_CLAIMED, false));
            meta.put("paynym_refused", PrefsUtil.getInstance(context).getValue(PrefsUtil.PAYNYM_REFUSED, false));
            meta.put("paynym_featured_v1", PrefsUtil.getInstance(context).getValue(PrefsUtil.PAYNYM_FEATURED_SEGWIT, false));
            meta.put("user_offline", AppUtil.getInstance(context).isUserOfflineMode());
            if(DojoUtil.getInstance(context).getDojoParams() != null)    {
                meta.put("dojo", DojoUtil.getInstance(context).toJSON());
            }

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

    private MnemonicCode computeMnemonicCode(Context ctx) throws IOException {
        InputStream wis = ctx.getResources().getAssets().open("BIP39/en.txt");
        MnemonicCode mc = null;
        if (wis != null) {
            mc = new MnemonicCode(wis, HD_WalletFactory.BIP39_ENGLISH_SHA256);
            wis.close();
        }
        return mc;
    }

    private HD_Wallet newHDWallet(Context ctx, int purpose, JSONObject jsonobj, NetworkParameters params) throws JSONException, DecoderException, MnemonicException.MnemonicLengthException, IOException {
        byte[] seed = org.apache.commons.codec.binary.Hex.decodeHex(((String) jsonobj.get("seed")).toCharArray());
        String strPassphrase = jsonobj.getString("passphrase");
        MnemonicCode mc = computeMnemonicCode(ctx);
        return new HD_Wallet(purpose, mc, params, seed, strPassphrase, SamouraiWallet.NB_ACCOUNTS);
    }

    public synchronized HD_Wallet restoreWalletfromJSON(JSONObject obj) throws DecoderException, MnemonicException.MnemonicLengthException {

//        Log.i("PayloadUtil", obj.toString());

        HD_Wallet hdw = null;

        NetworkParameters params = SamouraiWallet.getInstance().getCurrentNetworkParams();

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

            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();

//            Log.i("PayloadUtil", obj.toString());
            if(wallet != null) {

                if(wallet.has("testnet"))    {
                    SamouraiWallet.getInstance().setCurrentNetworkParams(wallet.getBoolean("testnet") ? TestNet3Params.get() : MainNetParams.get());
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.TESTNET, wallet.getBoolean("testnet"));
                }
                else    {
                    SamouraiWallet.getInstance().setCurrentNetworkParams(MainNetParams.get());
                    PrefsUtil.getInstance(context).removeValue(PrefsUtil.TESTNET);
                }

                hdw = newHDWallet(context, 44, wallet, params);
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

                if(meta.has("prev_balance")) {
                    APIFactory.getInstance(context).setXpubBalance(meta.getLong("prev_balance"));
                }
                if(meta.has("use_segwit")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.USE_SEGWIT, meta.getBoolean("use_segwit"));
                    editor.putBoolean("segwit", meta.getBoolean("use_segwit"));
                    editor.commit();
                }
                if(meta.has("use_like_typed_change")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.USE_LIKE_TYPED_CHANGE, meta.getBoolean("use_like_typed_change"));
                    editor.putBoolean("likeTypedChange", meta.getBoolean("use_like_typed_change"));
                    editor.commit();
                }
                if(meta.has("spend_type")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.SPEND_TYPE, meta.getInt("spend_type"));
                    editor.putBoolean("boltzmann", meta.getInt("spend_type") == SendActivity.SPEND_BOLTZMANN ? true : false);
                    editor.commit();
                }
                //
                // move BIP126 over to boltzmann spend setting
                //
                if(meta.has("rbf_opt_in")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.RBF_OPT_IN, meta.getBoolean("rbf_opt_in"));
                    editor.putBoolean("rbf", meta.getBoolean("rbf_opt_in") ? true : false);
                    editor.commit();
                }
                if(meta.has("use_ricochet")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.USE_RICOCHET, meta.getBoolean("use_ricochet"));
                }
                if(meta.has("ricochet_staggered_delivery")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.RICOCHET_STAGGERED, meta.getBoolean("ricochet_staggered_delivery"));
                }
                if(meta.has("sent_tos")) {
                    SendAddressUtil.getInstance().fromJSON((JSONArray) meta.get("sent_tos"));
                }
                if(meta.has("sent_tos_from_bip47")) {
                    SentToFromBIP47Util.getInstance().fromJSON((JSONArray) meta.get("sent_tos_from_bip47"));
                }
                if(meta.has("batch_send")) {
                    BatchSendUtil.getInstance().fromJSON((JSONArray) meta.get("batch_send"));
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
                if(meta.has("cahoots")) {
                    CahootsFactory.getInstance().fromJSON((JSONArray) meta.get("cahoots"));
                }
                if(meta.has("whirlpool")) {
                    WhirlpoolMeta.getInstance(context).fromJSON((JSONObject) meta.get("whirlpool"));
                }
                if(meta.has("trusted_node")) {
                    TrustedNodeUtil.getInstance().fromJSON((JSONObject) meta.get("trusted_node"));
                }
                if(meta.has("rbfs")) {
                    RBFUtil.getInstance().fromJSON((JSONArray) meta.get("rbfs"));
                }
                if(meta.has("tor")) {
                    TorManager.getInstance(context).fromJSON((JSONObject) meta.get("tor"));
                }
                if(meta.has("blocked_utxos")) {
                    BlockedUTXO.getInstance().fromJSON((JSONObject) meta.get("blocked_utxos"));
                }
                if(meta.has("utxo_tags")) {
                    UTXOUtil.getInstance().fromJSON((JSONArray) meta.get("utxo_tags"));
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
                if(meta.has("haptic_pin")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.HAPTIC_PIN, meta.getBoolean("haptic_pin"));
                    editor.putBoolean("haptic", meta.getBoolean("haptic_pin"));
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
                if(meta.has("use_trusted_node")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.USE_TRUSTED_NODE, meta.getBoolean("use_trusted_node"));
                }
                if(meta.has("fee_provider_sel")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.FEE_PROVIDER_SEL, 0);
                }
                if(meta.has("broadcast_tx")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.BROADCAST_TX, meta.getBoolean("broadcast_tx"));
                }
                if(meta.has("xpubreg44")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.XPUB44REG, meta.getBoolean("xpubreg44"));
                }
                if(meta.has("xpubreg49")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.XPUB49REG, meta.getBoolean("xpubreg49"));
                }
                if(meta.has("xpubreg84")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.XPUB84REG, meta.getBoolean("xpubreg84"));
                }
                if(meta.has("xpublock44")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.XPUB44LOCK, meta.getBoolean("xpublock44"));
                }
                if(meta.has("xpublock49")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.XPUB49LOCK, meta.getBoolean("xpublock49"));
                }
                if(meta.has("xpublock84")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.XPUB84LOCK, meta.getBoolean("xpublock84"));
                }
                if(meta.has("xpubprelock")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.XPUBPRELOCK, meta.getBoolean("xpubprelock"));
                }
                if(meta.has("xpubpostlock")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.XPUBPOSTLOCK, meta.getBoolean("xpubpostlock"));
                }
                if(meta.has("paynym_claimed")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.PAYNYM_CLAIMED, meta.getBoolean("paynym_claimed"));
                }
                if(meta.has("paynym_refused")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.PAYNYM_REFUSED, meta.getBoolean("paynym_refused"));
                }
                if(meta.has("paynym_featured_v1")) {
                    PrefsUtil.getInstance(context).setValue(PrefsUtil.PAYNYM_FEATURED_SEGWIT, meta.getBoolean("paynym_featured_v1"));
                }
                if(meta.has("user_offline")) {
                    AppUtil.getInstance(context).setUserOfflineMode(meta.getBoolean("user_offline"));
                }
                if(meta.has("dojo")) {
                    DojoUtil.getInstance(context).fromJSON(meta.getJSONObject("dojo"));
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

        tmpfile.createNewFile();

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

    private synchronized void serializeAux(JSONObject jsonobj, CharSequenceX password, String filename) throws IOException, JSONException, DecryptionException, UnsupportedEncodingException {

        File dir = context.getDir(dataDir, Context.MODE_PRIVATE);
        File newfile = new File(dir, filename);
        newfile.setWritable(true, true);

        newfile.createNewFile();

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
            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newfile), "UTF-8"));
            try {
                out.write(jsonObj.toString());
            } finally {
                out.close();
            }
        }
    }

    private synchronized JSONObject deserializeAux(CharSequenceX password, String filename) throws IOException, JSONException {

        File dir = context.getDir(dataDir, Context.MODE_PRIVATE);
        File file = new File(dir, filename);
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
            for(int i = 0; i < 5; i++) {
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
        File dir = null;
        if(context.getPackageName().contains("staging"))    {
            dir = Environment.getExternalStoragePublicDirectory(directory + strOptionalBackupDir + "/staging");
        }
        else    {
            dir = Environment.getExternalStoragePublicDirectory(directory + strOptionalBackupDir);
        }
        if(!dir.exists())   {
            dir.mkdirs();
            dir.setWritable(true, true);
            dir.setReadable(true, true);
        }
        File newfile = new File(dir, strOptionalFilename);
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
