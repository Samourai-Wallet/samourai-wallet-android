package com.samourai.wallet.fragments;

import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.samourai.codescanner.CodeScanner;
import com.samourai.codescanner.CodeScannerView;
import com.samourai.wallet.R;


public class CameraFragmentBottomSheet extends BottomSheetDialogFragment {
    private CodeScanner mCodeScanner;

    private ListenQRScan listenQRScan;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottomsheet_camera, null);
        CodeScannerView scannerView = view.findViewById(R.id.scanner_view);
        mCodeScanner = new CodeScanner(getActivity().getApplicationContext(), scannerView);
        mCodeScanner.setAutoFocusEnabled(true);
        mCodeScanner.setDecodeCallback(result -> getActivity().runOnUiThread(() -> {
            if (this.listenQRScan != null) {
                this.listenQRScan.onDetect(result.getText());
            }
        }));
        scannerView.setOnClickListener(view1 -> mCodeScanner.startPreview());
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mCodeScanner.startPreview();
    }

    public void setQrCodeScanLisenter(ListenQRScan listenQRScan) {
        this.listenQRScan = listenQRScan;
    }

    @Override
    public void onPause() {
        mCodeScanner.releaseResources();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mCodeScanner.releaseResources();
        super.onDestroy();
    }

    public interface ListenQRScan {
        void onDetect(String code);
    }

    @Override
    public void onStart() {
        super.onStart();

        View view = getView();
        if (view != null) {
            view.post(() -> {
                View parent = (View) view.getParent();
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) (parent).getLayoutParams();
                CoordinatorLayout.Behavior behavior = params.getBehavior();
                BottomSheetBehavior bottomSheetBehavior = (BottomSheetBehavior) behavior;
                if (bottomSheetBehavior != null) {
                    bottomSheetBehavior.setPeekHeight(view.getMeasuredHeight());
                }

            });
        }
    }
}