package com.samourai.wallet.ricochet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
//import android.util.Log;

import com.samourai.wallet.home.BalanceActivity;
import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.NotSecp256k1Exception;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.send.PushTx;

import org.bitcoinj.crypto.MnemonicException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;

public class RicochetActivity extends Activity {

    private String strPCode = null;
    private boolean samouraiFeeViaBIP47 = false;

    private ProgressDialog progress = null;
    private String strProgressTitle = null;
    private String strProgressMessage = null;

    private final static long SLEEP_DELAY = 10L * 1000L;

    private boolean broadcastOK = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ricochet);

        if(RicochetMeta.getInstance(RicochetActivity.this).size() > 0)    {

//            new QueueTask().execute(new String[]{ null });
            QueueTask qt = new QueueTask();
            qt.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new String[]{ null });
        }

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ricochet, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        // noinspection SimplifiableIfStatement
        if (id == R.id.action_show_script) {
            doShowScript();
        }
        else if (id == R.id.action_replay_script) {
            doReplayScript();
        }
        else {
            ;
        }

        return super.onOptionsItemSelected(item);
    }

    private class QueueTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            try {

                PayloadUtil.getInstance(RicochetActivity.this).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(RicochetActivity.this).getGUID() + AccessFactory.getInstance(RicochetActivity.this).getPIN()));

                final Iterator<JSONObject> itr = RicochetMeta.getInstance(RicochetActivity.this).getIterator();
                while(itr.hasNext()){

                    boolean hasConfirmation = false;
                    boolean txSeen = false;
                    JSONObject jObj = itr.next();
                    JSONArray jHops = jObj.getJSONArray("hops");
                    if(jHops.length() > 0)    {

                        JSONObject jHop = jHops.getJSONObject(jHops.length() - 1);
                        String txHash = jHop.getString("hash");

                        JSONObject txObj = APIFactory.getInstance(RicochetActivity.this).getTxInfo(txHash);
                        if(txObj != null && txObj.has("block_height") && txObj.getInt("block_height") != -1)    {
                            hasConfirmation = true;
                        }
                        else if(txObj != null && txObj.has("txid"))    {
                            txSeen = true;
                        }
                        // not broadcast, not seen
                        else if(txObj != null && txObj.has("status") && txObj.getString("status").equals("error"))    {
                            txSeen = false;
                        }
                        else    {
                            ;
                        }

                        if(hasConfirmation)    {
                            itr.remove();
                            continue;
                        }

                    }

                    if(!txSeen)    {

                        if(jObj.has("pcode") && jObj.getString("pcode").length() > 0)    {
                            strPCode = jObj.getString("pcode");
                        }

                        if(jObj.has("samouraiFeeViaBIP47"))    {
                            samouraiFeeViaBIP47 = jObj.getBoolean("samouraiFeeViaBIP47");
                        }

                        String[] txs = new String[jHops.length()];
                        String[] dests = new String[jHops.length()];
                        for(int i = 0; i < jHops.length(); i++)   {
                            JSONObject jSeq = jHops.getJSONObject(i);
                            int seq = jSeq.getInt("seq");
                            assert(seq == i);
                            String tx = jSeq.getString("tx");
//                            Log.d("RicochetActivity", "seq:" + seq + ":" + tx);
                            txs[i] = tx;
                            String dest = jSeq.getString("destination");
//                            Log.d("RicochetActivity", "seq:" + seq + ":" + dest);
                            dests[i] = dest;
                        }

                        if(txs.length >= 5 && dests.length >= 5)    {

                            boolean isOK = false;
                            int i = 0;
                            while(i < txs.length)   {

                                isOK = false;

                                String response = PushTx.getInstance(RicochetActivity.this).samourai(txs[i]);
                                Log.d("RicochetActivity", "pushTx:" + response);
                                JSONObject jsonObject = new JSONObject(response);
                                if(jsonObject.has("status") && jsonObject.getString("status").equals("ok"))    {
                                    isOK = true;
                                }

                                if(isOK)    {
                                    strProgressTitle = RicochetActivity.this.getText(R.string.ricochet_hop).toString() + " " + i;
                                    strProgressMessage = RicochetActivity.this.getText(R.string.ricochet_hopping).toString() + " " + dests[i];
                                    publishProgress();

                                    if(i == (txs.length - 1))    {

                                        broadcastOK = true;

                                        RicochetMeta.getInstance(RicochetActivity.this).setLastRicochet(jObj);

                                        //
                                        // increment change address
                                        //
                                        try {
                                            HD_WalletFactory.getInstance(RicochetActivity.this).get().getAccount(0).getChange().incAddrIdx();
                                        }
                                        catch(IOException ioe) {
                                            ;
                                        }
                                        catch(MnemonicException.MnemonicLengthException mle) {
                                            ;
                                        }

                                        //
                                        // increment BIP47 receive if send to BIP47
                                        //
                                        if(strPCode != null && strPCode.length() > 0)    {
                                            BIP47Meta.getInstance().getPCode4AddrLookup().put(dests[i], strPCode);
                                            BIP47Meta.getInstance().incOutgoingIdx(strPCode);
                                        }

                                        //
                                        // increment BIP47 donation if fee to BIP47 donation
                                        //
                                        if(samouraiFeeViaBIP47)    {
                                            for(int j = 0; j < 4; j++)   {
                                                PaymentCode pcode = new PaymentCode(BIP47Meta.strSamouraiDonationPCode);
                                                PaymentAddress paymentAddress = BIP47Util.getInstance(RicochetActivity.this).getSendAddress(pcode, BIP47Meta.getInstance().getOutgoingIdx(BIP47Meta.strSamouraiDonationPCode));
                                                String strAddress = paymentAddress.getSendECKey().toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString();
                                                BIP47Meta.getInstance().getPCode4AddrLookup().put(strAddress, BIP47Meta.strSamouraiDonationPCode);
                                                BIP47Meta.getInstance().incOutgoingIdx(BIP47Meta.strSamouraiDonationPCode);
                                            }
                                        }

                                    }

                                    i++;
                                    if(i < txs.length)    {
                                        Thread.sleep(SLEEP_DELAY);
                                    }

                                }
                                else    {

                                    Thread.sleep(SLEEP_DELAY);
                                    continue;

                                }

                            }

                        }
                        else    {
                            // badly formed error
                        }

                    }

                }

            }
            catch(JSONException je) {
                je.printStackTrace();
            }
            catch (InterruptedException ie) {
                ie.printStackTrace();
            }
            catch (NotSecp256k1Exception nse) {
                nse.printStackTrace();
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            return "OK";
        }

        @Override
        protected void onPostExecute(String result) {

            if(progress != null && progress.isShowing())    {
                progress.dismiss();
            }

            if(broadcastOK)    {
                AlertDialog.Builder dlg = new AlertDialog.Builder(RicochetActivity.this)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.ricochet_broadcast)
                        .setCancelable(false)
                        .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                Intent intent = new Intent(RicochetActivity.this, BalanceActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                startActivity(intent);

                            }
                        });
                if(!isFinishing())    {
                    dlg.show();
                }
            }
            else    {
                AlertDialog.Builder dlg = new AlertDialog.Builder(RicochetActivity.this)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.ricochet_not_broadcast_replay)
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                RicochetActivity.this.recreate();

                            }
                        }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ;
                            }
                        });
                if(!isFinishing())    {
                    dlg.show();
                }
            }

        }

        @Override
        protected void onPreExecute() {

            progress = new ProgressDialog(RicochetActivity.this);
            progress.setCancelable(false);
            progress.setTitle(R.string.app_name);
            progress.setMessage(getString(R.string.please_wait_ricochet));
            progress.show();

        }

        @Override
        protected void onProgressUpdate(Void... values) {

            progress.setTitle(strProgressTitle);
            progress.setMessage(strProgressMessage);

        }
    }

    private void doReplayScript() {

        if(RicochetMeta.getInstance(RicochetActivity.this).size() > 0)    {

            new AlertDialog.Builder(RicochetActivity.this)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.ricochet_replay)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            QueueTask qt = new QueueTask();
                            qt.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new String[]{ null });

                        }
                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    ;
                }
            }).show();

        }
        else    {
            Toast.makeText(RicochetActivity.this, R.string.no_ricochet_replay, Toast.LENGTH_SHORT).show();
        }

    }

    private void doShowScript() {

        if(RicochetMeta.getInstance(RicochetActivity.this).getLastRicochet() != null)    {

            TextView showText = new TextView(RicochetActivity.this);
            showText.setText(RicochetMeta.getInstance(RicochetActivity.this).getLastRicochet().toString());
            showText.setTextIsSelectable(true);
            showText.setPadding(40, 10, 40, 10);
            showText.setTextSize(18.0f);
            new AlertDialog.Builder(RicochetActivity.this)
                    .setTitle(R.string.app_name)
                    .setView(showText)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            ;
                        }
                    }).show();

        }
        else    {
            Toast.makeText(RicochetActivity.this, R.string.no_ricochet_display, Toast.LENGTH_SHORT).show();
        }

    }

}
