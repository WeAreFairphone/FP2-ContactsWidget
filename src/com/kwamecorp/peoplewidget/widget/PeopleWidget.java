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

    // List of custom views that contains the contact photo and contact name
    private int[] mContainerImage = {
            R.id.contact_photo_1, R.id.contact_photo_2, R.id.contact_photo_3, R.id.contact_photo_4, R.id.contact_photo_5, R.id.contact_photo_6,
    };
    private int[] mContainerName = {
            R.id.contact_name_1, R.id.contact_name_2, R.id.contact_name_3, R.id.contact_name_4, R.id.contact_name_5, R.id.contact_name_6,
    };
    private int[] mContainerPhone = {
            R.id.contact_phone_1, R.id.contact_phone_2, R.id.contact_phone_3, R.id.contact_phone_4, R.id.contact_phone_5, R.id.contact_phone_6,
    };
    private int[] mContainerSms = {
            R.id.contact_sms_1, R.id.contact_sms_2, R.id.contact_sms_3, R.id.contact_sms_4, R.id.contact_sms_5, R.id.contact_sms_6,
    };

    // List of custom views that contains the contact photo and contact name
    private int[] mContainerImage2 = {
            R.id.contact_photo_7, R.id.contact_photo_8, R.id.contact_photo_9, R.id.contact_photo_10,
    };
    private int[] mContainerName2 = {
            R.id.contact_name_7, R.id.contact_name_8, R.id.contact_name_9, R.id.contact_name_10,
    };
    private int[] mContainerPhone2 = {
            R.id.contact_phone_7, R.id.contact_phone_8, R.id.contact_phone_9, R.id.contact_phone_10,
    };
    private int[] mContainerSms2 = {
            R.id.contact_sms_7, R.id.contact_sms_8, R.id.contact_sms_9, R.id.contact_sms_10,
    };

    private RemoteViews mViews;
    private Context mContext;

    @Override
    public void onEnabled(Context context)
    {
        super.onEnabled(context);

        mViews = new RemoteViews(context.getPackageName(), R.layout.favourite_access_widget);
        mContext = context;
        updateView();
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        mViews = new RemoteViews(context.getPackageName(), R.layout.favourite_access_widget);
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

    private void updateImage(final int view, final String photoUrl)
    {
        if (photoUrl != null)
        {
            Bitmap bitmap = loadContactPhoto(photoUrl, mContext);
            mViews.setImageViewBitmap(view, bitmap);
        }
        else
        {
            mViews.setImageViewResource(view, android.R.drawable.sym_def_app_icon);
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

            List<ContactInfo> mostContacted = new ArrayList<ContactInfo>(instance.getMostContacted());
            updateContactView(mostContacted, mContainerName, mContainerImage, mContainerPhone, mContainerSms);

            List<ContactInfo> lastContacted = new ArrayList<ContactInfo>(instance.getLastContacted());
            updateContactView(lastContacted, mContainerName2, mContainerImage2, mContainerPhone2, mContainerSms2);

            toggleResetButtonVisibility(mViews, lastContacted, mostContacted);

            int code = 0;
            setupButtonClickIntents(mContext, code, mViews);

            ComponentName widget = new ComponentName(mContext, PeopleWidget.class);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
            appWidgetManager.updateAppWidget(widget, null);
            appWidgetManager.updateAppWidget(widget, mViews);

        }

        public void updateContactView(List<ContactInfo> contactList, int[] containerName, int[] containerPhoto, int[] containerPhone, int[] containerSms)
        {
            for (int i = 0; i < containerPhoto.length && i < (contactList.size()); i++)
            {
                updateImage(containerPhoto[i], contactList.get(i).photoUri);
                String contactName = TextUtils.isEmpty(contactList.get(i).name) ? "Unknown" : contactList.get(i).name;

                String textViewText = contactName + "\n" + contactList.get(i).phoneNumber;
                mViews.setTextViewText(containerName[i], textViewText);

                // open contact
                addOpenContactBehaviour(containerPhone, contactList, i);

                switch (contactList.get(i).getLastAction())
                {
                    case CALL:
                        // call contact
                        addCallContactBehaviour(containerSms, contactList, i, false);
                        mViews.setTextViewCompoundDrawables(containerName[i], R.drawable.home_icon, 0, 0, 0);
                        break;

                    case SMS:
                        // sms contact
                        addSmsContactBehaviour(containerSms, contactList, i, false);
                        mViews.setTextViewCompoundDrawables(containerName[i], R.drawable.sms_icon, 0, 0, 0);
                        break;

                    default:
                        break;
                }

            }
        }

        public void addSmsContactBehaviour(int[] containerSms, final List<ContactInfo> result, int i, boolean clearClickListener)
        {
            if (!clearClickListener)
            {
                String uriSms = "smsto:" + result.get(i).phoneNumber;
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
                mViews.setOnClickPendingIntent(containerSms[i], pendingIntentSms);

            }
            else
            {
                mViews.setOnClickPendingIntent(containerSms[i], null);
            }
        }

        public void addCallContactBehaviour(int[] containerPhone, final List<ContactInfo> result, int i, boolean clearClickListener)
        {
            if (!clearClickListener)
            {
                String uriCall = "tel:" + result.get(i).phoneNumber;
                Intent intentCall = new Intent(Intent.ACTION_CALL);

                ComponentName comp = new ComponentName("com.android.phone", "com.android.phone.OutgoingCallBroadcaster");
                intentCall.setComponent(comp);
                intentCall.setData(Uri.parse(uriCall));
                PendingIntent pendingIntentCall = PendingIntent.getActivity(mContext, 0, intentCall, PendingIntent.FLAG_UPDATE_CURRENT);
                mViews.setOnClickPendingIntent(containerPhone[i], pendingIntentCall);

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
                mViews.setOnClickPendingIntent(containerPhone[i], null);
            }
        }

        public void addOpenContactBehaviour(int[] containerImage, final List<ContactInfo> result, int i)
        {
            if (!TextUtils.isEmpty(result.get(i).contactId))
            {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, "" + result.get(i).contactId));
                PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0 /*
                                                                                     * no
                                                                                     * requestCode
                                                                                     */, intent, PendingIntent.FLAG_UPDATE_CURRENT /*
                                                                                                                                    * 0
                                                                                                                                    * no
                                                                                                                                    * flags
                                                                                                                                    */);
                mViews.setOnClickPendingIntent(containerImage[i], pendingIntent);
            }
            else
            {
                mViews.setOnClickPendingIntent(containerImage[i], null);
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
