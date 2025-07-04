package com.example.baicuoiky04;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ArchivedListingAdapter extends RecyclerView.Adapter<ArchivedListingAdapter.ViewHolder> {

    public interface OnRestoreListener {
        void onRestore(DataModels.Listing listing);
    }

    private Context context;
    private List<DataModels.Listing> listings;
    private OnRestoreListener listener;

    public ArchivedListingAdapter(Context context, List<DataModels.Listing> listings, OnRestoreListener listener) {
        this.context = context;
        this.listings = listings;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_archived_listing, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DataModels.Listing listing = listings.get(position);
        holder.bind(listing);
    }

    @Override
    public int getItemCount() {
        return listings.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewProduct;
        TextView textViewTitle, textViewPrice;
        Button btnRestore;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewProduct = itemView.findViewById(R.id.imageViewProductArchived);
            textViewTitle = itemView.findViewById(R.id.textViewTitleArchived);
            textViewPrice = itemView.findViewById(R.id.textViewPriceArchived);
            btnRestore = itemView.findViewById(R.id.btnRestore);
        }

        void bind(DataModels.Listing listing) {
            textViewTitle.setText(listing.getTitle());
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            textViewPrice.setText(formatter.format(listing.getPrice()));

            if (listing.getImageUrls() != null && !listing.getImageUrls().isEmpty()) {
                Glide.with(context).load(listing.getImageUrls().get(0)).into(imageViewProduct);
            }

            btnRestore.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRestore(listing);
                }
            });
        }
    }
}