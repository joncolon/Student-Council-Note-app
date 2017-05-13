package nyc.c4q.jonathancolon.inContaq.data;

import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.RealmResults;
import nyc.c4q.jonathancolon.inContaq.sms.SmsHelper;
import nyc.c4q.jonathancolon.inContaq.sms.model.Sms;

public class WordFrequency {

    private RealmResults<Sms> smsList;
    private static List<String> excludedWords = new ArrayList<>(Arrays.asList("the","of","and","to","a",
            "in","for","is","on", "that","by","this","with","i","you","it","not","or","be","are",
            "from","at","as","your","have","new","more","an","was", "I'm", "I", "and", "just"));

    public WordFrequency(RealmResults<Sms> smsList) {
        this.smsList = smsList;
    }


    public String mostCommonWordReceived() {
        ArrayList<Sms> smsReceived = SmsHelper.parseReceivedSms(smsList);
        ArrayList<String> wordArrayList = new ArrayList<>();

        for (int i = 0; i < smsReceived.size(); i++) {
            String message = smsReceived.get(i).getMsg();
            for (String word : message.split(" ")) {
                if (word.length() >= 3 && !excludedWords.contains(word)) {
                    wordArrayList.add(word);
                }
            }
        }
        return mostCommonElement(wordArrayList);
    }

    @Nullable
    private String mostCommonElement(List<String> list) {

        Map<String, Integer> map = new HashMap<>();

        for (int i = 0; i < list.size(); i++) {

            Integer frequency = map.get(list.get(i));
            if (frequency == null) {
                map.put(list.get(i), 1);
            } else {
                map.put(list.get(i), frequency + 1);
            }
        }

        String mostCommonKey = null;
        int maxValue = -1;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {

            if (entry.getValue() > maxValue) {
                mostCommonKey = entry.getKey();
                maxValue = entry.getValue();
            }
        }
        return mostCommonKey;
    }

    public String mostCommonWordSent() {
        ArrayList<Sms> smsSent = SmsHelper.parseSentSms(smsList);
        ArrayList<String> wordArrayList = new ArrayList<>();

        for (int i = 0; i < smsSent.size(); i++) {
            String message = smsSent.get(i).getMsg();
            for (String word : message.split(" ")) {
                if (word.length() > 3 && !excludedWords.contains(word)) {
                    wordArrayList.add(word);
                }
            }
        }
        return mostCommonElement(wordArrayList);
    }

}