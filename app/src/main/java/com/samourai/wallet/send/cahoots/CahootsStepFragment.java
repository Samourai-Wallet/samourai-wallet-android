package com.samourai.wallet.send.cahoots;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialog;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.samourai.wallet.R;
import com.samourai.wallet.fragments.CameraFragmentBottomSheet;

public class CahootsStepFragment extends Fragment {

    private Button showQRBtn, scanQRbtn;
    private String qrData = "PlaceHolder : A modern bitcoin wallet hand forged to keep your transactions private your identity masked and your funds secured.";
    private int step = 0;
    private static final String TAG = "CahootsStepView";

    private CahootsScanListener cahootsScanListener;


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
        step = getArguments().getInt("step");
        stepText.setText("Step ".concat(String.valueOf(step + 1)));
        scanQRbtn.setOnClickListener(view1 -> {
            CameraFragmentBottomSheet cameraFragmentBottomSheet = new CameraFragmentBottomSheet();
            cameraFragmentBottomSheet.show(((AppCompatActivity) getActivity()).getSupportFragmentManager(), cameraFragmentBottomSheet.getTag());
            cameraFragmentBottomSheet.setQrCodeScanLisenter(code -> {
                cameraFragmentBottomSheet.dismissAllowingStateLoss();
                if (cahootsScanListener != null) {
                    cahootsScanListener.onScan(step, code);
                }
            });
        });
        showQRBtn.setOnClickListener(view1 -> showQRCodeAlert());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.cahoots_step_view, container, false);
    }


    private void showQRCodeAlert() {
        final AppCompatDialog dialog = new AppCompatDialog(getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.cahoots_qr_dialog_layout);
        ImageView qrCode = dialog.findViewById(R.id.qr_code_imageview);
        qrCode.setImageBitmap(generateQRCode(qrData));
        Button copy = dialog.findViewById(R.id.cahoots_copy_btn);
        Button share = dialog.findViewById(R.id.cahoots_share);

        share.setOnClickListener(v -> dialog.dismiss());

        copy.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    public void setCahootsScanListener(CahootsScanListener cahootsScanListener) {
        this.cahootsScanListener = cahootsScanListener;
    }

    public void setQrData(String qrData) {
        this.qrData = qrData;
    }

    interface CahootsScanListener {
        void onScan(int step, String qrData);
    }

    private Bitmap generateQRCode(String uri) {

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int imgWidth = size.x - 120;

        Bitmap bitmap = null;

        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(uri, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), imgWidth);

        try {
            bitmap = qrCodeEncoder.encodeAsBitmap();
        } catch (WriterException e) {
            e.printStackTrace();
        }

        return bitmap;
    }

}
