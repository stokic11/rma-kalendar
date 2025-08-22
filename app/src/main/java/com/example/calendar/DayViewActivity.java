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
    private LinearLayout layoutDayInfo;
    private Button btnAddEvent;
    private FrameLayout eventsContainer;
    private HolidayService holidayService;
    private Database database;
    private SharedPreferences sharedPreferences;
    private List<CalendarEvent> events;
    private long selectedDateMillis;
    private int currentUserId;
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

        String username = sharedPreferences.getString("username", "");
        User currentUser = database.getUserByUsername(username);
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
        loadEventsFromDatabase();
        renderEvents();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvSelectedDate = findViewById(R.id.tv_selected_date);
        tvHolidayInfo = findViewById(R.id.tv_holiday_info);
        layoutDayInfo = findViewById(R.id.layout_day_info);
        btnAddEvent = findViewById(R.id.btn_add_event);
        eventsContainer = findViewById(R.id.events_container);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnAddEvent.setOnClickListener(v -> showCreateEventDialog());
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
                        showCreateEventDialogAtTime(hour, minute);
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

    private void checkHolidays(long selectedDateMillis) {
        holidayService.checkHoliday(selectedDateMillis, new HolidayService.HolidayCallback() {
            @Override
            public void onHolidayFound(String holidayName) {
                runOnUiThread(() -> {
                    tvHolidayInfo.setText(holidayName);
                    tvHolidayInfo.setVisibility(View.VISIBLE);
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

    private void renderEvents() {
        eventsContainer.removeAllViews();
        for (CalendarEvent event : events) {
            createEventView(event);
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

    private void setEventPosition(View eventView, int startMinuteOfDay, int durationMinutes) {
        int topMargin = (int) dpToPx(startMinuteOfDay);
        int height = Math.max((int) dpToPx(durationMinutes), (int) dpToPx(30));
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
            Toast.makeText(this, "Event created: " + event.getTitle(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to create event", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditEventDialog(CalendarEvent event) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
                            Toast.makeText(this, "Event updated: " + updatedEvent.getTitle(), Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to delete event", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
