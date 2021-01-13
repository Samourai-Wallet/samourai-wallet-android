package com.samourai.wallet.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.transition.Transition
import androidx.transition.TransitionInflater
import com.google.android.material.transition.MaterialSharedAxis
import com.samourai.wallet.R
import com.samourai.wallet.SamouraiActivity


class MainSettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener {

    var targetTransition: Transition? = null;

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_root, rootKey)
        findPreference<Preference>("txs")?.onPreferenceClickListener = this
        findPreference<Preference>("wallet")?.onPreferenceClickListener = this
        findPreference<Preference>("troubleshoot")?.onPreferenceClickListener = this
        findPreference<Preference>("other")?.onPreferenceClickListener = this
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        this.targetTransition?.addTarget(view);
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onPreferenceClick(preference: Preference?): Boolean {

        val fragment = SettingsDetailsFragment(preference?.key)

        (activity as SettingsActivity).setFragment(fragment, true);
        return true
    }

}