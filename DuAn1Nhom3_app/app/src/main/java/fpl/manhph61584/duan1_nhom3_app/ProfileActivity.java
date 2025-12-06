package fpl.manhph61584.duan1_nhom3_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import fpl.manhph61584.duan1_nhom3_app.network.dto.UserDto;

public class ProfileActivity extends AppCompatActivity {

    private ImageView imgAvatar;
    private TextView txtUserName, txtUserEmail, txtEmail;
    private EditText edtName;
    private Button btnLogout, btnSaveProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        imgAvatar = findViewById(R.id.imgAvatar);
        txtUserName = findViewById(R.id.txtUserName);
        txtUserEmail = findViewById(R.id.txtUserEmail);
        edtName = findViewById(R.id.edtName);
        txtEmail = findViewById(R.id.txtEmail);
        btnLogout = findViewById(R.id.btnLogout);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);

        loadUserInfo();
        setupBottomNavigation();
        setupSaveButton();
        setupOrderHistory();

        btnLogout.setOnClickListener(v -> {
            UserManager.clearSession(ProfileActivity.this);
            Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void setupBottomNavigation() {
        LinearLayout btnHome = findViewById(R.id.btnHome);
        LinearLayout btnBottomCart = findViewById(R.id.btnBottomCart);
        LinearLayout btnProfile = findViewById(R.id.btnProfile);

        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
            });
        }

        if (btnBottomCart != null) {
            btnBottomCart.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, OrderStatusActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }

        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                // Đã ở trang Profile rồi, không cần làm gì
            });
        }
    }


    private void setupSaveButton() {
        btnSaveProfile.setOnClickListener(v -> {
            // Chuyển sang trang EditProfileActivity
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivityForResult(intent, 1);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    private void setupOrderHistory() {
        LinearLayout layoutOrderHistory = findViewById(R.id.layoutOrderHistory);
        if (layoutOrderHistory != null) {
            layoutOrderHistory.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, OrderHistoryActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            // Refresh thông tin sau khi sửa
            loadUserInfo();
        }
    }

    private void loadUserInfo() {
        UserDto user = UserManager.getCurrentUser();
        if (user != null) {
            String name = user.getName() != null ? user.getName() : "Người dùng";
            String email = user.getEmail() != null ? user.getEmail() : "";
            String avatar = user.getAvatar();

            txtUserName.setText(name);
            txtUserEmail.setText(email);
            edtName.setText(name);
            txtEmail.setText(email);

            // Load avatar
            if (avatar != null && !avatar.isEmpty()) {
                String imageUrl = avatar;
                if (imageUrl.startsWith("/uploads/")) {
                    imageUrl = "http://10.0.2.2:3000" + imageUrl;
                }
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_user)
                        .error(R.drawable.ic_user)
                        .circleCrop()
                        .into(imgAvatar);
            } else {
                imgAvatar.setImageResource(R.drawable.ic_user);
            }
        } else {
            // Nếu chưa đăng nhập, quay về login
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        }
    }
}




