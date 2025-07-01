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
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class FeaturedListingAdapter extends RecyclerView.Adapter<FeaturedListingAdapter.ViewHolder> {

    private Context context;
    private List<DataModels.Listing> listings;

    public FeaturedListingAdapter(Context context, List<DataModels.Listing> listings) {
        this.context = context;
        this.listings = listings;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_featured, parent, false);
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
        TextView textViewTitle, textViewPrice, textViewLocation;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewProduct = itemView.findViewById(R.id.imageViewProduct);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewPrice = itemView.findViewById(R.id.textViewPrice);
            textViewLocation = itemView.findViewById(R.id.textViewLocation);
        }

        void bind(DataModels.Listing listing) {
            textViewTitle.setText(listing.getTitle());

            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            textViewPrice.setText(formatter.format(listing.getPrice()));

            textViewLocation.setText(listing.getLocationName());

            if (listing.getImageUrls() != null && !listing.getImageUrls().isEmpty()) {
                Glide.with(context).load(listing.getImageUrls().get(0)).into(imageViewProduct);
            }

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ListingDetailActivity.class);
                intent.putExtra("LISTING_ID", listing.getListingId());
                context.startActivity(intent);
            });
        }
    }
}