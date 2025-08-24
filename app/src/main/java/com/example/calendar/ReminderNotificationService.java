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
    private static final String TYPE_TWO_DAYS_BEFORE = "two_days_before";
    private static final String TYPE_ONE_DAY_BEFORE = "one_day_before";
    private static final String TYPE_ONE_HOUR_BEFORE = "one_hour_before";
    private static final String TYPE_EXACT_TIME = "exact_time";

    @Override
    public void onReceive(Context context, Intent intent) {
        createNotificationChannel(context);

        String reminderId = intent.getStringExtra(EXTRA_REMINDER_ID);
        String notificationType = intent.getStringExtra(EXTRA_NOTIFICATION_TYPE);

        if (reminderId != null && notificationType != null) {
            handleScheduledNotification(context, reminderId, notificationType);
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            rescheduleAllNotifications(context);
        } else {
            checkRemindersAndNotify(context);
        }
    }

    private void rescheduleAllNotifications(Context context) {
        Database database = new Database(context);
        android.content.SharedPreferences prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE);
        String username = prefs.getString("username", "");

        if (username.isEmpty()) return;

        User currentUser = database.getUserByUsername(username);
        if (currentUser == null) return;

        List<Reminder> allReminders = database.getRemindersForUser(currentUser.getId());
        for (Reminder reminder : allReminders) {
            scheduleReminderNotifications(context, reminder);
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return;
        }

        long reminderTimeMillis = reminder.getFullDateTimeMillis();
        long currentTimeMillis = System.currentTimeMillis();

        long twoDaysBeforeMillis = reminderTimeMillis - (2 * 24 * 60 * 60 * 1000);
        long oneDayBeforeMillis = reminderTimeMillis - (24 * 60 * 60 * 1000);
        long oneHourBeforeMillis = reminderTimeMillis - (60 * 60 * 1000);

        if (twoDaysBeforeMillis > currentTimeMillis && reminder.isNotificationsEnabled()) {
            scheduleNotification(context, reminder, twoDaysBeforeMillis, TYPE_TWO_DAYS_BEFORE);
        }

        if (oneDayBeforeMillis > currentTimeMillis && reminder.isNotificationsEnabled()) {
            scheduleNotification(context, reminder, oneDayBeforeMillis, TYPE_ONE_DAY_BEFORE);
        }

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

        String[] notificationTypes = {TYPE_TWO_DAYS_BEFORE, TYPE_ONE_DAY_BEFORE, TYPE_ONE_HOUR_BEFORE, TYPE_EXACT_TIME};

        for (String type : notificationTypes) {
            Intent intent = new Intent(context, ReminderNotificationService.class);
            intent.putExtra(EXTRA_REMINDER_ID, reminder.getId());
            intent.putExtra(EXTRA_NOTIFICATION_TYPE, type);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (reminder.getId() + type).hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            alarmManager.cancel(pendingIntent);
        }
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

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent);
            }
        } catch (SecurityException e) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent);
            } catch (Exception fallbackException) {
            }
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

        switch (notificationType) {
            case TYPE_TWO_DAYS_BEFORE:
                title = "Reminder in 2 Days";
                message = "Upcoming: " + reminder.getTitle() + " in 2 days at " + reminder.getTimeString();
                break;
            case TYPE_ONE_DAY_BEFORE:
                title = "Reminder Tomorrow";
                message = "Upcoming: " + reminder.getTitle() + " tomorrow at " + reminder.getTimeString();
                break;
            case TYPE_ONE_HOUR_BEFORE:
                title = "Reminder in 1 Hour";
                message = "Upcoming: " + reminder.getTitle() + " at " + reminder.getTimeString();
                break;
            case TYPE_EXACT_TIME:
                title = "Reminder Now!";
                message = reminder.getTitle() + " - It's time!";
                break;
            default:
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
