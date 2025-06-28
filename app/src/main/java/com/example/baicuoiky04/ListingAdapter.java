package com.example.baicuoiky04;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ListingAdapter extends RecyclerView.Adapter<ListingAdapter.ListingViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(DataModels.Listing listing);
    }

    private Context context;
    private List<DataModels.Listing> listingList;
    private OnItemClickListener listener;
    private static final String TAG = "ListingAdapter";

    public ListingAdapter(Context context, List<DataModels.Listing> listingList) {
        this.context = context;
        this.listingList = listingList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ListingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_listing, parent, false);
        return new ListingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListingViewHolder holder, int position) {
        DataModels.Listing listing = listingList.get(position);

        holder.textViewTitle.setText(listing.getTitle());

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        holder.textViewPrice.setText(formatter.format(listing.getPrice()));

        holder.textViewLocation.setText(listing.getLocationName());

        if (listing.getImageUrls() != null && !listing.getImageUrls().isEmpty()) {
            String imageUrl = listing.getImageUrls().get(0);
            Log.d(TAG, "Loading image for '" + listing.getTitle() + "' from URL: " + imageUrl);

            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.color.light_gray)
                    .error(R.drawable.ic_image_broken)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.imageViewProduct);
        } else {
            Log.w(TAG, "No image URL for listing: " + listing.getTitle());
            holder.imageViewProduct.setImageResource(R.drawable.ic_image_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(listing);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listingList.size();
    }

    public static class ListingViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewProduct;
        TextView textViewTitle, textViewPrice, textViewLocation;

        public ListingViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewProduct = itemView.findViewById(R.id.imageViewProduct);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewPrice = itemView.findViewById(R.id.textViewPrice);
            textViewLocation = itemView.findViewById(R.id.textViewLocation);
        }
    }
}