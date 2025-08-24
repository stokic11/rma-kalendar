package com.example.calendar;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventNotificationService extends BroadcastReceiver {
    private static final String CHANNEL_ID = "event_notifications";
    private static final String CHANNEL_NAME = "Event Notifications";
    private static final String CHANNEL_DESCRIPTION = "Notifications for upcoming events";

    private static final String EXTRA_EVENT_ID = "event_id";
    private static final String EXTRA_NOTIFICATION_TYPE = "notification_type";
    private static final String TYPE_TWO_DAYS_BEFORE = "two_days_before";
    private static final String TYPE_ONE_DAY_BEFORE = "one_day_before";
    private static final String TYPE_ONE_HOUR_BEFORE = "one_hour_before";
    private static final String TYPE_EVENT_START = "event_start";
    private static final String TYPE_EVENT_END = "event_end";

    @Override
    public void onReceive(Context context, Intent intent) {
        createNotificationChannel(context);

        String eventId = intent.getStringExtra(EXTRA_EVENT_ID);
        String notificationType = intent.getStringExtra(EXTRA_NOTIFICATION_TYPE);

        if (eventId != null && notificationType != null) {
            handleScheduledNotification(context, eventId, notificationType);
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            rescheduleAllNotifications(context);
        } else {
            checkEventsAndNotify(context);
        }
    }

    private void rescheduleAllNotifications(Context context) {
        Database database = new Database(context);
        List<CalendarEvent> allEvents = database.getAllEvents();
        for (CalendarEvent event : allEvents) {
            if (event.isNotificationsEnabled()) {
                scheduleEventNotifications(context, event);
            }
        }
    }

    private void handleScheduledNotification(Context context, String eventId, String notificationType) {
        Database database = new Database(context);
        CalendarEvent event = database.getEventById(Integer.parseInt(eventId));

        if (event != null) {
            String title = getNotificationTitle(notificationType, event);
            String message = getNotificationMessage(notificationType, event);
            sendNotification(context, title, message);
        }
    }

    private void checkEventsAndNotify(Context context) {
        checkAndSendImmediateNotifications(context);
    }

    private String getNotificationTitle(String type, CalendarEvent event) {
        switch (type) {
            case TYPE_TWO_DAYS_BEFORE:
                return "Event in 2 days";
            case TYPE_ONE_DAY_BEFORE:
                return "Event tomorrow";
            case TYPE_ONE_HOUR_BEFORE:
                return "Event in 1 hour";
            case TYPE_EVENT_START:
                return "Event starting now";
            case TYPE_EVENT_END:
                return "Event ending";
            default:
                return "Event reminder";
        }
    }

    private String getNotificationMessage(String type, CalendarEvent event) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String startTime = timeFormat.format(new Date(event.getStartTimeMillis()));

        switch (type) {
            case TYPE_TWO_DAYS_BEFORE:
                return event.getTitle() + " starts in 2 days at " + startTime;
            case TYPE_ONE_DAY_BEFORE:
                return event.getTitle() + " starts tomorrow at " + startTime;
            case TYPE_ONE_HOUR_BEFORE:
                return event.getTitle() + " starts in 1 hour at " + startTime;
            case TYPE_EVENT_START:
                return event.getTitle() + " is starting now";
            case TYPE_EVENT_END:
                return event.getTitle() + " is ending";
            default:
                return event.getTitle();
        }
    }

    public static void scheduleEventNotifications(Context context, CalendarEvent event) {
        if (!event.isNotificationsEnabled()) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        long eventStartTime = event.getStartTimeMillis();
        long eventEndTime = event.getEndTimeMillis();

        scheduleNotification(context, alarmManager, Integer.parseInt(event.getId()), TYPE_TWO_DAYS_BEFORE, eventStartTime - (2 * 24 * 60 * 60 * 1000));
        scheduleNotification(context, alarmManager, Integer.parseInt(event.getId()), TYPE_ONE_DAY_BEFORE, eventStartTime - (24 * 60 * 60 * 1000));
        scheduleNotification(context, alarmManager, Integer.parseInt(event.getId()), TYPE_ONE_HOUR_BEFORE, eventStartTime - (60 * 60 * 1000));
        scheduleNotification(context, alarmManager, Integer.parseInt(event.getId()), TYPE_EVENT_START, eventStartTime);
        scheduleNotification(context, alarmManager, Integer.parseInt(event.getId()), TYPE_EVENT_END, eventEndTime);
    }

    private static void scheduleNotification(Context context, AlarmManager alarmManager, int eventId, String type, long triggerTime) {
        if (triggerTime <= System.currentTimeMillis()) {
            return;
        }

        Intent intent = new Intent(context, EventNotificationService.class);
        intent.putExtra(EXTRA_EVENT_ID, String.valueOf(eventId));
        intent.putExtra(EXTRA_NOTIFICATION_TYPE, type);

        int requestCode = generateRequestCode(eventId, type);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }

    public static void cancelEventNotifications(Context context, CalendarEvent event) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        String[] types = {TYPE_TWO_DAYS_BEFORE, TYPE_ONE_DAY_BEFORE, TYPE_ONE_HOUR_BEFORE, TYPE_EVENT_START, TYPE_EVENT_END};

        for (String type : types) {
            Intent intent = new Intent(context, EventNotificationService.class);
            intent.putExtra(EXTRA_EVENT_ID, event.getId());
            intent.putExtra(EXTRA_NOTIFICATION_TYPE, type);

            int requestCode = generateRequestCode(Integer.parseInt(event.getId()), type);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            alarmManager.cancel(pendingIntent);
        }
    }

    private static int generateRequestCode(int eventId, String type) {
        return eventId * 1000 + type.hashCode() % 1000;
    }

    public static void checkAndSendImmediateNotifications(Context context) {
        Database database = new Database(context);
        android.content.SharedPreferences sharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE);
        String username = sharedPreferences.getString("username", "");
        User currentUser = database.getUserByUsername(username);

        if (currentUser == null) return;

        Calendar today = Calendar.getInstance();
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        Calendar dayAfterTomorrow = Calendar.getInstance();
        dayAfterTomorrow.add(Calendar.DAY_OF_YEAR, 2);

        checkAndNotifyForDate(context, database, currentUser.getId(), today, "today");
        checkAndNotifyForDate(context, database, currentUser.getId(), tomorrow, "tomorrow");
        checkAndNotifyForDate(context, database, currentUser.getId(), dayAfterTomorrow, "in 2 days");
    }

    private static void checkAndNotifyForDate(Context context, Database database, int userId, Calendar date, String timeFrame) {
        long dayStart = date.getTimeInMillis();
        Calendar dayEndCal = (Calendar) date.clone();
        dayEndCal.add(Calendar.DAY_OF_YEAR, 1);
        long dayEnd = dayEndCal.getTimeInMillis();

        List<CalendarEvent> events = database.getEventsForUserByDate(userId, dayStart, dayEnd);

        for (CalendarEvent event : events) {
            if (event.isNotificationsEnabled()) {
                String correctTimeFrame = calculateTimeFrame(event.getStartTimeMillis());
                sendEventNotification(context, event, correctTimeFrame);
            }
        }
    }

    private static String calculateTimeFrame(long eventStartTime) {
        Calendar now = Calendar.getInstance();
        Calendar eventDate = Calendar.getInstance();
        eventDate.setTimeInMillis(eventStartTime);

        Calendar todayStart = Calendar.getInstance();
        todayStart.set(Calendar.HOUR_OF_DAY, 0);
        todayStart.set(Calendar.MINUTE, 0);
        todayStart.set(Calendar.SECOND, 0);
        todayStart.set(Calendar.MILLISECOND, 0);

        Calendar eventDayStart = Calendar.getInstance();
        eventDayStart.setTimeInMillis(eventStartTime);
        eventDayStart.set(Calendar.HOUR_OF_DAY, 0);
        eventDayStart.set(Calendar.MINUTE, 0);
        eventDayStart.set(Calendar.SECOND, 0);
        eventDayStart.set(Calendar.MILLISECOND, 0);

        long daysDifference = (eventDayStart.getTimeInMillis() - todayStart.getTimeInMillis()) / (24 * 60 * 60 * 1000);

        if (daysDifference == 0) {
            return "today";
        } else if (daysDifference == 1) {
            return "tomorrow";
        } else if (daysDifference == 2) {
            return "in 2 days";
        } else {
            return "upcoming";
        }
    }

    private static void sendEventNotification(Context context, CalendarEvent event, String timeFrame) {
        String title = "Event " + timeFrame;
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String eventTime = timeFormat.format(new Date(event.getStartTimeMillis()));
        String message = event.getTitle() + " at " + eventTime;

        sendNotification(context, title, message);
    }

    private static void sendNotification(Context context, String title, String message) {
        createNotificationChannel(context);

        if (!hasNotificationPermission(context)) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESCRIPTION);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }
}
