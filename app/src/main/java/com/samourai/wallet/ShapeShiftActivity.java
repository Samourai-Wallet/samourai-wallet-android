package com.samourai.wallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
//import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONArray;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

import com.samourai.shapeshift.Tx;
import com.samourai.shapeshift.TxQueue;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.TimeOutUtil;
import com.squareup.picasso.Picasso;

import com.samourai.shapeshift.APIFactory;
import com.samourai.wallet.hd.HD_Address;

public class ShapeShiftActivity extends Activity {

    private List<String> coinsList = null;
    private String[] coins = null;
    private HashMap<String,String> iconsMap = null;

    private List<String> coins_na = null;
    private Spinner spAvailableCoins = null;
    private MyCustomAdapter adapter = null;

    private LinearLayout sendLayout = null;

    private Button btConfirm = null;
    private Button btCancel = null;

    private static Display display = null;
    private static int imgWidth = 0;

    private ProgressDialog progress = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shapeshift);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        display = ShapeShiftActivity.this.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        imgWidth = size.x - 80;

        coins_na = new ArrayList<String>();
        coins_na.add("BITUSD");
        coins_na.add("BTS");
        coins_na.add("NXT");
        coins_na.add("XMR");
        coins_na.add("XRP");

        spAvailableCoins = (Spinner)findViewById(R.id.available_coins);
        coins = new String[1];
        coins[0] = "BTC";
        iconsMap =  new HashMap<String,String>();
        iconsMap.put("BTC", "https://shapeshift.io/images/coins/bitcoin.png");
        adapter = new MyCustomAdapter(ShapeShiftActivity.this, R.layout.shapeshift_selector_row, coins);
        spAvailableCoins.setAdapter(adapter);
        spAvailableCoins.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(i == 0) {
                    ((ImageView)findViewById(R.id.qr)).setVisibility(View.INVISIBLE);
                    sendLayout.setVisibility(View.INVISIBLE);

                    ((LinearLayout)findViewById(R.id.coin_info_layout)).setVisibility(View.INVISIBLE);
                    ((TextView)findViewById(R.id.coin_name)).setVisibility(View.INVISIBLE);
                    ((TextView)findViewById(R.id.limit)).setVisibility(View.INVISIBLE);
                    ((TextView)findViewById(R.id.max)).setVisibility(View.INVISIBLE);

                }
                else {
                    sendLayout.setVisibility(View.VISIBLE);
                    ((ImageView)findViewById(R.id.qr)).setVisibility(View.INVISIBLE);
                    ((TextView)findViewById(R.id.send_address)).setText("");

                    ((LinearLayout)findViewById(R.id.coin_info_layout)).setVisibility(View.VISIBLE);
                    ((TextView)findViewById(R.id.coin_name)).setVisibility(View.VISIBLE);
                    ((TextView)findViewById(R.id.limit)).setVisibility(View.VISIBLE);
                    ((TextView)findViewById(R.id.max)).setVisibility(View.VISIBLE);
                    ((TextView)findViewById(R.id.coin_name)).setText("");
                    ((TextView)findViewById(R.id.limit)).setText("");
                    ((TextView)findViewById(R.id.max)).setText("");

                    final String selectedCoin = (String)spAvailableCoins.getSelectedItem();
//                    Log.i("ShapeShiftActivity", selectedCoin);
                    int idx = selectedCoin.indexOf("(");
                    setupSpend(selectedCoin.substring(idx + 1, selectedCoin.length() - 1), selectedCoin.substring(0, idx).trim());

                    progress = new ProgressDialog(ShapeShiftActivity.this);
                    progress.setCancelable(true);
                    progress.setIndeterminate(true);
                    progress.setMessage(ShapeShiftActivity.this.getText(R.string.please_wait));
                    progress.show();

                }
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
                return;
            }

        });

        sendLayout = (LinearLayout)findViewById(R.id.send_address_layout);
        sendLayout.setVisibility(View.INVISIBLE);
        sendLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                new AlertDialog.Builder(ShapeShiftActivity.this)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.send_address_to_clipboard)
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                android.content.ClipboardManager clipboard = (android.content.ClipboardManager)ShapeShiftActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                android.content.ClipData clip = null;
                                clip = android.content.ClipData.newPlainText("Send address", ((TextView)findViewById(R.id.send_address)).getText().toString());
                                clipboard.setPrimaryClip(clip);
                                Toast.makeText(ShapeShiftActivity.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                            }

                        }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        ;
                    }
                }).show();

                return false;
            }
        });

        init();

    }

    @Override
    protected void onResume() {
        super.onResume();

        AppUtil.getInstance(ShapeShiftActivity.this).setIsInForeground(true);

        if(TimeOutUtil.getInstance().isTimedOut()) {
            Intent intent = new Intent(ShapeShiftActivity.this, PinEntryActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        else {
            TimeOutUtil.getInstance().updatePin();

        }

    }

    private void init() {

        progress = new ProgressDialog(this);
        progress.setCancelable(true);
        progress.setIndeterminate(true);
        progress.setMessage(this.getText(R.string.getting_market_info));
        progress.show();

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String strMarketData = APIFactory.getInstance().marketInfo();
                    final String strAvailableCoins = APIFactory.getInstance().availableCoins();

//                    Log.i("ShapeShiftActivity", strMarketData);
//                    Log.i("ShapeShiftActivity", strAvailableCoins);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                final JSONArray marketObj = new JSONArray(strMarketData);
                                final JSONObject availableCoinsObj = new JSONObject(strAvailableCoins);

                                if((availableCoinsObj == null || availableCoinsObj.has("error"))) {
                                    Toast.makeText(ShapeShiftActivity.this, ShapeShiftActivity.this.getText(R.string.error_getting_market_info), Toast.LENGTH_SHORT).show();
                                    if (progress != null && progress.isShowing()) {
                                        progress.dismiss();
                                    }
                                    return;
                                }

                                coinsList = new ArrayList<String>();
                                iconsMap = new HashMap<String,String>();
                                for(int i = 0; i < marketObj.length(); i++) {
                                    if(((JSONObject)marketObj.get(i)).has("pair")) {
                                        String[] pair = ((String)((JSONObject)marketObj.get(i)).getString("pair")).split("_");
                                        if(pair != null && pair.length == 2 && pair[0] != null && pair[1] != null && pair[1].equals("BTC") && !coins_na.contains(pair[0])) {
//                                            Log.i("ShapeShiftActivity", ((JSONObject)marketObj.get(i)).getString("pair"));
                                            if(availableCoinsObj.has(pair[0])) {
                                                JSONObject coinObj = availableCoinsObj.getJSONObject(pair[0]);
                                                if(coinObj != null && coinObj.has("status") && ((String)coinObj.get("status")).equals("available")) {
//                                                Log.i("ShapeShiftActivity", (String)coinObj.get("name"));
                                                    coinsList.add((String)coinObj.get("name") + " (" + pair[0] + ")");
                                                    iconsMap.put(pair[0], (String)coinObj.get("image"));
                                                }
                                            }
                                        }
                                    }
                                }

                                Collections.sort(coinsList);
                                coinsList.add(0, (String)ShapeShiftActivity.this.getText(R.string.select_cryptocurrency));
                                coins = coinsList.toArray(new String[coinsList.size()]);
                                adapter = new MyCustomAdapter(ShapeShiftActivity.this, R.layout.shapeshift_selector_row, coins);
                                spAvailableCoins.setAdapter(adapter);
                                adapter.notifyDataSetChanged();

                            } catch (Exception e) {
                                if (progress != null && progress.isShowing()) {
                                    progress.dismiss();
                                }
                                e.printStackTrace();
                            }
                        }
                    });
                } catch (Exception e) {
                    if (progress != null && progress.isShowing()) {
                        progress.dismiss();
                    }
                    e.printStackTrace();
                }

                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                }

            }
        }).start();
    }

    private void setupSpend(final String coin, final String name) {

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                try {
                    final String strPairLimit = APIFactory.getInstance().limit(coin + "_btc");
                    final String strPairRate = APIFactory.getInstance().rate(coin + "_btc");
                    final JSONObject rateObj = new JSONObject(strPairRate);
                    final JSONObject limitObj = new JSONObject(strPairLimit);

                    final String min;
                    final String limit;
                    final String rate;

                    if((rateObj == null || rateObj.has("error")) || (limitObj == null || limitObj.has("error"))) {
                        Toast.makeText(ShapeShiftActivity.this, rateObj.getString("error"), Toast.LENGTH_SHORT).show();
                        if (progress != null && progress.isShowing()) {
                            progress.dismiss();
                        }
                        return;
                    }

                    if(rateObj.has("rate")) {
                        rate = (String)rateObj.get("rate");
                    }
                    else    {
                        rate = "";
                    }

                    if(limitObj.has("limit") && limitObj.has("min")) {
                        min = (String)limitObj.get("min");
                        limit = (String)limitObj.get("limit");
                    }
                    else    {
                        min = "";
                        limit = "";
                    }

//                    Log.i("ShapeShiftActivity", "Rate:" + rate + ", Limit:" + limit + ", Min:" + min);

                    int receiveIdx = HD_WalletFactory.getInstance(ShapeShiftActivity.this).get().getAccount(SamouraiWallet.MIXING_ACCOUNT).getReceive().getAddrIdx();
                    HD_Address addr = HD_WalletFactory.getInstance(ShapeShiftActivity.this).get().getAccount(SamouraiWallet.MIXING_ACCOUNT).getReceive().getAddressAt(receiveIdx);
                    Log.i("ShapeShiftActivity", addr.toString());
                    String txResult = APIFactory.getInstance().normalTx(coin + "_btc", addr.getAddressString(), null);
                    Log.i("ShapeShiftActivity", txResult);

                    final JSONObject txObj = new JSONObject(txResult);

                    if(txObj == null || txObj.has("error")) {

                        Toast.makeText(ShapeShiftActivity.this, txObj.getString("error"), Toast.LENGTH_SHORT).show();

                        if (progress != null && progress.isShowing()) {
                            progress.dismiss();
                        }

                        return;

                    }

//                    Log.i("ShapeShiftActivity", txObj.getString("deposit"));

                    handler.post(new Runnable() {
                        @Override
                        public void run() {

                            try {

                                ((TextView)findViewById(R.id.coin_name)).setText(name);
                                ((TextView)findViewById(R.id.limit)).setText(ShapeShiftActivity.this.getText(R.string.limit) + limit);
                                ((TextView)findViewById(R.id.max)).setText(ShapeShiftActivity.this.getText(R.string.minimum) + min);

                                sendLayout.setVisibility(View.VISIBLE);

                                Bitmap bitmap = null;
                                QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(txObj.getString("deposit"), null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), imgWidth);
                                try {
                                    bitmap = qrCodeEncoder.encodeAsBitmap();
                                } catch (WriterException e) {
                                    e.printStackTrace();
                                }

                                ((ImageView)findViewById(R.id.qr)).setVisibility(View.VISIBLE);
                                ((ImageView)findViewById(R.id.qr)).setImageBitmap(bitmap);
                                ((TextView)findViewById(R.id.send_address)).setText(txObj.getString("deposit"));

                                HD_WalletFactory.getInstance(ShapeShiftActivity.this).get().getAccount(SamouraiWallet.MIXING_ACCOUNT).getReceive().incAddrIdx();

                                Tx tx = new Tx();
                                tx.ts = System.currentTimeMillis() / 1000L;
                                tx.address = txObj.getString("deposit");
                                TxQueue.getInstance(ShapeShiftActivity.this).add(tx);

                                if (progress != null && progress.isShowing()) {
                                    progress.dismiss();
                                }

                            }
                            catch(Exception e) {
                                if (progress != null && progress.isShowing()) {
                                    progress.dismiss();
                                }
                                e.printStackTrace();
                            }

                        }
                    });

                } catch (Exception e) {
                    if (progress != null && progress.isShowing()) {
                        progress.dismiss();
                    }
                    e.printStackTrace();
                }

                Looper.loop();

            }
        }).start();

        if (progress != null && progress.isShowing()) {
            progress.dismiss();
        }

    }

    public class MyCustomAdapter extends ArrayAdapter<String>{

        public MyCustomAdapter(Context context, int textViewResourceId, String[] objects) {
            super(context, textViewResourceId, objects);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        public View getCustomView(int position, View convertView, ViewGroup parent) {
            //return super.getView(position, convertView, parent);

            LayoutInflater inflater = getLayoutInflater();
            View row = inflater.inflate(R.layout.shapeshift_selector_row, parent, false);
            TextView label = (TextView)row.findViewById(R.id.coin);
            label.setText(coins[position]);

            ImageView icon = (ImageView)row.findViewById(R.id.icon);
//            icon.setImageResource(image[position]);
            int idx = coins[position].indexOf("(");
            if(idx == 0) {
                icon.setImageResource(R.drawable.ic_receive_shapeshift);
            }
            else {
                Picasso.with(ShapeShiftActivity.this).load(iconsMap.get(coins[position].substring(idx + 1, coins[position].length() - 1))).into(icon);
            }

            return row;
        }
    }

}
