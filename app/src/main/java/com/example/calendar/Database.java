package com.example.calendar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class Database extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "calendar_db";
    private static final String TABLE_USERS = "users";
    private static final String ID = "id";
    private static final String FIRST_NAME = "first_name";
    private static final String LAST_NAME = "last_name";
    private static final String BIRTHDAY = "birthday";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    private static final String TABLE_EVENTS = "events";
    private static final String EVENT_ID = "event_id";
    private static final String USER_ID = "user_id";
    private static final String EVENT_TITLE = "title";
    private static final String EVENT_DESCRIPTION = "description";
    private static final String EVENT_START_TIME = "start_time";
    private static final String EVENT_END_TIME = "end_time";
    private static final String EVENT_COLOR = "color";

    public Database(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USERS + " (" +
                ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                FIRST_NAME + " TEXT NOT NULL, " +
                LAST_NAME + " TEXT NOT NULL, " +
                BIRTHDAY + " TEXT NOT NULL, " +
                USERNAME + " TEXT UNIQUE NOT NULL, " +
                PASSWORD + " TEXT NOT NULL)");

        db.execSQL("CREATE TABLE " + TABLE_EVENTS + " (" +
                EVENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                USER_ID + " INTEGER NOT NULL, " +
                EVENT_TITLE + " TEXT NOT NULL, " +
                EVENT_DESCRIPTION + " TEXT, " +
                EVENT_START_TIME + " TEXT NOT NULL, " +
                EVENT_END_TIME + " TEXT NOT NULL, " +
                EVENT_COLOR + " INTEGER NOT NULL, " +
                "FOREIGN KEY(" + USER_ID + ") REFERENCES " + TABLE_USERS + "(" + ID + "))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return password;
        }
    }

    private String millisToDateTimeString(long millis) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(millis));
    }

    private long dateTimeStringToMillis(String dateTimeString) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            return sdf.parse(dateTimeString).getTime();
        } catch (Exception e) {
            return 0;
        }
    }

    private User createUserFromCursor(Cursor cursor) {
        User user = new User();
        user.setId(cursor.getInt(cursor.getColumnIndexOrThrow(ID)));
        user.setFirstName(cursor.getString(cursor.getColumnIndexOrThrow(FIRST_NAME)));
        user.setLastName(cursor.getString(cursor.getColumnIndexOrThrow(LAST_NAME)));
        user.setBirthday(cursor.getString(cursor.getColumnIndexOrThrow(BIRTHDAY)));
        user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(USERNAME)));
        return user;
    }

    private CalendarEvent createEventFromCursor(Cursor cursor) {
        String startTimeStr = cursor.getString(cursor.getColumnIndexOrThrow(EVENT_START_TIME));
        String endTimeStr = cursor.getString(cursor.getColumnIndexOrThrow(EVENT_END_TIME));
        long startTime = dateTimeStringToMillis(startTimeStr);
        long endTime = dateTimeStringToMillis(endTimeStr);

        return new CalendarEvent(
                cursor.getString(cursor.getColumnIndexOrThrow(EVENT_TITLE)),
                cursor.getString(cursor.getColumnIndexOrThrow(EVENT_DESCRIPTION)),
                startTime,
                endTime,
                cursor.getInt(cursor.getColumnIndexOrThrow(EVENT_COLOR))
        );
    }

    public boolean createUser(String firstName, String lastName, String birthday, String username, String password) {
        if (usernameExists(username)) return false;
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(FIRST_NAME, firstName);
            values.put(LAST_NAME, lastName);
            values.put(BIRTHDAY, birthday);
            values.put(USERNAME, username);
            values.put(PASSWORD, hashPassword(password));
            return db.insert(TABLE_USERS, null, values) != -1;
        } catch (Exception e) {
            return false;
        }
    }

    public List<User> getAllUsers() {
        List<User> userList = new ArrayList<>();
        try (SQLiteDatabase db = this.getReadableDatabase();
             Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " ORDER BY " + FIRST_NAME, null)) {
            if (cursor.moveToFirst()) {
                do {
                    userList.add(createUserFromCursor(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception ignored) {}
        return userList;
    }

    public User getUserById(int userId) {
        try (SQLiteDatabase db = this.getReadableDatabase();
             Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + ID + " = ?",
                     new String[]{String.valueOf(userId)})) {
            return cursor.moveToFirst() ? createUserFromCursor(cursor) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public User getUserByUsername(String username) {
        try (SQLiteDatabase db = this.getReadableDatabase();
             Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + USERNAME + " = ?",
                     new String[]{username})) {
            return cursor.moveToFirst() ? createUserFromCursor(cursor) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean updateUser(int userId, String firstName, String lastName, String birthday, String username) {
        User currentUser = getUserById(userId);
        if (currentUser != null && !username.equals(currentUser.getUsername()) && usernameExists(username)) {
            return false;
        }
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(FIRST_NAME, firstName);
            values.put(LAST_NAME, lastName);
            values.put(BIRTHDAY, birthday);
            values.put(USERNAME, username);
            return db.update(TABLE_USERS, values, ID + " = ?", new String[]{String.valueOf(userId)}) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean updateUserPassword(int userId, String newPassword) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(PASSWORD, hashPassword(newPassword));
            return db.update(TABLE_USERS, values, ID + " = ?", new String[]{String.valueOf(userId)}) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean updateUserComplete(int userId, String firstName, String lastName, String birthday, String username, String password) {
        User currentUser = getUserById(userId);
        if (currentUser != null && !username.equals(currentUser.getUsername()) && usernameExists(username)) {
            return false;
        }
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(FIRST_NAME, firstName);
            values.put(LAST_NAME, lastName);
            values.put(BIRTHDAY, birthday);
            values.put(USERNAME, username);
            if (password != null && !password.isEmpty()) {
                values.put(PASSWORD, hashPassword(password));
            }
            return db.update(TABLE_USERS, values, ID + " = ?", new String[]{String.valueOf(userId)}) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteUser(int userId) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            return db.delete(TABLE_USERS, ID + " = ?", new String[]{String.valueOf(userId)}) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteUserByUsername(String username) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            return db.delete(TABLE_USERS, USERNAME + " = ?", new String[]{username}) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteAllUsers() {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            return db.delete(TABLE_USERS, null, null) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public int getUserCount() {
        try (SQLiteDatabase db = this.getReadableDatabase();
             Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_USERS, null)) {
            cursor.moveToFirst();
            return cursor.getInt(0);
        } catch (Exception e) {
            return 0;
        }
    }

    public List<User> searchUsersByName(String searchTerm) {
        List<User> userList = new ArrayList<>();
        try (SQLiteDatabase db = this.getReadableDatabase();
             Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS +
                     " WHERE " + FIRST_NAME + " LIKE ? OR " + LAST_NAME + " LIKE ?" +
                     " ORDER BY " + FIRST_NAME, new String[]{"%" + searchTerm + "%", "%" + searchTerm + "%"})) {
            if (cursor.moveToFirst()) {
                do {
                    userList.add(createUserFromCursor(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception ignored) {}
        return userList;
    }

    public boolean registerUser(String firstName, String lastName, String birthday, String username, String password) {
        return createUser(firstName, lastName, birthday, username, password);
    }

    public boolean loginUser(String username, String password) {
        try (SQLiteDatabase db = this.getReadableDatabase();
             Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " +
                     USERNAME + " = ? AND " + PASSWORD + " = ?", new String[]{username, hashPassword(password)})) {
            return cursor.getCount() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean usernameExists(String username) {
        try (SQLiteDatabase db = this.getReadableDatabase();
             Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + USERNAME + " = ?",
                     new String[]{username})) {
            return cursor.getCount() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public User getUserDetails(String username) {
        return getUserByUsername(username);
    }

    public boolean createEvent(int userId, String title, String description, long startTime, long endTime, int color) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(USER_ID, userId);
            values.put(EVENT_TITLE, title);
            values.put(EVENT_DESCRIPTION, description);
            values.put(EVENT_START_TIME, millisToDateTimeString(startTime));
            values.put(EVENT_END_TIME, millisToDateTimeString(endTime));
            values.put(EVENT_COLOR, color);
            return db.insert(TABLE_EVENTS, null, values) != -1;
        } catch (Exception e) {
            return false;
        }
    }

    public List<CalendarEvent> getEventsForUser(int userId) {
        List<CalendarEvent> eventList = new ArrayList<>();
        try (SQLiteDatabase db = this.getReadableDatabase();
             Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_EVENTS + " WHERE " + USER_ID + " = ? ORDER BY " + EVENT_START_TIME,
                     new String[]{String.valueOf(userId)})) {
            if (cursor.moveToFirst()) {
                do {
                    eventList.add(createEventFromCursor(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception ignored) {}
        return eventList;
    }

    public List<CalendarEvent> getEventsForUserByDate(int userId, long dateStart, long dateEnd) {
        List<CalendarEvent> eventList = new ArrayList<>();
        String startDateStr = millisToDateTimeString(dateStart);
        String endDateStr = millisToDateTimeString(dateEnd);

        try (SQLiteDatabase db = this.getReadableDatabase();
             Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_EVENTS +
                     " WHERE " + USER_ID + " = ? AND " +
                     "((" + EVENT_START_TIME + " >= ? AND " + EVENT_START_TIME + " < ?) OR " +
                     "(" + EVENT_END_TIME + " > ? AND " + EVENT_END_TIME + " <= ?) OR " +
                     "(" + EVENT_START_TIME + " <= ? AND " + EVENT_END_TIME + " >= ?))" +
                     " ORDER BY " + EVENT_START_TIME,
                     new String[]{String.valueOf(userId), startDateStr, endDateStr,
                             startDateStr, endDateStr, startDateStr, endDateStr})) {
            if (cursor.moveToFirst()) {
                do {
                    eventList.add(createEventFromCursor(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception ignored) {}
        return eventList;
    }

    public boolean deleteEvent(int userId, CalendarEvent event) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            String startTimeStr = millisToDateTimeString(event.getStartTimeMillis());
            String endTimeStr = millisToDateTimeString(event.getEndTimeMillis());

            db.execSQL("DELETE FROM " + TABLE_EVENTS +
                            " WHERE " + USER_ID + " = ? AND " +
                            EVENT_TITLE + " = ? AND " +
                            EVENT_START_TIME + " = ? AND " +
                            EVENT_END_TIME + " = ?",
                    new String[]{String.valueOf(userId), event.getTitle(), startTimeStr, endTimeStr});
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteAllEventsForUser(int userId) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            return db.delete(TABLE_EVENTS, USER_ID + " = ?", new String[]{String.valueOf(userId)}) >= 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean updateEvent(int userId, CalendarEvent oldEvent, String newTitle, String newDescription,
                              long newStartTime, long newEndTime, int newColor) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            String oldStartTimeStr = millisToDateTimeString(oldEvent.getStartTimeMillis());
            String oldEndTimeStr = millisToDateTimeString(oldEvent.getEndTimeMillis());

            ContentValues values = new ContentValues();
            values.put(EVENT_TITLE, newTitle);
            values.put(EVENT_DESCRIPTION, newDescription);
            values.put(EVENT_START_TIME, millisToDateTimeString(newStartTime));
            values.put(EVENT_END_TIME, millisToDateTimeString(newEndTime));
            values.put(EVENT_COLOR, newColor);
            return db.update(TABLE_EVENTS, values,
                    USER_ID + " = ? AND " + EVENT_TITLE + " = ? AND " + EVENT_START_TIME + " = ? AND " + EVENT_END_TIME + " = ?",
                    new String[]{String.valueOf(userId), oldEvent.getTitle(), oldStartTimeStr, oldEndTimeStr}) > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
