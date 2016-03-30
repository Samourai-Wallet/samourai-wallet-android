package com.samourai.shapeshift;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.ShapeShiftActivity;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.NotificationsFactory;
import com.samourai.wallet.util.WebUtil;

public class TxQueue	{

    private static Timer timer = null;
    private static Handler handler = null;

    private static Context context = null;

    private static ConcurrentLinkedQueue<Tx> queue = null;

    private static TxQueue instance = null;

    private final static long DEPOSIT_TIMEOUT = 60 * 15;

    private TxQueue()	{ ; }

    public static TxQueue getInstance(Context ctx) {

        context = ctx.getApplicationContext();

        if (instance == null) {

            queue = new ConcurrentLinkedQueue<Tx>();

            instance = new TxQueue();
        }

        return instance;
    }

    public void add(Tx tx)   {
        queue.add(tx);
        doTimer();
    }

    private static void status()   {

        new Thread(new Runnable() {
            @Override
            public void run() {

//                Looper.prepare();

                if(queue.size() > 0) {

                    boolean relaunch = false;

                    ConcurrentLinkedQueue<Tx> _queue = new ConcurrentLinkedQueue<Tx>();

                    Tx tx = null;
                    while(queue.peek() != null)   {
                        tx = queue.poll();
                        long ts = tx.ts;
                        String result = APIFactory.getInstance().txStatus(tx.address);
                        if(result != null)   {
                            Tx _tx = APIFactory.getInstance().txStatusResult(result);
                            _tx.ts = tx.ts;
                            if(_tx != null)   {
                                if(_tx.status.equals("complete"))   {
                                    // notify complete
//                                    Log.i("TxQueue", "Complete:" + _tx.address);
                                    // remove from queue
                                    NotificationsFactory.getInstance(context).setNotification(_tx.address, _tx.address, "Shapeshift transaction complete", R.drawable.ic_launcher, ShapeShiftActivity.class, 1000);

                                    if(!SamouraiWallet.getInstance().getShowTotalBalance())    {
                                        SamouraiWallet.getInstance().setShowTotalBalance(true);
                                        relaunch = true;
                                    }
                                }
                                else if (_tx.status.equals("received"))   {
                                    // notify received
//                                    Log.i("TxQueue", "Received:" + _tx.address);
                                    NotificationsFactory.getInstance(context).setNotification(_tx.address, _tx.address, "Shapeshift transaction received", R.drawable.ic_launcher, ShapeShiftActivity.class, 1000);
                                    _queue.add(_tx);
                                }
                                else if (_tx.status.equals("no_deposits"))   {
                                    // notify no deposit
//                                    Log.i("TxQueue", "No deposits:" + _tx.address);
                                    long now = System.currentTimeMillis() / 1000L;
                                    if(now - ts > DEPOSIT_TIMEOUT)   {
                                        Log.i("TxQueue", "Timeout:" + _tx.address);
                                    }
                                    else    {
//                                        NotificationsFactory.getInstance(context).setNotification(context.getString(R.string.app_name) + ":" + "ShapeShift", "Shapeshift transaction waiting for deposit", _tx.address, R.drawable.ic_launcher, ShapeShiftActivity.class, 1000);
                                        _queue.add(_tx);
                                    }
                                }
                                else   {
                                    ;
                                }
                            }
                        }
                    }

                    queue = _queue;
                    if(queue.size() == 0)   {
                        if(timer != null)   {
                            timer.cancel();
                            timer = null;
                        }
                    }

                    if(relaunch)    {
                        AppUtil.getInstance(context).restartApp();
                    }

                }

//                Looper.loop();

            }
        }).start();

    }

    private void doTimer() {

        if(timer == null) {
            Log.i("TxQueue", "Creating timer");
            timer = new Timer();
            handler = new Handler();

            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            status();
                        }
                    });
                }
            }, 30000, 60000);
        }

    }

}
