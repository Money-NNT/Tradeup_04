package com.example.baicuoiky04;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast; // Dòng import đã được thêm
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    private List<DataModels.Chat> chatList;
    private FirebaseUser currentUser;
    private Context context;

    public ConversationAdapter(Context context, List<DataModels.Chat> chatList) {
        this.context = context;
        this.chatList = chatList;
        this.currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DataModels.Chat chat = chatList.get(position);
        holder.bind(chat);
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imageViewAvatar;
        TextView textViewName, textViewLastMessage, textViewTimestamp;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewAvatar = itemView.findViewById(R.id.imageViewAvatar);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewLastMessage = itemView.findViewById(R.id.textViewLastMessage);
            textViewTimestamp = itemView.findViewById(R.id.textViewTimestamp);
        }

        void bind(DataModels.Chat chat) {
            if (currentUser == null) return;

            String otherUserId = null;
            String otherUserName = null;
            String otherUserPhoto = null;

            if (chat.getUser1Id() != null && currentUser.getUid().equals(chat.getUser1Id())) {
                otherUserId = chat.getUser2Id();
                otherUserName = chat.getUser2Name();
                otherUserPhoto = chat.getUser2Photo();
            } else if (chat.getUser2Id() != null && currentUser.getUid().equals(chat.getUser2Id())) {
                otherUserId = chat.getUser1Id();
                otherUserName = chat.getUser1Name();
                otherUserPhoto = chat.getUser1Photo();
            }

            textViewName.setText(otherUserName != null ? otherUserName : "Người dùng không xác định");
            textViewLastMessage.setText(chat.getLastMessage());

            if (chat.getLastMessageTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                textViewTimestamp.setText(sdf.format(chat.getLastMessageTimestamp()));
            } else {
                textViewTimestamp.setText("");
            }

            if (otherUserPhoto != null && !otherUserPhoto.isEmpty()) {
                Glide.with(context).load(otherUserPhoto).placeholder(R.drawable.ic_profile_placeholder).into(imageViewAvatar);
            } else {
                imageViewAvatar.setImageResource(R.drawable.ic_profile_placeholder);
            }

            final String finalOtherUserId = otherUserId;
            final String finalOtherUserName = otherUserName;
            itemView.setOnClickListener(v -> {
                if (finalOtherUserId != null && finalOtherUserName != null) {
                    Intent intent = new Intent(context, ChatActivity.class);
                    intent.putExtra("receiver_id", finalOtherUserId);
                    intent.putExtra("receiver_name", finalOtherUserName);
                    context.startActivity(intent);
                } else {
                    Toast.makeText(context, "Lỗi: Không tìm thấy thông tin người nhận.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}