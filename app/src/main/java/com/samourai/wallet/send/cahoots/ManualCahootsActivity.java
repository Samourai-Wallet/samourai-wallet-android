package com.samourai.wallet.send.cahoots;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialog;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.wallet.cahoots.STONEWALLx2;
import com.samourai.wallet.cahoots.Stowaway;
import com.samourai.wallet.cahoots._TransactionOutPoint;
import com.samourai.wallet.cahoots._TransactionOutput;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.segwit.bech32.Bech32Segwit;
import com.samourai.wallet.segwit.bech32.Bech32Util;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.whirlpool.WhirlpoolMeta;
import com.samourai.wallet.widgets.HorizontalStepsViewIndicator;
import com.samourai.wallet.widgets.ViewPager;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.script.Script;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static com.samourai.wallet.cahoots.CahootsUtil.getCahootsUTXO;

public class ManualCahootsActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private HorizontalStepsViewIndicator stepsViewGroup;
    private ArrayList<Fragment> steps = new ArrayList<>();
    private CahootReviewFragment cahootReviewFragment;
    private TextView stepCounts;
    private AppCompatDialog dialog;
    private int account = 0;
    private long amount = 0L;
    private String address = "";
    private static final String TAG = "ManualCahootsActivity";
    private Cahoots payload;
    private int type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_stone_wall);
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        cahootReviewFragment = CahootReviewFragment.newInstance();
        createSteps();
        stepsViewGroup = findViewById(R.id.step_view);
        stepCounts = findViewById(R.id.step_numbers);
        viewPager = findViewById(R.id.view_flipper);
        viewPager.enableSwipe(false);
        stepsViewGroup.setTotalSteps(5);
        steps.add(cahootReviewFragment);
        viewPager.setAdapter(new StepAdapter(getSupportFragmentManager()));

        if (getIntent().hasExtra("account")) {
            account = getIntent().getIntExtra("account", 0);
        }
        if (getIntent().hasExtra("amount")) {
            amount = getIntent().getLongExtra("amount", 0);
        }
        if (getIntent().hasExtra("type")) {
            type = getIntent().getIntExtra("type", Cahoots.CAHOOTS_STOWAWAY);
        }
        if (getIntent().hasExtra("address")) {
            address = getIntent().getStringExtra("address");
        }
        if (getIntent().hasExtra("payload")) {

            String cahootsPayload = getIntent().getStringExtra("payload");

            if (Cahoots.isCahoots(cahootsPayload.trim())) {
                try {
                    JSONObject obj = new JSONObject(cahootsPayload);
                    if (obj.has("cahoots") && obj.getJSONObject("cahoots").has("type")) {
                        int type = obj.getJSONObject("cahoots").getInt("type");
                        if (type == Cahoots.CAHOOTS_STOWAWAY) {
                            payload = new Stowaway(obj);
                            onScanCahootsPayload(payload.toJSON().toString());
//                            viewPager.setCurrentItem(payload.getStep(), true);onScanCahootsPayload(payload.toJSON().toString());
                        }
                        if (type == Cahoots.CAHOOTS_STONEWALLx2) {
                            payload = new STONEWALLx2(obj);
                            onScanCahootsPayload(payload.toJSON().toString());
//                            viewPager.setCurrentItem(payload.getStep(), true);onScanCahootsPayload(payload.toJSON().toString());
                        }
                    }
                } catch (JSONException e) {
                    finish();
                    e.printStackTrace();
                }

            }
        } else if (amount != 0L) {
            if (type == Cahoots.CAHOOTS_STOWAWAY) {
                doStowaway0(amount, account);
                return;

            }
            if (type == Cahoots.CAHOOTS_STONEWALLx2) {
                doSTONEWALLx2_0(amount, address, account);

            }
        } else {
            finish();
        }


    }
    @Override
    protected void onResume() {
        super.onResume();

        AppUtil.getInstance(this).setIsInForeground(true);

        AppUtil.getInstance(this).checkTimeOut();

    }

    private void createSteps() {
        for (int i = 0; i < 4; i++) {
            CahootsStepFragment stepView = CahootsStepFragment.newInstance(i);
            stepView.setCahootsFragmentListener(listener);
            steps.add(stepView);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.manual_stonewall_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            finish();
        }

        if(menuItem.getItemId() == R.id.action_menu_paste_cahoots){
            try {
                ClipboardManager clipboard = (ClipboardManager)  getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                onScanCahootsPayload(item.getText().toString());
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Invalid data", Toast.LENGTH_SHORT).show();
            }
        }

        return true;
    }

    private CahootsStepFragment.CahootsFragmentListener listener = new CahootsStepFragment.CahootsFragmentListener() {
        @Override
        public void onScan(int step, String qrData) {
            onScanCahootsPayload(qrData);
        }

        @Override
        public void onShare(int step) {
            shareCahootsPayload();
        }
    };

    private void onScanCahootsPayload(String qrData) {

        Stowaway stowaway = null;
        STONEWALLx2 stonewall = null;

        try {
            JSONObject obj = new JSONObject(qrData);
            Log.d("CahootsUtil", "incoming st:" + qrData);
            Log.d("CahootsUtil", "object json:" + obj.toString());
            if (obj.has("cahoots") && obj.getJSONObject("cahoots").has("type")) {

                int type = obj.getJSONObject("cahoots").getInt("type");

                if (type == Cahoots.CAHOOTS_STOWAWAY) {
                    stowaway = new Stowaway(obj);
                } else if (type == Cahoots.CAHOOTS_STONEWALLx2) {
                    stonewall = new STONEWALLx2(obj);
                } else {
                    Toast.makeText(getApplicationContext(), R.string.unrecognized_cahoots, Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                Toast.makeText(getApplicationContext(), R.string.not_cahoots, Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (JSONException je) {
            Toast.makeText(getApplicationContext(), R.string.cannot_process_cahoots, Toast.LENGTH_SHORT).show();
            return;
        }

        if (stowaway != null) {
            int step = stowaway.getStep();
            viewPager.post(() -> viewPager.setCurrentItem(step + 1, true));
            stepsViewGroup.post(() -> stepsViewGroup.setStep(step + 2));
            stepCounts.setText(String.valueOf((step + 2)).concat("/5"));

            try {
                switch (step) {
                    case 0:
                        doStowaway1(stowaway);
                        break;
                    case 1:
                        doStowaway2(stowaway);
                        break;
                    case 2:
                        doStowaway3(stowaway);
                        break;
                    case 3:
                        doStowaway4(stowaway);
                        break;
                    default:
                        Toast.makeText(getApplicationContext(), R.string.unrecognized_step, Toast.LENGTH_SHORT).show();
                        break;
                }
            } catch (Exception e) {
//            Toast.makeText(getApplicationContext(), R.string.cannot_process_stonewall, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }

        if (stonewall != null) {
            int step = stonewall.getStep();
            viewPager.post(() -> viewPager.setCurrentItem(step + 1, true));
            stepsViewGroup.post(() -> stepsViewGroup.setStep(step + 2));
            stepCounts.setText(String.valueOf((step + 2)).concat("/5"));
            if(step == 2){
                ( (CahootsStepFragment) steps.get(step + 1) ).setStowaway(stonewall);
            }
            try {
                switch (step) {
                    case 0:
                        stonewall.setCounterpartyAccount(account);  // set counterparty account
                        doSTONEWALLx2_1(stonewall);
                        break;
                    case 1:
                        doSTONEWALLx2_2(stonewall);
                        break;
                    case 2:
                        doSTONEWALLx2_3(stonewall);
                        break;
                    case 3:
                        doSTONEWALLx2_4(stonewall);
                        break;
                    default:
                        Toast.makeText(this, R.string.unrecognized_step, Toast.LENGTH_SHORT).show();
                        break;
                }
            } catch (Exception e) {
                Toast.makeText(this, R.string.cannot_process_stowaway, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                Log.d("CahootsUtil", e.getMessage());
            }
        }

    }

    private void processScan(String code) {
        Toast.makeText(getApplicationContext(), code, Toast.LENGTH_LONG).show();
    }

    private class StepAdapter extends FragmentPagerAdapter {


        StepAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return steps.get(position);
        }

        @Override
        public int getCount() {
            return steps.size();
        }
    }

    private void shareCahootsPayload() {

        String strCahoots = this.payload.toJSON().toString();
        final int QR_ALPHANUM_CHAR_LIMIT = 4296;    // tx max size in bytes == 2148
        dialog = new AppCompatDialog(this, R.style.stowaway_dialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.cahoots_qr_dialog_layout);
        ImageView qrCode = dialog.findViewById(R.id.qr_code_imageview);
        Button copy = dialog.findViewById(R.id.cahoots_copy_btn);
        Button share = dialog.findViewById(R.id.cahoots_share);
        TextView qrErrorMessage = dialog.findViewById(R.id.qr_error_stowaway);

        if (strCahoots.length() <= QR_ALPHANUM_CHAR_LIMIT) {
            Display display = this.getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int imgWidth = Math.max(size.x - 240, 150);

            Bitmap bitmap = null;

            QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(strCahoots, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), imgWidth);

            try {
                bitmap = qrCodeEncoder.encodeAsBitmap();
            } catch (WriterException e) {
                qrErrorMessage.setVisibility(View.VISIBLE);
                e.printStackTrace();
            }
            qrCode.setImageBitmap(bitmap);
        } else {
            qrErrorMessage.setVisibility(View.VISIBLE);
            Toast.makeText(getApplicationContext(), R.string.tx_too_large_qr, Toast.LENGTH_SHORT).show();
        }
        share.setOnClickListener(v -> {
            if (!(strCahoots.length() <= QR_ALPHANUM_CHAR_LIMIT)) {
                Intent txtIntent = new Intent(android.content.Intent.ACTION_SEND);
                txtIntent.setType("text/plain");
                txtIntent.putExtra(android.content.Intent.EXTRA_TEXT, strCahoots);
                startActivity(Intent.createChooser(txtIntent, "Share"));
                return;
            }
            String strFileName = AppUtil.getInstance(getApplicationContext()).getReceiveQRFilename();
            File file = new File(strFileName);
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
            file.setReadable(true, false);

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
            } catch (FileNotFoundException fnfe) {
            }

            if (file != null && fos != null) {
                Bitmap bitmap1 = ((BitmapDrawable) qrCode.getDrawable()).getBitmap();
                bitmap1.compress(Bitmap.CompressFormat.PNG, 0, fos);

                try {
                    fos.close();
                } catch (IOException ioe) {
                }

                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.setType("image/png");
                if (android.os.Build.VERSION.SDK_INT >= 24) {
                    //From API 24 sending FIle on intent ,require custom file provider
                    intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                            getApplicationContext(),
                            getApplicationContext().getApplicationContext()
                                    .getPackageName() + ".provider", file));
                } else {
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                }
                getApplicationContext().startActivity(Intent.createChooser(intent, getApplicationContext().getText(R.string.send_tx)));
            }

        });

        copy.setOnClickListener(v -> {

            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getApplicationContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = null;
            clip = android.content.ClipData.newPlainText("Cahoots", strCahoots);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getApplicationContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();

        });

        dialog.show();


    }

    public void doStowaway0(long spendAmount, int account) {
        // Bob -> Alice, spendAmount in sats
        stepsViewGroup.post(() -> stepsViewGroup.setStep(1));
        NetworkParameters params = SamouraiWallet.getInstance().getCurrentNetworkParams();

        //
        //
        // step0: B sends spend amount to A,  creates step0
        //
        //
        Log.d("CahootsUtil", "sender account (0):" + account);
        Stowaway stowaway0 = new Stowaway(spendAmount, params, account);
        stowaway0.setStep(0);
        payload = stowaway0;
    }

    private void doStowaway1(Stowaway stowaway0) throws Exception {

        List<UTXO> utxos = getCahootsUTXO(0);
        // sort in descending order by value
        Collections.sort(utxos, new UTXO.UTXOComparator());

        Log.d("CahootsUtil", "BIP84 utxos:" + utxos.size());

        List<UTXO> selectedUTXO = new ArrayList<UTXO>();
        long totalContributedAmount = 0L;
        for (UTXO utxo : utxos) {
            selectedUTXO.add(utxo);
            totalContributedAmount += utxo.getValue();
            Log.d("CahootsUtil", "BIP84 selected utxo:" + utxo.getValue());
            if (totalContributedAmount > stowaway0.getSpendAmount() + SamouraiWallet.bDust.longValue()) {
                break;
            }
        }
        if (!(totalContributedAmount > stowaway0.getSpendAmount() + SamouraiWallet.bDust.longValue())) {
            Toast.makeText(getApplicationContext(), R.string.cannot_compose_cahoots, Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("CahootsUtil", "BIP84 selected utxos:" + selectedUTXO.size());

        NetworkParameters params = stowaway0.getParams();

        //
        //
        // step1: A utxos -> B (take largest that cover amount)
        // A provides 5750000
        //
        //

        String zpub = BIP84Util.getInstance(getApplicationContext()).getWallet().getAccount(0).zpubstr();
        HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>> inputsA = new HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>>();
//        inputsA.put(outpoint_A0, Triple.of(Hex.decode("0221b719bc26fb49971c7dd328a6c7e4d17dfbf4e2217bee33a65c53ed3daf041e"), FormatsUtil.getInstance().getFingerprintFromXPUB("vpub5Z3hqXewnCbxnMsWygxN8AyjNVJbFeV2VCdDKpTFazdoC29qK4Y5DSQ1aaAPBrsBZ1TzN5va6xGy4eWa9uAqh2AwifuA1eofkedh3eUgF6b"), "M/0/4"));
//        inputsA.put(outpoint_A1, Triple.of(Hex.decode("020ab261e1a3cf986ecb3cd02299de36295e804fd799934dc5c99dde0d25e71b93"), FormatsUtil.getInstance().getFingerprintFromXPUB("vpub5Z3hqXewnCbxnMsWygxN8AyjNVJbFeV2VCdDKpTFazdoC29qK4Y5DSQ1aaAPBrsBZ1TzN5va6xGy4eWa9uAqh2AwifuA1eofkedh3eUgF6b"), "M/0/2"));

        for (UTXO utxo : selectedUTXO) {
            for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                _TransactionOutPoint _outpoint = new _TransactionOutPoint(outpoint);

                ECKey eckey = SendFactory.getPrivKey(_outpoint.getAddress(), 0);
                String path = APIFactory.getInstance(getApplicationContext()).getUnspentPaths().get(_outpoint.getAddress());
                inputsA.put(_outpoint, Triple.of(eckey.getPubKey(), FormatsUtil.getInstance().getFingerprintFromXPUB(zpub), path));
            }
        }

        // destination output
        int idx = BIP84Util.getInstance(getApplicationContext()).getWallet().getAccount(0).getReceive().getAddrIdx();
        SegwitAddress segwitAddress = BIP84Util.getInstance(getApplicationContext()).getAddressAt(0, idx);
        HashMap<_TransactionOutput, Triple<byte[], byte[], String>> outputsA = new HashMap<_TransactionOutput, Triple<byte[], byte[], String>>();
//        byte[] scriptPubKey_A = getScriptPubKey("tb1qewwlc2dksuez3zauf38d82m7uqd4ewkf2avdl8", params);
        Pair<Byte, byte[]> pair = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress.getBech32AsString());
        byte[] scriptPubKey_A = Bech32Segwit.getScriptPubkey(pair.getLeft(), pair.getRight());
        _TransactionOutput output_A0 = new _TransactionOutput(params, null, Coin.valueOf(stowaway0.getSpendAmount()), scriptPubKey_A);
        outputsA.put(output_A0, Triple.of(segwitAddress.getECKey().getPubKey(), FormatsUtil.getInstance().getFingerprintFromXPUB(BIP84Util.getInstance(getApplicationContext()).getWallet().getAccount(0).zpubstr()), "M/0/" + idx));

        stowaway0.setDestination(segwitAddress.getBech32AsString());

        Stowaway stowaway1 = new Stowaway(stowaway0);
        stowaway1.inc(inputsA, outputsA, null);
        stowaway1.setStep(1);

        payload = stowaway1;
    }

    //
    // sender
    //
    private void doStowaway2(Stowaway stowaway1) throws Exception {

        Log.d("CahootsUtil", "sender account (2):" + stowaway1.getAccount());
//        int account = stowaway1.getAccount();

        Transaction transaction = stowaway1.getTransaction();
        Log.d("CahootsUtil", "step2 tx:" + org.spongycastle.util.encoders.Hex.toHexString(transaction.bitcoinSerialize()));
//        Log.d("CahootsUtil", "input value:" + transaction.getInputs().get(0).getValue().longValue());
        int nbIncomingInputs = transaction.getInputs().size();

        List<UTXO> utxos = getCahootsUTXO(stowaway1.getAccount());
        // sort in ascending order by value
        Collections.sort(utxos, new UTXO.UTXOComparator());
        Collections.reverse(utxos);

        Log.d("CahootsUtil", "BIP84 utxos:" + utxos.size());

        List<UTXO> selectedUTXO = new ArrayList<UTXO>();
        int nbTotalSelectedOutPoints = 0;
        long totalSelectedAmount = 0L;
        for (UTXO utxo : utxos) {
            selectedUTXO.add(utxo);
            totalSelectedAmount += utxo.getValue();
            Log.d("BIP84 selected utxo:", "" + utxo.getValue());
            nbTotalSelectedOutPoints += utxo.getOutpoints().size();
            if (totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue() + stowaway1.getSpendAmount() + SamouraiWallet.bDust.longValue()) {

                // discard "extra" utxo, if any
                List<UTXO> _selectedUTXO = new ArrayList<UTXO>();
                Collections.reverse(selectedUTXO);
                int _nbTotalSelectedOutPoints = 0;
                long _totalSelectedAmount = 0L;
                for (UTXO utxoSel : selectedUTXO) {
                    _selectedUTXO.add(utxoSel);
                    _totalSelectedAmount += utxoSel.getValue();
                    Log.d("CahootsUtil", "BIP84 post selected utxo:" + utxoSel.getValue());
                    _nbTotalSelectedOutPoints += utxoSel.getOutpoints().size();
                    if (_totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, _nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue() + stowaway1.getSpendAmount() + SamouraiWallet.bDust.longValue()) {
                        selectedUTXO.clear();
                        selectedUTXO.addAll(_selectedUTXO);
                        totalSelectedAmount = _totalSelectedAmount;
                        nbTotalSelectedOutPoints = _nbTotalSelectedOutPoints;
                        break;
                    }
                }

                break;
            }
        }
        if (!(totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue() + stowaway1.getSpendAmount() + SamouraiWallet.bDust.longValue())) {
            Toast.makeText(getApplicationContext(), R.string.cannot_compose_cahoots, Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("CahootsUtil", "BIP84 selected utxos:" + selectedUTXO.size());

        long fee = FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 2).longValue();
        Log.d("CahootsUtil", "fee:" + fee);

        NetworkParameters params = stowaway1.getParams();

        //
        //
        // step2: B verif, utxos -> A (take smallest that cover amount)
        // B provides 1000000, 1500000 (250000 change, A input larger)
        //
        //

        String zpub = BIP84Util.getInstance(getApplicationContext()).getWallet().getAccountAt(stowaway1.getAccount()).zpubstr();
        HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>> inputsB = new HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>>();

        for (UTXO utxo : selectedUTXO) {
            for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                _TransactionOutPoint _outpoint = new _TransactionOutPoint(outpoint);

                ECKey eckey = SendFactory.getPrivKey(_outpoint.getAddress(), stowaway1.getAccount());
                String path = APIFactory.getInstance(getApplicationContext()).getUnspentPaths().get(_outpoint.getAddress());
                inputsB.put(_outpoint, Triple.of(eckey.getPubKey(), FormatsUtil.getInstance().getFingerprintFromXPUB(zpub), path));
            }
        }

        Log.d("CahootsUtil", "inputsB:" + inputsB.size());

        // change output
        SegwitAddress segwitAddress = null;
        int idx = 0;
        if (stowaway1.getAccount() == WhirlpoolMeta.getInstance(getApplicationContext()).getWhirlpoolPostmix()) {
            idx = AddressFactory.getInstance(getApplicationContext()).getHighestPostChangeIdx();
            HD_Address addr = BIP84Util.getInstance(getApplicationContext()).getWallet().getAccountAt(stowaway1.getAccount()).getChange().getAddressAt(idx);
            segwitAddress = new SegwitAddress(addr.getPubKey(), SamouraiWallet.getInstance().getCurrentNetworkParams());
        } else {
            idx = BIP84Util.getInstance(getApplicationContext()).getWallet().getAccount(0).getChange().getAddrIdx();
            segwitAddress = BIP84Util.getInstance(getApplicationContext()).getAddressAt(1, idx);
        }
        HashMap<_TransactionOutput, Triple<byte[], byte[], String>> outputsB = new HashMap<_TransactionOutput, Triple<byte[], byte[], String>>();
        Pair<Byte, byte[]> pair = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress.getBech32AsString());
        byte[] scriptPubKey_B = Bech32Segwit.getScriptPubkey(pair.getLeft(), pair.getRight());
        _TransactionOutput output_B0 = new _TransactionOutput(params, null, Coin.valueOf((totalSelectedAmount - stowaway1.getSpendAmount()) - fee), scriptPubKey_B);
        outputsB.put(output_B0, Triple.of(segwitAddress.getECKey().getPubKey(), FormatsUtil.getInstance().getFingerprintFromXPUB(BIP84Util.getInstance(getApplicationContext()).getWallet().getAccountAt(stowaway1.getAccount()).zpubstr()), "M/1/" + idx));

        Log.d("CahootsUtil", "outputsB:" + outputsB.size());

        Stowaway stowaway2 = new Stowaway(stowaway1);
//        stowaway2.setAccount(account);
        stowaway2.inc(inputsB, outputsB, null);
        System.out.println("step 2:" + stowaway2.toJSON().toString(2));
//        steps.get(2).setCahootsPayload(stowaway2.toJSON().toString());
        stowaway2.setStep(2);
        stowaway2.setFeeAmount(fee);

        payload = stowaway2;

    }

    //
    // receiver
    //
    private void doStowaway3(Stowaway stowaway2) throws Exception {

        Log.d("CahootsUtil", "sender account (3):" + stowaway2.getAccount());

        HashMap<String, String> utxo2Address = new HashMap<String, String>();
        List<UTXO> utxos = APIFactory.getInstance(getApplicationContext()).getUtxos(true);
        for (UTXO utxo : utxos) {
            for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                utxo2Address.put(outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN(), outpoint.getAddress());
                Log.d("CahootsUtil", "outpoint address:" + outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN() + "," + outpoint.getAddress());
            }
        }

        Transaction transaction = stowaway2.getPSBT().getTransaction();
        HashMap<String, ECKey> keyBag_A = new HashMap<String, ECKey>();
        for (TransactionInput input : transaction.getInputs()) {
            TransactionOutPoint outpoint = input.getOutpoint();
            String key = outpoint.getHash().toString() + "-" + outpoint.getIndex();
            if (utxo2Address.containsKey(key)) {
                String address = utxo2Address.get(key);
                ECKey eckey = SendFactory.getPrivKey(address, 0);
                keyBag_A.put(outpoint.toString(), eckey);
            }
        }

        //
        //
        // step3: A verif, BIP69, sig
        //
        //
        /*
        HashMap<String,ECKey> keyBag_A = new HashMap<String,ECKey>();
        keyBag_A.put(outpoint_A0.toString(), ecKey_A0);
        keyBag_A.put(outpoint_A1.toString(), ecKey_A1);
        */

        Stowaway stowaway3 = new Stowaway(stowaway2);
        stowaway3.inc(null, null, keyBag_A);
        System.out.println("step 3:" + stowaway3.toJSON().toString(2));
//        steps.get(3).setCahootsPayload(stowaway3.toJSON().toString());
        stowaway3.setStep(3);
        payload = stowaway3;
//        cahootReviewFragment.setCahoots(payload);

//        shareCahootsPayload(stowaway3.toJSON().toString());
    }

    //
    // sender
    //
    private void doStowaway4(Stowaway stowaway3) throws Exception {

        Log.d("CahootsUtil", "sender account (4):" + stowaway3.getAccount());
//        stowaway3.setAccount(WhirlpoolMeta.getInstance(context).getWhirlpoolPostmix());

        HashMap<String, String> utxo2Address = new HashMap<String, String>();
        List<UTXO> utxos = null;
        if (stowaway3.getAccount() == WhirlpoolMeta.getInstance(getApplicationContext()).getWhirlpoolPostmix()) {
            utxos = APIFactory.getInstance(getApplicationContext()).getUtxosPostMix(true);
        } else {
            utxos = APIFactory.getInstance(getApplicationContext()).getUtxos(true);
        }
        for (UTXO utxo : utxos) {
            for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                utxo2Address.put(outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN(), outpoint.getAddress());
                Log.d("CahootsUtil", "outpoint address:" + outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN() + "," + outpoint.getAddress());
            }
        }

        Transaction transaction = stowaway3.getPSBT().getTransaction();
        HashMap<String, ECKey> keyBag_B = new HashMap<String, ECKey>();
        for (TransactionInput input : transaction.getInputs()) {
            TransactionOutPoint outpoint = input.getOutpoint();
            String key = outpoint.getHash().toString() + "-" + outpoint.getIndex();
            if (utxo2Address.containsKey(key)) {
                String address = utxo2Address.get(key);
                ECKey eckey = SendFactory.getPrivKey(address, stowaway3.getAccount());
                keyBag_B.put(outpoint.toString(), eckey);
            }
        }

        //
        //
        // step4: B verif, sig, broadcast
        //
        //

        /*
        HashMap<String,ECKey> keyBag_B = new HashMap<String,ECKey>();
        keyBag_B.put(outpoint_B0.toString(), ecKey_B0);
        keyBag_B.put(outpoint_B1.toString(), ecKey_B1);
        */

        Stowaway stowaway4 = new Stowaway(stowaway3);
        stowaway4.inc(null, null, keyBag_B);
        System.out.println("step 4:" + stowaway4.toJSON().toString());
        System.out.println("step 4 psbt:" + stowaway4.getPSBT().toString());
        System.out.println("step 4 tx:" + stowaway4.getTransaction().toString());
        System.out.println("step 4 tx hex:" + Hex.toHexString(stowaway4.getTransaction().bitcoinSerialize()));

//        Stowaway s = new Stowaway(stowaway4.toJSON());
//        System.out.println(s.toJSON().toString());
//        steps.get(4).setCahootsPayload(stowaway4.toJSON().toString());
        stowaway4.setStep(4);
        payload = stowaway4;

        cahootReviewFragment.setCahoots(payload);

        // broadcast ???
//        shareCahootsPayload(stowaway4.toJSON().toString());
    }

    //
    // sender
    //
    public void doSTONEWALLx2_0(long spendAmount, String address, int account) {
        stepsViewGroup.post(() -> stepsViewGroup.setStep(1));

        // Bob -> Alice, spendAmount in sats
        NetworkParameters params = SamouraiWallet.getInstance().getCurrentNetworkParams();

        //
        //
        // step0: B sends spend amount to A,  creates step0
        //
        //
        STONEWALLx2 stonewall0 = new STONEWALLx2(spendAmount, address, params, account);
//        System.out.println(stonewall0.toJSON().toString(2));

//        shareCahootsPayload(stonewall0.toJSON().toString());
        payload = stonewall0;
    }


    //
    // counterparty
    //
    private void doSTONEWALLx2_1(STONEWALLx2 stonewall0) throws Exception {

        List<UTXO> utxos = getCahootsUTXO(stonewall0.getCounterpartyAccount());
        Collections.shuffle(utxos);

        Log.d("CahootsUtil", "BIP84 utxos:" + utxos.size());

        List<UTXO> selectedUTXO = new ArrayList<UTXO>();
        long totalContributedAmount = 0L;
        for (int step = 0; step < 3; step++) {

            if (stonewall0.getCounterpartyAccount() == 0) {
                step = 2;
            }

            List<String> seenTxs = new ArrayList<String>();
            selectedUTXO = new ArrayList<UTXO>();
            totalContributedAmount = 0L;
            for (UTXO utxo : utxos) {

                switch (step) {
                    case 0:
                        if (utxo.getPath() != null && utxo.getPath().length() > 3 && utxo.getPath().charAt(2) != '0') {
                            continue;
                        }
                        break;
                    case 1:
                        if (utxo.getPath() != null && utxo.getPath().length() > 3 && utxo.getPath().charAt(2) != '1') {
                            continue;
                        }
                        break;
                    default:
                        break;
                }

                UTXO _utxo = new UTXO();
                for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                    if (!seenTxs.contains(outpoint.getTxHash().toString())) {
                        _utxo.getOutpoints().add(outpoint);
                        seenTxs.add(outpoint.getTxHash().toString());
                    }
                }

                if (_utxo.getOutpoints().size() > 0) {
                    selectedUTXO.add(_utxo);
                    totalContributedAmount += _utxo.getValue();
                    Log.d("CahootsUtil", "BIP84 selected utxo:" + _utxo.getValue());
                }

                if (totalContributedAmount > stonewall0.getSpendAmount() + SamouraiWallet.bDust.longValue()) {
                    break;
                }
            }
            if (totalContributedAmount > stonewall0.getSpendAmount() + SamouraiWallet.bDust.longValue()) {
                break;
            }
        }
        if (!(totalContributedAmount > stonewall0.getSpendAmount() + SamouraiWallet.bDust.longValue())) {
            Toast.makeText(this, R.string.cannot_compose_cahoots, Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("CahootsUtil", "BIP84 selected utxos:" + selectedUTXO.size());

        NetworkParameters params = stonewall0.getParams();

        //
        //
        // step1: A utxos -> B (take largest that cover amount)
        // A provides 5750000
        //
        //

        String zpub = BIP84Util.getInstance(getApplicationContext()).getWallet().getAccountAt(stonewall0.getCounterpartyAccount()).zpubstr();
        HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>> inputsA = new HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>>();

        for (UTXO utxo : selectedUTXO) {
            for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                _TransactionOutPoint _outpoint = new _TransactionOutPoint(outpoint);

                ECKey eckey = SendFactory.getPrivKey(_outpoint.getAddress(), stonewall0.getCounterpartyAccount());
                String path = APIFactory.getInstance(getApplicationContext()).getUnspentPaths().get(_outpoint.getAddress());
                inputsA.put(_outpoint, Triple.of(eckey.getPubKey(), FormatsUtil.getInstance().getFingerprintFromXPUB(zpub), path));
            }
        }

        HashMap<_TransactionOutput, Triple<byte[], byte[], String>> outputsA = new HashMap<_TransactionOutput, Triple<byte[], byte[], String>>();
        if (stonewall0.getCounterpartyAccount() == WhirlpoolMeta.getInstance(getApplicationContext()).getWhirlpoolPostmix()) {
            // contributor mix output
            int idx = AddressFactory.getInstance(getApplicationContext()).getHighestPostChangeIdx();
            SegwitAddress segwitAddress0 = BIP84Util.getInstance(getApplicationContext()).getAddressAt(stonewall0.getCounterpartyAccount(), 1, idx);
            Pair<Byte, byte[]> pair0 = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress0.getBech32AsString());
            byte[] scriptPubKey_A0 = Bech32Segwit.getScriptPubkey(pair0.getLeft(), pair0.getRight());
            _TransactionOutput output_A0 = new _TransactionOutput(params, null, Coin.valueOf(stonewall0.getSpendAmount()), scriptPubKey_A0);
            outputsA.put(output_A0, Triple.of(segwitAddress0.getECKey().getPubKey(), FormatsUtil.getInstance().getFingerprintFromXPUB(BIP84Util.getInstance(getApplicationContext()).getWallet().getAccountAt(stonewall0.getCounterpartyAccount()).zpubstr()), "M/1/" + idx));

            // contributor change output
            ++idx;
            SegwitAddress segwitAddress1 = BIP84Util.getInstance(getApplicationContext()).getAddressAt(stonewall0.getCounterpartyAccount(), 1, idx);
            Pair<Byte, byte[]> pair1 = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress1.getBech32AsString());
            byte[] scriptPubKey_A1 = Bech32Segwit.getScriptPubkey(pair1.getLeft(), pair1.getRight());
            _TransactionOutput output_A1 = new _TransactionOutput(params, null, Coin.valueOf(totalContributedAmount - stonewall0.getSpendAmount()), scriptPubKey_A1);
            outputsA.put(output_A1, Triple.of(segwitAddress1.getECKey().getPubKey(), FormatsUtil.getInstance().getFingerprintFromXPUB(BIP84Util.getInstance(getApplicationContext()).getWallet().getAccountAt(stonewall0.getCounterpartyAccount()).zpubstr()), "M/1/" + idx));
        } else {
            // contributor mix output
            int idx = BIP84Util.getInstance(getApplicationContext()).getWallet().getAccount(0).getReceive().getAddrIdx();
            SegwitAddress segwitAddress0 = BIP84Util.getInstance(getApplicationContext()).getAddressAt(0, 0, idx);
            if (segwitAddress0.getBech32AsString().equalsIgnoreCase(stonewall0.getDestination())) {
                segwitAddress0 = BIP84Util.getInstance(getApplicationContext()).getAddressAt(0, 0, idx + 1);
            }
            Pair<Byte, byte[]> pair0 = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress0.getBech32AsString());
            byte[] scriptPubKey_A0 = Bech32Segwit.getScriptPubkey(pair0.getLeft(), pair0.getRight());
            _TransactionOutput output_A0 = new _TransactionOutput(params, null, Coin.valueOf(stonewall0.getSpendAmount()), scriptPubKey_A0);
            outputsA.put(output_A0, Triple.of(segwitAddress0.getECKey().getPubKey(), FormatsUtil.getInstance().getFingerprintFromXPUB(BIP84Util.getInstance(getApplicationContext()).getWallet().getAccount(0).zpubstr()), "M/0/" + idx));

            // contributor change output
            idx = BIP84Util.getInstance(getApplicationContext()).getWallet().getAccount(0).getChange().getAddrIdx();
            SegwitAddress segwitAddress1 = BIP84Util.getInstance(getApplicationContext()).getAddressAt(0, 1, idx);
            Pair<Byte, byte[]> pair1 = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress1.getBech32AsString());
            byte[] scriptPubKey_A1 = Bech32Segwit.getScriptPubkey(pair1.getLeft(), pair1.getRight());
            _TransactionOutput output_A1 = new _TransactionOutput(params, null, Coin.valueOf(totalContributedAmount - stonewall0.getSpendAmount()), scriptPubKey_A1);
            outputsA.put(output_A1, Triple.of(segwitAddress1.getECKey().getPubKey(), FormatsUtil.getInstance().getFingerprintFromXPUB(BIP84Util.getInstance(getApplicationContext()).getWallet().getAccount(0).zpubstr()), "M/1/" + idx));
        }

        STONEWALLx2 stonewall1 = new STONEWALLx2(stonewall0);
        stonewall1.inc(inputsA, outputsA, null);

