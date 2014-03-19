package com.kwamecorp.favouriteaccess;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class OutgoingCallInterceptor extends BroadcastReceiver
{

    public static final String ACTION_CALL_MADE = "ACTION_CALL_MADE";
    private static final String TAG = OutgoingCallInterceptor.class.getSimpleName();
    public static final String CALLED_NUMBER = "CALLED_NUMBER";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        final String originalNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
        String msg = "Intercepted outgoing call. Number " + originalNumber;
        Log.d(TAG, msg);

        Intent intentCall = new Intent(ACTION_CALL_MADE);
        intentCall.putExtra(CALLED_NUMBER, originalNumber);
        context.sendBroadcast(intentCall);
    }
}