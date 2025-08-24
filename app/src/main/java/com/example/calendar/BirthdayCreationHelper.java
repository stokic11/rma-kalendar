package com.example.calendar;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class BirthdayCreationHelper {
    public interface BirthdayCreationCallback {
        void onBirthdayCreated(Birthday birthday);
    }

    private Context context;
    private BirthdayCreationCallback callback;
    private long selectedDateMillis;
    private List<EditText> nameEditTexts;
    private LinearLayout layoutNames;
    private Birthday editingBirthday;
    private boolean isEditMode;

    public BirthdayCreationHelper(Context context, long selectedDateMillis, BirthdayCreationCallback callback) {
        this.context = context;
        this.callback = callback;
        this.selectedDateMillis = selectedDateMillis;
        this.nameEditTexts = new ArrayList<>();
        this.isEditMode = false;
    }

    public BirthdayCreationHelper(Context context, Birthday birthday, BirthdayCreationCallback callback) {
        this.context = context;
        this.callback = callback;
        this.editingBirthday = birthday;
        this.selectedDateMillis = birthday.getDateMillis();
        this.nameEditTexts = new ArrayList<>();
        this.isEditMode = true;
    }

    public void showCreateBirthdayDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_birthday, null);

        layoutNames = dialogView.findViewById(R.id.layout_names);
        EditText etDescription = dialogView.findViewById(R.id.et_birthday_description);
        Switch cbYearlyRecurrence = dialogView.findViewById(R.id.cb_yearly_recurrence);
        Switch cbNotifications = dialogView.findViewById(R.id.cb_notifications);
        Button btnAddName = dialogView.findViewById(R.id.btn_add_name);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnCreateBirthday = dialogView.findViewById(R.id.btn_create_birthday);

        if (isEditMode) {
            btnCreateBirthday.setText("Update Birthday");
        }

        nameEditTexts.clear();
        EditText firstNameInput = dialogView.findViewById(R.id.et_name_0);
        nameEditTexts.add(firstNameInput);

        if (isEditMode && editingBirthday != null) {
            List<String> names = editingBirthday.getNames();
            if (!names.isEmpty()) {
                firstNameInput.setText(names.get(0));

                for (int i = 1; i < names.size(); i++) {
                    addNameField();
                    nameEditTexts.get(i).setText(names.get(i));
                }
            }

            etDescription.setText(editingBirthday.getDescription());
            cbYearlyRecurrence.setChecked(editingBirthday.isYearlyRecurrence());
            cbNotifications.setChecked(editingBirthday.isNotificationsEnabled());
        }

        AlertDialog dialog = builder.setView(dialogView).create();

        dialog.setOnShowListener(dialogInterface -> {
            TextView titleView = dialog.findViewById(android.R.id.title);
            if (titleView != null) {
                titleView.setText(isEditMode ? "Edit Birthday" : "Create Birthday");
                titleView.setGravity(android.view.Gravity.CENTER);
                titleView.setPadding(0, 20, 0, 10);
            }
        });

        btnAddName.setOnClickListener(v -> addNameField());

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnCreateBirthday.setOnClickListener(v -> {
            List<String> names = new ArrayList<>();
            boolean hasValidName = false;

            for (EditText nameEdit : nameEditTexts) {
                String name = nameEdit.getText().toString().trim();
                if (!name.isEmpty()) {
                    names.add(name);
                    hasValidName = true;
                }
            }

            if (!hasValidName) {
                firstNameInput.setError("At least one name is required");
                return;
            }

            String description = etDescription.getText().toString().trim();
            boolean isYearlyRecurrence = cbYearlyRecurrence.isChecked();
            boolean notificationsEnabled = cbNotifications.isChecked();

            Calendar birthdayDate = Calendar.getInstance();
            birthdayDate.setTimeInMillis(selectedDateMillis);
            birthdayDate.set(Calendar.HOUR_OF_DAY, 0);
            birthdayDate.set(Calendar.MINUTE, 0);
            birthdayDate.set(Calendar.SECOND, 0);
            birthdayDate.set(Calendar.MILLISECOND, 0);

            Birthday birthday = new Birthday(names, description, birthdayDate.getTimeInMillis(),
                isYearlyRecurrence, notificationsEnabled);

            callback.onBirthdayCreated(birthday);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void addNameField() {
        EditText newNameEdit = new EditText(context);
        newNameEdit.setHint("Enter person's name");
        newNameEdit.setTextColor(Color.WHITE);
        newNameEdit.setHintTextColor(Color.parseColor("#888888"));
        newNameEdit.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
        newNameEdit.setHeight(dpToPx(48));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dpToPx(8));
        newNameEdit.setLayoutParams(params);

        LinearLayout nameContainer = new LinearLayout(context);
        nameContainer.setOrientation(LinearLayout.HORIZONTAL);
        nameContainer.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
        );
        editParams.setMargins(0, 0, dpToPx(8), dpToPx(8));
        newNameEdit.setLayoutParams(editParams);

        Button removeButton = new Button(context);
        removeButton.setText("×");
        removeButton.setTextColor(Color.WHITE);
        removeButton.setTextSize(18);
        removeButton.setBackground(null);
        removeButton.setLayoutParams(new LinearLayout.LayoutParams(
            dpToPx(40), dpToPx(48)
        ));

        nameContainer.addView(newNameEdit);
        nameContainer.addView(removeButton);

        layoutNames.addView(nameContainer);
        nameEditTexts.add(newNameEdit);

        removeButton.setOnClickListener(v -> {
            layoutNames.removeView(nameContainer);
            nameEditTexts.remove(newNameEdit);
        });
    }

    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
}
