package com.example.calendar;

import android.os.AsyncTask;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HolidayService {
    private static final String TAG = "HolidayService";
    private Map<String, String> holidays;

    public interface HolidayCallback {
        void onHolidayFound(String holidayName);
        void onNoHoliday();
        void onError(String error);
    }

    public HolidayService() {
        initializeHolidays();
    }

    private void initializeHolidays() {
        holidays = new HashMap<>();
        holidays.put("01-01", "New Year's Day");
        holidays.put("02-14", "Valentine's Day");
        holidays.put("03-08", "International Women's Day");
        holidays.put("04-01", "April Fool's Day");
        holidays.put("05-01", "Labor Day");
        holidays.put("07-04", "Independence Day (US)");
        holidays.put("10-31", "Halloween");
        holidays.put("11-11", "Veterans Day");
        holidays.put("11-25", "Thanksgiving (approx)");
        holidays.put("12-25", "Christmas Day");
        holidays.put("12-31", "New Year's Eve");
    }

    public void checkHoliday(long dateMillis, HolidayCallback callback) {
        new HolidayTask(callback, dateMillis).execute();
    }

    private class HolidayTask extends AsyncTask<Void, Void, String> {
        private HolidayCallback callback;
        private long dateMillis;
        private String error;

        public HolidayTask(HolidayCallback callback, long dateMillis) {
            this.callback = callback;
            this.dateMillis = dateMillis;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                return checkLocalHolidays(dateMillis);
            } catch (Exception e) {
                error = e.getMessage();
                Log.e(TAG, "Holiday check error: " + error);
                return null;
            }
        }

        private String checkLocalHolidays(long dateMillis) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd", Locale.getDefault());
            return holidays.get(dateFormat.format(new Date(dateMillis)));
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                callback.onHolidayFound(result);
            } else if (error != null) {
                callback.onError(error);
            } else {
                callback.onNoHoliday();
            }
        }
    }
}
