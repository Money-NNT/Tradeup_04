package com.example.baicuoiky04;

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

        if (currentUser != null) {
            loadMyListings();
        }

        return view;
    }

    private void setupRecyclerView() {
        myListings = new ArrayList<>();
        ManageListingAdapter.OnActionListener listener = new ManageListingAdapter.OnActionListener() {
            @Override
            public void onEdit(DataModels.Listing listing) {
                Toast.makeText(getContext(), "Chức năng Sửa đang phát triển", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDelete(DataModels.Listing listing) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc chắn muốn xóa tin '" + listing.getTitle() + "' không? Hành động này không thể hoàn tác.")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            deleteListing(listing);
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }

            @Override
            public void onChangeStatus(DataModels.Listing listing) {
                Toast.makeText(getContext(), "Chức năng Đổi trạng thái đang phát triển", Toast.LENGTH_SHORT).show();
            }
        };

        adapter = new ManageListingAdapter(getContext(), myListings, listener);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void deleteListing(DataModels.Listing listing) {
        db.collection("listings").document(listing.getListingId()).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Đã xóa tin thành công", Toast.LENGTH_SHORT).show();
                    loadMyListings(); // Tải lại danh sách
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi khi xóa tin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadMyListings() {
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
                        for (DataModels.Listing listing : queryDocumentSnapshots.toObjects(DataModels.Listing.class)) {
                            myListings.add(listing);
                        }
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