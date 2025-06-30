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

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class MyOffersActivity extends AppCompatActivity {

    private static final String TAG = "MyOffersActivity";

    private RecyclerView recyclerView;
    private MyOffersAdapter adapter;
    private List<DataModels.OfferWithListing> offerList;
    private ProgressBar progressBar;
    private TextView textViewEmpty;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_offers);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        initViews();
        setupRecyclerView();
        loadMyOffers();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewMyOffers);
        progressBar = findViewById(R.id.progressBar);
        textViewEmpty = findViewById(R.id.textViewEmpty);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Lịch sử trả giá");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        offerList = new ArrayList<>();
        adapter = new MyOffersAdapter(this, offerList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadMyOffers() {
        if (currentUser == null) return;
        progressBar.setVisibility(View.VISIBLE);

        // Dùng Collection Group Query để lấy tất cả sub-collection "offers"
        db.collectionGroup("offers")
                .whereEqualTo("buyerId", currentUser.getUid())
                .get()
                .addOnSuccessListener(offerSnapshots -> {
                    if (offerSnapshots.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        textViewEmpty.setVisibility(View.VISIBLE);
                        return;
                    }

                    List<Task<DocumentSnapshot>> listingTasks = new ArrayList<>();
                    for (DocumentSnapshot offerDoc : offerSnapshots) {
                        // Từ offer, lấy document listing cha của nó
                        DocumentReference parentListingRef = offerDoc.getReference().getParent().getParent();
                        if (parentListingRef != null) {
                            listingTasks.add(parentListingRef.get());
                        }
                    }

                    // Chờ tất cả các tác vụ lấy listing cha hoàn tất
                    Tasks.whenAllSuccess(listingTasks).addOnSuccessListener(listingDocs -> {
                        offerList.clear();
                        for (int i = 0; i < listingDocs.size(); i++) {
                            DocumentSnapshot listingDoc = (DocumentSnapshot) listingDocs.get(i);
                            DocumentSnapshot offerDoc = offerSnapshots.getDocuments().get(i);

                            DataModels.Listing listing = listingDoc.toObject(DataModels.Listing.class);
                            DataModels.Offer offer = offerDoc.toObject(DataModels.Offer.class);

                            if (listing != null && offer != null) {
                                offerList.add(new DataModels.OfferWithListing(offer, listing));
                            }
                        }
                        adapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE);
                    });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error fetching offers: ", e);
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