package com.example.baicuoiky04;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class BlockedUserAdapter extends RecyclerView.Adapter<BlockedUserAdapter.ViewHolder> {

    public interface OnUnblockClickListener {
        void onUnblock(DataModels.User user);
    }

    private Context context;
    private List<DataModels.User> blockedUserList;
    private OnUnblockClickListener listener;

    public BlockedUserAdapter(Context context, List<DataModels.User> userList, OnUnblockClickListener listener) {
        this.context = context;
        this.blockedUserList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_blocked_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DataModels.User user = blockedUserList.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return blockedUserList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imageViewAvatar;
        TextView textViewName;
        MaterialButton btnUnblock;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewAvatar = itemView.findViewById(R.id.imageViewAvatar);
            textViewName = itemView.findViewById(R.id.textViewName);
            btnUnblock = itemView.findViewById(R.id.btnUnblock);
        }

        void bind(final DataModels.User user) {
            textViewName.setText(user.getDisplayName());
            if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                Glide.with(context).load(user.getPhotoUrl()).placeholder(R.drawable.ic_profile_placeholder).into(imageViewAvatar);
            } else {
                imageViewAvatar.setImageResource(R.drawable.ic_profile_placeholder);
            }

            btnUnblock.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUnblock(user);
                }
            });
        }
    }
}