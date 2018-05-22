package com.fairphone.mycontacts.service;

import android.net.Uri;

public interface CallListener {

    public void onOutgoingCall(String number);
    public void onOutgoingSMS(String number);
    public void onContactChanged();
}
