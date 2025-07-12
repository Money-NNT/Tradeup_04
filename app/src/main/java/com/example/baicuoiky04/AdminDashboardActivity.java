// Dán toàn bộ code này để thay thế file AdminDashboardActivity.java cũ

package com.example.baicuoiky04;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {

    private static final String TAG = "AdminDashboardActivity";
    private RecyclerView recyclerView;
    private ReportAdapter adapter;
    private List<DataModels.Report> reportList;
    private List<String> reportIdList;
    private ProgressBar progressBar;
    private TextView textViewEmpty;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        db = FirebaseFirestore.getInstance();
        initViews();
        setupRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReports(); // Tải lại dữ liệu mỗi khi quay lại màn hình
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewReports);
        progressBar = findViewById(R.id.progressBar);
        textViewEmpty = findViewById(R.id.textViewEmpty);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Admin - Quản lý Báo cáo");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        reportList = new ArrayList<>();
        reportIdList = new ArrayList<>();

        // **QUAN TRỌNG**: Đảm bảo bạn đã cập nhật file ReportAdapter.java
        // để gọi đúng các hàm listener này, đặc biệt là hàm onRemoveReview mới.
        adapter = new ReportAdapter(this, reportList, reportIdList, new ReportAdapter.OnReportActionListener() {
            @Override
            public void onViewContent(DataModels.Report report) {
                if (!TextUtils.isEmpty(report.getReportedListingId())) {
                    Intent intent = new Intent(AdminDashboardActivity.this, ListingDetailActivity.class);
                    intent.putExtra("LISTING_ID", report.getReportedListingId());
                    startActivity(intent);
                } else if (!TextUtils.isEmpty(report.getReportedUserId())) {
                    Intent intent = new Intent(AdminDashboardActivity.this, ProfileViewActivity.class);
                    intent.putExtra("USER_ID", report.getReportedUserId());
                    startActivity(intent);
                }
                // Nếu là báo cáo review, có thể xem profile của người viết review
                else if (!TextUtils.isEmpty(report.getReportedReviewId())) {
                    Intent intent = new Intent(AdminDashboardActivity.this, ProfileViewActivity.class);
                    intent.putExtra("USER_ID", report.getReportedUserId());
                    startActivity(intent);
                }
            }

            @Override
            public void onSuspendUser(String userId) {
                toggleUserSuspension(userId, true);
            }

            @Override
            public void onUnsuspendUser(String userId) {
                toggleUserSuspension(userId, false);
            }

            // ================== HÀM MỚI ĐƯỢC IMPLEMENT ==================
            // Chữ ký hàm có thể được điều chỉnh để truyền cả report và reportId cho tiện
            @Override
            public void onRemoveReview(DataModels.Report report, String reportId) {
                // Hiển thị dialog xác nhận trước khi thực hiện hành động
                new AlertDialog.Builder(AdminDashboardActivity.this)
                        .setTitle("Xác nhận xóa Review")
                        .setMessage("Bạn có chắc muốn ẩn/xóa đánh giá này? Review sẽ không còn hiển thị với người dùng.")
                        .setPositiveButton("Xóa Review", (dialog, which) -> {
                            // Cập nhật status của review thành 'removed_by_admin'
                            db.collection("users").document(report.getReportedUserId()).collection("reviews")
                                    .document(report.getReportedReviewId())
                                    .update("status", "removed_by_admin")
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(AdminDashboardActivity.this, "Đã xóa/ẩn review.", Toast.LENGTH_SHORT).show();
                                        // Sau khi xóa review thành công, xóa luôn report này
                                        onDismissReport(reportId);
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(AdminDashboardActivity.this, "Lỗi khi xóa review: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }
            // =============================================================

            @Override
            public void onDismissReport(String reportId) {
                db.collection("reports").document(reportId).delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(AdminDashboardActivity.this, "Đã bỏ qua báo cáo.", Toast.LENGTH_SHORT).show();
                            loadReports(); // Tải lại danh sách
                        });
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadReports() {
        progressBar.setVisibility(View.VISIBLE);
        textViewEmpty.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        db.collection("reports").orderBy("createdAt", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        textViewEmpty.setVisibility(View.VISIBLE);
                        reportList.clear();
                        reportIdList.clear();
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    List<Task<DocumentSnapshot>> userTasks = new ArrayList<>();
                    List<DataModels.Report> tempReportList = new ArrayList<>();
                    List<String> tempReportIdList = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        DataModels.Report report = doc.toObject(DataModels.Report.class);
                        if (!TextUtils.isEmpty(report.getReportedUserId())) {
                            userTasks.add(db.collection("users").document(report.getReportedUserId()).get());
                        } else {
                            // Nếu report không có reportedUserId, thêm một task rỗng để giữ đúng thứ tự
                            userTasks.add(Tasks.forResult(null));
                        }
                        tempReportList.add(report);
                        tempReportIdList.add(doc.getId());
                    }

                    Tasks.whenAllSuccess(userTasks).addOnSuccessListener(userSnapshots -> {
                        reportList.clear();
                        reportIdList.clear();
                        for (int i = 0; i < tempReportList.size(); i++) {
                            DataModels.Report report = tempReportList.get(i);
                            DocumentSnapshot userSnapshot = (DocumentSnapshot) userSnapshots.get(i);
                            if (userSnapshot != null && userSnapshot.exists()) {
                                report.setReportedUserObject(userSnapshot.toObject(DataModels.User.class));
                            }
                            reportList.add(report);
                            reportIdList.add(tempReportIdList.get(i));
                        }
                        progressBar.setVisibility(View.GONE);
                        if(reportList.isEmpty()) {
                            textViewEmpty.setVisibility(View.VISIBLE);
                        } else {
                            textViewEmpty.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                        }
                        adapter.notifyDataSetChanged();
                    });

                }).addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    textViewEmpty.setVisibility(View.VISIBLE);
                    Log.e(TAG, "Error loading reports", e);
                });
    }

    private void toggleUserSuspension(String userId, boolean suspend) {
        if (TextUtils.isEmpty(userId)) return;

        String newStatus = suspend ? "suspended" : "active";
        String title = suspend ? "Xác nhận khóa tài khoản" : "Xác nhận mở khóa tài khoản";
        String message = "Bạn có chắc muốn " + (suspend ? "khóa" : "mở khóa") + " tài khoản này?";
        String buttonText = suspend ? "Khóa" : "Mở khóa";

        new AlertDialog.Builder(AdminDashboardActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(buttonText, (dialog, which) -> {
                    db.collection("users").document(userId)
                            .update("accountStatus", newStatus)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(AdminDashboardActivity.this, "Đã " + buttonText.toLowerCase() + " tài khoản.", Toast.LENGTH_SHORT).show();
                                loadReports(); // Tải lại để cập nhật trạng thái nút
                            })
                            .addOnFailureListener(e -> Toast.makeText(AdminDashboardActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Hủy", null).show();
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