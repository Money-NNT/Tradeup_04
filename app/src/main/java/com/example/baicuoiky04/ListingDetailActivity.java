package com.example.baicuoiky04;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

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
    private MaterialButton btnSaveListing, btnMakeOffer, btnBuyNow, btnPayNow, btnStartChat;
    private ImageSliderAdapter imageSliderAdapter;
    private View sellerInfoLayout;
    private LinearLayout bottomActionLayout;
    private FirebaseFirestore db;
    private ListenerRegistration listingListener;
    private String listingId;
    private FirebaseUser currentUser;
    private DataModels.Listing currentListing = null;
    private boolean isSaved = false;
    private boolean isDataLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listing_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        listingId = getIntent().getStringExtra("LISTING_ID");

        if (listingId == null || listingId.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không tìm thấy sản phẩm.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initViews();
        incrementViewCount();
    }

    private void initViews() {
        viewPagerImageSlider = findViewById(R.id.viewPagerImageSlider);
        textViewTitleDetail = findViewById(R.id.textViewTitleDetail);
        textViewPriceDetail = findViewById(R.id.textViewPriceDetail);
        textViewTimestamp = findViewById(R.id.textViewTimestamp);
        textViewDescriptionDetail = findViewById(R.id.textViewDescriptionDetail);
        textViewSellerName = findViewById(R.id.textViewSellerName);
        imageViewSeller = findViewById(R.id.imageViewSeller);
        sellerInfoLayout = findViewById(R.id.sellerInfoLayout);
        bottomActionLayout = findViewById(R.id.bottom_action_layout);
        btnSaveListing = findViewById(R.id.btnSaveListing);
        btnStartChat = findViewById(R.id.btnStartChat);
        btnMakeOffer = findViewById(R.id.btnMakeOffer);
        btnBuyNow = findViewById(R.id.btnBuyNow);
        btnPayNow = findViewById(R.id.btnPayNow);

        setBottomActionsEnabled(false);

        btnSaveListing.setOnClickListener(v -> toggleSaveListing());
        btnStartChat.setOnClickListener(v -> startChat());
        btnMakeOffer.setOnClickListener(v -> showMakeOfferDialog());
        btnBuyNow.setOnClickListener(v -> processBuyNow());
        btnPayNow.setOnClickListener(v -> openPaymentActivity());
        sellerInfoLayout.setOnClickListener(v -> {
            if (isDataLoaded && currentListing != null) {
                Intent intent = new Intent(this, ProfileViewActivity.class);
                intent.putExtra("USER_ID", currentListing.getSellerId());
                startActivity(intent);
            }
        });
    }

    private void setBottomActionsEnabled(boolean enabled) {
        btnSaveListing.setEnabled(enabled);
        btnStartChat.setEnabled(enabled);
        btnMakeOffer.setEnabled(enabled);
        btnBuyNow.setEnabled(enabled);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isDataLoaded && currentUser != null && currentListing != null && !currentUser.getUid().equals(currentListing.getSellerId())) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.detail_menu, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_report_listing) {
            showReportDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startChat() {
        if (!isDataLoaded || currentUser == null) {
            Toast.makeText(this, "Dữ liệu chưa sẵn sàng, vui lòng thử lại.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("receiver_id", currentListing.getSellerId());
        intent.putExtra("receiver_name", currentListing.getSellerName());
        startActivity(intent);
    }

    private void incrementViewCount() {
        if (listingId != null) {
            db.collection("listings").document(listingId).update("views", FieldValue.increment(1));
        }
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
                        Log.w(TAG, "Listen failed.", error);
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        currentListing = snapshot.toObject(DataModels.Listing.class);
                        if (currentListing != null) {
                            isDataLoaded = true;
                            setBottomActionsEnabled(true);
                            updateUI(currentListing);
                            invalidateOptionsMenu();
                        }
                    } else {
                        Toast.makeText(this, "Sản phẩm không còn tồn tại.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void updateUI(DataModels.Listing listing) {
        if (isDestroyed()) return;

        bottomActionLayout.setVisibility(View.VISIBLE);

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
            textViewTimestamp.setText(String.format("Đăng lúc %s tại %s", formattedDate, listing.getLocationName()));
        }

        // Kiểm tra sellerName trước khi setText
        if (listing.getSellerName() != null && !listing.getSellerName().isEmpty()) {
            textViewSellerName.setText(listing.getSellerName());
        } else {
            textViewSellerName.setText("Người bán ẩn danh"); // Hoặc một giá trị mặc định khác
        }

        if (listing.getSellerPhotoUrl() != null && !listing.getSellerPhotoUrl().isEmpty()) {
            Glide.with(this).load(listing.getSellerPhotoUrl()).placeholder(R.drawable.ic_profile_placeholder).into(imageViewSeller);
        } else {
            imageViewSeller.setImageResource(R.drawable.ic_profile_placeholder);
        }

        boolean isSold = "sold".equals(listing.getStatus());
        boolean isOwner = currentUser != null && currentUser.getUid().equals(listing.getSellerId());
        boolean isTheBuyer = isSold && currentUser != null && currentUser.getUid().equals(listing.getBuyerId());

        btnPayNow.setVisibility(isTheBuyer ? View.VISIBLE : View.GONE);
        btnBuyNow.setVisibility(!isOwner && !isSold ? View.VISIBLE : View.GONE);
        btnMakeOffer.setVisibility(!isOwner && !isSold ? View.VISIBLE : View.GONE);
        btnStartChat.setVisibility(!isOwner ? View.VISIBLE : View.GONE);
        btnSaveListing.setVisibility(!isOwner ? View.VISIBLE : View.GONE);

        if (!isOwner) {
            checkIsSaved();
        }
    }

    private void processBuyNow() {
        if (!isDataLoaded || currentUser == null) {
            Toast.makeText(this, "Vui lòng đợi tải xong dữ liệu.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận mua ngay")
                .setMessage("Bạn có chắc chắn muốn mua sản phẩm '" + currentListing.getTitle() + "' với giá " + NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(currentListing.getPrice()) + "?")
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    WriteBatch batch = db.batch();
                    DocumentReference listingRef = db.collection("listings").document(listingId);
                    batch.update(listingRef, "status", "sold", "buyerId", currentUser.getUid());
                    DocumentReference sellerRef = db.collection("users").document(currentListing.getSellerId());
                    DocumentReference buyerRef = db.collection("users").document(currentUser.getUid());
                    batch.update(sellerRef, "totalTransactions", FieldValue.increment(1));
                    batch.update(buyerRef, "totalTransactions", FieldValue.increment(1));
                    batch.commit().addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Giao dịch đã được ghi nhận! Chuẩn bị thanh toán...", Toast.LENGTH_SHORT).show();
                        openPaymentActivity();
                    }).addOnFailureListener(e -> Toast.makeText(this, "Có lỗi xảy ra: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Hủy", null).show();
    }

    private void openPaymentActivity() {
        if (currentListing != null) {
            Intent intent = new Intent(this, PaymentActivity.class);
            intent.putExtra(PaymentActivity.EXTRA_LISTING_NAME, currentListing.getTitle());
            intent.putExtra(PaymentActivity.EXTRA_SELLER_NAME, currentListing.getSellerName());
            intent.putExtra(PaymentActivity.EXTRA_OFFER_PRICE, currentListing.getPrice());
            startActivity(intent);
        }
    }

    private void showReportDialog() {
        if (!isDataLoaded || currentUser == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_report, null);
        builder.setView(dialogView);
        TextView reportTitle = dialogView.findViewById(R.id.textViewReportTitle);
        reportTitle.setText("Báo cáo tin đăng");
        final RadioGroup radioGroup = dialogView.findViewById(R.id.radioGroupReasons);
        final TextInputLayout commentLayout = dialogView.findViewById(R.id.textInputLayoutComment);
        final EditText commentEditText = dialogView.findViewById(R.id.editTextComment);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioOther) {
                commentLayout.setVisibility(View.VISIBLE);
            } else {
                commentLayout.setVisibility(View.GONE);
            }
        });
        builder.setPositiveButton("Gửi báo cáo", (dialog, which) -> {
            int selectedId = radioGroup.getCheckedRadioButtonId();
            if (selectedId == -1) { Toast.makeText(this, "Vui lòng chọn lý do báo cáo", Toast.LENGTH_SHORT).show(); return; }
            String reason = "";
            if (selectedId == R.id.radioScam) reason = "Lừa đảo";
            else if (selectedId == R.id.radioInappropriate) reason = "Nội dung không phù hợp";
            else if (selectedId == R.id.radioSpam) reason = "Spam";
            else if (selectedId == R.id.radioOther) reason = "Khác";
            String comment = commentEditText.getText().toString().trim();
            submitReport(reason, comment);
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void submitReport(String reason, String comment) {
        if (currentUser == null || currentListing == null) return;
        DataModels.Report report = new DataModels.Report();
        report.setReporterId(currentUser.getUid());
        report.setReportedListingId(listingId);
        report.setReportedUserId(currentListing.getSellerId());
        report.setReason(reason);
        report.setComment(comment);
        db.collection("reports").add(report)
                .addOnSuccessListener(documentReference -> Toast.makeText(this, "Cảm ơn bạn đã gửi báo cáo.", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showMakeOfferDialog() {
        if (!isDataLoaded || currentUser == null) return;
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
        builder.setNegativeButton("Hủy", null).create().show();
    }

    private void submitOffer(long offerPrice) {
        if (currentUser == null || currentListing == null) return;
        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Lỗi không tìm thấy thông tin của bạn", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String buyerName = documentSnapshot.getString("displayName");
                    if (TextUtils.isEmpty(buyerName)) {
                        buyerName = currentUser.getEmail(); // Dùng email làm phương án dự phòng
                    }

                    DataModels.Offer newOffer = new DataModels.Offer();
                    newOffer.setBuyerId(currentUser.getUid());
                    newOffer.setBuyerName(buyerName);
                    newOffer.setOfferPrice(offerPrice);
                    newOffer.setStatus("pending");

                    DocumentReference listingRef = db.collection("listings").document(listingId);
                    listingRef.collection("offers").add(newOffer)
                            .addOnSuccessListener(docRef -> {
                                Toast.makeText(this, "Gửi trả giá thành công!", Toast.LENGTH_SHORT).show();
                                listingRef.update("offersCount", FieldValue.increment(1));
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                });
    }

    private void toggleSaveListing() {
        if (!isDataLoaded || currentUser == null) return;
        DocumentReference userRef = db.collection("users").document(currentUser.getUid());
        if (isSaved) {
            userRef.update("savedListings", FieldValue.arrayRemove(listingId));
        } else {
            userRef.update("savedListings", FieldValue.arrayUnion(listingId));
        }
    }

    private void checkIsSaved() {
        if (currentUser == null) return;
        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isDestroyed()) return;
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
            btnSaveListing.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorPrimary)));
        } else {
            btnSaveListing.setIconResource(R.drawable.ic_bookmark_24);
            btnSaveListing.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.darker_gray)));
        }
    }
}