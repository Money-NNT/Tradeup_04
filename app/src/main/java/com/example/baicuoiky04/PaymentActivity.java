package com.example.baicuoiky04;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.NumberFormat;
import java.util.Locale;

public class PaymentActivity extends AppCompatActivity {

    public static final String EXTRA_LISTING_NAME = "listing_name";
    public static final String EXTRA_SELLER_NAME = "seller_name";
    public static final String EXTRA_OFFER_PRICE = "offer_price";
    public static final String EXTRA_LISTING_ID = "listing_id";
    public static final String EXTRA_SELLER_ID = "seller_id";
    public static final String EXTRA_BUYER_ID = "buyer_id";

    private TextView textViewProductName, textViewSellerName, textViewOfferPrice;
    private Button btnConfirmPayment;
    private ProgressBar progressBar;

    private String listingId, sellerId, buyerId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        db = FirebaseFirestore.getInstance();

        initViews();
        populateData();
        setupListeners();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Xác nhận Thanh toán");
        }
        textViewProductName = findViewById(R.id.textViewProductName);
        textViewSellerName = findViewById(R.id.textViewSellerName);
        textViewOfferPrice = findViewById(R.id.textViewOfferPrice);
        btnConfirmPayment = findViewById(R.id.btnConfirmPayment);
        progressBar = findViewById(R.id.progressBar);
    }

    private void populateData() {
        String listingName = getIntent().getStringExtra(EXTRA_LISTING_NAME);
        String sellerName = getIntent().getStringExtra(EXTRA_SELLER_NAME);
        long offerPrice = getIntent().getLongExtra(EXTRA_OFFER_PRICE, 0);

        listingId = getIntent().getStringExtra(EXTRA_LISTING_ID);
        sellerId = getIntent().getStringExtra(EXTRA_SELLER_ID);
        buyerId = getIntent().getStringExtra(EXTRA_BUYER_ID);

        textViewProductName.setText("Sản phẩm: " + listingName);
        textViewSellerName.setText("Người bán: " + sellerName);

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        textViewOfferPrice.setText("Giá đã trả: " + formatter.format(offerPrice));
    }

    private void setupListeners() {
        btnConfirmPayment.setOnClickListener(v -> processPayment());
    }

    private void processPayment() {
        if (TextUtils.isEmpty(sellerId) || TextUtils.isEmpty(buyerId)) {
            Toast.makeText(this, "Lỗi: Thiếu thông tin giao dịch.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        // Giả lập quá trình thanh toán
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            WriteBatch batch = db.batch();
            DocumentReference sellerRef = db.collection("users").document(sellerId);
            DocumentReference buyerRef = db.collection("users").document(buyerId);

            batch.update(sellerRef, "totalTransactions", FieldValue.increment(1));
            batch.update(buyerRef, "totalTransactions", FieldValue.increment(1));

            batch.commit().addOnSuccessListener(aVoid -> {
                setLoading(false);
                Toast.makeText(this, "Thanh toán thành công! Cảm ơn bạn đã mua hàng.", Toast.LENGTH_LONG).show();

                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();

            }).addOnFailureListener(e -> {
                setLoading(false);
                Toast.makeText(this, "Thanh toán thành công nhưng có lỗi ghi nhận giao dịch.", Toast.LENGTH_LONG).show();
                // Dù lỗi ghi nhận vẫn cho về trang chủ
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });

        }, 2000); // 2 giây
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnConfirmPayment.setEnabled(false);
            btnConfirmPayment.setText("Đang xử lý...");
        } else {
            progressBar.setVisibility(View.GONE);
            btnConfirmPayment.setEnabled(true);
            btnConfirmPayment.setText("Xác nhận thanh toán");
        }
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