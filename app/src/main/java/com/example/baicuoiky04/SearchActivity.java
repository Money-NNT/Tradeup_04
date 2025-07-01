package com.example.baicuoiky04;

import android.content.Context;
import android.content.Intent;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchActivity extends AppCompatActivity implements FilterBottomSheetFragment.FilterListener {

    private static final String TAG = "SearchActivity";
    private static final long DEBOUNCE_DELAY_MS = 300; // Tăng lên 300ms để trải nghiệm tốt hơn

    private SearchView searchView;
    private Button btnFilter;
    private RecyclerView recyclerViewResults;
    private ListingAdapter adapter;
    private TextView textViewPlaceholder;
    private ProgressBar progressBar;

    private List<DataModels.Listing> resultList;
    private FirebaseFirestore db;

    // --- CÁC BIẾN MỚI CHO DEBOUNCING ---
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    // ------------------------------------

    private String currentKeyword = "";
    private String currentSortBy = "createdAt";
    private Query.Direction currentSortDirection = Query.Direction.DESCENDING;
    private float currentMinPrice = 0;
    private float currentMaxPrice = 50000000;
    private String currentCategory = "Tất cả";
    private String currentCondition = "Tất cả";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        db = FirebaseFirestore.getInstance();
        initViews();
        setupRecyclerView();
        setupSearchView();
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

    // --- HÀM setupSearchView ĐƯỢC CẬP NHẬT HOÀN TOÀN ---
    private void setupSearchView() {
        searchView.setIconified(false); // Mở rộng thanh tìm kiếm ngay từ đầu
        searchView.requestFocus(); // Tự động focus để bàn phím hiện lên

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Khi người dùng nhấn nút tìm kiếm trên bàn phím
                searchHandler.removeCallbacks(searchRunnable); // Hủy bỏ các tìm kiếm đang chờ
                performSearch(query);
                hideKeyboard();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Khi người dùng gõ từng chữ
                searchHandler.removeCallbacks(searchRunnable); // Hủy bỏ tìm kiếm trước đó

                // Tạo một tác vụ tìm kiếm mới
                searchRunnable = () -> performSearch(newText);

                // Đặt lịch để thực hiện tác vụ này sau DEBOUNCE_DELAY_MS
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
                100.0f // Tạm thời chưa dùng distance trong Search
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
        performSearch(searchView.getQuery().toString());
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
            if (queryDocumentSnapshots.isEmpty()) {
                textViewPlaceholder.setVisibility(View.VISIBLE);
                textViewPlaceholder.setText("Không tìm thấy kết quả nào");
            } else {
                recyclerViewResults.setVisibility(View.VISIBLE);
                resultList.addAll(queryDocumentSnapshots.toObjects(DataModels.Listing.class));
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