package com.example.calendar;

import android.graphics.Color;
import java.util.Calendar;

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
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
    public boolean isAllDay() { return isAllDay; }
    public void setAllDay(boolean allDay) { isAllDay = allDay; }

    public int getDurationInMinutes() { return (int) ((endTime - startTime) / (1000 * 60)); }

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
        Calendar startCal = Calendar.getInstance();
        startCal.setTimeInMillis(startTime);
        Calendar endCal = Calendar.getInstance();
        endCal.setTimeInMillis(endTime);

        if (isSameDay(startTime, endTime)) {
            return String.format("%02d:%02d - %02d:%02d",
                    startCal.get(Calendar.HOUR_OF_DAY), startCal.get(Calendar.MINUTE),
                    endCal.get(Calendar.HOUR_OF_DAY), endCal.get(Calendar.MINUTE));
        } else {
            return String.format("%02d/%02d %02d:%02d - %02d/%02d %02d:%02d",
                    startCal.get(Calendar.DAY_OF_MONTH), startCal.get(Calendar.MONTH) + 1,
                    startCal.get(Calendar.HOUR_OF_DAY), startCal.get(Calendar.MINUTE),
                    endCal.get(Calendar.DAY_OF_MONTH), endCal.get(Calendar.MONTH) + 1,
                    endCal.get(Calendar.HOUR_OF_DAY), endCal.get(Calendar.MINUTE));
        }
    }

    public long getStartTimeMillis() { return startTime; }
    public long getEndTimeMillis() { return endTime; }

    public boolean isMultiDay() { return !isSameDay(startTime, endTime); }

    public int getDayInSequence(long checkDate) {
        if (!isMultiDay()) return 1;
        return (int) ((getDayStart(checkDate) - getDayStart(startTime)) / (24 * 60 * 60 * 1000)) + 1;
    }

    public int getTotalDays() {
        if (!isMultiDay()) return 1;
        return (int) ((getDayStart(endTime) - getDayStart(startTime)) / (24 * 60 * 60 * 1000)) + 1;
    }

    public boolean occursOnDate(long checkDate) {
        long checkDayStart = getDayStart(checkDate);
        long startDayStart = getDayStart(startTime);
        long endDayStart = getDayStart(endTime);
        return checkDayStart >= startDayStart && checkDayStart <= endDayStart;
    }

    private boolean isSameDay(long time1, long time2) {
        return getDayStart(time1) == getDayStart(time2);
    }

    private long getDayStart(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private int generateRandomColor() {
        int[] colors = {
                Color.parseColor("#4285F4"), Color.parseColor("#34A853"),
                Color.parseColor("#FBBC04"), Color.parseColor("#EA4335"),
                Color.parseColor("#9C27B0"), Color.parseColor("#FF9800"),
                Color.parseColor("#795548"), Color.parseColor("#607D8B")
        };
        return colors[(int) (Math.random() * colors.length)];
    }
}
