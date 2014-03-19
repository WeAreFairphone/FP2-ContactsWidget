package com.kwamecorp.favouriteaccess;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Log;

public class ContactInfo
{
    private static final String TAG = ContactInfo.class.getSimpleName();

    private static final String SEPARATOR = ",";
    public static SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    public enum LAST_ACTION
    {
        CALL, SMS
    }

    public String name;
    public String photoUri;
    public String lookup;
    public String contactId;
    public String phoneNumber;
    private long mCounter;
    private Date mLastExecution;
    private LAST_ACTION mLastAction;

    public ContactInfo(String name, String photoUri, String lookup, String contactID, String phoneNumber)
    {
        this.name = name;
        this.photoUri = photoUri;
        this.lookup = lookup;
        this.contactId = contactID;
        this.phoneNumber = phoneNumber;
        this.mCounter = 0l;
        this.mLastExecution = null;
        this.mLastAction = null;
    }

    public ContactInfo(String phoneNumber)
    {
        this.name = "";
        this.photoUri = "";
        this.lookup = "";
        this.contactId = "";
        this.phoneNumber = phoneNumber;
        this.mCounter = 0l;
        this.mLastExecution = null;
        this.mLastAction = null;
    }

    public ContactInfo(Context context, String phoneNumber, long counter, Date lastExecution, String lastAction)
    {
        ContactInfo contact = getContactFromPhoneNumber(context, phoneNumber, getLastActionFromString(lastAction));

        this.name = contact.name;
        this.photoUri = contact.photoUri;
        this.lookup = contact.lookup;
        this.contactId = contact.contactId;
        this.phoneNumber = contact.phoneNumber;
        this.mCounter = counter;
        this.mLastExecution = lastExecution;
        this.mLastAction = contact.getLastAction();
    }

    private LAST_ACTION getLastActionFromString(String lastAction)
    {
        LAST_ACTION action = null;
        try
        {
            action = LAST_ACTION.valueOf(lastAction);
        } catch (Exception e)
        {
            Log.w(TAG, "Invalid action. Setting Call");
            action = LAST_ACTION.CALL;
        }
        return action;
    }

    public static String serializeContact(ContactInfo contact)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(contact.getCount());
        sb.append(SEPARATOR);
        sb.append(DATE_FORMATTER.format(contact.getLastExecution()));
        sb.append(SEPARATOR);
        sb.append(contact.getLastAction().name());
        return sb.toString();
    }

    public LAST_ACTION getLastAction()
    {
        return mLastAction;
    }

    public void setLastAction(LAST_ACTION action)
    {
        mLastAction = action;
    }

    public static ContactInfo deserializeContact(Context context, String number, String data)
    {
        Log.d(TAG, "Contact data > " + data);
        String[] splits = data.split(SEPARATOR);
        Date lastExecution = null;
        long count = 0l;
        String lastAction = null;
        if (splits != null && splits.length == 3)
        {
            count = Long.parseLong(splits[0]);
            try
            {
                lastExecution = DATE_FORMATTER.parse(splits[1]);
            } catch (ParseException e)
            {
                e.printStackTrace();
                lastExecution = Calendar.getInstance().getTime();
            }

            lastAction = splits[2];
        }
        ContactInfo contactInfo = new ContactInfo(context, number, count, lastExecution, lastAction);

        return contactInfo;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Contact [name=");
        builder.append(name);
        builder.append(", photoUri=");
        builder.append(photoUri);
        builder.append(", lookup=");
        builder.append(lookup);
        builder.append(", contactID=");
        builder.append(contactId);
        builder.append(", phoneNumber=");
        builder.append(phoneNumber);
        builder.append("]");
        return builder.toString();
    }

    public void resetCount()
    {
        mCounter = 0l;
    }

    public void incrementCount()
    {
        mCounter++;
    }

    public long getCount()
    {
        return mCounter;
    }

    public void decrementCount()
    {
        if (mCounter > 0)
        {
            mCounter--;
        }
    }

    public Date getLastExecution()
    {
        return mLastExecution;
    }

    public void setLastExecution(Date lastExecution)
    {
        this.mLastExecution = lastExecution;
    }

    public static ContactInfo getContactFromPhoneNumber(Context context, String number, LAST_ACTION action)
    {
        Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

        String[] projection = new String[] {
                PhoneLookup.DISPLAY_NAME, PhoneLookup.PHOTO_URI, PhoneLookup.LOOKUP_KEY, PhoneLookup._ID, PhoneLookup.NUMBER,
        };

        String selection = PhoneLookup.NUMBER + " LIKE %" + number + "%";
        Cursor cursor = context.getContentResolver().query(uri, projection, selection, null, null);
        ContactInfo contact = null;
        if (cursor != null && cursor.moveToNext())
        {

            String contactId = cursor.getString(cursor.getColumnIndex(PhoneLookup._ID));
            String lookup = cursor.getString(cursor.getColumnIndex(PhoneLookup.LOOKUP_KEY));
            String photoUri = cursor.getString(cursor.getColumnIndex(PhoneLookup.PHOTO_URI));
            String name = cursor.getString(cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME));
            String phonenumber = cursor.getString(cursor.getColumnIndex(PhoneLookup.NUMBER));
            contact = new ContactInfo(name, photoUri, lookup, contactId, number);
        }

        if (contact != null)
        {
            Log.d(TAG, "Number " + number + " belongs to contact " + contact);
        }
        else
        {
            contact = new ContactInfo(number);
            Log.d(TAG, "Number " + number + " as no contact associated.");
        }
        contact.setLastExecution(Calendar.getInstance().getTime());
        contact.setLastAction(action);

        return contact;
    }
}
