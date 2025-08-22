package com.example.calendar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private CalendarView calendarView;
    private TextView tvInstruction;
    private TextView tvCurrentYear;
    private TextView btnPrevYear;
    private TextView btnNextYear;
    private Button btnToday;
    private Button btnLogout;
    private Button btnOpenDayView;
    private SharedPreferences sharedPreferences;
    private Database database;
    private HolidayService holidayService;

    private long currentSelectedDate = 0;
    private Calendar currentCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_main);

            sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE);
            database = new Database(this);
            holidayService = new HolidayService();
            currentCalendar = Calendar.getInstance();

            if (!sharedPreferences.getBoolean("is_logged_in", false)) {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }

            initViews();
            loadUserData();
            setupCalendarListener();
            setupDirectNavigationControls();
            updateMonthYearDisplay();

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error starting main activity: " + e.getMessage(), Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tv_welcome);
        calendarView = findViewById(R.id.calendar_view);
        tvInstruction = findViewById(R.id.tv_instruction);
        tvCurrentYear = findViewById(R.id.tv_current_year);
        btnPrevYear = findViewById(R.id.btn_prev_year);
        btnNextYear = findViewById(R.id.btn_next_year);
        btnToday = findViewById(R.id.btn_reset);
        btnLogout = findViewById(R.id.btn_logout);
        btnOpenDayView = findViewById(R.id.btn_open_day_view);

        btnLogout.setOnClickListener(v -> logout());
        btnOpenDayView.setOnClickListener(v -> {
            if (currentSelectedDate != 0) {
                openDayView(currentSelectedDate);
            } else {
                Toast.makeText(this, "Please select a date first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupDirectNavigationControls() {
        btnPrevYear.setOnClickListener(v -> {
            currentCalendar.add(Calendar.YEAR, -1);
            navigateCalendar();
            Toast.makeText(this, "Year: " + currentCalendar.get(Calendar.YEAR), Toast.LENGTH_SHORT).show();
        });

        btnNextYear.setOnClickListener(v -> {
            currentCalendar.add(Calendar.YEAR, 1);
            navigateCalendar();
            Toast.makeText(this, "Year: " + currentCalendar.get(Calendar.YEAR), Toast.LENGTH_SHORT).show();
        });

        btnToday.setOnClickListener(v -> {
            currentCalendar = Calendar.getInstance(); // Reset to current date
            navigateCalendar();
            Toast.makeText(this, "Jumped to today", Toast.LENGTH_SHORT).show();
        });
    }

    private void navigateCalendar() {
        // Update the actual CalendarView to show the new month/year
        calendarView.setDate(currentCalendar.getTimeInMillis(), true, true);
        updateMonthYearDisplay();
    }

    private void updateMonthYearDisplay() {
        // Update the year display (highlighted in gold)
        String year = String.valueOf(currentCalendar.get(Calendar.YEAR));
        tvCurrentYear.setText(year);
    }

    private void setupCalendarListener() {
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            currentSelectedDate = calendar.getTimeInMillis();

            // Update current calendar to match selected date's month/year
            currentCalendar.set(year, month, dayOfMonth);
            updateMonthYearDisplay();

            showDateInfo(currentSelectedDate);
            btnOpenDayView.setVisibility(android.view.View.VISIBLE);
        });
    }

    private void showDateInfo(long dateMillis) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault());
        String dateStr = dateFormat.format(new Date(dateMillis));
        tvInstruction.setText("Selected: " + dateStr);

        checkHoliday(dateMillis);
    }

    private void loadUserData() {
        try {
            String username = sharedPreferences.getString("username", "");
            User user = database.getUserDetails(username);

            if (user != null) {
                tvWelcome.setText("Welcome back, " + user.getFullName() + "!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            tvWelcome.setText("Welcome to Calendar!");
        }
    }


    private void checkHoliday(long dateMillis) {
        holidayService.checkHoliday(dateMillis, new HolidayService.HolidayCallback() {
            @Override
            public void onHolidayFound(String holidayName) {
                runOnUiThread(() -> {
                    String currentText = tvInstruction.getText().toString();
                    tvInstruction.setText(currentText + " 🎉 " + holidayName);
                });
            }

            @Override
            public void onNoHoliday() {
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    private void openDayView(long dateMillis) {
        Toast.makeText(this, "Opening day view...", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, DayViewActivity.class);
        intent.putExtra("selected_date", dateMillis);
        startActivity(intent);
    }

    private void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}