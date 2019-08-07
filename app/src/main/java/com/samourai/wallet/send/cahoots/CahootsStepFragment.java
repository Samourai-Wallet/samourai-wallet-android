package com.samourai.wallet.send.cahoots;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.samourai.wallet.R;
import com.samourai.wallet.cahoots.STONEWALLx2;
import com.samourai.wallet.fragments.CameraFragmentBottomSheet;

import java.util.Locale;

public class CahootsStepFragment extends Fragment {

    private Button showQRBtn, scanQRbtn;
    private TextView stoneWallx2TotalFee, stoneWallx2SplitFee;
    private ViewGroup feeSplitUpContainer;
    private int step = 0;
    private static final String TAG = "CahootsStepView";

    private CahootsFragmentListener cahootsFragmentListener;
    private STONEWALLx2 stowaway;

    public static CahootsStepFragment newInstance(int position) {
        Bundle args = new Bundle();
        args.putInt("step", position);
        CahootsStepFragment fragment = new CahootsStepFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        TextView stepText = view.findViewById(R.id.cahoots_step_text);
        showQRBtn = view.findViewById(R.id.cahoots_step_show_qr_btn);
        scanQRbtn = view.findViewById(R.id.cahoots_step_scan_qr_btn);
        stoneWallx2TotalFee = view.findViewById(R.id.stonewall_splitup_total_fee);
        stoneWallx2SplitFee = view.findViewById(R.id.stonewall_collab_fee);
        feeSplitUpContainer = view.findViewById(R.id.stonewall_fee_splitup_container);
        step = getArguments().getInt("step");
        stepText.setText("Step ".concat(String.valueOf(step + 1)));

        scanQRbtn.setOnClickListener(view1 -> {
            CameraFragmentBottomSheet cameraFragmentBottomSheet = new CameraFragmentBottomSheet();
            cameraFragmentBottomSheet.show(getActivity().getSupportFragmentManager(), cameraFragmentBottomSheet.getTag());
            cameraFragmentBottomSheet.setQrCodeScanLisenter(code -> {
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
        feeSplitUpContainer.setVisibility(View.GONE);

        if (this.stowaway != null) {
            if (stowaway.getStep() == 2) {
                feeSplitUpContainer.post(() -> {
                    feeSplitUpContainer.setVisibility(View.VISIBLE);
                });

                stoneWallx2TotalFee.setText(formatForBtc(this.stowaway.getFeeAmount()));
                stoneWallx2SplitFee.setText(formatForBtc(this.stowaway.getFeeAmount() / 2));
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.cahoots_step_view, container, false);
    }

    public void setCahootsFragmentListener(CahootsFragmentListener cahootsFragmentListener) {
        this.cahootsFragmentListener = cahootsFragmentListener;
    }

    public void setStowaway(STONEWALLx2 stonewalLx2) {
        this.stowaway = stonewalLx2;
    }

    interface CahootsFragmentListener {
        void onScan(int step, String qrData);

        void onShare(int step);
    }


    private String formatForBtc(Long amount) {
        return (String.format(Locale.ENGLISH, "%.8f", getBtcValue((double) amount)).concat(" BTC"));
    }

    private Double getBtcValue(Double sats) {
        return (sats / 1e8);
    }


}
