package com.samourai.wallet.network.dojo;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;
import com.samourai.wallet.R;
import com.yanzhenjie.zbar.Symbol;

public class DojoConfigureBottomSheet extends BottomSheetDialogFragment {

    Button preOrder, connect;
    int SCAN_QR = 1200;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottomsheet_dojo_configure, null);
        connect = view.findViewById(R.id.connect_dojo);
        preOrder = view.findViewById(R.id.preorder_dojo);
        view.findViewById(R.id.close_dojo).setOnClickListener(view1 -> {
            this.dismiss();
        });

        connect.setOnClickListener(view1 -> {
            showConnectionAlert();
        });

        return view;
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

    private void showConnectionAlert() {

        Dialog dialog = new Dialog(getActivity(), android.R.style.Theme_Dialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dojo_connect_dialog);
        dialog.setCanceledOnTouchOutside(true);
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        dialog.setCanceledOnTouchOutside(false);

        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        dialog.show();

        dialog.findViewById(R.id.dojo_scan_qr).setOnClickListener(view -> {

            Intent intent = new Intent(this.getContext(), ZBarScannerActivity.class);
            intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{Symbol.QRCODE});
            this.startActivityForResult(intent, SCAN_QR);

        });

        dialog.findViewById(R.id.dojo_paste_config).setOnClickListener(view -> {

        });


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }
}