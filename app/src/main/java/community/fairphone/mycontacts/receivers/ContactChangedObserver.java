package community.fairphone.mycontacts.receivers;

import android.database.ContentObserver;

import community.fairphone.mycontacts.service.CallListener;

/**
 * Created by kwamecorp on 6/15/15.
 */
public class ContactChangedObserver extends ContentObserver {

    private static final String TAG = ContactChangedObserver.class.getSimpleName();

    private final CallListener mListener;

    public ContactChangedObserver (CallListener listener) {
        super(null);
        mListener = listener;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);

        mListener.onContactChanged();
    }

    @Override
    public boolean deliverSelfNotifications() {
        return true;
    }

}
