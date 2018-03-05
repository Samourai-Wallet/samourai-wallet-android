package com.samourai.wallet.fragments;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.samourai.wallet.R;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.widgets.MnemonicSeedEditText;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

import static android.content.Context.CLIPBOARD_SERVICE;


public class ImportWalletFragment extends Fragment {
    private onRestoreDataSets mListener;
    private String mode = "mnemonic";
    private ViewSwitcher viewSwitcher;
    private String samouraiBackup = null;
    private EditText passPhraseMnemonic, passPhraseBackup;
    private MultiAutoCompleteTextView mnemonicSeed;
    private TextView lastUpdatedTextView, backupFileTextView;
    private LinearLayout passPhraseContainer;
    private File backUpFile;
    private Button addPassphraseBtn, pasteButton, fileChooserButton;
    private static final String TAG = "ImportWalletFragment";
    private static final int REQUEST_FILE_CODE = 9895;

    public ImportWalletFragment() {
        // Required empty public constructor
    }

    public static ImportWalletFragment newInstance(String mode) {
        ImportWalletFragment fragment = new ImportWalletFragment();
        Bundle args = new Bundle();
        args.putString("mode", mode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        backUpFile = PayloadUtil.getInstance(getActivity()).getBackupFile();
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.mode = getArguments().getString("mode");
        return inflater.inflate(R.layout.fragment_import_wallet, container, false);
    }

    public void setMode() {
        if (mode.equals("mnemonic")) {
            viewSwitcher.setDisplayedChild(0);
            RestoreFromMnemonicSeed();
        } else {
            viewSwitcher.setDisplayedChild(1);
            RestoreFromBackUp();
        }
    }

    private View.OnClickListener TogglePassphraseContainer = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (passPhraseContainer.getVisibility() == View.VISIBLE) {
                passPhraseContainer.setVisibility(View.GONE);
                addPassphraseBtn.setText(R.string.add_bip39_password);
            } else {
                passPhraseContainer.setVisibility(View.VISIBLE);
                addPassphraseBtn.setText(R.string.close_bip39_password);
            }
        }
    };

    private View.OnClickListener chooseFileClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent = Intent.createChooser(intent, "Choose a file");
            startActivityForResult(intent, REQUEST_FILE_CODE);
        }
    };

    private View.OnClickListener PasteButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(CLIPBOARD_SERVICE);
            lastUpdatedTextView.setVisibility(View.GONE);
            if (clipboard.hasPrimaryClip()) {
                String backup = clipboard.getPrimaryClip().getItemAt(0).getText().toString();
                backupFileTextView.setText(backup.trim());
                samouraiBackup = backup.trim();
            }
        }
    };

    /**
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null && data.getData() != null && data.getData().getPath() != null) {
            StringBuilder sb = new StringBuilder();
            backupFileTextView.setText(data.getData().getPath());
            try {
                BufferedReader in = new BufferedReader((new InputStreamReader(getActivity().getContentResolver().openInputStream(data.getData()))));
                String str = null;
                while ((str = in.readLine()) != null) {
                    sb.append(str);
                }
                in.close();
            } catch (FileNotFoundException fnfe) {
                fnfe.printStackTrace();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            samouraiBackup = sb.toString();
            mListener.onRestoreData(passPhraseBackup.getText().toString(), samouraiBackup);
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewSwitcher = (ViewSwitcher) view.findViewById(R.id.view_switcher_wallet_restore);
        mnemonicSeed = (MultiAutoCompleteTextView) view.findViewById(R.id.mnemonic_code_edittext);
        passPhraseContainer = (LinearLayout) view.findViewById(R.id.passphrase_container);
        addPassphraseBtn = (Button) view.findViewById(R.id.add_bip39_password_btn);
        addPassphraseBtn.setOnClickListener(TogglePassphraseContainer);
        backupFileTextView = (TextView) view.findViewById(R.id.backup_file_txtview);
        passPhraseBackup = (EditText) view.findViewById(R.id.passphrase);
        passPhraseMnemonic = (EditText) view.findViewById(R.id.passphrase_mnemonic);
        lastUpdatedTextView = (TextView) view.findViewById(R.id.last_updated_txtview);
        pasteButton = (Button) view.findViewById(R.id.paste_backup_button);
        fileChooserButton = (Button) view.findViewById(R.id.choose_file_button);
        pasteButton.setOnClickListener(PasteButtonClick);
        fileChooserButton.setOnClickListener(chooseFileClick);
        setMode();
        setAutoCompleteText();
    }

    private void setAutoCompleteText() {
        String BIP39_EN = null;
        StringBuilder sb = new StringBuilder();
        String mLine = null;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(getActivity().getAssets().open("BIP39/en.txt")));
            mLine = reader.readLine();
            while (mLine != null) {
                sb.append("\n".concat(mLine));
                mLine = reader.readLine();
            }
            reader.close();
            BIP39_EN = sb.toString();
            List<String> validWordList = Arrays.asList(BIP39_EN.split("\\n"));
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_list_item_1,validWordList);
            mnemonicSeed.setTokenizer(new MnemonicSeedEditText.SpaceTokenizer(getActivity()));
            mnemonicSeed.setAdapter(adapter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void RestoreFromBackUp() {
        readFile(backUpFile);
        if (backUpFile.exists()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd yyyy");
            lastUpdatedTextView.setText(getResources()
                    .getString(R.string.last_updated)
                    .concat(" ")
                    .concat(dateFormat.format(backUpFile.lastModified())));
            lastUpdatedTextView.setVisibility(View.VISIBLE);
        } else {
            lastUpdatedTextView.setVisibility(View.INVISIBLE);
        }
        passPhraseBackup.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    mListener.onRestoreData(passPhraseBackup.getText().toString(), samouraiBackup);
                    return false;
                }
                return false;
            }
        });
        passPhraseBackup.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                mListener.onRestoreData(charSequence.toString(), samouraiBackup);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private void RestoreFromMnemonicSeed() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                mListener.onRestoreData(passPhraseMnemonic.getText().toString(), mnemonicSeed.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        };
        mnemonicSeed.addTextChangedListener(watcher);
        passPhraseMnemonic.addTextChangedListener(watcher);
    }

    private void readFile(File file) {
        backupFileTextView.setText(file.getAbsolutePath());
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
            String str = null;
            while ((str = in.readLine()) != null) {
                sb.append(str);
            }
            in.close();
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        samouraiBackup = sb.toString();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof onRestoreDataSets) {
            mListener = (onRestoreDataSets) activity;
        } else {
            throw new RuntimeException(activity.toString()
                    + " must implement onRestoreDataSets");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface onRestoreDataSets {
        void onRestoreData(String password, String backupData);
    }

}
