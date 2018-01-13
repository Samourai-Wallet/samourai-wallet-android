package com.samourai.wallet.create;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.samourai.wallet.R;


public class SetPinFragment extends Fragment {


    private static final String IS_CONFIRMATION_KEY = "is_confirmation";

    private boolean mIsConfirmation;

    public SetPinFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
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
        return inflater.inflate(R.layout.fragment_set_pin, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
