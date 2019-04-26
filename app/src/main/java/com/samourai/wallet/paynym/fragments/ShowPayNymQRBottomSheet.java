package com.samourai.wallet.paynym.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.samourai.wallet.R;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.BIP47ShowQR;
import com.samourai.wallet.bip47.BIP47Util;
import com.samourai.wallet.segwit.BIP49Util;
import com.samourai.wallet.util.AppUtil;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class ShowPayNymQRBottomSheet extends BottomSheetDialogFragment {

    private String pcode;

    ImageView payNymQr;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottomsheet_paynym_qr, null);
        pcode = getArguments().getString("pcode");

        view.findViewById(R.id.close_qr_modal).setOnClickListener(view1 -> {
            this.dismiss();
        });

        view.findViewById(R.id.share_paynym_pcode).setOnClickListener(view1 -> {
            share();
        });
        payNymQr = view.findViewById(R.id.paynym_qr_image_view);

        Disposable disposable = generateQRCode(pcode).subscribe(bitmap -> {
            payNymQr.setImageBitmap(bitmap);
        });

        ((TextView) view.findViewById(R.id.paynym_string_pcode)).setText(pcode);
        ((TextView) view.findViewById(R.id.paynym_title)).setText(BIP47Meta.getInstance().getLabel(pcode));

        view.findViewById(R.id.paynym_string_pcode).setOnClickListener(view12 -> {
            copy();
        });

        disposables.add(disposable);
        return view;
    }

    private void copy() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.app_name)
                .setMessage(R.string.receive_address_to_share)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, (dialog, whichButton) -> {

                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = null;
                    clip = android.content.ClipData.newPlainText("Receive address", pcode);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(getContext(), "payment code copied", Toast.LENGTH_SHORT).show();
                    }

                }).setNegativeButton(R.string.no, (dialog, whichButton) -> {

        }).show();
    }

    private void share() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.app_name)
                .setMessage(R.string.receive_address_to_share)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, (dialog, whichButton) -> {

                    String strFileName = AppUtil.getInstance(getContext()).getReceiveQRFilename();
                    File file = new File(strFileName);
                    if (!file.exists()) {
                        try {
                            file.createNewFile();
                        } catch (Exception e) {
                            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                    file.setReadable(true, false);

                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(file);
                    } catch (FileNotFoundException fnfe) {
                        ;
                    }

                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = null;
                    clip = android.content.ClipData.newPlainText("Receive address", pcode);
                    clipboard.setPrimaryClip(clip);

                    if (file != null && fos != null) {
                        Bitmap bitmap = ((BitmapDrawable) payNymQr.getDrawable()).getBitmap();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 0, fos);

                        try {
                            fos.close();
                        } catch (IOException ioe) {
                            ;
                        }

                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_SEND);
                        intent.setType("image/png");
                        if (android.os.Build.VERSION.SDK_INT >= 24) {
                            //From API 24 sending FIle on intent ,require custom file provider
                            intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                                    getContext(),
                                    getContext()
                                            .getPackageName() + ".provider", file));
                        } else {
                            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                        }
                        startActivity(Intent.createChooser(intent, getContext().getText(R.string.send_payment_code)));
                    }

                }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                ;
            }
        }).show();

    }


    private Observable<Bitmap> generateQRCode(String uri) {
        return Observable.fromCallable(() -> {
            Bitmap bitmap = null;
            QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(uri, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), 250);
            try {
                bitmap = qrCodeEncoder.encodeAsBitmap();
            } catch (WriterException e) {
                e.printStackTrace();
            }
            return bitmap;
        });
    }

}