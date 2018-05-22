package community.fairphone.mycontacts.receivers;

import community.fairphone.mycontacts.service.CommunicationMonitorService;

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
