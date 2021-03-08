package com.samourai.wallet.send.cahoots;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.material.button.MaterialButton;
import com.samourai.wallet.R;
import com.samourai.wallet.fragments.CameraFragmentBottomSheet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ManualCahootsStepFragment extends AbstractCahootsStepFragment {

    private MaterialButton showQRBtn, scanQRbtn;

    private CahootsFragmentListener cahootsFragmentListener;

    public static ManualCahootsStepFragment newInstance(int position, CahootsFragmentListener listener) {
        Bundle args = new Bundle();
        args.putInt("step", position);
        ManualCahootsStepFragment fragment = new ManualCahootsStepFragment();
        fragment.setArguments(args);
        fragment.setCahootsFragmentListener(listener);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        showQRBtn = view.findViewById(R.id.cahoots_step_show_qr_btn);
        scanQRbtn = view.findViewById(R.id.cahoots_step_scan_qr_btn);

        scanQRbtn.setOnClickListener(view1 -> {
            CameraFragmentBottomSheet cameraFragmentBottomSheet = new CameraFragmentBottomSheet();
            cameraFragmentBottomSheet.show(getActivity().getSupportFragmentManager(), cameraFragmentBottomSheet.getTag());
            cameraFragmentBottomSheet.setQrCodeScanListener(code -> {
                cameraFragmentBottomSheet.dismissAllowingStateLoss();
                if (cahootsFragmentListener != null) {
                    cahootsFragmentListener.onScan(step, code);
                }
            });
        });
        showQRBtn.setOnClickListener(view1 -> {
            if (cahootsFragmentListener != null) {
                cahootsFragmentListener.onShare(step);
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.cahoots_step_view, container, false);
    }

    public void setCahootsFragmentListener(CahootsFragmentListener cahootsFragmentListener) {
        this.cahootsFragmentListener = cahootsFragmentListener;
    }

    interface CahootsFragmentListener {
        void onScan(int step, String qrData);

        void onShare(int step);
    }
}
