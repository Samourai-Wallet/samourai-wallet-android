package com.samourai.wallet.paynym.fragments;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;

public class PaynymListFragmentViewModel extends ViewModel {

    MutableLiveData<ArrayList<String>> pcodes = new MutableLiveData<>();

    public PaynymListFragmentViewModel() {
        this.pcodes.setValue(new ArrayList<>());
    }

    void addPcodes(ArrayList<String> list) {
        this.pcodes.setValue(list);
    }
}
