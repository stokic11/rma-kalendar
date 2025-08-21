package com.example.calendar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private Button btnLogout;
    private SharedPreferences sharedPreferences;
    private Database database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_main);

            sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE);
            database = new Database(this);

            if (!sharedPreferences.getBoolean("is_logged_in", false)) {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }

            initViews();
            loadUserData();

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
        btnLogout = findViewById(R.id.btn_logout);

        btnLogout.setOnClickListener(v -> logout());
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

    private void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}