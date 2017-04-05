package nyc.c4q.jonathancolon.inContaq.notifications;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.WakefulBroadcastReceiver;

import java.util.List;

import nyc.c4q.jonathancolon.inContaq.R;
import nyc.c4q.jonathancolon.inContaq.contactlist.model.Contact;
import nyc.c4q.jonathancolon.inContaq.contactlist.activities.ContactListActivity;
import nyc.c4q.jonathancolon.inContaq.utlities.sqlite.ContactDatabaseHelper;
import nyc.c4q.jonathancolon.inContaq.utlities.sqlite.SqlHelper;

import static android.app.Notification.PRIORITY_HIGH;


public class ContactNotificationService extends IntentService {

    private NotificationCompat.Builder mBuilder;
    private NotificationManager notificationManager;
    private SQLiteDatabase db;
    private Context context;

    private static final long WEEK_IN_MILLISECONDS = 604800000;
    private static final int TWELVE_HOURS_IN_MILLIS = 43200000;
    public static boolean hasStarted = false;

    public ContactNotificationService() {
        super("ContactNotificationService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        WakefulBroadcastReceiver.completeWakefulIntent(intent);

        System.out.println("Called IntentService...");

        if (!hasStarted) { // Needed or else it'll keep scheduling new alarms and you'll be swarmed with notifications
            System.out.println("Setting alarm for service...");
            scheduleAlarm();
            hasStarted = true;
        }
        checkInspectionTime();
    }

    public void startNotification(Contact contact, Context context) {

            int NOTIFICATION_ID = 555;
            Intent intent = new Intent(context, ContactListActivity.class);
            int requestID = (int) System.currentTimeMillis();
            int flags = PendingIntent.FLAG_CANCEL_CURRENT; // Cancel old intent and create new one
            PendingIntent pendingIntent = PendingIntent.getActivity(context, requestID, intent, flags);

            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            mBuilder = new NotificationCompat.Builder(context)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setSmallIcon(R.drawable.vectorpaint)
                    .setPriority(PRIORITY_HIGH)
                    .setFullScreenIntent(pendingIntent, true)
                    .setContentTitle("We're better together..")
                    .setContentText("Why don't you contact " + contact.getFirstName() + " " +
                    contact.getLastName())
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.vectorpaint))
                    .setSound(notification);

            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    private void checkInspectionTime() {
        ContactDatabaseHelper dbHelper = ContactDatabaseHelper.getInstance(getApplicationContext());
        db = dbHelper.getWritableDatabase();
        List<Contact> contacts = SqlHelper.selectAllContacts(db);
        for (int i = 0; i < contacts.size(); i++) {
            Contact contact = contacts.get(i);
            if (contact.isReminderEnabled()){
                startNotification(contact, context);
            }
        }
    }

    //// TODO: 3/11/17 add this check to the notification
    private boolean hasWeekPassed(Contact c) {
        return System.currentTimeMillis() - c.getTimeLastContacted() > WEEK_IN_MILLISECONDS;
    }

    private void scheduleAlarm() {
        AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
        Intent i = new Intent(this, ContactNotificationService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, i, 0);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), TWELVE_HOURS_IN_MILLIS,
                pendingIntent);
    }

    public void cancelAlarm() {
        Intent intent = new Intent(getApplicationContext(), MyAlarmReceiver.class);
        final PendingIntent pIntent = PendingIntent.getBroadcast(this, MyAlarmReceiver.REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pIntent);
    }
}
