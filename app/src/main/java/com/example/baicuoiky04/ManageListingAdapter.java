package com.example.baicuoiky04;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ManageListingAdapter extends RecyclerView.Adapter<ManageListingAdapter.ViewHolder> {

    public interface OnActionListener {
        void onEdit(DataModels.Listing listing);
        void onDelete(DataModels.Listing listing);
        void onChangeStatus(DataModels.Listing listing);
    }

    private Context context;
    private List<DataModels.Listing> listings;
    private OnActionListener listener;

    public ManageListingAdapter(Context context, List<DataModels.Listing> listings, OnActionListener listener) {
        this.context = context;
        this.listings = listings;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_manage_listing, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DataModels.Listing listing = listings.get(position);
        holder.bind(listing, listener);
    }

    @Override
    public int getItemCount() {
        return listings.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewProduct;
        TextView textViewTitle, textViewPrice, textViewStatus, textViewViews, textViewOffers;
        ImageButton buttonMoreOptions;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewProduct = itemView.findViewById(R.id.imageViewProductManage);
            textViewTitle = itemView.findViewById(R.id.textViewTitleManage);
            textViewPrice = itemView.findViewById(R.id.textViewPriceManage);
            textViewStatus = itemView.findViewById(R.id.textViewStatusManage);
            buttonMoreOptions = itemView.findViewById(R.id.buttonMoreOptions);
            textViewViews = itemView.findViewById(R.id.textViewViews);
            textViewOffers = itemView.findViewById(R.id.textViewOffers);
        }

        void bind(DataModels.Listing listing, OnActionListener listener) {
            textViewTitle.setText(listing.getTitle());

            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            textViewPrice.setText(formatter.format(listing.getPrice()));

            String status = listing.getStatus();

            if ("available".equalsIgnoreCase(status)) {
                textViewStatus.setText("ĐANG BÁN");
                textViewStatus.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_available)));
            } else if ("sold".equalsIgnoreCase(status)) {
                textViewStatus.setText("ĐÃ BÁN");
                textViewStatus.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_sold)));
            } else if ("paused".equalsIgnoreCase(status)) {
                textViewStatus.setText("TẠM DỪNG");
                textViewStatus.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_paused)));
            }

            textViewViews.setText(String.valueOf(listing.getViews()));
            textViewOffers.setText(String.valueOf(listing.getOffersCount()));

            if (listing.getImageUrls() != null && !listing.getImageUrls().isEmpty()) {
                Glide.with(context).load(listing.getImageUrls().get(0)).into(imageViewProduct);
            } else {
                imageViewProduct.setImageResource(R.drawable.ic_image_placeholder);
            }

            buttonMoreOptions.setOnClickListener(v -> showPopupMenu(v, listing, listener));

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, OffersActivity.class);
                intent.putExtra("LISTING_ID", listing.getListingId());
                context.startActivity(intent);
            });
        }

        private void showPopupMenu(View view, DataModels.Listing listing, OnActionListener listener) {
            PopupMenu popup = new PopupMenu(context, view);
            popup.getMenuInflater().inflate(R.menu.manage_listing_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_edit) {
                    listener.onEdit(listing);
                    return true;
                } else if (itemId == R.id.action_delete) {
                    listener.onDelete(listing);
                    return true;
                } else if (itemId == R.id.action_change_status) {
                    listener.onChangeStatus(listing);
                    return true;
                }
                return false;
            });
            popup.show();
        }
    }
}