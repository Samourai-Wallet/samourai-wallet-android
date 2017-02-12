package com.samourai.wallet;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.BlockExplorerUtil;
import com.samourai.wallet.util.PrefsUtil;

import org.apache.commons.lang3.tuple.Pair;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class UTXOActivity extends Activity {

    private List<Pair> data = null;
    private ListView listView = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_utxo);
        setTitle(R.string.options_utxo);

        listView = (ListView)findViewById(R.id.list);

        data = new ArrayList<Pair>();
        for(UTXO utxo : APIFactory.getInstance(UTXOActivity.this).getUtxos())   {
            for(MyTransactionOutPoint outpoint : utxo.getOutpoints())   {
                Pair pair = Pair.of(outpoint.getAddress(), outpoint.getValue());
                data.add(pair);
            }
        }

        final DecimalFormat df = new DecimalFormat("#");
        df.setMinimumIntegerDigits(1);
        df.setMinimumFractionDigits(8);
        df.setMaximumFractionDigits(8);

        ArrayAdapter adapter = new ArrayAdapter(UTXOActivity.this, android.R.layout.simple_list_item_2, android.R.id.text1, data) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                View view = super.getView(position, convertView, parent);

                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                text1.setText(data.get(position).getLeft().toString());
                text2.setText(df.format(((double)((BigInteger)data.get(position).getRight()).longValue()) / 1e8) + " BTC");

                return view;
            }
        };
        listView.setAdapter(adapter);
        AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                int sel = PrefsUtil.getInstance(UTXOActivity.this).getValue(PrefsUtil.BLOCK_EXPLORER, 0);
                CharSequence url = BlockExplorerUtil.getInstance().getBlockExplorerAddressUrls()[sel];

                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url + data.get(position).getLeft().toString()));
                startActivity(browserIntent);

            }
        };
        listView.setOnItemClickListener(listener);

    }

    @Override
    public void onResume() {
        super.onResume();
    }

}
