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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class SavedListingsFragment extends Fragment {
    private static final String TAG = "SavedListingsFragment";

    private RecyclerView recyclerView;
    private ListingAdapter adapter;
    private List<DataModels.Listing> savedListings;
    private ProgressBar progressBar;
    private TextView textViewEmpty;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ListenerRegistration userListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_saved_listings, container, false);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        recyclerView = view.findViewById(R.id.recyclerViewSavedListings);
        progressBar = view.findViewById(R.id.progressBarSaved);
        textViewEmpty = view.findViewById(R.id.textViewEmpty);

        setupRecyclerView();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (currentUser != null) {
            attachUserListener();
        } else {
            Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            textViewEmpty.setText("Vui lòng đăng nhập để xem tin đã lưu");
            textViewEmpty.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (userListener != null) {
            userListener.remove();
        }
    }

    private void setupRecyclerView() {
        savedListings = new ArrayList<>();
        adapter = new ListingAdapter(getContext(), savedListings);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(listing -> {
            Intent intent = new Intent(getActivity(), ListingDetailActivity.class);
            intent.putExtra("LISTING_ID", listing.getListingId());
            startActivity(intent);
        });
    }

    private void attachUserListener() {
        progressBar.setVisibility(View.VISIBLE);
        textViewEmpty.setVisibility(View.GONE);
        savedListings.clear();
        adapter.notifyDataSetChanged();

        userListener = db.collection("users").document(currentUser.getUid())
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed.", error);
                        progressBar.setVisibility(View.GONE);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        List<String> savedListingIds = (List<String>) documentSnapshot.get("savedListings");
                        if (savedListingIds == null || savedListingIds.isEmpty()) {
                            progressBar.setVisibility(View.GONE);
                            textViewEmpty.setVisibility(View.VISIBLE);
                            savedListings.clear();
                            adapter.notifyDataSetChanged();
                        } else {
                            fetchListingsByIds(savedListingIds);
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        textViewEmpty.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void fetchListingsByIds(List<String> ids) {
        if (ids.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            textViewEmpty.setVisibility(View.VISIBLE);
            savedListings.clear();
            adapter.notifyDataSetChanged();
            return;
        }

        db.collection("listings").whereIn("listingId", ids).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    savedListings.clear();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for(DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()){
                            savedListings.add(doc.toObject(DataModels.Listing.class));
                        }
                    }
                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                    if (savedListings.isEmpty()) {
                        textViewEmpty.setVisibility(View.VISIBLE);
                    } else {
                        textViewEmpty.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error fetching listings by IDs", e);
                });
    }
}