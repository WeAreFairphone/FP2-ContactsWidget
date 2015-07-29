package com.fairphone.mycontacts.widget;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.fairphone.mycontacts.ContactDetailView;
import com.fairphone.mycontacts.R;
import com.fairphone.mycontacts.data.ContactDetails;
import com.fairphone.mycontacts.data.ContactDetailsManager;
import com.fairphone.mycontacts.service.CommunicationMonitorService;
import com.fairphone.mycontacts.utils.CircleTransform;
import com.fairphone.mycontacts.utils.LetterTileDrawable;

public class PeopleWidget extends AppWidgetProvider
{

    private static final String TAG = PeopleWidget.class.getSimpleName();

    public static final String EXTRA_CONTACT_NAME = "EXTRA_CONTACT_NAME";
    public static final String EXTRA_CONTACT_LOOKUP = "EXTRA_CONTACT_LOOKUP";
    public static final String EXTRA_CONTACT_ID = "EXTRA_CONTACT_ID";
    public static final String EXTRA_CONTACT_PHONENUMBERTYPE = "EXTRA_CONTACT_PHONENUMBERTYPE";
    public static final String EXTRA_CONTACT_PHONENUMBER = "EXTRA_CONTACT_PHONENUMBER";
    public static final String EXTRA_CONTACT_PHOTOURI = "EXTRA_CONTACT_PHOTOURI";

    private static final SecureRandom r = new SecureRandom();
    public static final double MOST_USED_COUNT = 4;


