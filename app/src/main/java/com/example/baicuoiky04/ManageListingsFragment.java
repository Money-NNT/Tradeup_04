package com.example.baicuoiky04;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.Arrays;
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
                        .setMessage("Bạn có chắc chắn muốn xóa vĩnh viễn tin '" + listing.getTitle() + "' không? Hành động này không thể hoàn tác.")
                        .setPositiveButton("Xóa vĩnh viễn", (dialog, which) -> deleteListing(listing))
                        .setNegativeButton("Hủy", null)
                        .show();
            }
            @Override
            public void onChangeStatus(DataModels.Listing listing) {
                showChangeStatusDialog(listing);
            }

            @Override
            public void onArchive(DataModels.Listing listing) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Lưu trữ tin đăng")
                        .setMessage("Tin đăng sẽ được ẩn đi. Bạn có thể xem và khôi phục lại trong mục 'Tin đã lưu trữ' ở trang cá nhân. Bạn có chắc chắn không?")
                        .setPositiveButton("Lưu trữ", (dialog, which) -> updateListingStatus(listing, "archived"))
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        };
        adapter = new ManageListingAdapter(getContext(), myListings, listener);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    // ============= SỬA LẠI HOÀN TOÀN HÀM NÀY =============
    private void showChangeStatusDialog(final DataModels.Listing listing) {
        // Chỉ cho phép đổi các trạng thái này
        final String[] statuses = {"available", "paused", "sold"};
        final String[] displayStatuses = {"Đang bán", "Tạm dừng", "Đánh dấu là Đã bán"};

        new AlertDialog.Builder(getContext())
                .setTitle("Chọn trạng thái mới cho tin")
                .setItems(displayStatuses, (dialog, which) -> {
                    String newStatus = statuses[which];

                    // Nếu người dùng chọn "Đã bán"
                    if ("sold".equals(newStatus)) {
                        // Hỏi thêm một lần nữa để xác nhận
                        new AlertDialog.Builder(getContext())
                                .setTitle("Xác nhận đã bán?")
                                .setMessage("Hành động này sẽ ẩn tin đăng khỏi trang chủ và không thể bán lại. Bạn chắc chắn chứ?")
                                .setPositiveButton("Xác nhận", (confirmDialog, confirmWhich) -> {
                                    // Cập nhật status thành "sold" và buyerId là một giá trị đặc biệt
                                    updateListingStatus(listing, "sold");
                                })
                                .setNegativeButton("Hủy", null)
                                .show();
                    } else {
                        // Nếu chọn các trạng thái khác ("available", "paused") thì cập nhật ngay
                        updateListingStatus(listing, newStatus);
                    }
                })
                .create().show();
    }

    // ============= SỬA LẠI HÀM NÀY ĐỂ XỬ LÝ TRƯỜNG HỢP "SOLD" =============
    private void updateListingStatus(DataModels.Listing listing, String newStatus) {
        if ("sold".equals(newStatus)) {
            // Khi bán thủ công, không có người mua cụ thể trên app
            // Chúng ta có thể set buyerId là một chuỗi đặc biệt để nhận biết
            db.collection("listings").document(listing.getListingId())
                    .update("status", "sold", "buyerId", "sold_manually")
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Đã cập nhật trạng thái thành 'Đã bán'!", Toast.LENGTH_SHORT).show();
                        loadMyListings();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            // Các trường hợp khác (available, paused, archived)
            db.collection("listings").document(listing.getListingId())
                    .update("status", newStatus)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Cập nhật trạng thái thành công!", Toast.LENGTH_SHORT).show();
                        loadMyListings();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
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

        List<String> statusesToView = Arrays.asList("available", "paused", "sold");
        db.collection("listings")
                .whereEqualTo("sellerId", currentUser.getUid())
                .whereIn("status", statusesToView)
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
    }
}