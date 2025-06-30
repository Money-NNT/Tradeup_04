package com.example.baicuoiky04;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    public static final String HISTORY_TYPE = "HISTORY_TYPE";
    public static final String TYPE_PURCHASE = "PURCHASE";
    public static final String TYPE_SALES = "SALES";
    // Thêm các loại khác nếu cần

    private RecyclerView recyclerView;
    private ListingAdapter adapter; // Tái sử dụng ListingAdapter
    private List<DataModels.Listing> historyList;
    private ProgressBar progressBar;
    private TextView textViewEmpty;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String historyType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        historyType = getIntent().getStringExtra(HISTORY_TYPE);
        if (historyType == null) {
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        initViews();
        setupRecyclerView();
        loadHistory();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewHistory);
        progressBar = findViewById(R.id.progressBar);
        textViewEmpty = findViewById(R.id.textViewEmpty);

        String title = "Lịch sử";
        if (TYPE_PURCHASE.equals(historyType)) title = "Lịch sử mua hàng";
        else if (TYPE_SALES.equals(historyType)) title = "Lịch sử bán hàng";

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        historyList = new ArrayList<>();
        adapter = new ListingAdapter(this, historyList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadHistory() {
        if (currentUser == null) return;

        progressBar.setVisibility(View.VISIBLE);
        textViewEmpty.setVisibility(View.GONE);

        Query query = db.collection("listings");

        // Xây dựng câu truy vấn dựa trên loại lịch sử
        if (TYPE_PURCHASE.equals(historyType)) {
            // FR-9.2.1: Purchase history
            query = query.whereEqualTo("buyerId", currentUser.getUid());
        } else if (TYPE_SALES.equals(historyType)) {
            // FR-9.1.2: Sales History
            query = query.whereEqualTo("sellerId", currentUser.getUid())
                    .whereEqualTo("status", "sold");
        } else {
            // Loại không xác định, không làm gì cả
            progressBar.setVisibility(View.GONE);
            return;
        }

        query.orderBy("lastUpdatedAt", Query.Direction.DESCENDING).get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        historyList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            historyList.add(document.toObject(DataModels.Listing.class));
                        }
                        adapter.notifyDataSetChanged();
                        if (historyList.isEmpty()) {
                            textViewEmpty.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Toast.makeText(this, "Lỗi tải lịch sử.", Toast.LENGTH_SHORT).show();
                    }
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