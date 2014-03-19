
package com.kwamecorp.peoplewidget.receivers;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.util.Log;

public class SmsObserver extends ContentObserver {

    private static final String TAG = SmsObserver.class.getSimpleName();
    public static final String CONTENT_SMS = "content://sms/outbox";
    // public static final String CONTENT_SMS = "content://sms/sent";

    public static final int MESSAGE_TYPE_SENT = 2;

    private Context mContext;

    CallListener mListener;

    public SmsObserver(Context context, CallListener listener) {
        super(null);
        mContext = context;
        mListener = listener;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);

        String[] reqCols = new String[] {
                "_id", "protocol", "type", "address", "body", "date"
        };
        Cursor cursor = mContext.getContentResolver().query(Uri.parse(CONTENT_SMS), reqCols, null,
                null, "date desc");
        while(cursor.moveToNext()) {

            Log.d(TAG, DatabaseUtils.dumpCursorToString(cursor));
            String protocol = cursor.getString(cursor.getColumnIndex("protocol"));
            int type = cursor.getInt(cursor.getColumnIndex("type"));

            Log.d(TAG, "Protocol: " + protocol + " - Type: " + type);
            // Only processing outgoing sms event & only when it
            // is sent successfully (available in SENT box).
            if (protocol != null || type != MESSAGE_TYPE_SENT) {
                Log.d(TAG, "out");
                // return;
            }
            int bodyColumn = cursor.getColumnIndex("body");
            int addressColumn = cursor.getColumnIndex("address");
            String to = cursor.getString(addressColumn);
            String message = cursor.getString(bodyColumn);
            Log.d(TAG, "To: " + to + " - Message: " + message);
            if (to != null && !to.isEmpty()) {
                mListener.onOutgoingSMS(to);
            }
        }
    }
}
