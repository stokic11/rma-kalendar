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
    private static final String EVENT_NOTIFICATIONS = "notifications_enabled";

    private static final String TABLE_BIRTHDAYS = "birthdays";
    private static final String BIRTHDAY_ID = "birthday_id";
    private static final String BIRTHDAY_NAMES = "names";
    private static final String BIRTHDAY_DESCRIPTION = "description";
    private static final String BIRTHDAY_DATE = "birthday_date";
    private static final String BIRTHDAY_YEARLY = "yearly_recurrence";
    private static final String BIRTHDAY_NOTIFICATIONS = "notifications_enabled";
    private static final String BIRTHDAY_COLOR = "color";

    private static final String TABLE_REMINDERS = "reminders";
    private static final String REMINDER_ID = "reminder_id";
    private static final String REMINDER_TITLE = "title";
    private static final String REMINDER_DESCRIPTION = "description";
    private static final String REMINDER_DATE = "reminder_date";
    private static final String REMINDER_HOUR = "reminder_hour";
    private static final String REMINDER_MINUTE = "reminder_minute";
    private static final String REMINDER_NOTIFICATIONS = "notifications_enabled";
    private static final String REMINDER_COLOR = "color";

    private Context context;

    public Database(Context context) {
        super(context, DATABASE_NAME, null, 1);
        this.context = context;
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
                EVENT_NOTIFICATIONS + " INTEGER NOT NULL, " +
                "FOREIGN KEY(" + USER_ID + ") REFERENCES " + TABLE_USERS + "(" + ID + "))");

        db.execSQL("CREATE TABLE " + TABLE_BIRTHDAYS + " (" +
                BIRTHDAY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                USER_ID + " INTEGER NOT NULL, " +
                BIRTHDAY_NAMES + " TEXT NOT NULL, " +
                BIRTHDAY_DESCRIPTION + " TEXT, " +
                BIRTHDAY_DATE + " TEXT NOT NULL, " +
                BIRTHDAY_YEARLY + " INTEGER NOT NULL, " +
                BIRTHDAY_NOTIFICATIONS + " INTEGER NOT NULL, " +
                BIRTHDAY_COLOR + " INTEGER NOT NULL, " +
                "FOREIGN KEY(" + USER_ID + ") REFERENCES " + TABLE_USERS + "(" + ID + "))");

        db.execSQL("CREATE TABLE " + TABLE_REMINDERS + " (" +
                REMINDER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                USER_ID + " INTEGER NOT NULL, " +
                REMINDER_TITLE + " TEXT NOT NULL, " +
                REMINDER_DESCRIPTION + " TEXT, " +
                REMINDER_DATE + " TEXT NOT NULL, " +
                REMINDER_HOUR + " INTEGER NOT NULL, " +
                REMINDER_MINUTE + " INTEGER NOT NULL, " +
                REMINDER_NOTIFICATIONS + " INTEGER NOT NULL, " +
                REMINDER_COLOR + " INTEGER NOT NULL, " +
                "FOREIGN KEY(" + USER_ID + ") REFERENCES " + TABLE_USERS + "(" + ID + "))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REMINDERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BIRTHDAYS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
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
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return 0;
        }
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            java.util.Date date = sdf.parse(dateTimeString);
            return date != null ? date.getTime() : 0;
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

        CalendarEvent event = new CalendarEvent(
                cursor.getString(cursor.getColumnIndexOrThrow(EVENT_TITLE)),
                cursor.getString(cursor.getColumnIndexOrThrow(EVENT_DESCRIPTION)),
                startTime,
                endTime,
                cursor.getInt(cursor.getColumnIndexOrThrow(EVENT_COLOR))
        );

        try {
            event.setNotificationsEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(EVENT_NOTIFICATIONS)) == 1);
        } catch (Exception e) {
            event.setNotificationsEnabled(true);
        }

        event.setId(String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(EVENT_ID))));
        return event;
    }

    private Birthday createBirthdayFromCursor(Cursor cursor) {
        String namesStr = cursor.getString(cursor.getColumnIndexOrThrow(BIRTHDAY_NAMES));
        String[] namesArray = namesStr.split(",");
        List<String> names = new ArrayList<>();
        for (String name : namesArray) {
            if (!name.trim().isEmpty()) {
                names.add(name.trim());
            }
        }

        String dateStr = cursor.getString(cursor.getColumnIndexOrThrow(BIRTHDAY_DATE));
        long dateMillis = dateTimeStringToMillis(dateStr);

        Birthday birthday = new Birthday(
                names,
                cursor.getString(cursor.getColumnIndexOrThrow(BIRTHDAY_DESCRIPTION)),
                dateMillis,
                cursor.getInt(cursor.getColumnIndexOrThrow(BIRTHDAY_YEARLY)) == 1,
                cursor.getInt(cursor.getColumnIndexOrThrow(BIRTHDAY_NOTIFICATIONS)) == 1
        );
        birthday.setId(String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(BIRTHDAY_ID))));
        birthday.setColor(cursor.getInt(cursor.getColumnIndexOrThrow(BIRTHDAY_COLOR)));
        return birthday;
    }

    private Reminder createReminderFromCursor(Cursor cursor) {
        String dateStr = cursor.getString(cursor.getColumnIndexOrThrow(REMINDER_DATE));
        long dateMillis = dateTimeStringToMillis(dateStr);

        int hour = 9;
        int minute = 0;

        try {
            hour = cursor.getInt(cursor.getColumnIndexOrThrow(REMINDER_HOUR));
            minute = cursor.getInt(cursor.getColumnIndexOrThrow(REMINDER_MINUTE));
        } catch (Exception e) {
        }

        Reminder reminder = new Reminder(
                cursor.getString(cursor.getColumnIndexOrThrow(REMINDER_TITLE)),
                cursor.getString(cursor.getColumnIndexOrThrow(REMINDER_DESCRIPTION)),
                dateMillis,
                hour,
                minute,
                cursor.getInt(cursor.getColumnIndexOrThrow(REMINDER_NOTIFICATIONS)) == 1
        );
        reminder.setId(String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(REMINDER_ID))));
        reminder.setColor(cursor.getInt(cursor.getColumnIndexOrThrow(REMINDER_COLOR)));
        return reminder;
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
            values.put(EVENT_NOTIFICATIONS, 1);
            return db.insert(TABLE_EVENTS, null, values) != -1;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean saveEvent(CalendarEvent event, int userId) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(USER_ID, userId);
            values.put(EVENT_TITLE, event.getTitle());
            values.put(EVENT_DESCRIPTION, event.getDescription());
            values.put(EVENT_START_TIME, millisToDateTimeString(event.getStartTimeMillis()));
            values.put(EVENT_END_TIME, millisToDateTimeString(event.getEndTimeMillis()));
            values.put(EVENT_COLOR, event.getColor());
            values.put(EVENT_NOTIFICATIONS, event.isNotificationsEnabled() ? 1 : 0);
            long result = db.insert(TABLE_EVENTS, null, values);
            if (result != -1) {
                event.setId(String.valueOf(result));
                if (event.isNotificationsEnabled()) {
                    EventNotificationService.scheduleEventNotifications(context, event);
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean updateEvent(int userId, CalendarEvent oldEvent, CalendarEvent newEvent) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            EventNotificationService.cancelEventNotifications(context, oldEvent);

            ContentValues values = new ContentValues();
            values.put(EVENT_TITLE, newEvent.getTitle());
            values.put(EVENT_DESCRIPTION, newEvent.getDescription());
            values.put(EVENT_START_TIME, millisToDateTimeString(newEvent.getStartTimeMillis()));
            values.put(EVENT_END_TIME, millisToDateTimeString(newEvent.getEndTimeMillis()));
            values.put(EVENT_COLOR, newEvent.getColor());
            values.put(EVENT_NOTIFICATIONS, newEvent.isNotificationsEnabled() ? 1 : 0);

            boolean success = db.update(TABLE_EVENTS, values, EVENT_ID + " = ?",
                    new String[]{oldEvent.getId()}) > 0;

            if (success) {
                newEvent.setId(oldEvent.getId());
                if (newEvent.isNotificationsEnabled()) {
                    EventNotificationService.scheduleEventNotifications(context, newEvent);
                }
            }
            return success;
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

    public List<CalendarEvent> getAllEvents() {
        List<CalendarEvent> eventList = new ArrayList<>();
        try (SQLiteDatabase db = this.getReadableDatabase();
             Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_EVENTS + " ORDER BY " + EVENT_START_TIME, null)) {
            if (cursor.moveToFirst()) {
                do {
                    eventList.add(createEventFromCursor(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception ignored) {}
        return eventList;
    }

    public CalendarEvent getEventById(int eventId) {
        try (SQLiteDatabase db = this.getReadableDatabase();
             Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_EVENTS + " WHERE " + EVENT_ID + " = ?",
                     new String[]{String.valueOf(eventId)})) {
            return cursor.moveToFirst() ? createEventFromCursor(cursor) : null;
        } catch (Exception e) {
            return null;
        }
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
            EventNotificationService.cancelEventNotifications(context, event);

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

    public boolean saveBirthday(Birthday birthday, int userId) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(USER_ID, userId);
            values.put(BIRTHDAY_NAMES, String.join(",", birthday.getNames()));
            values.put(BIRTHDAY_DESCRIPTION, birthday.getDescription());
            values.put(BIRTHDAY_DATE, millisToDateTimeString(birthday.getDateMillis()));
            values.put(BIRTHDAY_YEARLY, birthday.isYearlyRecurrence() ? 1 : 0);
            values.put(BIRTHDAY_NOTIFICATIONS, birthday.isNotificationsEnabled() ? 1 : 0);
            values.put(BIRTHDAY_COLOR, birthday.getColor());
            return db.insert(TABLE_BIRTHDAYS, null, values) != -1;
        } catch (Exception e) {
            return false;
        }
    }

    public List<Birthday> getBirthdaysForUser(int userId) {
        List<Birthday> birthdays = new ArrayList<>();
        try (SQLiteDatabase db = this.getReadableDatabase();
             Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_BIRTHDAYS + " WHERE " + USER_ID + " = ?",
                     new String[]{String.valueOf(userId)})) {
            if (cursor.moveToFirst()) {
                do {
                    birthdays.add(createBirthdayFromCursor(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
        }
        return birthdays;
    }

    public List<Birthday> getBirthdaysForDate(int userId, long dateMillis) {
        List<Birthday> birthdays = new ArrayList<>();
        java.util.Calendar targetCal = java.util.Calendar.getInstance();
        targetCal.setTimeInMillis(dateMillis);
        int targetMonth = targetCal.get(java.util.Calendar.MONTH);
        int targetDay = targetCal.get(java.util.Calendar.DAY_OF_MONTH);

        try (SQLiteDatabase db = this.getReadableDatabase();
             Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_BIRTHDAYS + " WHERE " + USER_ID + " = ?",
                     new String[]{String.valueOf(userId)})) {
            if (cursor.moveToFirst()) {
                do {
                    Birthday birthday = createBirthdayFromCursor(cursor);
                    java.util.Calendar birthdayCal = java.util.Calendar.getInstance();
                    birthdayCal.setTimeInMillis(birthday.getDateMillis());

                    if (birthdayCal.get(java.util.Calendar.MONTH) == targetMonth &&
                        birthdayCal.get(java.util.Calendar.DAY_OF_MONTH) == targetDay) {
                        birthdays.add(birthday);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
        }
        return birthdays;
    }

    public boolean deleteBirthday(String birthdayId, int userId) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            return db.delete(TABLE_BIRTHDAYS, BIRTHDAY_ID + " = ? AND " + USER_ID + " = ?",
                    new String[]{birthdayId, String.valueOf(userId)}) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean updateBirthday(String birthdayId, Birthday updatedBirthday, int userId) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(BIRTHDAY_NAMES, String.join(",", updatedBirthday.getNames()));
            values.put(BIRTHDAY_DESCRIPTION, updatedBirthday.getDescription());
            values.put(BIRTHDAY_DATE, millisToDateTimeString(updatedBirthday.getDateMillis()));
            values.put(BIRTHDAY_YEARLY, updatedBirthday.isYearlyRecurrence() ? 1 : 0);
            values.put(BIRTHDAY_NOTIFICATIONS, updatedBirthday.isNotificationsEnabled() ? 1 : 0);
            values.put(BIRTHDAY_COLOR, updatedBirthday.getColor());
            return db.update(TABLE_BIRTHDAYS, values, BIRTHDAY_ID + " = ? AND " + USER_ID + " = ?",
                    new String[]{birthdayId, String.valueOf(userId)}) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean saveReminder(Reminder reminder, int userId) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(USER_ID, userId);
            values.put(REMINDER_TITLE, reminder.getTitle());
            values.put(REMINDER_DESCRIPTION, reminder.getDescription());
            values.put(REMINDER_DATE, millisToDateTimeString(reminder.getDateMillis()));
            values.put(REMINDER_HOUR, reminder.getHour());
            values.put(REMINDER_MINUTE, reminder.getMinute());
            values.put(REMINDER_NOTIFICATIONS, reminder.isNotificationsEnabled() ? 1 : 0);
            values.put(REMINDER_COLOR, reminder.getColor());
            long result = db.insert(TABLE_REMINDERS, null, values);
            if (result != -1) {
                reminder.setId(String.valueOf(result));
                if (reminder.isNotificationsEnabled()) {
                    ReminderNotificationService.scheduleReminderNotifications(context, reminder);
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public List<Reminder> getRemindersForUser(int userId) {
        List<Reminder> reminders = new ArrayList<>();
        try (SQLiteDatabase db = this.getReadableDatabase();
             Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_REMINDERS + " WHERE " + USER_ID + " = ?",
                     new String[]{String.valueOf(userId)})) {
            if (cursor.moveToFirst()) {
                do {
                    reminders.add(createReminderFromCursor(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
        }
        return reminders;
    }

    public List<Reminder> getRemindersForDate(int userId, long dateMillis) {
        List<Reminder> reminders = new ArrayList<>();
        java.util.Calendar targetCal = java.util.Calendar.getInstance();
        targetCal.setTimeInMillis(dateMillis);
        int targetYear = targetCal.get(java.util.Calendar.YEAR);
        int targetMonth = targetCal.get(java.util.Calendar.MONTH);
        int targetDay = targetCal.get(java.util.Calendar.DAY_OF_MONTH);

        try (SQLiteDatabase db = this.getReadableDatabase();
             Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_REMINDERS + " WHERE " + USER_ID + " = ?",
                     new String[]{String.valueOf(userId)})) {
            if (cursor.moveToFirst()) {
                do {
                    Reminder reminder = createReminderFromCursor(cursor);
                    java.util.Calendar reminderCal = java.util.Calendar.getInstance();
                    reminderCal.setTimeInMillis(reminder.getDateMillis());

                    if (reminderCal.get(java.util.Calendar.YEAR) == targetYear &&
                        reminderCal.get(java.util.Calendar.MONTH) == targetMonth &&
                        reminderCal.get(java.util.Calendar.DAY_OF_MONTH) == targetDay) {
                        reminders.add(reminder);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
        }
        return reminders;
    }

    public boolean deleteReminder(String reminderId, int userId) {
        List<Reminder> userReminders = getRemindersForUser(userId);
        Reminder reminderToDelete = null;
        for (Reminder reminder : userReminders) {
            if (reminder.getId().equals(reminderId)) {
                reminderToDelete = reminder;
                break;
            }
        }

        try (SQLiteDatabase db = this.getWritableDatabase()) {
            boolean success = db.delete(TABLE_REMINDERS, REMINDER_ID + " = ? AND " + USER_ID + " = ?",
                    new String[]{reminderId, String.valueOf(userId)}) > 0;

            if (success && reminderToDelete != null) {
                ReminderNotificationService.cancelReminderNotifications(context, reminderToDelete);
            }

            return success;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean updateReminder(String reminderId, Reminder updatedReminder, int userId) {
        List<Reminder> userReminders = getRemindersForUser(userId);
        Reminder oldReminder = null;
        for (Reminder reminder : userReminders) {
            if (reminder.getId().equals(reminderId)) {
                oldReminder = reminder;
                break;
            }
        }

        try (SQLiteDatabase db = this.getWritableDatabase()) {
            if (oldReminder != null) {
                ReminderNotificationService.cancelReminderNotifications(context, oldReminder);
            }

            ContentValues values = new ContentValues();
            values.put(REMINDER_TITLE, updatedReminder.getTitle());
            values.put(REMINDER_DESCRIPTION, updatedReminder.getDescription());
            values.put(REMINDER_DATE, millisToDateTimeString(updatedReminder.getDateMillis()));
            values.put(REMINDER_HOUR, updatedReminder.getHour());
            values.put(REMINDER_MINUTE, updatedReminder.getMinute());
            values.put(REMINDER_NOTIFICATIONS, updatedReminder.isNotificationsEnabled() ? 1 : 0);
            values.put(REMINDER_COLOR, updatedReminder.getColor());

            boolean success = db.update(TABLE_REMINDERS, values, REMINDER_ID + " = ? AND " + USER_ID + " = ?",
                    new String[]{reminderId, String.valueOf(userId)}) > 0;

            if (success) {
                updatedReminder.setId(reminderId);
                if (updatedReminder.isNotificationsEnabled()) {
                    ReminderNotificationService.scheduleReminderNotifications(context, updatedReminder);
                }
            }

            return success;
        } catch (Exception e) {
            return false;
        }
    }
}
