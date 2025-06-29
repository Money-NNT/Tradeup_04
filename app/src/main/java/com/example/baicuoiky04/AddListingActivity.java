package com.example.baicuoiky04;

import android.Manifest;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddListingActivity extends AppCompatActivity {

    private static final String TAG = "AddListingActivity";
    private static final int PICK_IMAGES_REQUEST = 1;

    private TextInputEditText editTextTitle, editTextDescription, editTextPrice, editTextLocation;
    private TextInputLayout textInputLayoutLocation;
    private Spinner spinnerCategory, spinnerCondition;
    private Button btnChooseImages, btnSubmitListing;
    private ProgressBar progressBar;
    private TextView textViewScreenTitle;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private GeoPoint currentLocationGeoPoint;

    private String listingIdToEdit;
    private boolean isEditMode = false;
    private ArrayList<Uri> newImageUris;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    checkGpsAndGetLocation();
                } else {
                    Toast.makeText(this, "Bạn cần cấp quyền vị trí để sử dụng tính năng này.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_listing);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        newImageUris = new ArrayList<>();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

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
        editTextLocation = findViewById(R.id.editTextLocation);
        textInputLayoutLocation = findViewById(R.id.textInputLayoutLocation);
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
        textInputLayoutLocation.setEndIconOnClickListener(v -> checkLocationPermissionAndGetLocation());
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
        editTextLocation.setText(listing.getLocationName());
        currentLocationGeoPoint = listing.getLocationGeoPoint();

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

    private void checkLocationPermissionAndGetLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            checkGpsAndGetLocation();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void checkGpsAndGetLocation() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            new AlertDialog.Builder(this)
                    .setTitle("Yêu cầu bật GPS")
                    .setMessage("Để lấy vị trí chính xác, bạn cần bật GPS. Bạn có muốn bật ngay bây giờ không?")
                    .setPositiveButton("Cài đặt", (dialog, which) -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .setNegativeButton("Hủy", null)
                    .show();
        } else {
            requestNewLocationData();
        }
    }

    private void requestNewLocationData() {
        Toast.makeText(this, "Đang lấy vị trí, vui lòng chờ...", Toast.LENGTH_SHORT).show();

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location lastLocation = locationResult.getLastLocation();
                if (lastLocation != null) {
                    currentLocationGeoPoint = new GeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude());
                    getAddressFromLocation(currentLocationGeoPoint);
                    fusedLocationClient.removeLocationUpdates(locationCallback);
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission not granted, cannot request updates.", e);
        }
    }

    private void getAddressFromLocation(GeoPoint geoPoint) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(geoPoint.getLatitude(), geoPoint.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String subLocality = address.getSubLocality() != null ? address.getSubLocality() : "";
                String city = address.getAdminArea() != null ? address.getAdminArea() : "";
                String addressString = subLocality + ", " + city;
                editTextLocation.setText(addressString.startsWith(", ") ? addressString.substring(2) : addressString);
            } else {
                editTextLocation.setText("Không tìm thấy địa chỉ");
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder service not available", e);
            editTextLocation.setText("Lỗi dịch vụ địa chỉ");
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
            saveListingToFirestore(new ArrayList<>());
        }
    }

    private boolean validateInput() {
        if (TextUtils.isEmpty(editTextTitle.getText())) { editTextTitle.setError("Tiêu đề không được để trống"); return false; }
        if (TextUtils.isEmpty(editTextDescription.getText())) { editTextDescription.setError("Mô tả không được để trống"); return false; }
        if (TextUtils.isEmpty(editTextPrice.getText())) { editTextPrice.setError("Giá không được để trống"); return false; }
        if (TextUtils.isEmpty(editTextLocation.getText())) { editTextLocation.setError("Địa điểm không được để trống"); return false; }
        return true;
    }

    private void updateListing() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", editTextTitle.getText().toString().trim());
        updates.put("description", editTextDescription.getText().toString().trim());
        updates.put("price", Long.parseLong(editTextPrice.getText().toString()));
        updates.put("category", spinnerCategory.getSelectedItem().toString());
        updates.put("condition", spinnerCondition.getSelectedItem().toString());
        updates.put("locationName", editTextLocation.getText().toString().trim());
        if (currentLocationGeoPoint != null) {
            updates.put("locationGeoPoint", currentLocationGeoPoint);
        }

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
        newListing.setLocationName(editTextLocation.getText().toString().trim());
        if (currentLocationGeoPoint != null) {
            newListing.setLocationGeoPoint(currentLocationGeoPoint);
        }
        newListing.setImageUrls(imageUrls);
        newListing.setStatus("available");
        newListing.setViews(0);
        newListing.setOffersCount(0);
        newListing.setNegotiable(true);

        ArrayList<String> tags = new ArrayList<>();
        String[] titleWords = newListing.getTitle().toLowerCase(Locale.ROOT).split("\\s+");
        for (String word : titleWords) {
            if (!word.trim().isEmpty()) {
                tags.add(word.replaceAll("[^a-z0-9]", ""));
            }
        }
        newListing.setTags(tags);

        db.collection("listings").document(listingId).set(newListing)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đăng tin (không ảnh) thành công!", Toast.LENGTH_SHORT).show();
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