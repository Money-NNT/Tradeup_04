package com.example.baicuoiky04;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AddListingActivity extends AppCompatActivity {

    private static final String TAG = "AddListingActivity";
    private static final int PICK_IMAGES_REQUEST = 1;

    private TextInputEditText editTextTitle, editTextDescription, editTextPrice;
    private Spinner spinnerCategory, spinnerCondition;
    private Button btnChooseImages, btnSubmitListing;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private ArrayList<Uri> imageUris;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_listing);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        imageUris = new ArrayList<>();

        if (currentUser == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để đăng tin", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupSpinners();
        setupListeners();
    }

    private void initViews() {
        editTextTitle = findViewById(R.id.editTextTitle);
        editTextDescription = findViewById(R.id.editTextDescription);
        editTextPrice = findViewById(R.id.editTextPrice);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerCondition = findViewById(R.id.spinnerCondition);
        btnChooseImages = findViewById(R.id.btnChooseImages);
        btnSubmitListing = findViewById(R.id.btnSubmitListing);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(this,
                R.array.category_array, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        ArrayAdapter<CharSequence> conditionAdapter = ArrayAdapter.createFromResource(this,
                R.array.condition_array, android.R.layout.simple_spinner_item);
        conditionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCondition.setAdapter(conditionAdapter);
    }

    private void setupListeners() {
        btnChooseImages.setOnClickListener(v -> openImagePicker());
        btnSubmitListing.setOnClickListener(v -> handleSubmit());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Chọn tối đa 10 ảnh"), PICK_IMAGES_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK && data != null) {
            imageUris.clear();
            if (data.getClipData() != null) {
                ClipData clipData = data.getClipData();
                int count = clipData.getItemCount();
                int limit = Math.min(count, 10);
                for (int i = 0; i < limit; i++) {
                    imageUris.add(clipData.getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                imageUris.add(data.getData());
            }
            btnChooseImages.setText(imageUris.size() + " ảnh đã được chọn");
        }
    }

    private void handleSubmit() {
        if (!validateInputWithoutImage()) {
            return;
        }
        setLoading(true);

        // Tạo một danh sách URL ảnh giả để test
        ArrayList<String> dummyImageUrls = new ArrayList<>();
        dummyImageUrls.add("https://via.placeholder.com/400x300.png?text=" + editTextTitle.getText().toString().replace(" ", "+"));

        // Gọi thẳng hàm lưu vào Firestore
        saveListingToFirestore(dummyImageUrls);
    }

    private boolean validateInputWithoutImage() {
        if (TextUtils.isEmpty(editTextTitle.getText())) {
            editTextTitle.setError("Tiêu đề không được để trống");
            return false;
        }
        if (TextUtils.isEmpty(editTextDescription.getText())) {
            editTextDescription.setError("Mô tả không được để trống");
            return false;
        }
        if (TextUtils.isEmpty(editTextPrice.getText())) {
            editTextPrice.setError("Giá không được để trống");
            return false;
        }
        return true;
    }

    private void saveListingToFirestore(List<String> imageUrls) {
        String listingId = db.collection("listings").document().getId();
        DataModels.Listing newListing = new DataModels.Listing();

        newListing.setListingId(listingId);
        newListing.setSellerId(currentUser.getUid());
        newListing.setSellerName(currentUser.getDisplayName());
        newListing.setSellerPhotoUrl(currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : "");
        newListing.setTitle(editTextTitle.getText().toString().trim());
        newListing.setDescription(editTextDescription.getText().toString().trim());
        newListing.setPrice(Long.parseLong(editTextPrice.getText().toString()));
        newListing.setCategory(spinnerCategory.getSelectedItem().toString());
        newListing.setCondition(spinnerCondition.getSelectedItem().toString());
        newListing.setLocationName("Hà Nội, Việt Nam");
        newListing.setImageUrls(imageUrls);
        newListing.setStatus("available");
        newListing.setViews(0);
        newListing.setOffersCount(0);
        newListing.setNegotiable(true);

        ArrayList<String> tags = new ArrayList<>();
        String[] titleWords = newListing.getTitle().toLowerCase(Locale.ROOT).split("\\s+");
        for (String word : titleWords) {
            tags.add(word.replaceAll("[^a-z0-9]", "")); // Loại bỏ ký tự đặc biệt
        }
        newListing.setTags(tags);

        db.collection("listings").document(listingId).set(newListing)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Listing created successfully with ID: " + listingId);
                    Toast.makeText(AddListingActivity.this, "Đăng tin (test) thành công!", Toast.LENGTH_SHORT).show();
                    setLoading(false);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding document", e);
                    Toast.makeText(AddListingActivity.this, "Lỗi khi lưu tin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    setLoading(false);
                });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnSubmitListing.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnSubmitListing.setEnabled(true);
        }
    }
}