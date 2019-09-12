package com.samourai.wallet.send.cahoots;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.Group;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.samourai.boltzmann.beans.BoltzmannSettings;
import com.samourai.boltzmann.beans.Txos;
import com.samourai.boltzmann.linker.TxosLinkerOptionEnum;
import com.samourai.boltzmann.processor.TxProcessor;
import com.samourai.boltzmann.processor.TxProcessorResult;
import com.samourai.wallet.R;
import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.wallet.cahoots.Stowaway;
import com.samourai.wallet.home.BalanceActivity;
import com.samourai.wallet.send.PushTx;
import com.samourai.wallet.widgets.EntropyBar;

import org.bitcoinj.core.TransactionOutput;
import org.bouncycastle.util.encoders.Hex;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.samourai.wallet.send.SendActivity.stubAddress;

public class CahootReviewFragment extends Fragment {


    private static final String TAG = "CahootReviewFragment";
    TextView toAddress, amountInBtc, amountInSats, feeInBtc, feeInSats, entropyBits;
    EntropyBar entropyBar;
    Button sendBtn;
    Group cahootsEntropyGroup, cahootsProgressGroup;
    private Cahoots payload;
    private CompositeDisposable disposables = new CompositeDisposable();

    public static CahootReviewFragment newInstance() {
        Bundle args = new Bundle();
        CahootReviewFragment fragment = new CahootReviewFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        if (payload != null) {
            toAddress.setText(payload.getDestination());
            sendBtn.setText(getString(R.string.send).concat(" ").concat(formatForBtc(payload.getSpendAmount()+payload.getFeeAmount())));
            amountInBtc.setText(formatForBtc(payload.getSpendAmount()));
            amountInSats.setText(String.valueOf(payload.getSpendAmount()).concat(" sat"));
            if ((payload.getFeeAmount() == 0)) {
                feeInBtc.setText("__");
                feeInSats.setText("__");
            } else {
                feeInBtc.setText(formatForBtc(payload.getFeeAmount()));
                feeInSats.setText(String.valueOf(payload.getFeeAmount()).concat(" sat"));

            }
            if (payload instanceof Stowaway) {
                cahootsEntropyGroup.setVisibility(View.GONE);
            } else {
                calculateEntropy();
            }

        }

        sendBtn.setOnClickListener(view1 -> {


            if (payload != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.app_name);
                builder.setMessage("Are you sure want to broadcast this transaction ?");
                builder.setCancelable(false);
                sendBtn.setEnabled(false);
                builder.setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    cahootsProgressGroup.setVisibility(View.VISIBLE);
                    new Thread(() -> {
                        Looper.prepare();

                        boolean success = PushTx.getInstance(getActivity()).pushTx(Hex.toHexString(payload.getTransaction().bitcoinSerialize()));

                        if (success) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getActivity(), R.string.tx_sent, Toast.LENGTH_SHORT).show();
                                notifyWalletAndFinish();
                            });
                        } else {
                            Toast.makeText(this.getActivity(), "Error broadcasting tx", Toast.LENGTH_SHORT).show();
                            getActivity().runOnUiThread(() -> {
                                cahootsProgressGroup.setVisibility(View.GONE);
                            });
                        }

                        Looper.loop();

                    }).start();
                });
                builder.setNegativeButton(R.string.no, (dialogInterface, i) -> {
                    sendBtn.setEnabled(true);
                });

                builder.create().show();
            }

        });
    }

    private void calculateEntropy() {

        CalculateEntropy(payload)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<TxProcessorResult>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(TxProcessorResult entropyResult) {
                        entropyBar.setRange(entropyResult);
                        DecimalFormat decimalFormat = new DecimalFormat("##.00");
                        entropyBits.setText(decimalFormat.format(entropyResult.getEntropy()).concat(" bits"));
                    }

                    @Override
                    public void onError(Throwable e) {
                        entropyBar.disable();
                        entropyBits.setText("0.0 bits");
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.cahoots_broadcast_details, container, false);
        toAddress = view.findViewById(R.id.cahoots_review_address);
        amountInBtc = view.findViewById(R.id.cahoots_review_amount);
        amountInSats = view.findViewById(R.id.cahoots_review_amount_sats);
        sendBtn = view.findViewById(R.id.cahoot_send_btn);
        feeInBtc = view.findViewById(R.id.cahoots_review_fee);
        entropyBits = view.findViewById(R.id.cahoots_entropy_value);
        entropyBar = view.findViewById(R.id.cahoots_entropy_bar);
        feeInSats = view.findViewById(R.id.cahoots_review_fee_sats);
        cahootsEntropyGroup = view.findViewById(R.id.cahoots_entropy_group);
        cahootsProgressGroup = view.findViewById(R.id.cahoots_progress_group);
        return view;
    }

    public void setCahoots(Cahoots payload) {
        this.payload = payload;
    }


    private String formatForBtc(Long amount) {
        return (String.format(Locale.ENGLISH, "%.8f", getBtcValue((double) amount)).concat(" BTC"));
    }

    private String formatForSats(Long amount) {
        return formattedSatValue(getBtcValue((double) amount)).concat(" sat");
    }

    private Double getBtcValue(Double sats) {
        return (sats / 1e8);
    }

    private String formattedSatValue(Object number) {
        NumberFormat nformat = NumberFormat.getNumberInstance(Locale.US);
        DecimalFormat decimalFormat = (DecimalFormat) nformat;
        decimalFormat.applyPattern("#,###");
        return decimalFormat.format(number).replace(",", " ");
    }

    private void notifyWalletAndFinish() {
        Intent intent = new Intent("com.samourai.wallet.BalanceFragment.REFRESH");
        intent.putExtra("notifTx", false);
        intent.putExtra("fetch", true);
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).sendBroadcast(intent);
        cahootsProgressGroup.setVisibility(View.GONE);
        Intent i = new Intent(this.getActivity(), BalanceActivity.class);
        this.getActivity().finish();
        startActivity(i);
        getActivity().finish();

    }

    private Observable<TxProcessorResult> CalculateEntropy(Cahoots payload) {

        return Observable.create(emitter -> {

            Map<String, Long> inputs = new HashMap<>();
            Map<String, Long> outputs = new HashMap<>();

            int counter = 0;
            for (Map.Entry<String, Long> entry : payload.getOutpoints().entrySet()) {
                inputs.put(stubAddress[counter], entry.getValue());
                counter++;
            }

            for (int i = 0; i < payload.getTransaction().getOutputs().size(); i++) {
                TransactionOutput output = payload.getTransaction().getOutputs().get(i);
                outputs.put(stubAddress[counter + i], output.getValue().value);

            }

            TxProcessor txProcessor = new TxProcessor(BoltzmannSettings.MAX_DURATION_DEFAULT, BoltzmannSettings.MAX_TXOS_DEFAULT);
            Txos txos = new Txos(inputs, outputs);
            TxProcessorResult result = txProcessor.processTx(txos, 0.005f, TxosLinkerOptionEnum.PRECHECK, TxosLinkerOptionEnum.LINKABILITY, TxosLinkerOptionEnum.MERGE_INPUTS);
            emitter.onNext(result);
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposables.dispose();
    }
}
