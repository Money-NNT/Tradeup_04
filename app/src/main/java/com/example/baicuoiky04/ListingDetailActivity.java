package com.example.baicuoiky04;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

    private ViewPager2 viewPagerImageSlider;
    private TextView textViewTitleDetail, textViewPriceDetail, textViewTimestamp, textViewDescriptionDetail, textViewSellerName;
    private CircleImageView imageViewSeller;
    private MaterialButton btnSaveListing, btnMakeOffer;
    private ImageSliderAdapter imageSliderAdapter;
    private View sellerInfoLayout; // Thêm biến cho layout người bán

    private FirebaseFirestore db;
    private ListenerRegistration listingListener;
    private String listingId;
    private FirebaseUser currentUser;
    private DataModels.Listing currentListing;
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

        // Đặt tiêu đề và nút back cho Action Bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Chi tiết sản phẩm");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initViews();
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Xử lý khi người dùng nhấn nút back trên Action Bar
        onBackPressed();
        return true;
    }

    private void initViews() {
        viewPagerImageSlider = findViewById(R.id.viewPagerImageSlider);
        textViewTitleDetail = findViewById(R.id.textViewTitleDetail);
        textViewPriceDetail = findViewById(R.id.textViewPriceDetail);
        textViewTimestamp = findViewById(R.id.textViewTimestamp);
        textViewDescriptionDetail = findViewById(R.id.textViewDescriptionDetail);
        textViewSellerName = findViewById(R.id.textViewSellerName);
        imageViewSeller = findViewById(R.id.imageViewSeller);
        btnSaveListing = findViewById(R.id.btnSaveListing);
        btnMakeOffer = findViewById(R.id.btnMakeOffer);
        sellerInfoLayout = findViewById(R.id.sellerInfoLayout); // Ánh xạ layout

        btnSaveListing.setOnClickListener(v -> toggleSaveListing());
        btnMakeOffer.setOnClickListener(v -> showMakeOfferDialog());

        // Gắn sự kiện click để xem trang cá nhân người bán
        sellerInfoLayout.setOnClickListener(v -> {
            if (currentListing != null && currentListing.getSellerId() != null) {
                Intent intent = new Intent(this, ProfileViewActivity.class);
                intent.putExtra("USER_ID", currentListing.getSellerId());
                startActivity(intent);
            }
        });
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
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        currentListing = snapshot.toObject(DataModels.Listing.class);
                        if (currentListing != null) {
                            updateUI(currentListing);
                        }
                    } else {
                        Toast.makeText(this, "Sản phẩm không còn tồn tại.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void updateUI(DataModels.Listing listing) {
        if (isDestroyed()) return;

        if (listing.getImageUrls() != null && !listing.getImageUrls().isEmpty()) {
            imageSliderAdapter = new ImageSliderAdapter(this, listing.getImageUrls());
            viewPagerImageSlider.setAdapter(imageSliderAdapter);
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
            Glide.with(this).load(listing.getSellerPhotoUrl()).placeholder(R.drawable.ic_profile_placeholder).into(imageViewSeller);
        }

        if (currentUser != null && currentUser.getUid().equals(listing.getSellerId())) {
            btnMakeOffer.setEnabled(false);
            btnMakeOffer.setText("Đây là tin đăng của bạn");
            btnSaveListing.setVisibility(View.GONE);
            sellerInfoLayout.setClickable(false); // Không cho nhấn vào profile của chính mình
        } else {
            btnMakeOffer.setEnabled(true);
            btnMakeOffer.setText("Liên hệ người bán / Trả giá");
            btnSaveListing.setVisibility(View.VISIBLE);
            sellerInfoLayout.setClickable(true);
        }

        checkIsSaved();
    }

    private void showMakeOfferDialog() {
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để trả giá", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentListing == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_make_offer, null);
        builder.setView(dialogView);

        TextView textViewCurrentPrice = dialogView.findViewById(R.id.textViewCurrentPrice);
        EditText editTextOfferPrice = dialogView.findViewById(R.id.editTextOfferPrice);

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        textViewCurrentPrice.setText("Giá hiện tại: " + formatter.format(currentListing.getPrice()));

        builder.setPositiveButton("Gửi", (dialog, which) -> {
            String offerPriceStr = editTextOfferPrice.getText().toString();
            if (TextUtils.isEmpty(offerPriceStr)) {
                Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
                return;
            }
            long offerPrice = Long.parseLong(offerPriceStr);
            submitOffer(offerPrice);
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void submitOffer(long offerPrice) {
        DataModels.Offer newOffer = new DataModels.Offer();
        newOffer.setBuyerId(currentUser.getUid());
        newOffer.setBuyerName(currentUser.getDisplayName());
        newOffer.setOfferPrice(offerPrice);
        newOffer.setStatus("pending");

        db.collection("listings").document(listingId).collection("offers")
                .add(newOffer)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Gửi trả giá thành công!", Toast.LENGTH_SHORT).show();
                    db.collection("listings").document(listingId).update("offersCount", FieldValue.increment(1));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void toggleSaveListing() {
        if (currentUser == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để thực hiện chức năng này", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();
        if (isSaved) {
            db.collection("users").document(userId).update("savedListings", FieldValue.arrayRemove(listingId))
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã bỏ lưu", Toast.LENGTH_SHORT).show());
        } else {
            db.collection("users").document(userId).update("savedListings", FieldValue.arrayUnion(listingId))
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã lưu tin", Toast.LENGTH_SHORT).show());
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