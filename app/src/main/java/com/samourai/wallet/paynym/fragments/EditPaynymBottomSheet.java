package com.samourai.wallet.paynym.fragments;

import android.os.Bundle;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import androidx.core.content.FileProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.material.textfield.TextInputEditText;
import com.samourai.wallet.R;

public class EditPaynymBottomSheet extends BottomSheetDialogFragment {


    public enum type {EDIT, SAVE}

    private String pcode;
    private String label;
    private String buttonText;


    private TextInputEditText labelEdt, pcodeEdt;
    private Button saveButton;
    private View.OnClickListener onClickListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottomsheet_edit_paynym, null);

        pcode = getArguments().getString("pcode");
        label = getArguments().getString("label");
        buttonText = getArguments().getString("buttonText");

        labelEdt = view.findViewById(R.id.paynym_label);
        pcodeEdt = view.findViewById(R.id.paynym_pcode);
        saveButton = view.findViewById(R.id.edit_paynym_button);

        labelEdt.setText(label);
        pcodeEdt.setText(pcode);
        saveButton.setText(buttonText);
        saveButton.setOnClickListener(button -> {
            this.dismiss();
            if (onClickListener != null) {
                onClickListener.onClick(button);
            }
        });
        return view;
    }


    public String getLabel() {
        return labelEdt.getText().toString();
    }

    public String getPcode() {
        return pcodeEdt.getText().toString();
    }

    public void setSaveButtonListener(View.OnClickListener listener) {
        onClickListener = listener;
    }
}