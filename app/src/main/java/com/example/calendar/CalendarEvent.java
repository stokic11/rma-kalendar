package com.example.calendar;

import android.graphics.Color;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CalendarEvent {
    private String id;
    private String title;
    private String description;
    private long startTime;
    private long endTime;
    private int color;
    private boolean isAllDay;

    public CalendarEvent(String title, String description, long startTime, long endTime, int color) {
        this.id = String.valueOf(System.currentTimeMillis());
        this.title = title;
        this.description = description != null ? description : "";
        this.startTime = startTime;
        this.endTime = endTime;
        this.color = color != 0 ? color : generateRandomColor();
        this.isAllDay = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public long getStartTime() { return startTime; }
    public long getStartTimeMillis() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public long getEndTime() { return endTime; }
    public long getEndTimeMillis() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
    public boolean isAllDay() { return isAllDay; }
    public void setAllDay(boolean allDay) { isAllDay = allDay; }

    public int getDurationInMinutes() {
        return (int) ((endTime - startTime) / (1000 * 60));
    }

    public int getStartHour() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startTime);
        return cal.get(Calendar.HOUR_OF_DAY);
    }

    public int getStartMinute() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startTime);
        return cal.get(Calendar.MINUTE);
    }

    public int getEndHour() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(endTime);
        return cal.get(Calendar.HOUR_OF_DAY);
    }

    public int getEndMinute() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(endTime);
        return cal.get(Calendar.MINUTE);
    }

    public String getTimeRange() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return timeFormat.format(new Date(startTime)) + " - " + timeFormat.format(new Date(endTime));
    }

    public boolean isMultiDay() {
        Calendar startCal = Calendar.getInstance();
        startCal.setTimeInMillis(startTime);
        Calendar endCal = Calendar.getInstance();
        endCal.setTimeInMillis(endTime);

        return startCal.get(Calendar.YEAR) != endCal.get(Calendar.YEAR) ||
               startCal.get(Calendar.DAY_OF_YEAR) != endCal.get(Calendar.DAY_OF_YEAR);
    }

    public int getDayInSequence(long dateMillis) {
        Calendar eventStart = Calendar.getInstance();
        eventStart.setTimeInMillis(startTime);
        eventStart.set(Calendar.HOUR_OF_DAY, 0);
        eventStart.set(Calendar.MINUTE, 0);
        eventStart.set(Calendar.SECOND, 0);
        eventStart.set(Calendar.MILLISECOND, 0);

        Calendar targetDate = Calendar.getInstance();
        targetDate.setTimeInMillis(dateMillis);
        targetDate.set(Calendar.HOUR_OF_DAY, 0);
        targetDate.set(Calendar.MINUTE, 0);
        targetDate.set(Calendar.SECOND, 0);
        targetDate.set(Calendar.MILLISECOND, 0);

        long diffInMillis = targetDate.getTimeInMillis() - eventStart.getTimeInMillis();
        return (int) (diffInMillis / (1000 * 60 * 60 * 24)) + 1;
    }

    public int getTotalDays() {
        Calendar startCal = Calendar.getInstance();
        startCal.setTimeInMillis(startTime);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        Calendar endCal = Calendar.getInstance();
        endCal.setTimeInMillis(endTime);
        endCal.set(Calendar.HOUR_OF_DAY, 0);
        endCal.set(Calendar.MINUTE, 0);
        endCal.set(Calendar.SECOND, 0);
        endCal.set(Calendar.MILLISECOND, 0);

        long diffInMillis = endCal.getTimeInMillis() - startCal.getTimeInMillis();
        return (int) (diffInMillis / (1000 * 60 * 60 * 24)) + 1;
    }

    private int generateRandomColor() {
        int[] colors = {
            Color.parseColor("#4285F4"),
            Color.parseColor("#34A853"),
            Color.parseColor("#EA4335"),
            Color.parseColor("#FF9800"),
            Color.parseColor("#9C27B0")
        };
        return colors[(int) (Math.random() * colors.length)];
    }
}
