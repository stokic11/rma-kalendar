package com.example.calendar;

import android.content.Context;
import android.graphics.Color;
import android.widget.CalendarView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarDecorator {
    private Context context;
    private HolidayService holidayService;
    private User currentUser;
    private List<Birthday> userBirthdays;

    public CalendarDecorator(Context context, HolidayService holidayService, User currentUser) {
        this.context = context;
        this.holidayService = holidayService;
        this.currentUser = currentUser;
        this.userBirthdays = new ArrayList<>();
    }

    public void setBirthdays(List<Birthday> birthdays) {
        this.userBirthdays = birthdays != null ? new ArrayList<>(birthdays) : new ArrayList<>();
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
        return isUserBirthday(dateMillis) || hasCustomBirthday(dateMillis);
    }

    private boolean isUserBirthday(long dateMillis) {
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

    private boolean hasCustomBirthday(long dateMillis) {
        if (userBirthdays == null || userBirthdays.isEmpty()) {
            return false;
        }

        Calendar targetCal = Calendar.getInstance();
        targetCal.setTimeInMillis(dateMillis);
        int targetMonth = targetCal.get(Calendar.MONTH);
        int targetDay = targetCal.get(Calendar.DAY_OF_MONTH);

        for (Birthday birthday : userBirthdays) {
            Calendar birthdayCal = Calendar.getInstance();
            birthdayCal.setTimeInMillis(birthday.getDateMillis());

            if (birthdayCal.get(Calendar.MONTH) == targetMonth &&
                birthdayCal.get(Calendar.DAY_OF_MONTH) == targetDay) {
                return true;
            }
        }
        return false;
    }

    public List<Birthday> getBirthdaysForDate(long dateMillis) {
        List<Birthday> birthdaysOnDate = new ArrayList<>();

        if (userBirthdays == null || userBirthdays.isEmpty()) {
            return birthdaysOnDate;
        }

        Calendar targetCal = Calendar.getInstance();
        targetCal.setTimeInMillis(dateMillis);
        int targetMonth = targetCal.get(Calendar.MONTH);
        int targetDay = targetCal.get(Calendar.DAY_OF_MONTH);

        for (Birthday birthday : userBirthdays) {
            Calendar birthdayCal = Calendar.getInstance();
            birthdayCal.setTimeInMillis(birthday.getDateMillis());

            if (birthdayCal.get(Calendar.MONTH) == targetMonth &&
                birthdayCal.get(Calendar.DAY_OF_MONTH) == targetDay) {
                birthdaysOnDate.add(birthday);
            }
        }
        return birthdaysOnDate;
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
