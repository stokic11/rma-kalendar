package com.example.calendar;

import android.content.Context;
import android.graphics.Color;
import android.widget.CalendarView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CalendarDecorator {
    private Context context;
    private HolidayService holidayService;
    private User currentUser;

    public CalendarDecorator(Context context, HolidayService holidayService, User currentUser) {
        this.context = context;
        this.holidayService = holidayService;
        this.currentUser = currentUser;
    }

    public boolean isHoliday(long dateMillis) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd", Locale.getDefault());
        String dateKey = dateFormat.format(new Date(dateMillis));
        return holidayService.isHolidayDate(dateKey);
    }

    public String getHolidayName(long dateMillis) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd", Locale.getDefault());
        String dateKey = dateFormat.format(new Date(dateMillis));
        return holidayService.getHolidayName(dateKey);
    }

    public boolean isBirthday(long dateMillis) {
        if (currentUser == null || currentUser.getBirthday() == null || currentUser.getBirthday().trim().isEmpty()) {
            return false;
        }

        try {
            String birthdayStr = currentUser.getBirthday().trim();
            SimpleDateFormat[] formats = {
                new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault()),
                new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
                new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            };

            Date birthday = null;
            for (SimpleDateFormat format : formats) {
                try {
                    birthday = format.parse(birthdayStr);
                    if (birthday != null) break;
                } catch (Exception e) {
                }
            }

            if (birthday == null) {
                return false;
            }

            Calendar birthdayCalendar = Calendar.getInstance();
            birthdayCalendar.setTime(birthday);

            Calendar selectedCalendar = Calendar.getInstance();
            selectedCalendar.setTimeInMillis(dateMillis);

            return birthdayCalendar.get(Calendar.MONTH) == selectedCalendar.get(Calendar.MONTH) &&
                   birthdayCalendar.get(Calendar.DAY_OF_MONTH) == selectedCalendar.get(Calendar.DAY_OF_MONTH);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getDateType(long dateMillis) {
        if (isBirthday(dateMillis)) {
            return "birthday";
        } else if (isHoliday(dateMillis)) {
            return "holiday";
        }
        return "normal";
    }

    public int getDateColor(long dateMillis) {
        String dateType = getDateType(dateMillis);
        switch (dateType) {
            case "birthday":
                return Color.parseColor("#FFD700");
            case "holiday":
                return Color.parseColor("#32CD32");
            default:
                return Color.WHITE;
        }
    }

    public String getDateSymbol(long dateMillis) {
        String dateType = getDateType(dateMillis);
        switch (dateType) {
            case "birthday":
                return "⭐";
            case "holiday":
                return "🎉";
            default:
                return "";
        }
    }
}
