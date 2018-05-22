package com.fairphone.mycontacts.data;

import android.util.Log;

/**
 * Created by kwamecorp on 5/29/15.
 */
public class CommunicationModel {

    private static final String TAG = CommunicationModel.class.getSimpleName();

    private String mPhoneNumber;
    private long mTimeStamp;
    private CommunicationType mCommunicationType;


    public String getPhoneNumber() {
        return mPhoneNumber;
    }

    public void setPhoneNumber(String mPhoneNumber) {
        this.mPhoneNumber = mPhoneNumber;
    }

    public long getTimeStamp() {
        return mTimeStamp;
    }

    public void setTimeStamp(long mTimeStamp) {
        this.mTimeStamp = mTimeStamp;
    }

    public CommunicationType getCommunicationType() {
        return mCommunicationType;
    }

    public void setCommunicationType(CommunicationType communicationType){
        this.mCommunicationType = communicationType;
    }

    public void setCommunicationTypeFromString(String mCommunicationType) {
        try
        {
            this.mCommunicationType = CommunicationType.valueOf(mCommunicationType);
        } catch (Exception e)
        {
            Log.w(TAG, "Invalid communication type. Setting Call");
            this.mCommunicationType = CommunicationType.CALL;
        }
    }


}
