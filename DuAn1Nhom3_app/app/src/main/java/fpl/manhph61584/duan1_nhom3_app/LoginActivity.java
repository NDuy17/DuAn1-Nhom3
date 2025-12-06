package fpl.manhph61584.duan1_nhom3_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import fpl.manhph61584.duan1_nhom3_app.network.ApiClient;
import fpl.manhph61584.duan1_nhom3_app.network.ApiService;
import fpl.manhph61584.duan1_nhom3_app.network.dto.LoginRequest;
import fpl.manhph61584.duan1_nhom3_app.network.dto.LoginResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText edtUsername, edtPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private ProgressBar loginProgress;
    private CheckBox checkboxRememberMe;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        loginProgress = findViewById(R.id.loginProgress);
        checkboxRememberMe = findViewById(R.id.checkboxRememberMe);
        apiService = ApiClient.getApiService();

        // Khôi phục email đã lưu
        String savedEmail = UserManager.getSavedEmail(this);
        if (!savedEmail.isEmpty()) {
            edtUsername.setText(savedEmail);
            checkboxRememberMe.setChecked(true);
        }

        // Kiểm tra nếu đã đăng nhập (remember me)
        if (UserManager.restoreSession(this)) {
            // Đã có session, chuyển thẳng đến MainActivity
            android.util.Log.d("LoginActivity", "✅ Session restored, redirecting to MainActivity");
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        btnLogin.setOnClickListener(v -> attemptLogin());
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    private void attemptLogin() {
        String email = edtUsername.getText().toString().trim();
        String pass = edtPassword.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        apiService.login(new LoginRequest(email, pass)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse body = response.body();
                    
                    // Log chi tiết response
                    android.util.Log.d("LoginActivity", "=== LOGIN RESPONSE ===");
                    android.util.Log.d("LoginActivity", "Token: " + (body.getToken() != null ? body.getToken().substring(0, Math.min(20, body.getToken().length())) + "..." : "null"));
                    
                    if (body.getUser() != null) {
                        android.util.Log.d("LoginActivity", "User object:");
                        android.util.Log.d("LoginActivity", "  - ID: " + body.getUser().getId());
                        android.util.Log.d("LoginActivity", "  - Email: " + body.getUser().getEmail());
                        android.util.Log.d("LoginActivity", "  - Name: " + body.getUser().getName());
                        android.util.Log.d("LoginActivity", "  - Role: " + body.getUser().getRole());
                    } else {
                        android.util.Log.e("LoginActivity", "❌ User object is null in response!");
                    }
                    
                    // Lưu session với remember me
                    boolean rememberMe = checkboxRememberMe.isChecked();
                    UserManager.saveSession(body.getUser(), body.getToken(), LoginActivity.this, rememberMe);
                    
                    // Verify saved session
                    fpl.manhph61584.duan1_nhom3_app.network.dto.UserDto savedUser = UserManager.getCurrentUser();
                    if (savedUser != null) {
                        android.util.Log.d("LoginActivity", "✅ Session saved - Role: " + savedUser.getRole() + ", Remember Me: " + rememberMe);
                    } else {
                        android.util.Log.e("LoginActivity", "❌ Failed to save session!");
                    }
                    
                    Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                    
                    // Luôn chuyển đến MainActivity (chỉ dành cho khách hàng)
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    finish();
                } else {
                    android.util.Log.e("LoginActivity", "❌ Login failed - Response code: " + response.code());
                    if (response.errorBody() != null) {
                        try {
                            String errorStr = response.errorBody().string();
                            android.util.Log.e("LoginActivity", "Error body: " + errorStr);
                        } catch (Exception e) {
                            android.util.Log.e("LoginActivity", "Error reading error body", e);
                        }
                    }
                    Toast.makeText(LoginActivity.this, "Sai email hoặc mật khẩu!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        loginProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        tvRegister.setEnabled(!loading);
    }
}