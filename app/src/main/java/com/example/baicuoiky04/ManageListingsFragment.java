// Dán toàn bộ code này để thay thế file ManageListingsFragment.java cũ

package com.example.baicuoiky04;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu; // Thêm import này nếu thiếu
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Arrays; // Thêm import này
import java.util.List;

public class ManageListingsFragment extends Fragment {
    private static final String TAG = "ManageListingsFragment";

    private RecyclerView recyclerView;
    private ManageListingAdapter adapter;
    private List<DataModels.Listing> myListings;
    private ProgressBar progressBar;
    private TextView textViewEmpty;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_listings, container, false);
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        recyclerView = view.findViewById(R.id.recyclerViewManageListings);
        progressBar = view.findViewById(R.id.progressBarManage);
        textViewEmpty = view.findViewById(R.id.textViewEmptyManage);
        setupRecyclerView();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentUser != null) {
            loadMyListings();
        }
    }

    private void setupRecyclerView() {
        myListings = new ArrayList<>();
        ManageListingAdapter.OnActionListener listener = new ManageListingAdapter.OnActionListener() {
            @Override
            public void onEdit(DataModels.Listing listing) {
                Intent intent = new Intent(getActivity(), AddListingActivity.class);
                intent.putExtra("LISTING_ID", listing.getListingId());
                startActivity(intent);
            }
            @Override
            public void onDelete(DataModels.Listing listing) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc chắn muốn xóa tin '" + listing.getTitle() + "' không?")
                        .setPositiveButton("Xóa", (dialog, which) -> deleteListing(listing))
                        .setNegativeButton("Hủy", null)
                        .show();
            }
            @Override
            public void onChangeStatus(DataModels.Listing listing) {
                showChangeStatusDialog(listing);
            }

            // ================== THÊM HÀM MỚI NÀY ==================
            @Override
            public void onArchive(DataModels.Listing listing) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Lưu trữ tin đăng")
                        .setMessage("Tin đăng sẽ được ẩn đi. Bạn có thể xem và khôi phục lại trong mục 'Tin đã lưu trữ' ở trang cá nhân. Bạn có chắc chắn không?")
                        .setPositiveButton("Lưu trữ", (dialog, which) -> updateListingStatus(listing, "archived"))
                        .setNegativeButton("Hủy", null)
                        .show();
            }
            // ========================================================
        };
        // Sửa hàm khởi tạo adapter
        adapter = new ManageListingAdapter(getContext(), myListings, listener);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void showChangeStatusDialog(DataModels.Listing listing) {
        final String[] statuses = {"available", "paused", "sold"};
        final String[] displayStatuses = {"Đang bán", "Tạm dừng", "Đã bán"};
        new AlertDialog.Builder(getContext())
                .setTitle("Chọn trạng thái mới")
                .setItems(displayStatuses, (dialog, which) -> {
                    String newStatus = statuses[which];
                    // Nếu người dùng chọn "Đã bán", cần có thêm bước chọn người mua
                    if ("sold".equals(newStatus)) {
                        Toast.makeText(getContext(), "Vui lòng chấp nhận một trả giá để đánh dấu là 'Đã bán'.", Toast.LENGTH_LONG).show();
                    } else {
                        updateListingStatus(listing, newStatus);
                    }
                })
                .create().show();
    }

    private void updateListingStatus(DataModels.Listing listing, String newStatus) {
        db.collection("listings").document(listing.getListingId())
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    String message = "Cập nhật trạng thái thành công!";
                    if ("archived".equals(newStatus)) {
                        message = "Đã lưu trữ tin đăng!";
                    }
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    loadMyListings();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void deleteListing(DataModels.Listing listing) {
        db.collection("listings").document(listing.getListingId()).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Đã xóa tin thành công", Toast.LENGTH_SHORT).show();
                    loadMyListings();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi khi xóa tin: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadMyListings() {
        if (currentUser == null) return;
        progressBar.setVisibility(View.VISIBLE);
        textViewEmpty.setVisibility(View.GONE);

        // ================== CẬP NHẬT CÂU QUERY Ở ĐÂY ==================
        // Chỉ hiển thị các tin đang hoạt động hoặc đã bán, không hiển thị tin lưu trữ
        List<String> statusesToView = Arrays.asList("available", "paused", "sold");

        db.collection("listings")
                .whereEqualTo("sellerId", currentUser.getUid())
                .whereIn("status", statusesToView) // Chỉ lấy các tin có status này
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    myListings.clear();
                    if (queryDocumentSnapshots.isEmpty()) {
                        textViewEmpty.setVisibility(View.VISIBLE);
                    } else {
                        myListings.addAll(queryDocumentSnapshots.toObjects(DataModels.Listing.class));
                    }
                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading listings", e);
                });
        // =============================================================
    }
}