package com.samourai.wallet.bip47;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;
//import android.util.Log;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;

import net.i2p.android.ext.floatingactionbutton.FloatingActionsMenu;
import net.i2p.android.ext.floatingactionbutton.FloatingActionButton;

import net.sourceforge.zbar.Symbol;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.params.MainNetParams;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import com.samourai.wallet.OpCallback;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.rpc.PaymentAddress;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.send.UnspentOutputsBundle;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.R;
import com.samourai.wallet.util.MonetaryUtil;

import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.baoyz.swipemenulistview.SwipeMenuItem;

public class BIP47Activity extends Activity {

    private static final int EDIT_PCODE = 2000;
    private static final int SCAN_PCODE = 2077;

    private SwipeMenuListView listView = null;
    BIP47EntryAdapter adapter = null;
    private String[] pcodes = null;

    private FloatingActionsMenu ibBIP47Menu = null;
    private FloatingActionButton actionAdd = null;
    private FloatingActionButton actionPartners = null;

    private Timer timer = null;
    private Handler handler = null;

    private ProgressDialog progress = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bip47_list);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        ibBIP47Menu = (FloatingActionsMenu)findViewById(R.id.bip47_menu);

        actionAdd = (FloatingActionButton)findViewById(R.id.bip47_add);
        actionAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                doAdd();
            }
        });

        actionPartners = (FloatingActionButton)findViewById(R.id.bip47_partners);
        actionPartners.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {

                AlertDialog.Builder dlg = new AlertDialog.Builder(BIP47Activity.this)
                        .setTitle(R.string.app_name)
                        .setMessage("Want to see your payment code suggested here? Contact us at wallet@samouraiwallet.com.")
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ;
                            }

                        });

                dlg.show();

            }
        });

        listView = (SwipeMenuListView) findViewById(R.id.list);

        handler = new Handler();
        refreshList();

        setDisplay();

        adapter = new BIP47EntryAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final String itemValue = (String) listView.getItemAtPosition(position);
                String msg = "";
                if (BIP47Meta.getInstance().getLabel(itemValue) != null && BIP47Meta.getInstance().getLabel(itemValue).length() > 0) {
                    msg = BIP47Meta.getInstance().getLabel(itemValue) + ":";
                }
