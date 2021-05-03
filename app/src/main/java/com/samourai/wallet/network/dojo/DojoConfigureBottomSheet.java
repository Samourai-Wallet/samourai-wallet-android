package com.samourai.wallet.network.dojo;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import androidx.constraintlayout.widget.Group;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.samourai.wallet.R;
import com.samourai.wallet.fragments.CameraFragmentBottomSheet;
import com.samourai.wallet.tor.TorManager;
import com.samourai.wallet.util.PrefsUtil;

import io.matthewnelson.topl_service.TorServiceController;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class DojoConfigureBottomSheet extends BottomSheetDialogFragment {

    Button preOrder, connect;
    int SCAN_QR = 1200;
    private CompositeDisposable compositeDisposables = new CompositeDisposable();
    private ProgressBar dojoConnectProgress;
    private TextView progressStates;
    private Group btnGroup, progressGroup;
    private CameraFragmentBottomSheet cameraFragmentBottomSheet;
    private DojoConfigurationListener dojoConfigurationListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottomsheet_dojo_configure, null);
        connect = view.findViewById(R.id.connect_dojo);
        preOrder = view.findViewById(R.id.preorder_dojo);
        view.findViewById(R.id.close_dojo).setOnClickListener(view1 -> {
            this.dismiss();
        });
        progressGroup = view.findViewById(R.id.dojo_progress_group);
        btnGroup = view.findViewById(R.id.dojo_btn_group);
        progressStates = view.findViewById(R.id.dojo_progress_status_text);
        dojoConnectProgress = view.findViewById(R.id.dojo_connect_progress);
        dojoConnectProgress.setIndeterminate(false);
        dojoConnectProgress.setMax(100);
        connect.setOnClickListener(view1 -> {
            showConnectionAlert();
        });


        return view;
    }

    public void setDojoConfigurationListener(DojoConfigurationListener dojoConfigurationListener) {
        this.dojoConfigurationListener = dojoConfigurationListener;
    }

    @Override
    public void onStart() {
        super.onStart();

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
            dialog.dismiss();
            cameraFragmentBottomSheet = new CameraFragmentBottomSheet();
            cameraFragmentBottomSheet.show(getActivity().getSupportFragmentManager(), cameraFragmentBottomSheet.getTag());
            cameraFragmentBottomSheet.setQrCodeScanListener(this::connectToDojo);

        });
        dialog.findViewById(R.id.dojo_paste_config).setVisibility(View.GONE);

//        dialog.findViewById(R.id.dojo_paste_config).setOnClickListener(view -> {
//
//            try {
//                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
//                ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
//                connectToDojo(item.getText().toString());
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//            dialog.dismiss();
//
//        });
//

    }

    private void connectToDojo(String dojoParams) {
        cameraFragmentBottomSheet.dismissAllowingStateLoss();
        btnGroup.setVisibility(View.INVISIBLE);
        progressGroup.setVisibility(View.VISIBLE);
        dojoConnectProgress.setProgress(30);
        if (TorManager.INSTANCE.isConnected()) {
            dojoConnectProgress.setProgress(60);
            progressStates.setText("Tor Connected, Connecting to Dojo Node...");
            DojoUtil.getInstance(getActivity().getApplicationContext()).clear();
            doPairing(dojoParams);
        } else {
            progressStates.setText("Waiting for Tor...");
            TorServiceController.startTor();
            TorManager.INSTANCE.getTorStateLiveData().observe(this.getViewLifecycleOwner(),torState -> {
                if (torState ==  TorManager.TorState.WAITING) {
                    progressStates.setText("Waiting for Tor...");
                } else if (torState == TorManager.TorState.ON) {
                    PrefsUtil.getInstance(getActivity()).setValue(PrefsUtil.ENABLE_TOR, true);
                    dojoConnectProgress.setProgress(60);
                    progressStates.setText("Tor Connected, Connecting to Dojo Node...");
                    DojoUtil.getInstance(getActivity().getApplicationContext()).clear();
                    doPairing(dojoParams);
                }

            });
        }
    }

    private void doPairing(String params) {

        Disposable disposable = DojoUtil.getInstance(getActivity().getApplicationContext()).setDojoParams(params)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(success -> {
                    if(success){
                        progressStates.setText("Successfully connected to Dojo Node");
                        if (this.dojoConfigurationListener != null) {
                            this.dojoConfigurationListener.onConnect();
                        }
                        dojoConnectProgress.setProgress(100);
                        new Handler().postDelayed(() -> {
                            Toast.makeText(getActivity(), "Successfully connected to Dojo", Toast.LENGTH_SHORT).show();
                            dismissAllowingStateLoss();
                        }, 800);
                    }else{
                        dojoConnectProgress.setVisibility(View.INVISIBLE);
                        dismissAllowingStateLoss();
                        if (this.dojoConfigurationListener != null) {
                            this.dojoConfigurationListener.onError();
                        }
                        Toast.makeText(requireContext(),getString(R.string.dojo_connection_error), Toast.LENGTH_LONG).show();
                        DojoUtil.getInstance(getActivity().getApplicationContext()).removeDojoParams();
                        if (this.dojoConfigurationListener != null) {
                            this.dojoConfigurationListener.onError();
                        }
                    }

                }, error -> {
                    error.printStackTrace();
                    if (this.dojoConfigurationListener != null) {
                        this.dojoConfigurationListener.onError();
                    }
                    progressStates.setText("Error Connecting node : ".concat(error.getMessage()));
                });
        compositeDisposables.add(disposable);

    }

    @Override
    public void onDestroy() {
        if (!compositeDisposables.isDisposed())
            compositeDisposables.dispose();
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }

    public interface DojoConfigurationListener {
        void onConnect();

        void onError();
    }
}