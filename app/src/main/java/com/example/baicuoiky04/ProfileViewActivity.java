package com.example.baicuoiky04;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ProfileViewActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_view);

        String userId = getIntent().getStringExtra("USER_ID");

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không tìm thấy người dùng.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Đặt tiêu đề cho Activity
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Trang cá nhân");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Dùng ProfileFragment.newInstance để truyền userId vào
        ProfileFragment profileFragment = ProfileFragment.newInstance(userId);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_profile, profileFragment)
                .commit();
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Xử lý nút back trên action bar
        onBackPressed();
        return true;
    }
}