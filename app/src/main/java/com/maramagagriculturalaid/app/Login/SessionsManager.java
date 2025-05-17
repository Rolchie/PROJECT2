package com.maramagagriculturalaid.app.Login;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Session manager to save and fetch user session data
 */
public class SessionsManager {

    // Shared Preferences
    private SharedPreferences pref;

    // Editor for Shared preferences
    private SharedPreferences.Editor editor;

    // Context
    private Context context;

    // Shared pref mode
    private int PRIVATE_MODE = 0;

    // Sharedpref file name
    private static final String PREF_NAME = "MaramagAgriAidPref";

    // User role key (Make private variables to not access from outside class)
    private static final String KEY_USER_ROLE = "user_role";

    // User email
    private static final String KEY_USER_EMAIL = "user_email";

    // User ID
    private static final String KEY_USER_ID = "user_id";

    // Role constants
    public static final String ROLE_MUNICIPAL = "Municipal";
    public static final String ROLE_BARANGAY = "Barangay";

    // Constructor - must match class name exactly
    public SessionsManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    /**
     * Create login session
     * */
    public void createLoginSession(String userId, String email, String role) {
        // Storing user id in pref
        editor.putString(KEY_USER_ID, userId);

        // Storing email in pref
        editor.putString(KEY_USER_EMAIL, email);

        // Storing role in pref
        editor.putString(KEY_USER_ROLE, role);

        // commit changes
        editor.apply();
    }

    /**
     * Get stored session data
     * */
    public String getUserRole() {
        return pref.getString(KEY_USER_ROLE, "");
    }

    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, "");
    }

    public String getUserId() {
        return pref.getString(KEY_USER_ID, "");
    }

    /**
     * Clear session details
     * */
    public void logoutUser() {
        // Clearing all data from Shared Preferences
        editor.clear();
        editor.apply();
    }

    /**
     * Quick check for login
     * **/
    public boolean isLoggedIn() {
        return !pref.getString(KEY_USER_ID, "").isEmpty();
    }

    /**
     * Check if user is Municipal
     * */
    public boolean isMunicipal() {
        return ROLE_MUNICIPAL.equals(getUserRole());
    }

    /**
     * Check if user is Barangay
     * */
    public boolean isBarangay() {
        return ROLE_BARANGAY.equals(getUserRole());
    }
}
