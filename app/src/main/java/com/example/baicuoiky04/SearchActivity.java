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

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class SearchActivity extends AppCompatActivity implements FilterBottomSheetFragment.FilterListener {

    private static final String TAG = "SearchActivity";
    private static final long DEBOUNCE_DELAY_MS = 300; // Có thể đổi thành 200ms

    private SearchView searchView;
    private Button btnFilter;
    private RecyclerView recyclerViewResults;
    private ListingAdapter adapter;
    private TextView textViewPlaceholder;
    private ProgressBar progressBar;

    private List<DataModels.Listing> resultList;
    private FirebaseFirestore db;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private FusedLocationProviderClient fusedLocationClient;
    private Location currentUserLocation = null;
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
        initViews();
        setupRecyclerView();
        setupSearchView();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fetchCurrentUserLocation();
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
        searchView.setIconified(false);
        searchView.requestFocus();
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
                searchHandler.postDelayed(searchRunnable, DEBOUNCE_DELAY_MS);
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
                currentDistance
        );
        filterSheet.setFilterListener(this);
        filterSheet.show(getSupportFragmentManager(), "FilterBottomSheetFragment");
    }

    @Override
    public void onFilterApplied(String sortBy, boolean isAscending, float minPrice, float maxPrice, String category, String condition, float distance) {
        if (distance < 100.0f && currentUserLocation == null) {
            Toast.makeText(this, "Không thể tìm theo khoảng cách. Vui lòng bật GPS và thử lại.", Toast.LENGTH_LONG).show();
            return;
        }
        this.currentSortBy = sortBy;
        this.currentSortDirection = isAscending ? Query.Direction.ASCENDING : Query.Direction.DESCENDING;
        this.currentMinPrice = minPrice;
        this.currentMaxPrice = maxPrice;
        this.currentCategory = category;
        this.currentCondition = "Tất cả".equalsIgnoreCase(condition) ? "" : condition;
        this.currentDistance = distance;
        performSearch(searchView.getQuery().toString());
    }

    private void fetchCurrentUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Location permission not granted.");
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentUserLocation = location;
                        Log.d(TAG, "Current user location fetched: " + currentUserLocation.getLatitude() + "," + currentUserLocation.getLongitude());
                    } else {
                        Log.d(TAG, "Could not fetch user location.");
                    }
                });
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

        if (!currentKeyword.isEmpty()) { query = query.whereArrayContains("tags", currentKeyword); }
        if (!"Tất cả".equals(currentCategory)) { query = query.whereEqualTo("category", currentCategory); }
        if (currentCondition != null && !currentCondition.isEmpty()) { query = query.whereEqualTo("condition", currentCondition); }
        if (currentMinPrice > 0) { query = query.whereGreaterThanOrEqualTo("price", currentMinPrice); }
        if (currentMaxPrice < 50000000) { query = query.whereLessThanOrEqualTo("price", currentMaxPrice); }

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<DataModels.Listing> initialResults = queryDocumentSnapshots.toObjects(DataModels.Listing.class);

            List<DataModels.Listing> finalResults;
            boolean isDistanceSort = (currentDistance < 100.0f && currentUserLocation != null);

            if (isDistanceSort) {
                finalResults = filterAndCalculateDistance(initialResults);
            } else {
                finalResults = initialResults;
            }

            sortFinalResults(finalResults, isDistanceSort);
            updateUIWithResults(finalResults);

        }).addOnFailureListener(this::handleQueryFailure);
    }

    private List<DataModels.Listing> filterAndCalculateDistance(List<DataModels.Listing> listings) {
        final double radiusInMeters = currentDistance * 1000;
        List<DataModels.Listing> listingsInRadius = new ArrayList<>();

        for (DataModels.Listing listing : listings) {
            if (listing.getLocationGeoPoint() != null) {
                Location itemLocation = new Location("");
                itemLocation.setLatitude(listing.getLocationGeoPoint().getLatitude());
                itemLocation.setLongitude(listing.getLocationGeoPoint().getLongitude());
                float distance = currentUserLocation.distanceTo(itemLocation);
                if (distance <= radiusInMeters) {
                    listing.setDistanceToUser(distance);
                    listingsInRadius.add(listing);
                }
            }
        }
        return listingsInRadius;
    }

    private void sortFinalResults(List<DataModels.Listing> listings, boolean isDistanceSort) {
        if (isDistanceSort) {
            Collections.sort(listings, Comparator.comparing(DataModels.Listing::getDistanceToUser));
        } else {
            if ("price".equals(currentSortBy)) {
                if (currentSortDirection == Query.Direction.ASCENDING) {
                    Collections.sort(listings, Comparator.comparingLong(DataModels.Listing::getPrice));
                } else {
                    Collections.sort(listings, Comparator.comparingLong(DataModels.Listing::getPrice).reversed());
                }
            } else {
                Collections.sort(listings, (l1, l2) -> {
                    if (l1.getCreatedAt() == null || l2.getCreatedAt() == null) return 0;
                    return l2.getCreatedAt().compareTo(l1.getCreatedAt());
                });
            }
        }
    }

    private void updateUIWithResults(List<DataModels.Listing> listings) {
        progressBar.setVisibility(View.GONE);
        resultList.clear();
        if (listings.isEmpty()) {
            textViewPlaceholder.setVisibility(View.VISIBLE);
            textViewPlaceholder.setText("Không tìm thấy kết quả nào");
        } else {
            recyclerViewResults.setVisibility(View.VISIBLE);
            resultList.addAll(listings);
        }
        adapter.notifyDataSetChanged();
    }

    private void handleQueryFailure(Exception e) {
        progressBar.setVisibility(View.GONE);
        textViewPlaceholder.setVisibility(View.VISIBLE);
        textViewPlaceholder.setText("Lỗi: " + e.getMessage());
        Log.e(TAG, "Search query failed. Check Firestore Indexes.", e);
        Toast.makeText(this, "Query lỗi. Vui lòng kiểm tra Logcat để lấy link tạo Index.", Toast.LENGTH_LONG).show();
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