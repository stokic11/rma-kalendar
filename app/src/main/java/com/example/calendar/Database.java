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
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_USERS = "users";
    private static final String ID = "id";
    private static final String FIRST_NAME = "first_name";
    private static final String LAST_NAME = "last_name";
    private static final String BIRTHDAY = "birthday";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                FIRST_NAME + " TEXT NOT NULL, " +
                LAST_NAME + " TEXT NOT NULL, " +
                BIRTHDAY + " TEXT NOT NULL, " +
                USERNAME + " TEXT UNIQUE NOT NULL, " +
                PASSWORD + " TEXT NOT NULL)";

        db.execSQL(createUsersTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
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
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return password;
        }
    }

    public boolean createUser(String firstName, String lastName, String birthday, String username, String password) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();

            String checkQuery = "SELECT * FROM " + TABLE_USERS + " WHERE " + USERNAME + " = ?";
            Cursor checkCursor = db.rawQuery(checkQuery, new String[]{username});
            boolean usernameExists = checkCursor.getCount() > 0;
            checkCursor.close();

            if (usernameExists) {
                return false;
            }

            ContentValues values = new ContentValues();
            values.put(FIRST_NAME, firstName);
            values.put(LAST_NAME, lastName);
            values.put(BIRTHDAY, birthday);
            values.put(USERNAME, username);
            values.put(PASSWORD, hashPassword(password));

            long result = db.insert(TABLE_USERS, null, values);
            return result != -1;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    public List<User> getAllUsers() {
        List<User> userList = new ArrayList<>();
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            String query = "SELECT * FROM " + TABLE_USERS + " ORDER BY " + FIRST_NAME + " ASC";
            Cursor cursor = db.rawQuery(query, null);

            if (cursor.moveToFirst()) {
                do {
                    User user = new User();
                    user.setId(cursor.getInt(cursor.getColumnIndexOrThrow(ID)));
                    user.setFirstName(cursor.getString(cursor.getColumnIndexOrThrow(FIRST_NAME)));
                    user.setLastName(cursor.getString(cursor.getColumnIndexOrThrow(LAST_NAME)));
                    user.setBirthday(cursor.getString(cursor.getColumnIndexOrThrow(BIRTHDAY)));
                    user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(USERNAME)));
                    userList.add(user);
                } while (cursor.moveToNext());
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return userList;
    }

    public User getUserById(int userId) {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            String query = "SELECT * FROM " + TABLE_USERS + " WHERE " + ID + " = ?";
            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

            User user = null;
            if (cursor.moveToFirst()) {
                user = new User();
                user.setId(cursor.getInt(cursor.getColumnIndexOrThrow(ID)));
                user.setFirstName(cursor.getString(cursor.getColumnIndexOrThrow(FIRST_NAME)));
                user.setLastName(cursor.getString(cursor.getColumnIndexOrThrow(LAST_NAME)));
                user.setBirthday(cursor.getString(cursor.getColumnIndexOrThrow(BIRTHDAY)));
                user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(USERNAME)));
            }
            cursor.close();
            db.close();
            return user;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public User getUserByUsername(String username) {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            String query = "SELECT * FROM " + TABLE_USERS + " WHERE " + USERNAME + " = ?";
            Cursor cursor = db.rawQuery(query, new String[]{username});

            User user = null;
            if (cursor.moveToFirst()) {
                user = new User();
                user.setId(cursor.getInt(cursor.getColumnIndexOrThrow(ID)));
                user.setFirstName(cursor.getString(cursor.getColumnIndexOrThrow(FIRST_NAME)));
                user.setLastName(cursor.getString(cursor.getColumnIndexOrThrow(LAST_NAME)));
                user.setBirthday(cursor.getString(cursor.getColumnIndexOrThrow(BIRTHDAY)));
                user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(USERNAME)));
            }
            cursor.close();
            db.close();
            return user;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean updateUser(int userId, String firstName, String lastName, String birthday, String username) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();

            if (!username.equals(getUserById(userId).getUsername()) && usernameExists(username)) {
                db.close();
                return false;
            }

            ContentValues values = new ContentValues();
            values.put(FIRST_NAME, firstName);
            values.put(LAST_NAME, lastName);
            values.put(BIRTHDAY, birthday);
            values.put(USERNAME, username);

            int rowsAffected = db.update(TABLE_USERS, values, ID + " = ?", new String[]{String.valueOf(userId)});
            db.close();
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateUserPassword(int userId, String newPassword) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(PASSWORD, hashPassword(newPassword));

            int rowsAffected = db.update(TABLE_USERS, values, ID + " = ?", new String[]{String.valueOf(userId)});
            db.close();
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateUserComplete(int userId, String firstName, String lastName, String birthday, String username, String password) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();

            User currentUser = getUserById(userId);
            if (currentUser != null && !username.equals(currentUser.getUsername()) && usernameExists(username)) {
                db.close();
                return false;
            }

            ContentValues values = new ContentValues();
            values.put(FIRST_NAME, firstName);
            values.put(LAST_NAME, lastName);
            values.put(BIRTHDAY, birthday);
            values.put(USERNAME, username);
            if (password != null && !password.isEmpty()) {
                values.put(PASSWORD, hashPassword(password));
            }

            int rowsAffected = db.update(TABLE_USERS, values, ID + " = ?", new String[]{String.valueOf(userId)});
            db.close();
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteUser(int userId) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            int rowsAffected = db.delete(TABLE_USERS, ID + " = ?", new String[]{String.valueOf(userId)});
            db.close();
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteUserByUsername(String username) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            int rowsAffected = db.delete(TABLE_USERS, USERNAME + " = ?", new String[]{username});
            db.close();
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteAllUsers() {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            int rowsAffected = db.delete(TABLE_USERS, null, null);
            db.close();
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int getUserCount() {
        int count = 0;
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            String query = "SELECT COUNT(*) FROM " + TABLE_USERS;
            Cursor cursor = db.rawQuery(query, null);
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    public List<User> searchUsersByName(String searchTerm) {
        List<User> userList = new ArrayList<>();
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            String query = "SELECT * FROM " + TABLE_USERS +
                          " WHERE " + FIRST_NAME + " LIKE ? OR " + LAST_NAME + " LIKE ?" +
                          " ORDER BY " + FIRST_NAME + " ASC";
            Cursor cursor = db.rawQuery(query, new String[]{"%" + searchTerm + "%", "%" + searchTerm + "%"});

            if (cursor.moveToFirst()) {
                do {
                    User user = new User();
                    user.setId(cursor.getInt(cursor.getColumnIndexOrThrow(ID)));
                    user.setFirstName(cursor.getString(cursor.getColumnIndexOrThrow(FIRST_NAME)));
                    user.setLastName(cursor.getString(cursor.getColumnIndexOrThrow(LAST_NAME)));
                    user.setBirthday(cursor.getString(cursor.getColumnIndexOrThrow(BIRTHDAY)));
                    user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(USERNAME)));
                    userList.add(user);
                } while (cursor.moveToNext());
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return userList;
    }

    public boolean registerUser(String firstName, String lastName, String birthday, String username, String password) {
        return createUser(firstName, lastName, birthday, username, password);
    }

    public boolean loginUser(String username, String password) {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            String hashedPassword = hashPassword(password);

            String query = "SELECT * FROM " + TABLE_USERS + " WHERE " +
                    USERNAME + " = ? AND " + PASSWORD + " = ?";

            Cursor cursor = db.rawQuery(query, new String[]{username, hashedPassword});
            boolean loginSuccess = cursor.getCount() > 0;
            cursor.close();
            db.close();
            return loginSuccess;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean usernameExists(String username) {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            String query = "SELECT * FROM " + TABLE_USERS + " WHERE " + USERNAME + " = ?";
            Cursor cursor = db.rawQuery(query, new String[]{username});
            boolean exists = cursor.getCount() > 0;
            cursor.close();
            db.close();
            return exists;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public User getUserDetails(String username) {
        return getUserByUsername(username);
    }
}
