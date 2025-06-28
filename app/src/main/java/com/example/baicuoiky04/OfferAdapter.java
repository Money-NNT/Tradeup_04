package com.example.baicuoiky04;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class OfferAdapter extends RecyclerView.Adapter<OfferAdapter.ViewHolder> {

    public interface OnOfferActionListener {
        void onAccept(DataModels.Offer offer, String offerId);
        void onReject(DataModels.Offer offer, String offerId);
        void onReview(DataModels.Offer offer);
    }

    private Context context;
    private List<DataModels.Offer> offerList;
    private List<String> offerIdList;
    private OnOfferActionListener listener;
    private boolean hasAcceptedOffer;

    public OfferAdapter(Context context, List<DataModels.Offer> offerList, List<String> offerIdList, OnOfferActionListener listener) {
        this.context = context;
        this.offerList = offerList;
        this.offerIdList = offerIdList;
        this.listener = listener;
        updateAcceptedState();
    }

    public void updateAcceptedState() {
        this.hasAcceptedOffer = false;
        for (DataModels.Offer offer : offerList) {
            if ("accepted".equalsIgnoreCase(offer.getStatus())) {
                this.hasAcceptedOffer = true;
                break;
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_offer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DataModels.Offer offer = offerList.get(position);
        String offerId = offerIdList.get(position);
        holder.bind(offer, offerId);
    }

    @Override
    public int getItemCount() {
        return offerList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewBuyerName, textViewOfferPrice, textViewStatus;
        MaterialButton btnAccept, btnReject, btnReview;
        LinearLayout layoutActions;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewBuyerName = itemView.findViewById(R.id.textViewBuyerName);
            textViewOfferPrice = itemView.findViewById(R.id.textViewOfferPrice);
            textViewStatus = itemView.findViewById(R.id.textViewStatus);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnReview = itemView.findViewById(R.id.btnReview);
        }

        void bind(final DataModels.Offer offer, final String offerId) {
            textViewBuyerName.setText(offer.getBuyerName());
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            textViewOfferPrice.setText(formatter.format(offer.getOfferPrice()));

            btnAccept.setOnClickListener(v -> listener.onAccept(offer, offerId));
            btnReject.setOnClickListener(v -> listener.onReject(offer, offerId));
            btnReview.setOnClickListener(v -> listener.onReview(offer));

            String status = offer.getStatus();

            if (hasAcceptedOffer) {
                layoutActions.setVisibility(View.GONE);
                textViewStatus.setVisibility(View.VISIBLE);
                if ("accepted".equalsIgnoreCase(status)) {
                    textViewStatus.setText("ĐÃ CHẤP NHẬN");
                    textViewStatus.setTextColor(ContextCompat.getColor(context, R.color.status_available));
                    btnReview.setVisibility(View.VISIBLE);
                } else {
                    textViewStatus.setText("ĐÃ TỪ CHỐI");
                    textViewStatus.setTextColor(ContextCompat.getColor(context, R.color.status_sold));
                    btnReview.setVisibility(View.GONE);
                }
            } else {
                if ("pending".equalsIgnoreCase(status)) {
                    layoutActions.setVisibility(View.VISIBLE);
                    textViewStatus.setVisibility(View.GONE);
                    btnReview.setVisibility(View.GONE);
                } else {
                    layoutActions.setVisibility(View.GONE);
                    btnReview.setVisibility(View.GONE);
                    textViewStatus.setVisibility(View.VISIBLE);
                    textViewStatus.setText("ĐÃ TỪ CHỐI");
                    textViewStatus.setTextColor(ContextCompat.getColor(context, R.color.status_sold));
                }
            }
        }
    }
}