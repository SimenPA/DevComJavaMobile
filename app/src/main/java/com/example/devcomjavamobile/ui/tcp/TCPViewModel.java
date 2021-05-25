package com.example.devcomjavamobile.ui.tcp;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class TCPViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public TCPViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is dashboard fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}