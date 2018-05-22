package community.fairphone.mycontacts.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;


/**
 * Created by kwamecorp on 5/29/15.
 */
public class ContactDetails {
    private static final String TAG = ContactDetails.class.getSimpleName();

    public String name;
    public String photoUri;
    public String lookup;
    public String contactId;
    public String phoneNumber;
    public int numberType;
    public long timeStamp;
    public CommunicationType communicationType;

    public ContactDetails(String name, String photoUri, String lookup, String contactId, String phoneNumber, int numberType){
        this.contactId = contactId;
        this.name = name;
        this.photoUri = photoUri;
        this.lookup = lookup;
        this.contactId = contactId;
        this.phoneNumber = phoneNumber;
        this.numberType = numberType;
    }

    public ContactDetails(Context context, CommunicationModel communication){

        if(communication != null)
        {
            this.phoneNumber = communication.getPhoneNumber();
            this.communicationType = communication.getCommunicationType();
            this.timeStamp = communication.getTimeStamp();

            ContactDetails contactDetails =  getContactDetailsFromPhoneNumber(context, communication.getPhoneNumber());

            if(contactDetails != null)
            {
                this.phoneNumber = contactDetails.phoneNumber;
                this.contactId = contactDetails.contactId;
                this.name = contactDetails.name;
                this.photoUri = contactDetails.photoUri;
                this.lookup = contactDetails.lookup;
                this.contactId = contactDetails.contactId;
                this.numberType = contactDetails.numberType;
            }
        }
    }

    public String getNumberTypeAsString(Context context)
    {
        String numberTypeName = this.phoneNumber;
        if (!TextUtils.isEmpty(this.contactId))
        {
            if (numberType == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM) {
                numberTypeName = getCustomLabel(context);
            } else {
                numberTypeName = ContactsContract.CommonDataKinds.Phone.getTypeLabel(context.getResources(), numberType, "").toString();
            }
        }
        return numberTypeName;
    }

    private String getCustomLabel(Context context){
        String label = "";
        ContentResolver cr = context.getContentResolver();
        Cursor phoneCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,new String[]{ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.LABEL},
                            ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY +" = ? AND "+ ContactsContract.CommonDataKinds.Phone.NUMBER +" = ?",new String[]{this.lookup, this.phoneNumber}, null);
        if (phoneCur.moveToNext()) {
            int phonetype = phoneCur.getInt(phoneCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
            String customLabel = phoneCur.getString(phoneCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL));
            label = (String) ContactsContract.CommonDataKinds.Phone.getTypeLabel(context.getResources(), phonetype, customLabel);
        } else {
            ContactsContract.CommonDataKinds.Phone.getTypeLabel(context.getResources(), ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM, "").toString();
        }
        phoneCur.close();
        return label;
    }


    private ContactDetails getContactDetailsFromPhoneNumber(Context context, String phoneNumber){

        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));

        String[] projection = new String[] {
                ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.PHOTO_URI, ContactsContract.PhoneLookup.TYPE, ContactsContract.PhoneLookup.LOOKUP_KEY, ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.NUMBER,
        };

        ContentResolver contentResolver = context.getContentResolver();
        String selection = ContactsContract.PhoneLookup.NUMBER + " LIKE %" + phoneNumber + "%";
        Cursor cursor = contentResolver.query(uri, projection, selection, null, null);
        ContactDetails contact = null;
        if (cursor != null && cursor.moveToNext())
        {
            Log.i(TAG, "Contact for number " + phoneNumber + " exists!");

            String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup._ID));
            String lookup = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.LOOKUP_KEY));
            String photoUri = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI));
            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
            int type = cursor.getInt(cursor.getColumnIndex(ContactsContract.PhoneLookup.TYPE));
            String number = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.NUMBER));

            contact = new ContactDetails(name, photoUri, lookup, contactId, number, type);
        }

        if (cursor != null)
        {
            cursor.close();
        }

        return contact;
    }

}
