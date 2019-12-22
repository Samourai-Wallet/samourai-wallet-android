package com.samourai.wallet.whirlpool.newPool;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.samourai.wallet.R;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.LogUtil;

import static com.samourai.wallet.util.FormatsUtil.valueAsDp;
import static com.samourai.wallet.whirlpool.WhirlpoolMain.NEWPOOL_REQ_CODE;

public class DepositOrChooseUtxoDialog extends BottomSheetDialogFragment {


    private TextView addressText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottomsheet_deposit_or_choose_utxo, null);
        addressText = view.findViewById(R.id.bottomsheet_whirlpool_deposit_address);

        Window window = getActivity().getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getResources().getColor(R.color.off_black));

        view.findViewById(R.id.whirlpool_dialog_choose_utxos_btn).setOnClickListener(view1 -> {
            getActivity().startActivityForResult(new Intent(getActivity(), NewPoolActivity.class),NEWPOOL_REQ_CODE);
            this.dismiss();
        });

        view.findViewById(R.id.whirlpool_dialog_deposit_btn).setOnClickListener(view1 -> {
            String addr84 = AddressFactory.getInstance(getActivity()).getBIP84(AddressFactory.RECEIVE_CHAIN).getBech32AsString();
            ImageView showQR = new ImageView(getContext());
            Bitmap bitmap = null;
            QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(addr84, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), 500);
            try {
                bitmap = qrCodeEncoder.encodeAsBitmap();
            } catch (WriterException e) {
                e.printStackTrace();
            }
            showQR.setImageBitmap(bitmap);

            TextView showText = new TextView(getContext());
            showText.setText(addr84);
            showText.setTextIsSelectable(true);


            showText.setPadding(valueAsDp(getContext(), 12), valueAsDp(getContext(), 10), valueAsDp(getContext(), 12), valueAsDp(getContext(), 12));
            showText.setTextSize(18.0f);

            LinearLayout privkeyLayout = new LinearLayout(getContext());
            privkeyLayout.setOrientation(LinearLayout.VERTICAL);
            privkeyLayout.addView(showQR);
            privkeyLayout.addView(showText);
            privkeyLayout.setPadding(valueAsDp(getContext(), 12), valueAsDp(getContext(), 12), valueAsDp(getContext(), 12), valueAsDp(getContext(), 12));

            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.app_name)
                    .setView(privkeyLayout)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, (dialog, whichButton) -> {
                    }).show();
        });
        return view;
    }

    @Override
    public void onDestroy() {
        Window window = getActivity().getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getResources().getColor(R.color.whirlpoolBlue));

        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();

        if (dialog != null) {
            View bottomSheet = dialog.findViewById(R.id.design_bottom_sheet);
            bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
        }
        View view = getView();
        view.post(() -> {
            View parent = (View) view.getParent();
            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) (parent).getLayoutParams();
            CoordinatorLayout.Behavior behavior = params.getBehavior();
            BottomSheetBehavior bottomSheetBehavior = (BottomSheetBehavior) behavior;
            bottomSheetBehavior.setPeekHeight(view.getMeasuredHeight());
        });
    }


}