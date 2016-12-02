package com.samourai.wallet.bip47;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
//import android.util.Log;

import com.samourai.wallet.util.WebUtil;
import com.samourai.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class BIP47Recommended extends Activity {

    private ListView listView = null;
    private ArrayAdapter adapter = null;
    private String[] pcodes = null;

    private ArrayList<Recommended> recommended = null;

    private Handler handler = null;

    private ProgressDialog progress = null;

    private class Recommended   {
        String label;
        String pcode;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bip47_recommended_list);

        setTitle(R.string.bip47_recommended);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        listView = (ListView) findViewById(R.id.list);

        handler = new Handler();

        recommended = new ArrayList<Recommended>();
        Recommended r1 = new Recommended();
        r1.label = "Samourai Donations";
        r1.pcode = "PM8TJVzLGqWR3dtxZYaTWn3xJUop3QP3itR4eYzX7XvV5uAfctEEuHhKNo3zCcqfAbneMhyfKkCthGv5werVbwLruhZyYNTxqbCrZkNNd2pPJA2e2iAh";
        recommended.add(r1);
        Recommended r2 = new Recommended();
        r2.label = "OBPP Donations";
        r2.pcode = "PM8TJfWE5j2dborjh8CT9VN4K4dUxN2a3qugNeWeydkYynvanU5257CZQgcuYEEha8xca8oqv7AJYbBaypLMmMAVtTU4XRQ17xbjRiypVt3rUib3PcUd";
        recommended.add(r2);

        adapter = new ArrayAdapter(BIP47Recommended.this, android.R.layout.simple_list_item_2, android.R.id.text1, recommended) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                View view = super.getView(position, convertView, parent);

                TextView tvLabel = (TextView) view.findViewById(android.R.id.text1);
                TextView tvPCode = (TextView) view.findViewById(android.R.id.text2);

                tvLabel.setText(recommended.get(position).label);
                tvPCode.setText(BIP47Meta.getInstance().getAbbreviatedPcode(recommended.get(position).pcode));

                return view;
            }
        };
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Recommended itemValue = (Recommended) listView.getItemAtPosition(position);

                if(BIP47Meta.getInstance().getLabel(itemValue.pcode) != null && BIP47Meta.getInstance().getLabel(itemValue.pcode).length() > 0)    {
                    Toast.makeText(getApplicationContext(), R.string.bip47_already_has_recommended, Toast.LENGTH_LONG).show();
                }
                else    {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("label", itemValue.label);
                    resultIntent.putExtra("pcode", itemValue.pcode);
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                }

            }

        });

        refreshDisplay();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if(id == android.R.id.home) {
            finish();
        }
        else {
            ;
        }

        return super.onOptionsItemSelected(item);
    }

    private void refreshDisplay()    {

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    String response = WebUtil.getInstance(null).getURL(WebUtil.RECOMMENDED_BIP47_URL);
                    JSONObject jsonObject = new JSONObject(response);
                    parse(jsonObject);

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();
                        }
                    });

                }
                catch(JSONException je) {
                    je.printStackTrace();
                }
                catch(Exception e) {
                    ;
                }
                finally {
                    ;
                }

            }
        }).start();
    }

    private void parse(JSONObject obj)    {

        JSONArray recs = null;
        try {
            recs = obj.getJSONArray("recommended");
        }
        catch(JSONException je) {
            je.printStackTrace();
        }

        if(recs != null && recs.length() > 0)    {

            recommended.clear();

            for(int i = 0; i < recs.length(); i++)   {

                try {
                    JSONObject rec = recs.getJSONObject(i);

                    Recommended r = new Recommended();
                    r.label = rec.getString("title");
                    r.pcode = rec.getString("pcode");
                    recommended.add(r);
                }
                catch(JSONException je) {
                    je.printStackTrace();
                }

            }

        }

    }

}
