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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddListingActivity extends AppCompatActivity {

    private static final String TAG = "AddListingActivity";
    private static final int PICK_IMAGES_REQUEST = 1;

    private TextInputEditText editTextTitle, editTextDescription, editTextPrice;
    private Spinner spinnerCategory, spinnerCondition;
    private Button btnChooseImages, btnSubmitListing;
    private ProgressBar progressBar;
    private TextView textViewScreenTitle;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private String listingIdToEdit;
    private boolean isEditMode = false;
    private ArrayList<Uri> newImageUris;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_listing);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        newImageUris = new ArrayList<>();

        initViews();
        setupSpinners();
        setupListeners();

        if (getIntent().hasExtra("LISTING_ID")) {
            listingIdToEdit = getIntent().getStringExtra("LISTING_ID");
            isEditMode = true;
            prepareEditMode();
        }
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
        textViewScreenTitle = findViewById(R.id.textViewScreenTitle);
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(this, R.array.category_array, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);
        ArrayAdapter<CharSequence> conditionAdapter = ArrayAdapter.createFromResource(this, R.array.condition_array, android.R.layout.simple_spinner_item);
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

    private void prepareEditMode() {
        textViewScreenTitle.setText("Chỉnh sửa tin đăng");
        btnSubmitListing.setText("Lưu thay đổi");

        db.collection("listings").document(listingIdToEdit).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        DataModels.Listing listing = documentSnapshot.toObject(DataModels.Listing.class);
                        if (listing != null) populateFields(listing);
                    }
                });
    }

    private void populateFields(DataModels.Listing listing) {
        editTextTitle.setText(listing.getTitle());
        editTextDescription.setText(listing.getDescription());
        editTextPrice.setText(String.valueOf(listing.getPrice()));

        ArrayAdapter<CharSequence> categoryAdapter = (ArrayAdapter<CharSequence>) spinnerCategory.getAdapter();
        spinnerCategory.setSelection(categoryAdapter.getPosition(listing.getCategory()));

        ArrayAdapter<CharSequence> conditionAdapter = (ArrayAdapter<CharSequence>) spinnerCondition.getAdapter();
        spinnerCondition.setSelection(conditionAdapter.getPosition(listing.getCondition()));
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK && data != null) {
            newImageUris.clear();
            if (data.getClipData() != null) {
                ClipData clipData = data.getClipData();
                int count = Math.min(clipData.getItemCount(), 10);
                for (int i = 0; i < count; i++) newImageUris.add(clipData.getItemAt(i).getUri());
            } else if (data.getData() != null) {
                newImageUris.add(data.getData());
            }
            btnChooseImages.setText(newImageUris.size() + " ảnh đã được chọn");
        }
    }

    private void handleSubmit() {
        if (!validateInput()) {
            return;
        }
        setLoading(true);

        if (isEditMode) {
            updateListing();
        } else {
            ArrayList<String> dummyImageUrls = new ArrayList<>();
            dummyImageUrls.add("https://via.placeholder.com/400x300.png?text=" + editTextTitle.getText().toString().replace(" ", "+"));
            saveListingToFirestore(dummyImageUrls);
        }
    }

    private boolean validateInput() {
        if (TextUtils.isEmpty(editTextTitle.getText())) { editTextTitle.setError("Tiêu đề không được để trống"); return false; }
        if (TextUtils.isEmpty(editTextDescription.getText())) { editTextDescription.setError("Mô tả không được để trống"); return false; }
        if (TextUtils.isEmpty(editTextPrice.getText())) { editTextPrice.setError("Giá không được để trống"); return false; }
        return true;
    }

    private void updateListing() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", editTextTitle.getText().toString().trim());
        updates.put("description", editTextDescription.getText().toString().trim());
        updates.put("price", Long.parseLong(editTextPrice.getText().toString()));
        updates.put("category", spinnerCategory.getSelectedItem().toString());
        updates.put("condition", spinnerCondition.getSelectedItem().toString());

        ArrayList<String> tags = new ArrayList<>();
        String[] titleWords = editTextTitle.getText().toString().trim().toLowerCase(Locale.ROOT).split("\\s+");
        for (String word : titleWords) {
            tags.add(word.replaceAll("[^a-z0-9]", ""));
        }
        updates.put("tags", tags);
        updates.put("lastUpdatedAt", FieldValue.serverTimestamp());

        db.collection("listings").document(listingIdToEdit).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cập nhật tin đăng thành công!", Toast.LENGTH_SHORT).show();
                    setLoading(false);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    setLoading(false);
                });
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
        newListing.setLocationName("Hà Nội, Việt Nam"); // Tạm thời hard-code
        newListing.setImageUrls(imageUrls);
        newListing.setStatus("available");
        newListing.setViews(0);
        newListing.setOffersCount(0);
        newListing.setNegotiable(true);

        ArrayList<String> tags = new ArrayList<>();
        String[] titleWords = newListing.getTitle().toLowerCase(Locale.ROOT).split("\\s+");
        for (String word : titleWords) {
            tags.add(word.replaceAll("[^a-z0-9]", ""));
        }
        newListing.setTags(tags);

        db.collection("listings").document(listingId).set(newListing)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đăng tin (test) thành công!", Toast.LENGTH_SHORT).show();
                    setLoading(false);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi lưu tin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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