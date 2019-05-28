package com.samourai.wallet.send;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Looper;
import android.widget.Toast;
import android.util.Log;

import com.samourai.wallet.JSONRPC.TrustedNodeUtil;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.PrivKeyReader;
import com.samourai.wallet.R;

import org.bitcoinj.core.Coin;
import org.json.JSONException;
import org.json.JSONObject;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;

public class SweepUtil  {

    public static int TYPE_P2PKH = 0;
    public static int TYPE_P2SH_P2WPKH = 1;
    public static int TYPE_P2WPKH = 2;

    private static Context context = null;
    private static SweepUtil instance = null;

    private static UTXO utxoP2PKH = null;
    private static UTXO utxoP2SH_P2WPKH = null;
    private static UTXO utxoP2WPKH = null;

    private static String addressP2PKH = null;
    private static String addressP2SH_P2WPKH = null;
    private static String addressP2WPKH = null;

    private SweepUtil() { ; }

    public static SweepUtil getInstance(Context ctx) {

        context = ctx;

        if(instance == null)    {
            instance = new SweepUtil();
        }

        return instance;
    }

    public void sweep(final PrivKeyReader privKeyReader, final int type)  {

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                try {

                    if(privKeyReader == null || privKeyReader.getKey() == null || !privKeyReader.getKey().hasPrivKey())    {
                        Toast.makeText(context, R.string.cannot_recognize_privkey, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    final String address;
                    UTXO utxo = null;

                    if(type == TYPE_P2SH_P2WPKH)    {
                        utxo = utxoP2SH_P2WPKH;
                        address = addressP2SH_P2WPKH;
                    }
                    else if(type == TYPE_P2WPKH)    {
                        utxo = utxoP2WPKH;
                        address = addressP2WPKH;
                    }
                    else    {
                        addressP2PKH = privKeyReader.getKey().toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
                        Log.d("SweepUtil", "address derived P2PKH:" + addressP2PKH);
                        addressP2SH_P2WPKH = new SegwitAddress(privKeyReader.getKey(), SamouraiWallet.getInstance().getCurrentNetworkParams()).getAddressAsString();
                        Log.d("SweepUtil", "address derived P2SH_P2WPKH:" + addressP2SH_P2WPKH);
                        addressP2WPKH = new SegwitAddress(privKeyReader.getKey(), SamouraiWallet.getInstance().getCurrentNetworkParams()).getBech32AsString();
                        Log.d("SweepUtil", "address derived P2WPKH:" + addressP2WPKH);

                        utxoP2PKH = APIFactory.getInstance(context).getUnspentOutputsForSweep(addressP2PKH);
                        utxoP2SH_P2WPKH = APIFactory.getInstance(context).getUnspentOutputsForSweep(addressP2SH_P2WPKH);
                        utxoP2WPKH = APIFactory.getInstance(context).getUnspentOutputsForSweep(addressP2WPKH);

                        utxo = utxoP2PKH;
                        address = addressP2PKH;
                    }

                    if(utxo != null)    {

                        long total_value = 0L;
                        final List<MyTransactionOutPoint> outpoints = utxo.getOutpoints();
                        for(MyTransactionOutPoint outpoint : outpoints)   {
                            total_value += outpoint.getValue().longValue();
                        }

                        if(FeeUtil.getInstance().getSuggestedFee().getDefaultPerKB().longValue() <= 1000L)    {
                            SuggestedFee suggestedFee = new SuggestedFee();
                            suggestedFee.setDefaultPerKB(BigInteger.valueOf(1100L));
                            Log.d("SweepUtil", "adjusted fee:" + suggestedFee.getDefaultPerKB().longValue());
                            FeeUtil.getInstance().setSuggestedFee(suggestedFee);
                        }

                        Log.d("SweepUtil", "outpoints:" + outpoints.size());
                        Log.d("SweepUtil", "type:" + type);

                        final BigInteger fee;
                        if(type == TYPE_P2SH_P2WPKH)    {
                            fee = FeeUtil.getInstance().estimatedFeeSegwit(0, outpoints.size(), 1);
                        }
                        else if(type == TYPE_P2WPKH)    {
                            fee = FeeUtil.getInstance().estimatedFeeSegwit(0, 0, outpoints.size(), 1);
                        }
                        else    {
                            fee = FeeUtil.getInstance().estimatedFee(outpoints.size(), 1);
                        }

                        final long amount = total_value - fee.longValue();
//                        Log.d("BalanceActivity", "Total value:" + total_value);
//                        Log.d("BalanceActivity", "Amount:" + amount);
                        Log.d("SweepUtil", "Fee:" + fee.toString());

                        String message = "Sweep " + Coin.valueOf(amount).toPlainString() + " from " + address + " (fee:" + Coin.valueOf(fee.longValue()).toPlainString() + ")?";

                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle(R.string.app_name);
                        builder.setMessage(message);
                        builder.setCancelable(false);
                        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, int whichButton) {

                                final ProgressDialog progress = new ProgressDialog(context);
                                progress.setCancelable(false);
                                progress.setTitle(R.string.app_name);
                                progress.setMessage(context.getString(R.string.please_wait_sending));
                                progress.show();

                                String receive_address = null;
                                if(PrefsUtil.getInstance(context).getValue(PrefsUtil.USE_SEGWIT, true) == true)    {
                                    receive_address = AddressFactory.getInstance(context).getBIP84(AddressFactory.RECEIVE_CHAIN).getBech32AsString();
                                }
                                else    {
                                    receive_address = AddressFactory.getInstance(context).get(AddressFactory.RECEIVE_CHAIN).getAddressString();
                                }
                                final HashMap<String, BigInteger> receivers = new HashMap<String, BigInteger>();
                                receivers.put(receive_address, BigInteger.valueOf(amount));
                                org.bitcoinj.core.Transaction tx = SendFactory.getInstance(context).makeTransaction(0, outpoints, receivers);

                                tx = SendFactory.getInstance(context).signTransactionForSweep(tx, privKeyReader);
                                Log.d("SweepUtil", "tx size:" + tx.bitcoinSerialize().length);
                                final String hexTx = new String(Hex.encode(tx.bitcoinSerialize()));
//                                Log.d("BalanceActivity", hexTx);

                                String response = null;
                                try {
                                    if(PrefsUtil.getInstance(context).getValue(PrefsUtil.USE_TRUSTED_NODE, false) == true)    {
                                        if(TrustedNodeUtil.getInstance().isSet())    {
                                            response = PushTx.getInstance(context).trustedNode(hexTx);
                                            JSONObject jsonObject = new org.json.JSONObject(response);
                                            if(jsonObject.has("result"))    {
                                                if(jsonObject.getString("result").matches("^[A-Za-z0-9]{64}$"))    {
                                                    Toast.makeText(context, R.string.tx_sent, Toast.LENGTH_SHORT).show();
                                                }
                                                else    {
                                                    Toast.makeText(context, R.string.trusted_node_tx_error, Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        }
                                        else    {
                                            Toast.makeText(context, R.string.trusted_node_not_valid, Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                    else    {
                                        response = PushTx.getInstance(context).samourai(hexTx);

                                        if(response != null)    {
                                            JSONObject jsonObject = new org.json.JSONObject(response);
                                            if(jsonObject.has("status"))    {
                                                if(jsonObject.getString("status").equals("ok"))    {
                                                    Toast.makeText(context, R.string.tx_sent, Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        }
                                        else    {
                                            Toast.makeText(context, R.string.pushtx_returns_null, Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                                catch(JSONException je) {
                                    Toast.makeText(context, "pushTx:" + je.getMessage(), Toast.LENGTH_SHORT).show();
                                }

                                if(progress != null && progress.isShowing())    {
                                    progress.dismiss();
                                }

                            }
                        });
                        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, int whichButton) {
                                ;
                            }
                        });

                        AlertDialog alert = builder.create();
                        alert.show();

                    }
                    else if(type == TYPE_P2SH_P2WPKH)    {
                        sweep(privKeyReader, TYPE_P2WPKH);
                    }
                    else if(type == TYPE_P2PKH)    {
                        sweep(privKeyReader, TYPE_P2SH_P2WPKH);
                    }
                    else if(type == TYPE_P2WPKH)    {
                        ;
                    }
                    else    {
                        ;
                    }

                }
                catch(Exception e) {
                    Toast.makeText(context, R.string.cannot_sweep_privkey, Toast.LENGTH_SHORT).show();
                }

                Looper.loop();

            }
        }).start();

    }

}
