package com.example.devcomjavamobile.ui.transport;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class TransportViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public TransportViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is TCP fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}