package com.example.baicuoiky04;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class CreateProfileActivity extends AppCompatActivity {

    private static final String TAG = "CreateProfileActivity";

    private TextInputEditText editTextDisplayName;
    private MaterialButton buttonCompleteProfile;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_profile);

        Log.d(TAG, "onCreate: Activity đã được tạo.");
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        editTextDisplayName = findViewById(R.id.editTextDisplayName);
        buttonCompleteProfile = findViewById(R.id.buttonCompleteProfile);
        // BỎ QUA progressBar VÌ KHÔNG CÓ TRONG LAYOUT CỦA BẠN
        // progressBar = findViewById(R.id.progressBar);

        // *** DÒNG CODE SỬA LỖI ĐƯỢC THÊM VÀO ĐÂY ***
        buttonCompleteProfile.setOnClickListener(v -> completeProfile());
    }

    private void completeProfile() {
        Log.d(TAG, "completeProfile: Nút hoàn tất được nhấn.");
        String displayName = editTextDisplayName.getText().toString().trim();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (TextUtils.isEmpty(displayName)) {
            editTextDisplayName.setError("Tên không được để trống.");
            Log.w(TAG, "completeProfile: Tên hiển thị rỗng.");
            return;
        }

        if (currentUser == null) {
            Log.e(TAG, "completeProfile: Lỗi nghiêm trọng, user hiện tại là null.");
            Toast.makeText(this, "Lỗi xác thực, vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setLoadingState(true);

        Log.d(TAG, "completeProfile: Bắt đầu cập nhật displayName trên Firestore cho user: " + currentUser.getUid());
        db.collection("users").document(currentUser.getUid())
                .update("displayName", displayName)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "update:success - Cập nhật tên thành công.");
                    Toast.makeText(CreateProfileActivity.this, "Hồ sơ đã được tạo! Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();

                    mAuth.signOut();
                    Log.d(TAG, "signOut:success - Đăng xuất người dùng để họ đăng nhập lại.");

                    Intent intent = new Intent(CreateProfileActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "update:failure - Lỗi khi cập nhật tên.", e);
                    Toast.makeText(CreateProfileActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    setLoadingState(false);
                });
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            buttonCompleteProfile.setEnabled(false);
            buttonCompleteProfile.setText("Đang lưu...");
        } else {
            buttonCompleteProfile.setEnabled(true);
            buttonCompleteProfile.setText("Hoàn tất");
        }
    }
}