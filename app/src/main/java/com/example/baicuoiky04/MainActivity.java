package com.example.baicuoiky04;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;


public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnItemSelectedListener, FilterBottomSheetFragment.FilterListener {

    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;

    private RecyclerView recyclerViewListings;
    private FrameLayout fragmentContainer;
    private AppBarLayout appBarLayout;
    private ListingAdapter listingAdapter;
    private ProgressBar progressBar;
    private TextView fakeSearchView;
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabAddItem;

    private List<DataModels.Listing> listingList;
    private Location lastKnownLocation;

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
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        checkUserStatus();
        initViews();
        initRecyclerView();
        setupListeners();

        if (savedInstanceState == null) {
            showHomeContent();
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestLocationAndFetchData();
    }

    private void checkUserStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            goToLoginActivity();
        }
    }

    private void initViews() {
        recyclerViewListings = findViewById(R.id.recyclerViewListings);
        fragmentContainer = findViewById(R.id.fragment_container);
        appBarLayout = findViewById(R.id.appBarLayout);
        progressBar = findViewById(R.id.progressBar);
        fakeSearchView = findViewById(R.id.fakeSearchView);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        fabAddItem = findViewById(R.id.fabAddItem);
    }

    private void initRecyclerView() {
        listingList = new ArrayList<>();
        listingAdapter = new ListingAdapter(this, listingList);
        recyclerViewListings.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerViewListings.setAdapter(listingAdapter);
    }

    private void setupListeners() {
        fakeSearchView.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SearchActivity.class)));
        fabAddItem.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AddListingActivity.class)));
        bottomNavigationView.setOnItemSelectedListener(this);
        bottomNavigationView.setBackground(null);

        listingAdapter.setOnItemClickListener(listing -> {
            Intent intent = new Intent(MainActivity.this, ListingDetailActivity.class);
            intent.putExtra("LISTING_ID", listing.getListingId());
            startActivity(intent);
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        Fragment selectedFragment = null;

        if (itemId == R.id.nav_home) {
            showHomeContent();
            return true;
        } else if (itemId == R.id.nav_saved) {
            selectedFragment = new SavedListingsFragment();
        } else if (itemId == R.id.nav_manage) {
            selectedFragment = new ManageListingsFragment();
        } else if (itemId == R.id.nav_profile) {
            selectedFragment = new ProfileFragment();
        }

        if (selectedFragment != null) {
            loadFragment(selectedFragment);
        }
        return true;
    }

    private void loadFragment(Fragment fragment) {
        recyclerViewListings.setVisibility(View.GONE);
        appBarLayout.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.VISIBLE);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void showHomeContent() {
        fragmentContainer.setVisibility(View.GONE);
        appBarLayout.setVisibility(View.VISIBLE);
        recyclerViewListings.setVisibility(View.VISIBLE);

        getSupportFragmentManager().getFragments().forEach(fragment ->
                getSupportFragmentManager().beginTransaction().remove(fragment).commit()
        );
    }

    private void openFilterDialog() {
        // This method is now handled by the SearchActivity.
        // If needed here, you'd call FilterBottomSheetFragment.
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

        fetchListings();
    }

    private void requestLocationAndFetchData() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    lastKnownLocation = location;
                    if (recyclerViewListings.getVisibility() == View.VISIBLE) {
                        fetchListings();
                    }
                })
                .addOnFailureListener(e -> {
                    if (recyclerViewListings.getVisibility() == View.VISIBLE) {
                        fetchListings();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationAndFetchData();
            } else {
                Toast.makeText(this, "Quyền vị trí bị từ chối. Không thể lọc theo khoảng cách.", Toast.LENGTH_SHORT).show();
                fetchListings();
            }
        }
    }

    private void fetchListings() {
        progressBar.setVisibility(View.VISIBLE);

        Query query = db.collection("listings").whereEqualTo("status", "available");

        if (!currentCategory.equals("Tất cả")) {
            query = query.whereEqualTo("category", currentCategory);
        }
        if (currentCondition != null && !currentCondition.isEmpty() && !"Tất cả".equalsIgnoreCase(currentCondition)) {
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

        query.get().addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                List<DataModels.Listing> fetchedListings = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    fetchedListings.add(document.toObject(DataModels.Listing.class));
                }
                filterAndDisplayListings(fetchedListings);
            } else {
                Toast.makeText(MainActivity.this, "Lỗi tải dữ liệu.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterAndDisplayListings(List<DataModels.Listing> fetchedListings) {
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

        listingList.clear();
        listingList.addAll(finalList);
        listingAdapter.notifyDataSetChanged();

        if (listingList.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy sản phẩm nào phù hợp.", Toast.LENGTH_SHORT).show();
        }
    }

    private void goToLoginActivity() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (fragmentContainer.getVisibility() == View.VISIBLE) {
            showHomeContent();
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        } else {
            super.onBackPressed();
        }
    }
}