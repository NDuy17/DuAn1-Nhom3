package fpl.manhph61584.duan1_nhom3_app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import fpl.manhph61584.duan1_nhom3_app.network.ApiClient;
import fpl.manhph61584.duan1_nhom3_app.network.ApiService;
import fpl.manhph61584.duan1_nhom3_app.network.dto.UpdateProfileRequest;
import fpl.manhph61584.duan1_nhom3_app.network.dto.UpdateProfileResponse;
import fpl.manhph61584.duan1_nhom3_app.network.dto.UploadResponse;
import fpl.manhph61584.duan1_nhom3_app.network.dto.UserDto;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView imgAvatar;
    private TextView txtEmail, btnChangeAvatar, btnBack;
    private EditText edtName;
    private Button btnSave;
    private String currentAvatarPath = null;
    private Uri selectedImageUri = null;
    
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        imgAvatar = findViewById(R.id.imgAvatar);
        txtEmail = findViewById(R.id.txtEmail);
        edtName = findViewById(R.id.edtName);
        btnSave = findViewById(R.id.btnSave);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        btnBack = findViewById(R.id.btnBack);

        setupImagePicker();
        loadUserInfo();
        setupClickListeners();
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    Uri imageUri = result.getData().getData();
                    selectedImageUri = imageUri;
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                        imgAvatar.setImageBitmap(bitmap);
                        Toast.makeText(this, "Đã chọn ảnh. Bấm 'Lưu thông tin' để lưu", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(this, "Lỗi khi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        imgAvatar.setOnClickListener(v -> pickImage());
        btnChangeAvatar.setOnClickListener(v -> pickImage());

        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void saveProfile() {
        String newName = edtName.getText().toString().trim();
        if (newName.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = UserManager.getAuthToken();
        if (token == null) {
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Đang lưu...");

        // Nếu có ảnh mới được chọn, upload trước
        if (selectedImageUri != null) {
            uploadImageAndSaveProfile(selectedImageUri, newName, token);
        } else {
            // Không có ảnh mới, chỉ update tên và avatar hiện tại
            saveProfileToServer(newName, currentAvatarPath, token);
        }
    }

    private void uploadImageAndSaveProfile(Uri imageUri, String newName, String token) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Toast.makeText(this, "Không thể đọc ảnh", Toast.LENGTH_SHORT).show();
                btnSave.setEnabled(true);
                btnSave.setText("Lưu thông tin");
                return;
            }

            // Đọc ảnh thành byte array
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int nRead;
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            byte[] imageBytes = buffer.toByteArray();
            inputStream.close();

            // Detect MIME type thực tế từ URI
            String mimeType = getContentResolver().getType(imageUri);
            if (mimeType == null || !mimeType.startsWith("image/")) {
                // Fallback: kiểm tra extension từ URI
                String uriString = imageUri.toString().toLowerCase();
                if (uriString.endsWith(".png")) {
                    mimeType = "image/png";
                } else if (uriString.endsWith(".jpg") || uriString.endsWith(".jpeg")) {
                    mimeType = "image/jpeg";
                } else if (uriString.endsWith(".gif")) {
                    mimeType = "image/gif";
                } else if (uriString.endsWith(".webp")) {
                    mimeType = "image/webp";
                } else {
                    mimeType = "image/jpeg"; // Default
                }
            }

            // Tạo tên file với extension đúng
            String fileName = "avatar";
            if (mimeType.contains("png")) {
                fileName = "avatar.png";
            } else if (mimeType.contains("jpeg") || mimeType.contains("jpg")) {
                fileName = "avatar.jpg";
            } else if (mimeType.contains("gif")) {
                fileName = "avatar.gif";
            } else if (mimeType.contains("webp")) {
                fileName = "avatar.webp";
            } else {
                fileName = "avatar.jpg"; // Default
            }

            // Tạo RequestBody từ byte array với MIME type đúng
            RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), imageBytes);
            MultipartBody.Part body = MultipartBody.Part.createFormData("image", fileName, requestFile);

            ApiClient.getApiService().uploadImage(body).enqueue(new Callback<UploadResponse>() {
                @Override
                public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        UploadResponse uploadResponse = response.body();
                        String avatarPath = uploadResponse.getPath();
                        // Sau khi upload thành công, lưu profile
                        saveProfileToServer(newName, avatarPath, token);
                    } else {
                        btnSave.setEnabled(true);
                        btnSave.setText("Lưu thông tin");
                        String errorMsg = "Lỗi upload ảnh";
                        if (response.errorBody() != null) {
                            try {
                                String errorStr = response.errorBody().string();
                                errorMsg += ": " + errorStr;
                            } catch (Exception e) {
                                errorMsg += ": " + response.message();
                            }
                        } else {
                            errorMsg += ": " + response.message();
                        }
                        Toast.makeText(EditProfileActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<UploadResponse> call, Throwable t) {
                    btnSave.setEnabled(true);
                    btnSave.setText("Lưu thông tin");
                    Toast.makeText(EditProfileActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            btnSave.setEnabled(true);
            btnSave.setText("Lưu thông tin");
            Toast.makeText(this, "Lỗi khi đọc ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveProfileToServer(String newName, String avatarPath, String token) {
        String authHeader = "Bearer " + token;
        UpdateProfileRequest request = new UpdateProfileRequest(newName, avatarPath);
        
        android.util.Log.d("EditProfile", "Saving profile - Name: " + newName + ", Avatar: " + avatarPath);
        android.util.Log.d("EditProfile", "API URL: http://10.0.2.2:3000/api/users/profile");
        android.util.Log.d("EditProfile", "Auth Header: " + (authHeader.length() > 20 ? authHeader.substring(0, 20) + "..." : authHeader));

        ApiClient.getApiService().updateProfile(authHeader, request).enqueue(new Callback<UpdateProfileResponse>() {
            @Override
            public void onResponse(Call<UpdateProfileResponse> call, Response<UpdateProfileResponse> response) {
                btnSave.setEnabled(true);
                android.util.Log.d("EditProfile", "Response code: " + response.code());
                android.util.Log.d("EditProfile", "Response isSuccessful: " + response.isSuccessful());
                
                if (response.isSuccessful() && response.body() != null) {
                    UpdateProfileResponse updateResponse = response.body();
                    UserDto updatedUser = updateResponse.getUser();
                    
                    // Cập nhật user trong UserManager
                    UserManager.saveSession(updatedUser, token);
                    
                    Toast.makeText(EditProfileActivity.this, "Cập nhật profile thành công!", Toast.LENGTH_SHORT).show();
                    
                    // Quay về ProfileActivity và refresh
                    Intent resultIntent = new Intent();
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } else {
                    btnSave.setText("Lưu thông tin");
                    String errorMsg = "Lỗi cập nhật";
                    android.util.Log.e("EditProfile", "Error response code: " + response.code());
                    
                    if (response.errorBody() != null) {
                        try {
                            String errorStr = response.errorBody().string();
                            android.util.Log.e("EditProfile", "Error body: " + errorStr);
                            
                            // Loại bỏ HTML tags nếu có
                            errorStr = errorStr.replaceAll("<[^>]*>", "").trim();
                            
                            if (errorStr.contains("Cannot PUT") || response.code() == 404) {
                                errorMsg = "Lỗi: Server chưa có endpoint này. Vui lòng restart server!";
                            } else if (response.code() == 401) {
                                errorMsg = "Lỗi: Token không hợp lệ. Vui lòng đăng nhập lại!";
                            } else {
                                errorMsg += " (" + response.code() + "): " + errorStr;
                            }
                        } catch (Exception e) {
                            android.util.Log.e("EditProfile", "Error reading error body", e);
                            errorMsg += " (" + response.code() + "): " + response.message();
                        }
                    } else {
                        errorMsg += " (" + response.code() + "): " + response.message();
                    }
                    Toast.makeText(EditProfileActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<UpdateProfileResponse> call, Throwable t) {
                btnSave.setEnabled(true);
                btnSave.setText("Lưu thông tin");
                Toast.makeText(EditProfileActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserInfo() {
        UserDto user = UserManager.getCurrentUser();
        if (user != null) {
            String name = user.getName() != null ? user.getName() : "Người dùng";
            String email = user.getEmail() != null ? user.getEmail() : "";
            String avatar = user.getAvatar();

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
                currentAvatarPath = avatar;
            } else {
                imgAvatar.setImageResource(R.drawable.ic_user);
                currentAvatarPath = null;
            }
        } else {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}