    @Override
    public void onEnabled(Context context)
    {
        super.onEnabled(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        Log.i(TAG, "onUpdate");
        long start = System.currentTimeMillis();
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        RemoteViews mainView = new RemoteViews(context.getPackageName(), R.layout.people_widget_main);

        context.startService(new Intent(context, CommunicationMonitorService.class));

        updateBoard(context, mainView);

        for (int widget : appWidgetIds){
            appWidgetManager.updateAppWidget(widget, mainView);
        }

        long end = System.currentTimeMillis();
        Log.d(TAG, "took "+(end-start)+" ms to update UI");
    }

    private void updateImage(Context context, RemoteViews view, final int viewId, final ContactDetails details, float imgSize)
    {
        Bitmap bitmap = loadContactPhoto(details.photoUri, context);
        if (bitmap != null)
        {
            Bitmap circle = CircleTransform.transform(bitmap, imgSize);
            view.setImageViewBitmap(viewId, circle);
        }
        else
        {
            view.setImageViewBitmap(viewId, LetterTileDrawable.getLetterTileBitmap(context, TextUtils.isEmpty(details.name) ? details.phoneNumber : details.name, details.lookup, (int)imgSize));
        }
    }

    public static Bitmap loadContactPhoto(final String photoData, Context context)
    {
        Uri thumbUri;
        AssetFileDescriptor afd = null;
        if (!TextUtils.isEmpty(photoData))
        {
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
                    Log.i(TAG, "Uri = " + thumbUri.toString());
                    return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, null);
                }
                // If the file isn't found
            } catch (FileNotFoundException e)
            {
                Log.e(TAG, e.getMessage());
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
        }
        return null;
    }

    private void updateBoard(Context context, RemoteViews mainView)
    {
        ContactDetails lastContacted = ContactDetailsManager.getLastContacted(context);
        List<ContactDetails> mostContacted = ContactDetailsManager.getMostContacted(context);

        toggleMostAndLastContactedViewsVisibility(mainView, mostContacted, lastContacted);

        if(lastContacted != null){
            updateLastContacted(context, mainView,lastContacted);
        }
        updateMostContactedList(context, mainView, mostContacted);

        setupAllContactsClickIntents(context, mainView);
    }

    private void updateLastContacted(Context context, RemoteViews widget, ContactDetails contactDetail)
    {
        setupLastContactedView(context, widget, contactDetail);
    }

    private void updateMostContactedList(Context context, RemoteViews widget, List<ContactDetails> contactDetailsList)
    {
        // Clear existing views before update
        widget.removeAllViews(R.id.most_contacted_row_1);
        widget.removeAllViews(R.id.most_contacted_row_2);

        for (int i = 0; i < contactDetailsList.size() && i < MOST_USED_COUNT; i++)
        {
            RemoteViews view = createMostContactView(context, contactDetailsList.get(i));

            if (view != null)
            {
                if (i < (MOST_USED_COUNT / 2))
                {
                    widget.addView(R.id.most_contacted_row_1, view);
                }
                else
                {
                    widget.addView(R.id.most_contacted_row_2, view);
                }
            }
        }
    }

    private RemoteViews createMostContactView(Context context, ContactDetails contactDetails)
    {
        RemoteViews mostContactRow = new RemoteViews(context.getPackageName(), R.layout.most_contacted_item);
        setupMostContactedView(context, mostContactRow, contactDetails);

        return mostContactRow;
    }

    private void setupMostContactedView(Context context, RemoteViews view, ContactDetails details)
    {
        updateImage(context, view, R.id.contact_photo, details, context.getResources().getDimension(R.dimen.most_contact_picture_size));
        String contactName = TextUtils.isEmpty(details.name) ? details.phoneNumber : details.name;

        view.setTextViewText(R.id.contact_name, contactName);
        if (TextUtils.isEmpty(details.name)){
            view.setTextViewText(R.id.contact_name, details.phoneNumber);
        } else {
            view.setTextViewText(R.id.contact_name, details.name);
        }

        // open contact
        addOpenContactBehaviour(context, view,R.id.contacted_item, details);
    }

    private void setupLastContactedView(Context context, RemoteViews view, ContactDetails details)
    {
        updateImage(context, view, R.id.contact_photo, details, context.getResources().getDimension(R.dimen.last_contact_picture_size));

        String contactName = "";
        if(TextUtils.isEmpty(details.name))
        {
            contactName = details.phoneNumber;
            view.setViewVisibility(R.id.contact_number_label, View.GONE);
            view.setViewVisibility(R.id.contact_phone_number, View.GONE);
        }
        else{
            contactName = details.name;
            view.setTextViewText(R.id.contact_number_label, details.getNumberTypeAsString(context));
            view.setTextViewText(R.id.contact_phone_number, details.phoneNumber);
            view.setViewVisibility(R.id.contact_number_label, View.VISIBLE);
            view.setViewVisibility(R.id.contact_phone_number, View.VISIBLE);
        }

        view.setTextViewText(R.id.contact_name, contactName);
        setTimeStampLabel(context, view, details.timeStamp);

        // open contact
        addOpenContactBehaviour(context, view, R.id.recent_contacts_row_1, details);
    }

    private void addOpenContactBehaviour(Context context, RemoteViews view, int id,ContactDetails contactDetails)
    {
        // set up the all apps intent
        Intent launchIntent = new Intent(context, ContactDetailView.class);

        launchIntent.putExtra(EXTRA_CONTACT_NAME, contactDetails.name);
        launchIntent.putExtra(EXTRA_CONTACT_LOOKUP, contactDetails.lookup);
        launchIntent.putExtra(EXTRA_CONTACT_ID, contactDetails.contactId);
        launchIntent.putExtra(EXTRA_CONTACT_PHONENUMBERTYPE, contactDetails.numberType);
        launchIntent.putExtra(EXTRA_CONTACT_PHONENUMBER, contactDetails.phoneNumber);
        launchIntent.putExtra(EXTRA_CONTACT_PHOTOURI, contactDetails.photoUri);

        PendingIntent launchPendingIntent = PendingIntent.getActivity(context, r.nextInt(), launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(id, launchPendingIntent);
    }

    private void setupAllContactsClickIntents(Context context, RemoteViews widget)
    {
        Intent launchIntent = new Intent();

        launchIntent.setAction(Intent.ACTION_VIEW);
        launchIntent.setData(ContactsContract.Contacts.CONTENT_URI);

        PendingIntent launchPendingIntent = PendingIntent.getActivity(context, r.nextInt(), launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        widget.setOnClickPendingIntent(R.id.contacts_launcher, launchPendingIntent);
    }

    private static void toggleMostAndLastContactedViewsVisibility(RemoteViews widget, List<ContactDetails> mostContacted, ContactDetails lastContacted)
    {
        if (mostContacted.isEmpty() && lastContacted == null)
        {
            widget.setViewVisibility(R.id.mostUsedContactsOOBEDescription, View.VISIBLE);
            widget.setViewVisibility(R.id.recent_contacts_row_1, View.GONE);
        }
        else
        {
            widget.setViewVisibility(R.id.mostUsedContactsOOBEDescription, View.GONE);
            widget.setViewVisibility(R.id.recent_contacts_row_1, View.VISIBLE);
        }
    }

    private void setTimeStampLabel(Context context, RemoteViews widget, long timestamp)
    {
        Resources res = context.getResources();

        String timeStampText;

        long communicationTimeDiff = System.currentTimeMillis() - timestamp;

        long hours = TimeUnit.MILLISECONDS.toHours(communicationTimeDiff);
        long days = TimeUnit.MILLISECONDS.toDays(communicationTimeDiff);

        if(days > 0)
        {
            if (days == 1) {
                timeStampText = MessageFormat.format(res.getString(R.string.recent_contact_timestamp_label_day), days);
            } else {
                timeStampText = MessageFormat.format(res.getString(R.string.recent_contact_timestamp_label_days), days);
            }
        }
        else if (hours > 0) {
            if (hours == 1) {
                timeStampText = MessageFormat.format(res.getString(R.string.recent_contact_timestamp_label_hour), hours);
            } else {
                timeStampText = MessageFormat.format(res.getString(R.string.recent_contact_timestamp_label_hours), hours);
            }
        }
        else {
            timeStampText = res.getString(R.string.recent_contact_timestamp_label_now);
        }

        widget.setTextViewText(R.id.last_contacted_time, timeStampText);
    }
}
