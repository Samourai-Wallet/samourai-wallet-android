package com.samourai.wallet

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.transition.MaterialSharedAxis
import com.samourai.wallet.access.AccessFactory
import com.samourai.wallet.util.AppUtil
import com.samourai.wallet.util.TimeOutUtil
import kotlinx.android.synthetic.main.activity_recovery_words.*
import kotlinx.android.synthetic.main.fragment_paper_wallet_instructions.*
import kotlinx.android.synthetic.main.fragment_recovery_passphrase.*
import kotlinx.android.synthetic.main.fragment_recovery_words.*

class RecoveryWordsActivity : AppCompatActivity() {


    private var step = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recovery_words)
        window.statusBarColor = ContextCompat.getColor(this, R.color.window)
        navigate()
        nextButton.setOnClickListener {
            step += 1
            navigate()
        }

        if(BuildConfig.FLAVOR != "staging"){
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }

    }

    private fun navigate() {
        val wordList = arrayListOf<String>();
        var passphrase = ""
        intent.extras?.getString(WORD_LIST)?.let { it ->
            val words: Array<String> = it.trim { it <= ' ' }.split(" ").toTypedArray()
            wordList.addAll(words)
        }
        intent.extras?.getString(PASSPHRASE)?.let { it ->
            passphrase = it
        }
        if (step >= 3) {
            AccessFactory.getInstance(applicationContext).setIsLoggedIn(true)
            TimeOutUtil.getInstance().updatePin()
            AppUtil.getInstance(applicationContext).restartApp()
            return
        }
        val fragment = when (step) {
            0 -> RecoveryTemplateDownload()
            1 -> RecoveryWords.newInstance(ArrayList(wordList))
            2 -> PassphraseFragment.newInstance(passphrase)
            else -> RecoveryTemplateDownload()
        }
        supportFragmentManager
                .beginTransaction()
                .replace(recoveryWordsFrame.id, fragment)
                .commit()
    }

    override fun onBackPressed() {
        if (step == 0) {
            super.onBackPressed()
        } else {
            step -= 1
            this.navigate()
        }

    }

    class RecoveryTemplateDownload : Fragment() {

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.fragment_paper_wallet_instructions, container, false)
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
            returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            downloadRecoveryTemplate.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://samouraiwallet.com/recovery/worksheet")
                })
            }
        }
    }

    class PassphraseFragment : Fragment() {

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.fragment_recovery_passphrase, container, false)
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            enterTransition = MaterialSharedAxis(MaterialSharedAxis.X,  true)
            returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            arguments?.let { bundle->
                bundle.getString(PASSPHRASE)?.let {
                    passphraseView.text = it
                }
            }
        }

        companion object {
            fun newInstance(passPhrase: String): Fragment {
                return PassphraseFragment().apply {
                    arguments = Bundle().apply {
                        putString(PASSPHRASE, passPhrase)
                    }
                }
            }
        }

    }

    class RecoveryWords : Fragment() {

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.fragment_recovery_words, container, false)
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            enterTransition = MaterialSharedAxis(MaterialSharedAxis.X,true)
            returnTransition = MaterialSharedAxis(MaterialSharedAxis.X,false)

        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            arguments?.let { bundle ->
                bundle.getStringArrayList(WORD_LIST)
                        ?.let {
                            if (it.size == 12) {
                                it.map { word -> "(${it.indexOf(word) + 1}) $word" }
                                        .forEachIndexed { index, word ->
                                            run {
                                                when (index) {
                                                    0 -> word1.text = word
                                                    1 -> word2.text = word
                                                    2 -> word3.text = word
                                                    3 -> word4.text = word
                                                    4 -> word5.text = word
                                                    5 -> word6.text = word
                                                    6 -> word7.text = word
                                                    7 -> word8.text = word
                                                    8 -> word9.text = word
                                                    9 -> word10.text = word
                                                    10 -> word11.text = word
                                                    11 -> word12.text = word
                                                }
                                            }
                                        }
                            }
                        }
            }
        }

        companion object {
            fun newInstance(list: ArrayList<String>): Fragment {
                return RecoveryWords().apply {
                    arguments = Bundle().apply {
                        putStringArrayList(WORD_LIST, list)
                    }
                }
            }
        }
    }


    companion object {
        const val WORD_LIST = "BIP39_WORD_LIST"
        const val PASSPHRASE = "PASSPHRASE"
    }
}