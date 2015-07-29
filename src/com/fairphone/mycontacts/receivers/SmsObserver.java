package com.fairphone.mycontacts.receivers;

import com.fairphone.mycontacts.service.CallListener;
import com.fairphone.mycontacts.service.CommunicationMonitorService;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.Date;

public class SmsObserver extends ContentObserver
{

    private static final String TAG = SmsObserver.class.getSimpleName();
    public static final String CONTENT_SMS = "content://sms/sent";

    public static final int MESSAGE_TYPE_SENT = 2;

    private Context mContext;

    CallListener mListener;

    public SmsObserver(Context context, CallListener listener)
    {
        super(null);
        mContext = context;
        mListener = listener;
    }

    @Override
    public void onChange(boolean selfChange)
    {
        super.onChange(selfChange);

        String[] reqCols = new String[] {
                "address", "protocol", "type", "_id", "body", "date"
        };

        String selection = "_id > " + getLastSentSmsId(mContext);

        Cursor cursor = mContext.getContentResolver().query(Uri.parse(CONTENT_SMS), reqCols, selection, null, "_id desc");

        if (cursor != null) {

            try {
                    while (cursor.moveToNext()) {
                        // Log.d(TAG, DatabaseUtils.dumpCursorToString(cursor));
                        String protocol = cursor.getString(cursor.getColumnIndex("protocol"));
                        int type = cursor.getInt(cursor.getColumnIndex("type"));

                        Log.d(TAG, "Protocol: " + protocol + " - Type: " + type);
                        // Only processing outgoing sms event & only when it
                        // is sent successfully (available in SENT box).
                        if (type == MESSAGE_TYPE_SENT) {
                            Log.d(TAG, "Sms Sent");
                            int idColumn = cursor.getColumnIndex("_id");
                            int bodyColumn = cursor.getColumnIndex("body");

                            int addressColumn = cursor.getColumnIndex("address");
                            int dateColumn = cursor.getColumnIndex("date");
                            long id = cursor.getLong(idColumn);
                            String to = cursor.getString(addressColumn);
                            String message = cursor.getString(bodyColumn);
                            long date = cursor.getLong(dateColumn);

                            Log.d(TAG, "To: " + to + " - Message: " + message + " - Date: " + date);
                            if (!TextUtils.isEmpty(to)) {
                                setLastSentSmsId(mContext, id);
                                mListener.onOutgoingSMS(to);
                            }
                        }
                    }
            } catch (RuntimeException e) {
                Log.wtf(TAG, "Ex" + e.toString());
            }finally {
                cursor.close();
            }
        }
    }

    private long getLastSentSmsId(Context context)
    {
        long id = 0;

        SharedPreferences prefs = context.getSharedPreferences(CommunicationMonitorService.PREFS_PEOPLE_WIDGET_CONTACTS_DATA, 0);
        if(prefs.getLong(CommunicationMonitorService.LAST_SMS_ID, 0) == 0)
        {
            String[] reqCols = new String[] { "_id", "date" };

            String selection = "date < " + (System.currentTimeMillis() - 5000);

            Cursor cursor = context.getContentResolver().query(Uri.parse(CONTENT_SMS), reqCols, selection, null, "_id desc");

            if(cursor != null && cursor.moveToNext()) {
                int idColumn = cursor.getColumnIndex("_id");
                id = cursor.getLong(idColumn);
                cursor.close();
            }

            return id;
        }
        else
        {
            return prefs.getLong(CommunicationMonitorService.LAST_SMS_ID, id);
        }
    }

    private void setLastSentSmsId(Context context, long id)
    {
        SharedPreferences prefs = context.getSharedPreferences(CommunicationMonitorService.PREFS_PEOPLE_WIDGET_CONTACTS_DATA, 0);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(CommunicationMonitorService.LAST_SMS_ID, id);
        editor.commit();
    }
}
