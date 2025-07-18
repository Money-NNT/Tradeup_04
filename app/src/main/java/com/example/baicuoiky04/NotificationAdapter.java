package com.example.baicuoiky04;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private static final String TAG = "NotificationAdapter";
    private Context context;
    private List<DataModels.AppNotification> notificationList;
    private List<String> notificationIdList;

    public NotificationAdapter(Context context, List<DataModels.AppNotification> notificationList, List<String> notificationIdList) {
        this.context = context;
        this.notificationList = notificationList;
        this.notificationIdList = notificationIdList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DataModels.AppNotification notification = notificationList.get(position);
        String notificationId = notificationIdList.get(position);
        holder.bind(notification, notificationId);
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTitle, textViewBody, textViewTimestamp;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewBody = itemView.findViewById(R.id.textViewBody);
            textViewTimestamp = itemView.findViewById(R.id.textViewTimestamp);
        }

        void bind(final DataModels.AppNotification notification, final String notificationId) {
            // In log để kiểm tra dữ liệu
            Log.d(TAG, "Binding notification: " + notification.getTitle());

            textViewTitle.setText(notification.getTitle());
            textViewBody.setText(notification.getBody());

            if (notification.getCreatedAt() != null) {
                long now = System.currentTimeMillis();
                CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                        notification.getCreatedAt().getTime(), now, DateUtils.MINUTE_IN_MILLIS);
                textViewTimestamp.setText(relativeTime);
            }

            if (!notification.isRead()) {
                textViewTitle.setTypeface(null, Typeface.BOLD);
                textViewBody.setTypeface(null, Typeface.BOLD);
            } else {
                textViewTitle.setTypeface(null, Typeface.NORMAL);
                textViewBody.setTypeface(null, Typeface.NORMAL);
            }

            itemView.setOnClickListener(v -> {
                // Đánh dấu là đã đọc
                FirebaseFirestore.getInstance().collection("notifications").document(notificationId)
                        .update("read", true);

                // Điều hướng
                if (notification.getTitle() != null && notification.getTitle().contains("đã được chấp nhận")) {
                    Intent intent = new Intent(context, MyOffersActivity.class);
                    context.startActivity(intent);
                } else if (notification.getTitle() != null && notification.getTitle().contains("Bạn có trả giá mới")) {
                    Intent intent = new Intent(context, OffersActivity.class);
                    intent.putExtra("LISTING_ID", notification.getListingId());
                    context.startActivity(intent);
                } else if (notification.getChatId() != null && notification.getSenderId() != null) {
                    // Mở màn hình chat - Cần tên người gửi
                    // Lấy tên người gửi từ title để đơn giản hóa
                    String title = notification.getTitle();
                    String senderName = title.replace("Tin nhắn mới từ ", "").trim();
                    Intent intent = new Intent(context, ChatActivity.class);
                    intent.putExtra("receiver_id", notification.getSenderId());
                    intent.putExtra("receiver_name", senderName);
                    context.startActivity(intent);
                } else if (notification.getListingId() != null) {
                    Intent intent = new Intent(context, ListingDetailActivity.class);
                    intent.putExtra("LISTING_ID", notification.getListingId());
                    context.startActivity(intent);
                }
            });
        }
    }
}