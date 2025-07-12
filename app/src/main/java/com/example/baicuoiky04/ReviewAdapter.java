// Dán toàn bộ code này để thay thế file ReviewAdapter.java cũ

package com.example.baicuoiky04;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import de.hdodenhof.circleimageview.CircleImageView;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    public interface OnReviewActionListener {
        void onReportReview(DataModels.Review review);
    }

    private Context context;
    private List<DataModels.Review> reviewList;
    private OnReviewActionListener listener;

    public ReviewAdapter(Context context, List<DataModels.Review> reviewList, OnReviewActionListener listener) {
        this.context = context;
        this.reviewList = reviewList;
        this.listener = listener;
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
        ImageButton buttonMoreOptions;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewReviewer = itemView.findViewById(R.id.imageViewReviewer);
            textViewReviewerName = itemView.findViewById(R.id.textViewReviewerName);
            textViewComment = itemView.findViewById(R.id.textViewComment);
            ratingBarReview = itemView.findViewById(R.id.ratingBarReview);
            buttonMoreOptions = itemView.findViewById(R.id.buttonMoreOptions);
        }

        void bind(DataModels.Review review) {
            // Hiển thị tên, rating và ảnh
            textViewReviewerName.setText(review.getReviewerName());
            ratingBarReview.setRating((float) review.getRating());

            if (review.getReviewerPhotoUrl() != null && !review.getReviewerPhotoUrl().isEmpty()) {
                Glide.with(context).load(review.getReviewerPhotoUrl()).placeholder(R.drawable.ic_profile_placeholder).into(imageViewReviewer);
            } else {
                imageViewReviewer.setImageResource(R.drawable.ic_profile_placeholder);
            }

            // Hiển thị comment nếu có
            if (!TextUtils.isEmpty(review.getComment())) {
                textViewComment.setVisibility(View.VISIBLE);
                textViewComment.setText(review.getComment());
            } else {
                textViewComment.setVisibility(View.GONE);
            }

            // Xử lý nút 3 chấm (Báo cáo)
            String currentUserId = (FirebaseAuth.getInstance().getCurrentUser() != null) ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

            // Nút báo cáo chỉ hiện khi:
            // 1. Adapter này được khởi tạo với một listener (tức là nó đang ở ProfileFragment)
            // 2. Có người dùng đang đăng nhập
            // 3. Người dùng hiện tại không phải là người viết review này
            if (listener != null && currentUserId != null && !currentUserId.equals(review.getReviewerId())) {
                buttonMoreOptions.setVisibility(View.VISIBLE);
                buttonMoreOptions.setOnClickListener(v -> {
                    PopupMenu popup = new PopupMenu(context, v);
                    popup.getMenu().add("Báo cáo đánh giá này");
                    popup.setOnMenuItemClickListener(item -> {
                        listener.onReportReview(review);
                        return true;
                    });
                    popup.show();
                });
            } else {
                // Ẩn nút 3 chấm trong mọi trường hợp khác
                buttonMoreOptions.setVisibility(View.GONE);
            }
        }
    }
}