package com.example.baicuoiky04;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText editTextEmail;
    private MaterialButton buttonSendResetEmail;
    private TextView textViewBackToLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();

        editTextEmail = findViewById(R.id.editTextEmail);
        buttonSendResetEmail = findViewById(R.id.buttonSendResetEmail);
        textViewBackToLogin = findViewById(R.id.textViewBackToLogin);

        buttonSendResetEmail.setOnClickListener(v -> sendPasswordResetEmail());

        textViewBackToLogin.setOnClickListener(v -> finish()); // Đơn giản là đóng Activity này lại
    }

    private void sendPasswordResetEmail() {
        String email = editTextEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Vui lòng nhập email hợp lệ.");
            editTextEmail.requestFocus();
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ForgotPasswordActivity.this, "Email khôi phục mật khẩu đã được gửi. Vui lòng kiểm tra hộp thư của bạn.", Toast.LENGTH_LONG).show();
                        // Tự động đóng màn hình này sau khi gửi thành công
                        finish();
                    } else {
                        Toast.makeText(ForgotPasswordActivity.this, "Lỗi: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}