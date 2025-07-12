package com.example.baicuoiky04;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewLoginLink = findViewById(R.id.textViewLoginLink);
        progressBar = findViewById(R.id.progressBar);

        buttonRegister.setOnClickListener(v -> registerUserWithEmail());

        textViewLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUserWithEmail() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (!validateInput(email, password)) {
            return;
        }

        setLoadingState(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            sendVerificationAndCreateUserDocument(firebaseUser);
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this, "Đăng ký thất bại: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        setLoadingState(false);
                    }
                });
    }

    private void sendVerificationAndCreateUserDocument(FirebaseUser firebaseUser) {
        firebaseUser.sendEmailVerification();

        DataModels.User newUser = new DataModels.User();
        newUser.setUid(firebaseUser.getUid());
        newUser.setEmail(firebaseUser.getEmail());
        newUser.setDisplayName(""); // Tạm thời để trống
        newUser.setPhotoUrl("");
        newUser.setAccountStatus("active");

        db.collection("users").document(firebaseUser.getUid())
                .set(newUser)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "Đăng ký thành công! Vui lòng kiểm tra email để xác thực.", Toast.LENGTH_LONG).show();

                    // Chuyển sang CreateProfileActivity và mang theo UID
                    Intent intent = new Intent(RegisterActivity.this, CreateProfileActivity.class);
                    // Không cần truyền gì cả, CreateProfileActivity sẽ tự lấy từ mAuth
                    startActivity(intent);
                    finishAffinity(); // Đóng tất cả các activity trước đó
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Lỗi khi lưu thông tin người dùng.", Toast.LENGTH_SHORT).show();
                    setLoadingState(false);
                });
    }

    private boolean validateInput(String email, String password) {
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Vui lòng nhập email hợp lệ.");
            return false;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            editTextPassword.setError("Mật khẩu phải có ít nhất 6 ký tự.");
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