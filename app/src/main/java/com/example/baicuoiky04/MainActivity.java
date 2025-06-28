package com.example.baicuoiky04;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements FilterBottomSheetFragment.FilterListener, BottomNavigationView.OnItemSelectedListener {

    private static final String TAG = "MainActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private RecyclerView recyclerViewListings;
    private FrameLayout fragmentContainer;
    private AppBarLayout appBarLayout;
    private ListingAdapter listingAdapter;
    private ProgressBar progressBar;
    private SearchView searchView;
    private MaterialButton btnFilter;
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabAddItem;

    private List<DataModels.Listing> listingList;

    private String currentKeyword = "";
    private String currentSortBy = "createdAt";
    private Query.Direction currentSortDirection = Query.Direction.DESCENDING;
    private float currentMinPrice = 0;
    private float currentMaxPrice = 50000000;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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
        if (recyclerViewListings.getVisibility() == View.VISIBLE) {
            fetchListings();
        }
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
        searchView = findViewById(R.id.searchView);
        btnFilter = findViewById(R.id.btnFilter);
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
        btnFilter.setOnClickListener(v -> openFilterDialog());
        fabAddItem.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AddListingActivity.class)));
        bottomNavigationView.setOnItemSelectedListener(this);
        bottomNavigationView.setBackground(null);

        listingAdapter.setOnItemClickListener(listing -> {
            Intent intent = new Intent(MainActivity.this, ListingDetailActivity.class);
            intent.putExtra("LISTING_ID", listing.getListingId());
            startActivity(intent);
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchHandler.removeCallbacks(searchRunnable);
                currentKeyword = query.toLowerCase(Locale.ROOT).trim();
                fetchListings();
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> {
                    currentKeyword = newText.toLowerCase(Locale.ROOT).trim();
                    fetchListings();
                };
                searchHandler.postDelayed(searchRunnable, 500);
                return true;
            }
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

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
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
        FilterBottomSheetFragment filterSheet = FilterBottomSheetFragment.newInstance(
                currentSortBy,
                currentSortDirection == Query.Direction.ASCENDING,
                currentMinPrice,
                currentMaxPrice
        );
        filterSheet.setFilterListener(this);
        filterSheet.show(getSupportFragmentManager(), "FilterBottomSheetFragment");
    }

    @Override
    public void onFilterApplied(String sortBy, boolean isAscending, float minPrice, float maxPrice) {
        this.currentSortBy = sortBy;
        this.currentSortDirection = isAscending ? Query.Direction.ASCENDING : Query.Direction.DESCENDING;
        this.currentMinPrice = minPrice;
        this.currentMaxPrice = maxPrice;
        fetchListings();
    }

    private void fetchListings() {
        progressBar.setVisibility(View.VISIBLE);
        Log.d(TAG, "Đang lấy danh sách với từ khóa: '" + currentKeyword + "'");

        Query query = db.collection("listings").whereEqualTo("status", "available");

        if (!currentKeyword.isEmpty()) {
            query = query.whereArrayContains("tags", currentKeyword);
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

        query.get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        listingList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            listingList.add(document.toObject(DataModels.Listing.class));
                        }
                        listingAdapter.notifyDataSetChanged(); // Đã thay từ 'adapter' thành 'listingAdapter'
                        if (listingList.isEmpty()) {
                            Toast.makeText(this, "Không tìm thấy sản phẩm nào.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Lỗi khi lấy tài liệu: ", task.getException());
                        Toast.makeText(MainActivity.this, "Lỗi tải dữ liệu: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
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