package com.example.calendar;

import android.graphics.Color;
import java.util.Calendar;
import java.util.Locale;

public class Reminder {
    private String id;
    private String title;
    private String description;
    private long dateMillis;
    private int hour;
    private int minute;
    private boolean notificationsEnabled;
    private int color;

    public Reminder(String title, String description, long dateMillis, boolean notificationsEnabled) {
        this.id = String.valueOf(System.currentTimeMillis());
        this.title = title != null ? title : "";
        this.description = description != null ? description : "";
        this.dateMillis = dateMillis;
        this.hour = 9;
        this.minute = 0;
        this.notificationsEnabled = notificationsEnabled;
        this.color = Color.parseColor("#FFA500");
    }

    public Reminder(String title, String description, long dateMillis, int hour, int minute, boolean notificationsEnabled) {
        this.id = String.valueOf(System.currentTimeMillis());
        this.title = title != null ? title : "";
        this.description = description != null ? description : "";
        this.dateMillis = dateMillis;
        this.hour = hour;
        this.minute = minute;
        this.notificationsEnabled = notificationsEnabled;
        this.color = Color.parseColor("#FFA500");
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getDateMillis() { return dateMillis; }
    public void setDateMillis(long dateMillis) { this.dateMillis = dateMillis; }

    public int getHour() { return hour; }
    public void setHour(int hour) { this.hour = hour; }

    public int getMinute() { return minute; }
    public void setMinute(int minute) { this.minute = minute; }

    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }

    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }

    public String getTimeString() {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
    }

    public long getFullDateTimeMillis() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateMillis);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public int getStartMinuteOfDay() {
        return hour * 60 + minute;
    }

    public int getDurationInMinutes() {
        return 30;
    }
}
