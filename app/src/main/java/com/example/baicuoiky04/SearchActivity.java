package com.example.baicuoiky04;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import java.util.stream.Collectors;

public class SearchActivity extends AppCompatActivity implements FilterBottomSheetFragment.FilterListener {

    private static final String TAG = "SearchActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private SearchView searchView;
    private Button btnFilter;
    private RecyclerView recyclerViewResults;
    private ListingAdapter adapter;
    private TextView textViewPlaceholder;
    private ProgressBar progressBar;

    private List<DataModels.Listing> resultList;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private Location lastKnownLocation;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private String currentKeyword = "";
    private String currentSortBy = "createdAt";
    private Query.Direction currentSortDirection = Query.Direction.DESCENDING;
    private float currentMinPrice = 0;
    private float currentMaxPrice = 50000000;
    private String currentCategory = "Tất cả";
    private String currentCondition = "Tất cả";
    private float currentDistance = 100.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        initViews();
        setupRecyclerView();
        setupSearchView();
        requestLocation();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        searchView = findViewById(R.id.searchView);
        btnFilter = findViewById(R.id.btnFilter);
        recyclerViewResults = findViewById(R.id.recyclerViewResults);
        textViewPlaceholder = findViewById(R.id.textViewPlaceholder);
        progressBar = findViewById(R.id.progressBar);

        btnFilter.setOnClickListener(v -> openFilterDialog());
    }

    private void setupRecyclerView() {
        resultList = new ArrayList<>();
        adapter = new ListingAdapter(this, resultList);
        recyclerViewResults.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerViewResults.setAdapter(adapter);

        adapter.setOnItemClickListener(listing -> {
            Intent intent = new Intent(this, ListingDetailActivity.class);
            intent.putExtra("LISTING_ID", listing.getListingId());
            startActivity(intent);
        });
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchHandler.removeCallbacks(searchRunnable);
                performSearch(query);
                hideKeyboard();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> performSearch(newText);
                searchHandler.postDelayed(searchRunnable, 500);
                return true;
            }
        });
    }

    private void openFilterDialog() {
        FilterBottomSheetFragment filterSheet = FilterBottomSheetFragment.newInstance(
                currentSortBy,
                currentSortDirection == Query.Direction.ASCENDING,
                currentMinPrice,
                currentMaxPrice,
                currentCategory,
                currentCondition,
                currentDistance // Thêm tham số distance
        );
        filterSheet.setFilterListener(this);
        filterSheet.show(getSupportFragmentManager(), "FilterBottomSheetFragment");
    }

    @Override
    public void onFilterApplied(String sortBy, boolean isAscending, float minPrice, float maxPrice, String category, String condition, float distance) {
        this.currentSortBy = sortBy;
        this.currentSortDirection = isAscending ? Query.Direction.ASCENDING : Query.Direction.DESCENDING;
        this.currentMinPrice = minPrice;
        this.currentMaxPrice = maxPrice;
        this.currentCategory = category;
        this.currentCondition = "Tất cả".equalsIgnoreCase(condition) ? "" : condition;
        this.currentDistance = distance;
        performSearch(searchView.getQuery().toString());
    }

    private void requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    lastKnownLocation = location;
                    performSearch(searchView.getQuery().toString());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get location", e);
                    performSearch(searchView.getQuery().toString());
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocation();
            } else {
                Toast.makeText(this, "Quyền vị trí bị từ chối. Không thể lọc theo khoảng cách.", Toast.LENGTH_SHORT).show();
                performSearch(searchView.getQuery().toString());
            }
        }
    }

    private void performSearch(String keyword) {
        currentKeyword = keyword.toLowerCase(Locale.ROOT).trim();
        buildAndExecuteQuery();
    }

    private void buildAndExecuteQuery() {
        progressBar.setVisibility(View.VISIBLE);
        textViewPlaceholder.setVisibility(View.GONE);
        recyclerViewResults.setVisibility(View.GONE);

        Query query = db.collection("listings").whereEqualTo("status", "available");

        if (!currentKeyword.isEmpty()) {
            query = query.whereArrayContains("tags", currentKeyword);
        }
        if (!"Tất cả".equals(currentCategory)) {
            query = query.whereEqualTo("category", currentCategory);
        }
        if (currentCondition != null && !currentCondition.isEmpty()) {
            query = query.whereEqualTo("condition", currentCondition);
        }
        if (currentMinPrice > 0) {
            query = query.whereGreaterThanOrEqualTo("price", currentMinPrice);
        }
        if (currentMaxPrice < 50000000) {
            query = query.whereLessThanOrEqualTo("price", currentMaxPrice);
        }

        if (currentSortBy.equals("price")) {
            query = query.orderBy(currentSortBy, currentSortDirection);
        } else {
            if (currentMinPrice > 0 || currentMaxPrice < 50000000) {
                query = query.orderBy("price");
            }
            query = query.orderBy(currentSortBy, currentSortDirection);
        }

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            progressBar.setVisibility(View.GONE);
            resultList.clear();
            List<DataModels.Listing> fetchedListings = queryDocumentSnapshots.toObjects(DataModels.Listing.class);

            List<DataModels.Listing> finalList;
            if (currentDistance >= 100.0f || lastKnownLocation == null) {
                finalList = fetchedListings;
            } else {
                finalList = fetchedListings.stream()
                        .filter(listing -> {
                            if (listing.getLocationGeoPoint() == null) return false;
                            Location itemLocation = new Location("");
                            itemLocation.setLatitude(listing.getLocationGeoPoint().getLatitude());
                            itemLocation.setLongitude(listing.getLocationGeoPoint().getLongitude());
                            float distanceInKm = lastKnownLocation.distanceTo(itemLocation) / 1000;
                            return distanceInKm <= currentDistance;
                        })
                        .collect(Collectors.toList());
            }

            if (finalList.isEmpty()) {
                textViewPlaceholder.setVisibility(View.VISIBLE);
                textViewPlaceholder.setText("Không tìm thấy kết quả nào");
            } else {
                recyclerViewResults.setVisibility(View.VISIBLE);
                resultList.addAll(finalList);
            }
            adapter.notifyDataSetChanged();
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            textViewPlaceholder.setVisibility(View.VISIBLE);
            textViewPlaceholder.setText("Lỗi: " + e.getMessage());
            Log.e(TAG, "Search query failed. Check Firestore Indexes.", e);
        });
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}