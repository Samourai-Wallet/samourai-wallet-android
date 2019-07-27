package com.samourai.wallet.paynym.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.FileProvider;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.samourai.wallet.R;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.util.AppUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class EditPaynymBottomSheet extends BottomSheetDialogFragment {


    public enum type {EDIT, SAVE}

    private String pcode;
    private String label;
    private String buttonText;


    private EditText labelEdt, pcodeEdt;
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