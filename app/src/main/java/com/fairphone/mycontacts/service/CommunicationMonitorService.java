package com.fairphone.mycontacts.service;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.util.Log;

import com.fairphone.mycontacts.data.CommunicationModel;
import com.fairphone.mycontacts.data.CommunicationType;
import com.fairphone.mycontacts.data.ContactDetails;

import com.fairphone.mycontacts.receivers.ContactChangedObserver;
import com.fairphone.mycontacts.data.ContactDetailsManager;
import com.fairphone.mycontacts.receivers.OutgoingCallInterceptor;
import com.fairphone.mycontacts.receivers.SmsObserver;
import com.fairphone.mycontacts.widget.PeopleWidget;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

public class CommunicationMonitorService extends Service implements CallListener
{
    private static final String TAG = CommunicationMonitorService.class.getSimpleName();

    public static final String PREFS_PEOPLE_WIDGET_CONTACTS_DATA = "FAIRPHONE_PEOPLE_WIDGET_CONTACT_DB";
    public static final String LAST_SMS_ID = "LAST_SENT_SMS_ID";

    private OutgoingCallInterceptor mCallBroadcastReceiver;
    private ContentObserver mSmsObserver;
    private ContactChangedObserver mContactChangedObserver;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        registerCommsListeners();

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        unregisterCommsListeners();
    }

    @Override
    public void onOutgoingCall(String number)
    {
        Log.d(TAG, "Intercepted outgoing call. Number " + number);
        processNumberCalled(number, CommunicationType.CALL);
        updatePeopleWidgets();
    }

    @Override
    public void onOutgoingSMS(String number)
    {
        Log.d(TAG, "Intercepted outgoing SMS. Number " + number);
        processNumberCalled(number, CommunicationType.SMS);
        updatePeopleWidgets();
    }

    @Override
    public void onContactChanged()
    {
        updatePeopleWidgets();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    public void processNumberCalled(String number, CommunicationType action)
    {
        String validPhoneNumber = getValidPhoneNumber(number);

        if(validPhoneNumber != null && validPhoneNumber != "") {

            CommunicationModel communication = new CommunicationModel();
            communication.setPhoneNumber(validPhoneNumber);
            communication.setCommunicationType(action);
            communication.setTimeStamp(System.currentTimeMillis());

            ContactDetails contactDetails = new ContactDetails(this, communication);
            ContactDetailsManager.addUsedContact(this, contactDetails);

            updatePeopleWidgets();
        }
    }

    public void registerCommsListeners()
    {
        registerCallListener();
        registerSmsListener();
        registerContactChangedListener();
    }

    public void unregisterCommsListeners()
    {
        if (mCallBroadcastReceiver != null) {
            unregisterReceiver(mCallBroadcastReceiver);
            mCallBroadcastReceiver = null;
        }
        if (mSmsObserver != null) {
            getContentResolver().unregisterContentObserver(mSmsObserver);
            mSmsObserver = null;
        }
        if (mContactChangedObserver != null) {
            getContentResolver().unregisterContentObserver(mContactChangedObserver);
            mContactChangedObserver = null;
        }
    }

    public void registerSmsListener()
    {
        if (mSmsObserver == null) {
            mSmsObserver = new SmsObserver(this, this);
            getContentResolver().registerContentObserver(Uri.parse("content://sms/"), true, mSmsObserver);
            Log.d(TAG, "Registered sms listener");
        }
    }

    public void registerCallListener()
    {
        if (mCallBroadcastReceiver == null) {
            mCallBroadcastReceiver = new OutgoingCallInterceptor(this);

            IntentFilter iFilter = new IntentFilter(Intent.ACTION_NEW_OUTGOING_CALL);
            registerReceiver(mCallBroadcastReceiver, iFilter);
            Log.d(TAG, "Registered call listener");
        }
    }

    public void registerContactChangedListener()
    {
        if (mContactChangedObserver == null) {
            mContactChangedObserver = new ContactChangedObserver(this);

            getContentResolver().registerContentObserver(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, false, mContactChangedObserver);
        }
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

    private String getValidPhoneNumber(String numberToValidate){
        String phoneNumber = numberToValidate;

        // check if has country code
        // if it does, save with country code
        // if not, get from operator and save with country
        // else f*ck it

        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

        Log.d(TAG, phoneNumber);
        try
        {
            PhoneNumber parsedNumber = phoneUtil.parse(numberToValidate, null);
            Log.w(TAG, "Nacional number " + phoneUtil.format(parsedNumber, PhoneNumberFormat.NATIONAL));
            Log.w(TAG, "E164 number " + phoneUtil.format(parsedNumber, PhoneNumberFormat.E164));
            phoneNumber = phoneUtil.format(parsedNumber, PhoneNumberFormat.NATIONAL);
        } catch (NumberParseException e)
        {
            Log.e(TAG, "NumberParseException was thrown: " + e.toString());
        }
        phoneNumber = phoneNumber.replaceAll("\\s+", "");
        Log.d(TAG, "Number after replace " + phoneNumber);

        return phoneNumber;
    }


}
