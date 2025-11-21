package fpl.manhph61584.duan1_nhom3_app;

import fpl.manhph61584.duan1_nhom3_app.network.dto.UserDto;

public class UserManager {
    private static UserDto currentUser;
    private static String authToken;

    private UserManager() {
    }

    public static void saveSession(UserDto user, String token) {
        currentUser = user;
        authToken = token;
    }

    public static void clearSession() {
        currentUser = null;
        authToken = null;
    }

    public static UserDto getCurrentUser() {
        return currentUser;
    }

    public static String getAuthToken() {
        return authToken;
    }
}