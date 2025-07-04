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

import java.util.ArrayList;
import java.util.List;

public class ArchivedListingsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ArchivedListingAdapter adapter;
    private List<DataModels.Listing> archivedList;
    private ProgressBar progressBar;
    private TextView textViewEmpty;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archived_listings);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        initViews();
        setupRecyclerView();
        loadArchivedListings();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewArchived);
        progressBar = findViewById(R.id.progressBarArchived);
        textViewEmpty = findViewById(R.id.textViewEmptyArchived);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Tin đã lưu trữ");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        archivedList = new ArrayList<>();
        adapter = new ArchivedListingAdapter(this, archivedList, listing -> {
            restoreListing(listing);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadArchivedListings() {
        if (currentUser == null) return;
        progressBar.setVisibility(View.VISIBLE);

        db.collection("listings")
                .whereEqualTo("sellerId", currentUser.getUid())
                .whereEqualTo("status", "archived")
                .orderBy("lastUpdatedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    archivedList.clear();
                    if (queryDocumentSnapshots.isEmpty()) {
                        textViewEmpty.setVisibility(View.VISIBLE);
                    } else {
                        textViewEmpty.setVisibility(View.GONE);
                        archivedList.addAll(queryDocumentSnapshots.toObjects(DataModels.Listing.class));
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void restoreListing(DataModels.Listing listing) {
        // Khôi phục về trạng thái "Tạm dừng" để người bán tự quyết định
        db.collection("listings").document(listing.getListingId())
                .update("status", "paused")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã khôi phục tin đăng!", Toast.LENGTH_SHORT).show();
                    // Tải lại danh sách sau khi khôi phục
                    loadArchivedListings();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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