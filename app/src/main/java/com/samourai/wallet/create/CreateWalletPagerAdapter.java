package com.samourai.wallet.create;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.samourai.wallet.util.LogUtil;

import java.util.ArrayList;


public class CreateWalletPagerAdapter extends FragmentStatePagerAdapter {
    private static final String TAG = LogUtil.getTag();

    private ArrayList<Fragment> fragmentList;

    public CreateWalletPagerAdapter(FragmentManager fm) {
        super(fm);
        fragmentList = new ArrayList<>();
        fragmentList.add(PassphraseFragment.newInstance());
        fragmentList.add(SetPinFragment.newInstance(false));
        fragmentList.add(SetPinFragment.newInstance(true));
    }

    @Override
    public Fragment getItem(int position) {
        return fragmentList.get(position);
    }

    @Override
    public int getCount() {
        return fragmentList.size();
    }
}
