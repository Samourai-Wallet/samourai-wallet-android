package com.samourai.wallet.whirlpool.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.samourai.wallet.R;
import com.samourai.wallet.util.LogUtil;
import com.samourai.wallet.whirlpool.service.WhirlpoolNotificationService;
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class WhirlPoolLoaderDialog extends BottomSheetDialogFragment {

    private static final String TAG = "WhirlPoolLoaderDialog";
    private TextView statusText;
    private LinearProgressIndicator statusProgress;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private onInitComplete onInitComplete;

    public interface onInitComplete {
        void init();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottomsheet_whirlpool_loading, null);
        this.setCancelable(false);
        Window window = getActivity().getWindow();
        statusText = view.findViewById(R.id.whirlpool_loader_status_text);
        statusProgress = view.findViewById(R.id.whirlpool_loader_progress);
        statusProgress.setMax(100);

        RotateAnimation rotate = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF,
                0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(1000);
        view.findViewById(R.id.imageView8).startAnimation(rotate);
        rotate.setInterpolator(new LinearInterpolator());
        rotate.setRepeatCount(Animation.INFINITE);
        rotate.setRepeatMode(Animation.RESTART);
        rotate.start();


        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getResources().getColor(R.color.off_black));
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        statusProgress.setProgress(20);
        statusText.setText(R.string.loading);
        WhirlpoolNotificationService.startService(getActivity());
        AndroidWhirlpoolWalletService androidWhirlpoolWalletService = AndroidWhirlpoolWalletService.getInstance();
        Disposable disposable = androidWhirlpoolWalletService.listenConnectionStatus()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> {
                     switch (s) {
                        case LOADING: {
                            new Handler().postDelayed(() -> {
                                statusText.setText(R.string.initializing_whirlpool);
                                statusProgress.setProgressCompat(35, true);
                            }, 300);

                            break;
                        }
                        case STARTING: {
                            new Handler().postDelayed(() -> {
                                statusText.setText(R.string.connecting_to_service);
                                statusProgress.setProgressCompat(65, true);
                            }, 600);
                            break;
                        }
                        case CONNECTED: {
                            statusText.setText(R.string.connected);
                            statusProgress.setProgressCompat(100, true);
                            Disposable disposable1 = Observable
                                    .interval(600, TimeUnit.MILLISECONDS)
                                    .subscribeOn(Schedulers.io())
                                    .flatMap(aLong -> Observable.fromCallable(() -> {
                                        WhirlpoolWallet wallet = androidWhirlpoolWalletService.getWhirlpoolWalletOrNull();
                                        if (wallet != null) {
                                            return wallet.isStarted();
                                        }
                                        return false;
                                    }))
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(isInitialized -> {
                                        WhirlpoolWallet wallet = androidWhirlpoolWalletService.getWhirlpoolWalletOrNull();
                                        if (wallet != null) {
                                            if (wallet.isStarted()) {
                                                onInitComplete.init();
                                                this.dismiss();
                                            }
                                        }
                                    }, throwable -> {
                                        LogUtil.error(TAG, throwable);
                                    });
                            compositeDisposable.add(disposable1);
                            break;

                        }
                        case DISCONNECTED:
                            statusText.setText(R.string.disconnected);
                            statusProgress.setProgressCompat(0, true);
                            new Handler().postDelayed(() -> {
                                // exit on Whirlpool start failure
                                getActivity().onBackPressed();
                            }, 1200);
                            break;
                        default:

                    }
                }, Throwable::printStackTrace);
        compositeDisposable.add(disposable);
    }

    public void setOnInitComplete(WhirlPoolLoaderDialog.onInitComplete onInitComplete) {
        this.onInitComplete = onInitComplete;
    }

    @Override
    public void onDestroy() {
        Window window = getActivity().getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getResources().getColor(R.color.whirlpoolBlue));
        compositeDisposable.dispose();
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
