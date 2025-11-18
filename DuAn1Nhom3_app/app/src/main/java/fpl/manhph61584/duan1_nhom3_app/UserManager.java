package fpl.manhph61584.duan1_nhom3_app;

import java.util.HashMap;

public class UserManager {
    private static HashMap<String, String> userDatabase = new HashMap<>();

    // Đăng ký người dùng mới
    public static boolean register(String email, String password) {
        if (userDatabase.containsKey(email)) {
            return false; // Email đã tồn tại
        }
        userDatabase.put(email, password);
        return true;
    }

    // Kiểm tra đăng nhập
    public static boolean login(String email, String password) {
        return userDatabase.containsKey(email) && userDatabase.get(email).equals(password);
    }
}