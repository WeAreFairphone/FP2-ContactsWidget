package com.kwamecorp.peoplewidget;

import com.kwamecorp.peoplewidget.service.CommunicationMonitorService;

import android.app.Activity;
import android.os.Bundle;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class CentralActivity extends Activity
{
    private static final String TAG = CentralActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_central);

        CommunicationMonitorService.startCommunicationMonitorService(this);
    }
}
