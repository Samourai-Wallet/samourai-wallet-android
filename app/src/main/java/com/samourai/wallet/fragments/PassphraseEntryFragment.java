package com.samourai.wallet.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.samourai.wallet.R;

public class PassphraseEntryFragment extends Fragment {

    private onPassPhraseListener mListener = null;
    private EditText passPhrase, confirmPassphrase;
    private CheckBox acceptDisclaimer;

    public PassphraseEntryFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    private TextWatcher watcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            stateChange();
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        passPhrase = (EditText) view.findViewById(R.id.password_entry);
        confirmPassphrase = (EditText) view.findViewById(R.id.password_entry_confirm);
        acceptDisclaimer = (CheckBox) view.findViewById(R.id.disclaimer_checkbox);
        confirmPassphrase.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    stateChange();
                    return false;
                }
                return false;
            }
        });

        confirmPassphrase.addTextChangedListener(watcher);
        passPhrase.addTextChangedListener(watcher);

        acceptDisclaimer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                stateChange();
            }
        });

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_password_entry, container, false);
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof onPassPhraseListener) {
            mListener = (onPassPhraseListener) activity;
        }
    }

    private void stateChange() {
        if (mListener != null)
            this.mListener.passPhraseSet(passPhrase.getText().toString(), confirmPassphrase.getText().toString(), acceptDisclaimer.isChecked());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    public interface onPassPhraseListener {
        void passPhraseSet(String passPhrase, String confirmPhrase, boolean checked);
    }
}