//        doCahoots(stonewall1.toJSON().toString());
        payload = stonewall1;
    }

    //
    // sender
    //
    private void doSTONEWALLx2_2(STONEWALLx2 stonewall1) throws Exception {

        Transaction transaction = stonewall1.getTransaction();
        Log.d("CahootsUtil", "step2 tx:" + org.spongycastle.util.encoders.Hex.toHexString(transaction.bitcoinSerialize()));
//        Log.d("CahootsUtil", "input value:" + transaction.getInputs().get(0).getValue().longValue());
        int nbIncomingInputs = transaction.getInputs().size();

        List<UTXO> utxos = getCahootsUTXO(stonewall1.getAccount());
        Collections.shuffle(utxos);

        Log.d("CahootsUtil", "BIP84 utxos:" + utxos.size());

        List<String> seenTxs = new ArrayList<String>();
        for (TransactionInput input : transaction.getInputs()) {
            if (!seenTxs.contains(input.getOutpoint().getHash().toString())) {
                seenTxs.add(input.getOutpoint().getHash().toString());
            }
        }

        List<UTXO> selectedUTXO = new ArrayList<UTXO>();
        long totalSelectedAmount = 0L;
        int nbTotalSelectedOutPoints = 0;
        for (int step = 0; step < 3; step++) {

            if (stonewall1.getCounterpartyAccount() == 0) {
                step = 2;
            }

            List<String> _seenTxs = seenTxs;
            selectedUTXO = new ArrayList<UTXO>();
            nbTotalSelectedOutPoints = 0;
            for (UTXO utxo : utxos) {

                switch (step) {
                    case 0:
                        if (utxo.getPath() != null && utxo.getPath().length() > 3 && utxo.getPath().charAt(2) != '0') {
                            continue;
                        }
                        break;
                    case 1:
                        if (utxo.getPath() != null && utxo.getPath().length() > 3 && utxo.getPath().charAt(2) != '1') {
                            continue;
                        }
                        break;
                    default:
                        break;
                }

                UTXO _utxo = new UTXO();
                for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                    if (!_seenTxs.contains(outpoint.getTxHash().toString())) {
                        _utxo.getOutpoints().add(outpoint);
                        _seenTxs.add(outpoint.getTxHash().toString());
                    }
                }

                if (_utxo.getOutpoints().size() > 0) {
                    selectedUTXO.add(_utxo);
                    totalSelectedAmount += _utxo.getValue();
                    nbTotalSelectedOutPoints += _utxo.getOutpoints().size();
                    Log.d("CahootsUtil", "BIP84 selected utxo:" + _utxo.getValue());
                }

                if (totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 4).longValue() + stonewall1.getSpendAmount() + SamouraiWallet.bDust.longValue()) {
                    break;
                }
            }
            if (totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 4).longValue() + stonewall1.getSpendAmount() + SamouraiWallet.bDust.longValue()) {
                break;
            }
        }
        if (!(totalSelectedAmount > FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 4).longValue() + stonewall1.getSpendAmount() + SamouraiWallet.bDust.longValue())) {
            Toast.makeText(this, R.string.cannot_compose_cahoots, Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("CahootsUtil", "BIP84 selected utxos:" + selectedUTXO.size());

        long fee = FeeUtil.getInstance().estimatedFeeSegwit(0, 0, nbTotalSelectedOutPoints + nbIncomingInputs, 4).longValue();
        Log.d("CahootsUtil", "fee:" + fee);
        if (fee % 2L != 0) {
            fee++;
        }
        Log.d("CahootsUtil", "fee pair:" + fee);
        stonewall1.setFeeAmount(fee);

        Log.d("CahootsUtil", "destination:" + stonewall1.getDestination());
        if (transaction.getOutputs() != null && transaction.getOutputs().size() == 2) {
            for (int i = 0; i < 2; i++) {
                byte[] buf = transaction.getOutputs().get(i).getScriptBytes();
                byte[] script = new byte[buf.length - 1];
                System.arraycopy(buf, 1, script, 0, script.length);
                Log.d("CahootsUtil", "script:" + new Script(script).toString());
                Log.d("CahootsUtil", "address from script:" + Bech32Util.getInstance().getAddressFromScript(new Script(script)));
                if (Bech32Util.getInstance().getAddressFromScript(new Script(script)) == null ||
                        (!Bech32Util.getInstance().getAddressFromScript(new Script(script)).equalsIgnoreCase(stonewall1.getDestination())
                                && transaction.getOutputs().get(i).getValue().longValue() != stonewall1.getSpendAmount())
                ) {
                    Log.d("CahootsUtil", "output value:" + transaction.getOutputs().get(i).getValue().longValue());
                    Coin value = transaction.getOutputs().get(i).getValue();
                    Coin _value = Coin.valueOf(value.longValue() - (fee / 2L));
                    Log.d("CahootsUtil", "output value post fee:" + _value);
                    transaction.getOutputs().get(i).setValue(_value);
                    stonewall1.getPSBT().setTransaction(transaction);
                    break;
                }
            }
        } else {
            return;
        }

        NetworkParameters params = stonewall1.getParams();

        //
        //
        // step2: B verif, utxos -> A (take smallest that cover amount)
        // B provides 1000000, 1500000 (250000 change, A input larger)
        //
        //

        String zpub = BIP84Util.getInstance(getApplicationContext()).getWallet().getAccountAt(stonewall1.getAccount()).zpubstr();
        HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>> inputsB = new HashMap<_TransactionOutPoint, Triple<byte[], byte[], String>>();

        for (UTXO utxo : selectedUTXO) {
            for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                _TransactionOutPoint _outpoint = new _TransactionOutPoint(outpoint);

                ECKey eckey = SendFactory.getPrivKey(_outpoint.getAddress(), stonewall1.getAccount());
                String path = APIFactory.getInstance(getApplicationContext()).getUnspentPaths().get(_outpoint.getAddress());
                inputsB.put(_outpoint, Triple.of(eckey.getPubKey(), FormatsUtil.getInstance().getFingerprintFromXPUB(zpub), path));
            }
        }

        // spender change output
        HashMap<_TransactionOutput, Triple<byte[], byte[], String>> outputsB = new HashMap<_TransactionOutput, Triple<byte[], byte[], String>>();
        if (stonewall1.getAccount() == WhirlpoolMeta.getInstance(getApplicationContext()).getWhirlpoolPostmix()) {
            int idx = AddressFactory.getInstance(getApplicationContext()).getHighestPostChangeIdx();
            SegwitAddress segwitAddress = BIP84Util.getInstance(getApplicationContext()).getAddressAt(stonewall1.getAccount(), 1, idx);
            Pair<Byte, byte[]> pair0 = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress.getBech32AsString());
            byte[] scriptPubKey_B0 = Bech32Segwit.getScriptPubkey(pair0.getLeft(), pair0.getRight());
            _TransactionOutput output_B0 = new _TransactionOutput(params, null, Coin.valueOf((totalSelectedAmount - stonewall1.getSpendAmount()) - (fee / 2L)), scriptPubKey_B0);
            outputsB.put(output_B0, Triple.of(segwitAddress.getECKey().getPubKey(), FormatsUtil.getInstance().getFingerprintFromXPUB(BIP84Util.getInstance(getApplicationContext()).getWallet().getAccountAt(stonewall1.getAccount()).zpubstr()), "M/1/" + idx));
        } else {
            int idx = BIP84Util.getInstance(getApplicationContext()).getWallet().getAccount(0).getChange().getAddrIdx();
            SegwitAddress segwitAddress = BIP84Util.getInstance(getApplicationContext()).getAddressAt(0, 1, idx);
            Pair<Byte, byte[]> pair0 = Bech32Segwit.decode(SamouraiWallet.getInstance().isTestNet() ? "tb" : "bc", segwitAddress.getBech32AsString());
            byte[] scriptPubKey_B0 = Bech32Segwit.getScriptPubkey(pair0.getLeft(), pair0.getRight());
            _TransactionOutput output_B0 = new _TransactionOutput(params, null, Coin.valueOf((totalSelectedAmount - stonewall1.getSpendAmount()) - (fee / 2L)), scriptPubKey_B0);
            outputsB.put(output_B0, Triple.of(segwitAddress.getECKey().getPubKey(), FormatsUtil.getInstance().getFingerprintFromXPUB(BIP84Util.getInstance(getApplicationContext()).getWallet().getAccount(0).zpubstr()), "M/1/" + idx));
        }

        STONEWALLx2 stonewall2 = new STONEWALLx2(stonewall1);
        stonewall2.inc(inputsB, outputsB, null);
        System.out.println("step 2:" + stonewall2.toJSON().toString());

