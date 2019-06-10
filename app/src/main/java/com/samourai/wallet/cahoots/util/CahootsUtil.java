package com.samourai.wallet.cahoots.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Looper;
import android.support.v4.content.FileProvider;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.wallet.cahoots.STONEWALLx2;
import com.samourai.wallet.cahoots.Stowaway;
import com.samourai.wallet.cahoots._TransactionOutPoint;
import com.samourai.wallet.cahoots._TransactionOutput;
import com.samourai.wallet.cahoots.psbt.PSBT;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.segwit.bech32.Bech32Segwit;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.PushTx;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.FormatsUtil;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class CahootsUtil {

    private static Context context = null;

    private static CahootsUtil instance = null;

    private CahootsUtil()    { ; }

    public static CahootsUtil getInstance(Context ctx) {

        context = ctx;

        if(instance == null)    {
            instance = new CahootsUtil();
        }

        return instance;
    }

    public void processCahoots(String strCahoots)    {

        Stowaway stowaway = null;
        STONEWALLx2 stonewall = null;

        try {
            JSONObject obj = new JSONObject(strCahoots);
            Log.d("CahootsUtil", "incoming st:" + strCahoots);
            Log.d("CahootsUtil", "object json:" + obj.toString());
            if(obj.has("cahoots") && obj.getJSONObject("cahoots").has("type"))    {

                int type = obj.getJSONObject("cahoots").getInt("type");
                switch(type)    {
                    case Cahoots.CAHOOTS_STOWAWAY:
                        stowaway = new Stowaway(obj);
                        Log.d("CahootsUtil", "stowaway st:" + stowaway.toJSON().toString());
                        break;
                    case Cahoots.CAHOOTS_STONEWALLx2:
                        stonewall = new STONEWALLx2(obj);
                        Log.d("CahootsUtil", "stonewall st:" + stonewall.toJSON().toString());
                        break;
                    default:
                        Toast.makeText(context, R.string.unrecognized_cahoots, Toast.LENGTH_SHORT).show();
                        return;
                }

            }
            else    {
                Toast.makeText(context, R.string.not_cahoots, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        catch(JSONException je) {
            Toast.makeText(context, R.string.cannot_process_cahoots, Toast.LENGTH_SHORT).show();
            return;
        }

        if(stowaway != null)    {

            int step = stowaway.getStep();

            try {
                switch(step)    {
                    case 0:
                        Log.d("CahootsUtil", "calling doStowaway1");
                        doStowaway1(stowaway);
                        break;
                    case 1:
                        doStowaway2(stowaway);
                        break;
                    case 2:
                        doStowaway3(stowaway);
                        break;
                    case 3:
                        doStowaway4(stowaway);
                        break;
                    default:
                        Toast.makeText(context, R.string.unrecognized_step, Toast.LENGTH_SHORT).show();
                        break;
                }
            }
            catch(Exception e) {
                Toast.makeText(context, R.string.cannot_process_stonewall, Toast.LENGTH_SHORT).show();
                Log.d("CahootsUtil", e.getMessage());
                e.printStackTrace();
            }

            return;

        }
        else if(stonewall != null)    {

            int step = stonewall.getStep();

            try {
                switch(step)    {
                    case 0:
                        doSTONEWALLx2_1(stonewall);
                        break;
                    case 1:
                        doSTONEWALLx2_2(stonewall);
                        break;
                    case 2:
                        doSTONEWALLx2_3(stonewall);
                        break;
                    case 3:
                        doSTONEWALLx2_4(stonewall);
                        break;
                    default:
                        Toast.makeText(context, R.string.unrecognized_step, Toast.LENGTH_SHORT).show();
                        break;
                }
            }
            catch(Exception e) {
                Toast.makeText(context, R.string.cannot_process_stowaway, Toast.LENGTH_SHORT).show();
                Log.d("CahootsUtil", e.getMessage());
                e.printStackTrace();
            }

            return;

        }
        else    {
            Toast.makeText(context, "error processing #Cahoots", Toast.LENGTH_SHORT).show();
        }

    }

    private void doCahoots(final String strCahoots) {

        Cahoots cahoots = null;
        Transaction transaction = null;
        int step = 0;
        try {
            JSONObject jsonObject = new JSONObject(strCahoots);
            if(jsonObject != null && jsonObject.has("cahoots") && jsonObject.getJSONObject("cahoots").has("step"))    {
                step = jsonObject.getJSONObject("cahoots").getInt("step");
                if(step == 4 || step == 3) {
                    cahoots = new Stowaway(jsonObject);
                    transaction = cahoots.getPSBT().getTransaction();
                }
            }
        }
        catch(JSONException je) {
            Toast.makeText(context, je.getMessage(), Toast.LENGTH_SHORT).show();
        }

        final int _step = step;
        final Transaction _transaction = transaction;

        final int QR_ALPHANUM_CHAR_LIMIT = 4296;    // tx max size in bytes == 2148

        TextView showTx = new TextView(context);
        showTx.setText(step != 4 ? strCahoots : Hex.toHexString(transaction.bitcoinSerialize()));
        showTx.setTextIsSelectable(true);
        showTx.setPadding(40, 10, 40, 10);
        showTx.setTextSize(18.0f);

        LinearLayout hexLayout = new LinearLayout(context);
        hexLayout.setOrientation(LinearLayout.VERTICAL);
        hexLayout.addView(showTx);

        String title = context.getString(R.string.cahoots);
        title += ", ";
        title += (_step + 1);
        title += "/5";

        AlertDialog.Builder dlg = new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(hexLayout)
                .setCancelable(true)
                .setPositiveButton(R.string.copy_to_clipboard, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager)context.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip = null;
                        clip = android.content.ClipData.newPlainText("Cahoots", _step != 4 ? strCahoots : Hex.toHexString(_transaction.bitcoinSerialize()));
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();

                    }
                })
                .setNegativeButton(R.string.show_qr, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        if(strCahoots.length() <= QR_ALPHANUM_CHAR_LIMIT)    {

                            final ImageView ivQR = new ImageView(context);

                            Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
                            Point size = new Point();
                            display.getSize(size);
                            int imgWidth = Math.max(size.x - 240, 150);

                            Bitmap bitmap = null;

                            QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(strCahoots, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), imgWidth);

                            try {
                                bitmap = qrCodeEncoder.encodeAsBitmap();
                            } catch (WriterException e) {
                                e.printStackTrace();
                            }

                            ivQR.setImageBitmap(bitmap);

                            LinearLayout qrLayout = new LinearLayout(context);
                            qrLayout.setOrientation(LinearLayout.VERTICAL);
                            qrLayout.addView(ivQR);

                            new AlertDialog.Builder(context)
                                    .setTitle(R.string.cahoots)
                                    .setView(qrLayout)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            dialog.dismiss();

                                        }
                                    })
                                    .setNegativeButton(R.string.share_qr, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            String strFileName = AppUtil.getInstance(context).getReceiveQRFilename();
                                            File file = new File(strFileName);
                                            if(!file.exists()) {
                                                try {
                                                    file.createNewFile();
                                                }
                                                catch(Exception e) {
                                                    Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                            file.setReadable(true, false);

                                            FileOutputStream fos = null;
                                            try {
                                                fos = new FileOutputStream(file);
                                            }
                                            catch(FileNotFoundException fnfe) {
                                                ;
                                            }

                                            if(file != null && fos != null) {
                                                Bitmap bitmap = ((BitmapDrawable)ivQR.getDrawable()).getBitmap();
                                                bitmap.compress(Bitmap.CompressFormat.PNG, 0, fos);

                                                try {
                                                    fos.close();
                                                }
                                                catch(IOException ioe) {
                                                    ;
                                                }

                                                Intent intent = new Intent();
                                                intent.setAction(Intent.ACTION_SEND);
                                                intent.setType("image/png");
                                                if (android.os.Build.VERSION.SDK_INT >= 24) {
                                                    //From API 24 sending FIle on intent ,require custom file provider
                                                    intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                                                            context,
                                                            context.getApplicationContext()
                                                                    .getPackageName() + ".provider", file));
                                                } else {
                                                    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                                                }
                                                context.startActivity(Intent.createChooser(intent, context.getText(R.string.send_tx)));
                                            }

                                        }
                                    }).show();
                        }
                        else    {

                            Toast.makeText(context, R.string.tx_too_large_qr, Toast.LENGTH_SHORT).show();

                        }

                    }
                });

        if(_step == 4)    {
            dlg.setPositiveButton(R.string.broadcast, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    dialog.dismiss();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Looper.prepare();

                            PushTx.getInstance(context).pushTx(Hex.toHexString(_transaction.bitcoinSerialize()));

                            Looper.loop();

                        }
                    }).start();

                }
            });
            dlg.setNeutralButton(R.string.show_tx, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    String tx = "";
                    if(_transaction != null) {
                        tx = _transaction.toString();
                    }

                    TextView showText = new TextView(context);
                    showText.setText(tx);
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
            });
        }
        else if(_step == 3)   {
            dlg.setNeutralButton(R.string.show_tx, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    String tx = "";
                    if(_transaction != null) {
                        tx = _transaction.toString();
                    }

                    TextView showText = new TextView(context);
                    showText.setText(tx);
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
            });
        }
        else    {
            ;
        }

        if(!((Activity)context).isFinishing())    {
            dlg.show();
        }

    }

    public void doPSBT(final String strPSBT)    {

        String msg = null;
        PSBT psbt = new PSBT(strPSBT, SamouraiWallet.getInstance().getCurrentNetworkParams());
        try {
            psbt.read();
            msg = psbt.dump();
        }
        catch(Exception e) {
            msg = e.getMessage();
        }

        final EditText edPSBT = new EditText(context);
        edPSBT.setSingleLine(false);
        edPSBT.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        edPSBT.setLines(10);
        edPSBT.setHint(R.string.PSBT);
        edPSBT.setGravity(Gravity.START);
        TextWatcher textWatcher = new TextWatcher() {

            public void afterTextChanged(Editable s) {
                edPSBT.setSelection(0);
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }
        };
        edPSBT.addTextChangedListener(textWatcher);
        edPSBT.setText(msg);

        AlertDialog.Builder dlg = new AlertDialog.Builder(context)
                .setTitle(R.string.app_name)
                .setMessage(R.string.PSBT)
                .setView(edPSBT)
                .setCancelable(true)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                    }

                });
        if(!((Activity)context).isFinishing())    {
            dlg.show();
        }

    }

    public void doStowaway0(long spendAmount)    {
        // Bob -> Alice, spendAmount in sats
        NetworkParameters params = SamouraiWallet.getInstance().getCurrentNetworkParams();

        //
        //
        // step0: B sends spend amount to A,  creates step0
        //
        //
        Stowaway stowaway0 = new Stowaway(spendAmount, params);
        System.out.println(stowaway0.toJSON().toString());

        doCahoots(stowaway0.toJSON().toString());
    }

    private void doStowaway1(Stowaway stowaway0) throws Exception    {

        List<UTXO> utxos = getCahootsUTXO();
        // sort in descending order by value
        Collections.sort(utxos, new UTXO.UTXOComparator());

        Log.d("CahootsUtil", "BIP84 utxos:" + utxos.size());

        List<UTXO> selectedUTXO = new ArrayList<UTXO>();
        long totalContributedAmount = 0L;
        for(UTXO utxo : utxos)   {
            selectedUTXO.add(utxo);
            totalContributedAmount += utxo.getValue();
            Log.d("CahootsUtil", "BIP84 selected utxo:" + utxo.getValue());
            if(totalContributedAmount > stowaway0.getSpendAmount() + SamouraiWallet.bDust.longValue())    {
                break;
            }
        }
        if(!(totalContributedAmount > stowaway0.getSpendAmount() + SamouraiWallet.bDust.longValue()))    {
            Toast.makeText(context, R.string.cannot_compose_cahoots, Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("CahootsUtil", "BIP84 selected utxos:" + selectedUTXO.size());

        NetworkParameters params = stowaway0.getParams();

        //
        //
        // step1: A utxos -> B (take largest that cover amount)
        // A provides 5750000
        //
        //

        String zpub = BIP84Util.getInstance(context).getWallet().getAccount(0).zpubstr();
        HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>> inputsA = new HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>>();
//        inputsA.put(outpoint_A0, Triple.of(Hex.decode("0221b719bc26fb49971c7dd328a6c7e4d17dfbf4e2217bee33a65c53ed3daf041e"), FormatsUtil.getInstance().getFingerprintFromXPUB("vpub5Z3hqXewnCbxnMsWygxN8AyjNVJbFeV2VCdDKpTFazdoC29qK4Y5DSQ1aaAPBrsBZ1TzN5va6xGy4eWa9uAqh2AwifuA1eofkedh3eUgF6b"), "M/0/4"));
//        inputsA.put(outpoint_A1, Triple.of(Hex.decode("020ab261e1a3cf986ecb3cd02299de36295e804fd799934dc5c99dde0d25e71b93"), FormatsUtil.getInstance().getFingerprintFromXPUB("vpub5Z3hqXewnCbxnMsWygxN8AyjNVJbFeV2VCdDKpTFazdoC29qK4Y5DSQ1aaAPBrsBZ1TzN5va6xGy4eWa9uAqh2AwifuA1eofkedh3eUgF6b"), "M/0/2"));

        for(UTXO utxo : selectedUTXO)  {
            for(MyTransactionOutPoint outpoint : utxo.getOutpoints())   {
                _TransactionOutPoint _outpoint = new _TransactionOutPoint(outpoint);

                ECKey eckey = SendFactory.getPrivKey(_outpoint.getAddress());
                String path = APIFactory.getInstance(context).getUnspentPaths().get(_outpoint.getAddress());
                inputsA.put(_outpoint, Triple.of(eckey.getPubKey(), FormatsUtil.getInstance().getFingerprintFromXPUB(zpub), path));
            }
        }

        // destination output
        int idx = BIP84Util.getInstance(context).getWallet().getAccount(0).getReceive().getAddrIdx();
        SegwitAddress segwitAddress = BIP84Util.getInstance(context).getAddressAt(0, idx);
        HashMap<_TransactionOutput, Triple<byte[], byte[], String>> outputsA = new HashMap<_TransactionOutput, Triple<byte[], byte[], String>>();
//        byte[] scriptPubKey_A = getScriptPubKey("tb1qewwlc2dksuez3zauf38d82m7uqd4ewkf2avdl8", params);
        Pair<Byte, byte[]> pair = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress.getBech32AsString());
        byte[] scriptPubKey_A = Bech32Segwit.getScriptPubkey(pair.getLeft(), pair.getRight());
        _TransactionOutput output_A0 = new _TransactionOutput(params, null, Coin.valueOf(stowaway0.getSpendAmount()), scriptPubKey_A);
        outputsA.put(output_A0, Triple.of(segwitAddress.getECKey().getPubKey(), FormatsUtil.getInstance().getFingerprintFromXPUB(BIP84Util.getInstance(context).getWallet().getAccount(0).zpubstr()), "M/0/" + idx));

        Stowaway stowaway1 = new Stowaway(stowaway0);
        stowaway1.inc(inputsA, outputsA, null);

        doCahoots(stowaway1.toJSON().toString());
    }

    private void doStowaway2(Stowaway stowaway1) throws Exception    {

        Transaction transaction = stowaway1.getTransaction();
        Log.d("CahootsUtil", "step2 tx:" + org.spongycastle.util.encoders.Hex.toHexString(transaction.bitcoinSerialize()));
//        Log.d("CahootsUtil", "input value:" + transaction.getInputs().get(0).getValue().longValue());
        int nbIncomingInputs = transaction.getInputs().size();

        List<UTXO> utxos = getCahootsUTXO();
        // sort in ascending order by value
        Collections.sort(utxos, new UTXO.UTXOComparator());
        Collections.reverse(utxos);

        Log.d("CahootsUtil", "BIP84 utxos:" + utxos.size());

        List<UTXO> selectedUTXO = new ArrayList<UTXO>();
        int nbTotalSelectedOutPoints = 0;
        long totalSelectedAmount = 0L;
        for(UTXO utxo : utxos)   {
            selectedUTXO.add(utxo);
            totalSelectedAmount += utxo.getValue();
            Log.d("BIP84 selected utxo:", "" + utxo.getValue());
            nbTotalSelectedOutPoints += utxo.getOutpoints().size();
            if(totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue() + stowaway1.getSpendAmount() + SamouraiWallet.bDust.longValue())    {

                // discard "extra" utxo, if any
                List<UTXO> _selectedUTXO = new ArrayList<UTXO>();
                Collections.reverse(selectedUTXO);
                int _nbTotalSelectedOutPoints = 0;
                long _totalSelectedAmount = 0L;
                for(UTXO utxoSel : selectedUTXO)   {
                    _selectedUTXO.add(utxoSel);
                    _totalSelectedAmount += utxoSel.getValue();
                    Log.d("CahootsUtil", "BIP84 post selected utxo:" + utxoSel.getValue());
                    _nbTotalSelectedOutPoints += utxoSel.getOutpoints().size();
                    if(_totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, _nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue() + stowaway1.getSpendAmount() + SamouraiWallet.bDust.longValue())    {
                        selectedUTXO.clear();
                        selectedUTXO.addAll(_selectedUTXO);
                        totalSelectedAmount = _totalSelectedAmount;
                        nbTotalSelectedOutPoints = _nbTotalSelectedOutPoints;
                        break;
                    }
                }

                break;
            }
        }
        if(!(totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue() + stowaway1.getSpendAmount() + SamouraiWallet.bDust.longValue()))    {
            Toast.makeText(context, R.string.cannot_compose_cahoots, Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("CahootsUtil", "BIP84 selected utxos:" + selectedUTXO.size());

        long fee = FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue();
        Log.d("CahootsUtil", "fee:" + fee);

        NetworkParameters params = stowaway1.getParams();

        //
        //
        // step2: B verif, utxos -> A (take smallest that cover amount)
        // B provides 1000000, 1500000 (250000 change, A input larger)
        //
        //

        String zpub = BIP84Util.getInstance(context).getWallet().getAccount(0).zpubstr();
        HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>> inputsB = new HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>>();

        for(UTXO utxo : selectedUTXO)  {
            for(MyTransactionOutPoint outpoint : utxo.getOutpoints())   {
                _TransactionOutPoint _outpoint = new _TransactionOutPoint(outpoint);

                ECKey eckey = SendFactory.getPrivKey(_outpoint.getAddress());
                String path = APIFactory.getInstance(context).getUnspentPaths().get(_outpoint.getAddress());
                inputsB.put(_outpoint, Triple.of(eckey.getPubKey(), FormatsUtil.getInstance().getFingerprintFromXPUB(zpub), path));
            }
        }

        Log.d("CahootsUtil", "inputsB:" + inputsB.size());

        // change output
        int idx = BIP84Util.getInstance(context).getWallet().getAccount(0).getChange().getAddrIdx();
        SegwitAddress segwitAddress = BIP84Util.getInstance(context).getAddressAt(1, idx);
        HashMap<_TransactionOutput, Triple<byte[], byte[], String>> outputsB = new HashMap<_TransactionOutput, Triple<byte[], byte[], String>>();
        Pair<Byte, byte[]> pair = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress.getBech32AsString());
        byte[] scriptPubKey_B = Bech32Segwit.getScriptPubkey(pair.getLeft(), pair.getRight());
        _TransactionOutput output_B0 = new _TransactionOutput(params, null, Coin.valueOf((totalSelectedAmount - stowaway1.getSpendAmount()) - fee), scriptPubKey_B);
        outputsB.put(output_B0, Triple.of(segwitAddress.getECKey().getPubKey(), FormatsUtil.getInstance().getFingerprintFromXPUB(BIP84Util.getInstance(context).getWallet().getAccount(0).zpubstr()), "M/1/" + idx));

        Log.d("CahootsUtil", "outputsB:" + outputsB.size());

        Stowaway stowaway2 = new Stowaway(stowaway1);
        stowaway2.inc(inputsB, outputsB, null);
        System.out.println("step 2:" + stowaway2.toJSON().toString());

        doCahoots(stowaway2.toJSON().toString());
    }

    private void doStowaway3(Stowaway stowaway2) throws Exception    {

        HashMap<String,String> utxo2Address = new HashMap<String,String>();
        List<UTXO> utxos = APIFactory.getInstance(context).getUtxos(true);
        for(UTXO utxo : utxos)   {
            for(MyTransactionOutPoint outpoint : utxo.getOutpoints())   {
                utxo2Address.put(outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN(), outpoint.getAddress());
                Log.d("CahootsUtil", "outpoint address:" + outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN() + "," + outpoint.getAddress());
            }
        }

        Transaction transaction = stowaway2.getPSBT().getTransaction();
        HashMap<String,ECKey> keyBag_A = new HashMap<String,ECKey>();
        for(TransactionInput input : transaction.getInputs())   {
            TransactionOutPoint outpoint = input.getOutpoint();
            String key = outpoint.getHash().toString() + "-" + outpoint.getIndex();
            if(utxo2Address.containsKey(key))    {
                String address = utxo2Address.get(key);
                ECKey eckey = SendFactory.getPrivKey(address);
                keyBag_A.put(outpoint.toString(), eckey);
            }
        }

        //
        //
        // step3: A verif, BIP69, sig
        //
        //
        /*
        HashMap<String,ECKey> keyBag_A = new HashMap<String,ECKey>();
        keyBag_A.put(outpoint_A0.toString(), ecKey_A0);
        keyBag_A.put(outpoint_A1.toString(), ecKey_A1);
        */

        Stowaway stowaway3 = new Stowaway(stowaway2);
        stowaway3.inc(null, null, keyBag_A);
        System.out.println("step 3:" + stowaway3.toJSON().toString());

        doCahoots(stowaway3.toJSON().toString());
    }

    private void doStowaway4(Stowaway stowaway3) throws Exception    {

        HashMap<String,String> utxo2Address = new HashMap<String,String>();
        List<UTXO> utxos = APIFactory.getInstance(context).getUtxos(true);
        for(UTXO utxo : utxos)   {
            for(MyTransactionOutPoint outpoint : utxo.getOutpoints())   {
                utxo2Address.put(outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN(), outpoint.getAddress());
                Log.d("CahootsUtil", "outpoint address:" + outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN() + "," + outpoint.getAddress());
            }
        }

        Transaction transaction = stowaway3.getPSBT().getTransaction();
        HashMap<String,ECKey> keyBag_B = new HashMap<String,ECKey>();
        for(TransactionInput input : transaction.getInputs())   {
            TransactionOutPoint outpoint = input.getOutpoint();
            String key = outpoint.getHash().toString() + "-" + outpoint.getIndex();
            if(utxo2Address.containsKey(key))    {
                String address = utxo2Address.get(key);
                ECKey eckey = SendFactory.getPrivKey(address);
                keyBag_B.put(outpoint.toString(), eckey);
            }
        }

        //
        //
        // step4: B verif, sig, broadcast
        //
        //

        /*
        HashMap<String,ECKey> keyBag_B = new HashMap<String,ECKey>();
        keyBag_B.put(outpoint_B0.toString(), ecKey_B0);
        keyBag_B.put(outpoint_B1.toString(), ecKey_B1);
        */

        Stowaway stowaway4 = new Stowaway(stowaway3);
        stowaway4.inc(null, null, keyBag_B);
        System.out.println("step 4:" + stowaway4.toJSON().toString());
        System.out.println("step 4 psbt:" + stowaway4.getPSBT().toString());
        System.out.println("step 4 tx:" + stowaway4.getTransaction().toString());
        System.out.println("step 4 tx hex:" + Hex.toHexString(stowaway4.getTransaction().bitcoinSerialize()));

//        Stowaway s = new Stowaway(stowaway4.toJSON());
//        System.out.println(s.toJSON().toString());

        // broadcast ???
        doCahoots(stowaway4.toJSON().toString());
    }

    public void doSTONEWALLx2_0(long spendAmount, String address)    {
        // Bob -> Alice, spendAmount in sats
        NetworkParameters params = SamouraiWallet.getInstance().getCurrentNetworkParams();

        //
        //
        // step0: B sends spend amount to A,  creates step0
        //
        //
        STONEWALLx2 stonewall0 = new STONEWALLx2(spendAmount, address, params);
        System.out.println(stonewall0.toJSON().toString());

        doCahoots(stonewall0.toJSON().toString());
    }

    private void doSTONEWALLx2_1(STONEWALLx2 stonewall0) throws Exception    {

        List<UTXO> utxos = getCahootsUTXO();
        Collections.shuffle(utxos);

        Log.d("CahootsUtil", "BIP84 utxos:" + utxos.size());

        List<String> seenTxs = new ArrayList<String>();
        List<UTXO> selectedUTXO = new ArrayList<UTXO>();
        long totalContributedAmount = 0L;
        for(UTXO utxo : utxos)   {

            UTXO _utxo = new UTXO();
            for(MyTransactionOutPoint outpoint : utxo.getOutpoints())   {
                if(!seenTxs.contains(outpoint.getTxHash().toString()))    {
                    _utxo.getOutpoints().add(outpoint);
                    seenTxs.add(outpoint.getTxHash().toString());
                }
            }

            if(_utxo.getOutpoints().size() > 0)    {
                selectedUTXO.add(_utxo);
                totalContributedAmount += _utxo.getValue();
                Log.d("CahootsUtil", "BIP84 selected utxo:" + _utxo.getValue());
            }

            if(totalContributedAmount > stonewall0.getSpendAmount() + SamouraiWallet.bDust.longValue())    {
                break;
            }
        }
        if(!(totalContributedAmount > stonewall0.getSpendAmount() + SamouraiWallet.bDust.longValue()))    {
            Toast.makeText(context, R.string.cannot_compose_cahoots, Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("CahootsUtil", "BIP84 selected utxos:" + selectedUTXO.size());

        NetworkParameters params = stonewall0.getParams();

        //
        //
        // step1: A utxos -> B (take largest that cover amount)
        // A provides 5750000
        //
        //

        String zpub = BIP84Util.getInstance(context).getWallet().getAccount(0).zpubstr();
        HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>> inputsA = new HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>>();

        for(UTXO utxo : selectedUTXO)  {
            for(MyTransactionOutPoint outpoint : utxo.getOutpoints())   {
                _TransactionOutPoint _outpoint = new _TransactionOutPoint(outpoint);

                ECKey eckey = SendFactory.getPrivKey(_outpoint.getAddress());
                String path = APIFactory.getInstance(context).getUnspentPaths().get(_outpoint.getAddress());
                inputsA.put(_outpoint, Triple.of(eckey.getPubKey(), FormatsUtil.getInstance().getFingerprintFromXPUB(zpub), path));
            }
        }

        // contributor mix output
        int idx = BIP84Util.getInstance(context).getWallet().getAccount(0).getReceive().getAddrIdx();
        SegwitAddress segwitAddress0 = BIP84Util.getInstance(context).getAddressAt(0, idx);
        if(segwitAddress0.getBech32AsString().equalsIgnoreCase(stonewall0.getDestination()))    {
            segwitAddress0 = BIP84Util.getInstance(context).getAddressAt(0, idx + 1);
        }
        HashMap<_TransactionOutput, Triple<byte[], byte[], String>> outputsA = new HashMap<_TransactionOutput, Triple<byte[], byte[], String>>();
        Pair<Byte, byte[]> pair0 = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress0.getBech32AsString());
        byte[] scriptPubKey_A0 = Bech32Segwit.getScriptPubkey(pair0.getLeft(), pair0.getRight());
        _TransactionOutput output_A0 = new _TransactionOutput(params, null, Coin.valueOf(stonewall0.getSpendAmount()), scriptPubKey_A0);
        outputsA.put(output_A0, Triple.of(segwitAddress0.getECKey().getPubKey(), FormatsUtil.getInstance().getFingerprintFromXPUB(BIP84Util.getInstance(context).getWallet().getAccount(0).zpubstr()), "M/0/" + idx));

        // contributor change output
        idx = BIP84Util.getInstance(context).getWallet().getAccount(0).getChange().getAddrIdx();
        SegwitAddress segwitAddress1 = BIP84Util.getInstance(context).getAddressAt(1, idx);
        Pair<Byte, byte[]> pair1 = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress1.getBech32AsString());
        byte[] scriptPubKey_A1 = Bech32Segwit.getScriptPubkey(pair1.getLeft(), pair1.getRight());
        _TransactionOutput output_A1 = new _TransactionOutput(params, null, Coin.valueOf(totalContributedAmount - stonewall0.getSpendAmount()), scriptPubKey_A1);
        outputsA.put(output_A1, Triple.of(segwitAddress1.getECKey().getPubKey(), FormatsUtil.getInstance().getFingerprintFromXPUB(BIP84Util.getInstance(context).getWallet().getAccount(0).zpubstr()), "M/1/" + idx));

        STONEWALLx2 stonewall1 = new STONEWALLx2(stonewall0);
        stonewall1.inc(inputsA, outputsA, null);

        doCahoots(stonewall1.toJSON().toString());
    }

    private void doSTONEWALLx2_2(STONEWALLx2 stonewall1) throws Exception    {

        Transaction transaction = stonewall1.getTransaction();
        Log.d("CahootsUtil", "step2 tx:" + org.spongycastle.util.encoders.Hex.toHexString(transaction.bitcoinSerialize()));
//        Log.d("CahootsUtil", "input value:" + transaction.getInputs().get(0).getValue().longValue());
        int nbIncomingInputs = transaction.getInputs().size();

        List<UTXO> utxos = getCahootsUTXO();
        Collections.shuffle(utxos);

        Log.d("CahootsUtil", "BIP84 utxos:" + utxos.size());

        List<String> seenTxs = new ArrayList<String>();
        for(TransactionInput input : transaction.getInputs())   {
            if(!seenTxs.contains(input.getOutpoint().getHash().toString()))    {
                seenTxs.add(input.getOutpoint().getHash().toString());
            }
        }

        List<UTXO> selectedUTXO = new ArrayList<UTXO>();
        int nbTotalSelectedOutPoints = 0;
        long totalSelectedAmount = 0L;
        for(UTXO utxo : utxos)   {

            UTXO _utxo = new UTXO();
            for(MyTransactionOutPoint outpoint : utxo.getOutpoints())   {
                if(!seenTxs.contains(outpoint.getTxHash().toString()))    {
                    _utxo.getOutpoints().add(outpoint);
                    seenTxs.add(outpoint.getTxHash().toString());
                }
            }

            if(_utxo.getOutpoints().size() > 0)    {
                selectedUTXO.add(_utxo);
                totalSelectedAmount += _utxo.getValue();
                nbTotalSelectedOutPoints += _utxo.getOutpoints().size();
                Log.d("CahootsUtil", "BIP84 selected utxo:" + _utxo.getValue());
            }

            if(totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 4).longValue() + stonewall1.getSpendAmount() + SamouraiWallet.bDust.longValue())    {
                break;
            }
        }
        if(!(totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 4).longValue() + stonewall1.getSpendAmount() + SamouraiWallet.bDust.longValue()))    {
            Toast.makeText(context, R.string.cannot_compose_cahoots, Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("CahootsUtil", "BIP84 selected utxos:" + selectedUTXO.size());

        long fee = FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 4).longValue();
        Log.d("CahootsUtil", "fee:" + fee);

        NetworkParameters params = stonewall1.getParams();

        //
        //
        // step2: B verif, utxos -> A (take smallest that cover amount)
        // B provides 1000000, 1500000 (250000 change, A input larger)
        //
        //

        String zpub = BIP84Util.getInstance(context).getWallet().getAccount(0).zpubstr();
        HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>> inputsB = new HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>>();

        for(UTXO utxo : selectedUTXO)  {
            for(MyTransactionOutPoint outpoint : utxo.getOutpoints())   {
                _TransactionOutPoint _outpoint = new _TransactionOutPoint(outpoint);

                ECKey eckey = SendFactory.getPrivKey(_outpoint.getAddress());
                String path = APIFactory.getInstance(context).getUnspentPaths().get(_outpoint.getAddress());
                inputsB.put(_outpoint, Triple.of(eckey.getPubKey(), FormatsUtil.getInstance().getFingerprintFromXPUB(zpub), path));
            }
        }

        // spender change output
        int idx = BIP84Util.getInstance(context).getWallet().getAccount(0).getChange().getAddrIdx();
        SegwitAddress segwitAddress = BIP84Util.getInstance(context).getAddressAt(1, idx);
        HashMap<_TransactionOutput, Triple<byte[], byte[], String>> outputsB = new HashMap<_TransactionOutput, Triple<byte[], byte[], String>>();
        Pair<Byte, byte[]> pair0 = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress.getBech32AsString());
        byte[] scriptPubKey_B0 = Bech32Segwit.getScriptPubkey(pair0.getLeft(), pair0.getRight());
        _TransactionOutput output_B0 = new _TransactionOutput(params, null, Coin.valueOf((totalSelectedAmount - stonewall1.getSpendAmount()) - fee), scriptPubKey_B0);
        outputsB.put(output_B0, Triple.of(segwitAddress.getECKey().getPubKey(), FormatsUtil.getInstance().getFingerprintFromXPUB(BIP84Util.getInstance(context).getWallet().getAccount(0).zpubstr()), "M/1/" + idx));

        STONEWALLx2 stonewall2 = new STONEWALLx2(stonewall1);
        stonewall2.inc(inputsB, outputsB, null);
        System.out.println("step 2:" + stonewall2.toJSON().toString());

        doCahoots(stonewall2.toJSON().toString());
    }

    private void doSTONEWALLx2_3(STONEWALLx2 stonewall2) throws Exception    {

        HashMap<String,String> utxo2Address = new HashMap<String,String>();
        List<UTXO> utxos = APIFactory.getInstance(context).getUtxos(true);
        for(UTXO utxo : utxos)   {
            for(MyTransactionOutPoint outpoint : utxo.getOutpoints())   {
                utxo2Address.put(outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN(), outpoint.getAddress());
            }
        }

//        Transaction transaction = stonewall2.getPSBT().getTransaction();
        Transaction transaction = stonewall2.getTransaction();
        HashMap<String,ECKey> keyBag_A = new HashMap<String,ECKey>();
        for(TransactionInput input : transaction.getInputs())   {
            TransactionOutPoint outpoint = input.getOutpoint();
            String key = outpoint.getHash().toString() + "-" + outpoint.getIndex();
            if(utxo2Address.containsKey(key))    {
                String address = utxo2Address.get(key);
                ECKey eckey = SendFactory.getPrivKey(address);
                keyBag_A.put(outpoint.toString(), eckey);
            }
        }

        //
        //
        // step3: A verif, BIP69, sig
        //
        //
        /*
        HashMap<String,ECKey> keyBag_A = new HashMap<String,ECKey>();
        keyBag_A.put(outpoint_A0.toString(), ecKey_A0);
        keyBag_A.put(outpoint_A1.toString(), ecKey_A1);
        */

        STONEWALLx2 stonewall3 = new STONEWALLx2(stonewall2);
        stonewall3.inc(null, null, keyBag_A);
        System.out.println("step 3:" + stonewall3.toJSON().toString());

        doCahoots(stonewall3.toJSON().toString());
    }

    private void doSTONEWALLx2_4(STONEWALLx2 stonewall3) throws Exception    {

        HashMap<String,String> utxo2Address = new HashMap<String,String>();
        List<UTXO> utxos = APIFactory.getInstance(context).getUtxos(true);
        for(UTXO utxo : utxos)   {
            for(MyTransactionOutPoint outpoint : utxo.getOutpoints())   {
                utxo2Address.put(outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN(), outpoint.getAddress());
            }
        }

//        Transaction transaction = stonewall3.getPSBT().getTransaction();
        Transaction transaction = stonewall3.getTransaction();
        HashMap<String,ECKey> keyBag_B = new HashMap<String,ECKey>();
        for(TransactionInput input : transaction.getInputs())   {
            TransactionOutPoint outpoint = input.getOutpoint();
            String key = outpoint.getHash().toString() + "-" + outpoint.getIndex();
            if(utxo2Address.containsKey(key))    {
                String address = utxo2Address.get(key);
                ECKey eckey = SendFactory.getPrivKey(address);
                keyBag_B.put(outpoint.toString(), eckey);
            }
        }

        //
        //
        // step4: B verif, sig, broadcast
        //
        //

        /*
        HashMap<String,ECKey> keyBag_B = new HashMap<String,ECKey>();
        keyBag_B.put(outpoint_B0.toString(), ecKey_B0);
        keyBag_B.put(outpoint_B1.toString(), ecKey_B1);
        */

        STONEWALLx2 stonewall4 = new STONEWALLx2(stonewall3);
        stonewall4.inc(null, null, keyBag_B);
        System.out.println("step 4:" + stonewall4.toJSON().toString());
        System.out.println("step 4 psbt:" + stonewall4.getPSBT().toString());
        System.out.println("step 4 tx:" + stonewall4.getTransaction().toString());
        System.out.println("step 4 tx hex:" + Hex.toHexString(stonewall4.getTransaction().bitcoinSerialize()));

//        Stowaway s = new Stowaway(stowaway4.toJSON());
//        System.out.println(s.toJSON().toString());

        // broadcast ???
        doCahoots(stonewall4.toJSON().toString());
    }

    private List<UTXO> getCahootsUTXO() {
        List<UTXO> ret = new ArrayList<UTXO>();
        List<UTXO> _utxos = APIFactory.getInstance(context).getUtxos(true);
        for(UTXO utxo : _utxos)   {
            String script = Hex.toHexString(utxo.getOutpoints().get(0).getScriptBytes());
            if(script.startsWith("0014") && APIFactory.getInstance(context).getUnspentPaths().get(utxo.getOutpoints().get(0).getAddress()) != null)   {
                ret.add(utxo);
            }
        }

        return ret;
    }

    public long getCahootsValue() {
        long ret = 0L;
        List<UTXO> _utxos = getCahootsUTXO();
        for(UTXO utxo : _utxos)   {
            ret += utxo.getValue();
        }

        return ret;
    }

}
