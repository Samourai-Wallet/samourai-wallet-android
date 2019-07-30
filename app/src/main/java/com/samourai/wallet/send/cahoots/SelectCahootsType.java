package com.samourai.wallet.send.cahoots;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.samourai.wallet.R;

public class SelectCahootsType extends BottomSheetDialogFragment {


    public enum type {STONEWALLX2_MANUAL,STONEWALLX2_SAMOURAI, STOWAWAY, NONE}

    private OnSelectListener onSelectListener;
    private ViewGroup stowaway, stonewallx2;
    private ImageButton closeBtn;
    private LinearLayout typeChooserLayout, stowawayChooserLayout;
    private ViewGroup samouraiAsParticipant, inPerson;
    private TextView title;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        android.view.ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(getActivity(), R.style.ui_2_theme); // your app theme here
        return inflater.cloneInContext(contextThemeWrapper).inflate(R.layout.fragment_choose_cahoots_type, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        stowaway = view.findViewById(R.id.cahoots_type_stowaway_layout);
        stonewallx2 = view.findViewById(R.id.cahoots_type_stonewallx2_layout);
        typeChooserLayout = view.findViewById(R.id.cahoots_type_chooser_layout);
        stowawayChooserLayout = view.findViewById(R.id.stowaway_type_chooser_layout);
        samouraiAsParticipant = view.findViewById(R.id.samourai_as_participant_btn);
        inPerson = view.findViewById(R.id.in_person_manual_stowaway);
        closeBtn = view.findViewById(R.id.cahoots_type_close_btn);
        title = view.findViewById(R.id.cahoots_sheet_title);

        stowaway.setOnClickListener(view1 ->{
            if (onSelectListener != null) {
                onSelectListener.onSelect(type.STOWAWAY);
            }
            this.dismiss();
        });
        stonewallx2.setOnClickListener(view1 -> {
            this.switchToStoneWallx2();
        });
        inPerson.setOnClickListener(view1 -> {
            if (onSelectListener != null) {
                onSelectListener.onSelect(type.STONEWALLX2_MANUAL);
            }
            this.dismiss();
        });
        samouraiAsParticipant.setOnClickListener(view1 -> {
            Toast.makeText(getContext(),"Coming soon",Toast.LENGTH_SHORT).show();
//            if (onSelectListener != null) {
//                onSelectListener.onSelect(type.STONEWALLX2_SAMOURAI);
//            }
//            this.dismiss();
        });
        closeBtn.setOnClickListener(view1 -> {
            if (stowawayChooserLayout.getVisibility() == View.VISIBLE) {
                switchToCahootsOption();
                return;
            }
            if (onSelectListener != null) {
                onSelectListener.onDismiss();
            }
            this.dismiss();
        });
    }

    public void setOnSelectListener(OnSelectListener onSelectListener) {
        this.onSelectListener = onSelectListener;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (onSelectListener != null) {
            onSelectListener.onDismiss();
        }
        super.onDismiss(dialog);
    }

    private void switchToStoneWallx2() {
        typeChooserLayout.setVisibility(View.GONE);
        stowawayChooserLayout.setVisibility(View.VISIBLE);
        closeBtn.setImageResource(R.drawable.ic_navigate_before_white_24dp);
        title.setText(getString(R.string.select_participant));
    }

    private void switchToCahootsOption() {
        typeChooserLayout.setVisibility(View.VISIBLE);
        stowawayChooserLayout.setVisibility(View.INVISIBLE);
        closeBtn.setImageResource(R.drawable.ic_close_white_24dp);
        title.setText(getString(R.string.select_cahoots_type));
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
            ((View) getView().getParent()).setBackgroundColor(Color.TRANSPARENT);

        });
    }

    public interface OnSelectListener {
        void onSelect(type type);

        void onDismiss();
    }
}