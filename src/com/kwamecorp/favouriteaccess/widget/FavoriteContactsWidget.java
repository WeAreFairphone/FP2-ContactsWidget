package com.kwamecorp.favouriteaccess.widget;

import com.kwamecorp.favouriteaccess.R;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FavoriteContactsWidget extends AppWidgetProvider {


	// List of custom views that contains the contact photo and contact name
	private int [] mContainerImage = {
			R.id.contact_photo_1, 
			R.id.contact_photo_2,
			R.id.contact_photo_3,
			R.id.contact_photo_4,
			R.id.contact_photo_5,
			R.id.contact_photo_6,
	};
	private int [] mContainerName = {
			R.id.contact_name_1, 
			R.id.contact_name_2,
			R.id.contact_name_3,
			R.id.contact_name_4,
			R.id.contact_name_5,
			R.id.contact_name_6,
	};
	private int [] mContainerPhone = {
			R.id.contact_phone_1, 
			R.id.contact_phone_2,
			R.id.contact_phone_3,
			R.id.contact_phone_4,
			R.id.contact_phone_5,
			R.id.contact_phone_6,
	};
	private int [] mContainerSms = {
			R.id.contact_sms_1, 
			R.id.contact_sms_2,
			R.id.contact_sms_3,
			R.id.contact_sms_4,
			R.id.contact_sms_5,
			R.id.contact_sms_6,
	};

	private RemoteViews mViews;
	private Context mContext;

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);

		mViews = new RemoteViews(context.getPackageName(), R.layout.favourite_access_widget);
		mContext = context;
		updateView();
	}
	

	 @Override
	 public void onUpdate(Context context, AppWidgetManager appWidgetManager,
	 int[] appWidgetIds) {
		 super.onUpdate(context, appWidgetManager, appWidgetIds);
		 mViews = new RemoteViews(context.getPackageName(), R.layout.favourite_access_widget);
		 mContext = context;
		 Log.i(this.getClass().getSimpleName(), "onUpdate()");
		 updateView();
	 }
	 

	private void updateView() {
		Log.i(this.getClass().getSimpleName(), "updateView()");
		updateBoard();
	}

	private void updateBoard() {
		new AsyncGetContacts().execute(new String[] {null});
	}

	private void updateImage(final int view, final String photoUrl) {
		if (photoUrl != null) {
			Bitmap bitmap = loadContactPhoto(photoUrl, mContext);
			mViews.setImageViewBitmap(view, bitmap);
		}else{
			mViews.setImageViewResource(view, android.R.drawable.sym_def_app_icon);
		}
	}

	private Bitmap loadContactPhoto(final String photoData, Context context) {
		Uri thumbUri;
		AssetFileDescriptor afd = null;

		try {
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
			if (fileDescriptor != null) {
				// Decodes the bitmap
				Log.i(this.getClass().getSimpleName(), "Uri = " + thumbUri.toString());
				return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, null);
			}
			// If the file isn't found
		} catch (FileNotFoundException e) {

		} finally {

			if (afd != null) {
				try {
					afd.close();
				} catch (IOException e) {
				}
			}
		}
		return null;
	}
	
	
	private List<Contact> getLocalFrequentContacts(){

		List<Contact> ret = new ArrayList<Contact>();
		String[] projection = new String[] { "display_name", "photo_uri", "lookup", "_id", "has_phone_number" };

		
		
		Cursor pCur =
				mContext
				.getContentResolver()
				.query(ContactsContract.Contacts.CONTENT_URI, projection, null,
						null, null);

		while (pCur.moveToNext())
		{
			Contact contact;
			String name = pCur.getString(pCur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
			String photoUri = pCur.getString(pCur.getColumnIndex(ContactsContract.Contacts.PHOTO_URI));
			String lookupKey = pCur.getString(pCur.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
			String contactId = pCur.getString(pCur.getColumnIndex(ContactsContract.Contacts._ID));
			int hasNumber = pCur.getInt(pCur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
			
			ArrayList<String> numbers = new ArrayList<String>(); 
			
			
			Cursor phoneCur =
					mContext
					.getContentResolver()
					.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[] { "data1"}, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
							new String[] {
							contactId
					}, null);
			while (phoneCur.moveToNext())
			{
				numbers.add(phoneCur.getString(phoneCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
			}
			
			contact = new Contact(name, photoUri, lookupKey, contactId, numbers);
			
			phoneCur.close();
			if(hasNumber!=0){
			ret.add(contact);
			}
		}
		pCur.close();

		return ret;
	}
	
	
	private class AsyncGetContacts extends AsyncTask<String, Void, List<Contact>>
	{

		@Override
		protected List<Contact> doInBackground(String... in)
		{
			return makeMeRequest();
		}

		private List<Contact> makeMeRequest()
		{
			return getLocalFrequentContacts();
		}

		@Override
		protected void onPostExecute(final List<Contact> result)
		{
			if (result == null){
				return;
			}

			for (int i =0 ; i<mContainerImage.length && i<(result.size()) ;i++){
				updateImage(mContainerImage[i], result.get(i).photoUri);
				mViews.setTextViewText(mContainerName[i], result.get(i).name);
				//open contact
				addOpenContactBehaviour(result, i);
				//call contact
				addCallContactBehaviour(result, i);
                //sms contact
                addSmsContactBehaviour(result, i);
			}
			
			ComponentName widget = new ComponentName(mContext, FavoriteContactsWidget.class);
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
			appWidgetManager.updateAppWidget(widget, mViews);
			
		}

		public void addSmsContactBehaviour(final List<Contact> result, int i) {
			String uriSms = "smsto:" + result.get(i).phoneNumbers;
			Intent intentSms = new Intent(Intent.ACTION_SENDTO);
			intentSms.setData(Uri.parse(uriSms));

			PackageManager packageManager = mContext.getPackageManager(); 
			List<ResolveInfo> list = packageManager.queryIntentActivities(intentSms, 0);
			Log.i(this.getClass().getSimpleName(),"CENAS ->" + list.size());
		    for (ResolveInfo resolveInfo : list) {

		    	Log.i(this.getClass().getSimpleName(), resolveInfo.activityInfo.packageName + " " + resolveInfo.activityInfo.name);
		       
		    	if (resolveInfo.activityInfo.name.equals("com.android.mms.ui.ComposeMessageActivity")){
					ComponentName comp = new ComponentName("com.android.mms","com.android.mms.ui.ComposeMessageActivity");
					intentSms.setComponent(comp);
		    	}
		    	else if(resolveInfo.activityInfo.name.equals("com.android.mms.ui.ConversationComposer")) {
		    		ComponentName comp = new ComponentName("com.android.mms","com.android.mms.ui.ConversationComposer");
					intentSms.setComponent(comp);
		    	}
		    }
		    PendingIntent pendingIntentSms = PendingIntent.getActivity(mContext,
					0 /* no requestCode */, intentSms, PendingIntent.FLAG_UPDATE_CURRENT /*0 no flags*/);
			mViews.setOnClickPendingIntent(mContainerSms[i], pendingIntentSms);
		}

		public void addCallContactBehaviour(final List<Contact> result, int i) {
			
			String uriCall = "tel:" + result.get(i).phoneNumbers;
			Intent intentCall = new Intent(Intent.ACTION_CALL);
			
			
			
			ComponentName comp = new ComponentName("com.android.phone","com.android.phone.OutgoingCallBroadcaster");
			intentCall.setComponent(comp);
			intentCall.setData(Uri.parse(uriCall));
			PendingIntent pendingIntentCall = PendingIntent.getActivity(mContext,
					0 /* no requestCode */, intentCall, PendingIntent.FLAG_UPDATE_CURRENT /*0 no flags*/);
			mViews.setOnClickPendingIntent(mContainerPhone[i], pendingIntentCall);

//			String uriCall = "tel:" + result.get(i).phoneNumbers;
//			Intent intentCall = new Intent(Intent.ACTION_CALL);
//			intentCall.setData(Uri.parse(uriCall));
//			PendingIntent pendingIntentCall = PendingIntent.getActivity(mContext,
//					0 /* no requestCode */, intentCall, PendingIntent.FLAG_UPDATE_CURRENT /*0 no flags*/);
//			mViews.setOnClickPendingIntent(mContainerPhone[i], pendingIntentCall);
		}

		public void addOpenContactBehaviour(final List<Contact> result, int i) {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, ""+ result.get(i).contactID));
			PendingIntent pendingIntent = PendingIntent.getActivity(mContext,
					0 /* no requestCode */, intent, PendingIntent.FLAG_UPDATE_CURRENT /*0 no flags*/);
			mViews.setOnClickPendingIntent(mContainerImage[i], pendingIntent);
		}




	}
	
	
	private class Contact{
		String name;
		String photoUri;
		String lookup;
		String contactID;
		List<String> phoneNumbers;
		
		public Contact(String name, String photoUri, String lookup,
				String contactID, List<String> phoneNumbers) {
			super();
			this.name = name;
			this.photoUri = photoUri;
			this.lookup = lookup;
			this.contactID = contactID;
			this.phoneNumbers = phoneNumbers ; 
		}
		
		
	}
	
	
	
}
