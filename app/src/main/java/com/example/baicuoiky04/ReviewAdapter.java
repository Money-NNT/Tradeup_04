package com.example.baicuoiky04;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    private Context context;
    private List<DataModels.Review> reviewList;

    public ReviewAdapter(Context context, List<DataModels.Review> reviewList) {
        this.context = context;
        this.reviewList = reviewList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DataModels.Review review = reviewList.get(position);
        holder.bind(review);
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imageViewReviewer;
        TextView textViewReviewerName, textViewComment;
        RatingBar ratingBarReview;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewReviewer = itemView.findViewById(R.id.imageViewReviewer);
            textViewReviewerName = itemView.findViewById(R.id.textViewReviewerName);
            textViewComment = itemView.findViewById(R.id.textViewComment);
            ratingBarReview = itemView.findViewById(R.id.ratingBarReview);
        }

        void bind(DataModels.Review review) {
            textViewReviewerName.setText(review.getReviewerName());
            ratingBarReview.setRating((float) review.getRating());

            if (review.getReviewerPhotoUrl() != null && !review.getReviewerPhotoUrl().isEmpty()) {
                Glide.with(context).load(review.getReviewerPhotoUrl()).placeholder(R.drawable.ic_profile_placeholder).into(imageViewReviewer);
            } else {
                imageViewReviewer.setImageResource(R.drawable.ic_profile_placeholder);
            }

            if (!TextUtils.isEmpty(review.getComment())) {
                textViewComment.setVisibility(View.VISIBLE);
                textViewComment.setText(review.getComment());
            } else {
                textViewComment.setVisibility(View.GONE);
            }
        }
    }
}