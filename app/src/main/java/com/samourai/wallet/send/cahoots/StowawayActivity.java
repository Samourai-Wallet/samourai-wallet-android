package com.samourai.wallet.send.cahoots;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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

public class ManualStoneWall extends AppCompatActivity {

    private ViewPager viewPager;
    private HorizontalStepsViewIndicator stepsViewGroup;
    private ViewGroup broadCastReviewView;
    private ArrayList<Fragment> steps = new ArrayList<>();
    private CahootReviewFragment cahootReviewFragment;
    private AppCompatDialog dialog;
    private int account = 0;
    private long amount = 0L;
    private static final String TAG = "ManualStoneWall";
    private Cahoots payload;

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
        viewPager = findViewById(R.id.view_flipper);
        viewPager.enableSwipe(false);
        stepsViewGroup.setTotalSteps(5);
        viewPager.addOnPageChangeListener(new android.support.v4.view.ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                stepsViewGroup.setStep(position + 1);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        steps.add(cahootReviewFragment);
        broadCastReviewView = (ViewGroup) getLayoutInflater().inflate(R.layout.stonewall_broadcast_details, (ViewGroup) stepsViewGroup.getRootView(), false);
        viewPager.setAdapter(new StepAdapter(getSupportFragmentManager()));
        if (getIntent().hasExtra("account")) {
            account = getIntent().getIntExtra("account", 0);
        }
        if (getIntent().hasExtra("amount")) {
            amount = getIntent().getLongExtra("amount", 0);
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
//                            viewPager.setCurrentItem(payload.getStep(), true);onScanCahootsPayload(payload.toJSON().toString());
                        }
                    }
                } catch (JSONException e) {
                    finish();
                    e.printStackTrace();
                }

            }
        }
        if (amount != 0L) {
//            steps.get(0).setCahootsPayload(doStowaway0(amount, account));
            doStowaway0(amount, account);
        }


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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }

        return true;
    }

    private CahootsStepFragment.CahootsFragmentListener listener = new CahootsStepFragment.CahootsFragmentListener() {
        @Override
        public void onScan(int d, String qrData) {
            onScanCahootsPayload(qrData);

        }

        @Override
        public void onShare(int step, String qrData) {
            shareCahootsPayload(payload.toJSON().toString());
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

        int step = stowaway.getStep();
        viewPager.post(() -> viewPager.setCurrentItem(step+1, true));

        Log.i(TAG, "onScanCahootsPayload: ".concat("- - ".concat(String.valueOf(step))));
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
            Log.d("CahootsUtil", e.getMessage());
            e.printStackTrace();
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

    private void shareCahootsPayload(final String strCahoots) {
        Log.i(TAG, "shareCahootsPayload: ".concat(strCahoots));


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
            if (!(strCahoots.length() <= QR_ALPHANUM_CHAR_LIMIT)){
                Intent txtIntent = new Intent(android.content.Intent.ACTION_SEND);
                txtIntent .setType("text/plain");
                 txtIntent .putExtra(android.content.Intent.EXTRA_TEXT, strCahoots);
                startActivity(Intent.createChooser(txtIntent ,"Share"));
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
                ;
            }

            if (file != null && fos != null) {
                Bitmap bitmap1 = ((BitmapDrawable) qrCode.getDrawable()).getBitmap();
                bitmap1.compress(Bitmap.CompressFormat.PNG, 0, fos);

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

    public String doStowaway0(long spendAmount, int account) {
        // Bob -> Alice, spendAmount in sats
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
        return stowaway0.toJSON().toString();
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

        Stowaway stowaway1 = new Stowaway(stowaway0);
        stowaway1.inc(inputsA, outputsA, null);
        stowaway1.setStep(1);

//        steps.get(1).setCahootsPayload(stowaway1.toJSON().toString());
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
    }


}
