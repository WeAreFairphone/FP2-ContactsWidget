package com.fairphone.mycontacts.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.fairphone.mycontacts.service.CallListener;

public class OutgoingCallInterceptor extends BroadcastReceiver
{
    private static final String TAG = OutgoingCallInterceptor.class.getSimpleName();
    private final CallListener mListener;

    public OutgoingCallInterceptor (CallListener listener) {
        mListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        final String originalNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);

        Log.d(TAG, "Intercepted outgoing call. Number " + originalNumber);

        mListener.onOutgoingCall(originalNumber);
    }
}