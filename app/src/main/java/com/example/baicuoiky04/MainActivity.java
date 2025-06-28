package com.example.baicuoiky04;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnItemSelectedListener {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private RecyclerView recyclerViewListings;
    private FrameLayout fragmentContainer;
    private AppBarLayout appBarLayout;
    private ListingAdapter listingAdapter;
    private ProgressBar progressBar;
    private TextView fakeSearchView;
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabAddItem;

    private List<DataModels.Listing> listingList;

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

    private void fetchListings() {
        progressBar.setVisibility(View.VISIBLE);
        Query query = db.collection("listings")
                .whereEqualTo("status", "available")
                .orderBy("createdAt", Query.Direction.DESCENDING);

        query.get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        listingList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            listingList.add(document.toObject(DataModels.Listing.class));
                        }
                        listingAdapter.notifyDataSetChanged();
                        if (listingList.isEmpty()) {
                            Toast.makeText(this, "Chưa có sản phẩm nào được đăng.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Lỗi tải dữ liệu.", Toast.LENGTH_SHORT).show();
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