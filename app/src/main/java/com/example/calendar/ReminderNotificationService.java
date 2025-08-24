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
import java.util.Calendar;
import java.util.List;

public class ReminderNotificationService extends BroadcastReceiver {
    private static final String CHANNEL_ID = "reminder_notifications";
    private static final String CHANNEL_NAME = "Reminder Notifications";
    private static final String CHANNEL_DESCRIPTION = "Notifications for upcoming reminders";

    private static final String EXTRA_REMINDER_ID = "reminder_id";
    private static final String EXTRA_NOTIFICATION_TYPE = "notification_type";
    private static final String TYPE_ONE_HOUR_BEFORE = "one_hour_before";
    private static final String TYPE_EXACT_TIME = "exact_time";

    @Override
    public void onReceive(Context context, Intent intent) {
        createNotificationChannel(context);

        String reminderId = intent.getStringExtra(EXTRA_REMINDER_ID);
        String notificationType = intent.getStringExtra(EXTRA_NOTIFICATION_TYPE);

        if (reminderId != null && notificationType != null) {
            handleScheduledNotification(context, reminderId, notificationType);
        } else {
            checkRemindersAndNotify(context);
        }
    }

    private void handleScheduledNotification(Context context, String reminderId, String notificationType) {
        Database database = new Database(context);

        android.content.SharedPreferences prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE);
        String username = prefs.getString("username", "");

        if (username.isEmpty()) return;

        User currentUser = database.getUserByUsername(username);
        if (currentUser == null) return;

        List<Reminder> allReminders = database.getRemindersForUser(currentUser.getId());
        Reminder targetReminder = null;

        for (Reminder reminder : allReminders) {
            if (reminder.getId().equals(reminderId)) {
                targetReminder = reminder;
                break;
            }
        }

        if (targetReminder != null) {
            sendScheduledReminderNotification(context, targetReminder, notificationType);
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableVibration(true);
            channel.enableLights(true);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static void scheduleReminderNotifications(Context context, Reminder reminder) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        long reminderTimeMillis = reminder.getFullDateTimeMillis();
        long oneHourBeforeMillis = reminderTimeMillis - (60 * 60 * 1000);
        long currentTimeMillis = System.currentTimeMillis();

        if (oneHourBeforeMillis > currentTimeMillis) {
            scheduleNotification(context, reminder, oneHourBeforeMillis, TYPE_ONE_HOUR_BEFORE);
        }

        if (reminderTimeMillis > currentTimeMillis) {
            scheduleNotification(context, reminder, reminderTimeMillis, TYPE_EXACT_TIME);
        }
    }

    public static void cancelReminderNotifications(Context context, Reminder reminder) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent1Hour = new Intent(context, ReminderNotificationService.class);
        intent1Hour.putExtra(EXTRA_REMINDER_ID, reminder.getId());
        intent1Hour.putExtra(EXTRA_NOTIFICATION_TYPE, TYPE_ONE_HOUR_BEFORE);
        PendingIntent pendingIntent1Hour = PendingIntent.getBroadcast(
            context,
            (reminder.getId() + TYPE_ONE_HOUR_BEFORE).hashCode(),
            intent1Hour,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent1Hour);

        Intent intentExact = new Intent(context, ReminderNotificationService.class);
        intentExact.putExtra(EXTRA_REMINDER_ID, reminder.getId());
        intentExact.putExtra(EXTRA_NOTIFICATION_TYPE, TYPE_EXACT_TIME);
        PendingIntent pendingIntentExact = PendingIntent.getBroadcast(
            context,
            (reminder.getId() + TYPE_EXACT_TIME).hashCode(),
            intentExact,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntentExact);
    }

    private static void scheduleNotification(Context context, Reminder reminder, long triggerTimeMillis, String notificationType) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, ReminderNotificationService.class);
        intent.putExtra(EXTRA_REMINDER_ID, reminder.getId());
        intent.putExtra(EXTRA_NOTIFICATION_TYPE, notificationType);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            (reminder.getId() + notificationType).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent);
        }
    }

    private void checkRemindersAndNotify(Context context) {
        Database database = new Database(context);

        android.content.SharedPreferences prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE);
        String username = prefs.getString("username", "");

        if (username.isEmpty()) return;

        User currentUser = database.getUserByUsername(username);
        if (currentUser == null) return;

        Calendar today = Calendar.getInstance();
        Calendar tomorrow = Calendar.getInstance();
        Calendar dayAfterTomorrow = Calendar.getInstance();

        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        dayAfterTomorrow.add(Calendar.DAY_OF_YEAR, 2);

        checkAndNotifyForDate(context, database, currentUser.getId(), today, "today");
        checkAndNotifyForDate(context, database, currentUser.getId(), tomorrow, "tomorrow");
        checkAndNotifyForDate(context, database, currentUser.getId(), dayAfterTomorrow, "in 2 days");
    }

    private void checkAndNotifyForDate(Context context, Database database, int userId, Calendar date, String timeFrame) {
        List<Reminder> reminders = database.getRemindersForDate(userId, date.getTimeInMillis());

        for (Reminder reminder : reminders) {
            if (reminder.isNotificationsEnabled()) {
                sendReminderNotification(context, reminder, timeFrame);
            }
        }
    }

    public static void checkAndSendImmediateNotifications(Context context) {
        ReminderNotificationService service = new ReminderNotificationService();
        service.createNotificationChannel(context);
        service.checkRemindersAndNotify(context);
    }

    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    private void sendScheduledReminderNotification(Context context, Reminder reminder, String notificationType) {
        if (!hasNotificationPermission(context)) {
            return;
        }

        String title;
        String message;

        if (TYPE_ONE_HOUR_BEFORE.equals(notificationType)) {
            title = "Reminder in 1 Hour";
            message = "Upcoming: " + reminder.getTitle() + " at " + reminder.getTimeString();
        } else if (TYPE_EXACT_TIME.equals(notificationType)) {
            title = "Reminder Now!";
            message = reminder.getTitle() + " - It's time!";
        } else {
            return;
        }

        if (!reminder.getDescription().isEmpty()) {
            message += "\n" + reminder.getDescription();
        }

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            (reminder.getId() + notificationType).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            int notificationId = (reminder.getId() + notificationType).hashCode();
            notificationManager.notify(notificationId, builder.build());
        } catch (SecurityException e) {
        }
    }

    private void sendReminderNotification(Context context, Reminder reminder, String timeFrame) {
        if (!hasNotificationPermission(context)) {
            return;
        }

        String title = "Reminder";
        String message;

        if (timeFrame.equals("today")) {
            message = "Reminder: " + reminder.getTitle() + " is today at " + reminder.getTimeString() + "!";
        } else if (timeFrame.equals("tomorrow")) {
            message = "Reminder: " + reminder.getTitle() + " is tomorrow at " + reminder.getTimeString() + "!";
        } else {
            message = "Reminder: " + reminder.getTitle() + " is " + timeFrame + " at " + reminder.getTimeString() + "!";
        }

        if (!reminder.getDescription().isEmpty()) {
            message += " - " + reminder.getDescription();
        }

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            reminder.getId().hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(reminder.getId().hashCode(), builder.build());
        } catch (SecurityException e) {
        }
    }
}
