package com.example.baicuoiky04;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class MyOffersAdapter extends RecyclerView.Adapter<MyOffersAdapter.ViewHolder> {

    public interface OnActionListener {
        void onPayClicked(DataModels.OfferWithListing item);
        void onMakeAnotherOfferClicked(DataModels.OfferWithListing item);
    }

    private List<DataModels.OfferWithListing> offerWithListingList;
    private Context context;
    private OnActionListener listener; // Thêm biến listener

    public MyOffersAdapter(Context context, List<DataModels.OfferWithListing> list) {
        this.context = context;
        this.offerWithListingList = list;
    }

    public void setOnActionListener(OnActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_my_offer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DataModels.OfferWithListing item = offerWithListingList.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return offerWithListingList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewProductTitle, textViewMyOfferPrice, textViewOfferStatus;
        Button btnPay, btnMakeAnotherOffer;
        LinearLayout layoutActions;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewProductTitle = itemView.findViewById(R.id.textViewProductTitle);
            textViewMyOfferPrice = itemView.findViewById(R.id.textViewMyOfferPrice);
            textViewOfferStatus = itemView.findViewById(R.id.textViewOfferStatus);
            btnPay = itemView.findViewById(R.id.btnPay);
            btnMakeAnotherOffer = itemView.findViewById(R.id.btnMakeAnotherOffer);
            layoutActions = itemView.findViewById(R.id.layoutActions);
        }

        void bind(DataModels.OfferWithListing item) {
            DataModels.Listing listing = item.getListing();
            DataModels.Offer offer = item.getOffer();

            textViewProductTitle.setText(listing.getTitle());
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            textViewMyOfferPrice.setText(formatter.format(offer.getOfferPrice()));

            String status = offer.getStatus();
            switch (status.toLowerCase()) {
                case "accepted":
                    textViewOfferStatus.setText("Đã được chấp nhận");
                    textViewOfferStatus.setTextColor(ContextCompat.getColor(context, R.color.status_available));
                    layoutActions.setVisibility(View.VISIBLE);
                    btnPay.setVisibility(View.VISIBLE);
                    btnMakeAnotherOffer.setVisibility(View.GONE);
                    break;
                case "rejected":
                    textViewOfferStatus.setText("Bị từ chối");
                    textViewOfferStatus.setTextColor(ContextCompat.getColor(context, R.color.status_sold));
                    layoutActions.setVisibility(View.VISIBLE);
                    btnPay.setVisibility(View.GONE);
                    btnMakeAnotherOffer.setVisibility(View.VISIBLE);
                    break;
                case "pending":
                default:
                    textViewOfferStatus.setText("Đang chờ");
                    textViewOfferStatus.setTextColor(ContextCompat.getColor(context, R.color.status_paused));
                    layoutActions.setVisibility(View.GONE);
                    break;
            }

            btnPay.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPayClicked(item);
                }
            });

            btnMakeAnotherOffer.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMakeAnotherOfferClicked(item);
                }
            });
        }
    }
}