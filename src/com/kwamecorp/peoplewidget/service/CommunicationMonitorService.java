package com.kwamecorp.peoplewidget.service;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.kwamecorp.peoplewidget.data.ContactInfo;
import com.kwamecorp.peoplewidget.data.ContactInfo.LAST_ACTION;
import com.kwamecorp.peoplewidget.data.PeopleManager;
import com.kwamecorp.peoplewidget.receivers.CallInterceptorReceiver;
import com.kwamecorp.peoplewidget.receivers.CallListener;
import com.kwamecorp.peoplewidget.receivers.OutgoingCallInterceptor;
import com.kwamecorp.peoplewidget.receivers.SmsObserver;
import com.kwamecorp.peoplewidget.widget.PeopleWidget;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommunicationMonitorService extends Service implements CallListener
{
    private static final String TAG = CommunicationMonitorService.class.getSimpleName();

    public static final String PREFS_PEOPLE_WIDGET_CONTACTS_DATA = "FAIRPHONE_PEOPLE_WIDGET_CONTACT_DB";

    public static final String LAUNCH_CONTACTS_APP = "LAUNCH_CONTACTS_APP";
    public static final String PEOPLE_WIDGET_RESET = "PEOPLE_WIDGET_RESET";

    public static final String LAST_SMS_TIMESTAMP = "LAST_SENT_SMS_TIMESTAMP";

    private CallInterceptorReceiver mCallBroadcastReceiver;
    private ContentObserver smsObserver;

    private BroadcastReceiver mBCastPeopleWidgetReset;

    private BroadcastReceiver mBCastAllContactsLauncher;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        registerCommsListeners();
        setupBroadcastReceivers();
        loadContactsInfo(this);
        updatePeopleWidgets();
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onDestroy()
    {
        // TODO Auto-generated method stub
        super.onDestroy();
        unregisterCommsListeners();
        clearPeopleWidgetBroadcastReceivers();
    }

    @Override
    public void onOutgoingCall(String number)
    {
        String msg = "Intercepted outgoing call. Number " + number;
        Log.d(TAG, msg);
        processNumberCalled(number, LAST_ACTION.CALL);
        updatePeopleWidgets();
    }

    @Override
    public void onOutgoingSMS(String number)
    {
        String msg = "Intercepted outgoing SMS. Number " + number;
        Log.d(TAG, msg);
        processNumberCalled(number, LAST_ACTION.SMS);
        updatePeopleWidgets();
    }

    public void processNumberCalled(String number, LAST_ACTION action)
    {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        String nationalNumber = number;
        try
        {
            PhoneNumber parsedNumber = phoneUtil.parse(number, "PT");
            Log.d(TAG, "Nacional number " + phoneUtil.format(parsedNumber, PhoneNumberFormat.NATIONAL));
            Log.d(TAG, "E164 number " + phoneUtil.format(parsedNumber, PhoneNumberFormat.E164));
            nationalNumber = phoneUtil.format(parsedNumber, PhoneNumberFormat.NATIONAL);
        } catch (NumberParseException e)
        {
            Log.e(TAG, "NumberParseException was thrown: " + e.toString());
        }
        ContactInfo contact = ContactInfo.getContactFromPhoneNumber(this, nationalNumber, action);
        PeopleManager.getInstance().contactUsed(contact);

        savePeopleWidgetData();
    }

    public void registerCommsListeners()
    {
        registerCallListener();
        registerSmsListener();
    }

    public void unregisterCommsListeners()
    {
        unregisterReceiver(mCallBroadcastReceiver);
        getContentResolver().unregisterContentObserver(smsObserver);
    }

    public void registerSmsListener()
    {
        smsObserver = new SmsObserver(this, this);
        getContentResolver().registerContentObserver(Uri.parse("content://sms/"), true, smsObserver);
        Log.d(TAG, "register sms listener");
    }

    public void registerCallListener()
    {
        mCallBroadcastReceiver = new CallInterceptorReceiver(this);

        IntentFilter iFilter = new IntentFilter(OutgoingCallInterceptor.ACTION_CALL_MADE);
        registerReceiver(mCallBroadcastReceiver, iFilter);
        Log.d(TAG, "register call listener");
    }

    public void loadContactsInfo(Context context)
    {
        // Most Used
        Log.d(TAG, "loadContactsInfo: loading ");
        PeopleManager.getInstance().resetState();

        SharedPreferences prefs = context.getSharedPreferences(PREFS_PEOPLE_WIDGET_CONTACTS_DATA, 0);

        List<ContactInfo> allContacts = new ArrayList<ContactInfo>();

        Map<String, ?> phoneNumbers = prefs.getAll();
        for (String number : phoneNumbers.keySet())
        {
            String data = prefs.getString(number, "");

            if (data.length() == 0)
            {
                continue;
            }

            allContacts.add(ContactInfo.deserializeContact(this, number, data));
        }

        // set the all contacts
        PeopleManager.getInstance().setAllContactInfo(allContacts);
    }

    public void persistContactInfo(Context context)
    {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_PEOPLE_WIDGET_CONTACTS_DATA, 0);

        // get the current prefs and clear to update
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.clear();

        for (ContactInfo contactInfo : PeopleManager.getInstance().getAllAppRunInfo())
        {
            editor.putString(contactInfo.phoneNumber, ContactInfo.serializeContact(contactInfo));
        }

        editor.commit();
    }

    public void savePeopleWidgetData()
    {
        persistContactInfo(this);
    }

    public void updatePeopleWidgets()
    {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, PeopleWidget.class));
        if (appWidgetIds.length > 0)
        {
            new PeopleWidget().onUpdate(this, appWidgetManager, appWidgetIds);
        }
    }

    private void setupBroadcastReceivers()
    {
        // launching the application
        mBCastPeopleWidgetReset = new BroadcastReceiver()
        {

            @Override
            public void onReceive(Context context, Intent intent)
            {
                Log.i(TAG, "Received a call from widget....Reset");
                PeopleManager.getInstance().resetState();
                savePeopleWidgetData();
                updatePeopleWidgets();
            }
        };

        mBCastAllContactsLauncher = new BroadcastReceiver()
        {

            @Override
            public void onReceive(Context context, Intent intent)
            {
                String packageName = "com.android.contacts";
                String className = "com.android.contacts.activities.PeopleActivity";

                Log.i(TAG, "Received a call from widget....Launch App " + packageName + " - " + className);

                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);

                if (launchIntent != null)
                {
                    launchIntent.setComponent(new ComponentName(packageName, className));
                    startActivity(launchIntent);
                }
            }
        };

        registerReceiver(mBCastPeopleWidgetReset, new IntentFilter(CommunicationMonitorService.PEOPLE_WIDGET_RESET));

        registerReceiver(mBCastAllContactsLauncher, new IntentFilter(CommunicationMonitorService.LAUNCH_CONTACTS_APP));
    }

    private void clearPeopleWidgetBroadcastReceivers()
    {
        unregisterReceiver(mBCastAllContactsLauncher);
        unregisterReceiver(mBCastPeopleWidgetReset);
    }
}
