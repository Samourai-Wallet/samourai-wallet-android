package com.samourai.wallet.create;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.samourai.wallet.R;
import com.samourai.wallet.util.LogUtil;
import com.samourai.wallet.view.PinDigitImageView;

import java.util.ArrayList;


public class SetPinFragment extends Fragment {
    private static final String TAG = LogUtil.getTag();

    private static final String IS_CONFIRMATION_KEY = "is_confirmation";

    private boolean mIsConfirmation;

    private TextView mTitle;
    private TextView mSubTitle;

    private ArrayList<PinDigitImageView> mDigitsList;

    public SetPinFragment() {
        // Required empty public constructor
    }

    public static SetPinFragment newInstance(boolean isConfirmation) {
        SetPinFragment fragment = new SetPinFragment();
        Bundle args = new Bundle();
        args.putBoolean(IS_CONFIRMATION_KEY, isConfirmation);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mIsConfirmation= getArguments().getBoolean(IS_CONFIRMATION_KEY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layout = inflater.inflate(R.layout.fragment_set_pin, container, false);

        findViews(layout);
        setupConfirmation();

        return layout;
    }

    private void findViews(View v) {
        mTitle = v.findViewById(R.id.txt_create_pin_title);
        mSubTitle = v.findViewById(R.id.txt_create_pin_subtitle);
        prepareDigits(v);
    }

    private void prepareDigits(View v) {
        LinearLayout container = v.findViewById(R.id.pin_digits_container);
        mDigitsList = new ArrayList<>();

        for (int i = 0; i < container.getChildCount(); i++) {
            mDigitsList.add((PinDigitImageView) container.getChildAt(i));
        }
    }

    private void setupConfirmation() {
        if (!mIsConfirmation) {
            mTitle.setText(R.string.create_pin_title);
            mSubTitle.setText(R.string.create_pin_subtitle);
        } else {
            mTitle.setText(R.string.confirm_pin_title);
            mSubTitle.setText(R.string.confirm_pin_subtitle);
        }
    }
}
