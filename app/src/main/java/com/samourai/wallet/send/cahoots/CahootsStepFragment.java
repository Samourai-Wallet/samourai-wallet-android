package com.samourai.wallet.send.cahoots;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.samourai.wallet.R;
import com.samourai.wallet.fragments.CameraFragmentBottomSheet;

public class CahootsStepFragment extends Fragment {

    private Button showQRBtn, scanQRbtn, pasteBtn;
    private String payload = "{}";
    private int step = 0;
    private static final String TAG = "CahootsStepView";

    private CahootsFragmentListener cahootsFragmentListener;

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
        pasteBtn = view.findViewById(R.id.cahoots_step_paste_payload);
        step = getArguments().getInt("step");
        stepText.setText("Step ".concat(String.valueOf(step + 1)));
        scanQRbtn.setOnClickListener(view1 -> {
            CameraFragmentBottomSheet cameraFragmentBottomSheet = new CameraFragmentBottomSheet();
            cameraFragmentBottomSheet.show(((AppCompatActivity) getActivity()).getSupportFragmentManager(), cameraFragmentBottomSheet.getTag());
            cameraFragmentBottomSheet.setQrCodeScanLisenter(code -> {
                cameraFragmentBottomSheet.dismissAllowingStateLoss();
                if (cahootsFragmentListener != null) {
                    cahootsFragmentListener.onScan(step, code);
                }
            });
        });
        showQRBtn.setOnClickListener(view1 -> {
            if (cahootsFragmentListener != null) {
                cahootsFragmentListener.onShare(step, payload);
            }
        });
        pasteBtn.setOnClickListener(view1 -> {
            try {
                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                if (cahootsFragmentListener != null) {
                    cahootsFragmentListener.onScan(step, item.getText().toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Invalid data", Toast.LENGTH_SHORT).show();
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

        void onShare(int step, String qrData);
    }


}
