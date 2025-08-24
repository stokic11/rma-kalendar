package com.example.calendar;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.EditText;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EventCreationHelper {
    public interface EventCreationCallback {
        void onEventCreated(CalendarEvent event);
    }

    private Context context;
    private EventCreationCallback callback;
    private Calendar startTime;
    private Calendar endTime;
    private Calendar endDate;
    private int selectedColor = Color.parseColor("#4285F4");
    private long selectedDateMillis;
    private boolean isMultiDay = false;
    private boolean isEditMode = false;
    private CalendarEvent editingEvent = null;

    public EventCreationHelper(Context context, long selectedDateMillis, EventCreationCallback callback) {
        this.context = context;
        this.callback = callback;
        this.selectedDateMillis = selectedDateMillis;

        startTime = Calendar.getInstance();
        startTime.setTimeInMillis(selectedDateMillis);
        startTime.set(Calendar.HOUR_OF_DAY, 9);
        startTime.set(Calendar.MINUTE, 0);
        startTime.set(Calendar.SECOND, 0);

        endTime = Calendar.getInstance();
        endTime.setTimeInMillis(startTime.getTimeInMillis());
        endTime.add(Calendar.HOUR, 1);

        endDate = Calendar.getInstance();
        endDate.setTimeInMillis(selectedDateMillis);
    }

    private boolean sameDay(Calendar start, Calendar end) {
        Calendar startDay = Calendar.getInstance();
        startDay.setTimeInMillis(start.getTimeInMillis());
        startDay.set(Calendar.HOUR_OF_DAY, 0);
        startDay.set(Calendar.MINUTE, 0);
        startDay.set(Calendar.SECOND, 0);
        startDay.set(Calendar.MILLISECOND, 0);

        Calendar endDay = Calendar.getInstance();
        endDay.setTimeInMillis(end.getTimeInMillis());
        endDay.set(Calendar.HOUR_OF_DAY, 0);
        endDay.set(Calendar.MINUTE, 0);
        endDay.set(Calendar.SECOND, 0);
        endDay.set(Calendar.MILLISECOND, 0);

        return startDay.equals(endDay);
    }

    public EventCreationHelper(Context context, CalendarEvent event, EventCreationCallback callback) {
        this.context = context;
        this.callback = callback;
        this.editingEvent = event;
        this.isEditMode = true;
        this.selectedColor = event.getColor();

        startTime = Calendar.getInstance();
        startTime.setTimeInMillis(event.getStartTimeMillis());

        endTime = Calendar.getInstance();
        endTime.setTimeInMillis(event.getEndTimeMillis());

        isMultiDay = !sameDay(startTime, endTime);

        if (isMultiDay) {
            endDate = Calendar.getInstance();
            endDate.setTimeInMillis(event.getEndTimeMillis());

            Calendar tempEndTime = Calendar.getInstance();
            tempEndTime.setTimeInMillis(event.getEndTimeMillis());

            endTime = Calendar.getInstance();
            endTime.setTimeInMillis(startTime.getTimeInMillis());
            endTime.set(Calendar.HOUR_OF_DAY, tempEndTime.get(Calendar.HOUR_OF_DAY));
            endTime.set(Calendar.MINUTE, tempEndTime.get(Calendar.MINUTE));
        } else {
            endDate = Calendar.getInstance();
            endDate.setTimeInMillis(startTime.getTimeInMillis());
        }

        this.selectedDateMillis = event.getStartTimeMillis();
    }

    public void showCreateEventDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.DarkDialogTheme);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_event, null);

        EditText etTitle = dialogView.findViewById(R.id.et_event_title);
        EditText etDescription = dialogView.findViewById(R.id.et_event_description);
        Button btnStartTime = dialogView.findViewById(R.id.btn_start_time);
        Switch cbMultiDay = dialogView.findViewById(R.id.cb_multi_day);
        Button btnEndTime = dialogView.findViewById(R.id.btn_end_time);
        Button btnEndDate = dialogView.findViewById(R.id.btn_end_date);
        TextView tvDuration = dialogView.findViewById(R.id.tv_duration);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnCreateEvent = dialogView.findViewById(R.id.btn_create_event);

        View colorBlue = dialogView.findViewById(R.id.color_blue);
        View colorGreen = dialogView.findViewById(R.id.color_green);
        View colorRed = dialogView.findViewById(R.id.color_red);
        View colorOrange = dialogView.findViewById(R.id.color_orange);
        View colorPurple = dialogView.findViewById(R.id.color_purple);

        AlertDialog dialog = builder.setView(dialogView).create();

        dialog.setOnShowListener(dialogInterface -> {
            TextView titleView = dialog.findViewById(android.R.id.title);
            if (titleView != null) {
                titleView.setText(isEditMode ? "Edit Event" : "Create Event");
                titleView.setGravity(android.view.Gravity.CENTER);
                titleView.setPadding(0, 20, 0, 10);
            }
        });

        updateTimeDisplays(btnStartTime, btnEndTime, btnEndDate, tvDuration);

        cbMultiDay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isMultiDay = isChecked;
            btnEndDate.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            updateTimeDisplays(btnStartTime, btnEndTime, btnEndDate, tvDuration);
        });

        btnStartTime.setOnClickListener(v -> showTimePicker(true, () ->
            updateTimeDisplays(btnStartTime, btnEndTime, btnEndDate, tvDuration)));

        btnEndTime.setOnClickListener(v -> showTimePicker(false, () ->
            updateTimeDisplays(btnStartTime, btnEndTime, btnEndDate, tvDuration)));

        btnEndDate.setOnClickListener(v -> showDatePicker(() ->
            updateTimeDisplays(btnStartTime, btnEndTime, btnEndDate, tvDuration)));

        colorBlue.setOnClickListener(v -> selectColor(Color.parseColor("#4285F4"), colorBlue, colorGreen, colorRed, colorOrange, colorPurple));
        colorGreen.setOnClickListener(v -> selectColor(Color.parseColor("#34A853"), colorBlue, colorGreen, colorRed, colorOrange, colorPurple));
        colorRed.setOnClickListener(v -> selectColor(Color.parseColor("#EA4335"), colorBlue, colorGreen, colorRed, colorOrange, colorPurple));
        colorOrange.setOnClickListener(v -> selectColor(Color.parseColor("#FF9800"), colorBlue, colorGreen, colorRed, colorOrange, colorPurple));
        colorPurple.setOnClickListener(v -> selectColor(Color.parseColor("#9C27B0"), colorBlue, colorGreen, colorRed, colorOrange, colorPurple));

        selectColor(selectedColor, colorBlue, colorGreen, colorRed, colorOrange, colorPurple);

        if (isEditMode) {
            btnCreateEvent.setText("Update Event");
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnCreateEvent.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (title.isEmpty()) {
                etTitle.setError("Event title is required");
                return;
            }

            Calendar finalEndTime = Calendar.getInstance();
            if (isMultiDay) {
                finalEndTime.set(endDate.get(Calendar.YEAR),
                               endDate.get(Calendar.MONTH),
                               endDate.get(Calendar.DAY_OF_MONTH),
                               endTime.get(Calendar.HOUR_OF_DAY),
                               endTime.get(Calendar.MINUTE), 0);
            } else {
                finalEndTime.setTimeInMillis(endTime.getTimeInMillis());
            }

            if (finalEndTime.getTimeInMillis() <= startTime.getTimeInMillis()) {
                finalEndTime.setTimeInMillis(startTime.getTimeInMillis());
                finalEndTime.add(Calendar.HOUR, 1);
            }

            CalendarEvent event = new CalendarEvent(title, description,
                startTime.getTimeInMillis(), finalEndTime.getTimeInMillis(), selectedColor);

            callback.onEventCreated(event);
            dialog.dismiss();
        });

        if (isEditMode && editingEvent != null) {
            etTitle.setText(editingEvent.getTitle());
            etDescription.setText(editingEvent.getDescription());
            startTime.setTimeInMillis(editingEvent.getStartTimeMillis());
            endTime.setTimeInMillis(editingEvent.getEndTimeMillis());
            isMultiDay = !sameDay(startTime, endTime);
            cbMultiDay.setChecked(isMultiDay);
            endDate.setTimeInMillis(isMultiDay ? endTime.getTimeInMillis() : startTime.getTimeInMillis());
            selectColor(editingEvent.getColor(), colorBlue, colorGreen, colorRed, colorOrange, colorPurple);
            updateTimeDisplays(btnStartTime, btnEndTime, btnEndDate, tvDuration);
        }

        dialog.show();
    }

    private void showDatePicker(Runnable onDateSet) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            context,
            (view, year, month, dayOfMonth) -> {
                endDate.set(year, month, dayOfMonth);
                onDateSet.run();
            },
            endDate.get(Calendar.YEAR),
            endDate.get(Calendar.MONTH),
            endDate.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.getDatePicker().setMinDate(startTime.getTimeInMillis());
        datePickerDialog.show();
    }

    private void showTimePicker(boolean isStartTime, Runnable onTimeSet) {
        Calendar timeToEdit = isStartTime ? startTime : endTime;

        AlertDialog.Builder builder = new AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog);
        View timePickerView = LayoutInflater.from(context).inflate(R.layout.custom_time_picker, null);

        EditText etHour = timePickerView.findViewById(R.id.et_hour);
        EditText etMinute = timePickerView.findViewById(R.id.et_minute);
        TextView btnHourUp = timePickerView.findViewById(R.id.btn_hour_up);
        TextView btnHourDown = timePickerView.findViewById(R.id.btn_hour_down);
        TextView btnMinuteUp = timePickerView.findViewById(R.id.btn_minute_up);
        TextView btnMinuteDown = timePickerView.findViewById(R.id.btn_minute_down);

        etHour.setText(String.format("%02d", timeToEdit.get(Calendar.HOUR_OF_DAY)));
        etMinute.setText(String.format("%02d", timeToEdit.get(Calendar.MINUTE)));

        AlertDialog dialog = builder
            .setTitle(isStartTime ? "Set Start Time" : "Set End Time")
            .setView(timePickerView)
            .setPositiveButton("OK", (d, which) -> {
                try {
                    int hour = Math.max(0, Math.min(23, Integer.parseInt(etHour.getText().toString())));
                    int minute = Math.max(0, Math.min(59, Integer.parseInt(etMinute.getText().toString())));

                    timeToEdit.set(Calendar.HOUR_OF_DAY, hour);
                    timeToEdit.set(Calendar.MINUTE, minute);

                    if (isStartTime && endTime.getTimeInMillis() <= startTime.getTimeInMillis()) {
                        endTime.setTimeInMillis(startTime.getTimeInMillis());
                        endTime.add(Calendar.HOUR, 1);
                    }

                    onTimeSet.run();
                } catch (NumberFormatException e) {
                    etHour.setText(String.format("%02d", timeToEdit.get(Calendar.HOUR_OF_DAY)));
                    etMinute.setText(String.format("%02d", timeToEdit.get(Calendar.MINUTE)));
                }
            })
            .setNegativeButton("Cancel", null)
            .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.black);
            dialog.getWindow().getAttributes().gravity = android.view.Gravity.CENTER;
        }

        btnHourUp.setOnClickListener(v -> {
            try {
                int hour = (Integer.parseInt(etHour.getText().toString()) + 1) % 24;
                etHour.setText(String.format("%02d", hour));
            } catch (NumberFormatException e) {
                etHour.setText("00");
            }
        });

        btnHourDown.setOnClickListener(v -> {
            try {
                int hour = (Integer.parseInt(etHour.getText().toString()) - 1 + 24) % 24;
                etHour.setText(String.format("%02d", hour));
            } catch (NumberFormatException e) {
                etHour.setText("23");
            }
        });

        btnMinuteUp.setOnClickListener(v -> {
            try {
                int minute = (Integer.parseInt(etMinute.getText().toString()) + 15) % 60;
                etMinute.setText(String.format("%02d", minute));
            } catch (NumberFormatException e) {
                etMinute.setText("00");
            }
        });

        btnMinuteDown.setOnClickListener(v -> {
            try {
                int minute = (Integer.parseInt(etMinute.getText().toString()) - 15 + 60) % 60;
                etMinute.setText(String.format("%02d", minute));
            } catch (NumberFormatException e) {
                etMinute.setText("45");
            }
        });

        setupTimeValidation(etHour, etMinute, timeToEdit);
        dialog.show();
    }

    private void setupTimeValidation(EditText etHour, EditText etMinute, Calendar timeToEdit) {
        etHour.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (s.length() > 0) {
                    try {
                        int hour = Integer.parseInt(s.toString());
                        if (hour > 23) {
                            etHour.setText("23");
                            etHour.setSelection(2);
                        }
                    } catch (NumberFormatException e) {
                        String cleaned = s.toString().replaceAll("[^0-9]", "");
                        if (!cleaned.equals(s.toString())) {
                            etHour.setText(cleaned);
                            etHour.setSelection(cleaned.length());
                        }
                    }
                }
            }
        });

        etMinute.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (s.length() > 0) {
                    try {
                        int minute = Integer.parseInt(s.toString());
                        if (minute > 59) {
                            etMinute.setText("59");
                            etMinute.setSelection(2);
                        }
                    } catch (NumberFormatException e) {
                        String cleaned = s.toString().replaceAll("[^0-9]", "");
                        if (!cleaned.equals(s.toString())) {
                            etMinute.setText(cleaned);
                            etMinute.setSelection(cleaned.length());
                        }
                    }
                }
            }
        });

        etHour.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                try {
                    int hour = Math.max(0, Math.min(23, Integer.parseInt(etHour.getText().toString())));
                    etHour.setText(String.format("%02d", hour));
                } catch (NumberFormatException e) {
                    etHour.setText(String.format("%02d", timeToEdit.get(Calendar.HOUR_OF_DAY)));
                }
            }
        });

        etMinute.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                try {
                    int minute = Math.max(0, Math.min(59, Integer.parseInt(etMinute.getText().toString())));
                    etMinute.setText(String.format("%02d", minute));
                } catch (NumberFormatException e) {
                    etMinute.setText(String.format("%02d", timeToEdit.get(Calendar.MINUTE)));
                }
            }
        });

        etHour.setFocusableInTouchMode(true);
        etMinute.setFocusableInTouchMode(true);
    }

    private void selectColor(int color, View... colorViews) {
        selectedColor = color;
        for (View view : colorViews) {
            view.setAlpha(0.6f);
            view.setScaleX(1.0f);
            view.setScaleY(1.0f);
        }
        View selectedView = getColorView(color, colorViews);
        if (selectedView != null) {
            selectedView.setAlpha(1.0f);
            selectedView.setScaleX(1.2f);
            selectedView.setScaleY(1.2f);
        }
    }

    private View getColorView(int color, View... colorViews) {
        if (color == Color.parseColor("#4285F4")) return colorViews[0];
        if (color == Color.parseColor("#34A853")) return colorViews[1];
        if (color == Color.parseColor("#EA4335")) return colorViews[2];
        if (color == Color.parseColor("#FF9800")) return colorViews[3];
        if (color == Color.parseColor("#9C27B0")) return colorViews[4];
        return colorViews[0];
    }

    private void updateTimeDisplays(Button btnStartTime, Button btnEndTime, Button btnEndDate, TextView tvDuration) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());

        btnStartTime.setText(timeFormat.format(startTime.getTime()));
        btnEndTime.setText(timeFormat.format(endTime.getTime()));
        btnEndDate.setText(isMultiDay ? dateFormat.format(endDate.getTime()) : "Same day");

        Calendar finalEndTime = Calendar.getInstance();
        if (isMultiDay) {
            finalEndTime.set(endDate.get(Calendar.YEAR),
                           endDate.get(Calendar.MONTH),
                           endDate.get(Calendar.DAY_OF_MONTH),
                           endTime.get(Calendar.HOUR_OF_DAY),
                           endTime.get(Calendar.MINUTE), 0);
        } else {
            finalEndTime.setTimeInMillis(endTime.getTimeInMillis());
        }

        long durationMillis = finalEndTime.getTimeInMillis() - startTime.getTimeInMillis();
        if (durationMillis <= 0) {
            tvDuration.setText("Duration: Invalid");
            return;
        }

        long totalMinutes = durationMillis / (1000 * 60);
        long days = totalMinutes / (24 * 60);
        long hours = (totalMinutes % (24 * 60)) / 60;
        long minutes = totalMinutes % 60;

        String durationText = "Duration: ";
        if (days > 0) durationText += days + "d ";
        if (hours > 0) durationText += hours + "h ";
        if (minutes > 0 || (days == 0 && hours == 0)) durationText += minutes + "m";

        tvDuration.setText(durationText.trim());
    }
}
