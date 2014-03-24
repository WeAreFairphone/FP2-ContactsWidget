package com.kwamecorp.peoplewidget.receivers;

import com.kwamecorp.peoplewidget.service.CommunicationMonitorService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootCompletedIntentReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))
        {
            Intent pushIntent = new Intent(context, CommunicationMonitorService.class);
            context.startService(pushIntent);
        }
    }
}
