package community.fairphone.mycontacts.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Pair;

import java.util.ArrayList;

/**
 * Created by kwamecorp on 5/29/15.
 */
public class DbHelper extends SQLiteOpenHelper {

    private static DbHelper mDbInstance;

    private static final String TAG = DbHelper.class.getSimpleName();

    private static final String DATABASE_NAME = "mypeople.db";
    private static final int DATABASE_VERSION = 10;

    public static final String TABLE_NAME = "communications";
    public static final String COLUMN_NAME_ID = "_id";
    public static final String COLUMN_NAME_NUMBER = "number";
    public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
    public static final String COLUMN_NAME_TYPE = "type";

    private static final String[] COLUMNS = {COLUMN_NAME_ID,COLUMN_NAME_NUMBER, COLUMN_NAME_TIMESTAMP, COLUMN_NAME_TYPE};

    private static final String SQL_CREATE_COMMUNICATIONS_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_NAME_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_NAME_NUMBER + " TEXT," +
                    COLUMN_NAME_TIMESTAMP + " INTEGER," +
                    COLUMN_NAME_TYPE + " TEXT" +" )";

    private static final String SQL_DELETE_COMMUNICATIONS_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    public static final int LAST_CONTACTED_MAX_COUNT_LIMIT = 1;

    public static final String COMMUNICATION_TYPE_CALL = "CALL";
    public static final String COMMUNICATION_TYPE_SMS = "SMS";



    //Constructor made private to prevent direct instantiation
    private DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized DbHelper getInstance(Context context) {
        if (mDbInstance == null) {
            mDbInstance = new DbHelper(context.getApplicationContext());
        }
        return mDbInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_COMMUNICATIONS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_COMMUNICATIONS_TABLE);
        onCreate(db);
    }

    public ArrayList<Pair<CommunicationModel, Integer>> getMostContacted()
    {
        // TODO Validations
        ArrayList<Pair<CommunicationModel, Integer>> communications = new ArrayList<>();

        String query = "SELECT *, COUNT(" + COLUMN_NAME_NUMBER + ") AS NCALLS" +
                " FROM "  + TABLE_NAME +
                " GROUP BY " + COLUMN_NAME_NUMBER +
                " ORDER BY COUNT(" + COLUMN_NAME_NUMBER + ") DESC ";

        SQLiteDatabase db = this.getReadableDatabase();


        if(db != null)
        {
            Cursor c = db.rawQuery(query, null);

            if(c != null)
            {
                while(c.moveToNext())
                {
                    communications.add(Pair.create(mapToModel(c), c.getInt(c.getColumnIndex("NCALLS"))));
                }
            }

            c.close();
        }

        return communications;
    }

    public CommunicationModel getMostRecent(){
        // TODO Validations
        CommunicationModel communication = null;

        String query = "SELECT * " +
                " FROM "  + TABLE_NAME +
                " ORDER BY " + COLUMN_NAME_ID + " DESC " +
                " LIMIT " + LAST_CONTACTED_MAX_COUNT_LIMIT;


        SQLiteDatabase db = this.getReadableDatabase();

        if(db != null)
        {
            // check use query instead of rawQuery
            Cursor c = db.rawQuery(query,null);

            if (c != null && c.moveToNext()) {

                communication =  mapToModel(c);
                c.close();
                return communication;
            }
        }

        return null;
    }

    public void insertCommunication(CommunicationModel communication)
    {
        //TODO Validations
        ContentValues contentValues = mapToDb(communication);
        SQLiteDatabase db = this.getWritableDatabase();
        if (db != null) {
            db.insert(TABLE_NAME, null, contentValues);
        }
    }


    private ContentValues mapToDb(CommunicationModel communicationModel)
    {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_NAME_NUMBER, communicationModel.getPhoneNumber());
        contentValues.put(COLUMN_NAME_TIMESTAMP, communicationModel.getTimeStamp());

        if(communicationModel.getCommunicationType() == CommunicationType.CALL){
            contentValues.put(COLUMN_NAME_TYPE, COMMUNICATION_TYPE_CALL);
        }
        else{
            contentValues.put(COLUMN_NAME_TYPE, COMMUNICATION_TYPE_SMS);
        }

        return contentValues;
    }

    private CommunicationModel mapToModel(Cursor c)
    {
        CommunicationModel communication = new CommunicationModel();

        communication.setPhoneNumber(c.getString(c.getColumnIndex(COLUMN_NAME_NUMBER)));
        communication.setTimeStamp(c.getLong(c.getColumnIndex(COLUMN_NAME_TIMESTAMP)));
        communication.setCommunicationTypeFromString(c.getString(c.getColumnIndex(COLUMN_NAME_TYPE)));

        return communication;
    }

}
