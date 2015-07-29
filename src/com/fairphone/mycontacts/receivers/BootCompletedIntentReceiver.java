package com.fairphone.mycontacts.receivers;

import com.fairphone.mycontacts.service.CommunicationMonitorService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootCompletedIntentReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        context.startService(new Intent(context, CommunicationMonitorService.class));
    }
}
