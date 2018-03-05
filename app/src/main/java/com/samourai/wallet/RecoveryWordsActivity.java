package com.samourai.wallet;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.TextView;

public class RecoveryWordsActivity extends Activity {
    private GridView recoveryWordsGrid;
    private Button returnToWallet;
    private CheckBox desclaimerCheckbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recovery_words);
        if (getActionBar() != null) {
            getActionBar().hide();
        }
        recoveryWordsGrid = (GridView) findViewById(R.id.grid_recovery_words);
        returnToWallet = (Button) findViewById(R.id.return_to_wallet);
        returnToWallet.setTextColor(Color.GRAY);
        returnToWallet.setClickable(false);
        desclaimerCheckbox = (CheckBox) findViewById(R.id.disclaimer_checkbox);
        String words[] = {"machine", "marine", "mountain", "document", "mom"};
        RecoveryWordGridAdapter adapter = new RecoveryWordGridAdapter(this, words);
        recoveryWordsGrid.setAdapter(adapter);

        desclaimerCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                returnToWallet.setTextColor(b ? getResources().getColor(R.color.accent) : Color.GRAY);
                returnToWallet.setClickable(b);
            }
        });

    }

    private class RecoveryWordGridAdapter extends BaseAdapter {

        private Context mContext;
        private String mWords[];

        // 1
        RecoveryWordGridAdapter(Context context, String words[]) {
            this.mContext = context;
            this.mWords = words;
        }

        // 2
        @Override
        public int getCount() {
            return this.mWords.length;
        }


        @Override
        public View getView(int position, View convertview, ViewGroup viewGroup) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            ViewHolder holder;
            if (convertview == null) {
                convertview = inflater.inflate(R.layout.word_grid_item_view, null);
                holder = new ViewHolder();
                holder.number = (TextView) convertview.findViewById(R.id.index_grid_item);
                holder.word = (TextView) convertview.findViewById(R.id.word_grid_item);
                convertview.setTag(holder);
            } else {
                holder = (ViewHolder) convertview.getTag();
            }
            holder.word.setText(this.mWords[position]);
            holder.number.setText(String.valueOf(position + 1));
            return convertview;
        }

        // 4
        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }


    }

    static class ViewHolder {
        private TextView number;
        private TextView word;
    }
}
