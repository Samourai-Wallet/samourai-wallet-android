package com.samourai.wallet.whirlpool.newPool;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.samourai.wallet.R;
import com.samourai.wallet.util.AddressFactory;

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
            startActivity(new Intent(getActivity(), NewPoolActivity.class));
            this.dismiss();
        });

        view.findViewById(R.id.whirlpool_dialog_deposit_btn).setOnClickListener(view1 -> {
            String addr84 = AddressFactory.getInstance(getActivity()).getBIP84(AddressFactory.RECEIVE_CHAIN).getBech32AsString();
            TransitionManager.beginDelayedTransition((ViewGroup) view);
            addressText.setText(addr84);
            addressText.setVisibility(View.VISIBLE);
            addressText.setOnClickListener(view2 -> {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getActivity().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = null;
                clip = android.content.ClipData.newPlainText("Receive address", addr84);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
            });
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