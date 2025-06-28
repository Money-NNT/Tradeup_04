package com.example.baicuoiky04;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.RangeSlider;

// ... imports
import java.util.List;

public class FilterBottomSheetFragment extends BottomSheetDialogFragment {

    // Interface để gửi dữ liệu về MainActivity
    public interface FilterListener {
        // Cập nhật interface để nhận các tham số
        void onFilterApplied(String sortBy, boolean isAscending, float minPrice, float maxPrice);
    }

    private FilterListener listener;

    // Các biến để nhận giá trị ban đầu từ MainActivity
    private String initialSortBy;
    private boolean initialIsAscending;
    private float initialMinPrice;
    private float initialMaxPrice;

    // Sửa hàm newInstance để nhận giá trị
    public static FilterBottomSheetFragment newInstance(String sortBy, boolean isAscending, float minPrice, float maxPrice) {
        FilterBottomSheetFragment fragment = new FilterBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString("SORT_BY", sortBy);
        args.putBoolean("IS_ASCENDING", isAscending);
        args.putFloat("MIN_PRICE", minPrice);
        args.putFloat("MAX_PRICE", maxPrice);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            initialSortBy = getArguments().getString("SORT_BY");
            initialIsAscending = getArguments().getBoolean("IS_ASCENDING");
            initialMinPrice = getArguments().getFloat("MIN_PRICE");
            initialMaxPrice = getArguments().getFloat("MAX_PRICE");
        }
    }

    public void setFilterListener(FilterListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_filter_bottom_sheet, container, false);

        ChipGroup chipGroupSort = view.findViewById(R.id.chipGroupSort);
        RangeSlider rangeSliderPrice = view.findViewById(R.id.rangeSliderPrice);
        Button btnApplyFilter = view.findViewById(R.id.btnApplyFilter);

        // Set giá trị ban đầu cho các view
        rangeSliderPrice.setValues(initialMinPrice, initialMaxPrice);
        // (Thêm logic để check chip sắp xếp ban đầu)

        btnApplyFilter.setOnClickListener(v -> {
            // Logic lấy dữ liệu từ các view
            String sortBy = "createdAt"; // Mặc định
            boolean isAscending = false;
            int selectedChipId = chipGroupSort.getCheckedChipId();
            if (selectedChipId == R.id.chipSortNewest) { // Giả sử bạn có ID cho chip
                sortBy = "price";
                isAscending = true;
            } else if (selectedChipId == R.id.chipSortNewest) {
                sortBy = "price";
                isAscending = false;
            }

            List<Float> values = rangeSliderPrice.getValues();
            float minPrice = values.get(0);
            float maxPrice = values.get(1);

            if (listener != null) {
                listener.onFilterApplied(sortBy, isAscending, minPrice, maxPrice);
            }
            dismiss();
        });

        return view;
    }
}