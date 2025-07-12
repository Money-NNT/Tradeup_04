package com.example.baicuoiky04;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

public class CreateProfileActivity extends AppCompatActivity {

    private static final String TAG = "CreateProfileActivity";

    private TextInputEditText editTextDisplayName;
    private MaterialButton buttonCompleteProfile;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_profile);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        editTextDisplayName = findViewById(R.id.editTextDisplayName);
        buttonCompleteProfile = findViewById(R.id.buttonCompleteProfile);

        buttonCompleteProfile.setOnClickListener(v -> completeProfile());
    }

    private void completeProfile() {
        final String displayName = editTextDisplayName.getText().toString().trim();
        final FirebaseUser currentUser = mAuth.getCurrentUser();

        if (TextUtils.isEmpty(displayName)) {
            editTextDisplayName.setError("Tên không được để trống.");
            return;
        }

        if (currentUser == null) {
            Toast.makeText(this, "Lỗi xác thực, vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setLoadingState(true);

        // BƯỚC 1: Cập nhật displayName trong Firebase Authentication
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build();

        currentUser.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "BƯỚC 1 THÀNH CÔNG: Cập nhật tên trong Firebase Auth.");

                // BƯỚC 2: Cập nhật displayName trong Cloud Firestore
                db.collection("users").document(currentUser.getUid())
                        .update("displayName", displayName)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "BƯỚC 2 THÀNH CÔNG: Cập nhật tên trong Firestore.");
                            Toast.makeText(CreateProfileActivity.this, "Hồ sơ đã được tạo! Chào mừng bạn!", Toast.LENGTH_LONG).show();

                            // Chuyển thẳng vào MainActivity
                            Intent intent = new Intent(CreateProfileActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "BƯỚC 2 THẤT BẠI: Lỗi khi cập nhật Firestore.", e);
                            Toast.makeText(this, "Lỗi khi lưu hồ sơ.", Toast.LENGTH_SHORT).show();
                            setLoadingState(false);
                        });
            } else {
                Log.e(TAG, "BƯỚC 1 THẤT BẠI: Lỗi khi cập nhật Firebase Auth.", task.getException());
                Toast.makeText(this, "Lỗi khi cập nhật hồ sơ.", Toast.LENGTH_SHORT).show();
                setLoadingState(false);
            }
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