package com.example.baicuoiky04;

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
        };
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
                    updateListingStatus(listing, newStatus);
                })
                .create().show();
    }

    private void updateListingStatus(DataModels.Listing listing, String newStatus) {
        db.collection("listings").document(listing.getListingId())
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Cập nhật trạng thái thành công!", Toast.LENGTH_SHORT).show();
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

        db.collection("listings")
                .whereEqualTo("sellerId", currentUser.getUid())
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