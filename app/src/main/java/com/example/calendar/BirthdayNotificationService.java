package com.example.calendar;

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

public class BirthdayNotificationService extends BroadcastReceiver {
    private static final String CHANNEL_ID = "birthday_notifications";
    private static final String CHANNEL_NAME = "Birthday Notifications";
    private static final String CHANNEL_DESCRIPTION = "Notifications for upcoming birthdays";

    @Override
    public void onReceive(Context context, Intent intent) {
        createNotificationChannel(context);
        checkBirthdaysAndNotify(context);
    }

    private void createNotificationChannel(Context context) {
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

    private void checkBirthdaysAndNotify(Context context) {
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

        checkAndNotifyForDate(context, database, currentUser.getId(), today, "today", true);
        checkAndNotifyForDate(context, database, currentUser.getId(), tomorrow, "tomorrow", false);
        checkAndNotifyForDate(context, database, currentUser.getId(), dayAfterTomorrow, "in 2 days", false);
    }

    private void checkAndNotifyForDate(Context context, Database database, int userId, Calendar date, String timeFrame, boolean mandatory) {
        List<Birthday> birthdays = database.getBirthdaysForDate(userId, date.getTimeInMillis());

        for (Birthday birthday : birthdays) {
            if (mandatory || birthday.isNotificationsEnabled()) {
                sendBirthdayNotification(context, birthday, timeFrame);
            }
        }
    }

    public static void checkAndSendImmediateNotifications(Context context) {
        BirthdayNotificationService service = new BirthdayNotificationService();
        service.createNotificationChannel(context);
        service.checkBirthdaysAndNotify(context);
    }

    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    private void sendBirthdayNotification(Context context, Birthday birthday, String timeFrame) {
        if (!hasNotificationPermission(context)) {
            return;
        }

        String title = "Birthday Reminder";
        String message;

        if (timeFrame.equals("today")) {
            message = birthday.getNamesText() + "'s birthday is today!";
        } else if (timeFrame.equals("tomorrow")) {
            message = birthday.getNamesText() + "'s birthday is tomorrow!";
        } else {
            message = birthday.getNamesText() + "'s birthday is " + timeFrame + "!";
        }

        if (!birthday.getDescription().isEmpty()) {
            message += " - " + birthday.getDescription();
        }

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            birthday.getId().hashCode(),
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
            .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(birthday.getId().hashCode(), builder.build());
        } catch (SecurityException e) {
            // Handle permission denied
        }
    }

    public static void scheduleDailyNotificationCheck(Context context) {
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, BirthdayNotificationService.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (alarmManager != null) {
            alarmManager.setRepeating(
                android.app.AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                android.app.AlarmManager.INTERVAL_DAY,
                pendingIntent
            );
        }
    }
}
