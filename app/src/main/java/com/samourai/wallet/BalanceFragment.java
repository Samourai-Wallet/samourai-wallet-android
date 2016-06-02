package com.samourai.wallet;

import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnticipateInterpolator;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
//import android.util.Log;

import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.crypto.MnemonicException;

import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.api.Tx;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.bip47.rpc.*;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.BlockExplorerUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.DateUtil;
import com.samourai.wallet.util.ExchangeRateFactory;
import com.samourai.wallet.util.MonetaryUtil;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.TypefaceUtil;

import org.bitcoinj.core.Coin;
import org.json.JSONException;

import net.i2p.android.ext.floatingactionbutton.FloatingActionButton;
import net.i2p.android.ext.floatingactionbutton.FloatingActionsMenu;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class BalanceFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private ProgressDialog progress = null;

    private LinearLayout tvBalanceBar = null;
    private TextView tvBalanceAmount = null;
    private TextView tvBalanceUnits = null;

    private ListView txList = null;
    private List<Tx> txs = null;
    private HashMap<String, Boolean> txStates = null;
    private TransactionAdapter txAdapter = null;

    private FloatingActionsMenu ibQuickSend = null;
    private FloatingActionButton actionReceive = null;
    private FloatingActionButton actionSend = null;
    private FloatingActionButton actionShapeShift = null;
    private FloatingActionButton actionBIP47 = null;

    private boolean isBTC = true;

    private int refreshed = 0;

    private Activity thisActivity = null;

    public static BalanceFragment newInstance(int sectionNumber) {
        BalanceFragment fragment = new BalanceFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public static final String ACTION_INTENT = "com.samourai.wallet.BalanceFragment.REFRESH";

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {

            if(ACTION_INTENT.equals(intent.getAction())) {

                if(isAdded())    {

                    final boolean notifTx = intent.getBooleanExtra("notifTx", false);
                    final boolean fetch = intent.getBooleanExtra("fetch", false);

                    final String rbfHash;
                    if(intent.hasExtra("rbf"))    {
                        rbfHash = intent.getStringExtra("rbf");
                    }
                    else    {
                        rbfHash = null;
                    }

                    thisActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvBalanceAmount.setText("");
                            tvBalanceUnits.setText("");
                            refreshTx(notifTx, fetch);

                            if(thisActivity != null)    {

                                try {
                                    HD_WalletFactory.getInstance(thisActivity).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(thisActivity).getGUID() + AccessFactory.getInstance(thisActivity).getPIN()));
                                }
                                catch(MnemonicException.MnemonicLengthException mle) {
                                    ;
                                }
                                catch(JSONException je) {
                                    ;
                                }
                                catch(IOException ioe) {
                                    ;
                                }

                                if(rbfHash != null)    {
                                    new AlertDialog.Builder(thisActivity)
                                            .setTitle(R.string.app_name)
                                            .setMessage(rbfHash + "\n\n" + getString(R.string.rbf_incoming))
                                            .setCancelable(true)
                                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {

                                                    doExplorerView(rbfHash);

                                                }
                                            })
                                            .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    ;
                                                }
                                            }).show();

                                }

                            }

                        }
                    });

                }

            }

        }
    };

    public BalanceFragment() {
        ;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_balance, container, false);

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            rootView.setBackgroundColor(getActivity().getResources().getColor(R.color.divider));
        }

        rootView.setFilterTouchesWhenObscured(true);

        getActivity().getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        thisActivity = getActivity();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tvBalanceBar = (LinearLayout)inflater.inflate(R.layout.balance_layout, container, false);
        } else {
            tvBalanceBar = (LinearLayout)inflater.inflate(R.layout.balance_layout, null, false);
        }
        tvBalanceBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(isBTC)    {
                    isBTC = false;
                }
                else    {
                    isBTC = true;
                }
                displayBalance();
                txAdapter.notifyDataSetChanged();
                return false;
            }
        });
        tvBalanceAmount = (TextView)tvBalanceBar.findViewById(R.id.BalanceAmount);
        tvBalanceUnits = (TextView)tvBalanceBar.findViewById(R.id.BalanceUnits);

        ibQuickSend = (FloatingActionsMenu)rootView.findViewById(R.id.wallet_menu);
        actionSend = (FloatingActionButton)rootView.findViewById(R.id.send);
        actionSend.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {

                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction().add(R.id.container, SendFragment.newInstance(2)).addToBackStack("Send").commit();

            }
        });

        actionReceive = (FloatingActionButton)rootView.findViewById(R.id.receive);
        actionReceive.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {

                if(SamouraiWallet.getInstance().getCurrentSelectedAccount() == 2 ||
                        (SamouraiWallet.getInstance().getCurrentSelectedAccount() == 0 && SamouraiWallet.getInstance().getShowTotalBalance())
                        )    {

                    new AlertDialog.Builder(thisActivity)
                            .setTitle(R.string.app_name)
                            .setMessage(R.string.receive2samourai)
                            .setCancelable(false)
                            .setPositiveButton(R.string.generate_receive_yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    FragmentManager fragmentManager = getFragmentManager();
                                    fragmentManager.beginTransaction().add(R.id.container, ReceiveFragment.newInstance(1)).addToBackStack("Receive").commit();
                                }
                            })
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ;
                                }
                            }).show();

                }
                else    {
                    FragmentManager fragmentManager = getFragmentManager();
                    fragmentManager.beginTransaction().add(R.id.container, ReceiveFragment.newInstance(1)).addToBackStack("Receive").commit();
                }

            }
        });

        actionShapeShift = (FloatingActionButton)rootView.findViewById(R.id.shapeshift);
        actionShapeShift.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent(getActivity(), ShapeShiftActivity.class);
                startActivity(intent);
            }
        });

        actionBIP47 = (FloatingActionButton)rootView.findViewById(R.id.bip47);
        actionBIP47.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent(getActivity(), com.samourai.wallet.bip47.BIP47Activity.class);
                startActivity(intent);
            }
        });

        txList = (ListView)rootView.findViewById(R.id.txList);
        txAdapter = new TransactionAdapter();
        txList.setAdapter(txAdapter);
        txList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {

                if(position == 0) {
                    return;
                }

                long viewId = view.getId();
                View v = (View)view.getParent();
                Tx tx = txs.get(position - 1);
                ImageView ivTxStatus = (ImageView)v.findViewById(R.id.TransactionStatus);
                TextView tvConfirmationCount = (TextView)v.findViewById(R.id.ConfirmationCount);

                if(viewId == R.id.ConfirmationCount || viewId == R.id.TransactionStatus) {

                    if(txStates.containsKey(tx.getHash()) && txStates.get(tx.getHash()) == true) {
                        txStates.put(tx.getHash(), false);
                        displayTxStatus(false, tx.getConfirmations(), tvConfirmationCount, ivTxStatus);
                    }
                    else {
                        txStates.put(tx.getHash(), true);
                        displayTxStatus(true, tx.getConfirmations(), tvConfirmationCount, ivTxStatus);
                    }

                }
                else {

                    doExplorerView(tx.getHash());

                }

            }
        });

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            txList.setDivider(new ColorDrawable(R.color.divider));
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(ACTION_INTENT);
        LocalBroadcastManager.getInstance(thisActivity).registerReceiver(receiver, filter);

        getActivity().getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        if(getArguments() != null)    {
            if(getArguments().getBoolean("notifTx"))    {
                refresh(true);
            }
            else    {
                refresh(false);
            }
        }

    }

    @Override
    public void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(thisActivity).unregisterReceiver(receiver);

        ibQuickSend.collapse();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity2) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
    }

    @Override
    public void onViewStateRestored (Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
    }

    private class TransactionAdapter extends BaseAdapter {

        private LayoutInflater inflater = null;
        private static final int TYPE_ITEM = 0;
        private static final int TYPE_BALANCE = 1;

        TransactionAdapter() {
            inflater = (LayoutInflater)thisActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            if(txs == null) {
                txs = new ArrayList<Tx>();
                txStates = new HashMap<String, Boolean>();
            }
            return txs.size() + 1;
        }

        @Override
        public String getItem(int position) {
            if(txs == null) {
                txs = new ArrayList<Tx>();
                txStates = new HashMap<String, Boolean>();
            }
            if(position == 0) {
                return "";
            }
            return txs.get(position - 1).toString();
        }

        @Override
        public long getItemId(int position) {
            return position - 1;
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? TYPE_BALANCE : TYPE_ITEM;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {

            View view = null;

            int type = getItemViewType(position);
            if(convertView == null) {
                if(type == TYPE_BALANCE) {
                    view = tvBalanceBar;
                }
                else {
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        view = inflater.inflate(R.layout.tx_layout_simple, parent, false);
                    }
                    else {
                        view = inflater.inflate(R.layout.tx_layout_simple_compat, parent, false);
                    }
                }
            }
            else {
                view = convertView;
            }

            if(type == TYPE_BALANCE) {
                ;
            }
            else {
                view.findViewById(R.id.TransactionStatus).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((ListView)parent).performItemClick(v, position, 0);
                    }
                });

                view.findViewById(R.id.ConfirmationCount).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((ListView)parent).performItemClick(v, position, 0);
                    }
                });

                Tx tx = txs.get(position - 1);

                TextView tvTodayLabel = (TextView)view.findViewById(R.id.TodayLabel);
                String strDateGroup = DateUtil.getInstance(thisActivity).group(tx.getTS());
                if(position == 1) {
                    tvTodayLabel.setText(strDateGroup);
                    tvTodayLabel.setVisibility(View.VISIBLE);
                }
                else {
                    Tx prevTx = txs.get(position - 2);
                    String strPrevDateGroup = DateUtil.getInstance(thisActivity).group(prevTx.getTS());

                    if(strPrevDateGroup.equals(strDateGroup)) {
                        tvTodayLabel.setVisibility(View.GONE);
                    }
                    else {
                        tvTodayLabel.setText(strDateGroup);
                        tvTodayLabel.setVisibility(View.VISIBLE);
                    }
                }

                String strDetails = null;
                String strTS = DateUtil.getInstance(thisActivity).formatted(tx.getTS());
                long _amount = 0L;
                if(tx.getAmount() < 0.0) {
                    _amount = Math.abs((long)tx.getAmount());
                    strDetails = thisActivity.getString(R.string.you_sent);
                }
                else {
                    _amount = (long)tx.getAmount();
                    strDetails = thisActivity.getString(R.string.you_received);
                }
                String strAmount = null;
                String strUnits = null;
                if(isBTC)    {
                    strAmount = getBTCDisplayAmount(_amount);
                    strUnits = getBTCDisplayUnits();
                }
                else    {
                    strAmount = getFiatDisplayAmount(_amount);
                    strUnits = getFiatDisplayUnits();
                }

                TextView tvDirection = (TextView)view.findViewById(R.id.TransactionDirection);
                TextView tvDirection2 = (TextView)view.findViewById(R.id.TransactionDirection2);
                TextView tvDetails = (TextView)view.findViewById(R.id.TransactionDetails);
                ImageView ivTxStatus = (ImageView)view.findViewById(R.id.TransactionStatus);
                TextView tvConfirmationCount = (TextView)view.findViewById(R.id.ConfirmationCount);

                tvDirection.setTypeface(TypefaceUtil.getInstance(thisActivity).getAwesomeTypeface());
                if(tx.getAmount() < 0.0) {
                    tvDirection.setTextColor(Color.RED);
                    tvDirection.setText(Character.toString((char) TypefaceUtil.awesome_arrow_up));
                }
                else {
                    tvDirection.setTextColor(Color.GREEN);
                    tvDirection.setText(Character.toString((char) TypefaceUtil.awesome_arrow_down));
                }

                if(txStates.containsKey(tx.getHash()) && txStates.get(tx.getHash()) == false) {
                    txStates.put(tx.getHash(), false);
                    displayTxStatus(false, tx.getConfirmations(), tvConfirmationCount, ivTxStatus);
                }
                else {
                    txStates.put(tx.getHash(), true);
                    displayTxStatus(true, tx.getConfirmations(), tvConfirmationCount, ivTxStatus);
                }

                tvDirection2.setText(strDetails + " " + strAmount + " " + strUnits);
                if(tx.getPaymentCode() != null)    {
                    String strTaggedTS = strTS + " ";
                    String strSubText = " " + BIP47Meta.getInstance().getDisplayLabel(tx.getPaymentCode()) + " ";
                    strTaggedTS += strSubText;
                    tvDetails.setText(strTaggedTS);
                } else {
                    tvDetails.setText(strTS);
                }
            }

            return view;
        }

    }

    private void refreshTx(final boolean notifTx, final boolean fetch) {

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                if(notifTx)    {
                    //
                    // check for incoming payment code notification tx
                    //
                    try {
                        PaymentCode pcode = BIP47Util.getInstance(thisActivity).getPaymentCode();
                        APIFactory.getInstance(thisActivity).getNotifAddress(pcode.notificationAddress().getAddressString());
//                    Log.i("BalanceFragment", "payment code:" + pcode.toString());
//                    Log.i("BalanceFragment", "notification address:" + pcode.notificationAddress().getAddressString());
                    }
                    catch (AddressFormatException afe) {
                        afe.printStackTrace();
                        Toast.makeText(getActivity(), "HD wallet error", Toast.LENGTH_SHORT).show();
                    }

                    //
                    // check on outgoing payment code notification tx
                    //
                    List<Pair<String,String>> outgoingUnconfirmed = BIP47Meta.getInstance().getOutgoingUnconfirmed();
//                Log.i("BalanceFragment", "outgoingUnconfirmed:" + outgoingUnconfirmed.size());
                    for(Pair<String,String> pair : outgoingUnconfirmed)   {
//                    Log.i("BalanceFragment", "outgoing payment code:" + pair.getLeft());
//                    Log.i("BalanceFragment", "outgoing payment code tx:" + pair.getRight());
                        int confirmations = APIFactory.getInstance(thisActivity).getNotifTxConfirmations(pair.getRight());
                        if(confirmations > 0)    {
                            BIP47Meta.getInstance().setOutgoingStatus(pair.getLeft(), BIP47Meta.STATUS_SENT_CFM);
                        }
                        if(confirmations == -1)    {
                            BIP47Meta.getInstance().setOutgoingStatus(pair.getLeft(), BIP47Meta.STATUS_NOT_SENT);
                        }
                    }
                }

                //
                // TBD: check on lookahead/lookbehind for all incoming payment codes
                //
                if(fetch || txs.size() == 0)    {
                    APIFactory.getInstance(thisActivity).initWalletAmounts();
                }

                try {
                    int acc = 0;
                    if(SamouraiWallet.getInstance().getShowTotalBalance())    {
                        if(SamouraiWallet.getInstance().getCurrentSelectedAccount() == 0)    {
                            txs = APIFactory.getInstance(thisActivity).getAllXpubTxs();
                        }
                        else    {
                            acc = SamouraiWallet.getInstance().getCurrentSelectedAccount() - 1;
                            txs = APIFactory.getInstance(thisActivity).getXpubTxs().get(HD_WalletFactory.getInstance(getActivity()).get().getAccount(acc).xpubstr());
                        }
                    }
                    else    {
                        txs = APIFactory.getInstance(thisActivity).getXpubTxs().get(HD_WalletFactory.getInstance(getActivity()).get().getAccount(acc).xpubstr());
                    }
                    if(txs != null)    {
                        Collections.sort(txs, new APIFactory.TxMostRecentDateComparator());
                    }

                    if(AddressFactory.getInstance().getHighestTxReceiveIdx(acc) > HD_WalletFactory.getInstance(getActivity()).get().getAccount(acc).getReceive().getAddrIdx()) {
                        HD_WalletFactory.getInstance(getActivity()).get().getAccount(acc).getReceive().setAddrIdx(AddressFactory.getInstance().getHighestTxReceiveIdx(acc));
                    }
                    if(AddressFactory.getInstance().getHighestTxChangeIdx(acc) > HD_WalletFactory.getInstance(getActivity()).get().getAccount(acc).getChange().getAddrIdx()) {
                        HD_WalletFactory.getInstance(getActivity()).get().getAccount(acc).getChange().setAddrIdx(AddressFactory.getInstance().getHighestTxChangeIdx(acc));
                    }
                }
                catch(IOException ioe) {
                    ioe.printStackTrace();
                }
                catch(MnemonicException.MnemonicLengthException mle) {
                    mle.printStackTrace();
                }
                finally {
                    ;
                }

                handler.post(new Runnable() {
                    public void run() {
                        tvBalanceAmount.setText("");
                        tvBalanceUnits.setText("");
                        displayBalance();
                        txAdapter.notifyDataSetChanged();
                    }
                });

                PrefsUtil.getInstance(thisActivity).setValue(PrefsUtil.FIRST_RUN, false);

                if(notifTx)    {
                    Intent intent = new Intent("com.samourai.wallet.MainActivity2.RESTART_SERVICE");
                    LocalBroadcastManager.getInstance(thisActivity).sendBroadcast(intent);
                }

                Looper.loop();

            }
        }).start();

    }

    private void displayBalance() {
        String strFiat = PrefsUtil.getInstance(thisActivity).getValue(PrefsUtil.CURRENT_FIAT, "USD");
        double btc_fx = ExchangeRateFactory.getInstance(thisActivity).getAvgPrice(strFiat);

        long balance = 0L;
        if(SamouraiWallet.getInstance().getShowTotalBalance())    {
            if(SamouraiWallet.getInstance().getCurrentSelectedAccount() == 0)    {
                balance = APIFactory.getInstance(thisActivity).getXpubBalance();
            }
            else    {
                if(APIFactory.getInstance(thisActivity).getXpubAmounts().size() > 0)    {
                    try    {
                        if(APIFactory.getInstance(thisActivity).getXpubAmounts().get(HD_WalletFactory.getInstance(getActivity()).get().getAccount(SamouraiWallet.getInstance().getCurrentSelectedAccount() - 1).xpubstr()) != null)    {
                            balance = APIFactory.getInstance(thisActivity).getXpubAmounts().get(HD_WalletFactory.getInstance(getActivity()).get().getAccount(SamouraiWallet.getInstance().getCurrentSelectedAccount() - 1).xpubstr());
                        }
                    }
                    catch(IOException ioe)    {
                        ;
                    }
                    catch(MnemonicException.MnemonicLengthException mle)    {
                        ;
                    }
                }
            }
        }
        else    {
            if(APIFactory.getInstance(thisActivity).getXpubAmounts().size() > 0)    {
                try    {
                    if(APIFactory.getInstance(thisActivity).getXpubAmounts().get(HD_WalletFactory.getInstance(getActivity()).get().getAccount(SamouraiWallet.getInstance().getCurrentSelectedAccount()).xpubstr()) != null)    {
                        balance = APIFactory.getInstance(thisActivity).getXpubAmounts().get(HD_WalletFactory.getInstance(getActivity()).get().getAccount(SamouraiWallet.SAMOURAI_ACCOUNT).xpubstr());
                    }
                }
                catch(IOException ioe)    {
                    ;
                }
                catch(MnemonicException.MnemonicLengthException mle)    {
                    ;
                }
            }
        }
        double btc_balance = (((double)balance) / 1e8);
        double fiat_balance = btc_fx * btc_balance;

        if(isBTC) {
            tvBalanceAmount.setText(getBTCDisplayAmount(balance));
            tvBalanceUnits.setText(getBTCDisplayUnits());
        }
        else {
            tvBalanceAmount.setText(MonetaryUtil.getInstance().getFiatFormat(strFiat).format(fiat_balance));
            tvBalanceUnits.setText(strFiat);
        }

    }

    private String getBTCDisplayAmount(long value) {

        String strAmount = null;
        DecimalFormat df = new DecimalFormat("#");
        df.setMinimumIntegerDigits(1);
        df.setMinimumFractionDigits(1);
        df.setMaximumFractionDigits(8);

        int unit = PrefsUtil.getInstance(thisActivity).getValue(PrefsUtil.BTC_UNITS, MonetaryUtil.UNIT_BTC);
        switch(unit) {
            case MonetaryUtil.MICRO_BTC:
                strAmount = df.format(((double)(value * 1000000L)) / 1e8);
                break;
            case MonetaryUtil.MILLI_BTC:
                strAmount = df.format(((double)(value * 1000L)) / 1e8);
                break;
            default:
                strAmount = Coin.valueOf(value).toPlainString();
                break;
        }

        return strAmount;
    }

    private String getBTCDisplayUnits() {

        return (String) MonetaryUtil.getInstance().getBTCUnits()[PrefsUtil.getInstance(thisActivity).getValue(PrefsUtil.BTC_UNITS, MonetaryUtil.UNIT_BTC)];

    }

    private String getFiatDisplayAmount(long value) {

        String strFiat = PrefsUtil.getInstance(thisActivity).getValue(PrefsUtil.CURRENT_FIAT, "USD");
        double btc_fx = ExchangeRateFactory.getInstance(thisActivity).getAvgPrice(strFiat);
        String strAmount = MonetaryUtil.getInstance().getFiatFormat(strFiat).format(btc_fx * (((double)value) / 1e8));

        return strAmount;
    }

    private String getFiatDisplayUnits() {

        return PrefsUtil.getInstance(thisActivity).getValue(PrefsUtil.CURRENT_FIAT, "USD");

    }

    private void displayTxStatus(boolean heads, long confirmations, TextView tvConfirmationCount, ImageView ivTxStatus)	{

        if(heads)	{
            if(confirmations == 0) {
                rotateTxStatus(tvConfirmationCount, true);
                ivTxStatus.setVisibility(View.VISIBLE);
                ivTxStatus.setImageResource(R.drawable.ic_query_builder_white);
                tvConfirmationCount.setVisibility(View.GONE);
            }
            else if(confirmations > 3) {
                rotateTxStatus(tvConfirmationCount, true);
                ivTxStatus.setVisibility(View.VISIBLE);
                ivTxStatus.setImageResource(R.drawable.ic_done_white);
                tvConfirmationCount.setVisibility(View.GONE);
            }
            else {
                rotateTxStatus(ivTxStatus, false);
                tvConfirmationCount.setVisibility(View.VISIBLE);
                tvConfirmationCount.setText(Long.toString(confirmations));
                ivTxStatus.setVisibility(View.GONE);
            }
        }
        else	{
            if(confirmations < 100) {
                rotateTxStatus(ivTxStatus, false);
                tvConfirmationCount.setVisibility(View.VISIBLE);
                tvConfirmationCount.setText(Long.toString(confirmations));
                ivTxStatus.setVisibility(View.GONE);
            }
            else    {
                rotateTxStatus(ivTxStatus, false);
                tvConfirmationCount.setVisibility(View.VISIBLE);
                tvConfirmationCount.setText("\u221e");
                ivTxStatus.setVisibility(View.GONE);
            }
        }

    }

    private void rotateTxStatus(View view, boolean clockwise)	{

        float degrees = 360f;
        if(!clockwise)	{
            degrees = -360f;
        }

        ObjectAnimator animation = ObjectAnimator.ofFloat(view, "rotationY", 0.0f, degrees);
        animation.setDuration(1000);
        animation.setRepeatCount(0);
        animation.setInterpolator(new AnticipateInterpolator());
        animation.start();
    }

    private void refresh(boolean notifTx)  {

        if(refreshed % 2 == 0)    {

            refreshed++;

            try {
                if(HD_WalletFactory.getInstance(getActivity()).get() != null)    {
                    refreshTx(notifTx, false);
                }
            }
            catch(IOException ioe) {
                ;
            }
            catch(MnemonicException.MnemonicLengthException mle) {
                ;
            }
        }
    }

    private void doExplorerView(String strHash)   {

        if(strHash != null) {
            int sel = PrefsUtil.getInstance(thisActivity).getValue(PrefsUtil.BLOCK_EXPLORER, 0);
            CharSequence url = BlockExplorerUtil.getInstance().getBlockExplorerUrls()[sel];

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url + strHash));
            startActivity(browserIntent);
        }

    }

}
