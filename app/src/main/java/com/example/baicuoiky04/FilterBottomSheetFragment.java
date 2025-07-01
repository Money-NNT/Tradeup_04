package com.example.baicuoiky04;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.slider.Slider;

import java.util.List;
import java.util.Locale;

public class  FilterBottomSheetFragment extends BottomSheetDialogFragment {

    public interface FilterListener {
        void onFilterApplied(String sortBy, boolean isAscending, float minPrice, float maxPrice, String category, String condition, float distance);
    }

    private FilterListener listener;

    private ChipGroup chipGroupSort, chipGroupCondition;
    private RangeSlider rangeSliderPrice;
    private Spinner spinnerCategory;
    private Slider sliderDistance;
    private TextView textViewDistanceValue;
    private Button btnApplyFilter, btnResetFilter;

    private String initialSortBy;
    private boolean initialIsAscending;
    private float initialMinPrice;
    private float initialMaxPrice;
    private String initialCategory;
    private String initialCondition;
    private float initialDistance;

    public static FilterBottomSheetFragment newInstance(String sortBy, boolean isAscending, float minPrice, float maxPrice, String category, String condition, float distance) {
        FilterBottomSheetFragment fragment = new FilterBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString("SORT_BY", sortBy);
        args.putBoolean("IS_ASCENDING", isAscending);
        args.putFloat("MIN_PRICE", minPrice);
        args.putFloat("MAX_PRICE", maxPrice);
        args.putString("CATEGORY", category);
        args.putString("CONDITION", condition);
        args.putFloat("DISTANCE", distance);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            initialSortBy = getArguments().getString("SORT_BY", "createdAt");
            initialIsAscending = getArguments().getBoolean("IS_ASCENDING", false);
            initialMinPrice = getArguments().getFloat("MIN_PRICE", 0);
            initialMaxPrice = getArguments().getFloat("MAX_PRICE", 50000000);
            initialCategory = getArguments().getString("CATEGORY", "Tất cả");
            initialCondition = getArguments().getString("CONDITION", "Tất cả");
            initialDistance = getArguments().getFloat("DISTANCE", 100.0f);
        }
    }

    public void setFilterListener(FilterListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_filter_bottom_sheet, container, false);

        chipGroupSort = view.findViewById(R.id.chipGroupSort);
        chipGroupCondition = view.findViewById(R.id.chipGroupCondition);
        rangeSliderPrice = view.findViewById(R.id.rangeSliderPrice);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        sliderDistance = view.findViewById(R.id.sliderDistance);
        textViewDistanceValue = view.findViewById(R.id.textViewDistanceValue);
        btnApplyFilter = view.findViewById(R.id.btnApplyFilter);
        btnResetFilter = view.findViewById(R.id.btnResetFilter);

        setupSpinners();
        setInitialState();

        sliderDistance.addOnChangeListener((slider, value, fromUser) -> updateDistanceText(value));
        btnApplyFilter.setOnClickListener(v -> applyFilters());
        btnResetFilter.setOnClickListener(v -> resetFilters());

        return view;
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.category_array_with_all, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);
    }

    private void setInitialState() {
        rangeSliderPrice.setValues(initialMinPrice, initialMaxPrice);
        sliderDistance.setValue(initialDistance);
        updateDistanceText(initialDistance);

        if ("price".equals(initialSortBy)) {
            if (initialIsAscending) chipGroupSort.check(R.id.chipSortPriceAsc);
            else chipGroupSort.check(R.id.chipSortPriceDesc);
        } else {
            chipGroupSort.check(R.id.chipSortNewest);
        }

        if ("Mới".equals(initialCondition)) chipGroupCondition.check(R.id.chipConditionNew);
        else if ("Đã qua sử dụng".equals(initialCondition)) chipGroupCondition.check(R.id.chipConditionUsed);
        else chipGroupCondition.check(R.id.chipConditionAll);

        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerCategory.getAdapter();
        int position = adapter.getPosition(initialCategory);
        spinnerCategory.setSelection(position >= 0 ? position : 0);
    }

    private void updateDistanceText(float value) {
        if (value >= 100.0f) {
            textViewDistanceValue.setText("Mọi khoảng cách");
        } else {
            textViewDistanceValue.setText(String.format(Locale.US, "< %.0f km", value));
        }
    }

    private void applyFilters() {
        if (listener == null) {
            dismiss();
            return;
        }

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

        List<Float> values = rangeSliderPrice.getValues();
        float minPrice = values.get(0);
        float maxPrice = values.get(1);

        String category = spinnerCategory.getSelectedItem().toString();

        String condition = "";
        int selectedConditionId = chipGroupCondition.getCheckedChipId();
        if (selectedConditionId != View.NO_ID) {
            Chip selectedChip = chipGroupCondition.findViewById(selectedConditionId);
            condition = selectedChip.getText().toString();
        }

        float distance = sliderDistance.getValue();

        listener.onFilterApplied(sortBy, isAscending, minPrice, maxPrice, category, condition, distance);
        dismiss();
    }

    private void resetFilters() {
        if (listener != null) {
            listener.onFilterApplied("createdAt", false, 0, 50000000, "Tất cả", "Tất cả", 100.0f);
        }
        dismiss();
    }
}