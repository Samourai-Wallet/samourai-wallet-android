package com.samourai.wallet;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;
import com.samourai.wallet.util.AppUtil;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.zbar.Symbol;

public class BatchSendActivity extends Activity {

    private final static int SCAN_QR = 2012;

    private class DisplayData   {
        private String addr = null;
        private long amount = 0L;
    }

    private List<DisplayData> data = null;
    private ListView listView = null;
    private BatchAdapter adapter = null;

    private Menu _menu = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batchsend);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        BatchSendActivity.this.getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

        data = new ArrayList<DisplayData>();
        listView = (ListView)findViewById(R.id.list);
        adapter = new BatchAdapter();
        listView.setAdapter(adapter);
        AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                ;
            }
        };

        validateSpend();

    }

    @Override
    public void onResume() {
        super.onResume();
        AppUtil.getInstance(BatchSendActivity.this).checkTimeOut();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        BatchSendActivity.this.getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.batch_menu, menu);

        _menu = menu;

        menu.findItem(R.id.action_refresh).setVisible(false);
        menu.findItem(R.id.action_new).setVisible(false);
        menu.findItem(R.id.action_send).setVisible(false);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // noinspection SimplifiableIfStatement
        if (id == R.id.action_scan_qr) {
            doScan();
        }
        else if (id == R.id.action_new) {
            _menu.findItem(R.id.action_scan_qr).setVisible(true);
            _menu.findItem(R.id.action_refresh).setVisible(false);
            _menu.findItem(R.id.action_new).setVisible(false);
            _menu.findItem(R.id.action_send).setVisible(false);
        }
        else if (id == R.id.action_refresh) {

            data.clear();
            refreshDisplay();

            _menu.findItem(R.id.action_scan_qr).setVisible(true);
            _menu.findItem(R.id.action_refresh).setVisible(false);
            _menu.findItem(R.id.action_new).setVisible(false);
            _menu.findItem(R.id.action_send).setVisible(false);
        }
        else if (id == R.id.action_send) {
            _menu.findItem(R.id.action_scan_qr).setVisible(false);
            _menu.findItem(R.id.action_refresh).setVisible(false);
            _menu.findItem(R.id.action_new).setVisible(false);
            _menu.findItem(R.id.action_send).setVisible(false);
        }
        else {
            ;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(resultCode == Activity.RESULT_OK && requestCode == SCAN_QR)	{

            if(data != null && data.getStringExtra(ZBarConstants.SCAN_RESULT) != null)	{

                final String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT);

                processScan(strResult);

            }
        }
        else if(resultCode == Activity.RESULT_CANCELED && requestCode == SCAN_QR)	{
            ;
        }
        else {
            ;
        }

    }

    private void doScan() {
        Intent intent = new Intent(BatchSendActivity.this, ZBarScannerActivity.class);
        intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{ Symbol.QRCODE } );
        startActivityForResult(intent, SCAN_QR);
    }

    private void validateSpend()    {

        if(true)    {
            if(_menu != null)   {
                _menu.findItem(R.id.action_refresh).setVisible(false);
                _menu.findItem(R.id.action_new).setVisible(false);
                _menu.findItem(R.id.action_send).setVisible(false);
            }
        }

    }

    private void refreshDisplay()   {
        adapter.notifyDataSetInvalidated();
    }

    private void processScan(String data)  {

        if(_menu != null)    {
            _menu.findItem(R.id.action_scan_qr).setVisible(false);
            _menu.findItem(R.id.action_refresh).setVisible(true);
            _menu.findItem(R.id.action_new).setVisible(true);
            _menu.findItem(R.id.action_send).setVisible(true);
        }

    }

    private class BatchAdapter extends BaseAdapter {

        private LayoutInflater inflater = null;

        BatchAdapter() {
            inflater = (LayoutInflater)BatchSendActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public String getItem(int position) {
            return (String)data.get(position).addr;
        }

        @Override
        public long getItemId(int position) {
            return 0L;
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {

            View view = null;

            view = inflater.inflate(R.layout.simple_list_item3, parent, false);

            TextView text1 = (TextView) view.findViewById(R.id.text1);
            TextView text2 = (TextView) view.findViewById(R.id.text2);
            TextView text3 = (TextView) view.findViewById(R.id.text3);

            final DecimalFormat df = new DecimalFormat("#");
            df.setMinimumIntegerDigits(1);
            df.setMinimumFractionDigits(8);
            df.setMaximumFractionDigits(8);

            text1.setText(df.format(((double)(data.get(position).amount) / 1e8)) + " BTC");

            String addr = data.get(position).addr;
            text2.setText(addr);

            return view;
        }

    }

}
