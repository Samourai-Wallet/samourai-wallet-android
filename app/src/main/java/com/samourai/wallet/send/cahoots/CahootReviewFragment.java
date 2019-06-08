package com.samourai.wallet.send.cahoots;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.samourai.wallet.R;

public class CahootReviewFragment extends Fragment {


    public static CahootReviewFragment newInstance() {
        Bundle args = new Bundle();
        CahootReviewFragment fragment = new CahootReviewFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.stonewall_broadcast_details, container, false);
    }


}
