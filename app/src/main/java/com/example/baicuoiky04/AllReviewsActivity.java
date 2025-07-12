// Dán toàn bộ code này để thay thế file AllReviewsActivity.java cũ

package com.example.baicuoiky04;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot; // <<< THÊM IMPORT NÀY ĐỂ SỬA LỖI

import java.util.ArrayList;
import java.util.List;

public class AllReviewsActivity extends AppCompatActivity {

    private static final String TAG = "AllReviews_DEBUG";

    private RecyclerView recyclerView;
    private ReviewAdapter adapter;
    private List<DataModels.Review> reviewList;
    private ProgressBar progressBar;
    private TextView textViewEmpty;

    private FirebaseFirestore db;
    private String userId, userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_reviews);

        userId = getIntent().getStringExtra("USER_ID");
        userName = getIntent().getStringExtra("USER_NAME");

        if (userId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy người dùng.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        initViews();
        setupRecyclerView();
        loadAllReviews();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewAllReviews);
        progressBar = findViewById(R.id.progressBar);
        textViewEmpty = findViewById(R.id.textViewEmpty);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Tất cả đánh giá của " + userName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        reviewList = new ArrayList<>();
        // Trong AllReviewsActivity, chúng ta không cần listener để báo cáo nên truyền null
        adapter = new ReviewAdapter(this, reviewList, null);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadAllReviews() {
        progressBar.setVisibility(View.VISIBLE);
        Log.d(TAG, "Bắt đầu tải TẤT CẢ review cho user: " + userId);

        db.collection("users").document(userId).collection("reviews")
                // .whereEqualTo("status", "visible") // Bỏ tạm để test
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    Log.d(TAG, "Query thành công! Số lượng document trả về: " + queryDocumentSnapshots.size());

                    reviewList.clear();
                    if (queryDocumentSnapshots.isEmpty()) {
                        textViewEmpty.setVisibility(View.VISIBLE);
                        Log.d(TAG, "Không có review nào để hiển thị.");
                    } else {
                        textViewEmpty.setVisibility(View.GONE);

                        // =================== SỬA LỖI Ở ĐÂY ===================
                        // Thay 'var' bằng 'QueryDocumentSnapshot'
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            // Bây giờ các phương thức toObject() và getId() sẽ được nhận dạng
                            DataModels.Review review = doc.toObject(DataModels.Review.class);
                            review.setId(doc.getId());
                            reviewList.add(review);
                        }
                        // =======================================================

                        Log.d(TAG, "Đã thêm " + reviewList.size() + " review vào adapter.");
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "LỖI KHI TẢI TẤT CẢ REVIEW: ", e);
                    Toast.makeText(this, "Lỗi tải đánh giá.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}