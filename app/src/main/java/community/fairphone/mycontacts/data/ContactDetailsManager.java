package community.fairphone.mycontacts.data;

import android.content.Context;
import android.text.TextUtils;
import android.util.Pair;

import community.fairphone.mycontacts.utils.Triple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by kwamecorp on 6/1/15.
 */
public class ContactDetailsManager {

    private static final String TAG = ContactDetailsManager.class.getSimpleName();

    public static synchronized void addUsedContact(Context context, ContactDetails contact){

        CommunicationModel communication = new CommunicationModel();
        communication.setPhoneNumber(contact.phoneNumber);
        communication.setCommunicationType(contact.communicationType);
        communication.setTimeStamp(contact.timeStamp);

        DbHelper.getInstance(context).insertCommunication(communication);
    }

    public static LinkedList<ContactDetails> getMostContacted(Context context){

        LinkedList<ContactDetails> mostContacted = new LinkedList<ContactDetails>();

        ArrayList<Pair<CommunicationModel, Integer>> communications = DbHelper.getInstance(context).getMostContacted();


        // Map<lookup_key, Triple<contact, aggregated_sum, current_number_sum>>
        Map<String, Triple<ContactDetails, Integer, Integer>> aggregator = new HashMap<>();

        if(communications != null){
            for (Pair<CommunicationModel, Integer> entry : communications){
                ContactDetails c = new ContactDetails(context, entry.first);
                String key = c.lookup;
                if (TextUtils.isEmpty(key)){
                    key = c.phoneNumber;
                }
                int val = entry.second;
                int local_max = entry.second;
                if (aggregator.containsKey(key)) {
                    Triple<ContactDetails, Integer, Integer> contactDetailsCache = aggregator.get(key);
                    val += contactDetailsCache.second;
                    if(contactDetailsCache.third > local_max){
                        local_max = contactDetailsCache.third;
                        c = contactDetailsCache.first;
                    }
                }
                aggregator.put(key, Triple.create(c, val, local_max));
            }
        }

        List<Triple<ContactDetails, Integer, Integer>> l = new ArrayList<>(aggregator.values());
        Collections.sort(l, new Comparator<Triple<ContactDetails, Integer, Integer>>() {
            @Override
            public int compare(Triple<ContactDetails, Integer, Integer> lhs, Triple<ContactDetails, Integer, Integer> rhs) {
                return rhs.second.compareTo(lhs.second);
            }
        });

        LinkedList<ContactDetails> mostContactedToReturn = new LinkedList<>();

        for (Triple<ContactDetails, Integer, Integer> p: l) {
            mostContactedToReturn.add(p.first);
        }

        return mostContactedToReturn;
    }

    public static ContactDetails getLastContacted(Context context){
        ContactDetails result = null;
        CommunicationModel communication = DbHelper.getInstance(context).getMostRecent();

        if(communication != null){
            result =  new ContactDetails(context, communication);
        }

        return result;
    }

}
