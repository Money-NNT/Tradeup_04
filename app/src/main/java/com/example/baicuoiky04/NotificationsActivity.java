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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private static final String TAG = "NotificationsActivity"; // Tag để lọc log

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<DataModels.AppNotification> notificationList;
    private List<String> notificationIdList;
    private ProgressBar progressBar;
    private TextView textViewEmpty;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Thông báo");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        recyclerView = findViewById(R.id.recyclerViewNotifications);
        progressBar = findViewById(R.id.progressBar);
        textViewEmpty = findViewById(R.id.textViewEmpty);

        setupRecyclerView();
        loadNotifications();
    }

    private void setupRecyclerView() {
        notificationList = new ArrayList<>();
        notificationIdList = new ArrayList<>();
        adapter = new NotificationAdapter(this, notificationList, notificationIdList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadNotifications() {
        if (currentUser == null) {
            textViewEmpty.setText("Vui lòng đăng nhập để xem thông báo.");
            textViewEmpty.setVisibility(View.VISIBLE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        textViewEmpty.setVisibility(View.GONE);
        Log.d(TAG, "Bắt đầu tải thông báo cho user: " + currentUser.getUid());

        db.collection("notifications")
                .whereEqualTo("userId", currentUser.getUid())
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (isDestroyed()) return;

                    Log.d(TAG, "Query thành công! Số lượng document trả về: " + snapshots.size());

                    progressBar.setVisibility(View.GONE);
                    notificationList.clear();
                    notificationIdList.clear();

                    if (snapshots.isEmpty()) {
                        Log.d(TAG, "Không có thông báo nào để hiển thị.");
                        textViewEmpty.setVisibility(View.VISIBLE);
                    } else {
                        textViewEmpty.setVisibility(View.GONE);
                        for (QueryDocumentSnapshot doc : snapshots) {
                            notificationList.add(doc.toObject(DataModels.AppNotification.class));
                            notificationIdList.add(doc.getId());
                        }
                        Log.d(TAG, "Đã thêm " + notificationList.size() + " thông báo vào adapter.");
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isDestroyed()) return;
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Lỗi khi tải thông báo: ", e);
                    textViewEmpty.setText("Lỗi tải thông báo. Vui lòng kiểm tra Logcat.");
                    textViewEmpty.setVisibility(View.VISIBLE);
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