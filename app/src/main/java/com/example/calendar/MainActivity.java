package com.example.calendar;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private CalendarView calendarView;
    private TextView tvInstruction;
    private Button btnMonthPicker;
    private Button btnYearPicker;
    private Button btnToday;
    private Button btnOpenDayView;
    private ImageButton btnMenu;
    private DrawerLayout drawerLayout;
    private LinearLayout nav;
    private LinearLayout navProfileItem;
    private LinearLayout navLogoutItem;
    private TextView navUserName;
    private TextView navUserUsername;
    private SharedPreferences sharedPreferences;
    private Database database;
    private HolidayService holidayService;
    private User currentUser;
    private CalendarDecorator calendarDecorator;
    private List<Birthday> userBirthdays;

    private long currentSelectedDate = 0;
    private Calendar currentCalendar;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_main);

            // Initialize permission launcher
            requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
                        // Check for immediate notifications
                        BirthdayNotificationService.checkAndSendImmediateNotifications(this);
                    } else {
                        Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
            );

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
            setupDrawer();
            setupBackPressedCallback();
            applyButtonStyling();
            loadUserData();
            loadUserBirthdays();
            setupCalendarListener();
            setupNavigationControls();
            customizeCalendarAppearance();

            // Initialize birthday notification system
            BirthdayNotificationService.scheduleDailyNotificationCheck(this);

            // Request notification permission if needed
            requestNotificationPermission();

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

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!BirthdayNotificationService.hasNotificationPermission(this)) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            } else {
                // Check for immediate notifications if permission already granted
                BirthdayNotificationService.checkAndSendImmediateNotifications(this);
            }
        } else {
            // For older versions, check immediate notifications
            BirthdayNotificationService.checkAndSendImmediateNotifications(this);
        }
    }

    private void applyButtonStyling() {
        applyButtonBackground(btnToday, "#d4006d", "#FF69B4", 8);
        applyButtonBackground(btnOpenDayView, "#d4006d", "#FF69B4", 8);
        applyButtonBackground(btnMonthPicker, "#d4006d", "#FF69B4", 8);
        applyButtonBackground(btnYearPicker, "#d4006d", "#FF69B4", 8);
    }

    private void applyButtonBackground(Button button, String fillColor, String strokeColor, int cornerRadius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(android.graphics.Color.parseColor(fillColor));
        drawable.setStroke(2, android.graphics.Color.parseColor(strokeColor));
        drawable.setCornerRadius(cornerRadius * getResources().getDisplayMetrics().density);
        button.setBackground(drawable);
    }

    private void setupBackPressedCallback() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tv_welcome);
        calendarView = findViewById(R.id.calendar_view);
        tvInstruction = findViewById(R.id.tv_instruction);
        btnMonthPicker = findViewById(R.id.btn_month_picker);
        btnYearPicker = findViewById(R.id.btn_year_picker);
        btnToday = findViewById(R.id.btn_reset);
        btnOpenDayView = findViewById(R.id.btn_open_day_view);
        btnMenu = findViewById(R.id.btn_menu);
        drawerLayout = findViewById(R.id.drawer_layout);
        nav = findViewById(R.id.nav);
        navProfileItem = findViewById(R.id.nav_profile_item);
        navLogoutItem = findViewById(R.id.nav_logout_item);
        navUserName = findViewById(R.id.nav_user_name);
        navUserUsername = findViewById(R.id.nav_user_username);

        btnOpenDayView.setOnClickListener(v -> {
            if (currentSelectedDate != 0) {
                openDayView(currentSelectedDate);
            } else {
                Toast.makeText(this, "Please select a date first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupDrawer() {
        btnMenu.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        navProfileItem.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        navLogoutItem.setOnClickListener(v -> {
            logout();
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        updateNavigationHeader();
    }

    private void updateNavigationHeader() {
        if (currentUser != null) {
            if (navUserName != null) {
                navUserName.setText(currentUser.getFullName());
            }
            if (navUserUsername != null) {
                navUserUsername.setText("@" + currentUser.getUsername());
            }
        }
    }

    private void setupNavigationControls() {
        btnMonthPicker.setOnClickListener(v -> showMonthPickerDialog());
        btnYearPicker.setOnClickListener(v -> showYearPickerDialog());

        btnToday.setOnClickListener(v -> {
            currentCalendar = Calendar.getInstance();
            navigateCalendar();
        });
    }

    private void showMonthPickerDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_month_picker, null);
        NumberPicker monthPicker = dialogView.findViewById(R.id.month_picker);

        String[] months = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};

        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(months);
        monthPicker.setValue(currentCalendar.get(Calendar.MONTH));
        monthPicker.setWrapSelectorWheel(true);

        AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog)
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btn_ok).setOnClickListener(v -> {
            currentCalendar.set(Calendar.MONTH, monthPicker.getValue());
            navigateCalendar();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showYearPickerDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_year_picker, null);
        NumberPicker yearPicker = dialogView.findViewById(R.id.year_picker);

        int currentYear = currentCalendar.get(Calendar.YEAR);
        yearPicker.setMinValue(1900);
        yearPicker.setMaxValue(2100);
        yearPicker.setValue(currentYear);
        yearPicker.setWrapSelectorWheel(true);

        AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog)
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btn_ok).setOnClickListener(v -> {
            currentCalendar.set(Calendar.YEAR, yearPicker.getValue());
            navigateCalendar();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void navigateCalendar() {
        calendarView.setDate(currentCalendar.getTimeInMillis(), true, true);
    }

    private void setupCalendarListener() {
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            currentSelectedDate = calendar.getTimeInMillis();

            currentCalendar.set(year, month, dayOfMonth);

            showDateInfo(currentSelectedDate);
            btnOpenDayView.setVisibility(android.view.View.VISIBLE);
        });
    }

    private void loadUserData() {
        try {
            String username = sharedPreferences.getString("username", "");
            currentUser = database.getUserDetails(username);

            if (currentUser != null) {
                calendarDecorator = new CalendarDecorator(this, holidayService, currentUser);
                updateNavigationHeader();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadUserBirthdays() {
        String username = sharedPreferences.getString("username", "");
        currentUser = database.getUserByUsername(username);
        if (currentUser != null) {
            userBirthdays = database.getBirthdaysForUser(currentUser.getId());
            updateCalendarDecorators();
        }
    }

    private void updateCalendarDecorators() {
        if (calendarDecorator != null) {
            calendarDecorator.setBirthdays(userBirthdays);
        }
    }

    private void customizeCalendarAppearance() {
        if (calendarDecorator == null && currentUser != null) {
            calendarDecorator = new CalendarDecorator(this, holidayService, currentUser);
        }
    }

    private void showDateInfo(long dateMillis) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault());
        String dateStr = dateFormat.format(new Date(dateMillis));

        StringBuilder infoText = new StringBuilder("Selected: " + dateStr);

        boolean isUserBirthdayDate = isBirthday(dateMillis);
        List<Birthday> customBirthdays = database.getBirthdaysForDate(currentUser.getId(), dateMillis);
        boolean hasCustomBirthdays = !customBirthdays.isEmpty();

        if (isUserBirthdayDate) {
            infoText.append(" - Happy Birthday, ").append(currentUser.getFirstName()).append("!");
        }

        if (hasCustomBirthdays) {
            for (Birthday birthday : customBirthdays) {
                infoText.append(" - Birthday: ").append(birthday.getNamesText());
                if (!birthday.getDescription().isEmpty()) {
                    infoText.append(" (").append(birthday.getDescription()).append(")");
                }
            }
        }

        if (calendarDecorator != null) {
            boolean isHolidayDate = calendarDecorator.isHoliday(dateMillis);
            if (isHolidayDate) {
                String holidayName = calendarDecorator.getHolidayName(dateMillis);
                infoText.append(" - Holiday: ").append(holidayName);
            }
        } else {
            checkHoliday(dateMillis);
        }

        tvInstruction.setText(infoText.toString());
    }

    private boolean isBirthday(long dateMillis) {
        if (currentUser == null || currentUser.getBirthday() == null) {
            return false;
        }

        try {
            SimpleDateFormat birthdayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date birthday = birthdayFormat.parse(currentUser.getBirthday());

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

    private void checkHoliday(long dateMillis) {
        holidayService.checkHoliday(dateMillis, new HolidayService.HolidayCallback() {
            @Override
            public void onHolidayFound(String holidayName) {
                runOnUiThread(() -> {
                    String currentText = tvInstruction.getText().toString();
                    if (!currentText.contains("Happy Birthday")) {
                        tvInstruction.setText(currentText + " 🎉 " + holidayName);
                    } else {
                        tvInstruction.setText(currentText + " & " + holidayName);
                    }
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

    private boolean hasBirthdayOnDate(long dateMillis) {
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

    @Override
    protected void onResume() {
        super.onResume();
        loadUserBirthdays();
        if (currentSelectedDate != 0) {
            showDateInfo(currentSelectedDate);
        }
    }
}
