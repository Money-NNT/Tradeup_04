package com.example.baicuoiky04;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9001;

    private TextInputEditText editTextEmail, editTextPassword;
    private MaterialButton buttonLogin, buttonGoogleSignIn;
    private TextView textViewRegisterLink, textViewForgotPassword;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // --- Khởi tạo ---
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // --- Ánh xạ Views ---
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonGoogleSignIn = findViewById(R.id.buttonGoogleSignIn);
        textViewRegisterLink = findViewById(R.id.textViewRegisterLink);
        textViewForgotPassword = findViewById(R.id.textViewForgotPassword);

        // --- Cấu hình Google Sign In ---
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // --- Thiết lập sự kiện ---
        setupListeners();
    }

    private void setupListeners() {
        // Vô hiệu hóa nút Đăng nhập
        buttonLogin.setEnabled(false);
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String emailInput = editTextEmail.getText().toString().trim();
                String passwordInput = editTextPassword.getText().toString().trim();
                buttonLogin.setEnabled(!emailInput.isEmpty() && !passwordInput.isEmpty());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };
        editTextEmail.addTextChangedListener(textWatcher);
        editTextPassword.addTextChangedListener(textWatcher);

        buttonLogin.setOnClickListener(v -> loginWithEmail());
        buttonGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
        textViewRegisterLink.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
        textViewForgotPassword.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class)));
    }

    private void loginWithEmail() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        executor.execute(() -> {
            Log.d(TAG, "Bắt đầu đăng nhập email: " + System.currentTimeMillis());
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        runOnUiThread(() -> {
                            Log.d(TAG, "Kết thúc đăng nhập email: " + System.currentTimeMillis());
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null && user.isEmailVerified()) {
                                    navigateToMain();
                                } else {
                                    mAuth.signOut();
                                    Toast.makeText(LoginActivity.this, "Vui lòng xác thực email trước khi đăng nhập.", Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
        });
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Google Sign-In thất bại. Mã lỗi: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        executor.execute(() -> {
            Log.d(TAG, "Bắt đầu xác thực Google: " + System.currentTimeMillis());
            mAuth.signInWithCredential(credential)
                    .addOnCompleteListener(task -> {
                        runOnUiThread(() -> {
                            Log.d(TAG, "Kết thúc xác thực Google: " + System.currentTimeMillis());
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                checkIfUserExists(user);
                            } else {
                                Toast.makeText(LoginActivity.this, "Xác thực Firebase thất bại.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
        });
    }

    private void checkIfUserExists(FirebaseUser user) {
        if (user == null) return;

        executor.execute(() -> {
            Log.d(TAG, "Bắt đầu kiểm tra user Firestore: " + System.currentTimeMillis());
            db.collection("users").document(user.getUid()).get()
                    .addOnCompleteListener(task -> {
                        runOnUiThread(() -> {
                            Log.d(TAG, "Kết thúc kiểm tra user Firestore: " + System.currentTimeMillis());
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    Log.d(TAG, "User exists in Firestore. Logging in.");
                                    navigateToMain();
                                } else {
                                    Log.d(TAG, "User does not exist. Creating new profile.");
                                    createNewUserInFirestore(user);
                                }
                            } else {
                                Log.e(TAG, "Failed to check user existence: ", task.getException());
                                Toast.makeText(this, "Không thể kiểm tra thông tin người dùng.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
        });
    }

    private void createNewUserInFirestore(FirebaseUser user) {
        DataModels.User newUser = new DataModels.User();
        newUser.setUid(user.getUid());
        newUser.setEmail(user.getEmail());
        newUser.setDisplayName(user.getDisplayName());
        newUser.setPhotoUrl(user.getPhotoUrl() != null ? user.getPhotoUrl().toString().replace("s96-c", "s400-c") : "");
        newUser.setAccountStatus("active");
        newUser.setAverageRating(0);
        newUser.setTotalReviews(0);
        newUser.setTotalTransactions(0);

        executor.execute(() -> {
            Log.d(TAG, "Bắt đầu tạo user Firestore: " + System.currentTimeMillis());
            db.collection("users").document(user.getUid()).set(newUser)
                    .addOnSuccessListener(aVoid -> {
                        runOnUiThread(() -> {
                            Log.d(TAG, "Kết thúc tạo user Firestore: " + System.currentTimeMillis());
                            Log.d(TAG, "New user profile created in Firestore.");
                            navigateToMain();
                        });
                    })
                    .addOnFailureListener(e -> {
                        runOnUiThread(() -> {
                            Log.e(TAG, "Error creating user profile", e);
                            Toast.makeText(this, "Không thể tạo hồ sơ người dùng.", Toast.LENGTH_SHORT).show();
                        });
                    });
        });
    }

    private void navigateToMain() {
        Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_CTRL_LEFT) {
            Log.d(TAG, "Ctrl Left pressed, ignoring to prevent ANR");
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}