//        doCahoots(stonewall2.toJSON().toString());
        payload = stonewall2;
    }

    //
    // counterparty
    //
    private void doSTONEWALLx2_3(STONEWALLx2 stonewall2) throws Exception {

        HashMap<String, String> utxo2Address = new HashMap<String, String>();
        List<UTXO> utxos = null;
        if (stonewall2.getCounterpartyAccount() == WhirlpoolMeta.getInstance(getApplicationContext()).getWhirlpoolPostmix()) {
            utxos = APIFactory.getInstance(getApplicationContext()).getUtxosPostMix(true);
        } else {
            utxos = APIFactory.getInstance(getApplicationContext()).getUtxos(true);
        }
        for (UTXO utxo : utxos) {
            for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                utxo2Address.put(outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN(), outpoint.getAddress());
            }
        }

//        Transaction transaction = stonewall2.getPSBT().getTransaction();
        Transaction transaction = stonewall2.getTransaction();
        HashMap<String, ECKey> keyBag_A = new HashMap<String, ECKey>();
        for (TransactionInput input : transaction.getInputs()) {
            TransactionOutPoint outpoint = input.getOutpoint();
            String key = outpoint.getHash().toString() + "-" + outpoint.getIndex();
            if (utxo2Address.containsKey(key)) {
                String address = utxo2Address.get(key);
                ECKey eckey = SendFactory.getPrivKey(address, stonewall2.getCounterpartyAccount());
                keyBag_A.put(outpoint.toString(), eckey);
            }
        }

        //
        //
        // step3: A verif, BIP69, sig
        //
        //
        /*
        HashMap<String,ECKey> keyBag_A = new HashMap<String,ECKey>();
        keyBag_A.put(outpoint_A0.toString(), ecKey_A0);
        keyBag_A.put(outpoint_A1.toString(), ecKey_A1);
        */

        STONEWALLx2 stonewall3 = new STONEWALLx2(stonewall2);
        stonewall3.inc(null, null, keyBag_A);
        System.out.println("step 3:" + stonewall3.toJSON().toString());

