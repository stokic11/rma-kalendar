package com.example.calendar;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import java.util.Calendar;

public class ReminderCreationHelper {
    public interface ReminderCreationCallback {
        void onReminderCreated(Reminder reminder);
    }

    private Context context;
    private ReminderCreationCallback callback;
    private long selectedDateMillis;
    private Reminder editingReminder;
    private boolean isEditMode;

    public ReminderCreationHelper(Context context, long selectedDateMillis, ReminderCreationCallback callback) {
        this.context = context;
        this.callback = callback;
        this.selectedDateMillis = selectedDateMillis;
        this.isEditMode = false;
    }

    public ReminderCreationHelper(Context context, Reminder reminder, ReminderCreationCallback callback) {
        this.context = context;
        this.callback = callback;
        this.editingReminder = reminder;
        this.selectedDateMillis = reminder.getDateMillis();
        this.isEditMode = true;
    }

    public void showCreateReminderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.DarkDialogTheme);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_reminder, null);

        EditText etTitle = dialogView.findViewById(R.id.et_reminder_title);
        EditText etDescription = dialogView.findViewById(R.id.et_reminder_description);
        NumberPicker npHour = dialogView.findViewById(R.id.np_hour);
        NumberPicker npMinute = dialogView.findViewById(R.id.np_minute);
        Switch switchNotifications = dialogView.findViewById(R.id.switch_notifications);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnCreateReminder = dialogView.findViewById(R.id.btn_create_reminder);

        npHour.setMinValue(0);
        npHour.setMaxValue(23);
        npHour.setValue(9);

        npMinute.setMinValue(0);
        npMinute.setMaxValue(59);
        npMinute.setValue(0);

        if (isEditMode) {
            btnCreateReminder.setText("Update Reminder");
        }

        if (isEditMode && editingReminder != null) {
            etTitle.setText(editingReminder.getTitle());
            etDescription.setText(editingReminder.getDescription());
            npHour.setValue(editingReminder.getHour());
            npMinute.setValue(editingReminder.getMinute());
            switchNotifications.setChecked(editingReminder.isNotificationsEnabled());
        }

        AlertDialog dialog = builder.setView(dialogView).create();

        dialog.setOnShowListener(dialogInterface -> {
            TextView titleView = dialog.findViewById(android.R.id.title);
            if (titleView != null) {
                titleView.setText(isEditMode ? "Edit Reminder" : "Create Reminder");
                titleView.setGravity(android.view.Gravity.CENTER);
                titleView.setPadding(0, 20, 0, 10);
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnCreateReminder.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();

            if (title.isEmpty()) {
                etTitle.setError("Title is required");
                return;
            }

            String description = etDescription.getText().toString().trim();
            int hour = npHour.getValue();
            int minute = npMinute.getValue();
            boolean notificationsEnabled = switchNotifications.isChecked();

            Calendar reminderDate = Calendar.getInstance();
            reminderDate.setTimeInMillis(selectedDateMillis);
            reminderDate.set(Calendar.HOUR_OF_DAY, 0);
            reminderDate.set(Calendar.MINUTE, 0);
            reminderDate.set(Calendar.SECOND, 0);
            reminderDate.set(Calendar.MILLISECOND, 0);

            Reminder reminder = new Reminder(title, description, reminderDate.getTimeInMillis(), hour, minute, notificationsEnabled);

            if (isEditMode && editingReminder != null) {
                reminder.setId(editingReminder.getId());
            }

            callback.onReminderCreated(reminder);
            dialog.dismiss();
        });

        dialog.show();
    }
}
