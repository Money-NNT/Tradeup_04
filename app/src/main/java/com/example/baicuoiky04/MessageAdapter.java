package com.example.baicuoiky04;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

    private List<DataModels.Message> messageList;
    private String currentUserId;

    public MessageAdapter(List<DataModels.Message> messageList) {
        this.messageList = messageList;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @Override
    public int getItemViewType(int position) {
        DataModels.Message message = messageList.get(position);
        if (message.getSenderId().equals(currentUserId)) {
            // Nếu người gửi là user hiện tại -> hiển thị layout GỬI
            return VIEW_TYPE_MESSAGE_SENT;
        } else {
            // Nếu người gửi là người khác -> hiển thị layout NHẬN
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_message_sent, parent, false);
        } else { // viewType == VIEW_TYPE_MESSAGE_RECEIVED
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

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
        }

        void bind(DataModels.Message message) {
            textViewMessage.setText(message.getText());
        }
    }
}