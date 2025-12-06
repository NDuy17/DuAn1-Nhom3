package fpl.manhph61584.duan1_nhom3_app;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import fpl.manhph61584.duan1_nhom3_app.network.dto.UserDto;

public class UserManager {
    private static final String PREFS_NAME = "UserSession";
    private static final String KEY_USER = "user";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_REMEMBER_ME = "remember_me";
    private static final String KEY_SAVED_EMAIL = "saved_email";
    
    private static UserDto currentUser;
    private static String authToken;

    private UserManager() {
    }

    /**
     * Lưu session vào memory và SharedPreferences nếu rememberMe = true
     */
    public static void saveSession(UserDto user, String token, Context context, boolean rememberMe) {
        currentUser = user;
        authToken = token;
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        if (rememberMe) {
            // Lưu user và token
            Gson gson = new Gson();
            String userJson = gson.toJson(user);
            editor.putString(KEY_USER, userJson);
            editor.putString(KEY_TOKEN, token);
            editor.putBoolean(KEY_REMEMBER_ME, true);
            if (user != null && user.getEmail() != null) {
                editor.putString(KEY_SAVED_EMAIL, user.getEmail());
            }
        } else {
            // Xóa thông tin đã lưu
            editor.remove(KEY_USER);
            editor.remove(KEY_TOKEN);
            editor.putBoolean(KEY_REMEMBER_ME, false);
        }
        editor.apply();
    }

    /**
     * Lưu session vào memory (backward compatibility)
     */
    public static void saveSession(UserDto user, String token) {
        currentUser = user;
        authToken = token;
    }

    /**
     * Khôi phục session từ SharedPreferences
     */
    public static boolean restoreSession(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean rememberMe = prefs.getBoolean(KEY_REMEMBER_ME, false);
        
        if (rememberMe) {
            String userJson = prefs.getString(KEY_USER, null);
            String token = prefs.getString(KEY_TOKEN, null);
            
            if (userJson != null && token != null) {
                try {
                    Gson gson = new Gson();
                    UserDto user = gson.fromJson(userJson, UserDto.class);
                    currentUser = user;
                    authToken = token;
                    return true;
                } catch (Exception e) {
                    android.util.Log.e("UserManager", "Error restoring session", e);
                    clearSession(context);
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Xóa session từ memory và SharedPreferences
     */
    public static void clearSession(Context context) {
        currentUser = null;
        authToken = null;
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_USER);
        editor.remove(KEY_TOKEN);
        editor.putBoolean(KEY_REMEMBER_ME, false);
        editor.apply();
    }

    /**
     * Xóa session từ memory (backward compatibility)
     */
    public static void clearSession() {
        currentUser = null;
        authToken = null;
    }

    /**
     * Lấy email đã lưu
     */
    public static String getSavedEmail(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_SAVED_EMAIL, "");
    }

    public static UserDto getCurrentUser() {
        return currentUser;
    }

    public static String getAuthToken() {
        return authToken;
    }
}