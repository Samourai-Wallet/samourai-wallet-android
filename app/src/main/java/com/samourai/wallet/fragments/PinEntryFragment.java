package com.samourai.wallet.fragments;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.samourai.wallet.R;
import com.samourai.wallet.widgets.PinEntryView;


public class PinEntryFragment extends Fragment {
    private static String ARG_TITLE = "TITLE";
    private static String ARG_DESC = "DESC";
    private onPinEntryListener mListener;
    private PinEntryView entryView;
    private StringBuilder passPhrase = new StringBuilder("");
    private ImageView[] pinEntries;
    private LinearLayout maskPassPhraseContainer;
    private String mTitle = null, mDescription = null;
    private static final String TAG = "PinEntryFragment";

    public PinEntryFragment() {
        // Required empty public constructor
    }


    public static PinEntryFragment newInstance(String title, String description) {
        PinEntryFragment fragment = new PinEntryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESC, description);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments != null) {
            this.mTitle = arguments.getString(ARG_TITLE);
            this.mDescription = arguments.getString(ARG_DESC);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pin_entry, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        entryView = (PinEntryView) view.findViewById(R.id.pin_entry_view);
        maskPassPhraseContainer = (LinearLayout) view.findViewById(R.id.passphrase_mask_container);
        initMaskView();
        entryView.setEntryListener(new PinEntryView.pinEntryListener() {
            @Override
            public void onPinEntered(String key, View view) {
                passPhrase = passPhrase.append(key);
                addKeyText();
                propagateToActivity();

            }
        });
        entryView.setClearListener(new PinEntryView.pinClearListener() {
            @Override
            public void onPinClear(PinEntryView.KeyClearTypes clearType) {
                if (clearType == PinEntryView.KeyClearTypes.CLEAR) {
                    if (passPhrase.length() - 1 >= 0) {
                        passPhrase.deleteCharAt(passPhrase.length() - 1);
                    }
                } else {
                    passPhrase.setLength(0);
                }
                propagateToActivity();
                addKeyText();
            }
        });
        if (this.mTitle != null) {
            ((TextView) view.findViewById(R.id.pin_entry_title)).setText(this.mTitle);
        }
        if (this.mDescription != null) {
            ((TextView) view.findViewById(R.id.pin_entry_description)).setText(this.mDescription);
        }

    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof onPinEntryListener) {
            mListener = (onPinEntryListener) activity;
        }
    }

    private void initMaskView() {
        pinEntries = new ImageView[8];
        for (int i = 0; i < 8; i++) {
            pinEntries[i] = new ImageView(getActivity());
            pinEntries[i].setImageDrawable(getResources().getDrawable(R.drawable.circle_dot_white));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            maskPassPhraseContainer.addView(pinEntries[i], params);
        }
    }

    private void addKeyText() {
        for (int i = 0; i < 8; i++) {
            if (passPhrase.toString().length() > i) {
                pinEntries[i].getDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.ADD);
            } else {
                pinEntries[i].setImageDrawable(getActivity().getDrawable(R.drawable.circle_dot_white));
            }
        }
    }

    private void propagateToActivity() {
        if (this.mListener != null) {
            this.mListener.PinEntry(passPhrase.toString());
        }
    }


    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    public interface onPinEntryListener {
        void PinEntry(String pin);
    }
}