//                Toast.makeText(getApplicationContext(), msg + "Outgoing status:" + BIP47Meta.getInstance().getOutgoingStatus(itemValue), Toast.LENGTH_LONG).show();

                if (BIP47Meta.getInstance().getOutgoingStatus(itemValue) == BIP47Meta.STATUS_NOT_SENT) {

                    doNotifTx(itemValue);

                } else if (BIP47Meta.getInstance().getOutgoingStatus(itemValue) == BIP47Meta.STATUS_SENT_NO_CFM) {
                    Toast.makeText(BIP47Activity.this, R.string.bip47_wait_for_confirmation, Toast.LENGTH_SHORT).show();
                } else {

                    AlertDialog.Builder dlg = new AlertDialog.Builder(BIP47Activity.this)
                            .setTitle(R.string.app_name)
                            .setMessage(R.string.bip47_spend_to_pcode)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {

                                    AppUtil.getInstance(BIP47Activity.this).restartApp("pcode", itemValue);

                                }

                            }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ;
                                }
                            });

                    dlg.show();

                }

            }

        });

        listView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final int position, SwipeMenu menu, int index) {

                switch (index) {

                    case 0:

                        Intent intent = new Intent(BIP47Activity.this, BIP47Add.class);
                        intent.putExtra("label", BIP47Meta.getInstance().getLabel(pcodes[position]));
                        intent.putExtra("pcode", pcodes[position]);
                        startActivityForResult(intent, EDIT_PCODE);

                        break;

                    case 1:

                        doSync(pcodes[position]);

                        break;

                    case 2:

                        // archive

                        break;

                }

                return false;
            }
        });

        listView.setLongClickable(true);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {

                int outgoing = BIP47Meta.getInstance().getOutgoingIdx(pcodes[position]);
                int incoming = BIP47Meta.getInstance().getIncomingIdx(pcodes[position]);

                Toast.makeText(BIP47Activity.this, pcodes[position], Toast.LENGTH_SHORT).show();
                Toast.makeText(BIP47Activity.this, "Incoming index:" + incoming + ", Outgoing index:" + outgoing, Toast.LENGTH_SHORT).show();

                return true;
            }
        });

        SwipeMenuCreator creator = new SwipeMenuCreator() {

            @Override
            public void create(SwipeMenu menu) {

                // create "edit" item
                SwipeMenuItem openItem = new SwipeMenuItem(getApplicationContext());
                // set item background
                openItem.setBackground(new ColorDrawable(Color.rgb(0x17, 0x1B, 0x24)));
                // set item width
                openItem.setWidth(180);
                // set a icon
                openItem.setIcon(R.drawable.ic_edit_white_24dp);
                // add to menu
                menu.addMenuItem(openItem);

                // create "sync" item
                SwipeMenuItem syncItem = new SwipeMenuItem(getApplicationContext());
                // set item background
                syncItem.setBackground(new ColorDrawable(Color.rgb(0x17, 0x1B, 0x24)));
                // set item width
                syncItem.setWidth(180);
                // set a icon
                syncItem.setIcon(android.R.drawable.ic_popup_sync);
                // add to menu
                menu.addMenuItem(syncItem);
/*
                // create "archive" item
                SwipeMenuItem archiveItem = new SwipeMenuItem(getApplicationContext());
                // set item background
                archiveItem.setBackground(new ColorDrawable(Color.rgb(0x17, 0x1B, 0x24)));
                // set item width
                archiveItem.setWidth(180);
                // set a icon
                archiveItem.setIcon(android.R.drawable.ic_popup_sync);
                // add to menu
                menu.addMenuItem(syncItem);
*/
            }
        };

        listView.setMenuCreator(creator);
        listView.setSwipeDirection(SwipeMenuListView.DIRECTION_LEFT);

        doTimer();

    }

    @Override
    protected void onResume() {
        super.onResume();

        refreshList();

        setDisplay();

        adapter = new BIP47EntryAdapter();
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

    }

    @Override
    protected void onDestroy() {

        killTimer();

        try {
            HD_WalletFactory.getInstance(BIP47Activity.this).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(BIP47Activity.this).getGUID() + AccessFactory.getInstance(BIP47Activity.this).getPIN()));
        }
        catch(Exception e) {
            ;
        }

        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_OK && requestCode == SCAN_PCODE) {
            if (data != null && data.getStringExtra(ZBarConstants.SCAN_RESULT) != null) {
                String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT);
                processScan(strResult);
            }
        }
        else if (resultCode == Activity.RESULT_CANCELED && requestCode == SCAN_PCODE) {
            ;
        }
        else if (resultCode == Activity.RESULT_OK && requestCode == EDIT_PCODE) {

            if(data.hasExtra("pcode"))    {

                String pcode = data.getStringExtra("pcode");

                if(BIP47Meta.getInstance().getOutgoingStatus(pcode) == BIP47Meta.STATUS_NOT_SENT)    {

                    doNotifTx(pcode);

                }

            }

        }
        else if (resultCode == Activity.RESULT_CANCELED && requestCode == EDIT_PCODE) {
            ;
        }
        else {
            ;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bip47_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if(id == android.R.id.home) {
            finish();
        }
        else if(id == R.id.action_show_qr) {
            Intent intent = new Intent(BIP47Activity.this, BIP47ShowQR.class);
            startActivity(intent);
        }
        else {
            ;
        }

        return super.onOptionsItemSelected(item);
    }

    private void doScan() {
        Intent intent = new Intent(BIP47Activity.this, ZBarScannerActivity.class);
        intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{Symbol.QRCODE});
        startActivityForResult(intent, SCAN_PCODE);
    }

    private void processScan(String data) {

        if(data.startsWith("bitcoin:") && data.length() > 8)    {
            data = data.substring(8);
        }

        if(FormatsUtil.getInstance().isValidPaymentCode(data)) {

            try {
                if(data.equals(BIP47Util.getInstance(BIP47Activity.this).getPaymentCode().toString())) {
                    Toast.makeText(BIP47Activity.this, R.string.bip47_cannot_scan_own_pcode, Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (AddressFormatException afe) {
                ;
            }

            Intent intent = new Intent(BIP47Activity.this, BIP47Add.class);
            intent.putExtra("pcode", data);
            startActivityForResult(intent, EDIT_PCODE);

        } else {
            Toast.makeText(BIP47Activity.this, R.string.scan_error, Toast.LENGTH_SHORT).show();
        }

    }

    private void doAdd() {

        AlertDialog.Builder dlg = new AlertDialog.Builder(BIP47Activity.this)
                .setTitle(R.string.bip47_add1_title)
                .setMessage(R.string.bip47_add1_text)
                .setPositiveButton(R.string.paste, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                        Intent intent = new Intent(BIP47Activity.this, BIP47Add.class);
                        startActivityForResult(intent, EDIT_PCODE);

                    }

                }).setNegativeButton(R.string.scan, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        doScan();
                    }
                });

        dlg.show();

    }

    private void refreshList()  {

        Set<String> _pcodes = BIP47Meta.getInstance().getSortedByLabels();

        //
        // check for own payment code
        //
        try {
            if (_pcodes.contains(BIP47Util.getInstance(BIP47Activity.this).getPaymentCode().toString())) {
                _pcodes.remove(BIP47Util.getInstance(BIP47Activity.this).getPaymentCode().toString());
                BIP47Meta.getInstance().remove(BIP47Util.getInstance(BIP47Activity.this).getPaymentCode().toString());
            }
        } catch (AddressFormatException afe) {
            ;
        }

        pcodes = new String[_pcodes.size()];
        int i = 0;
        for (String s : _pcodes) {
            pcodes[i] = s;
            ++i;
        }

    }

    private void setDisplay()   {

        if(pcodes.length > 0)    {
            listView.setVisibility(View.VISIBLE);
            ((TextView)findViewById(R.id.text1)).setVisibility(View.GONE);
            ((TextView)findViewById(R.id.text2)).setVisibility(View.GONE);
            ((TextView)findViewById(R.id.text3)).setVisibility(View.GONE);
        }
        else    {
            listView.setVisibility(View.GONE);
            ((TextView)findViewById(R.id.text1)).setVisibility(View.VISIBLE);
            ((TextView)findViewById(R.id.text2)).setVisibility(View.VISIBLE);
            ((TextView)findViewById(R.id.text3)).setVisibility(View.VISIBLE);
            ((TextView)findViewById(R.id.text3)).setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    doHelp();
                    return false;
                }
            });
        }

    }

    private void doHelp()  {

        new AlertDialog.Builder(BIP47Activity.this)
                .setTitle(R.string.bip47_setup1_title)
                .setMessage(R.string.bip47_setup1_text)
                .setPositiveButton(R.string.next, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        new AlertDialog.Builder(BIP47Activity.this)
                                .setTitle(R.string.bip47_setup2_title)
                                .setMessage(R.string.bip47_setup2_text)
                                .setPositiveButton(R.string.next, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        new AlertDialog.Builder(BIP47Activity.this)
                                                .setTitle(R.string.bip47_setup2_title)
                                                .setMessage(R.string.bip47_setup3_text)
                                                .setPositiveButton(R.string.next, new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int whichButton) {

                                                        Intent intent = new Intent(BIP47Activity.this, BIP47Add.class);
                                                        intent.putExtra("label", "Samourai Donations");
                                                        intent.putExtra("pcode", "PM8TJVzLGqWR3dtxZYaTWn3xJUop3QP3itR4eYzX7XvV5uAfctEEuHhKNo3zCcqfAbneMhyfKkCthGv5werVbwLruhZyYNTxqbCrZkNNd2pPJA2e2iAh");
                                                        startActivityForResult(intent, EDIT_PCODE);

                                                    }

                                                }).setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {

                                                ;

                                            }
                                        }).show();

                                    }

                                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                ;

                            }
                        }).show();

                    }

                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        ;

                    }
        }).show();

    }

    private void doNotifTx(final String pcode)  {

        try {
            if(APIFactory.getInstance(BIP47Activity.this).getXpubAmounts().get(HD_WalletFactory.getInstance(BIP47Activity.this).get().getAccount(0).xpubstr()) < SendNotifTxFactory._bNotifTxTotalAmount.longValue())    {
                Toast.makeText(BIP47Activity.this, R.string.insufficient_funds, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        catch(IOException ioe) {
            return;
        }
        catch(MnemonicException.MnemonicLengthException mle) {
            return;
        }

        String strNotifTxMsg = getText(R.string.bip47_setup4_text1) + " ";
        long notifAmount = SendNotifTxFactory._bNotifTxTotalAmount.longValue();
        String strAmount = MonetaryUtil.getInstance().getBTCFormat().format(((double) notifAmount) / 1e8) + " BTC ";
        strNotifTxMsg += strAmount + getText(R.string.bip47_setup4_text2);

        AlertDialog.Builder dlg = new AlertDialog.Builder(BIP47Activity.this)
                .setTitle(R.string.bip47_setup4_title)
                .setMessage(strNotifTxMsg)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        new Thread(new Runnable() {
                            @Override
                            public void run() {

                                Looper.prepare();

                                PaymentCode payment_code = null;
                                /*
                                try {
                                    payment_code = new PaymentCode(pcode);
                                }
                                catch (AddressFormatException afe) {
                                    ;
                                }
                                */
                                payment_code = new PaymentCode(pcode);
                                UnspentOutputsBundle unspentCoinsBundle = SendNotifTxFactory.getInstance(BIP47Activity.this).phase1(0);
                                if (unspentCoinsBundle == null) {
                                    Toast.makeText(BIP47Activity.this, R.string.no_confirmed_outputs_available, Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                Pair<Transaction, Long> pair = SendNotifTxFactory.getInstance(BIP47Activity.this).phase2(0, unspentCoinsBundle.getOutputs(), payment_code);
                                if (pair == null) {
                                    Toast.makeText(BIP47Activity.this, R.string.error_creating_tx, Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                SendNotifTxFactory.getInstance(BIP47Activity.this).phase3(pair, 0, payment_code, new OpCallback() {

                                    public void onSuccess() {
                                        Toast.makeText(BIP47Activity.this, R.string.payment_channel_init, Toast.LENGTH_SHORT).show();
                                    }

                                    public void onFail() {
                                        ;
                                    }

                                });

                                Looper.loop();

                            }
                        }).start();

                    }

                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ;
                    }
                });

        dlg.show();

    }

    private class BIP47EntryAdapter extends BaseAdapter {

        private LayoutInflater inflater = null;

        public BIP47EntryAdapter() {
            inflater = (LayoutInflater)BIP47Activity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return pcodes.length;
        }

        @Override
        public Object getItem(int position) {
            return pcodes[position];
        }

        @Override
        public long getItemId(int position) {
            return 0L;
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {

            View view = null;

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater)BIP47Activity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.bip47_entry, null);
            }
            else    {
                view = convertView;
            }

            String strLabel = BIP47Meta.getInstance().getDisplayLabel(pcodes[position]);

            TextView tvInitial = (TextView)view.findViewById(R.id.Initial);
            tvInitial.setText(strLabel.substring(0, 1).toUpperCase());
            if(position % 3 == 0)    {
                tvInitial.setBackgroundResource(R.drawable.ripple_initial_red);
            }
            else if(position % 2 == 1)    {
                tvInitial.setBackgroundResource(R.drawable.ripple_initial_green);
            }
            else {
                tvInitial.setBackgroundResource(R.drawable.ripple_initial_blue);
            }

            TextView tvLabel = (TextView)view.findViewById(R.id.Label);
            tvLabel.setText(strLabel);

            TextView tvLatest = (TextView)view.findViewById(R.id.Latest);
            String strLatest = "";
            if(BIP47Meta.getInstance().getOutgoingStatus(pcodes[position]) == BIP47Meta.STATUS_NOT_SENT) {
                if(BIP47Meta.getInstance().incomingExists(pcodes[position]))    {
                    strLatest = BIP47Activity.this.getText(R.string.bip47_status_incoming) + "\n";
                }
                strLatest += BIP47Activity.this.getText(R.string.bip47_status_tbe);
            }
            else if (BIP47Meta.getInstance().getOutgoingStatus(pcodes[position]) == BIP47Meta.STATUS_SENT_NO_CFM) {
                if(BIP47Meta.getInstance().incomingExists(pcodes[position]))    {
                    strLatest = BIP47Activity.this.getText(R.string.bip47_status_incoming) + "\n";
                }
                strLatest += BIP47Activity.this.getText(R.string.bip47_status_pending);
            }
            else if (BIP47Meta.getInstance().getOutgoingStatus(pcodes[position]) == BIP47Meta.STATUS_SENT_CFM) {
                if(BIP47Meta.getInstance().incomingExists(pcodes[position]))    {
                    strLatest = BIP47Activity.this.getText(R.string.bip47_status_incoming) + "\n";
                }
                strLatest += BIP47Activity.this.getText(R.string.bip47_status_active);
            }
            else {
                ;
            }

            if(BIP47Meta.getInstance().getLatestEvent(pcodes[position]) != null && BIP47Meta.getInstance().getLatestEvent(pcodes[position]).length() > 0)    {
                strLatest += "\n" + BIP47Meta.getInstance().getLatestEvent(pcodes[position]);
            }

            tvLatest.setText(strLatest);

            return view;
        }
    }

    private void killTimer()    {

        if(timer != null) {
            timer.cancel();
            timer = null;
        }

    }

    private void doTimer() {

        if(timer == null) {
            timer = new Timer();

            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {

                            new Thread(new Runnable() {
                                @Override
                                public void run() {

                                    if(refreshDisplay())    {
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                Log.i("BIP47Activity", "updating list");
                                                refreshList();
                                                adapter.notifyDataSetChanged();
                                            }
                                        });
                                    }

                                }
                            }).start();

                        }
                    });
                }
            }, 5000, 30000);

        }

    }

    public boolean refreshDisplay()   {

        Log.i("BIP47Activity", "handling message");

        boolean changed = false;

        //
        // check for incoming payment code notification tx
        //
        int before = BIP47Meta.getInstance().getLabels().size();
        try {
            PaymentCode pcode = BIP47Util.getInstance(BIP47Activity.this).getPaymentCode();
            APIFactory.getInstance(BIP47Activity.this).getNotifAddress(pcode.notificationAddress().getAddressString());
        }
        catch (AddressFormatException afe) {
            afe.printStackTrace();
            Toast.makeText(BIP47Activity.this, "HD wallet error", Toast.LENGTH_SHORT).show();
        }
        int after = BIP47Meta.getInstance().getLabels().size();

        if(before != after)    {
            changed = true;
        }

        //
        // check on outgoing payment code notification tx
        //
        List<org.apache.commons.lang3.tuple.Pair<String, String>> outgoingUnconfirmed = BIP47Meta.getInstance().getOutgoingUnconfirmed();
        for(org.apache.commons.lang3.tuple.Pair<String,String> pair : outgoingUnconfirmed)   {
            //Log.i("BalanceFragment", "outgoing payment code:" + pair.getLeft());
            //Log.i("BalanceFragment", "outgoing payment code tx:" + pair.getRight());
            int confirmations = APIFactory.getInstance(BIP47Activity.this).getNotifTxConfirmations(pair.getRight());
            if(confirmations > 0)    {
                BIP47Meta.getInstance().setOutgoingStatus(pair.getLeft(), BIP47Meta.STATUS_SENT_CFM);
                changed = true;
            }
            if(confirmations == -1)    {
                BIP47Meta.getInstance().setOutgoingStatus(pair.getLeft(), BIP47Meta.STATUS_NOT_SENT);
            }
        }

        return changed;
    }

    private void doSync(final String pcode)    {

        progress = new ProgressDialog(BIP47Activity.this);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage(getString(R.string.please_wait));
        progress.show();

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    PaymentCode payment_code = new PaymentCode(pcode);

                    int idx = 0;
                    boolean loop = true;
                    ArrayList<String> addrs = new ArrayList<String>();
                    while(loop) {
                        addrs.clear();
                        for(int i = idx; i < (idx + 20); i++)   {
                            PaymentAddress receiveAddress = BIP47Util.getInstance(BIP47Activity.this).getReceiveAddress(payment_code, i);
                            Log.i("BIP47Activity", "sync receive from " + i + ":" + receiveAddress.getReceiveECKey().toAddress(MainNetParams.get()).toString());
                            BIP47Meta.getInstance().setIncomingIdx(payment_code.toString(), i, receiveAddress.getReceiveECKey().toAddress(MainNetParams.get()).toString());
                            BIP47Meta.getInstance().getIdx4AddrLookup().put(receiveAddress.getReceiveECKey().toAddress(MainNetParams.get()).toString(), i);
                            BIP47Meta.getInstance().getPCode4AddrLookup().put(receiveAddress.getReceiveECKey().toAddress(MainNetParams.get()).toString(), payment_code.toString());
                            addrs.add(receiveAddress.getReceiveECKey().toAddress(MainNetParams.get()).toString());
                        }
                        String[] s = addrs.toArray(new String[addrs.size()]);
                        int nb = APIFactory.getInstance(BIP47Activity.this).syncBIP47Incoming(s);
//                        Log.i("BIP47Activity", "sync receive idx:" + idx + ", nb == " + nb);
                        if(nb == 0)    {
                            loop = false;
                        }
                        idx += 20;
                    }

                    idx = 0;
                    loop = true;
                    BIP47Meta.getInstance().setOutgoingIdx(pcode, 0);
                    while(loop) {
                        addrs.clear();
                        for(int i = idx; i < (idx + 20); i++)   {
                            PaymentAddress sendAddress = BIP47Util.getInstance(BIP47Activity.this).getSendAddress(payment_code, i);
                            Log.i("BIP47Activity", "sync send to " + i + ":" + sendAddress.getSendECKey().toAddress(MainNetParams.get()).toString());
//                            BIP47Meta.getInstance().setOutgoingIdx(payment_code.toString(), i);
                            BIP47Meta.getInstance().getIdx4AddrLookup().put(sendAddress.getSendECKey().toAddress(MainNetParams.get()).toString(), i);
                            BIP47Meta.getInstance().getPCode4AddrLookup().put(sendAddress.getSendECKey().toAddress(MainNetParams.get()).toString(), payment_code.toString());
                            addrs.add(sendAddress.getSendECKey().toAddress(MainNetParams.get()).toString());
                        }
                        String[] s = addrs.toArray(new String[addrs.size()]);
                        int nb = APIFactory.getInstance(BIP47Activity.this).syncBIP47Outgoing(s);
//                        Log.i("BIP47Activity", "sync send idx:" + idx + ", nb == " + nb);
                        if(nb == 0)    {
                            loop = false;
                        }
                        idx += 20;
                    }

                    BIP47Meta.getInstance().pruneIncoming();

                    HD_WalletFactory.getInstance(BIP47Activity.this).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(BIP47Activity.this).getGUID() + AccessFactory.getInstance(BIP47Activity.this).getPIN()));

                    Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
                    LocalBroadcastManager.getInstance(BIP47Activity.this).sendBroadcast(intent);

                }
                catch(Exception e) {
                    ;
                }

                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                    progress = null;
                }

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        new Thread(new Runnable() {
                            @Override
                            public void run() {

                                if (refreshDisplay()) {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {

                                            Log.i("BIP47Activity", "updating list");
                                            refreshList();
                                            adapter.notifyDataSetChanged();
                                        }
                                    });
                                }

                            }
                        }).start();

                    }
                });

            }
        }).start();

    }

}
