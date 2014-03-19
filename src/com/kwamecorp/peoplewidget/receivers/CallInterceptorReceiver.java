package com.kwamecorp.peoplewidget.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CallInterceptorReceiver extends BroadcastReceiver
{
    private static final String TAG = CallInterceptorReceiver.class.getSimpleName();
    private CallListener mListener;

    public CallInterceptorReceiver(CallListener listener)
    {
        mListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent.getAction().equals(OutgoingCallInterceptor.ACTION_CALL_MADE))
        {
            final String originalNumber = intent.getStringExtra(OutgoingCallInterceptor.CALLED_NUMBER);
            String msg = "Intercepted outgoing call. Number " + originalNumber;
            Log.d(TAG, msg);
            mListener.onOutgoingCall(originalNumber);
        }
    }
}