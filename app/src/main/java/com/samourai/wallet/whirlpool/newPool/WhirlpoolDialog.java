package com.samourai.wallet.whirlpool.newPool;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.samourai.wallet.R;
import com.samourai.wallet.send.SendActivity;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;

import static com.samourai.wallet.whirlpool.WhirlpoolHome.NEWPOOL_REQ_CODE;

public class WhirlpoolDialog extends BottomSheetDialogFragment {



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottomsheet_deposit_or_choose_utxo, null);

        Window window = getActivity().getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getResources().getColor(R.color.off_black));

        view.findViewById(R.id.whirlpool_dialog_choose_utxos_btn).setOnClickListener(view1 -> {
            getActivity().startActivityForResult(new Intent(getActivity(), NewPoolActivity.class),NEWPOOL_REQ_CODE);
            this.dismiss();
        });

        view.findViewById(R.id.spend_btn_whirlpool_dialog).setOnClickListener(view1 -> {
            Intent intent = new Intent(getActivity(), SendActivity.class);
            intent.putExtra("_account", WhirlpoolMeta.getInstance(getContext()).getWhirlpoolPostmix());
            startActivity(intent);
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