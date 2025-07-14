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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;

    private RecyclerView recyclerViewHome;
    private FrameLayout fragmentContainer;
    private AppBarLayout appBarLayout;
    private HomeFeedAdapter homeFeedAdapter;
    private ProgressBar progressBar;
    private TextView fakeSearchView;
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabAddItem;

    private List<DataModels.HomeFeedItem> homeFeedItems;
    private DocumentSnapshot lastVisible;
    private boolean isLoading = false;

    // Interface để xử lý callback sau khi lấy được vị trí
    interface LocationCallback {
        void onLocationFetched(Location location);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        checkUserStatus();
        initViews();
        initHomeFeedRecyclerView();
        setupListeners();

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }

    private void checkUserStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            goToLoginActivity();
        }
    }

    private void initViews() {
        recyclerViewHome = findViewById(R.id.recyclerViewHome);
        fragmentContainer = findViewById(R.id.fragment_container);
        appBarLayout = findViewById(R.id.appBarLayout);
        progressBar = findViewById(R.id.progressBar);
        fakeSearchView = findViewById(R.id.fakeSearchView);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        fabAddItem = findViewById(R.id.fabAddItem);
    }

    private void initHomeFeedRecyclerView() {
        homeFeedItems = new ArrayList<>();
        homeFeedAdapter = new HomeFeedAdapter(this, homeFeedItems);

        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position < homeFeedItems.size()) {
                    int type = homeFeedAdapter.getItemViewType(position);
                    if (type == DataModels.HomeFeedItem.TYPE_HEADER || type == DataModels.HomeFeedItem.TYPE_HORIZONTAL_LIST) {
                        return 2;
                    }
                }
                return 1;
            }
        });
        recyclerViewHome.setLayoutManager(layoutManager);
        recyclerViewHome.setAdapter(homeFeedAdapter);

        recyclerViewHome.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!isLoading && layoutManager.findLastCompletelyVisibleItemPosition() == homeFeedItems.size() - 1) {
                    loadMoreAllProducts();
                }
            }
        });
    }

    private void setupListeners() {
        fakeSearchView.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SearchActivity.class)));
        fabAddItem.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AddListingActivity.class)));
        bottomNavigationView.setOnNavigationItemSelectedListener(this);
        bottomNavigationView.setBackground(null);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        Fragment selectedFragment = null;
        if (itemId == R.id.nav_home) {
            showHomeContent();
            return true;
        } else if (itemId == R.id.nav_messages) {
            selectedFragment = new ConversationsFragment();
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
        appBarLayout.setVisibility(View.GONE);
        recyclerViewHome.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.VISIBLE);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }

    private void showHomeContent() {
        fragmentContainer.setVisibility(View.GONE);
        appBarLayout.setVisibility(View.VISIBLE);
        recyclerViewHome.setVisibility(View.VISIBLE);
        buildHomeFeed();
        getSupportFragmentManager().getFragments().forEach(fragment ->
                getSupportFragmentManager().beginTransaction().remove(fragment).commit()
        );
    }

    private void buildHomeFeed() {
        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);
        homeFeedItems.clear();

        fetchCurrentUserLocation(location -> {
            Task<QuerySnapshot> popularTask = db.collection("listings")
                    .whereEqualTo("status", "available")
                    .orderBy("views", Query.Direction.DESCENDING)
                    .limit(10).get();

            Task<QuerySnapshot> nearbyTask = db.collection("listings")
                    .whereEqualTo("status", "available")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(50).get();

            Tasks.whenAllSuccess(popularTask, nearbyTask).addOnSuccessListener(results -> {
                homeFeedItems.clear();

                QuerySnapshot popularResult = (QuerySnapshot) results.get(0);
                if (!popularResult.isEmpty()) {
                    homeFeedItems.add(new DataModels.HomeFeedItem(DataModels.HomeFeedItem.TYPE_HEADER, "Phổ biến nhất"));
                    homeFeedItems.add(new DataModels.HomeFeedItem(DataModels.HomeFeedItem.TYPE_HORIZONTAL_LIST, popularResult.toObjects(DataModels.Listing.class)));
                }

                if (location != null) {
                    QuerySnapshot nearbyResult = (QuerySnapshot) results.get(1);
                    List<DataModels.Listing> nearbyListings = filterAndSortByDistance(nearbyResult.toObjects(DataModels.Listing.class), location);
                    if (!nearbyListings.isEmpty()) {
                        homeFeedItems.add(new DataModels.HomeFeedItem(DataModels.HomeFeedItem.TYPE_HEADER, "Gần bạn"));
                        List<DataModels.Listing> topNearby = nearbyListings.subList(0, Math.min(10, nearbyListings.size()));
                        homeFeedItems.add(new DataModels.HomeFeedItem(DataModels.HomeFeedItem.TYPE_HORIZONTAL_LIST, topNearby));
                    }
                }

                homeFeedItems.add(new DataModels.HomeFeedItem(DataModels.HomeFeedItem.TYPE_HEADER, "Dành cho bạn"));

                homeFeedAdapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);

                lastVisible = null;
                loadMoreAllProducts();

            }).addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                isLoading = false;
                Log.e(TAG, "Error building home feed", e);
            });
        });
    }

    private void fetchCurrentUserLocation(LocationCallback callback) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Location permission not granted. Cannot fetch nearby items.");
            callback.onLocationFetched(null);
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        Log.d(TAG, "Current user location fetched for home feed.");
                    } else {
                        Log.d(TAG, "Could not fetch user location for home feed.");
                    }
                    callback.onLocationFetched(location);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get location", e);
                    callback.onLocationFetched(null);
                });
    }

    private List<DataModels.Listing> filterAndSortByDistance(List<DataModels.Listing> listings, Location currentUserLocation) {
        for (DataModels.Listing listing : listings) {
            if (listing.getLocationGeoPoint() != null) {
                Location itemLocation = new Location("");
                itemLocation.setLatitude(listing.getLocationGeoPoint().getLatitude());
                itemLocation.setLongitude(listing.getLocationGeoPoint().getLongitude());
                float distance = currentUserLocation.distanceTo(itemLocation);
                listing.setDistanceToUser(distance);
            } else {
                listing.setDistanceToUser(Float.MAX_VALUE);
            }
        }
        Collections.sort(listings, Comparator.comparing(DataModels.Listing::getDistanceToUser));
        return listings;
    }

    private void loadMoreAllProducts() {
        isLoading = true;
        Query allProductsQuery = db.collection("listings")
                .whereEqualTo("status", "available")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(10);
        if (lastVisible != null) {
            allProductsQuery = allProductsQuery.startAfter(lastVisible);
        }
        allProductsQuery.get().addOnSuccessListener(snapshots -> {
            if (!snapshots.isEmpty()) {
                lastVisible = snapshots.getDocuments().get(snapshots.size() - 1);
                for (QueryDocumentSnapshot doc : snapshots) {
                    DataModels.Listing listing = doc.toObject(DataModels.Listing.class);
                    homeFeedItems.add(new DataModels.HomeFeedItem(DataModels.HomeFeedItem.TYPE_GRID_LISTING, listing));
                }
                homeFeedAdapter.notifyDataSetChanged();
            }
            isLoading = false;
        }).addOnFailureListener(e -> isLoading = false);
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

    public void loadFragmentFromAnotherFragment(Fragment fragment) {
        appBarLayout.setVisibility(View.GONE);
        recyclerViewHome.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.VISIBLE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}