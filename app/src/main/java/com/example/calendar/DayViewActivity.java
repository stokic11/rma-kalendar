package com.example.calendar;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DayViewActivity extends AppCompatActivity {
    private ImageButton btnBack;
    private TextView tvSelectedDate;
    private TextView tvHolidayInfo;
    private TextView tvBirthdayInfo;
    private LinearLayout layoutDayInfo;
    private Button btnAddEvent;
    private FrameLayout eventsContainer;
    private HolidayService holidayService;
    private Database database;
    private SharedPreferences sharedPreferences;
    private List<CalendarEvent> events;
    private List<Birthday> birthdays;
    private List<Reminder> reminders;
    private long selectedDateMillis;
    private int currentUserId;
    private User currentUser;
    private static final int HOUR_HEIGHT_DP = 60;
    private float startY = 0;
    private float startX = 0;
    private boolean isDragging = false;
    private static final float SCROLL_THRESHOLD = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_day_view);

        holidayService = new HolidayService();
        database = new Database(this);
        sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE);
        events = new ArrayList<>();
        birthdays = new ArrayList<>();
        reminders = new ArrayList<>();

        String username = sharedPreferences.getString("username", "");
        currentUser = database.getUserByUsername(username);
        if (currentUser != null) {
            currentUserId = currentUser.getId();
        } else {
            finish();
            return;
        }

        initViews();
        setupListeners();
        selectedDateMillis = getIntent().getLongExtra("selected_date", System.currentTimeMillis());
        loadDateInfo(selectedDateMillis);
        checkHolidays(selectedDateMillis);
        checkBirthday(selectedDateMillis);
        loadEventsFromDatabase();
        loadBirthdaysFromDatabase();
        loadRemindersFromDatabase();
        renderEvents();
        checkCustomBirthdays(selectedDateMillis);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvSelectedDate = findViewById(R.id.tv_selected_date);
        tvHolidayInfo = findViewById(R.id.tv_holiday_info);
        tvBirthdayInfo = findViewById(R.id.tv_birthday_info);
        layoutDayInfo = findViewById(R.id.layout_day_info);
        btnAddEvent = findViewById(R.id.btn_add_event);
        eventsContainer = findViewById(R.id.events_container);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnAddEvent.setOnClickListener(v -> showAddTypeDialog());
        eventsContainer.setOnTouchListener(this::handleEventsContainerTouch);
    }

    private boolean handleEventsContainerTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startY = event.getY();
                startX = event.getX();
                isDragging = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                float deltaY = Math.abs(event.getY() - startY);
                float deltaX = Math.abs(event.getX() - startX);
                if (deltaY > SCROLL_THRESHOLD || deltaX > SCROLL_THRESHOLD) {
                    isDragging = true;
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (!isDragging) {
                    float y = event.getY();
                    int hour = (int) (y / dpToPx(HOUR_HEIGHT_DP));
                    int minute = (int) ((y % dpToPx(HOUR_HEIGHT_DP)) / dpToPx(HOUR_HEIGHT_DP) * 60);
                    minute = (minute / 15) * 15;
                    if (hour >= 0 && hour < 24) {
                        showAddTypeDialogAtTime(hour, minute);
                    }
                }
                return true;
            default:
                return false;
        }
    }

    private void loadDateInfo(long selectedDateMillis) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault());
        tvSelectedDate.setText(dateFormat.format(new Date(selectedDateMillis)));
    }

    private void checkBirthday(long selectedDateMillis) {
        if (isBirthday(selectedDateMillis)) {
            tvBirthdayInfo.setText("🎂 Happy Birthday, " + currentUser.getFirstName() + "!");
            tvBirthdayInfo.setVisibility(View.VISIBLE);
            tvBirthdayInfo.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
            tvBirthdayInfo.setPadding(16, 8, 16, 8);
        } else {
            tvBirthdayInfo.setVisibility(View.GONE);
        }
    }

    private boolean isBirthday(long dateMillis) {
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
            return false;
        }
    }

    private void checkHolidays(long selectedDateMillis) {
        holidayService.checkHoliday(selectedDateMillis, new HolidayService.HolidayCallback() {
            @Override
            public void onHolidayFound(String holidayName) {
                runOnUiThread(() -> {
                    tvHolidayInfo.setText("🎉 " + holidayName);
                    tvHolidayInfo.setVisibility(View.VISIBLE);
                    tvHolidayInfo.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                    tvHolidayInfo.setPadding(16, 8, 16, 8);
                });
            }

            @Override
            public void onNoHoliday() {
                runOnUiThread(() -> tvHolidayInfo.setVisibility(View.GONE));
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> tvHolidayInfo.setVisibility(View.GONE));
            }
        });
    }

    private void loadEventsFromDatabase() {
        Calendar dayStart = Calendar.getInstance();
        dayStart.setTimeInMillis(selectedDateMillis);
        dayStart.set(Calendar.HOUR_OF_DAY, 0);
        dayStart.set(Calendar.MINUTE, 0);
        dayStart.set(Calendar.SECOND, 0);
        dayStart.set(Calendar.MILLISECOND, 0);

        Calendar dayEnd = Calendar.getInstance();
        dayEnd.setTimeInMillis(selectedDateMillis);
        dayEnd.set(Calendar.HOUR_OF_DAY, 23);
        dayEnd.set(Calendar.MINUTE, 59);
        dayEnd.set(Calendar.SECOND, 59);
        dayEnd.set(Calendar.MILLISECOND, 999);

        events = database.getEventsForUserByDate(currentUserId, dayStart.getTimeInMillis(), dayEnd.getTimeInMillis());
    }

    private void loadBirthdaysFromDatabase() {
        birthdays = database.getBirthdaysForDate(currentUserId, selectedDateMillis);
    }

    private void loadRemindersFromDatabase() {
        reminders = database.getRemindersForDate(currentUserId, selectedDateMillis);
    }

    private void checkCustomBirthdays(long selectedDateMillis) {
        List<Birthday> todaysBirthdays = database.getBirthdaysForDate(currentUserId, selectedDateMillis);
        if (!todaysBirthdays.isEmpty()) {
            for (Birthday birthday : todaysBirthdays) {
                StringBuilder birthdayText = new StringBuilder();
                birthdayText.append("🎂 Birthday: ").append(birthday.getNamesText());
                if (!birthday.getDescription().isEmpty()) {
                    birthdayText.append(" - ").append(birthday.getDescription());
                }

                TextView birthdayInfoView = new TextView(this);
                birthdayInfoView.setText(birthdayText.toString());
                birthdayInfoView.setTextColor(getResources().getColor(android.R.color.white));
                birthdayInfoView.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
                birthdayInfoView.setPadding(16, 8, 16, 8);
                birthdayInfoView.setVisibility(View.VISIBLE);

                birthdayInfoView.setClickable(true);
                birthdayInfoView.setFocusable(true);
                birthdayInfoView.setOnClickListener(v -> showBirthdayDetailsDialog(birthday));

                android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
                drawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                drawable.setColor(getResources().getColor(android.R.color.holo_orange_light));
                drawable.setStroke(2, getResources().getColor(android.R.color.holo_orange_dark));
                drawable.setCornerRadius(8 * getResources().getDisplayMetrics().density);
                birthdayInfoView.setBackground(drawable);

                layoutDayInfo.addView(birthdayInfoView);
            }
        }
    }

    private void renderEvents() {
        eventsContainer.removeAllViews();
        for (CalendarEvent event : events) {
            createEventView(event);
        }
        for (Reminder reminder : reminders) {
            createReminderView(reminder);
        }
    }

    private void createEventView(CalendarEvent event) {
        View eventView = LayoutInflater.from(this).inflate(R.layout.event_card, null);
        TextView tvTitle = eventView.findViewById(R.id.tv_event_title);
        TextView tvTime = eventView.findViewById(R.id.tv_event_time);
        TextView tvDescription = eventView.findViewById(R.id.tv_event_description);

        CardView cardView = (CardView) eventView;
        LinearLayout background = (LinearLayout) cardView.getChildAt(0);
        background.setBackgroundColor(event.getColor());

        if (event.isMultiDay()) {
            Calendar selectedDay = Calendar.getInstance();
            selectedDay.setTimeInMillis(selectedDateMillis);
            selectedDay.set(Calendar.HOUR_OF_DAY, 0);
            selectedDay.set(Calendar.MINUTE, 0);
            selectedDay.set(Calendar.SECOND, 0);
            selectedDay.set(Calendar.MILLISECOND, 0);

            Calendar eventStartDay = Calendar.getInstance();
            eventStartDay.setTimeInMillis(event.getStartTimeMillis());
            eventStartDay.set(Calendar.HOUR_OF_DAY, 0);
            eventStartDay.set(Calendar.MINUTE, 0);
            eventStartDay.set(Calendar.SECOND, 0);
            eventStartDay.set(Calendar.MILLISECOND, 0);

            Calendar eventEndDay = Calendar.getInstance();
            eventEndDay.setTimeInMillis(event.getEndTimeMillis());
            eventEndDay.set(Calendar.HOUR_OF_DAY, 0);
            eventEndDay.set(Calendar.MINUTE, 0);
            eventEndDay.set(Calendar.SECOND, 0);
            eventEndDay.set(Calendar.MILLISECOND, 0);

            int dayInSequence = event.getDayInSequence(selectedDateMillis);
            int totalDays = event.getTotalDays();
            tvTitle.setText(event.getTitle() + " (Day " + dayInSequence + "/" + totalDays + ")");

            int startMinuteOfDay, endMinuteOfDay;
            if (selectedDay.equals(eventStartDay)) {
                startMinuteOfDay = event.getStartHour() * 60 + event.getStartMinute();
                endMinuteOfDay = 24 * 60;
                tvTime.setText("From " + String.format("%02d:%02d", event.getStartHour(), event.getStartMinute()) + " (Multi-day start)");
            } else if (selectedDay.equals(eventEndDay)) {
                startMinuteOfDay = 0;
                endMinuteOfDay = event.getEndHour() * 60 + event.getEndMinute();
                tvTime.setText("Until " + String.format("%02d:%02d", event.getEndHour(), event.getEndMinute()) + " (Multi-day end)");
            } else {
                startMinuteOfDay = 0;
                endMinuteOfDay = 24 * 60;
                tvTime.setText("All day (Day " + dayInSequence + "/" + totalDays + ")");
            }

            setEventPosition(eventView, startMinuteOfDay, endMinuteOfDay - startMinuteOfDay);
        } else {
            tvTitle.setText(event.getTitle());
            tvTime.setText(event.getTimeRange());
            int startMinuteOfDay = event.getStartHour() * 60 + event.getStartMinute();
            setEventPosition(eventView, startMinuteOfDay, event.getDurationInMinutes());
        }

        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            tvDescription.setText(event.getDescription());
            tvDescription.setVisibility(View.VISIBLE);
        }

        eventView.setOnClickListener(v -> showEditEventDialog(event));
        eventsContainer.addView(eventView);
    }

    private void createReminderView(Reminder reminder) {
        View eventView = LayoutInflater.from(this).inflate(R.layout.event_card, null);
        TextView tvTitle = eventView.findViewById(R.id.tv_event_title);
        TextView tvTime = eventView.findViewById(R.id.tv_event_time);
        TextView tvDescription = eventView.findViewById(R.id.tv_event_description);

        CardView cardView = (CardView) eventView;
        LinearLayout background = (LinearLayout) cardView.getChildAt(0);
        background.setBackgroundColor(reminder.getColor());

        tvTitle.setText("📝 " + reminder.getTitle());
        tvTime.setText(reminder.getTimeString());

        if (reminder.getDescription() != null && !reminder.getDescription().isEmpty()) {
            tvDescription.setText(reminder.getDescription());
            tvDescription.setVisibility(View.VISIBLE);
        } else {
            tvDescription.setVisibility(View.GONE);
        }

        int startMinuteOfDay = reminder.getStartMinuteOfDay();
        setEventPosition(eventView, startMinuteOfDay, reminder.getDurationInMinutes());

        eventView.setOnClickListener(v -> showReminderDetailsDialog(reminder));
        eventsContainer.addView(eventView);
    }

    private void setEventPosition(View eventView, int startMinuteOfDay, int durationMinutes) {
        int topMargin = (int) dpToPx(startMinuteOfDay);
        int height = Math.max((int) dpToPx(durationMinutes), (int) dpToPx(60));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, height);
        params.topMargin = topMargin;
        params.leftMargin = (int) dpToPx(4);
        params.rightMargin = (int) dpToPx(4);
        eventView.setLayoutParams(params);
    }

    private void showCreateEventDialog() {
        EventCreationHelper helper = new EventCreationHelper(this, selectedDateMillis, this::addNewEvent);
        helper.showCreateEventDialog();
    }

    private void showCreateEventDialogAtTime(int hour, int minute) {
        EventCreationHelper helper = new EventCreationHelper(this, selectedDateMillis, this::addNewEvent);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(selectedDateMillis);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        helper.showCreateEventDialog();
    }

    private void showAddTypeDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_type_selector, null);

        Button btnEvent = dialogView.findViewById(R.id.btn_event);
        Button btnBirthday = dialogView.findViewById(R.id.btn_birthday);
        Button btnReminder = dialogView.findViewById(R.id.btn_reminder);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        applyRoundedButtonBackground(btnEvent, "#d4006d", "#FF69B4", 8);
        applyRoundedButtonBackground(btnBirthday, "#d4006d", "#FF69B4", 8);
        applyRoundedButtonBackground(btnReminder, "#d4006d", "#FF69B4", 8);
        applyRoundedButtonBackground(btnCancel, "#d4006d", "#FF69B4", 8);

        AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog)
                .setView(dialogView)
                .create();

        btnEvent.setOnClickListener(v -> {
            dialog.dismiss();
            showCreateEventDialog();
        });

        btnBirthday.setOnClickListener(v -> {
            dialog.dismiss();
            showCreateBirthdayDialog();
        });

        btnReminder.setOnClickListener(v -> {
            dialog.dismiss();
            showCreateReminderDialog();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showAddTypeDialogAtTime(int hour, int minute) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_type_selector, null);

        Button btnEvent = dialogView.findViewById(R.id.btn_event);
        Button btnBirthday = dialogView.findViewById(R.id.btn_birthday);
        Button btnReminder = dialogView.findViewById(R.id.btn_reminder);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        applyRoundedButtonBackground(btnEvent, "#d4006d", "#FF69B4", 8);
        applyRoundedButtonBackground(btnBirthday, "#d4006d", "#FF69B4", 8);
        applyRoundedButtonBackground(btnReminder, "#d4006d", "#FF69B4", 8);
        applyRoundedButtonBackground(btnCancel, "#d4006d", "#FF69B4", 8);

        AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog)
                .setView(dialogView)
                .create();

        btnEvent.setOnClickListener(v -> {
            dialog.dismiss();
            showCreateEventDialogAtTime(hour, minute);
        });

        btnBirthday.setOnClickListener(v -> {
            dialog.dismiss();
            showCreateBirthdayDialog();
        });

        btnReminder.setOnClickListener(v -> {
            dialog.dismiss();
            showCreateReminderDialog();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void applyRoundedButtonBackground(Button button, String fillColor, String strokeColor, int cornerRadius) {
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        drawable.setColor(android.graphics.Color.parseColor(fillColor));
        drawable.setStroke(2, android.graphics.Color.parseColor(strokeColor));
        drawable.setCornerRadius(cornerRadius * getResources().getDisplayMetrics().density);
        button.setBackground(drawable);
    }

    private void addNewEvent(CalendarEvent event) {
        boolean success = database.createEvent(
            currentUserId,
            event.getTitle(),
            event.getDescription(),
            event.getStartTimeMillis(),
            event.getEndTimeMillis(),
            event.getColor()
        );

        if (success) {
            loadEventsFromDatabase();
            renderEvents();
        } else {
            Toast.makeText(this, "Failed to create event", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditEventDialog(CalendarEvent event) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DarkDialogTheme);
        builder.setTitle(event.getTitle())
                .setMessage("Event: " + event.getTimeRange() + "\n" + event.getDescription())
                .setPositiveButton("Edit", (dialog, which) -> {
                    EventCreationHelper editHelper = new EventCreationHelper(this, event, updatedEvent -> {
                        boolean success = database.updateEvent(
                            currentUserId,
                            event,
                            updatedEvent.getTitle(),
                            updatedEvent.getDescription(),
                            updatedEvent.getStartTimeMillis(),
                            updatedEvent.getEndTimeMillis(),
                            updatedEvent.getColor()
                        );

                        if (success) {
                            loadEventsFromDatabase();
                            renderEvents();
                        } else {
                            Toast.makeText(this, "Failed to update event", Toast.LENGTH_SHORT).show();
                        }
                    });
                    editHelper.showCreateEventDialog();
                })
                .setNegativeButton("Delete", (dialog, which) -> {
                    boolean success = database.deleteEvent(currentUserId, event);
                    if (success) {
                        loadEventsFromDatabase();
                        renderEvents();
                    } else {
                        Toast.makeText(this, "Failed to delete event", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void showCreateBirthdayDialog() {
        BirthdayCreationHelper helper = new BirthdayCreationHelper(this, selectedDateMillis, birthday -> {
            if (database.saveBirthday(birthday, currentUserId)) {
                Toast.makeText(this, "Birthday created successfully!", Toast.LENGTH_SHORT).show();
                loadBirthdaysFromDatabase();
                renderEvents();
                refreshCustomDisplay();

                BirthdayNotificationService.checkAndSendImmediateNotifications(this);
            } else {
                Toast.makeText(this, "Failed to create birthday", Toast.LENGTH_SHORT).show();
            }
        });
        helper.showCreateBirthdayDialog();
    }

    private void showBirthdayDetailsDialog(Birthday birthday) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DarkDialogTheme);
        String message = "Names: " + birthday.getNamesText();
        if (!birthday.getDescription().isEmpty()) {
            message += "\nDescription: " + birthday.getDescription();
        }
        message += "\nYearly recurrence: " + (birthday.isYearlyRecurrence() ? "Yes" : "No");
        message += "\nNotifications: " + (birthday.isNotificationsEnabled() ? "Enabled" : "Disabled");

        builder.setTitle("Birthday Details")
                .setMessage(message)
                .setPositiveButton("Edit", (dialog, which) -> {
                    showEditBirthdayDialog(birthday);
                })
                .setNegativeButton("Delete", (dialog, which) -> {
                    boolean success = database.deleteBirthday(birthday.getId(), currentUserId);
                    if (success) {
                        loadBirthdaysFromDatabase();
                        renderEvents();
                        refreshCustomDisplay();
                        Toast.makeText(this, "Birthday deleted successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to delete birthday", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Close", null)
                .show();
    }

    private void showEditBirthdayDialog(Birthday birthday) {
        BirthdayCreationHelper helper = new BirthdayCreationHelper(this, birthday, updatedBirthday -> {
            if (database.updateBirthday(birthday.getId(), updatedBirthday, currentUserId)) {
                Toast.makeText(this, "Birthday updated successfully!", Toast.LENGTH_SHORT).show();
                loadBirthdaysFromDatabase();
                renderEvents();
                refreshCustomDisplay();
            } else {
                Toast.makeText(this, "Failed to update birthday", Toast.LENGTH_SHORT).show();
            }
        });
        helper.showCreateBirthdayDialog();
    }

    private void showCreateReminderDialog() {
        ReminderCreationHelper helper = new ReminderCreationHelper(this, selectedDateMillis, reminder -> {
            if (database.saveReminder(reminder, currentUserId)) {
                Toast.makeText(this, "Reminder created successfully!", Toast.LENGTH_SHORT).show();
                loadRemindersFromDatabase();
                renderEvents();
                refreshCustomDisplay();

                ReminderNotificationService.scheduleReminderNotifications(this, reminder);

                ReminderNotificationService.checkAndSendImmediateNotifications(this);
            } else {
                Toast.makeText(this, "Failed to create reminder", Toast.LENGTH_SHORT).show();
            }
        });
        helper.showCreateReminderDialog();
    }

    private void showReminderDetailsDialog(Reminder reminder) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DarkDialogTheme);
        String message = "Title: " + reminder.getTitle();
        if (!reminder.getDescription().isEmpty()) {
            message += "\nDescription: " + reminder.getDescription();
        }
        message += "\nTime: " + reminder.getTimeString();
        message += "\nNotifications: " + (reminder.isNotificationsEnabled() ? "Enabled (1 hour before & exact time)" : "Disabled");

        builder.setTitle("Reminder Details")
                .setMessage(message)
                .setPositiveButton("Edit", (dialog, which) -> {
                    showEditReminderDialog(reminder);
                })
                .setNegativeButton("Delete", (dialog, which) -> {
                    ReminderNotificationService.cancelReminderNotifications(this, reminder);

                    boolean success = database.deleteReminder(reminder.getId(), currentUserId);
                    if (success) {
                        loadRemindersFromDatabase();
                        renderEvents();
                        refreshCustomDisplay();
                        Toast.makeText(this, "Reminder deleted successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to delete reminder", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Close", null)
                .show();
    }

    private void showEditReminderDialog(Reminder reminder) {
        ReminderNotificationService.cancelReminderNotifications(this, reminder);

        ReminderCreationHelper helper = new ReminderCreationHelper(this, reminder, updatedReminder -> {
            if (database.updateReminder(reminder.getId(), updatedReminder, currentUserId)) {
                Toast.makeText(this, "Reminder updated successfully!", Toast.LENGTH_SHORT).show();
                loadRemindersFromDatabase();
                renderEvents();
                refreshCustomDisplay();

                ReminderNotificationService.scheduleReminderNotifications(this, updatedReminder);
            } else {
                Toast.makeText(this, "Failed to update reminder", Toast.LENGTH_SHORT).show();
                ReminderNotificationService.scheduleReminderNotifications(this, reminder);
            }
        });
        helper.showCreateReminderDialog();
    }

    private void checkCustomReminders(long selectedDateMillis) {
    }

    private void refreshCustomDisplay() {
        for (int i = layoutDayInfo.getChildCount() - 1; i >= 0; i--) {
            View child = layoutDayInfo.getChildAt(i);
            if (child instanceof TextView && child != tvSelectedDate && child != tvHolidayInfo && child != tvBirthdayInfo) {
                layoutDayInfo.removeView(child);
            }
        }
        checkCustomBirthdays(selectedDateMillis);
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
