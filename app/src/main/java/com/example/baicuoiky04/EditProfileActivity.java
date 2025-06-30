package com.example.baicuoiky04;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    // *** THAY ĐỔI Ở ĐÂY: Khai báo thêm editTextContactInfo ***
    private TextInputEditText editTextDisplayName, editTextBio, editTextContactInfo;
    private MaterialButton buttonSaveChanges;
    private ProgressBar progressBar;
    private MaterialToolbar toolbar;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        initViews();
        loadCurrentUserData();
        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        editTextDisplayName = findViewById(R.id.editTextDisplayName);
        editTextBio = findViewById(R.id.editTextBio);
        // *** THÊM VÀO ĐÂY: Ánh xạ editTextContactInfo ***
        editTextContactInfo = findViewById(R.id.editTextContactInfo);
        buttonSaveChanges = findViewById(R.id.buttonSaveChanges);
        progressBar = findViewById(R.id.progressBar);
    }

    private void loadCurrentUserData() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            editTextDisplayName.setText(extras.getString("CURRENT_DISPLAY_NAME"));
            editTextBio.setText(extras.getString("CURRENT_BIO"));
            // *** THÊM VÀO ĐÂY: Lấy và hiển thị contact info ***
            editTextContactInfo.setText(extras.getString("CURRENT_CONTACT_INFO"));
        }
    }

    private void setupListeners() {
        toolbar.setNavigationOnClickListener(v -> finish());
        buttonSaveChanges.setOnClickListener(v -> saveProfileChanges());
    }

    private void saveProfileChanges() {
        String newDisplayName = editTextDisplayName.getText().toString().trim();
        String newBio = editTextBio.getText().toString().trim();
        // *** THÊM VÀO ĐÂY: Lấy giá trị từ ô contact info ***
        String newContactInfo = editTextContactInfo.getText().toString().trim();

        if (TextUtils.isEmpty(newDisplayName)) {
            editTextDisplayName.setError("Tên hiển thị không được để trống.");
            return;
        }

        if (currentUser == null) {
            Toast.makeText(this, "Lỗi xác thực, vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        Map<String, Object> updates = new HashMap<>();
        updates.put("displayName", newDisplayName);
        updates.put("bio", newBio);
        // *** THÊM VÀO ĐÂY: Thêm contact info vào map để update ***
        updates.put("contactInfo", newContactInfo);

        db.collection("users").document(currentUser.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    Toast.makeText(EditProfileActivity.this, "Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(EditProfileActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            buttonSaveChanges.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            buttonSaveChanges.setEnabled(true);
        }
    }
}