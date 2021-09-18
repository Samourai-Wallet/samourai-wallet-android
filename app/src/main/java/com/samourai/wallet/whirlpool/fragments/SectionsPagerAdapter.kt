package com.samourai.wallet.whirlpool.fragments

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.samourai.wallet.R


/**
 */
class SectionsPagerAdapter(private val context: Context, fm: FragmentManager) :
    FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> TransactionsFragment.newInstance()
            1 -> MixListFragment.newInstance(MixListFragment.MixListType.MIXING)
            2 -> MixListFragment.newInstance(MixListFragment.MixListType.REMIX)
            else -> TransactionsFragment.newInstance()
        }
    }

    override fun getPageTitle(position: Int): CharSequence {
        when (position) {
            0 -> {
                return context.resources.getString(R.string.transactions);
            }
            1 -> {
                return context.resources.getString(R.string.mixing_title);
            }
            2 -> {
                return context.resources.getString(R.string.remixing);
            }
        }
        return "";
    }

    override fun getCount(): Int {
        // Show 2 total pages.
        return 3
    }
}