//        doCahoots(stonewall3.toJSON().toString());
        payload = stonewall3;
    }

    //
    // sender
    //
    private void doSTONEWALLx2_4(STONEWALLx2 stonewall3) throws Exception {
        

        HashMap<String, String> utxo2Address = new HashMap<String, String>();
        List<UTXO> utxos = null;
        if (stonewall3.getAccount() == WhirlpoolMeta.getInstance(getApplicationContext()).getWhirlpoolPostmix()) {
            utxos = APIFactory.getInstance(getApplicationContext()).getUtxosPostMix(true);
        } else {
            utxos = APIFactory.getInstance(getApplicationContext()).getUtxos(true);
        }
        for (UTXO utxo : utxos) {
            for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                utxo2Address.put(outpoint.getTxHash().toString() + "-" + outpoint.getTxOutputN(), outpoint.getAddress());
            }
        }

//        Transaction transaction = stonewall3.getPSBT().getTransaction();
        Transaction transaction = stonewall3.getTransaction();
        HashMap<String, ECKey> keyBag_B = new HashMap<String, ECKey>();
        for (TransactionInput input : transaction.getInputs()) {
            TransactionOutPoint outpoint = input.getOutpoint();
            String key = outpoint.getHash().toString() + "-" + outpoint.getIndex();
            if (utxo2Address.containsKey(key)) {
                String address = utxo2Address.get(key);
                ECKey eckey = SendFactory.getPrivKey(address, stonewall3.getAccount());
                keyBag_B.put(outpoint.toString(), eckey);
            }
        }

        //
        //
        // step4: B verif, sig, broadcast
        //
        //

        /*
        HashMap<String,ECKey> keyBag_B = new HashMap<String,ECKey>();
        keyBag_B.put(outpoint_B0.toString(), ecKey_B0);
        keyBag_B.put(outpoint_B1.toString(), ecKey_B1);
        */

        STONEWALLx2 stonewall4 = new STONEWALLx2(stonewall3);
        stonewall4.inc(null, null, keyBag_B);
        System.out.println("step 4:" + stonewall4.toJSON().toString());
        System.out.println("step 4 psbt:" + stonewall4.getPSBT().toString());
        System.out.println("step 4 tx:" + stonewall4.getTransaction().toString());
        System.out.println("step 4 tx hex:" + Hex.toHexString(stonewall4.getTransaction().bitcoinSerialize()));

//        Stowaway s = new Stowaway(stowaway4.toJSON());
//        System.out.println(s.toJSON().toString());

        // broadcast ???
//        doCahoots(stonewall4.toJSON().toString());
        payload = stonewall4;
        cahootReviewFragment.setCahoots(payload);

    }


}
