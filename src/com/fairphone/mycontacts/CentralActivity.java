package com.fairphone.mycontacts;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.fairphone.mycontacts.widget.PeopleWidget;


public class CentralActivity extends Activity
{
    private static final String TAG = CentralActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        finish();
    }
}
