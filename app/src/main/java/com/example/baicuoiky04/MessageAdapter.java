package com.example.baicuoiky04;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

    private List<DataModels.Message> messageList;
    private String currentUserId;

    public MessageAdapter(List<DataModels.Message> messageList) {
        this.messageList = messageList;
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    @Override
    public int getItemViewType(int position) {
        DataModels.Message message = messageList.get(position);
        if (currentUserId != null && message.getSenderId() != null && message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_MESSAGE_SENT;
        } else {
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_message_sent, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_message_received, parent, false);
        }
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        DataModels.Message message = messageList.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMessage;
        ImageView imageViewMessage;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            imageViewMessage = itemView.findViewById(R.id.imageViewMessage);
        }

        void bind(DataModels.Message message) {
            boolean isText = message.getText() != null && !message.getText().isEmpty();
            boolean isImage = message.getImageUrl() != null && !message.getImageUrl().isEmpty();

            if (isText) {
                textViewMessage.setVisibility(View.VISIBLE);
                imageViewMessage.setVisibility(View.GONE);
                textViewMessage.setText(message.getText());
            } else if (isImage) {
                textViewMessage.setVisibility(View.GONE);
                imageViewMessage.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                        .load(message.getImageUrl())
                        .placeholder(R.color.light_gray)
                        .into(imageViewMessage);
            } else {
                // Trường hợp tin nhắn rỗng, ẩn cả hai đi
                textViewMessage.setVisibility(View.GONE);
                imageViewMessage.setVisibility(View.GONE);
            }
        }
    }
}