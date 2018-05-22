package community.fairphone.mycontacts;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import community.fairphone.mycontacts.data.ContactDetails;
import community.fairphone.mycontacts.utils.LetterTileDrawable;
import community.fairphone.mycontacts.widget.PeopleWidget;

public class ContactDetailView extends Activity {
	private static final String TAG = ContactDetailView.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();

		final ContactDetails details = new ContactDetails(
				intent.getStringExtra(PeopleWidget.EXTRA_CONTACT_NAME),
				intent.getStringExtra(PeopleWidget.EXTRA_CONTACT_PHOTOURI),
				intent.getStringExtra(PeopleWidget.EXTRA_CONTACT_LOOKUP),
				intent.getStringExtra(PeopleWidget.EXTRA_CONTACT_ID),
				intent.getStringExtra(PeopleWidget.EXTRA_CONTACT_PHONENUMBER),
				intent.getIntExtra(PeopleWidget.EXTRA_CONTACT_PHONENUMBERTYPE, 0)
		);
		
		setContentView(R.layout.contact_detail);

		TextView numberTypeView = (TextView) findViewById(R.id.contact_number_type);
		TextView numberView = (TextView) findViewById(R.id.contact_name);
		View createViewBtn = findViewById(R.id.contact_launcher);
		TextView createViewText = (TextView) findViewById(R.id.btn_create_view_contact);
		if(TextUtils.isEmpty(details.name)){
			numberView.setText(details.phoneNumber);
			numberTypeView.setVisibility(View.GONE);
			createViewText.setText(R.string.create_contact);
			createViewBtn.setOnClickListener(new View.OnClickListener() {
				private ContactDetails info;
				public View.OnClickListener setup(ContactDetails details){
					info = details;
					return this;
				}
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION, ContactsContract.Contacts.CONTENT_URI);
					intent.putExtra(ContactsContract.Intents.Insert.PHONE, details.phoneNumber);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
				}
			}.setup(details));
		} else {
			numberView.setText(details.name);
			String numberType = details.getNumberTypeAsString(getApplicationContext());
			if (!TextUtils.isEmpty(numberType)){
				numberTypeView.setText(numberType);
			} else {
				numberTypeView.setVisibility(View.GONE);
			}
			createViewText.setText(R.string.view_contact);
			createViewBtn.setOnClickListener(new View.OnClickListener() {
				private ContactDetails info;
				public View.OnClickListener setup(ContactDetails details){
					info = details;
					return this;
				}
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, info.lookup);
					intent.setData(uri);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
				}
			}.setup(details));
		}
		((TextView) findViewById(R.id.contact_phone_number)).setText(details.phoneNumber);
		ImageView photoView = (ImageView) findViewById(R.id.contact_photo);
		if (!TextUtils.isEmpty(details.photoUri)) {
			Bitmap bm = PeopleWidget.loadContactPhoto(details.photoUri, getApplicationContext());
			photoView.setImageBitmap(bm);
		} else {
			photoView.setBackgroundColor(LetterTileDrawable.getIntance(getApplicationContext()).pickColor(TextUtils.isEmpty(details.lookup) ? details.phoneNumber : details.lookup));
		}

		addSmsContactBehaviour(details);
		addCallContactBehaviour(details);
	}

	private void addSmsContactBehaviour(ContactDetails contactDetail)
	{
		View smsActionButton = findViewById(R.id.sms_action);
		smsActionButton.setOnClickListener(new View.OnClickListener() {
			private ContactDetails info;

			public View.OnClickListener setup(ContactDetails details) {
				info = details;
				return this;
			}

			@Override
			public void onClick(View v) {

				String uriSms = "smsto:" + info.phoneNumber;
				String encodedHash = Uri.encode("#");
				String encodedUriSms = uriSms.replaceAll("#", encodedHash);
				Intent intentSms = new Intent(Intent.ACTION_SENDTO);
				intentSms.setData(Uri.parse(encodedUriSms));
				intentSms.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intentSms);
			}
		}.setup(contactDetail));
	}

	private void addCallContactBehaviour(ContactDetails contactDetail)
	{
		View callActionButton = findViewById(R.id.call_action);
		callActionButton.setOnClickListener(new View.OnClickListener() {
			private ContactDetails info;
			public View.OnClickListener setup(ContactDetails details){
				info = details;
				return this;
			}
			@Override
			public void onClick(View v) {

				String uriCall ="tel:" + info.phoneNumber;
				String encodedHash = Uri.encode("#");
				String encodedUriCall = uriCall.replaceAll("#", encodedHash);
				Intent intentCall = new Intent(Intent.ACTION_CALL);
				intentCall.setData(Uri.parse(encodedUriCall));
				intentCall.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intentCall);

			}
		}.setup(contactDetail));

	}

	@Override
	protected void onPause() {
		super.onPause();
		finish();
	}
}
