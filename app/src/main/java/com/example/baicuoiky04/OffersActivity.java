package com.example.baicuoiky04;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OffersActivity extends AppCompatActivity {
    private static final String TAG = "OffersActivity";

    private ImageView imageViewProduct;
    private TextView textViewTitle, textViewPrice, textViewEmpty;
    private RecyclerView recyclerViewOffers;
    private ProgressBar progressBar;

    private OfferAdapter adapter;
    private List<DataModels.Offer> offerList;
    private List<String> offerIdList;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String listingId;
    private boolean hasAcceptedOffer = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offers);

        listingId = getIntent().getStringExtra("LISTING_ID");
        if (listingId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy tin đăng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        initViews();
        setupRecyclerView();
        loadListingDetails();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOffers();
    }

    private void initViews() {
        imageViewProduct = findViewById(R.id.imageViewProduct);
        textViewTitle = findViewById(R.id.textViewTitle);
        textViewPrice = findViewById(R.id.textViewPrice);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        recyclerViewOffers = findViewById(R.id.recyclerViewOffers);
        progressBar = findViewById(R.id.progressBar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Quản lý trả giá");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void setupRecyclerView() {
        offerList = new ArrayList<>();
        offerIdList = new ArrayList<>();
        OfferAdapter.OnOfferActionListener listener = new OfferAdapter.OnOfferActionListener() {
            @Override
            public void onAccept(DataModels.Offer offer, String offerId) {
                if (hasAcceptedOffer) {
                    Toast.makeText(OffersActivity.this, "Bạn đã chấp nhận một trả giá khác.", Toast.LENGTH_SHORT).show();
                    return;
                }
                acceptOffer(offer, offerId);
            }

            @Override
            public void onReject(DataModels.Offer offer, String offerId) {
                updateOfferStatus(offerId, "rejected");
            }

            @Override
            public void onReview(DataModels.Offer offer) {
                Intent intent = new Intent(OffersActivity.this, ReviewActivity.class);
                intent.putExtra("LISTING_ID", listingId);
                intent.putExtra("USER_ID_TO_REVIEW", offer.getBuyerId());
                intent.putExtra("USER_NAME_TO_REVIEW", offer.getBuyerName());
                startActivity(intent);
            }
        };
        adapter = new OfferAdapter(this, offerList, offerIdList, listener);
        recyclerViewOffers.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewOffers.setAdapter(adapter);
    }

    private void loadListingDetails() {
        db.collection("listings").document(listingId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        DataModels.Listing listing = doc.toObject(DataModels.Listing.class);
                        if(listing == null) return;

                        textViewTitle.setText(listing.getTitle());
                        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                        textViewPrice.setText(formatter.format(listing.getPrice()));
                        if (listing.getImageUrls() != null && !listing.getImageUrls().isEmpty()) {
                            Glide.with(this).load(listing.getImageUrls().get(0)).into(imageViewProduct);
                        }
                    }
                });
    }

    private void loadOffers() {
        progressBar.setVisibility(View.VISIBLE);
        textViewEmpty.setVisibility(View.GONE);

        db.collection("listings").document(listingId).collection("offers")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    offerList.clear();
                    offerIdList.clear();
                    hasAcceptedOffer = false;

                    if (queryDocumentSnapshots.isEmpty()) {
                        textViewEmpty.setVisibility(View.VISIBLE);
                    } else {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            DataModels.Offer offer = doc.toObject(DataModels.Offer.class);
                            offerList.add(offer);
                            offerIdList.add(doc.getId());
                            if ("accepted".equalsIgnoreCase(offer.getStatus())) {
                                hasAcceptedOffer = true;
                            }
                        }
                    }
                    adapter.updateAcceptedState();
                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi tải danh sách trả giá", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateOfferStatus(String offerId, String status) {
        db.collection("listings").document(listingId).collection("offers")
                .document(offerId).update("status", status)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã từ chối trả giá.", Toast.LENGTH_SHORT).show();
                    loadOffers();
                });
    }

    private void acceptOffer(DataModels.Offer acceptedOffer, String acceptedOfferId) {
        if (currentUser == null) {
            Toast.makeText(this, "Lỗi xác thực người dùng.", Toast.LENGTH_SHORT).show();
            return;
        }

        WriteBatch batch = db.batch();

        // 1. Cập nhật listing chính
        DocumentReference listingRef = db.collection("listings").document(listingId);
        batch.update(listingRef, "status", "sold", "buyerId", acceptedOffer.getBuyerId());

        // 2. Cập nhật offer được chấp nhận
        DocumentReference acceptedOfferRef = listingRef.collection("offers").document(acceptedOfferId);
        batch.update(acceptedOfferRef, "status", "accepted");

        // 3. Từ chối các offer khác
        for (int i = 0; i < offerIdList.size(); i++) {
            if (!offerIdList.get(i).equals(acceptedOfferId)) {
                DocumentReference otherOfferRef = listingRef.collection("offers").document(offerIdList.get(i));
                batch.update(otherOfferRef, "status", "rejected");
            }
        }

        // 4. Tăng totalTransactions cho cả người bán và người mua
        DocumentReference sellerRef = db.collection("users").document(currentUser.getUid());
        DocumentReference buyerRef = db.collection("users").document(acceptedOffer.getBuyerId());
        batch.update(sellerRef, "totalTransactions", FieldValue.increment(1));
        batch.update(buyerRef, "totalTransactions", FieldValue.increment(1));

        // Commit tất cả thay đổi
        batch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Đã chấp nhận trả giá và đóng tin!", Toast.LENGTH_LONG).show();

            // Tạo thông báo cho người mua
            DataModels.AppNotification notification = new DataModels.AppNotification();
            notification.setUserId(acceptedOffer.getBuyerId());
            notification.setTitle("Trả giá của bạn đã được chấp nhận!");
            notification.setBody("Người bán đã đồng ý với lời trả giá của bạn. Hãy vào thanh toán ngay!");
            notification.setListingId(listingId);
            db.collection("notifications").add(notification);

            // ============= SỬA LỖI Ở ĐÂY =============
            // Tải lại toàn bộ danh sách offer để cập nhật UI
            loadOffers();
            // =========================================

        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Có lỗi xảy ra: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}