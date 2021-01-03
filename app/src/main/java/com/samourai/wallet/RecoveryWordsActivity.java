package com.samourai.wallet;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
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

import org.w3c.dom.Text;

public class RecoveryWordsActivity extends Activity {
    private GridView recoveryWordsGrid;
    private Button returnToWallet;
    private CheckBox disclaimerCheckbox;
    private TextView disclaimerText;
    private Boolean accepted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recovery_words);
        if (getActionBar() != null) {
            getActionBar().hide();
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        recoveryWordsGrid = findViewById(R.id.grid_recovery_words);
        returnToWallet = findViewById(R.id.return_to_wallet);
        disclaimerCheckbox = findViewById(R.id.disclaimer_checkbox);
        disclaimerText = findViewById(R.id.declaimer_recovery_words_text);
        String recoveryWords = getIntent().getExtras().getString("BIP39_WORD_LIST");
        assert recoveryWords != null;
        String words[] = recoveryWords.trim().split(" ");
        RecoveryWordGridAdapter adapter = new RecoveryWordGridAdapter(this, words);
        recoveryWordsGrid.setAdapter(adapter);
        disclaimerCheckbox.setOnCheckedChangeListener((compoundButton, b) -> {
            accepted = b;
            setDisclaimerChange();
        });
        disclaimerText.setOnClickListener(v -> {
            accepted = !accepted;
            disclaimerCheckbox.setChecked(accepted);
            setDisclaimerChange();
        });

        returnToWallet.setOnClickListener(view -> {
            AccessFactory.getInstance(RecoveryWordsActivity.this).setIsLoggedIn(true);
            TimeOutUtil.getInstance().updatePin();
            AppUtil.getInstance(RecoveryWordsActivity.this).restartApp();
        });
        returnToWallet.setTextColor(Color.GRAY);
        returnToWallet.setAlpha(0.6f);
        returnToWallet.setClickable(false);
        returnToWallet.setFocusable(false);

        //set grid no of Columns based on display density
        int densityDpi = getResources().getDisplayMetrics().densityDpi;
        if (densityDpi <= DisplayMetrics.DENSITY_MEDIUM) {
            recoveryWordsGrid.setNumColumns(2);
        }

    }

    private void setDisclaimerChange() {
        returnToWallet.setTextColor(accepted ? getResources().getColor(R.color.accent) : Color.GRAY);
        returnToWallet.setAlpha(accepted ? 1 : 0.6f);
        returnToWallet.setClickable(accepted);
        returnToWallet.setFocusable(accepted);
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
                holder.number = convertview.findViewById(R.id.index_grid_item);
                holder.word = convertview.findViewById(R.id.word_grid_item);
                convertview.setTag(holder);
            } else {
                holder = (ViewHolder) convertview.getTag();
            }
            holder.word.setText(this.mWords[position].trim());
            holder.word.setSelected(true);
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
