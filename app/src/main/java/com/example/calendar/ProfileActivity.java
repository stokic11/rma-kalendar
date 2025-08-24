package com.example.calendar;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etBirthday;
    private TextView tvUsername;
    private Button btnEditFirstName, btnEditLastName, btnEditBirthday;
    private Button btnChangePassword;
    private ImageButton btnBackArrow;
    private Database database;
    private SharedPreferences sharedPreferences;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        database = new Database(this);
        sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE);

        initViews();
        applyStyling();
        loadUserData();
        setupClickListeners();
    }

    private void initViews() {
        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etBirthday = findViewById(R.id.et_birthday);
        tvUsername = findViewById(R.id.tv_username);

        btnEditFirstName = findViewById(R.id.btn_edit_first_name);
        btnEditLastName = findViewById(R.id.btn_edit_last_name);
        btnEditBirthday = findViewById(R.id.btn_edit_birthday);
        btnChangePassword = findViewById(R.id.btn_change_password);
        btnBackArrow = findViewById(R.id.btn_back_arrow);
    }

    private void applyStyling() {
        applyInputLineBackground(etFirstName);
        applyInputLineBackground(etLastName);
        applyInputLineBackground(etBirthday);
    }

    private void applyInputLineBackground(EditText editText) {
        GradientDrawable transparentBg = new GradientDrawable();
        transparentBg.setColor(android.graphics.Color.TRANSPARENT);

        GradientDrawable bottomLine = new GradientDrawable();
        bottomLine.setColor(android.graphics.Color.parseColor("#404040"));
        bottomLine.setSize(-1, (int) (2 * getResources().getDisplayMetrics().density));

        LayerDrawable layerDrawable = new LayerDrawable(new android.graphics.drawable.Drawable[]{
                transparentBg, bottomLine
        });
        layerDrawable.setLayerGravity(1, android.view.Gravity.BOTTOM);
        layerDrawable.setLayerHeight(1, (int) (2 * getResources().getDisplayMetrics().density));

        editText.setBackground(layerDrawable);
    }

    private void loadUserData() {
        String username = sharedPreferences.getString("username", "");
        currentUser = database.getUserDetails(username);

        if (currentUser != null) {
            etFirstName.setText(currentUser.getFirstName());
            etLastName.setText(currentUser.getLastName());
            etBirthday.setText(currentUser.getBirthday());
            tvUsername.setText(currentUser.getUsername());
        }
    }

    private void setupClickListeners() {
        btnEditFirstName.setOnClickListener(v -> editField("First Name", etFirstName, "first_name"));
        btnEditLastName.setOnClickListener(v -> editField("Last Name", etLastName, "last_name"));
        btnEditBirthday.setOnClickListener(v -> editBirthday());
        btnChangePassword.setOnClickListener(v -> changePassword());
        btnBackArrow.setOnClickListener(v -> finish());
    }

    private void editField(String fieldName, EditText editText, String fieldType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        layout.setBackgroundColor(android.graphics.Color.BLACK);

        android.widget.TextView titleView = new android.widget.TextView(this);
        String dialogTitle = "Change your " + fieldName.toLowerCase();
        titleView.setText(dialogTitle);
        titleView.setTextColor(android.graphics.Color.WHITE);
        titleView.setTextSize(20);
        titleView.setPadding(0, 0, 0, 30);
        titleView.setGravity(android.view.Gravity.CENTER);
        layout.addView(titleView);

        EditText input = new EditText(this);
        input.setText(editText.getText().toString());

        String placeholder = "";
        switch (fieldType) {
            case "first_name":
                placeholder = "Enter your new first name";
                break;
            case "last_name":
                placeholder = "Enter your new last name";
                break;
        }
        input.setHint(placeholder);

        input.setTextColor(android.graphics.Color.WHITE);
        input.setHintTextColor(android.graphics.Color.parseColor("#AAAAAA"));
        input.setBackgroundColor(android.graphics.Color.parseColor("#333333"));
        input.setPadding(24, 16, 24, 16);
        input.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.addView(input);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newValue = input.getText().toString().trim();
            if (!newValue.isEmpty()) {
                verifyPasswordAndUpdate(fieldType, newValue, editText);
            } else {
                Toast.makeText(this, fieldName + " cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();

        try {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.black);
            dialog.show();

            if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.WHITE);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(android.graphics.Color.parseColor("#d4006d"));
            }
            if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.WHITE);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(android.graphics.Color.parseColor("#666666"));
            }
        } catch (Exception e) {
            dialog.show();
        }
    }

    private void editBirthday() {
        Calendar calendar = Calendar.getInstance();

        if (!etBirthday.getText().toString().isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                calendar.setTime(sdf.parse(etBirthday.getText().toString()));
            } catch (Exception e) {
            }
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    String newBirthday = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    verifyPasswordAndUpdate("birthday", newBirthday, etBirthday);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    private void verifyPasswordAndUpdate(String fieldType, String newValue, EditText editText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        layout.setBackgroundColor(android.graphics.Color.BLACK);

        android.widget.TextView titleView = new android.widget.TextView(this);
        titleView.setText("Verify Password");
        titleView.setTextColor(android.graphics.Color.WHITE);
        titleView.setTextSize(20);
        titleView.setPadding(0, 0, 0, 20);
        titleView.setGravity(android.view.Gravity.CENTER);
        layout.addView(titleView);

        android.widget.TextView messageView = new android.widget.TextView(this);
        messageView.setText("Please enter your current password to make changes:");
        messageView.setTextColor(android.graphics.Color.WHITE);
        messageView.setTextSize(14);
        messageView.setPadding(0, 0, 0, 20);
        messageView.setGravity(android.view.Gravity.CENTER);
        layout.addView(messageView);

        EditText passwordInput = new EditText(this);
        passwordInput.setHint("Enter your current password");
        passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setTextColor(android.graphics.Color.WHITE);
        passwordInput.setHintTextColor(android.graphics.Color.parseColor("#AAAAAA"));
        passwordInput.setBackgroundColor(android.graphics.Color.parseColor("#333333"));
        passwordInput.setPadding(24, 16, 24, 16);
        passwordInput.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.addView(passwordInput);

        builder.setView(layout);

        builder.setPositiveButton("Verify", (dialog, which) -> {
            String password = passwordInput.getText().toString();
            if (database.loginUser(currentUser.getUsername(), password)) {
                updateUserField(fieldType, newValue, editText);
            } else {
                Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();

        try {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.black);
            dialog.show();

            if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.WHITE);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(android.graphics.Color.parseColor("#d4006d"));
            }
            if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.WHITE);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(android.graphics.Color.parseColor("#666666"));
            }
        } catch (Exception e) {
            dialog.show();
        }
    }

    private void updateUserField(String fieldType, String newValue, EditText editText) {
        boolean success = false;

        switch (fieldType) {
            case "first_name":
                success = database.updateUser(currentUser.getId(), newValue, currentUser.getLastName(),
                        currentUser.getBirthday(), currentUser.getUsername());
                break;
            case "last_name":
                success = database.updateUser(currentUser.getId(), currentUser.getFirstName(), newValue,
                        currentUser.getBirthday(), currentUser.getUsername());
                break;
            case "birthday":
                success = database.updateUser(currentUser.getId(), currentUser.getFirstName(),
                        currentUser.getLastName(), newValue, currentUser.getUsername());
                break;
        }

        if (success) {
            editText.setText(newValue);
            loadUserData();
            Toast.makeText(this, "Updated successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show();
        }
    }

    private void changePassword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        layout.setBackgroundColor(android.graphics.Color.BLACK);

        android.widget.TextView titleView = new android.widget.TextView(this);
        titleView.setText("Change your password");
        titleView.setTextColor(android.graphics.Color.WHITE);
        titleView.setTextSize(20);
        titleView.setPadding(0, 0, 0, 30);
        titleView.setGravity(android.view.Gravity.CENTER);
        layout.addView(titleView);

        EditText oldPasswordInput = new EditText(this);
        oldPasswordInput.setHint("Enter your current password");
        oldPasswordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        oldPasswordInput.setTextColor(android.graphics.Color.WHITE);
        oldPasswordInput.setHintTextColor(android.graphics.Color.parseColor("#AAAAAA"));
        oldPasswordInput.setBackgroundColor(android.graphics.Color.parseColor("#333333"));
        oldPasswordInput.setPadding(24, 16, 24, 16);
        oldPasswordInput.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        ((android.widget.LinearLayout.LayoutParams)oldPasswordInput.getLayoutParams()).setMargins(0, 0, 0, 16);
        layout.addView(oldPasswordInput);

        EditText newPasswordInput = new EditText(this);
        newPasswordInput.setHint("Enter your new password");
        newPasswordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        newPasswordInput.setTextColor(android.graphics.Color.WHITE);
        newPasswordInput.setHintTextColor(android.graphics.Color.parseColor("#AAAAAA"));
        newPasswordInput.setBackgroundColor(android.graphics.Color.parseColor("#333333"));
        newPasswordInput.setPadding(24, 16, 24, 16);
        newPasswordInput.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        ((android.widget.LinearLayout.LayoutParams)newPasswordInput.getLayoutParams()).setMargins(0, 0, 0, 16);
        layout.addView(newPasswordInput);

        EditText confirmPasswordInput = new EditText(this);
        confirmPasswordInput.setHint("Confirm your new password");
        confirmPasswordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        confirmPasswordInput.setTextColor(android.graphics.Color.WHITE);
        confirmPasswordInput.setHintTextColor(android.graphics.Color.parseColor("#AAAAAA"));
        confirmPasswordInput.setBackgroundColor(android.graphics.Color.parseColor("#333333"));
        confirmPasswordInput.setPadding(24, 16, 24, 16);
        confirmPasswordInput.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.addView(confirmPasswordInput);

        builder.setView(layout);

        builder.setPositiveButton("Change", (dialog, which) -> {
            String oldPassword = oldPasswordInput.getText().toString();
            String newPassword = newPasswordInput.getText().toString();
            String confirmPassword = confirmPasswordInput.getText().toString();

            if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!database.loginUser(currentUser.getUsername(), oldPassword)) {
                Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPassword.length() < 6) {
                Toast.makeText(this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            if (database.updateUserPassword(currentUser.getId(), newPassword)) {
                Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to change password", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.black);
        dialog.show();

        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.WHITE);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(android.graphics.Color.parseColor("#d4006d"));
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.WHITE);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(android.graphics.Color.parseColor("#666666"));
        }
    }
}
