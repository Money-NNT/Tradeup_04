// Dán toàn bộ code này để thay thế file PaymentActivity.java cũ

package com.example.baicuoiky04;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.text.NumberFormat;
import java.util.Locale;

public class PaymentActivity extends AppCompatActivity {

    public static final String EXTRA_LISTING_NAME = "listing_name";
    public static final String EXTRA_SELLER_NAME = "seller_name";
    public static final String EXTRA_OFFER_PRICE = "offer_price";

    private TextView textViewProductName, textViewSellerName, textViewOfferPrice;
    private Button btnConfirmPayment;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        initViews();
        populateData();
        setupListeners();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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

        textViewProductName.setText("Sản phẩm: " + listingName);
        textViewSellerName.setText("Người bán: " + sellerName);

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        textViewOfferPrice.setText("Giá đã trả: " + formatter.format(offerPrice));
    }

    private void setupListeners() {
        btnConfirmPayment.setOnClickListener(v -> processPayment());
    }

    // ================== CẬP NHẬT TOÀN BỘ HÀM NÀY ==================
    private void processPayment() {
        setLoading(true);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            setLoading(false);

            boolean isPaymentSuccessful = true; // Giả lập thanh toán thành công

            if (isPaymentSuccessful) {
                Toast.makeText(this, "Thanh toán thành công! Cảm ơn bạn đã mua hàng.", Toast.LENGTH_LONG).show();

                // Tạo một Intent để mở MainActivity (Trang chủ)
                Intent intent = new Intent(this, MainActivity.class);

                // Các cờ này sẽ xóa tất cả các Activity cũ (ListingDetail,...)
                // và đưa MainActivity lên làm đầu tiên.
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                startActivity(intent);
                finish(); // Đóng màn hình PaymentActivity hiện tại
            } else {
                Toast.makeText(this, "Thanh toán thất bại. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
            }
        }, 3000); // 3 giây
    }
    // =============================================================

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