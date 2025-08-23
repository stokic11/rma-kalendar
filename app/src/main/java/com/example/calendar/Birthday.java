package com.example.calendar;

import android.graphics.Color;
import java.util.ArrayList;
import java.util.List;

public class Birthday {
    private String id;
    private List<String> names;
    private String description;
    private long dateMillis;
    private boolean isYearlyRecurrence;
    private boolean notificationsEnabled;
    private int color;

    public Birthday(List<String> names, String description, long dateMillis, boolean isYearlyRecurrence, boolean notificationsEnabled) {
        this.id = String.valueOf(System.currentTimeMillis());
        this.names = names != null ? new ArrayList<>(names) : new ArrayList<>();
        this.description = description != null ? description : "";
        this.dateMillis = dateMillis;
        this.isYearlyRecurrence = isYearlyRecurrence;
        this.notificationsEnabled = notificationsEnabled;
        this.color = Color.parseColor("#FF69B4");
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public List<String> getNames() { return names; }
    public void setNames(List<String> names) { this.names = names; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getDateMillis() { return dateMillis; }
    public void setDateMillis(long dateMillis) { this.dateMillis = dateMillis; }

    public boolean isYearlyRecurrence() { return isYearlyRecurrence; }
    public void setYearlyRecurrence(boolean yearlyRecurrence) { isYearlyRecurrence = yearlyRecurrence; }

    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }

    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }

    public String getNamesText() {
        if (names.isEmpty()) return "";
        if (names.size() == 1) return names.get(0);
        if (names.size() == 2) return names.get(0) + " and " + names.get(1);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.size() - 1; i++) {
            sb.append(names.get(i));
            if (i < names.size() - 2) sb.append(", ");
        }
        sb.append(" and ").append(names.get(names.size() - 1));
        return sb.toString();
    }

    public void addName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            names.add(name.trim());
        }
    }

    public void removeName(int index) {
        if (index >= 0 && index < names.size()) {
            names.remove(index);
        }
    }
}
