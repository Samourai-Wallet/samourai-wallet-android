package com.samourai.wallet;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.TextView;

import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.TimeOutUtil;

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
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        recoveryWordsGrid = (GridView) findViewById(R.id.grid_recovery_words);
        returnToWallet = (Button) findViewById(R.id.return_to_wallet);
        desclaimerCheckbox = (CheckBox) findViewById(R.id.disclaimer_checkbox);
        String recoveryWords = getIntent().getExtras().getString("BIP39_WORD_LIST");
        assert recoveryWords != null;
        String words[] = recoveryWords.trim().split(" ");
        RecoveryWordGridAdapter adapter = new RecoveryWordGridAdapter(this, words);
        recoveryWordsGrid.setAdapter(adapter);
        desclaimerCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                returnToWallet.setTextColor(b ? getResources().getColor(R.color.accent) : Color.GRAY);
                returnToWallet.setAlpha(b ? 1 : 0.6f);
                returnToWallet.setClickable(b);
                returnToWallet.setFocusable(b);
            }
        });

        returnToWallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AccessFactory.getInstance(RecoveryWordsActivity.this).setIsLoggedIn(true);
                TimeOutUtil.getInstance().updatePin();
                AppUtil.getInstance(RecoveryWordsActivity.this).restartApp();
            }
        });
        returnToWallet.setTextColor(Color.GRAY);
        returnToWallet.setAlpha(0.6f);
        returnToWallet.setClickable(false);
        returnToWallet.setFocusable(false);
    }

    private class RecoveryWordGridAdapter extends BaseAdapter {

        private Context mContext;
        private String mWords[];

        RecoveryWordGridAdapter(Context context, String words[]) {
            this.mContext = context;
            this.mWords = words;
        }

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
            holder.word.setText(this.mWords[position].trim());
            holder.number.setText(String.valueOf(position + 1));
            return convertview;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }


    }

    private static class ViewHolder {
        private TextView number;
        private TextView word;
    }
}
