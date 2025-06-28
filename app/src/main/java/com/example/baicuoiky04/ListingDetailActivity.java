package com.example.baicuoiky04;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ListingDetailActivity extends AppCompatActivity {

    private static final String TAG = "ListingDetailActivity";

    private ImageView imageViewProductDetail;
    private TextView textViewTitleDetail, textViewPriceDetail, textViewTimestamp, textViewDescriptionDetail, textViewSellerName;
    private CircleImageView imageViewSeller;
    private MaterialButton btnSaveListing;

    private FirebaseFirestore db;
    private ListenerRegistration listingListener;
    private String listingId;
    private FirebaseUser currentUser;
    private boolean isSaved = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listing_detail);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        listingId = getIntent().getStringExtra("LISTING_ID");

        if (listingId == null || listingId.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không tìm thấy sản phẩm.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
    }

    private void initViews() {
        imageViewProductDetail = findViewById(R.id.imageViewProductDetail);
        textViewTitleDetail = findViewById(R.id.textViewTitleDetail);
        textViewPriceDetail = findViewById(R.id.textViewPriceDetail);
        textViewTimestamp = findViewById(R.id.textViewTimestamp);
        textViewDescriptionDetail = findViewById(R.id.textViewDescriptionDetail);
        textViewSellerName = findViewById(R.id.textViewSellerName);
        imageViewSeller = findViewById(R.id.imageViewSeller);
        btnSaveListing = findViewById(R.id.btnSaveListing);

        btnSaveListing.setOnClickListener(v -> toggleSaveListing());
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachListingListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listingListener != null) {
            listingListener.remove();
        }
    }

    private void attachListingListener() {
        listingListener = db.collection("listings").document(listingId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed.", error);
                        Toast.makeText(this, "Lỗi tải dữ liệu.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        DataModels.Listing listing = snapshot.toObject(DataModels.Listing.class);
                        if (listing != null) {
                            updateUI(listing);
                        }
                    } else {
                        Log.d(TAG, "Current data: null");
                        Toast.makeText(this, "Sản phẩm không còn tồn tại.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void updateUI(DataModels.Listing listing) {
        if (isDestroyed()) return;

        if (listing.getImageUrls() != null && !listing.getImageUrls().isEmpty()) {
            Glide.with(this)
                    .load(listing.getImageUrls().get(0))
                    .into(imageViewProductDetail);
        }

        textViewTitleDetail.setText(listing.getTitle());

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        textViewPriceDetail.setText(formatter.format(listing.getPrice()));

        textViewDescriptionDetail.setText(listing.getDescription());

        if (listing.getCreatedAt() != null && listing.getLocationName() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm, dd/MM/yyyy", Locale.getDefault());
            String formattedDate = sdf.format(listing.getCreatedAt());
            String timestampText = "Đăng lúc " + formattedDate + " tại " + listing.getLocationName();
            textViewTimestamp.setText(timestampText);
        }

        textViewSellerName.setText(listing.getSellerName());

        if (listing.getSellerPhotoUrl() != null && !listing.getSellerPhotoUrl().isEmpty()) {
            Glide.with(this)
                    .load(listing.getSellerPhotoUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(imageViewSeller);
        }

        checkIsSaved();
    }

    private void toggleSaveListing() {
        if (currentUser == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để thực hiện chức năng này", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        if (isSaved) {
            db.collection("users").document(userId)
                    .update("savedListings", FieldValue.arrayRemove(listingId))
                    .addOnSuccessListener(aVoid -> {
                        isSaved = false;
                        updateSaveButtonUI();
                        Toast.makeText(this, "Đã bỏ lưu", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            db.collection("users").document(userId)
                    .update("savedListings", FieldValue.arrayUnion(listingId))
                    .addOnSuccessListener(aVoid -> {
                        isSaved = true;
                        updateSaveButtonUI();
                        Toast.makeText(this, "Đã lưu tin", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void checkIsSaved() {
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> savedListings = (List<String>) documentSnapshot.get("savedListings");
                        isSaved = savedListings != null && savedListings.contains(listingId);
                        updateSaveButtonUI();
                    }
                });
    }

    private void updateSaveButtonUI() {
        if (isSaved) {
            btnSaveListing.setIconResource(R.drawable.ic_bookmark_filled_24);
            btnSaveListing.setIconTint(ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary, getTheme())));
        } else {
            btnSaveListing.setIconResource(R.drawable.ic_bookmark_24);
            btnSaveListing.setIconTint(ColorStateList.valueOf(getResources().getColor(android.R.color.darker_gray, getTheme())));
        }
    }
}
