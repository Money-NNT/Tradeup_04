package com.example.baicuoiky04;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class HomeFeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<DataModels.HomeFeedItem> feedItems;
    private Context context;

    public HomeFeedAdapter(Context context, List<DataModels.HomeFeedItem> feedItems) {
        this.context = context;
        this.feedItems = feedItems;
    }

    @Override
    public int getItemViewType(int position) {
        return feedItems.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == DataModels.HomeFeedItem.TYPE_HEADER) {
            View view = inflater.inflate(R.layout.home_item_header, parent, false);
            return new HeaderViewHolder(view);
        } else if (viewType == DataModels.HomeFeedItem.TYPE_HORIZONTAL_LIST) {
            View view = inflater.inflate(R.layout.home_item_horizontal_list, parent, false);
            return new HorizontalListViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.list_item_listing, parent, false);
            return new ListingAdapter.ListingViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DataModels.HomeFeedItem item = feedItems.get(position);
        if (holder.getItemViewType() == DataModels.HomeFeedItem.TYPE_HEADER) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            headerHolder.textViewHeader.setText(item.headerTitle);
        } else if (holder.getItemViewType() == DataModels.HomeFeedItem.TYPE_HORIZONTAL_LIST) {
            HorizontalListViewHolder listHolder = (HorizontalListViewHolder) holder;
            FeaturedListingAdapter horizontalAdapter = new FeaturedListingAdapter(context, item.listings);
            listHolder.recyclerViewHorizontal.setAdapter(horizontalAdapter);
        } else {
            ListingAdapter.ListingViewHolder gridHolder = (ListingAdapter.ListingViewHolder) holder;
            DataModels.Listing listing = item.singleListing;

            gridHolder.textViewTitle.setText(listing.getTitle());
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            gridHolder.textViewPrice.setText(formatter.format(listing.getPrice()));
            gridHolder.textViewLocation.setText(listing.getLocationName());

            float distance = listing.getDistanceToUser();
            if (distance != -1) {
                gridHolder.textViewDistance.setVisibility(View.VISIBLE);
                if (distance < 1000) {
                    gridHolder.textViewDistance.setText(String.format(Locale.US, "• %.0f m", distance));
                } else {
                    gridHolder.textViewDistance.setText(String.format(Locale.US, "• %.1f km", distance / 1000));
                }
            } else {
                gridHolder.textViewDistance.setVisibility(View.GONE);
            }

            if (listing.getImageUrls() != null && !listing.getImageUrls().isEmpty()) {
                Glide.with(context).load(listing.getImageUrls().get(0)).into(gridHolder.imageViewProduct);
            } else {
                gridHolder.imageViewProduct.setImageResource(R.drawable.ic_image_placeholder);
            }

            gridHolder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ListingDetailActivity.class);
                intent.putExtra("LISTING_ID", listing.getListingId());
                context.startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        return feedItems.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView textViewHeader;
        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewHeader = itemView.findViewById(R.id.textViewHeader);
        }
    }

    static class HorizontalListViewHolder extends RecyclerView.ViewHolder {
        RecyclerView recyclerViewHorizontal;
        HorizontalListViewHolder(@NonNull View itemView) {
            super(itemView);
            recyclerViewHorizontal = itemView.findViewById(R.id.recyclerViewHorizontal);
        }
    }
}