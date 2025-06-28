package com.example.baicuoiky04;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private TextInputEditText editTextEmail, editTextPassword;
    private MaterialButton buttonRegister;
    private TextView textViewLoginLink;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Log.d(TAG, "onCreate: Activity đã được tạo.");

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewLoginLink = findViewById(R.id.textViewLoginLink);
        progressBar = findViewById(R.id.progressBar);

        buttonRegister.setOnClickListener(v -> registerUserWithEmail());

        textViewLoginLink.setOnClickListener(v -> {
            Log.d(TAG, "onCreate: Chuyển đến LoginActivity.");
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUserWithEmail() {
        Log.d(TAG, "registerUserWithEmail: Nút đăng ký được nhấn.");
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // B1: Validation
        if (!validateInput(email, password)) {
            return;
        }

        // B2: Cập nhật UI
        setLoadingState(true);

        // B3: Gọi Firebase Auth
        Log.d(TAG, "registerUserWithEmail: Gọi mAuth.createUserWithEmailAndPassword...");
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success - Đăng ký Auth thành công.");
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            sendVerificationAndCreateUser(firebaseUser);
                        }
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(RegisterActivity.this, "Đăng ký thất bại: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        setLoadingState(false);
                    }
                });
    }

    private void sendVerificationAndCreateUser(FirebaseUser firebaseUser) {
        // B4: Gửi email xác thực
        firebaseUser.sendEmailVerification()
                .addOnCompleteListener(verificationTask -> {
                    if (verificationTask.isSuccessful()) {
                        Log.d(TAG, "sendEmailVerification:success - Gửi email xác thực thành công.");
                    } else {
                        Log.e(TAG, "sendEmailVerification:failure", verificationTask.getException());
                    }
                });

        // B5: Tạo document trên Firestore
        Log.d(TAG, "sendVerificationAndCreateUser: Tạo document cho user UID: " + firebaseUser.getUid());
        DataModels.User newUser = new DataModels.User();
        newUser.setUid(firebaseUser.getUid());
        newUser.setEmail(firebaseUser.getEmail());
        newUser.setDisplayName(""); // Để trống, sẽ cập nhật ở màn hình sau
        newUser.setPhotoUrl("");
        newUser.setAccountStatus("active");

        db.collection("users").document(firebaseUser.getUid())
                .set(newUser)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Firestore:success - Tạo user document thành công.");
                    Toast.makeText(RegisterActivity.this, "Đăng ký thành công! Vui lòng kiểm tra email để xác thực.", Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(RegisterActivity.this, CreateProfileActivity.class);
                    startActivity(intent);
                    finishAffinity();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore:failure - Lỗi khi tạo user document.", e);
                    Toast.makeText(RegisterActivity.this, "Lỗi khi lưu thông tin người dùng.", Toast.LENGTH_SHORT).show();
                    setLoadingState(false);
                });
    }

    private boolean validateInput(String email, String password) {
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Vui lòng nhập email hợp lệ.");
            editTextEmail.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            editTextPassword.setError("Mật khẩu phải có ít nhất 6 ký tự.");
            editTextPassword.requestFocus();
            return false;
        }
        return true;
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            buttonRegister.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            buttonRegister.setEnabled(true);
        }
    }
}