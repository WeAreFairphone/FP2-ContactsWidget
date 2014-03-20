package com.kwamecorp.peoplewidget.widget;

import com.kwamecorp.peoplewidget.R;
import com.kwamecorp.peoplewidget.data.ContactInfo;
import com.kwamecorp.peoplewidget.data.ContactInfoManager;
import com.kwamecorp.peoplewidget.data.PeopleManager;
import com.kwamecorp.peoplewidget.service.CommunicationMonitorService;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PeopleWidget extends AppWidgetProvider
{

    private static final String TAG = PeopleWidget.class.getSimpleName();

    private RemoteViews mWidget;
    private Context mContext;

    @Override
    public void onEnabled(Context context)
    {
        super.onEnabled(context);

        mWidget = new RemoteViews(context.getPackageName(), R.layout.favourite_access_widget);
        mContext = context;
        updateView();
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        mWidget = new RemoteViews(context.getPackageName(), R.layout.favourite_access_widget);
        mContext = context;
        Log.i(this.getClass().getSimpleName(), "onUpdate()");
        updateView();
    }

    private void updateView()
    {
        Log.i(TAG, "updateView()");
        updateBoard();
    }

    private void updateBoard()
    {
        new AsyncGetContacts().execute(new String[] {
            null
        });
    }

    private void updateImage(RemoteViews view, final int viewId, final String photoUrl)
    {
        if (!TextUtils.isEmpty(photoUrl))
        {
            Bitmap bitmap = loadContactPhoto(photoUrl, mContext);
            view.setImageViewBitmap(viewId, bitmap);
        }
        else
        {
            view.setImageViewResource(viewId, R.drawable.ic_contact_picture_holo_light);
        }
    }

    private Bitmap loadContactPhoto(final String photoData, Context context)
    {
        Uri thumbUri;
        AssetFileDescriptor afd = null;

        try
        {
            thumbUri = Uri.parse(photoData);
            /*
             * Retrieves an AssetFileDescriptor object for the thumbnail URI
             * using ContentResolver.openAssetFileDescriptor
             */
            afd = context.getContentResolver().openAssetFileDescriptor(thumbUri, "r");
            /*
             * Gets a file descriptor from the asset file descriptor. This
             * object can be used across processes.
             */
            FileDescriptor fileDescriptor = afd.getFileDescriptor();
            // Decode the photo file and return the result as a Bitmap
            // If the file descriptor is valid
            if (fileDescriptor != null)
            {
                // Decodes the bitmap
                Log.i(this.getClass().getSimpleName(), "Uri = " + thumbUri.toString());
                return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, null);
            }
            // If the file isn't found
        } catch (FileNotFoundException e)
        {

        } finally
        {

            if (afd != null)
            {
                try
                {
                    afd.close();
                } catch (IOException e)
                {
                }
            }
        }
        return null;
    }

    private List<ContactInfo> getLocalFrequentContacts()
    {
        return new ArrayList<ContactInfo>();
    }

    private class AsyncGetContacts extends AsyncTask<String, Void, List<ContactInfo>>
    {

        @Override
        protected List<ContactInfo> doInBackground(String... in)
        {
            return makeMeRequest();
        }

        private List<ContactInfo> makeMeRequest()
        {
            return getLocalFrequentContacts();
        }

        @Override
        protected void onPostExecute(final List<ContactInfo> result)
        {
            if (result == null)
            {
                return;
            }

            ContactInfoManager instance = PeopleManager.getInstance();

            // clear the current data
            mWidget.removeAllViews(R.id.last_contacted_row_1);
            mWidget.removeAllViews(R.id.last_contacted_row_2);

            mWidget.removeAllViews(R.id.most_contacted_row_1);
            mWidget.removeAllViews(R.id.most_contacted_row_2);

            List<ContactInfo> mostContacted = new ArrayList<ContactInfo>(instance.getMostContacted());
            updateMostContactedList(mContext, mWidget, mostContacted);

            List<ContactInfo> lastContacted = new ArrayList<ContactInfo>(instance.getLastContacted());
            updateLastContactedList(mContext, mWidget, lastContacted);

            toggleResetButtonVisibility(mWidget, lastContacted, mostContacted);

            int code = 0;
            setupButtonClickIntents(mContext, code, mWidget);

            ComponentName widget = new ComponentName(mContext, PeopleWidget.class);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
            appWidgetManager.updateAppWidget(widget, null);
            appWidgetManager.updateAppWidget(widget, mWidget);

        }

        private void updateLastContactedList(Context context, RemoteViews widget, List<ContactInfo> contactInfoList)
        {
            int viewCounter = 0;
            for (ContactInfo contactInfo : contactInfoList)
            {
                RemoteViews view = getRecentView(context, contactInfo);

                if (view != null)
                {
                    if (viewCounter < 2)
                    {
                        widget.addView(R.id.last_contacted_row_1, view);
                    }
                    else
                    {
                        widget.addView(R.id.last_contacted_row_2, view);
                    }
                    viewCounter++;
                }
            }
        }

        private void updateMostContactedList(Context context, RemoteViews widget, List<ContactInfo> contactInfoList)
        {
            int viewCounter = 0;
            for (ContactInfo contactInfo : contactInfoList)
            {
                RemoteViews view = getMostContactView(context, contactInfo);

                if (view != null)
                {
                    if (viewCounter < 3)
                    {
                        widget.addView(R.id.most_contacted_row_1, view);
                    }
                    else
                    {
                        widget.addView(R.id.most_contacted_row_2, view);
                    }
                    viewCounter++;
                }
            }
        }

        private RemoteViews getRecentView(Context context, ContactInfo info)
        {
            RemoteViews recentRow = new RemoteViews(context.getPackageName(), R.layout.last_contacted_item);
            setupView(recentRow, info);

            return recentRow;
        }

        private RemoteViews getMostContactView(Context context, ContactInfo info)
        {
            RemoteViews mostContactRow = new RemoteViews(context.getPackageName(), R.layout.most_contacted_item);
            setupView(mostContactRow, info);

            return mostContactRow;
        }

        public void setupView(RemoteViews view, ContactInfo info)
        {
            updateImage(view, R.id.contact_photo, info.photoUri);
            String contactName = TextUtils.isEmpty(info.name) ? "Unknown" : info.name;

            view.setTextViewText(R.id.contact_name, contactName);
            view.setTextViewText(R.id.contact_phone_number, info.getNumberTypeAsString(mContext));

            // open contact
            addOpenContactBehaviour(view, info);

            switch (info.getLastAction())
            {
                case CALL:
                    // call contact
                    addCallContactBehaviour(view, info, false);
                    view.setTextViewCompoundDrawables(R.id.contact_phone_number, R.drawable.home_icon, 0, 0, 0);
                    break;

                case SMS:
                    // sms contact
                    addSmsContactBehaviour(view, info, false);
                    view.setTextViewCompoundDrawables(R.id.contact_phone_number, R.drawable.sms_icon, 0, 0, 0);
                    break;

                default:
                    break;
            }
        }

        public void addSmsContactBehaviour(RemoteViews view, final ContactInfo contactInfo, boolean clearClickListener)
        {
            if (!clearClickListener)
            {
                String uriSms = "smsto:" + contactInfo.phoneNumber;
                Intent intentSms = new Intent(Intent.ACTION_SENDTO);
                intentSms.setData(Uri.parse(uriSms));

                PackageManager packageManager = mContext.getPackageManager();
                List<ResolveInfo> list = packageManager.queryIntentActivities(intentSms, 0);
                Log.i(this.getClass().getSimpleName(), "CENAS ->" + list.size());
                for (ResolveInfo resolveInfo : list)
                {

                    Log.i(this.getClass().getSimpleName(), resolveInfo.activityInfo.packageName + " " + resolveInfo.activityInfo.name);

                    if (resolveInfo.activityInfo.name.equals("com.android.mms.ui.ComposeMessageActivity"))
                    {
                        ComponentName comp = new ComponentName("com.android.mms", "com.android.mms.ui.ComposeMessageActivity");
                        intentSms.setComponent(comp);
                    }
                    else if (resolveInfo.activityInfo.name.equals("com.android.mms.ui.ConversationComposer"))
                    {
                        ComponentName comp = new ComponentName("com.android.mms", "com.android.mms.ui.ConversationComposer");
                        intentSms.setComponent(comp);
                    }
                }
                PendingIntent pendingIntentSms = PendingIntent.getActivity(mContext, 0, intentSms, PendingIntent.FLAG_UPDATE_CURRENT);
                view.setOnClickPendingIntent(R.id.last_action, pendingIntentSms);

            }
            else
            {
                view.setOnClickPendingIntent(R.id.last_action, null);
            }
        }

        public void addCallContactBehaviour(RemoteViews view, final ContactInfo contactInfo, boolean clearClickListener)
        {
            if (!clearClickListener)
            {
                String uriCall = "tel:" + contactInfo.phoneNumber;
                Intent intentCall = new Intent(Intent.ACTION_CALL);

                ComponentName comp = new ComponentName("com.android.phone", "com.android.phone.OutgoingCallBroadcaster");
                intentCall.setComponent(comp);
                intentCall.setData(Uri.parse(uriCall));
                PendingIntent pendingIntentCall = PendingIntent.getActivity(mContext, 0, intentCall, PendingIntent.FLAG_UPDATE_CURRENT);
                view.setOnClickPendingIntent(R.id.last_action, pendingIntentCall);

                // String uriCall = "tel:" + result.get(i).phoneNumbers;
                // Intent intentCall = new Intent(Intent.ACTION_CALL);
                // intentCall.setData(Uri.parse(uriCall));
                // PendingIntent pendingIntentCall =
                // PendingIntent.getActivity(mContext,
                // 0 /* no requestCode */, intentCall,
                // PendingIntent.FLAG_UPDATE_CURRENT /*0 no flags*/);
                // mViews.setOnClickPendingIntent(mContainerPhone[i],
                // pendingIntentCall);
            }
            else
            {
                view.setOnClickPendingIntent(R.id.last_action, null);
            }
        }

        public void addOpenContactBehaviour(RemoteViews view, final ContactInfo contactInfo)
        {
            if (!TextUtils.isEmpty(contactInfo.contactId))
            {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, "" + contactInfo.contactId));
                PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                view.setOnClickPendingIntent(R.id.contact_photo, pendingIntent);
            }
            else
            {
                view.setOnClickPendingIntent(R.id.contact_photo, null);
            }
        }

        private void toggleResetButtonVisibility(RemoteViews widget, List<ContactInfo> lastContacted, List<ContactInfo> mostContacted)
        {
            if (lastContacted.size() == 0 && mostContacted.size() == 0)
            {
                widget.setViewVisibility(R.id.buttonReset, View.GONE);
                widget.setViewVisibility(R.id.buttonResetDisabled, View.VISIBLE);
            }
            else
            {
                widget.setViewVisibility(R.id.buttonReset, View.VISIBLE);
                widget.setViewVisibility(R.id.buttonResetDisabled, View.GONE);
            }
        }

        private int setupButtonClickIntents(Context context, int code, RemoteViews widget)
        {
            // set up the all apps intent
            Intent launchIntent = new Intent();
            launchIntent.setAction(CommunicationMonitorService.LAUNCH_CONTACTS_APP);

            PendingIntent launchPendingIntent = PendingIntent.getBroadcast(context, code++, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            widget.setOnClickPendingIntent(R.id.buttonLauncher, launchPendingIntent);

            // set up the reset apps intent
            Intent resetIntent = new Intent();
            resetIntent.setAction(CommunicationMonitorService.PEOPLE_WIDGET_RESET);
            PendingIntent resetPendingIntent = PendingIntent.getBroadcast(context, code++, resetIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            widget.setOnClickPendingIntent(R.id.buttonReset, resetPendingIntent);
            return code;
        }
    }
}
