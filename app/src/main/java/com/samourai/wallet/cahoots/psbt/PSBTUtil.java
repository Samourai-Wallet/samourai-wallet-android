package com.samourai.wallet.cahoots.psbt;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.UTXO;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionWitness;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bouncycastle.util.encoders.Hex;

import java.util.HashMap;
import java.util.List;

import static com.samourai.wallet.util.LogUtil.debug;

public class PSBTUtil {

    private static Context context = null;

    private static PSBTUtil instance = null;

    private PSBTUtil()    { ; }

    public static PSBTUtil getInstance(Context ctx) {

        context = ctx;

        if(instance == null)    {
            instance = new PSBTUtil();
        }

        return instance;
    }

    public void doPSBT(final String strPSBT)    {

        String msg = null;
        PSBT psbt = new PSBT(strPSBT, SamouraiWallet.getInstance().getCurrentNetworkParams());
        psbt.setDebug(true);
        try {
            psbt.read();
            if(!psbt.isParseOK()) {
                Toast.makeText(context, R.string.psbt_error, Toast.LENGTH_SHORT).show();
                return;
            }
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
                .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                    }

                })
                .setNegativeButton(R.string.psbt_sign_tx, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                        String unsignedHash = psbt.getTransaction().getHashAsString();
                        String unsignedHex = new String(Hex.encode(psbt.getTransaction().bitcoinSerialize()));
                        Transaction tx = doPSBTSignTx(psbt);
                        debug("PSBTUtil", "unsigned tx hash:" + unsignedHash);
                        debug("PSBTUtil", "unsigned tx:" + unsignedHex);
                        debug("PSBTUtil", "  signed tx hash:" + tx.getHashAsString());
                        String signedHex = new String(Hex.encode(tx.bitcoinSerialize()));
                        debug("PSBTUtil", "  signed tx:" + signedHex);

                        final TextView tvHexTx = new TextView(context);
                        float scale = context.getResources().getDisplayMetrics().density;
                        tvHexTx.setSingleLine(false);
                        tvHexTx.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                        tvHexTx.setLines(10);
                        tvHexTx.setGravity(Gravity.START);
                        tvHexTx.setText(signedHex);
                        tvHexTx.setPadding((int) (8 * scale + 0.5f), (int) (6 * scale + 0.5f), (int) (8 * scale + 0.5f), (int) (6 * scale + 0.5f));

                        AlertDialog.Builder dlg = new AlertDialog.Builder(context)
                                .setTitle(R.string.app_name)
                                .setView(tvHexTx)
                                .setCancelable(false)
                                .setPositiveButton(R.string.copy_to_clipboard, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        dialog.dismiss();
                                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager)context.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                        android.content.ClipData clip = null;
                                        clip = android.content.ClipData.newPlainText("tx", signedHex);
                                        clipboard.setPrimaryClip(clip);
                                        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        dialog.dismiss();
                                    }
                                });
                        if(!((Activity)context).isFinishing())    {
                            dlg.show();
                        }

                    }

                });
        if(!((Activity)context).isFinishing())    {
            dlg.show();
        }

    }

    public Transaction doPSBTSignTx(PSBT psbt)    {

        Transaction tx = psbt.getTransaction();

        HashMap<String,ECKey> keyBag = new HashMap<String,ECKey>();
        HashMap<String,Long> amountBag = new HashMap<String,Long>();
        List<UTXO> utxos = APIFactory.getInstance(context).getUtxos(true);

        for(TransactionInput input : tx.getInputs()) {

            TransactionOutPoint _outpoint = input.getOutpoint();
            String _hash = _outpoint.getHash().toString();
            long _idx = _outpoint.getIndex();

            for(UTXO utxo : utxos) {
                List<MyTransactionOutPoint> outpoints = utxo.getOutpoints();
                for(MyTransactionOutPoint outpoint : outpoints) {
                    if(outpoint.getTxHash().toString().equalsIgnoreCase(_hash) && outpoint.getTxOutputN() == _idx) {
                        debug("PSBTUtil", "stored:" + input.toString());
                        ECKey ecKey = SendFactory.getPrivKey(outpoint.getAddress(), 0);
                        keyBag.put(outpoint.getAddress(), ecKey);
                        keyBag.put(_outpoint.toString(), ecKey);
                        amountBag.put(_outpoint.toString(), outpoint.getValue().longValue());
                    }
                }
            }

        }

        tx = signTx(psbt, tx, keyBag, amountBag);

        return tx;
    }

    public Transaction signTx(PSBT psbt, Transaction transaction, HashMap<String,ECKey> keyBag, HashMap<String,Long> amountBag) {

        List<PSBTEntry> psbtInputs = psbt.getPsbtInputs();

        for(PSBTEntry entry : psbtInputs) {

            if(entry.getKey() == null) {
                continue;
            }

/*
            if(org.spongycastle.util.encoders.Hex.toHexString(entry.getKeyType()).equals("01")) {
                byte[] data = entry.getData();
                byte[] amount = new byte[8];
                byte[] scriptpubkey = new byte[data.length - 8];
                System.arraycopy(data, 0, amount, 0, 8);
                System.arraycopy(data, 8, scriptpubkey, 0, data.length - 8);
                ByteBuffer bb = ByteBuffer.wrap(amount);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                // Assert.assertTrue(175000000L == bb.getLong());
                // Assert.assertTrue("16001407af7cfec745600da9bffebc6a1db29d42ea9ace".equalsIgnoreCase(org.spongycastle.util.encoders.Hex.toHexString(scriptpubkey)));
            }
            else if(org.spongycastle.util.encoders.Hex.toHexString(entry.getKeyType()).equals("06")) {
                byte[] keydata = entry.getKeyData();
                // Assert.assertTrue("tb1qq7hhelk8g4sqm2dll67x58djn4pw4xkwx040qg".equals(new SegwitAddress(keydata, TestNet3Params.get()).getBech32AsString()));
                String address = new SegwitAddress(keydata, SamouraiWallet.getInstance().getCurrentNetworkParams()).getBech32AsString();
                ECKey ecKey = SendFactory.getPrivKey(address, 0);
                keyBag.put(address, ecKey);
            }
*/
        }

        debug("Cahoots", "signTx:" + transaction.toString());

        for(int i = 0; i < transaction.getInputs().size(); i++)   {

            TransactionInput input = transaction.getInput(i);
            TransactionOutPoint outpoint = input.getOutpoint();
            if(keyBag.containsKey(outpoint.toString())) {

                debug("PSBTUtil", "signTx outpoint:" + outpoint.toString());

                ECKey key = keyBag.get(outpoint.toString());
                SegwitAddress segwitAddress = new SegwitAddress(key.getPubKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());

                debug("PSBTUtil", "signTx bech32:" + segwitAddress.getBech32AsString());

                final Script redeemScript = segwitAddress.segWitRedeemScript();
                debug("PSBTUtil", "signTx bech32:" + Hex.toHexString(redeemScript.getProgram()));
                final Script scriptCode = redeemScript.scriptCode();

                long value = amountBag.get(outpoint.toString());
                debug("PSBTUtil", "signTx value:" + value);

                TransactionSignature sig = transaction.calculateWitnessSignature(i, key, scriptCode, Coin.valueOf(value), Transaction.SigHash.ALL, false);
                final TransactionWitness witness = new TransactionWitness(2);
                witness.setPush(0, sig.encodeToBitcoin());
                witness.setPush(1, key.getPubKey());
                transaction.setWitness(i, witness);

            }

        }

        return transaction;

    }

}
