package com.example.baicuoiky04;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class PreviewListingActivity extends AppCompatActivity {

    public static final String EXTRA_PREVIEW_DATA = "EXTRA_PREVIEW_DATA";

    private ViewPager2 viewPagerImageSlider;
    private TextView textViewTitleDetail, textViewPriceDetail, textViewLocation, textViewDescriptionDetail;
    private Button btnEdit, btnPost;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview_listing);

        initViews();

        Intent intent = getIntent();
        Bundle data = intent.getBundleExtra(EXTRA_PREVIEW_DATA);

        if (data == null) {
            Toast.makeText(this, "Lỗi dữ liệu xem trước", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        populateData(data);

        btnEdit.setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });

        btnPost.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            btnPost.setEnabled(false);
            btnEdit.setEnabled(false);

            Intent resultIntent = new Intent();
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        });
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        viewPagerImageSlider = findViewById(R.id.viewPagerImageSlider);
        textViewTitleDetail = findViewById(R.id.textViewTitleDetail);
        textViewPriceDetail = findViewById(R.id.textViewPriceDetail);
        textViewLocation = findViewById(R.id.textViewLocation);
        textViewDescriptionDetail = findViewById(R.id.textViewDescriptionDetail);
        btnEdit = findViewById(R.id.btnEdit);
        btnPost = findViewById(R.id.btnPost);
        progressBar = findViewById(R.id.progressBar);
    }

    private void populateData(Bundle data) {
        textViewTitleDetail.setText(data.getString("title"));
        textViewDescriptionDetail.setText(data.getString("description"));
        textViewLocation.setText("Tại: " + data.getString("location"));

        long price = data.getLong("price", 0);
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        textViewPriceDetail.setText(formatter.format(price));

        ArrayList<Uri> imageUris = data.getParcelableArrayList("imageUris");
        if (imageUris != null && !imageUris.isEmpty()) {
            UriImageSliderAdapter adapter = new UriImageSliderAdapter(this, imageUris);
            viewPagerImageSlider.setAdapter(adapter);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(Activity.RESULT_CANCELED);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
    }
}