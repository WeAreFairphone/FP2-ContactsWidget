package com.kwamecorp.favouriteaccess;

import android.app.Service;
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

public class CommsService extends Service implements CallListener
{
    private static final String TAG = CommsService.class.getSimpleName();
    private static final String APP_RUN_INFO_SEPARATOR = ";";

    public static final String PREFS_PEOPLE_WIDGET_CONTACTS_DATA = "FAIRPHONE_PEOPLE_WIDGET_CONTACT_DB";

    private CallInterceptorReceiver mCallBroadcastReceiver;
    private ContentObserver smsObserver;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        registerCommsListeners();
        return Service.START_NOT_STICKY;
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
    }

    @Override
    public void onOutgoingCall(String number)
    {
        String msg = "Intercepted outgoing call. Number " + number;
        Log.d(TAG, msg);
        processNumberCalled(number);
    }

    @Override
    public void onOutgoingSMS(String number)
    {
        String msg = "Intercepted outgoing SMS. Number " + number;
        Log.d(TAG, msg);
        processNumberCalled(number);
    }

    public void processNumberCalled(String number)
    {
        ContactInfo contact = ContactInfo.getContactFromPhoneNumber(this, number);

        PeopleManager.getInstance().contactUsed(contact);
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

    public void persistAppRunInfo(Context context)
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

    public void saveAppSwitcherData()
    {
        persistAppRunInfo(this);
    }
}
