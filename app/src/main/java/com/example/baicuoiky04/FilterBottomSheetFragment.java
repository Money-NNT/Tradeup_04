package com.example.baicuoiky04;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.RangeSlider;

import java.util.List;

public class FilterBottomSheetFragment extends BottomSheetDialogFragment {

    // Nâng cấp Interface để gửi thêm các bộ lọc mới
    public interface FilterListener {
        void onFilterApplied(String sortBy, boolean isAscending, float minPrice, float maxPrice, String category, String condition);
    }

    private FilterListener listener;

    // Views
    private ChipGroup chipGroupSort, chipGroupCondition;
    private RangeSlider rangeSliderPrice;
    private Spinner spinnerCategory;
    private Button btnApplyFilter, btnResetFilter;

    // Biến để lưu trạng thái ban đầu (sẽ được phát triển sau nếu cần)

    // Sửa hàm newInstance để nhận nhiều giá trị hơn
    public static FilterBottomSheetFragment newInstance(String sortBy, boolean isAscending, float minPrice, float maxPrice, String category, String condition) {
        FilterBottomSheetFragment fragment = new FilterBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString("SORT_BY", sortBy);
        args.putBoolean("IS_ASCENDING", isAscending);
        args.putFloat("MIN_PRICE", minPrice);
        args.putFloat("MAX_PRICE", maxPrice);
        args.putString("CATEGORY", category);
        args.putString("CONDITION", condition);
        fragment.setArguments(args);
        return fragment;
    }

    public void setFilterListener(FilterListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_filter_bottom_sheet, container, false);

        // Ánh xạ Views
        chipGroupSort = view.findViewById(R.id.chipGroupSort);
        chipGroupCondition = view.findViewById(R.id.chipGroupCondition);
        rangeSliderPrice = view.findViewById(R.id.rangeSliderPrice);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        btnApplyFilter = view.findViewById(R.id.btnApplyFilter);
        btnResetFilter = view.findViewById(R.id.btnResetFilter);

        // Setup Spinner
        setupSpinners();

        // Setup trạng thái ban đầu cho các View (phần này sẽ được nâng cấp sau)
        // ...

        btnApplyFilter.setOnClickListener(v -> applyFilters());
        btnResetFilter.setOnClickListener(v -> resetFilters());

        return view;
    }

    private void setupSpinners() {
        // Thêm "Tất cả" vào đầu danh sách
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.category_array_with_all, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);
    }

    private void applyFilters() {
        if (listener == null) {
            dismiss();
            return;
        }

        // 1. Lấy dữ liệu Sắp xếp (Sort)
        String sortBy = "createdAt";
        boolean isAscending = false;
        int selectedSortId = chipGroupSort.getCheckedChipId();
        if (selectedSortId == R.id.chipSortPriceAsc) {
            sortBy = "price";
            isAscending = true;
        } else if (selectedSortId == R.id.chipSortPriceDesc) {
            sortBy = "price";
            isAscending = false;
        }

        // 2. Lấy dữ liệu Khoảng giá (Price Range)
        List<Float> values = rangeSliderPrice.getValues();
        float minPrice = values.get(0);
        float maxPrice = values.get(1);

        // 3. Lấy dữ liệu Danh mục (Category)
        String category = spinnerCategory.getSelectedItem().toString();

        // 4. Lấy dữ liệu Tình trạng (Condition)
        String condition = "";
        int selectedConditionId = chipGroupCondition.getCheckedChipId();
        if (selectedConditionId != View.NO_ID) {
            Chip selectedChip = chipGroupCondition.findViewById(selectedConditionId);
            condition = selectedChip.getText().toString();
        }

        // Gửi tất cả dữ liệu về MainActivity
        listener.onFilterApplied(sortBy, isAscending, minPrice, maxPrice, category, condition);
        dismiss();
    }

    private void resetFilters() {
        // Gửi về các giá trị mặc định
        if (listener != null) {
            listener.onFilterApplied("createdAt", false, 0, 50000000, "Tất cả", "Tất cả");
        }
        dismiss();
    }
}