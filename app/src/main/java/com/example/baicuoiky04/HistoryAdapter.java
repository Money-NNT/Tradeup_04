package com.example.baicuoiky04;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private Context context;
    private List<DataModels.Listing> historyList;
    private String historyType; // Để biết đây là lịch sử mua hay bán

    public HistoryAdapter(Context context, List<DataModels.Listing> list, String historyType) {
        this.context = context;
        this.historyList = list;
        this.historyType = historyType;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DataModels.Listing listing = historyList.get(position);
        holder.bind(listing);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewProduct;
        TextView textViewTitle, textViewPrice;
        MaterialButton btnReview;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewProduct = itemView.findViewById(R.id.imageViewProduct);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewPrice = itemView.findViewById(R.id.textViewPrice);
            btnReview = itemView.findViewById(R.id.btnReview);
        }

        void bind(DataModels.Listing listing) {
            textViewTitle.setText(listing.getTitle());
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            textViewPrice.setText(formatter.format(listing.getPrice()));

            if (listing.getImageUrls() != null && !listing.getImageUrls().isEmpty()) {
                Glide.with(context).load(listing.getImageUrls().get(0)).into(imageViewProduct);
            }

            // Chỉ hiện nút "Đánh giá" trong Lịch sử mua hàng và khi sản phẩm đã bán
            if (HistoryActivity.TYPE_PURCHASE.equals(historyType) && "sold".equals(listing.getStatus())) {
                btnReview.setVisibility(View.VISIBLE);
                btnReview.setOnClickListener(v -> {
                    Intent intent = new Intent(context, ReviewActivity.class);
                    intent.putExtra("LISTING_ID", listing.getListingId());
                    intent.putExtra("USER_ID_TO_REVIEW", listing.getSellerId());
                    intent.putExtra("USER_NAME_TO_REVIEW", listing.getSellerName());
                    context.startActivity(intent);
                });
            } else {
                btnReview.setVisibility(View.GONE);
            }
        }
    }
}